package com.stankovic.zebrascanner.data.local

import android.content.SharedPreferences
import javax.inject.Inject

/**
 * Data source for local preferences using SharedPreferences
 */
interface LocalDataSource {
    fun saveAuthToken(token: String)
    fun getAuthToken(): String?
    fun saveUserInfo(name: String, email: String)
    fun getUserName(): String?
    fun getUserEmail(): String?
    fun saveScanCount(total: Int, success: Int, failure: Int)
    fun getScanCount(): Triple<Int, Int, Int>
    fun incrementTotalScans(): Int
    fun incrementSuccessScans(): Int
    fun incrementFailureScans(): Int
    fun clearAll()
}

class SharedPreferencesDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : LocalDataSource {

    companion object {
        private const val PREFS_AUTH_TOKEN = "auth_token"
        private const val PREFS_USER_NAME = "user_full_name"
        private const val PREFS_USER_EMAIL = "user_email"
        private const val PREFS_TOTAL_SCANS = "total_scans"
        private const val PREFS_SUCCESS_SCANS = "success_scans"
        private const val PREFS_FAILURE_SCANS = "failure_scans"
    }

    override fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(PREFS_AUTH_TOKEN, token).apply()
    }

    override fun getAuthToken(): String? {
        return sharedPreferences.getString(PREFS_AUTH_TOKEN, null)
    }

    override fun saveUserInfo(name: String, email: String) {
        sharedPreferences.edit()
            .putString(PREFS_USER_NAME, name)
            .putString(PREFS_USER_EMAIL, email)
            .apply()
    }

    override fun getUserName(): String? {
        return sharedPreferences.getString(PREFS_USER_NAME, null)
    }

    override fun getUserEmail(): String? {
        return sharedPreferences.getString(PREFS_USER_EMAIL, null)
    }

    override fun saveScanCount(total: Int, success: Int, failure: Int) {
        sharedPreferences.edit()
            .putInt(PREFS_TOTAL_SCANS, total)
            .putInt(PREFS_SUCCESS_SCANS, success)
            .putInt(PREFS_FAILURE_SCANS, failure)
            .apply()
    }

    override fun getScanCount(): Triple<Int, Int, Int> {
        val total = sharedPreferences.getInt(PREFS_TOTAL_SCANS, 0)
        val success = sharedPreferences.getInt(PREFS_SUCCESS_SCANS, 0)
        val failure = sharedPreferences.getInt(PREFS_FAILURE_SCANS, 0)
        return Triple(total, success, failure)
    }

    override fun incrementTotalScans(): Int {
        val current = sharedPreferences.getInt(PREFS_TOTAL_SCANS, 0)
        val updated = current + 1
        sharedPreferences.edit().putInt(PREFS_TOTAL_SCANS, updated).apply()
        return updated
    }

    override fun incrementSuccessScans(): Int {
        val current = sharedPreferences.getInt(PREFS_SUCCESS_SCANS, 0)
        val updated = current + 1
        sharedPreferences.edit().putInt(PREFS_SUCCESS_SCANS, updated).apply()
        return updated
    }

    override fun incrementFailureScans(): Int {
        val current = sharedPreferences.getInt(PREFS_FAILURE_SCANS, 0)
        val updated = current + 1
        sharedPreferences.edit().putInt(PREFS_FAILURE_SCANS, updated).apply()
        return updated
    }

    override fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
