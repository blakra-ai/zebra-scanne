package com.stankovic.zebrascanner.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)
data class User(
    @SerializedName("full_name") val fullName: String?,
    val email: String?
)
data class LoginResponse(
    val data: Data?
) {
    data class Data(
        val token: String?,
        val message: String? = null,
    )
}
data class ErrorResponse(
    val data: ErrorData?
)

data class ErrorData(
    val message: String?
)

data class MarkShippedRequest(
    @SerializedName("tracking_number") val trackingNumber: String
)

data class MarkShippedResponse(
    val data: Map<String, Any>?
)
