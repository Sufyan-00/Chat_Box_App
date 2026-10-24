package com.example.chat_box_android.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat_box_android.data.local.TokenManager
import com.example.chat_box_android.data.models.ChatUser
import com.example.chat_box_android.data.models.UpdateProfileRequest
import com.example.chat_box_android.data.repository.MessagingRepository
import com.example.chat_box_android.di.Injection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: ChatUser) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: MessagingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    val username: String = tokenManager.getUsername() ?: ""

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repository.getMyProfile().fold(
                onSuccess = { _uiState.value = ProfileUiState.Success(it) },
                onFailure = { _uiState.value = ProfileUiState.Error(it.message ?: "Failed to load") }
            )
        }
    }

    fun saveProfile(displayName: String, bio: String) {
        viewModelScope.launch {
            _isSaving.value = true
            repository.updateProfile(
                UpdateProfileRequest(
                    displayName = displayName.ifBlank { null },
                    bio = bio.ifBlank { null }
                )
            ).fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Success(it)
                    _toast.emit("Profile saved!")
                },
                onFailure = { _toast.emit("Save failed: ${it.message}") }
            )
            _isSaving.value = false
        }
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                repository = Injection.provideMessagingRepository(context),
                tokenManager = Injection.provideTokenManager(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
