package com.yourcompany.zebrascanapp.ui.dashboard.history
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.zebrascanapp.data.local.dao.ScanDao
import com.yourcompany.zebrascanapp.data.local.entity.ScanEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ScanHistoryViewModel @Inject constructor(
    scanDao: ScanDao
) : ViewModel() {

    val scans = scanDao.getAllScans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList<ScanEntity>())
}
