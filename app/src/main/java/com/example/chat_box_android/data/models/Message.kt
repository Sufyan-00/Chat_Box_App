package com.example.chat_box_android.data.models

// Legacy sample data kept for reference only
// The real Message class is defined in MessagingModels.kt
object SampleMessages {
    fun getMessages(conversationId: String) = listOf(
        Message(
            id = "1",
            text = "Hello! How are you?",
            isFromMe = true,
            timestamp = "09:25 AM"
        ),
        Message(
            id = "2",
            text = "I'm good, thanks!",
            isFromMe = false,
            timestamp = "09:26 AM"
        )
    )
}
