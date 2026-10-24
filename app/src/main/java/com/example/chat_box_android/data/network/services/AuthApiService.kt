package com.example.chat_box_android.data.network.services

import com.example.chat_box_android.data.models.*
import retrofit2.http.*

interface AuthApiService {
    
    @POST("api/auth/register/")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
    
    @POST("api/auth/login/")
    suspend fun login(@Body request: LoginRequest): TokenPairResponse
    
    @POST("api/auth/refresh/")
    suspend fun refreshToken(@Body request: RefreshRequest): RefreshResponse
    
    @POST("api/auth/logout/")
    suspend fun logout(@Body request: LogoutRequest)
    
    @GET("api/auth/verification-status/")
    suspend fun getVerificationStatus(): VerificationStatusResponse
    
    @POST("api/auth/verify-email-otp/send/")
    suspend fun sendVerificationOTP(): SimpleDetailResponse
    
    @POST("api/auth/verify-email-otp/confirm/")
    suspend fun verifyEmailOTP(@Body request: VerifyEmailOTPRequest): SimpleDetailResponse
    
    @POST("api/auth/resend-verification/")
    suspend fun resendVerification(): SimpleDetailResponse
    
    @POST("api/auth/forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): SimpleDetailResponse
    
    @POST("api/auth/reset-password/")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): SimpleDetailResponse
    
    @POST("api/auth/forgot-password-otp/send/")
    suspend fun forgotPasswordOTPSend(@Body request: ForgotPasswordRequest): SimpleDetailResponse
    
    @POST("api/auth/forgot-password-otp/verify/")
    suspend fun forgotPasswordOTPVerify(@Body request: ForgotPasswordOTPVerifyRequest): ForgotPasswordOTPVerifyResponse
    
    @POST("api/auth/forgot-password-otp/confirm/")
    suspend fun forgotPasswordOTPConfirm(@Body request: ForgotPasswordOTPConfirmRequest): SimpleDetailResponse
}
