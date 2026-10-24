package com.example.chat_box_android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_box_android.data.local.TokenManager
import com.example.chat_box_android.data.network.RetrofitClient
import com.example.chat_box_android.data.network.services.AuthApiService
import com.example.chat_box_android.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tokenManager = TokenManager(application)
    private val authApiService = RetrofitClient.getInstance(tokenManager).create(AuthApiService::class.java)
    private val authRepository = AuthRepository(authApiService, tokenManager)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()
    
    init {
        // Check initial authentication and verification status
        if (isAuthenticated()) {
            _isEmailVerified.value = authRepository.isEmailVerified()
        }
    }
    
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Validate inputs
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("All fields are required")
                return@launch
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _authState.value = AuthState.Error("Invalid email address")
                return@launch
            }
            
            if (password.length < 8) {
                _authState.value = AuthState.Error("Password must be at least 8 characters")
                return@launch
            }
            
            val result = authRepository.register(username, email, password)
            
            result.onSuccess { response ->
                _isEmailVerified.value = response.is_email_verified
                
                // Auto-login to get auth tokens (needed for OTP endpoint)
                val loginResult = authRepository.login(username, password)
                loginResult.onSuccess {
                    // Now send OTP (requires auth token)
                    authRepository.sendVerificationOTP()
                    _authState.value = AuthState.RegisterSuccess(
                        "Account created! Please verify your email with the OTP sent."
                    )
                }.onFailure { loginError ->
                    _authState.value = AuthState.RegisterSuccess(
                        "Account created! Please log in to verify your email."
                    )
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(
                    error.message ?: "Registration failed"
                )
            }
        }
    }
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            if (username.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Username and password are required")
                return@launch
            }
            
            val result = authRepository.login(username, password)
            
            result.onSuccess { response ->
                _isEmailVerified.value = response.is_email_verified
                _authState.value = AuthState.LoginSuccess(
                    needsEmailVerification = !response.is_email_verified
                )
            }.onFailure { error ->
                _authState.value = AuthState.Error(
                    error.message ?: "Login failed. Please check your credentials."
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.logout()
            _isEmailVerified.value = false
            _authState.value = AuthState.LogoutSuccess
        }
    }
    
    fun sendVerificationOTP() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = authRepository.sendVerificationOTP()
            
            result.onSuccess { message ->
                _authState.value = AuthState.Success(message)
            }.onFailure { error ->
                _authState.value = AuthState.Error(
                    error.message ?: "Failed to send OTP"
                )
            }
        }
    }
    
    fun verifyOTP(otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            if (otp.length != 6) {
                _authState.value = AuthState.Error("OTP must be 6 digits")
                return@launch
            }
            
            val result = authRepository.verifyEmailOTP(otp)
            
            result.onSuccess { message ->
                _isEmailVerified.value = true
                _authState.value = AuthState.VerificationSuccess(message)
            }.onFailure { error ->
                _authState.value = AuthState.Error(
                    error.message ?: "Invalid OTP"
                )
            }
        }
    }
    
    fun checkVerificationStatus() {
        viewModelScope.launch {
            val result = authRepository.checkVerificationStatus()
            result.onSuccess { response ->
                _isEmailVerified.value = response.is_email_verified
            }
        }
    }
    
    fun refreshToken() {
        viewModelScope.launch {
            authRepository.refreshToken()
        }
    }
    
    fun isAuthenticated(): Boolean {
        return authRepository.isAuthenticated()
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
    
    fun sendForgotPasswordOTP(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _authState.value = AuthState.Error("Please enter a valid email address")
                return@launch
            }
            
            val result = authRepository.sendForgotPasswordOTP(email)
            _authState.value = when {
                result.isSuccess -> AuthState.ForgotPasswordOTPSent(
                    result.getOrNull() ?: "OTP sent to your email"
                )
                else -> AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send OTP"
                )
            }
        }
    }
    
    fun verifyForgotPasswordOTP(email: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            if (otp.isBlank()) {
                _authState.value = AuthState.Error("Please enter OTP")
                return@launch
            }
            
            val result = authRepository.verifyForgotPasswordOTP(email, otp)
            _authState.value = when {
                result.isSuccess -> AuthState.ForgotPasswordOTPVerified(
                    result.getOrNull() ?: ""
                )
                else -> AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Invalid OTP"
                )
            }
        }
    }
    
    fun confirmPasswordReset(resetToken: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            if (newPassword.isBlank()) {
                _authState.value = AuthState.Error("Password cannot be empty")
                return@launch
            }
            
            if (newPassword.length < 8) {
                _authState.value = AuthState.Error("Password must be at least 8 characters")
                return@launch
            }
            
            if (newPassword != confirmPassword) {
                _authState.value = AuthState.Error("Passwords do not match")
                return@launch
            }
            
            val result = authRepository.confirmPasswordReset(resetToken, newPassword)
            _authState.value = when {
                result.isSuccess -> AuthState.PasswordResetSuccess(
                    result.getOrNull() ?: "Password reset successful"
                )
                else -> AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to reset password"
                )
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class RegisterSuccess(val message: String) : AuthState()
    data class LoginSuccess(val needsEmailVerification: Boolean) : AuthState()
    object LogoutSuccess : AuthState()
    data class VerificationSuccess(val message: String) : AuthState()
    data class ForgotPasswordOTPSent(val message: String) : AuthState()
    data class ForgotPasswordOTPVerified(val resetToken: String) : AuthState()
    data class PasswordResetSuccess(val message: String) : AuthState()
}
