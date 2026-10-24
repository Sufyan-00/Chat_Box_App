package com.example.chat_box_android.data.network

import android.util.Log
import com.example.chat_box_android.data.local.TokenManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

// ── WebSocket event types the ViewModel will observe ──────────────────────────

sealed class WsEvent {
    data class MessageNew(
        val messageId: String,
        val conversationId: String,
        val senderId: String,
        val ciphertext: String,
        val clientMessageId: String,
        val createdAt: String
    ) : WsEvent()

    data class MessageAck(
        val messageId: String,
        val conversationId: String,
        val clientMessageId: String,
        val status: String,      // "stored" | "ack"
        val idempotent: Boolean,
        val createdAt: String
    ) : WsEvent()

    data class ReceiptEvent(
        val messageId: String,
        val status: String,      // "delivered" | "read"
        val userId: String,
        val createdAt: String
    ) : WsEvent()

    data class Subscribed(val conversationId: String) : WsEvent()
    object Disconnected : WsEvent()
    object Reconnecting : WsEvent()
}

enum class WsConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

// ── Manager ────────────────────────────────────────────────────────────────────

class WebSocketManager(private val tokenManager: TokenManager) {

    companion object {
        private const val TAG = "WebSocketManager"
        private const val MAX_BACKOFF_MS = 30_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var reconnectAttempts = 0
    private var isIntentionallyClosed = false

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    // ── Outgoing message queue (if sent before connection is up) ──────────────
    private val pendingJsonQueue = ArrayDeque<String>()

    fun connect() {
        val token = tokenManager.getAccessToken() ?: run {
            Log.w(TAG, "No access token, cannot connect WebSocket")
            return
        }
        isIntentionallyClosed = false
        openConnection(token)
    }

    private fun openConnection(token: String) {
        _connectionState.value = WsConnectionState.CONNECTING
        val url = ApiConfig.WS_URL + token
        val request = Request.Builder().url(url).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                reconnectAttempts = 0
                _connectionState.value = WsConnectionState.CONNECTED
                // Flush any queued messages
                while (pendingJsonQueue.isNotEmpty()) {
                    webSocket.send(pendingJsonQueue.removeFirst())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WS Recv: $text")
                handleIncoming(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS Closing: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS Closed: $reason")
                _connectionState.value = WsConnectionState.DISCONNECTED
                scope.launch { _events.emit(WsEvent.Disconnected) }
                if (!isIntentionallyClosed) scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS Failure: ${t.message}")
                _connectionState.value = WsConnectionState.DISCONNECTED
                scope.launch { _events.emit(WsEvent.Reconnecting) }
                if (!isIntentionallyClosed) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        scope.launch {
            val backoff = minOf(1000L * (1 shl reconnectAttempts), MAX_BACKOFF_MS)
            reconnectAttempts++
            Log.d(TAG, "Reconnecting in ${backoff}ms (attempt $reconnectAttempts)")
            delay(backoff)
            val token = tokenManager.getAccessToken() ?: return@launch
            openConnection(token)
        }
    }

    fun disconnect() {
        isIntentionallyClosed = true
        ws?.close(1000, "User disconnected")
        ws = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun subscribeToConversation(conversationId: String) {
        sendJson("""{"action":"conversation.subscribe","conversation_id":"$conversationId"}""")
    }

    fun sendMessage(conversationId: String, clientMessageId: String, ciphertext: String) {
        sendJson("""{"action":"message.send","conversation_id":"$conversationId","client_message_id":"$clientMessageId","ciphertext":"$ciphertext"}""")
    }

    fun sendReceipt(messageId: String, status: String) {
        sendJson("""{"action":"receipt.send","message_id":"$messageId","status":"$status"}""")
    }

    private fun sendJson(json: String) {
        val sent = ws?.send(json) ?: false
        if (!sent) {
            Log.w(TAG, "WS not connected, queuing message")
            pendingJsonQueue.addLast(json)
        }
    }

    // ── Inbound message handling ───────────────────────────────────────────────

    private fun handleIncoming(text: String) {
        try {
            val obj: JsonObject = JsonParser.parseString(text).asJsonObject
            val type = obj.get("type")?.asString ?: return
            scope.launch {
                when (type) {
                    "message.new" -> _events.emit(
                        WsEvent.MessageNew(
                            messageId = obj.get("message_id").asString,
                            conversationId = obj.get("conversation_id").asString,
                            senderId = obj.get("sender_id").asString,
                            ciphertext = obj.get("ciphertext").asString,
                            clientMessageId = obj.get("client_message_id")?.asString ?: "",
                            createdAt = obj.get("created_at")?.asString ?: ""
                        )
                    )
                    "message.ack" -> _events.emit(
                        WsEvent.MessageAck(
                            messageId = obj.get("message_id").asString,
                            conversationId = obj.get("conversation_id").asString,
                            clientMessageId = obj.get("client_message_id")?.asString ?: "",
                            status = obj.get("status")?.asString ?: "stored",
                            idempotent = obj.get("idempotent")?.asBoolean ?: false,
                            createdAt = obj.get("created_at")?.asString ?: ""
                        )
                    )
                    "message.receipt" -> _events.emit(
                        WsEvent.ReceiptEvent(
                            messageId = obj.get("message_id").asString,
                            status = obj.get("status").asString,
                            userId = obj.get("user_id").asString,
                            createdAt = obj.get("created_at")?.asString ?: ""
                        )
                    )
                    "conversation.subscribed" -> _events.emit(
                        WsEvent.Subscribed(obj.get("conversation_id").asString)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WS message: $text", e)
        }
    }
}
