package com.stankovic.zebrascanner.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/api/admin-portal/login-admin")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("/api/admin-portal/orders/mark-shipped")
    suspend fun markShipped(@Header("Authorization") auth: String?, @Body body: MarkShippedRequest): Response<MarkShippedResponse>
}
