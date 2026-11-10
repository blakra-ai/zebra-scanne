package com.stankovic.zebrascanner.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stankovic.zebrascanner.domain.usecase.ClearScanHistoryUseCase
import com.stankovic.zebrascanner.domain.usecase.GetScanStatsUseCase
import com.stankovic.zebrascanner.domain.usecase.GetUserInfoUseCase
import com.stankovic.zebrascanner.presentation.state.HistoryEntry
import com.stankovic.zebrascanner.presentation.state.ScannerUIState
import com.stankovic.zebrascanner.presentation.state.StatusType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Refactored ViewModel with clean architecture, using usecases and sealed states
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getScanStatsUseCase: GetScanStatsUseCase,
    private val clearScanHistoryUseCase: ClearScanHistoryUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<ScannerUIState>(ScannerUIState.Idle)
    val uiState: StateFlow<ScannerUIState> = _uiState.asStateFlow()

    // Scanned Data
    private val _scannedData = MutableStateFlow<String?>(null)
    val scannedData: StateFlow<String?> = _scannedData.asStateFlow()

    // Status (deprecated, use uiState instead)
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    // Counters
    private val _totalScans = MutableStateFlow(0)
    val totalScans: StateFlow<Int> = _totalScans.asStateFlow()

    private val _successCount = MutableStateFlow(0)
    val successCount: StateFlow<Int> = _successCount.asStateFlow()

    private val _failureCount = MutableStateFlow(0)
    val failureCount: StateFlow<Int> = _failureCount.asStateFlow()

    // History
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    // User Info
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    init {
        loadUserInfo()
        loadScanStats()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val userInfo = getUserInfoUseCase()
                _userName.value = userInfo.name
                _userEmail.value = userInfo.email
            } catch (e: Exception) {
                _userName.value = "Unknown"
                _userEmail.value = "Unknown"
            }
        }
    }

    private fun loadScanStats() {
        viewModelScope.launch {
            try {
                val stats = getScanStatsUseCase()
                _totalScans.value = stats.total
                _successCount.value = stats.success
                _failureCount.value = stats.failure
            } catch (e: Exception) {
                // Stats loading failed, use defaults
            }
        }
    }

    fun updateScannedData(data: String) {
        _scannedData.value = data
    }

    fun updateStatus(text: String) {
        _status.value = text
        // Parse status for new sealed state
        val parts = text.split(":", limit = 2)
        val type = parts.getOrNull(0)?.trim() ?: ""
        val message = parts.getOrNull(1)?.trim() ?: text
        
        _uiState.value = when (type) {
            "OK" -> ScannerUIState.Success(message)
            "Warning" -> ScannerUIState.Warning(message)
            "Error" -> ScannerUIState.Error(
                AppException.UnknownException(
                    Exception(message)
                )
            )
            else -> ScannerUIState.Idle
        }
    }

    fun addHistoryEntry(entry: HistoryEntry) {
        val newList = _history.value.toMutableList()
        newList.add(0, entry)
        if (newList.size > 50) newList.removeAt(newList.lastIndex)
        _history.value = newList
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

    fun clearHistory() {
        viewModelScope.launch {
            clearScanHistoryUseCase()
            _history.value = emptyList()
            _totalScans.value = 0
            _successCount.value = 0
            _failureCount.value = 0
        }
    }

    fun setTotals(total: Int, success: Int, failure: Int) {
        _totalScans.value = total
        _successCount.value = success
        _failureCount.value = failure
    }

    // For legacy compatibility
    fun getContext(): android.content.Context? = null
}
