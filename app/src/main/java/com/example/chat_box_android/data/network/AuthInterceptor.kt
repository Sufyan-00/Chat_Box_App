package com.example.chat_box_android.data.network

import android.util.Log
import com.example.chat_box_android.data.local.TokenManager
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Intercepts all requests:
 * 1. Attaches the access token to protected endpoints.
 * 2. On 401 → tries a token refresh automatically (once) and retries the original request.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private val SKIP_ENDPOINTS = listOf(
            "/api/auth/register/",
            "/api/auth/login/",
            "/api/auth/refresh/",
            "/api/auth/verify-email/",
            "/api/auth/forgot-password/",
            "/api/auth/reset-password/",
            "/api/auth/forgot-password-otp/"
        )
    }

    private val gson = Gson()

    // Separate lightweight OkHttpClient for the refresh call (avoids infinite loop)
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Skip auth header for public endpoints
        if (SKIP_ENDPOINTS.any { url.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Attach current access token
        val response = chain.proceed(originalRequest.withBearer(tokenManager.getAccessToken()))

        // Not a 401 — return as-is
        if (response.code != 401) return response

        // 401 — attempt token refresh
        Log.d(TAG, "Access token expired, attempting refresh")
        response.close()

        synchronized(this) {
            // Double-check — another thread may have already refreshed
            val freshToken = tokenManager.getAccessToken()
            if (freshToken != null && freshToken != originalRequest.header("Authorization")?.removePrefix("Bearer ")) {
                // Token was refreshed by another thread, just retry
                return chain.proceed(originalRequest.withBearer(freshToken))
            }

            val refreshToken = tokenManager.getRefreshToken() ?: run {
                Log.w(TAG, "No refresh token available")
                return chain.proceed(originalRequest) // Will 401 again, caller handles it
            }

            val refreshed = tryRefresh(refreshToken)
            if (refreshed != null) {
                tokenManager.saveTokens(
                    accessToken = refreshed.access,
                    refreshToken = refreshed.refresh ?: refreshToken,
                    isEmailVerified = tokenManager.isEmailVerified()
                )
                Log.d(TAG, "Token refreshed successfully, retrying original request")
                return chain.proceed(originalRequest.withBearer(refreshed.access))
            } else {
                Log.w(TAG, "Token refresh failed, clearing session")
                tokenManager.clearTokens()
                return chain.proceed(originalRequest)
            }
        }
    }

    private data class RefreshBody(val refresh: String)
    private data class RefreshedTokens(val access: String, val refresh: String? = null)

    private fun tryRefresh(refreshToken: String): RefreshedTokens? {
        return try {
            val body = gson.toJson(RefreshBody(refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}api/auth/refresh/")
                .post(body)
                .build()
            val response = refreshClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return null
                gson.fromJson(json, RefreshedTokens::class.java)
            } else {
                Log.w(TAG, "Refresh endpoint returned ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh request failed: ${e.message}")
            null
        }
    }

    private fun Request.withBearer(token: String?): Request {
        if (token == null) return this
        return newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}
