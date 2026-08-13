package com.example.duelingo.network.websocket

import android.annotation.SuppressLint
import android.util.Log
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.dto.event.DuelResultEvent
import com.example.duelingo.dto.event.DuelChallengeEvent
import com.example.duelingo.dto.request.DuelFinishRequest
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class StompManager(private val tokenManager: TokenManager) {

    private var stompClient: StompClient? = null
    private var subscriptions: MutableList<Disposable> = mutableListOf()
    private var lifecycleDisposable: Disposable? = null
    private var connectionReadyDisposable: Disposable? = null
    private var isConnected = false

    @SuppressLint("CheckResult")
    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit,
        onDuelResult: (DuelResultEvent) -> Unit,
        onDuelChallenge: (DuelChallengeEvent) -> Unit = {}
    ) {
        try {
            val token = tokenManager.getAccessToken() ?: throw IllegalStateException("Token is empty")

            // 1. Формируем URL с токеном в query параметре
            val wsUrl = "${AppConfig.BASE_URL.trimEnd('/').replaceFirst("https", "wss").replaceFirst("http", "ws")}/ws/websocket"

            // 2. Добавляем заголовок Authorization
            val authorization = "Bearer $token"
            val httpHeaders = mapOf("Authorization" to authorization)
            val stompHeaders = listOf(StompHeader("Authorization", authorization))

            stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                wsUrl,
                httpHeaders
            ).apply {
                withClientHeartbeat(10000)
                withServerHeartbeat(10000)
            }

            setupConnectionListeners(onConnected, onError, onDuelFound, onMatchmakingFailed, onDuelResult, onDuelChallenge)
            stompClient?.connect(stompHeaders)
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
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit,
        onDuelResult: (DuelResultEvent) -> Unit,
        onDuelChallenge: (DuelChallengeEvent) -> Unit
    ) {
        lifecycleDisposable = stompClient?.lifecycle()?.subscribe({ event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("StompManager", "WebSocket opened. Waiting for STOMP CONNECTED...")
                    waitForStompConnection(
                        onConnected,
                        onError,
                        onDuelFound,
                        onMatchmakingFailed,
                        onDuelResult,
                        onDuelChallenge
                    )
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("StompManager", "Connection error", event.exception)
                    isConnected = false
                    onError(event.exception ?: Exception("Unknown connection error"))
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("StompManager", "Disconnected")
                    isConnected = false
                    connectionReadyDisposable?.dispose()
                    subscriptions.forEach { it.dispose() }
                    subscriptions.clear()
                }
                else -> {}
            }
        }, { error ->
            Log.e("StompManager", "Lifecycle stream error", error)
            isConnected = false
            onError(error)
        })
    }

    private fun waitForStompConnection(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit,
        onDuelResult: (DuelResultEvent) -> Unit,
        onDuelChallenge: (DuelChallengeEvent) -> Unit
    ) {
        val client = stompClient ?: return
        connectionReadyDisposable?.dispose()
        connectionReadyDisposable = Observable.interval(0, 50, TimeUnit.MILLISECONDS)
            .filter { client.isConnected }
            .firstOrError()
            .timeout(10, TimeUnit.SECONDS)
            .subscribe({
                if (stompClient !== client) return@subscribe
                Log.d("StompManager", "STOMP connected. Subscribing to topics...")
                isConnected = true
                setupSubscriptions(onDuelFound, onMatchmakingFailed, onDuelResult, onDuelChallenge)
                onConnected()
            }, { error ->
                Log.e("StompManager", "STOMP connection was not established", error)
                isConnected = false
                onError(error)
            })
    }
    private fun setupSubscriptions(
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit,
        onDuelResult: (DuelResultEvent) -> Unit,
        onDuelChallenge: (DuelChallengeEvent) -> Unit
    ) {
        // Используем user-specific destinations
        subscriptions.add(stompClient?.topic("/user/queue/duel-found")?.subscribe(
            { message -> parseAndHandle(message.payload, DuelFoundEvent::class.java, onDuelFound, "duel info") },
            { error -> Log.e("StompManager", "Duel subscription failed", error) }
        ) ?: return)

        subscriptions.add(stompClient?.topic("/user/queue/matchmaking-failed")?.subscribe(
            { message -> parseAndHandle(message.payload, MatchmakingFailedEvent::class.java, onMatchmakingFailed, "matchmaking failed") },
            { error -> Log.e("StompManager", "Matchmaking subscription failed", error) }
        ) ?: return)

        subscriptions.add(stompClient?.topic("/user/queue/duel-result")?.subscribe(
            { message ->
                Log.d("StompManager", "Received duel result: ${message.payload}")
                parseAndHandle(message.payload, DuelResultEvent::class.java, onDuelResult, "duel result")
            },
            { error -> Log.e("StompManager", "Duel result subscription failed", error) }
        ) ?: return)

        subscriptions.add(stompClient?.topic("/user/queue/duel-challenge")?.subscribe(
            { message -> parseAndHandle(message.payload, DuelChallengeEvent::class.java, onDuelChallenge, "duel challenge") },
            { error -> Log.e("StompManager", "Challenge subscription failed", error) }
        ) ?: return)
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
    fun joinMatchmaking(difficulty: String = "MEDIUM"): Boolean {
        if (!isConnected) {
            Log.e("StompManager", "Not connected")
            return false
        }

        return try {
            Log.d("StompManager", "Sending JOIN to /app/matchmaking/join")
            stompClient?.send("/app/matchmaking/join", Gson().toJson(mapOf("difficulty" to difficulty)))
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
            connectionReadyDisposable?.dispose()
            connectionReadyDisposable = null
            lifecycleDisposable?.dispose()
            lifecycleDisposable = null

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

    @SuppressLint("CheckResult")
    fun finishDuel(duelId: String, correctAnswers: Int, timeSpent: Long): Boolean {
        if (!isConnected) {
            Log.e("StompManager", "Not connected")
            return false
        }

        return try {
            val request = DuelFinishRequest(duelId, correctAnswers, timeSpent, emptyList())
            Log.d("StompManager", "Sending duel finish request: $request")
            stompClient?.send("/app/duel/finish", Gson().toJson(request))
                ?.subscribe(
                    { Log.d("StompManager", "Duel finish request sent successfully") },
                    { error -> Log.e("StompManager", "Error sending duel finish request", error) }
                )
            true
        } catch (e: Exception) {
            Log.e("StompManager", "Error sending duel finish request", e)
            false
        }
    }
}
