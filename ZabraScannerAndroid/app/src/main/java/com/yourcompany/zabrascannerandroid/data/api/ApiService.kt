package com.yourcompany.zabrascannerandroid.data.api

import com.yourcompany.zabrascannerandroid.data.model.LoginRequest
import com.yourcompany.zabrascannerandroid.data.model.LoginResponse
import com.yourcompany.zabrascannerandroid.data.model.MarkShippedRequest
import com.yourcompany.zabrascannerandroid.data.model.MarkShippedResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/admin-portal/login-admin")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/admin-portal/orders/mark-shipped")
    suspend fun markShipped(@Body request: MarkShippedRequest): MarkShippedResponse
}
