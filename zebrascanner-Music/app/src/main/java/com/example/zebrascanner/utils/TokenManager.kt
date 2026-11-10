package com.example.zebrascanner.utils

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREF_NAME = "zebra_scanner_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPreferences(context).edit().apply {
            putString(KEY_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearToken(context: Context) {
        getPreferences(context).edit().apply {
            remove(KEY_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun logout(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}
