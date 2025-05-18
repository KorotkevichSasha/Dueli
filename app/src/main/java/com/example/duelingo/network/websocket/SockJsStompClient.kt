package com.example.duelingo.network.websocket

import android.annotation.SuppressLint
import android.util.Log
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.net.URLEncoder

class StompManager(private val tokenManager: TokenManager) {

    private var stompClient: StompClient? = null
    private var subscriptions: MutableList<Disposable> = mutableListOf()
    private var isConnected = false

    @SuppressLint("CheckResult")
    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        try {
            val token = tokenManager.getAccessToken() ?: throw IllegalStateException("Token is empty")

            // 1. Формируем URL с токеном в query параметре
            val wsUrl = "${AppConfig.BASE_URL.replace("http", "ws")}/ws/websocket"

            // 2. Добавляем заголовок Authorization
            val headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept-Version" to "1.2",
                "Heart-Beat" to "10000,10000"
            )

            stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                wsUrl,
                headers
            ).apply {
                withClientHeartbeat(10000)
                withServerHeartbeat(10000)
            }

            setupConnectionListeners(onConnected, onError, onDuelFound, onMatchmakingFailed)
            stompClient?.connect()
        } catch (e: Exception) {
            Log.e("StompManager", "Connection error", e)
            onError(e)
        }
    }

    @SuppressLint("CheckResult")
    private fun setupConnectionListeners(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        stompClient?.lifecycle()?.subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("StompManager", "Connection established. Subscribing to topics...")
                    isConnected = true
                    setupSubscriptions(onDuelFound, onMatchmakingFailed)
                    onConnected()
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("StompManager", "Connection error", event.exception)
                    isConnected = false
                    onError(event.exception ?: Exception("Unknown connection error"))
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("StompManager", "Disconnected")
                    isConnected = false
                    subscriptions.forEach { it.dispose() }
                    subscriptions.clear()
                }
                else -> {}
            }
        }
    }
    private fun setupSubscriptions(
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        // Используем user-specific destinations
        subscriptions.add(stompClient?.topic("/user/queue/duel-found")?.subscribe { message ->
            parseAndHandle(message.payload, DuelFoundEvent::class.java, onDuelFound, "duel info")
        } ?: return)

        subscriptions.add(stompClient?.topic("/user/queue/matchmaking-failed")?.subscribe { message ->
            parseAndHandle(message.payload, MatchmakingFailedEvent::class.java, onMatchmakingFailed, "matchmaking failed")
        } ?: return)
    }




    private fun <T> parseAndHandle(payload: String, clazz: Class<T>, handler: (T) -> Unit, logName: String) {
        try {
            val obj = Gson().fromJson(payload, clazz)
            Log.d("StompManager", "Received $logName: $obj")
            handler(obj)
        } catch (e: Exception) {
            Log.e("StompManager", "Error parsing $logName", e)
        }
    }

    @SuppressLint("CheckResult")
    fun joinMatchmaking(): Boolean {
        if (!isConnected) {
            Log.e("StompManager", "Not connected")
            return false
        }

        return try {
            Log.d("StompManager", "Sending JOIN to /app/matchmaking/join")
            stompClient?.send("/app/matchmaking/join", "")
                ?.subscribe(
                    { Log.d("StompManager", "Join request sent successfully") },
                    { error -> Log.e("StompManager", "Error sending join request", error) }
                )
            true
        } catch (e: Exception) {
            Log.e("StompManager", "Error sending join request", e)
            false
        }
    }

    fun cancelMatchmaking() {
        if (!isConnected) return

        try {
            stompClient?.send("/app/matchmaking/cancel", "").run {
                Log.d("StompManager", "Cancel request sent")
            }
        } catch (e: Exception) {
            Log.e("StompManager", "Error canceling matchmaking", e)
        }
    }

    fun disconnect() {
        try {
            Log.d("StompManager", "Disconnecting...")

            subscriptions.forEach { disposable ->
                try {
                    disposable.dispose()
                    Log.d("StompManager", "Disposed subscription")
                } catch (e: Exception) {
                    Log.e("StompManager", "Error disposing subscription", e)
                }
            }
            subscriptions.clear()

            stompClient?.let { client ->
                try {
                    client.disconnect()
                    Log.d("StompManager", "Disconnect request sent")
                } catch (e: Exception) {
                    Log.e("StompManager", "Error during disconnect", e)
                    try {
                        client.disconnect()
                    } catch (e: Exception) {
                        Log.e("StompManager", "Force disconnect failed", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StompManager", "Unexpected error during disconnect", e)
        } finally {
            // 3. Обновляем состояние
            isConnected = false
            stompClient = null
            Log.d("StompManager", "Disconnected and cleaned up")
        }
    }

    fun isConnected(): Boolean = isConnected
}