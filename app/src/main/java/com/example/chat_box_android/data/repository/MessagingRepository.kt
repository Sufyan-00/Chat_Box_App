package com.example.chat_box_android.data.repository

import com.example.chat_box_android.data.local.PendingMessageQueue
import com.example.chat_box_android.data.models.*
import com.example.chat_box_android.data.network.WebSocketManager
import com.example.chat_box_android.data.network.services.MessagingApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessagingRepository(
    val apiService: MessagingApiService,
    val wsMgr: WebSocketManager,
    val pendingQueue: PendingMessageQueue,
    val nameCache: com.example.chat_box_android.data.local.ConversationNameCache
) {

    // ── Profile ──────────────────────────────────────────────────────
    suspend fun getMyProfile(): Result<ChatUser> =
        withContext(Dispatchers.IO) { runCatching { apiService.getMyProfile() } }

    suspend fun updateProfile(req: UpdateProfileRequest): Result<ChatUser> =
        withContext(Dispatchers.IO) { runCatching { apiService.updateProfile(req) } }

    // ── User Search ──────────────────────────────────────────────────────────
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.searchUsers(query).results }
        }

    // ── Chat Requests ────────────────────────────────────────────────────────
    suspend fun sendChatRequest(body: CreateChatRequestBody): Result<ChatRequest> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.createChatRequest(body) }
        }

    suspend fun getIncomingRequests(): Result<List<ChatRequest>> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.getChatRequests("incoming") }
        }

    suspend fun getOutgoingRequests(): Result<List<ChatRequest>> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.getChatRequests("outgoing") }
        }

    suspend fun acceptRequest(id: String): Result<AcceptRequestResponse> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.acceptChatRequest(id) }
        }

    suspend fun rejectRequest(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.rejectChatRequest(id); Unit }
        }

    // ── Conversations ────────────────────────────────────────────────────────
    suspend fun getConversations(): Result<List<Conversation>> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.getConversations() }
        }

    suspend fun createConversation(request: CreateConversationRequest): Result<Conversation> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.createConversation(request) }
        }

    suspend fun deleteConversation(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.deleteConversation(id) }
        }

    // ── Messages ─────────────────────────────────────────────────────────────
    suspend fun getMessages(
        conversationId: String,
        since: String? = null,
        limit: Int? = null
    ): Result<List<ChatMessage>> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.getMessages(conversationId, since, limit) }
        }

    suspend fun sendMessageRest(request: CreateMessageRequest): Result<ChatMessage> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.createMessage(request) }
        }

    suspend fun deleteMessage(messageId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.deleteMessage(messageId) }
        }

    // ── Offline Queue Flush ──────────────────────────────────────────────────
    /**
     * Called on reconnect or app restore — sends all persisted pending messages
     * via the sync endpoint so nothing is lost.
     */
    suspend fun flushOfflineQueue(): Result<MessageSyncResponse> =
        withContext(Dispatchers.IO) {
            val pending = pendingQueue.getAll()
            if (pending.isEmpty()) return@withContext Result.success(MessageSyncResponse(emptyList()))
            val items = pending.map {
                MessageSyncItem(
                    conversationId = it.conversationId,
                    clientMessageId = it.clientMessageId,
                    ciphertext = it.ciphertext
                )
            }
            runCatching {
                val response = apiService.syncMessages(MessageSyncRequest(items))
                // Remove acked messages from the queue
                response.acks.forEach { pendingQueue.markSent(it.clientMessageId) }
                response
            }
        }

    // ── Receipts ─────────────────────────────────────────────────────────────
    suspend fun sendReceipt(messageId: String, status: String): Result<MessageReceipt> =
        withContext(Dispatchers.IO) {
            runCatching { apiService.createReceipt(CreateMessageReceiptRequest(messageId, status)) }
        }
}
