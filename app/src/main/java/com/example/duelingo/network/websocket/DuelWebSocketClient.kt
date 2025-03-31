package com.example.duelingo.network.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.CancellableContinuation
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DuelWebSocketClient(
    private val tokenManager: TokenManager
) {
    private var webSocketClient: WebSocketClient? = null
    private var isManualDisconnect = false
    private val connectionLock = Any()
    private var onErrorCallback: ((Throwable) -> Unit)? = null
    private var connectionContinuation: CancellableContinuation<Unit>? = null

    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit,
        onMatchmakingFailed: (MatchmakingFailedEvent) -> Unit
    ) {
        if (webSocketClient?.isOpen == true) {
            onConnected()
            return
        }

        val uri = URI("${AppConfig.BASE_URL.replace("http", "ws")}/ws")
        val token = tokenManager.getAccessToken() ?: run {
            onError(IllegalStateException("No access token available"))
            return
        }

        webSocketClient = object : WebSocketClient(uri) {
            val handler = Handler(Looper.getMainLooper())
            var timeoutRunnable: Runnable? = null

            override fun onOpen(handshakedata: ServerHandshake?) {
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                try {
                    sendAuthMessage(token)
                    connectionContinuation?.resume(Unit)
                    onConnected()
                } catch (e: Exception) {
                    connectionContinuation?.resumeWithException(e)
                    onError(e)
                }
            }

            override fun onMessage(message: String) {
                try {
                    when {
                        message.contains("DuelFoundEvent") -> {
                            val duelInfo = Gson().fromJson(message, DuelFoundEvent::class.java)
                            onDuelFound(duelInfo)
                        }
                        message.contains("MatchmakingFailedEvent") -> {
                            val event = Gson().fromJson(message, MatchmakingFailedEvent::class.java)
                            onMatchmakingFailed(event)
                        }
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                if (!isManualDisconnect && code != 1000) {
                    connectionContinuation?.resumeWithException(
                        IOException("Connection closed: $reason (code $code)")
                    )
                }
            }

            override fun onError(ex: Exception) {
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                if (!isManualDisconnect) {
                    connectionContinuation?.resumeWithException(ex)
                }
            }
        }.apply {
            timeoutRunnable = Runnable {
                closeConnection(1006, "Connection timeout")
                connectionContinuation?.resumeWithException(
                    SocketTimeoutException("Connection timeout")
                )
            }
            handler.postDelayed(timeoutRunnable!!, 15000) // 15 секунд таймаут
            connect()
        }
    }


    fun joinMatchmaking(): Boolean {
        return synchronized(connectionLock) {
            try {
                if (webSocketClient?.isOpen == true) {
                    val joinMessage = """{"type":"join_matchmaking"}"""
                    webSocketClient?.send(joinMessage)
                    Log.d("WebSocket", "Join matchmaking message sent")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error joining matchmaking", e)
                false
            }
        }
    }

    fun isConnected(): Boolean {
        return webSocketClient?.isOpen == true
    }

    fun cancelMatchmaking() {
        synchronized(connectionLock) {
            try {
                if (webSocketClient?.isOpen == true) {
                    val cancelMessage = """{"type":"cancel_matchmaking"}"""
                    webSocketClient?.send(cancelMessage)
                    Log.d("WebSocket", "Cancel matchmaking message sent")
                } else {

                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error canceling matchmaking", e)
            } finally {
                disconnect()
            }
        }
    }

    fun disconnect() {
        synchronized(connectionLock) {
            isManualDisconnect = true
            try {
                webSocketClient?.close(1000, "Normal closure")
                Log.d("WebSocket", "Disconnected normally")
            } catch (e: Exception) {
                Log.e("WebSocket", "Error disconnecting", e)
            } finally {
                webSocketClient = null
            }
        }
    }

    private fun sendAuthMessage(token: String) {
        try {
            val authMessage = """{"type":"auth","token":"$token"}"""
            webSocketClient?.send(authMessage)
            Log.d("WebSocket", "Auth message sent")
        } catch (e: Exception) {
            Log.e("WebSocket", "Error sending auth message", e)
            onErrorCallback?.invoke(e)
        }
    }
}