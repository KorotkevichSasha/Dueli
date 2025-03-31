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
import org.java_websocket.drafts.Draft_6455
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
        Log.d("WebSocket", "⚡ Initializing connection...")

        if (webSocketClient?.isOpen == true) {
            Log.d("WebSocket", "ℹ️ Already connected, reusing existing connection")
            onConnected()
            return
        }

        val uri = URI("${AppConfig.BASE_URL.replace("http", "ws")}/duel-websocket")
        Log.d("WebSocket", "🌐 Connecting to: $uri")

        val token = tokenManager.getAccessToken() ?: run {

            val error = IllegalStateException("No access token available")
            Log.e("WebSocket", "🔴 No access token", error)
            onError(error)
            return
        }
        Log.d("WebSocket", "Full token: $token")

        webSocketClient = object : WebSocketClient(
            URI("${AppConfig.BASE_URL.replace("http", "ws")}/ws"),
            Draft_6455(),
            mutableMapOf(
                "Authorization" to "Bearer $token",
                "Origin" to AppConfig.BASE_URL,
                "Connection" to "Upgrade",
                "Upgrade" to "websocket"
            ),
            15000
        ) {
            val handler = Handler(Looper.getMainLooper())
            var timeoutRunnable: Runnable? = null

            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("WebSocket", "✅ Connection opened! Handshake status: ${handshakedata?.httpStatus}")
                Log.d("WebSocket", "✅ Connected! Protocol: ${handshakedata?.httpStatusMessage}")
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                try {
                    Log.d("WebSocket", "🔐 Attempting to authenticate...")
                    sendAuthMessage(token)
                    connectionContinuation?.resume(Unit)
                    onConnected()
                    Log.d("WebSocket", "🟢 Connection fully established")
                } catch (e: Exception) {
                    Log.e("WebSocket", "🔴 Connection error in onOpen", e)
                    connectionContinuation?.resumeWithException(e)
                    onError(e)
                }
            }

            override fun onMessage(message: String) {
                Log.d("WebSocket", "📨 Received message: ${message.take(200)}...")
                try {
                    when {
                        message.contains("DuelFoundEvent") -> {
                            Log.d("WebSocket", "🎉 Duel found event received")
                            val duelInfo = Gson().fromJson(message, DuelFoundEvent::class.java)
                            Log.d("WebSocket", "🤺 Duel info: vs ${duelInfo.opponentId}")
                            onDuelFound(duelInfo)
                        }
                        message.contains("MatchmakingFailedEvent") -> {
                            Log.d("WebSocket", "❌ Matchmaking failed event received")
                            val event = Gson().fromJson(message, MatchmakingFailedEvent::class.java)
                            Log.d("WebSocket", "🛑 Failure reason: ${event.reason}")
                            onMatchmakingFailed(event)
                        }
                        else -> {
                            Log.d("WebSocket", "ℹ️ Unknown message type received")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "🔴 Error processing message", e)
                    onError(e)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "🚪 Connection closed. Code: $code, Reason: $reason, Remote: $remote")
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                if (!isManualDisconnect && code != 1000) {
                    val exception = IOException("Connection closed: $reason (code $code)")
                    Log.e("WebSocket", "🔴 Unexpected disconnect", exception)
                    connectionContinuation?.resumeWithException(exception)
                }
                when (code) {
                    400 -> Log.e("WebSocket", "🛑 Server rejected connection (bad request)")
                    1002 -> Log.e("WebSocket", "🛑 Protocol error")
                }
            }

            override fun onError(ex: Exception) {
                Log.e("WebSocket", "‼️ Critical error: ${ex.javaClass.simpleName}", ex)
                onError(ex)
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                if (!isManualDisconnect) {
                    connectionContinuation?.resumeWithException(ex)
                }
            }
        }.apply {
            timeoutRunnable = Runnable {
                Log.w("WebSocket", "⏰ Connection timeout reached (15s)")
                closeConnection(1006, "Connection timeout")
                connectionContinuation?.resumeWithException(
                    SocketTimeoutException("Connection timeout")
                )
            }
            Log.d("WebSocket", "⏳ Starting connection with 15s timeout...")
            handler.postDelayed(timeoutRunnable!!, 30000    )
            connect()
        }
    }

    fun joinMatchmaking(): Boolean {
        Log.d("WebSocket", "🔍 Attempting to join matchmaking...")
        return synchronized(connectionLock) {
            try {
                if (webSocketClient?.isOpen == true) {
                    val stompMessage = """
                    |SEND
                    |destination:/app/matchmaking/join
                    |content-type:application/json
                    |content-length:0
                    |
                    |\u0000
                    """.trimMargin()

                    Log.d("WebSocket", "📤 Sending STOMP join request")
                    webSocketClient?.send(stompMessage)
                    Log.d("WebSocket", "🟢 STOMP join request sent successfully")
                    true
                } else {
                    Log.e("WebSocket", "🔴 Can't join - connection not open")
                    false
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "🔴 Error joining matchmaking", e)
                false
            }
        }
    }

    fun isConnected(): Boolean {
        val isConnected = webSocketClient?.isOpen == true
        Log.d("WebSocket", "ℹ️ Connection check: ${if (isConnected) "🟢 Connected" else "🔴 Disconnected"}")
        return isConnected
    }

    fun cancelMatchmaking() {
        Log.d("WebSocket", "🛑 Attempting to cancel matchmaking...")
        synchronized(connectionLock) {
            try {
                if (webSocketClient?.isOpen == true) {
                    val cancelMessage = """{"type":"cancel_matchmaking"}"""
                    Log.d("WebSocket", "📤 Sending cancel_matchmaking: $cancelMessage")
                    webSocketClient?.send(cancelMessage)
                    Log.d("WebSocket", "✅ Matchmaking cancelled")
                } else {
                    Log.d("WebSocket", "ℹ️ No active connection to cancel")
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "🔴 Error canceling matchmaking", e)
            } finally {
                disconnect()
            }
        }
    }

    fun disconnect() {
        Log.d("WebSocket", "🚪 Starting disconnect...")
        synchronized(connectionLock) {
            isManualDisconnect = true
            try {
                Log.d("WebSocket", "ℹ️ Closing connection...")
                webSocketClient?.close(1000, "Normal closure")
                Log.d("WebSocket", "✅ Disconnected normally")
            } catch (e: Exception) {
                Log.e("WebSocket", "🔴 Error disconnecting", e)
            } finally {
                webSocketClient = null
                Log.d("WebSocket", "🧹 Connection resources cleaned up")
            }
        }
    }

    private fun sendAuthMessage(token: String) {
        try {
            val authMessage = """{"type":"auth","token":"$token"}"""
            Log.d("WebSocket", "🔐 Sending auth message (token: ${token.take(5)}...)")
            webSocketClient?.send(authMessage)
            Log.d("WebSocket", "✅ Auth message sent")
        } catch (e: Exception) {
            Log.e("WebSocket", "🔴 Error sending auth message", e)
            onErrorCallback?.invoke(e)
        }
    }
}