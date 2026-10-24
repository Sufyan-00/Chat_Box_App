package com.example.chat_box_android.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores a mapping of conversationId → contact display name.
 * Populated when:
 *   - A chat request is accepted (we know sender/recipient username)
 *   - Conversations are fetched and the backend provides member usernames
 */
class ConversationNameCache(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("conv_name_cache", Context.MODE_PRIVATE)

    fun put(conversationId: String, name: String) {
        prefs.edit().putString(conversationId, name).apply()
    }

    fun get(conversationId: String): String? = prefs.getString(conversationId, null)

    fun getAll(): Map<String, String> =
        prefs.all.mapNotNull { (k, v) -> if (v is String) k to v else null }.toMap()

    fun remove(conversationId: String) {
        prefs.edit().remove(conversationId).apply()
    }
}
