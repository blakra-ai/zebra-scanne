package com.example.zebrascanner.data.api

import com.example.zebrascanner.data.model.LoginRequest
import com.example.zebrascanner.data.model.LoginResponse
import com.example.zebrascanner.data.model.MarkShippedRequest
import com.example.zebrascanner.data.model.MarkShippedResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers(
        "accept: application/json, text/plain, */*",
        "content-type: application/json",
        "x-panel: true"
    )
    @POST("api/admin-portal/login-admin")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @Headers(
        "accept: application/json, text/plain, */*",
        "content-type: application/json",
        "x-panel: true"
    )
    @POST("api/admin-portal/orders/mark-shipped")
    suspend fun markOrderShipped(
        @Header("Authorization") authorization: String,
        @Body request: MarkShippedRequest
    ): Response<MarkShippedResponse>
}
