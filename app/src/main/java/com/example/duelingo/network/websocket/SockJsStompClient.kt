package com.example.duelingo.network.websocket

import android.annotation.SuppressLint
import android.util.Log
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.storage.TokenManager
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.provider.OkHttpConnectionProvider
import java.util.concurrent.TimeUnit

class StompManager(private val tokenManager: TokenManager) {

    private var stompClient: StompClient? = null
    private var subscriptions: MutableList<Disposable> = mutableListOf()
    private var isConnected = false

    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        try {
            val wsUrl = "ws://192.168.0.101:8082/ws"
            Log.d("StompManager", "Connecting to: $wsUrl")

            val token = tokenManager.getAccessToken()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Token is empty or not available")

            val headers = mapOf("Authorization" to "Bearer $token")

            stompClient = createStompClient(wsUrl, headers)
            setupConnectionListeners(onConnected, onError, onDuelFound, onMatchmakingFailed)
            stompClient?.connect()
        } catch (e: Exception) {
            Log.e("StompManager", "Initialization error", e)
            onError(e)
        }
    }


    private fun createStompClient(url: String, headers: Map<String, String>): StompClient {
        return Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            url,
            headers
        ).apply {
            withClientHeartbeat(15000)
            withServerHeartbeat(15000)
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
                    Log.d("StompManager", "WebSocket connection established")
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

    fun joinMatchmaking(): Boolean {
        if (!isConnected) {
            Log.e("StompManager", "Not connected")
            return false
        }

        return try {
            stompClient?.send("/app/matchmaking/join", "").run {
                Log.d("StompManager", "Join request sent")
                true
            }
        } catch (e: Exception) {
            Log.e("StompManager", "Error joining matchmaking", e)
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