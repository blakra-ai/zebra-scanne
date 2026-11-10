package com.example.zebrascanner.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("data")
    val data: LoginData?
)

data class LoginData(
    @SerializedName("token")
    val token: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("token_key")
    val tokenKey: String
)

data class ErrorResponse(
    @SerializedName("message")
    val message: String?,
    @SerializedName("error")
    val error: String?
)
