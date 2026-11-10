package com.yourcompany.zebrascanapp.data.remote

import com.yourcompany.zebrascanapp.data.remote.model.LoginRequest
import com.yourcompany.zebrascanapp.data.remote.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApi {

    @POST("login-admin")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}