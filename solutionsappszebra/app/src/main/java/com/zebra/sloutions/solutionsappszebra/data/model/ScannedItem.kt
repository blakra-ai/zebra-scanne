package com.zebra.sloutions.solutionsappszebra.data.model

data class ScannedItem(
    val trackingNumber: String,
    val orderNumber: String?,
    val shippedOn: String?,
    val status: ScanStatus
)

enum class ScanStatus {
    PENDING,
    SUCCESS,
    ERROR,
    ALREADY_SHIPPED
}
