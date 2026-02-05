package com.stankovic.zebrascanner.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {
    private val _scannedData = MutableStateFlow<String?>(null)
    val scannedData = _scannedData.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private val _totalScans = MutableStateFlow(0)
    val totalScans = _totalScans.asStateFlow()
    private val _successCount = MutableStateFlow(0)
    val successCount = _successCount.asStateFlow()
    private val _failureCount = MutableStateFlow(0)
    val failureCount = _failureCount.asStateFlow()
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history = _history.asStateFlow()

    fun updateScannedData(data: String) {
        _scannedData.value = data
    }

    fun updateStatus(text: String) {
        _status.value = text
    }

    fun incrementTotal() {
        _totalScans.value = _totalScans.value + 1
    }

    fun incrementSuccess() {
        _successCount.value = _successCount.value + 1
    }

    fun incrementFailure() {
        _failureCount.value = _failureCount.value + 1
    }

    fun addHistoryEntry(entry: String) {
        val newList = _history.value.toMutableList()
        newList.add(0, entry)
        if (newList.size > 50) newList.removeLast()
        _history.value = newList
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun setTotals(total: Int, success: Int, failure: Int) {
        _totalScans.value = total
        _successCount.value = success
        _failureCount.value = failure
    }
}
