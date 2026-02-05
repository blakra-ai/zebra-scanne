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



        // observe scanned data and show it
        lifecycleScope.launch {
            scannerViewModel.scannedData.collect { data ->
                binding.tvScanned.text = data ?: "No scans yet"
            }
        }
        // observe status and map to colors
        lifecycleScope.launch {
            scannerViewModel.status.collect { s ->
                if (s == null) {
                    binding.tvStatus.text = "Status: Idle"
                    binding.tvStatus.setTextColor(Color.DKGRAY)
                } else {
                    // status shape: "OK: message" | "Warning: message" | "Error: message"
                    val parts = s.split(":", limit = 2)
                    val kind = parts.getOrNull(0)?.trim() ?: ""
                    val msg = parts.getOrNull(1)?.trim() ?: s
                    when (kind) {
                        "OK" -> binding.tvStatus.setTextColor(Color.GREEN)
                        "Warning" -> binding.tvStatus.setTextColor(Color.YELLOW)
                        "Error" -> binding.tvStatus.setTextColor(Color.RED)
                        else -> binding.tvStatus.setTextColor(Color.DKGRAY)
                    }
                    binding.tvStatus.text = msg
                }
            }
        }

        // observe counters and history
        lifecycleScope.launch {
            scannerViewModel.totalScans.collect { total ->
                val success = scannerViewModel.successCount.value
                val fail = scannerViewModel.failureCount.value
                binding.tvCounts.text = "Scans: $total | Success: $success | Fail: $fail"
            }
        }

        lifecycleScope.launch {
            scannerViewModel.history.collect { list ->
                binding.tvHistory.text = list.joinToString(separator = "\n") { it }
            }
        }

        // populate saved user info
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val name = prefs.getString("user_full_name", "-") ?: "-"
        val email = prefs.getString("user_email", "-") ?: "-"
        binding.tvUserName.text = "User: $name"
        binding.tvUserEmail.text = "Email: $email"

        // initialize counters from prefs
        val total = prefs.getInt("total_scans", 0)
        val success = prefs.getInt("success_scans", 0)
        val failure = prefs.getInt("failure_scans", 0)
        scannerViewModel.setTotals(total, success, failure)

        binding.btnClearHistory.setOnClickListener {
            // clear viewmodel history and reset persisted counts
            scannerViewModel.clearHistory()
            scannerViewModel.setTotals(0, 0, 0)
            prefs.edit().putInt("total_scans", 0).putInt("success_scans", 0).putInt("failure_scans", 0).apply()
        }
    }

    private fun showStatus(text: String, color: Int) {
        binding.tvStatus.text = text
        binding.tvStatus.setTextColor(color)
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
