package com.example.chat_box_android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_box_android.data.local.ConversationNameCache
import com.example.chat_box_android.data.local.TokenManager
import com.example.chat_box_android.data.models.*
import com.example.chat_box_android.data.repository.MessagingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val conversations: List<ChatConversation>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class RequestsUiState(
    val incoming: List<ChatRequest> = emptyList(),
    val outgoing: List<ChatRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val repository: MessagingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val myUserId: String = tokenManager.getUserId() ?: ""
    private val myUsername: String = tokenManager.getUsername() ?: ""
    private val nameCache: ConversationNameCache = repository.nameCache

    // ── Conversations ──────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ── User Search ────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val searchResults: StateFlow<List<UserSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // ── Chat Requests ──────────────────────────────────────────────────────────
    private val _requestsState = MutableStateFlow(RequestsUiState())
    val requestsState: StateFlow<RequestsUiState> = _requestsState.asStateFlow()

    private val _incomingCount = MutableStateFlow(0)
    val incomingCount: StateFlow<Int> = _incomingCount.asStateFlow()

    // ── Toast & Refresh ────────────────────────────────────────────────────────
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchConversations()
        observeWsEvents()
    }

    private fun observeWsEvents() {
        viewModelScope.launch {
            repository.wsMgr.events.collect { event ->
                // When we receive a new message anywhere or accept requests,
                // re-fetch conversations to update the "Latest Message" snippet and sorting.
                when (event) {
                    is com.example.chat_box_android.data.network.WsEvent.MessageNew,
                    is com.example.chat_box_android.data.network.WsEvent.MessageAck -> {
                        fetchConversations()
                    }
                    else -> Unit
                }
            }
        }
    }

    // ── Conversations ──────────────────────────────────────────────────────────

    fun fetchConversations(isSwipeRefresh: Boolean = false) {
        viewModelScope.launch {
            if (_uiState.value !is HomeUiState.Success && !isSwipeRefresh) {
                _uiState.value = HomeUiState.Loading
            }
            if (isSwipeRefresh) _isRefreshing.value = true
            
            repository.getConversations().fold(
                onSuccess = { apiConversations ->
                    // Resolve names & last messages concurrently per conversation
                    val list = apiConversations.map { conv ->
                        async { buildChatConversation(conv) }
                    }.awaitAll()

                    // Sort by last activity (updatedAt descending)
                    _uiState.value = HomeUiState.Success(
                        list.sortedByDescending { it.rawUpdatedAt }
                    )
                    _isRefreshing.value = false
                },
                onFailure = { err ->
                    _uiState.value = HomeUiState.Error(err.message ?: "Failed to load conversations")
                    _isRefreshing.value = false
                }
            )
            loadIncomingCount()
        }
    }

    private suspend fun buildChatConversation(c: Conversation): ChatConversation {
        val displayName = resolveConversationName(c)
        // Store in local cache so ChatScreen can pick it up
        nameCache.put(c.id, displayName)

        // Last message: use embedded last_message if backend provides it, else fetch
        val lastMsg = c.lastMessage ?: fetchLastMessage(c.id)

        return ChatConversation(
            id = c.id,
            name = displayName,
            lastMessage = lastMsg?.ciphertext ?: "Tap to start chatting",
            timestamp = lastMsg?.createdAt?.toSmartTime() ?: c.updatedAt.toSmartTime(),
            rawUpdatedAt = lastMsg?.createdAt ?: c.updatedAt,
            unreadCount = 0,
            isOnline = false,
            memberIds = c.memberIds ?: emptyList()
        )
    }

    private suspend fun fetchLastMessage(conversationId: String): ChatMessage? =
        repository.getMessages(conversationId, limit = 1)
            .getOrNull()
            ?.firstOrNull()

    /**
     * Priority for display name:
     * 1. Non-empty title from backend
     * 2. Other member's displayName (from embedded members list)
     * 3. Cached name from prior lookups
     * 4. Other member's username from embedded members list
     * 5. Friendly fallback
     */
    private suspend fun resolveConversationName(c: Conversation): String {
        if (c.title.isNotBlank()) return c.title

        // First check memory cache, as it may have been populated by 'acceptRequest'
        val cached = nameCache.get(c.id)
        if (!cached.isNullOrBlank()) return cached

        // Then check if backend provided member objects
        val members = c.members
        if (!members.isNullOrEmpty()) {
            val other = members.firstOrNull { it.id != myUserId }
            if (other != null) {
                val name = other.displayName.ifBlank { other.username }
                nameCache.put(c.id, name)
                return name
            }
        }

        // Fallback — we don't have names yet, but we shouldn't show raw UUIDs.
        val memberIds = c.memberIds ?: emptyList()
        return if (!c.isGroup && memberIds.size == 2) "Direct Message"
        else if (c.isGroup) "Group Chat"
        else "Conversation"
    }

    fun getConversationName(id: String): String = nameCache.get(id) ?: "Chat"

    fun createConversation(title: String, memberIds: List<String>) {
        viewModelScope.launch {
            repository.createConversation(
                CreateConversationRequest(
                    title = title.ifBlank { null },
                    isGroup = memberIds.size > 1,
                    memberIds = memberIds
                )
            ).fold(
                onSuccess = { fetchConversations() },
                onFailure = { _toastMessage.emit("Failed to create: ${it.message}") }
            )
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id).fold(
                onSuccess = {
                    nameCache.remove(id)
                    fetchConversations()
                },
                onFailure = { _toastMessage.emit("Failed to delete: ${it.message}") }
            )
        }
    }

    // ── User Search ────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            repository.searchUsers(query).fold(
                onSuccess = { _searchResults.value = it.filter { r -> r.id != myUserId && r.username != myUsername } },
                onFailure = { _searchResults.value = emptyList() }
            )
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // ── Chat Requests ──────────────────────────────────────────────────────────

    fun sendChatRequest(recipientId: String, recipientUsername: String) {
        viewModelScope.launch {
            val body = CreateChatRequestBody(
                recipientId = recipientId,
                initialClientMessageId = java.util.UUID.randomUUID().toString(),
                // The backend requires initial_ciphertext to be non-blank.
                initialCiphertext = "Hello!"
            )
            repository.sendChatRequest(body).fold(
                onSuccess = {
                    _toastMessage.emit("Request sent to $recipientUsername")
                    clearSearch()
                },
                onFailure = { _toastMessage.emit("Request failed: ${it.message}") }
            )
        }
    }

    fun loadChatRequests() {
        viewModelScope.launch {
            _requestsState.value = _requestsState.value.copy(isLoading = true, error = null)
            val incoming = repository.getIncomingRequests().getOrElse { emptyList() }
            val outgoing = repository.getOutgoingRequests().getOrElse { emptyList() }
            _requestsState.value = RequestsUiState(incoming = incoming, outgoing = outgoing, isLoading = false)
            _incomingCount.value = incoming.count { it.status == "pending" }
        }
    }

    fun acceptRequest(requestId: String, senderUsername: String) {
        viewModelScope.launch {
            repository.acceptRequest(requestId).fold(
                onSuccess = { resp ->
                    // Cache the sender's name against the new conversation
                    nameCache.put(resp.conversationId, senderUsername)
                    repository.wsMgr.subscribeToConversation(resp.conversationId)
                    _toastMessage.emit("Chat accepted!")
                    loadChatRequests()
                    fetchConversations()
                },
                onFailure = { _toastMessage.emit("Failed: ${it.message}") }
            )
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectRequest(requestId).fold(
                onSuccess = { _toastMessage.emit("Request rejected"); loadChatRequests() },
                onFailure = { _toastMessage.emit("Failed: ${it.message}") }
            )
        }
    }

    private suspend fun loadIncomingCount() {
        _incomingCount.value = repository.getIncomingRequests()
            .getOrElse { emptyList() }
            .count { it.status == "pending" }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun String.toSmartTime(): String = try {
    val ymd = take(10) // "YYYY-MM-DD"
    val todayYmd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    if (ymd == todayYmd) {
        val timePart = drop(11).take(5) // "HH:mm"
        timePart
    } else {
        // Parse as "dd MMM"
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(ymd) ?: return this
        val diff = (Date().time - date.time) / (1000 * 60 * 60 * 24)
        if (diff < 7) SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
} catch (_: Exception) { this }
