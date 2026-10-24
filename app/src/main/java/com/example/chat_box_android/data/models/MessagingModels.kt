package com.example.chat_box_android.data.models

import com.google.gson.annotations.SerializedName

// ── Existing user / auth types ─────────────────────────────────────────────────

data class ChatUser(
    val id: String = "",
    val username: String,
    val email: String,
    @SerializedName("display_name") val displayName: String,
    val bio: String? = null,
    @SerializedName("updated_at") val updatedAt: String = ""
)

data class UpdateProfileRequest(
    @SerializedName("display_name") val displayName: String? = null,
    val bio: String? = null
)

// ── User Search ────────────────────────────────────────────────────────────────

data class UserSearchResult(
    val id: String,
    val username: String,
    val email: String,
    @SerializedName("display_name") val displayName: String
)

data class UserSearchResponse(
    val results: List<UserSearchResult>
)

// ── Chat Requests ──────────────────────────────────────────────────────────────

data class CreateChatRequestBody(
    @SerializedName("recipient_username") val recipientUsername: String? = null,
    @SerializedName("recipient_id") val recipientId: String? = null,
    @SerializedName("recipient_email") val recipientEmail: String? = null,
    @SerializedName("initial_client_message_id") val initialClientMessageId: String,
    @SerializedName("initial_ciphertext") val initialCiphertext: String
)

data class ChatRequest(
    val id: String,
    val sender: String,
    @SerializedName("sender_username") val senderUsername: String = "",
    val recipient: String,
    @SerializedName("recipient_username") val recipientUsername: String = "",
    @SerializedName("initial_client_message_id") val initialClientMessageId: String = "",
    val status: String,  // "pending" | "accepted" | "rejected" | "canceled"
    val conversation: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String? = null
)

data class AcceptRequestResponse(
    val detail: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("request_id") val requestId: String
)

// ── Conversations ──────────────────────────────────────────────────────────────

data class Conversation(
    val id: String,
    val title: String,
    @SerializedName("is_group") val isGroup: Boolean,
    @SerializedName("member_ids") val memberIds: List<String>?,
    @SerializedName("members") val members: List<ConversationMember>? = null,
    @SerializedName("last_message") val lastMessage: ChatMessage? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ConversationMember(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String = ""
)

data class CreateConversationRequest(
    val title: String? = null,
    @SerializedName("is_group") val isGroup: Boolean = false,
    @SerializedName("member_ids") val memberIds: List<String> = emptyList()
)

// ── Messages ───────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val conversation: String,
    val sender: String,
    @SerializedName("client_message_id") val clientMessageId: String,
    val ciphertext: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreateMessageRequest(
    val conversation: String,
    @SerializedName("client_message_id") val clientMessageId: String,
    val ciphertext: String
)

// ── Receipts ───────────────────────────────────────────────────────────────────

data class MessageReceipt(
    val id: String,
    val message: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreateMessageReceiptRequest(
    val message: String,
    val status: String
)

// ── Sync ───────────────────────────────────────────────────────────────────────

data class MessageSyncRequest(
    val messages: List<MessageSyncItem>
)

data class MessageSyncItem(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("client_message_id") val clientMessageId: String,
    val ciphertext: String
)

data class MessageSyncResponse(
    val acks: List<MessageSyncAck>
)

data class MessageSyncAck(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("client_message_id") val clientMessageId: String,
    @SerializedName("conversation_id") val conversationId: String,
    val status: String
)

// ── Local UI message model ─────────────────────────────────────────────────────

enum class MessageSendStatus { PENDING, SENT, FAILED, READ }

data class Message(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
    val clientMessageId: String = "",
    val sendStatus: MessageSendStatus = MessageSendStatus.SENT,
    val type: MessageType = MessageType.TEXT,
    val rawTimeMs: Long = System.currentTimeMillis() // for sorting
)

enum class MessageType { TEXT, IMAGE, FILE }
