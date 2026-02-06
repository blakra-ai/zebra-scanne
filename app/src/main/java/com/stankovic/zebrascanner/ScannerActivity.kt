package com.stankovic.zebrascanner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.stankovic.zebrascanner.config.ScanningConfig
import com.stankovic.zebrascanner.databinding.ActivityScannerBinding
import com.stankovic.zebrascanner.presentation.state.StatusType
import com.stankovic.zebrascanner.scanning.ScanBroadcastReceiver
import com.stankovic.zebrascanner.viewmodel.ScannerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScannerActivity : ComponentActivity() {
    private val scannerViewModel: ScannerViewModel by viewModels()
    private lateinit var scannerReceiver: ScanBroadcastReceiver
    private var isReceiverRegistered = false
    private lateinit var binding: ActivityScannerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scannerReceiver = ScanBroadcastReceiver(scannerViewModel)

        setupUI()
    }

    private fun setupUI() {
        // Observe scanned data
        lifecycleScope.launch {
            scannerViewModel.scannedData.collect { data ->
                binding.tvScanned.text = data ?: "No scans yet"
            }
        }

        // Observe UI state with proper color coding
        lifecycleScope.launch {
            scannerViewModel.uiState.collect { state ->
                updateStatusUI(state)
            }
        }

        // Observe counters
        lifecycleScope.launch {
            scannerViewModel.totalScans.collect { total ->
                updateCounterDisplay()
            }
        }

        // Observe history
        lifecycleScope.launch {
            scannerViewModel.history.collect { history ->
                binding.tvHistory.text = history.joinToString(separator = "\n") { it.toString() }
            }
        }

        // Populate user info

        // Clear history button
        binding.btnClearHistory.setOnClickListener {
            scannerViewModel.clearHistory()
        }
    }

    private fun updateStatusUI(state: com.stankovic.zebrascanner.presentation.state.ScannerUIState) {
        binding.tvStatus.text = "Status: ${state.getDisplayMessage()}"
        
        val color = when (state.getStatusType()) {
            StatusType.IDLE -> Color.DKGRAY
            StatusType.LOADING -> Color.BLUE
            StatusType.SUCCESS -> Color.GREEN
            StatusType.WARNING -> Color.YELLOW
            StatusType.ERROR -> Color.RED
        }
        binding.tvStatus.setTextColor(color)
    }

    private fun updateCounterDisplay() {
        val total = scannerViewModel.totalScans.value
        val success = scannerViewModel.successCount.value
        val fail = scannerViewModel.failureCount.value
        binding.tvCounts.text = "Scans: $total | Success: $success | Fail: $fail"
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerScannerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter()
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            filter.addAction(ScanningConfig.APP_SCANNER_INTENT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(scannerReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(scannerReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    private fun unregisterScannerReceiver() {
        if (isReceiverRegistered) {
            unregisterReceiver(scannerReceiver)
            isReceiverRegistered = false
        }
    }

    override fun onResume() {
        super.onResume()
        registerScannerReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterScannerReceiver()
    }
}
