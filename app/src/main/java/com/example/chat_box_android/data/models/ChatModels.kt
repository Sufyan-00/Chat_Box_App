package com.example.chat_box_android.data.models

data class ChatConversation(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val lastMessage: String,
    val timestamp: String,
    val rawUpdatedAt: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    var isMuted: Boolean = false,
    val memberIds: List<String> = emptyList()
)

// Sample data for testing
object SampleData {
    val conversations = listOf(
        ChatConversation(
            id = "1",
            name = "Alex Linderson",
            lastMessage = "How are you today?",
            timestamp = "2 min ago",
            unreadCount = 3,
            isOnline = true
        ),
        ChatConversation(
            id = "2",
            name = "Team Align",
            lastMessage = "Don't miss to attend the meeting.",
            timestamp = "2 min ago",
            unreadCount = 4,
            isOnline = false
        ),
        ChatConversation(
            id = "3",
            name = "John Ahraham",
            lastMessage = "Hey! Can you join the meeting?",
            timestamp = "2 min ago",
            unreadCount = 0,
            isOnline = true
        ),
        ChatConversation(
            id = "4",
            name = "Sabila Sayma",
            lastMessage = "How are you today?",
            timestamp = "2 min ago",
            unreadCount = 0,
            isOnline = false
        ),
        ChatConversation(
            id = "5",
            name = "John Borino",
            lastMessage = "Have a good day 🎉",
            timestamp = "2 min ago",
            unreadCount = 0,
            isOnline = true
        )
    )
    
    val statusUsers = listOf(
        StatusUser("0", "My status", null, true),
        StatusUser("1", "Adil", null, false),
        StatusUser("2", "Marina", null, false),
        StatusUser("3", "Dean", null, false),
        StatusUser("4", "Max", null, false)
    )
}

data class StatusUser(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val isMyStatus: Boolean = false
)
