package com.stankovic.zebrascanner.presentation.state

import AppException


/**
 * Sealed class for scanner UI state management using MVVM pattern
 */
sealed class ScannerUIState {
    data object Idle : ScannerUIState()
    data object Loading : ScannerUIState()
    data class Success(val message: String) : ScannerUIState()
    data class Warning(val message: String) : ScannerUIState()
    data class Error(val exception: AppException) : ScannerUIState()

    fun getDisplayMessage(): String? = when (this) {
        is Idle -> "Status: Idle"
        is Loading -> "Processing..."
        is Success -> message
        is Warning -> message
        is Error -> exception.message
    }

    fun getStatusType(): StatusType = when (this) {
        is Idle -> StatusType.IDLE
        is Loading -> StatusType.LOADING
        is Success -> StatusType.SUCCESS
        is Warning -> StatusType.WARNING
        is Error -> StatusType.ERROR
    }
}

enum class StatusType {
    IDLE, LOADING, SUCCESS, WARNING, ERROR
}

/**
 * Sealed class for scan event results
 */
sealed class ScanResult {
    data class OrderShipped(val trackingNumber: String, val message: String) : ScanResult()
    data class ValidationError(val trackingNumber: String, val message: String) : ScanResult()
    data class AlreadyShipped(val trackingNumber: String, val message: String) : ScanResult()
    data class NetworkError(val message: String) : ScanResult()
}

/**
 * Data class for history entry with timestamp and result
 */
data class HistoryEntry(
    val trackingNumber: String,
    val status: StatusType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun toString(): String = "$trackingNumber — ${status.name}: $message"
}
