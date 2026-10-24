package com.example.chat_box_android.data.network.services

import com.example.chat_box_android.data.models.*
import retrofit2.http.*

interface MessagingApiService {

    // ── User Profile ─────────────────────────────────────────────────────────
    @GET("api/users/me/")
    suspend fun getMyProfile(): ChatUser

    @PATCH("api/users/me/")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ChatUser

    // ── User Search ──────────────────────────────────────────────────────────
    @GET("api/messaging/users/search/")
    suspend fun searchUsers(@Query("q") query: String): UserSearchResponse

    // ── Chat Requests ────────────────────────────────────────────────────────
    @POST("api/messaging/requests/")
    suspend fun createChatRequest(@Body body: CreateChatRequestBody): ChatRequest

    @GET("api/messaging/requests/")
    suspend fun getChatRequests(@Query("direction") direction: String): List<ChatRequest>

    @POST("api/messaging/requests/{id}/accept/")
    suspend fun acceptChatRequest(@Path("id") id: String): AcceptRequestResponse

    @POST("api/messaging/requests/{id}/reject/")
    suspend fun rejectChatRequest(@Path("id") id: String): Map<String, String>

    // ── Conversations ────────────────────────────────────────────────────────
    @GET("api/messaging/conversations/")
    suspend fun getConversations(): List<Conversation>

    @POST("api/messaging/conversations/")
    suspend fun createConversation(@Body request: CreateConversationRequest): Conversation

    @DELETE("api/messaging/conversations/{id}/")
    suspend fun deleteConversation(@Path("id") id: String)

    // ── Messages ─────────────────────────────────────────────────────────────
    @GET("api/messaging/messages/")
    suspend fun getMessages(
        @Query("conversation") conversationId: String,
        @Query("since") since: String? = null,
        @Query("limit") limit: Int? = null
    ): List<ChatMessage>

    @POST("api/messaging/messages/")
    suspend fun createMessage(@Body request: CreateMessageRequest): ChatMessage

    @DELETE("api/messaging/messages/{id}/")
    suspend fun deleteMessage(@Path("id") id: String)

    // ── Receipts ─────────────────────────────────────────────────────────────
    @GET("api/messaging/receipts/")
    suspend fun getReceipts(): List<MessageReceipt>

    @POST("api/messaging/receipts/")
    suspend fun createReceipt(@Body request: CreateMessageReceiptRequest): MessageReceipt

    // ── Sync ─────────────────────────────────────────────────────────────────
    @POST("api/messaging/messages/sync/")
    suspend fun syncMessages(@Body request: MessageSyncRequest): MessageSyncResponse
}
