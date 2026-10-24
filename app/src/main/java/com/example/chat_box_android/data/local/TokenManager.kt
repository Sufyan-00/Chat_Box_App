package com.example.chat_box_android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import org.json.JSONObject
import java.util.UUID

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_token_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }

    fun saveTokens(accessToken: String, refreshToken: String, isEmailVerified: Boolean = false) {
        val editor = sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putBoolean(KEY_IS_EMAIL_VERIFIED, isEmailVerified)
        // Parse JWT payload to extract user info
        parseAndStoreUserInfo(accessToken, editor)
        editor.apply()
    }

    /**
     * Decodes the JWT payload (base64url) and stores user_id + username.
     * JWT format: header.payload.signature — all base64url encoded.
     */
    private fun parseAndStoreUserInfo(jwt: String, editor: SharedPreferences.Editor) {
        try {
            val parts = jwt.split(".")
            if (parts.size < 2) return
            // Base64url → Base64 standard
            val payload = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let {
                    // Pad to multiple of 4
                    it + "=".repeat((4 - it.length % 4) % 4)
                }
            val json = JSONObject(String(Base64.decode(payload, Base64.DEFAULT)))
            json.optString("user_id").takeIf { it.isNotEmpty() }
                ?.let { editor.putString(KEY_USER_ID, it) }
            json.optString("username").takeIf { it.isNotEmpty() }
                ?.let { editor.putString(KEY_USERNAME, it) }
            // Also update email verification from token claims
            if (json.has("is_email_verified")) {
                editor.putBoolean(KEY_IS_EMAIL_VERIFIED, json.getBoolean("is_email_verified"))
            }
        } catch (_: Exception) {
            // Silently ignore parse errors; user info just won't be cached
        }
    }

    fun getAccessToken(): String? = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)
    fun getUsername(): String? = sharedPreferences.getString(KEY_USERNAME, null)

    fun getDeviceId(): String {
        var deviceId = sharedPreferences.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    fun isEmailVerified(): Boolean = sharedPreferences.getBoolean(KEY_IS_EMAIL_VERIFIED, false)

    fun setEmailVerified(verified: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_EMAIL_VERIFIED, verified).apply()
    }

    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_IS_EMAIL_VERIFIED)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .apply()
        // Note: We keep device_id even after logout for device tracking
    }

    fun isAuthenticated(): Boolean = getAccessToken() != null && getRefreshToken() != null
}
