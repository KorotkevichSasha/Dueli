package com.example.duelingo.network.websocket

import android.content.Context
import android.util.Log
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class DuelWebSocketClient(
    private val context: Context,
    private val tokenManager: TokenManager
) {
    private var webSocketClient: WebSocketClient? = null
    private var onDuelFoundCallback: ((DuelFoundEvent) -> Unit)? = null

    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit,
        onDuelFound: (DuelFoundEvent) -> Unit
    ) {
        this.onDuelFoundCallback = onDuelFound

        val uri = URI("${AppConfig.BASE_URL.replace("http", "ws")}/ws")
        val token = tokenManager.getAccessToken() ?: run {
            onError(IllegalStateException("No access token available"))
            return
        }

        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("WebSocket", "Connection opened")
                sendAuthMessage(token)
                onConnected()
            }

            override fun onMessage(message: String) {
                try {
                    val duelInfo = Gson().fromJson(message, DuelFoundEvent::class.java)
                    onDuelFoundCallback?.invoke(duelInfo)
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message", e)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "Connection closed: $reason")
            }

            override fun onError(ex: Exception) {
                Log.e("WebSocket", "Error", ex)
                onError(ex)
            }
        }.apply {
            connect()
        }
    }

    private fun sendAuthMessage(token: String) {
        val authMessage = """
            {
                "type": "auth",
                "token": "$token"
            }
        """.trimIndent()
        webSocketClient?.send(authMessage)
    }

    fun joinMatchmaking() {
        val joinMessage = """
            {
                "type": "join_matchmaking"
            }
        """.trimIndent()
        webSocketClient?.send(joinMessage)
    }
    fun isConnected(): Boolean {
        return webSocketClient?.isOpen == true
    }

    fun cancelMatchmaking() {
        try {
            if (isConnected()) {
                val cancelMessage = """{"type": "cancel_matchmaking"}"""
                webSocketClient?.send(cancelMessage)
            }
            disconnect()
        } catch (e: Exception) {
            Log.e("WebSocket", "Cancel error", e)
        }
    }

    fun disconnect() {
        try {
            webSocketClient?.close()
        } catch (e: Exception) {
            Log.e("WebSocket", "Disconnect error", e)
        }
    }
}