package com.yourcompany.zebrascanapp.ui.dashboard.live
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.yourcompany.zebrascanapp.data.local.entity.ScanEntity
import com.yourcompany.zebrascanapp.data.local.dao.ScanDao
import com.yourcompany.zebrascanapp.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveScanViewModel @Inject constructor(
    private val scanRepository: ScanRepository) : ViewModel() {

    fun handleScan(barcode: String) {
        viewModelScope.launch {
            scanRepository.markShipped(barcode)
        }
    }

}
