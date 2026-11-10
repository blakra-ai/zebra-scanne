package com.stankovic.zebrascanner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.stankovic.zebrascanner.config.ScanningConfig
import com.stankovic.zebrascanner.databinding.ActivityScannerBinding
import com.stankovic.zebrascanner.presentation.state.StatusType
import com.stankovic.zebrascanner.scanning.ScanBroadcastReceiver
import com.stankovic.zebrascanner.viewmodel.ScannerViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScannerActivity : ComponentActivity() {
    private val scannerViewModel: ScannerViewModel by viewModels()
    private lateinit var scannerReceiver: ScanBroadcastReceiver
    private var isReceiverRegistered = false
    private lateinit var binding: ActivityScannerBinding

    // Camera fallback for when the hardware scanner/DataWedge isn't available
    private val cameraScanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { submitTrackingNumber(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Scanner stays in use for a whole shift; don't let the screen sleep between parcels
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
        lifecycleScope.launch { scannerViewModel.successCount.collect { updateCounterDisplay() } }
        lifecycleScope.launch { scannerViewModel.failureCount.collect { updateCounterDisplay() } }

        // Observe history
        lifecycleScope.launch {
            scannerViewModel.history.collect { history ->
                binding.tvHistory.text = if (history.isEmpty()) "No scans yet"
                    else history.joinToString(separator = "\n\n") { it.toString() }
            }
        }

        // Populate user info

        // Clear history button
        binding.btnClearHistory.setOnClickListener {
            scannerViewModel.clearHistory()
        }

        binding.btnLogout.setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().remove("auth_token").apply()
            startActivity(
                Intent(this, login_form::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }

        binding.btnSubmitManual.setOnClickListener { submitManualEntry() }
        binding.etManual.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitManualEntry()
                true
            } else false
        }

        binding.btnCameraScan.setOnClickListener {
            if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)) {
                android.widget.Toast.makeText(this, "This device has no camera", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val options = ScanOptions()
                .setPrompt("Point the camera at the barcode")
                .setBeepEnabled(true)
                .setOrientationLocked(false)
            cameraScanLauncher.launch(options)
        }
    }

    private fun submitManualEntry() {
        val text = binding.etManual.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            binding.tilManual.error = "Enter a tracking number"
            return
        }
        binding.tilManual.error = null
        binding.etManual.text?.clear()
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(binding.etManual.windowToken, 0)
        submitTrackingNumber(text)
    }

    private fun submitTrackingNumber(trackingNumber: String) {
        scannerReceiver.submit(this, trackingNumber.trim())
    }

    private fun updateStatusUI(state: com.stankovic.zebrascanner.presentation.state.ScannerUIState) {
        // Big colour-coded card so the result is readable at arm's length
        val (background, text, title) = when (state.getStatusType()) {
            StatusType.IDLE -> Triple(Color.WHITE, Color.DKGRAY, "READY TO SCAN")
            StatusType.LOADING -> Triple(Color.parseColor("#1565C0"), Color.WHITE, "SENDING…")
            StatusType.SUCCESS -> Triple(Color.parseColor("#2E7D32"), Color.WHITE, "SHIPPED ✔")
            StatusType.WARNING -> Triple(Color.parseColor("#F9A825"), Color.BLACK, "CHECK ⚠")
            StatusType.ERROR -> Triple(Color.parseColor("#C62828"), Color.WHITE, "NOT MARKED ✖")
        }
        binding.cardStatus.setCardBackgroundColor(background)
        val detail = state.getDisplayMessage()
            ?.takeIf { state.getStatusType() != StatusType.IDLE }
        binding.tvStatus.text = if (detail != null) "$title\n$detail" else title
        binding.tvStatus.setTextColor(text)
        binding.tvCounts.setTextColor(text)
    }

    private fun updateCounterDisplay() {
        val total = scannerViewModel.totalScans.value
        val success = scannerViewModel.successCount.value
        val fail = scannerViewModel.failureCount.value
        binding.tvCounts.text = "Scans $total  •  Shipped $success  •  Failed $fail"
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
