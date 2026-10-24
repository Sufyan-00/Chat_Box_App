package com.example.chat_box_android.data.models

import com.google.gson.annotations.SerializedName

// ============= Request Models =============

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String,
    val device_id: String,
    val device_name: String? = null
)

data class RefreshRequest(
    val refresh: String,
    val device_id: String
)

data class LogoutRequest(
    val refresh: String,
    val device_id: String? = null
)

data class VerifyEmailOTPRequest(
    val otp: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val uid: String,
    val token: String,
    val new_password: String
)

data class ForgotPasswordOTPVerifyRequest(
    val email: String,
    val otp: String
)

data class ForgotPasswordOTPConfirmRequest(
    val reset_session_token: String,
    val new_password: String
)

// ============= Response Models =============

data class RegisterResponse(
    val id: String,
    val username: String,
    val email: String,
    val is_email_verified: Boolean
)

data class TokenPairResponse(
    val access: String,
    val refresh: String,
    val is_email_verified: Boolean
)

data class RefreshResponse(
    val access: String,
    val refresh: String
)

data class VerificationStatusResponse(
    val email: String,
    val is_email_verified: Boolean
)

data class ForgotPasswordOTPVerifyResponse(
    val reset_session_token: String
)

data class SimpleDetailResponse(
    val detail: String
)

// ============= User Profile Model =============

data class UserProfile(
    val username: String,
    val email: String,
    val display_name: String? = null,
    val bio: String? = null,
    val updated_at: String? = null
)
