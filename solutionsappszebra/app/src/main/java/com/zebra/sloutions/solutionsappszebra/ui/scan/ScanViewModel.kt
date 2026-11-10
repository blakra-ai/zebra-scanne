package com.zebra.sloutions.solutionsappszebra.ui.scan


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScannedItemEntity
import com.zebra.sloutions.solutionsappszebra.data.repository.ScannedItemRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: ScannedItemRepository
) : ViewModel() {

    val scannedItems: StateFlow<List<ScannedItemEntity>> =
        repository.getAllScannedItems()
            .map { it }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun scanBarcode(trackingNumber: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.scanAndMarkShipped(trackingNumber)
            if (result.isSuccess) {
                onResult(true, "Scanned & marked shipped")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}