package com.yourcompany.zabrascannerandroid.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("token")
    val token: String?,
    @SerializedName("data")
    val data: LoginData?
)

data class LoginData(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("username")
    val username: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("role")
    val role: String?,
    @SerializedName("token")
    val token: String?
)
