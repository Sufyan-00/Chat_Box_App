package com.example.chat_box_android.di

import android.content.Context
import com.example.chat_box_android.data.local.PendingMessageQueue
import com.example.chat_box_android.data.local.TokenManager
import com.example.chat_box_android.data.network.RetrofitClient
import com.example.chat_box_android.data.network.WebSocketManager
import com.example.chat_box_android.data.network.services.MessagingApiService
import com.example.chat_box_android.data.repository.MessagingRepository

object Injection {

    @Volatile private var tokenManager: TokenManager? = null
    @Volatile private var webSocketManager: WebSocketManager? = null
    @Volatile private var messagingRepository: MessagingRepository? = null
    @Volatile private var nameCache: com.example.chat_box_android.data.local.ConversationNameCache? = null

    fun provideTokenManager(context: Context): TokenManager =
        tokenManager ?: synchronized(this) {
            tokenManager ?: TokenManager(context.applicationContext).also { tokenManager = it }
        }

    fun provideNameCache(context: Context) =
        nameCache ?: synchronized(this) {
            nameCache ?: com.example.chat_box_android.data.local.ConversationNameCache(context.applicationContext).also { nameCache = it }
        }

    fun provideWebSocketManager(context: Context): WebSocketManager =
        webSocketManager ?: synchronized(this) {
            webSocketManager ?: WebSocketManager(provideTokenManager(context)).also { webSocketManager = it }
        }

    fun provideMessagingRepository(context: Context): MessagingRepository =
        messagingRepository ?: synchronized(this) {
            messagingRepository ?: run {
                val tm = provideTokenManager(context)
                val retrofit = RetrofitClient.getInstance(tm)
                val apiService = retrofit.create(MessagingApiService::class.java)
                val wsMgr = provideWebSocketManager(context)
                val queue = PendingMessageQueue(context.applicationContext)
                val nc = provideNameCache(context)
                MessagingRepository(apiService, wsMgr, queue, nc).also { messagingRepository = it }
            }
        }

    /** Call on logout to reset cached singletons */
    fun reset() {
        webSocketManager?.disconnect()
        webSocketManager = null
        messagingRepository = null
        // Keep tokenManager — it handles its own clearTokens()
    }
}
