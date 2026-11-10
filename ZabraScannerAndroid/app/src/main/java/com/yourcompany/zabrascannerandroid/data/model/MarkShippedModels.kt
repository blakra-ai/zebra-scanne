package com.yourcompany.zabrascannerandroid.data.model

import com.google.gson.annotations.SerializedName

data class MarkShippedRequest(
    @SerializedName("tracking_number")
    val trackingNumber: String
)

data class MarkShippedResponse(
    @SerializedName("data")
    val data: MarkShippedData?
)

data class MarkShippedData(
    @SerializedName("message")
    val message: String?,
    @SerializedName("order_number")
    val orderNumber: String?,
    @SerializedName("shipped_on")
    val shippedOn: String?,
    @SerializedName("tracking_number")
    val trackingNumber: String?
)
