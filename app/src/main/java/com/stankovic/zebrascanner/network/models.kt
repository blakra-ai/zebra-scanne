package com.stankovic.zebrascanner.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val user: Map<String, Any>?,
    val token: String?,
    val message: String?,
    @SerializedName("token_key") val tokenKey: String?
)

data class MarkShippedRequest(
    @SerializedName("tracking_number") val trackingNumber: String
)

data class MarkShippedResponse(
    val data: Map<String, Any>?
)
