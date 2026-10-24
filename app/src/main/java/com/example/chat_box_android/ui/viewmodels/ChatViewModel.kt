package com.example.chat_box_android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_box_android.data.local.PendingMessageQueue
import com.example.chat_box_android.data.local.TokenManager
import com.example.chat_box_android.data.models.*
import com.example.chat_box_android.data.network.WsConnectionState
import com.example.chat_box_android.data.network.WsEvent
import com.example.chat_box_android.data.network.WebSocketManager
import com.example.chat_box_android.data.repository.MessagingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<Message>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatViewModel(
    private val conversationId: String,
    private val repository: MessagingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val wsMgr: WebSocketManager = repository.wsMgr
    private val pendingQueue: PendingMessageQueue = repository.pendingQueue

    // Logged-in user's own ID — used to set isFromMe correctly
    private val myUserId: String = tokenManager.getUserId() ?: ""

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<WsConnectionState> = wsMgr.connectionState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Single source of truth for the in-memory message list
    private val messageMap = LinkedHashMap<String, Message>() // key = server id or pending_clientId
    private val seenServerIds = mutableSetOf<String>()
    private val seenClientIds = mutableSetOf<String>()

    init {
        wsMgr.connect()
        wsMgr.subscribeToConversation(conversationId)
        fetchMessages()
        observeWsEvents()
        observeReconnect()
    }

    // ── Fetch messages from REST ───────────────────────────────────────────────

    fun fetchMessages(since: String? = null, isSwipeRefresh: Boolean = false) {
        viewModelScope.launch {
            if (since == null && messageMap.isEmpty() && !isSwipeRefresh) {
                _uiState.value = ChatUiState.Loading
            }
            if (isSwipeRefresh) _isRefreshing.value = true

            repository.getMessages(conversationId, since).fold(
                onSuccess = { serverMessages ->
                    serverMessages.forEach { cm ->
                        if (cm.id !in seenServerIds) {
                            seenServerIds.add(cm.id)
                            if (cm.clientMessageId.isNotEmpty()) {
                                // If there was a pending placeholder for this, remove it
                                val pendingKey = "pending_${cm.clientMessageId}"
                                if (pendingKey in messageMap) {
                                    messageMap.remove(pendingKey)
                                }
                                seenClientIds.add(cm.clientMessageId)
                            }
                            messageMap[cm.id] = cm.toUiMessage(myUserId)
                        }
                    }
                    pushToUi()
                    _isRefreshing.value = false
                },
                onFailure = { err ->
                    if (messageMap.isEmpty()) {
                        _uiState.value = ChatUiState.Error(err.message ?: "Failed to load messages")
                    }
                    _isRefreshing.value = false
                }
            )
        }
    }

    // ── Send message ─────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val clientMsgId = UUID.randomUUID().toString()
        val pendingKey = "pending_$clientMsgId"

        // Optimistic — add to map immediately so it appears at bottom
        val optimistic = Message(
            id = pendingKey,
            text = text,
            isFromMe = true,
            timestamp = nowTimestamp(),
            clientMessageId = clientMsgId,
            sendStatus = MessageSendStatus.PENDING
        )
        messageMap[pendingKey] = optimistic
        seenClientIds.add(clientMsgId)
        pushToUi()

        // Persist for offline recovery
        pendingQueue.enqueue(
            PendingMessageQueue.PendingEntry(
                clientMessageId = clientMsgId,
                conversationId = conversationId,
                ciphertext = text
            )
        )

        // Try WebSocket first; offline queue handles the rest on reconnect
        if (wsMgr.connectionState.value == WsConnectionState.CONNECTED) {
            wsMgr.sendMessage(conversationId, clientMsgId, text)
        }
    }

    // ── WebSocket events ──────────────────────────────────────────────────────

    private fun observeWsEvents() {
        viewModelScope.launch {
            wsMgr.events.collect { event ->
                when (event) {
                    is WsEvent.MessageNew -> handleNewMessage(event)
                    is WsEvent.MessageAck -> handleAck(event)
                    is WsEvent.ReceiptEvent -> handleReceipt(event)
                    else -> Unit
                }
            }
        }
    }

    private fun handleNewMessage(event: WsEvent.MessageNew) {
        if (event.conversationId != conversationId) return
        if (event.messageId in seenServerIds) return

        seenServerIds.add(event.messageId)

        // If this is an echo of our own sent message — replace the pending bubble
        if (event.clientMessageId.isNotEmpty() && event.clientMessageId in seenClientIds) {
            val pendingKey = "pending_${event.clientMessageId}"
            val existing = messageMap[pendingKey]
            if (existing != null) {
                messageMap.remove(pendingKey)
                messageMap[event.messageId] = existing.copy(
                    id = event.messageId,
                    sendStatus = MessageSendStatus.SENT
                )
                pendingQueue.markSent(event.clientMessageId)
                pushToUi()
                return
            }
        }

        seenClientIds.add(event.clientMessageId)
        // Incoming message from another user
        val msg = Message(
            id = event.messageId,
            text = event.ciphertext,
            isFromMe = event.senderId == myUserId,
            timestamp = event.createdAt.toDisplayTime(),
            clientMessageId = event.clientMessageId,
            sendStatus = MessageSendStatus.SENT
        )
        messageMap[event.messageId] = msg
        pushToUi()

        // Send delivery receipt for messages from others
        if (!msg.isFromMe) {
            viewModelScope.launch { wsMgr.sendReceipt(event.messageId, "delivered") }
        }
    }

    private fun handleAck(event: WsEvent.MessageAck) {
        if (event.conversationId != conversationId) return
        val pendingKey = "pending_${event.clientMessageId}"
        val existing = messageMap[pendingKey]
        if (existing != null) {
            messageMap.remove(pendingKey)
            messageMap[event.messageId] = existing.copy(
                id = event.messageId,
                sendStatus = MessageSendStatus.SENT
            )
            seenServerIds.add(event.messageId)
            pendingQueue.markSent(event.clientMessageId)
            pushToUi()
        }
    }

    private fun handleReceipt(event: WsEvent.ReceiptEvent) {
        if (event.status == "read" && event.userId != myUserId) {
            val updated = messageMap.mapValues { (_, msg) ->
                if (msg.isFromMe && msg.sendStatus == MessageSendStatus.SENT)
                    msg.copy(sendStatus = MessageSendStatus.READ)
                else msg
            }
            messageMap.clear()
            messageMap.putAll(updated)
            pushToUi()
        }
    }

    private fun observeReconnect() {
        viewModelScope.launch {
            wsMgr.connectionState
                .filter { it == WsConnectionState.CONNECTED }
                .collect {
                    repository.flushOfflineQueue()
                    wsMgr.subscribeToConversation(conversationId)
                }
        }
    }

    fun sendReadReceipt(messageId: String) {
        viewModelScope.launch { wsMgr.sendReceipt(messageId, "read") }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId).onSuccess {
                messageMap.remove(messageId)
                seenServerIds.remove(messageId)
                pushToUi()
            }
        }
    }

    private fun pushToUi() {
        // Sort oldest → newest so the chat reads naturally top-to-bottom
        _uiState.value = ChatUiState.Success(
            messageMap.values.sortedBy { it.rawTimeMs }
        )
    }

    override fun onCleared() {
        super.onCleared()
        // WS connection is shared across the app — do not disconnect here
    }
}

// ── Extensions ────────────────────────────────────────────────────────────────

private fun ChatMessage.toUiMessage(myUserId: String) = Message(
    id = id,
    text = ciphertext,
    isFromMe = sender == myUserId,
    timestamp = createdAt.toDisplayTime(),
    clientMessageId = clientMessageId,
    sendStatus = MessageSendStatus.SENT,
    rawTimeMs = createdAt.toEpochMillis()
)

private fun String.toDisplayTime(): String = try {
    val trimmed = take(19)
    val parts = trimmed.split("T")
    if (parts.size == 2) parts[1].take(5) else take(16).replace("T", " ")
} catch (_: Exception) { this }

/**
 * Parse ISO 8601 timestamp to epoch millis for chronological sorting.
 * Example input: "2026-03-01T06:12:34.123456Z"
 */
private fun String.toEpochMillis(): Long = try {
    val clean = replace("Z", "+00:00")
        .let { if (it.length > 19 && it[19] == '.') it.substring(0, 19) + it.dropWhile { c -> c != '+' && c != '-' }.takeLastWhile { true } else it }
    // Simple manual parse: "YYYY-MM-DDTHH:MM:SS"
    val parts = take(19).split("T")
    val dateParts = parts[0].split("-")
    val timeParts = parts[1].split(":")
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.set(
        dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt(),
        timeParts[0].toInt(), timeParts[1].toInt(), timeParts[2].toInt()
    )
    cal.set(java.util.Calendar.MILLISECOND, 0)
    cal.timeInMillis
} catch (_: Exception) { System.currentTimeMillis() }

private fun nowTimestamp(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
