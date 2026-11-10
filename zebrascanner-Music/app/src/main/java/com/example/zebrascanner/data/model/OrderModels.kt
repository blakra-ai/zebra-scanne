package com.example.zebrascanner.data.model

import com.google.gson.annotations.SerializedName

// Request model
data class MarkShippedRequest(
    @SerializedName("tracking_number")
    val trackingNumber: String
)

// Success response model (200)
data class MarkShippedResponse(
    @SerializedName("data")
    val data: ShippedOrderData?
)

data class ShippedOrderData(
    @SerializedName("message")
    val message: String,
    @SerializedName("order_number")
    val orderNumber: String,
    @SerializedName("shipped_on")
    val shippedOn: String,
    @SerializedName("tracking_number")
    val trackingNumber: String
)

// Validation error response (422)
data class ValidationErrorResponse(
    @SerializedName("tracking_number")
    val trackingNumber: List<String>?
)

// Already shipped error response (409)
data class AlreadyShippedResponse(
    @SerializedName("data")
    val data: AlreadyShippedData?
)

data class AlreadyShippedData(
    @SerializedName("message")
    val message: String
)
