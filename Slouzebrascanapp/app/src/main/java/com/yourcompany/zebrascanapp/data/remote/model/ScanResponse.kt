package com.yourcompany.zebrascanapp.data.remote.model


data class ScanResponse(
    val data: ScanData
)

data class ScanData(
    val message: String,
    val order_number: String,
    val shipped_on: String,
    val tracking_number: String
)