package com.example.chat_box_android.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persists unsent messages across app restarts using SharedPreferences + Gson.
 * Each entry is keyed by client_message_id so retries are idempotent.
 */
class PendingMessageQueue(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pending_message_queue", Context.MODE_PRIVATE)
    private val gson = Gson()

    data class PendingEntry(
        val clientMessageId: String,
        val conversationId: String,
        val ciphertext: String,
        val enqueuedAt: Long = System.currentTimeMillis()
    )

    private fun loadAll(): MutableMap<String, PendingEntry> {
        val json = prefs.getString("queue_map", null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, PendingEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAll(map: Map<String, PendingEntry>) {
        prefs.edit().putString("queue_map", gson.toJson(map)).apply()
    }

    fun enqueue(entry: PendingEntry) {
        val map = loadAll()
        map[entry.clientMessageId] = entry
        saveAll(map)
    }

    fun markSent(clientMessageId: String) {
        val map = loadAll()
        map.remove(clientMessageId)
        saveAll(map)
    }

    fun getAll(): List<PendingEntry> = loadAll().values.toList()

    fun isEmpty(): Boolean = loadAll().isEmpty()

    fun clear() {
        prefs.edit().remove("queue_map").apply()
    }
}
