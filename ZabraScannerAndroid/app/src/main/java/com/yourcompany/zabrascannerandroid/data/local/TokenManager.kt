package com.yourcompany.zabrascannerandroid.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "zabra_scanner_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserInfo(userId: Int?, username: String?, email: String?, role: String?) {
        prefs.edit().apply {
            userId?.let { putInt(KEY_USER_ID, it) }
            username?.let { putString(KEY_USERNAME, it) }
            email?.let { putString(KEY_EMAIL, it) }
            role?.let { putString(KEY_ROLE, it) }
            apply()
        }
    }

    fun getUserId(): Int? {
        val userId = prefs.getInt(KEY_USER_ID, -1)
        return if (userId == -1) null else userId
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun getRole(): String? {
        return prefs.getString(KEY_ROLE, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
