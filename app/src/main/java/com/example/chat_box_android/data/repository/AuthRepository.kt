package com.example.chat_box_android.data.repository

import android.os.Build
import com.example.chat_box_android.data.local.TokenManager
import com.example.chat_box_android.data.models.*
import com.example.chat_box_android.data.network.services.AuthApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    
    suspend fun register(username: String, email: String, password: String): Result<RegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(
                    RegisterRequest(username, email, password)
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun login(username: String, password: String): Result<TokenPairResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = tokenManager.getDeviceId()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                
                val response = apiService.login(
                    LoginRequest(username, password, deviceId, deviceName)
                )
                
                // Save tokens after successful login
                tokenManager.saveTokens(
                    response.access,
                    response.refresh,
                    response.is_email_verified
                )
                
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun refreshToken(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenManager.getRefreshToken()
                    ?: return@withContext Result.failure(Exception("No refresh token available"))
                
                val deviceId = tokenManager.getDeviceId()
                
                val response = apiService.refreshToken(
                    RefreshRequest(refreshToken, deviceId)
                )
                
                // Save new tokens
                val isEmailVerified = tokenManager.isEmailVerified()
                tokenManager.saveTokens(
                    response.access,
                    response.refresh,
                    isEmailVerified
                )
                
                Result.success(Unit)
            } catch (e: Exception) {
                // If refresh fails, clear tokens
                tokenManager.clearTokens()
                Result.failure(e)
            }
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenManager.getRefreshToken()
                val deviceId = tokenManager.getDeviceId()
                
                if (refreshToken != null) {
                    apiService.logout(LogoutRequest(refreshToken, deviceId))
                }
                
                // Clear local tokens regardless of API call success
                tokenManager.clearTokens()
                Result.success(Unit)
            } catch (e: Exception) {
                // Still clear tokens even if API call fails
                tokenManager.clearTokens()
                Result.success(Unit)
            }
        }
    }
    
    suspend fun checkVerificationStatus(): Result<VerificationStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getVerificationStatus()
                tokenManager.setEmailVerified(response.is_email_verified)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun sendVerificationOTP(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.sendVerificationOTP()
                Result.success(response.detail)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun verifyEmailOTP(otp: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyEmailOTP(VerifyEmailOTPRequest(otp))
                tokenManager.setEmailVerified(true)
                Result.success(response.detail)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun resendVerification(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.resendVerification()
                Result.success(response.detail)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun sendForgotPasswordOTP(email: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPasswordOTPSend(ForgotPasswordRequest(email))
                Result.success(response.detail)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun verifyForgotPasswordOTP(email: String, otp: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPasswordOTPVerify(
                    ForgotPasswordOTPVerifyRequest(email, otp)
                )
                Result.success(response.reset_session_token)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    suspend fun confirmPasswordReset(resetToken: String, newPassword: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPasswordOTPConfirm(
                    ForgotPasswordOTPConfirmRequest(resetToken, newPassword)
                )
                Result.success(response.detail)
            } catch (e: Exception) {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }
    
    fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }
    
    fun isEmailVerified(): Boolean {
        return tokenManager.isEmailVerified()
    }
    
    /**
     * Parse error message from HTTP exception response body
     * Handles multiple backend error formats:
     * - {"non_field_errors": ["error message"]}
     * - {"detail": "error message"}
     * - {"message": "error message"}
     * - {"field_name": ["error message"]}
     */
    private fun parseErrorMessage(exception: Exception): String {
        return when (exception) {
            is HttpException -> {
                try {
                    val errorBody = exception.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        val jsonObject = JSONObject(errorBody)
                        
                        // Try non_field_errors first (array format)
                        if (jsonObject.has("non_field_errors")) {
                            val errorsArray = jsonObject.getJSONArray("non_field_errors")
                            if (errorsArray.length() > 0) {
                                return errorsArray.getString(0)
                            }
                        }
                        
                        // Try detail field
                        val detail = jsonObject.optString("detail")
                        if (detail.isNotEmpty()) {
                            return detail
                        }
                        
                        // Try message field
                        val message = jsonObject.optString("message")
                        if (message.isNotEmpty()) {
                            return message
                        }
                        
                        // Try to find any field with an array of errors (like {"username": ["error"]})
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObject.opt(key)
                            if (value is org.json.JSONArray && value.length() > 0) {
                                return value.getString(0)
                            }
                        }
                        
                        // Fallback to HTTP message
                        exception.message()
                    } else {
                        exception.message()
                    }
                } catch (e: Exception) {
                    exception.message()
                }
            }
            else -> exception.message ?: "Unknown error occurred"
        }
    }
}
