package com.yourcompany.zebrascanapp.data.repository


import com.yourcompany.zebrascanapp.data.remote.LoginApi
import com.yourcompany.zebrascanapp.data.remote.model.LoginRequest
import com.yourcompany.zebrascanapp.util.TokenManager
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val loginApi: LoginApi,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = loginApi.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!.token
                tokenManager.saveToken(token)
                Result.success(token)
            } else {
                Result.failure(Exception("Login failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}