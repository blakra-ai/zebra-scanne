package com.zebra.sloutions.solutionsappszebra.data.remote.models


data class ShipResponse(
    val data: ShipData
)

data class ShipData(
    val message: String,
    val order_number: String,
    val shipped_on: String,
    val tracking_number: String
)