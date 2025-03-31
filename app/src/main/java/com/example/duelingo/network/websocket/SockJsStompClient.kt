package com.example.duelingo.network.websocket

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

class StompManager(private val tokenManager: TokenManager) {

    private var stompClient: StompClient? = null
    private var lifecycleDisposable: Disposable? = null
    private var duelFoundDisposable: Disposable? = null
    private var matchmakingFailedDisposable: Disposable? = null
    private var isConnected = false

    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        try {
            val baseUrl = AppConfig.BASE_URL
            if (baseUrl.isBlank()) {
                throw IllegalStateException("Base URL is not configured")
            }

            val serverUrl = "${baseUrl.replace("http", "ws")}/duel-websocket"
            Log.d("StompManager", "Connecting to: $serverUrl")

            val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No token available")
            val headers = mutableMapOf(
                "Authorization" to "Bearer $token",
                "Origin" to baseUrl,
                "Connection" to "Upgrade",
                "Upgrade" to "websocket",
                "Sec-WebSocket-Protocol" to "v12.stomp"
            )
            if (token.isBlank()) {
                throw IllegalStateException("Token is empty")
            }

            stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                serverUrl,
                headers
            ).apply {
                withClientHeartbeat(10000)
                withServerHeartbeat(10000)
            }

            lifecycleDisposable = stompClient?.lifecycle()?.subscribe { event ->
                when (event.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d("StompManager", "Connected to $serverUrl")
                        isConnected = true
                        setupSubscriptions(onDuelFound, onMatchmakingFailed)
                        onConnected()
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e("StompManager", "Connection error", event.exception)
                        isConnected = false
                        onError(event.exception ?: Exception("Connection failed"))
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.d("StompManager", "Disconnected")
                        isConnected = false
                    }
                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                        Log.e("StompManager", "Server heartbeat failed")
                        isConnected = false
                        onError(Exception("Server heartbeat failed"))
                    }
                }
            }

            stompClient?.connect()
        } catch (e: Exception) {
            Log.e("StompManager", "Initialization error", e)
            onError(e)
        }
    }

    private fun setupSubscriptions(
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        duelFoundDisposable = stompClient?.topic("/topic/duel_found")?.subscribe { message ->
            try {
                val duelInfo = Gson().fromJson(message.payload, DuelFoundEvent::class.java)
                onDuelFound(duelInfo)
            } catch (e: Exception) {
                Log.e("StompManager", "Error parsing duel info", e)
            }
        }

        matchmakingFailedDisposable = stompClient?.topic("/topic/matchmaking_failed")?.subscribe { message ->
            try {
                val reason = Gson().fromJson(message.payload, MatchmakingFailedEvent::class.java)
                onMatchmakingFailed(reason)
            } catch (e: Exception) {
                Log.e("StompManager", "Error parsing matchmaking failed", e)
            }
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
            duelFoundDisposable?.dispose()
            matchmakingFailedDisposable?.dispose()
            lifecycleDisposable?.dispose()
            stompClient?.disconnect()
        } catch (e: Exception) {
            Log.e("StompManager", "Error disconnecting", e)
        } finally {
            isConnected = false
            stompClient = null
        }
    }

    fun isConnected(): Boolean = isConnected
}