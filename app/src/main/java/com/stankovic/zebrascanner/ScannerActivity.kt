package com.stankovic.zebrascanner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.stankovic.zebrascanner.config.AppLanguage
import com.stankovic.zebrascanner.config.ScanningConfig
import com.stankovic.zebrascanner.databinding.ActivityScannerBinding
import com.stankovic.zebrascanner.presentation.state.HistoryEntry
import com.stankovic.zebrascanner.presentation.state.ScannerUIState
import com.stankovic.zebrascanner.presentation.state.StatusType
import com.stankovic.zebrascanner.scanning.DataWedgeMonitor
import com.stankovic.zebrascanner.scanning.ScanBroadcastReceiver
import com.stankovic.zebrascanner.viewmodel.ScannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// AppCompatActivity so an in-app language change recreates this screen on every Android version
@AndroidEntryPoint
class ScannerActivity : AppCompatActivity() {
    private val scannerViewModel: ScannerViewModel by viewModels()
    private lateinit var scannerReceiver: ScanBroadcastReceiver
    private var isReceiverRegistered = false
    private lateinit var binding: ActivityScannerBinding
    private lateinit var dataWedgeMonitor: DataWedgeMonitor
    private var lastScannerHealth: DataWedgeMonitor.ScannerHealth? = null

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
        dataWedgeMonitor = DataWedgeMonitor(this, ScanningConfig.DATAWEDGE_PROFILE, ::showScannerHealth)
        scannerReceiver.onHardwareScan = { runOnUiThread { dataWedgeMonitor.onScanReceived() } }
        binding.tvScannerHealth.setOnClickListener { onScannerHealthTapped() }

        setupUI()
    }

    private fun setupUI() {
        // Non-production builds get an orange bar so nobody marks real orders against a test server
        if (BuildConfig.ENVIRONMENT != "prod") {
            binding.topBar.setBackgroundColor(Color.parseColor("#E65100"))
            window.statusBarColor = Color.parseColor("#BF360C")
        }
        binding.btnLanguage.text = AppLanguage.current(this)
        binding.btnLanguage.setOnClickListener { AppLanguage.showPicker(this) }

        lifecycleScope.launch {
            scannerViewModel.scannedData.collect { data -> binding.tvScanned.text = data ?: "—" }
        }
        lifecycleScope.launch {
            scannerViewModel.uiState.collect { state -> updateStatusUI(state) }
        }
        lifecycleScope.launch { scannerViewModel.totalScans.collect { updateCounterDisplay() } }
        lifecycleScope.launch { scannerViewModel.successCount.collect { updateCounterDisplay() } }
        lifecycleScope.launch { scannerViewModel.failureCount.collect { updateCounterDisplay() } }
        lifecycleScope.launch {
            scannerViewModel.history.collect { history ->
                binding.tvHistory.text = if (history.isEmpty()) getString(R.string.no_scans)
                    else history.joinToString(separator = "\n\n") { formatHistory(it) }
            }
        }

        binding.btnClearHistory.setOnClickListener { scannerViewModel.clearHistory() }

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
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(this, R.string.no_camera, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val options = ScanOptions()
                .setPrompt(getString(R.string.camera_prompt))
                .setBeepEnabled(true)
                .setOrientationLocked(false)
            cameraScanLauncher.launch(options)
        }
    }

    private fun formatHistory(entry: HistoryEntry): String {
        val icon = when (entry.status) {
            StatusType.SUCCESS -> "✔"
            StatusType.WARNING -> "⚠"
            else -> "✖"
        }
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestamp))
        val order = entry.orderNumber?.let { "  •  " + getString(R.string.order_label, it) } ?: ""
        return "$icon $time  ${entry.trackingNumber}$order\n    ${entry.message}"
    }

    private fun submitManualEntry() {
        val text = binding.etManual.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            binding.tilManual.error = getString(R.string.enter_tracking)
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

    private fun updateStatusUI(state: ScannerUIState) {
        // Big colour-coded card so the result is readable at arm's length
        val type = state.getStatusType()
        val (background, text, title) = when (type) {
            StatusType.IDLE -> Triple(Color.WHITE, getColor(R.color.text_primary), R.string.status_ready)
            StatusType.LOADING -> Triple(Color.parseColor(BLUE), Color.WHITE, R.string.status_sending)
            StatusType.SUCCESS -> Triple(Color.parseColor(GREEN), Color.WHITE, R.string.status_shipped)
            StatusType.WARNING -> Triple(Color.parseColor(AMBER), Color.BLACK, R.string.status_check)
            StatusType.ERROR -> Triple(Color.parseColor(RED), Color.WHITE, R.string.status_not_marked)
        }
        binding.cardStatus.setCardBackgroundColor(background)
        binding.progressSending.visibility = if (type == StatusType.LOADING) View.VISIBLE else View.GONE
        val detail = state.getDisplayMessage()
            ?.takeIf { type != StatusType.IDLE && type != StatusType.LOADING }
        val titleText = getString(title)
        binding.tvStatus.text = if (detail != null) "$titleText\n$detail" else titleText
        binding.tvStatus.setTextColor(text)
        binding.tvCounts.setTextColor(if (type == StatusType.IDLE) getColor(R.color.text_secondary) else text)
    }

    private fun updateCounterDisplay() {
        binding.tvCounts.text = getString(
            R.string.counts,
            scannerViewModel.totalScans.value,
            scannerViewModel.successCount.value,
            scannerViewModel.failureCount.value
        )
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
        dataWedgeMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        unregisterScannerReceiver()
        dataWedgeMonitor.stop()
    }

    // ---------- Network pill ----------

    private val connectivityManager by lazy { getSystemService(ConnectivityManager::class.java) }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = postConnection()
        override fun onLost(network: Network) = postConnection()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = postConnection()
    }

    private fun postConnection() = runOnUiThread { showConnection(isOnline()) }

    private fun isOnline(): Boolean {
        val cm = connectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showConnection(online: Boolean) {
        binding.tvNetwork.setText(if (online) R.string.network_online else R.string.network_offline)
        binding.tvNetwork.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(if (online) GREEN else RED))
    }

    override fun onStart() {
        super.onStart()
        showConnection(isOnline())
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w("ScannerActivity", "Network callback unavailable: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }

    // ---------- Hardware scanner (DataWedge) pill ----------

    private fun showScannerHealth(health: DataWedgeMonitor.ScannerHealth) {
        val color = when (health.level) {
            DataWedgeMonitor.Level.CHECKING -> GREY
            DataWedgeMonitor.Level.OK -> GREEN
            DataWedgeMonitor.Level.WARNING -> AMBER
            DataWedgeMonitor.Level.ERROR -> RED
            DataWedgeMonitor.Level.UNAVAILABLE -> GREY
        }
        binding.tvScannerHealth.text = "● ${health.title}"
        binding.tvScannerHealth.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
        binding.tvScannerHealth.setTextColor(
            if (health.level == DataWedgeMonitor.Level.WARNING) Color.BLACK else Color.WHITE
        )
        // Only show the explanation line when something needs attention
        val showDetail = health.level == DataWedgeMonitor.Level.WARNING ||
            health.level == DataWedgeMonitor.Level.ERROR ||
            health.level == DataWedgeMonitor.Level.UNAVAILABLE
        binding.tvScannerDetail.text = health.detail
        binding.tvScannerDetail.visibility = if (showDetail) View.VISIBLE else View.GONE
        lastScannerHealth = health
    }

    private fun onScannerHealthTapped() {
        if (lastScannerHealth?.level == DataWedgeMonitor.Level.WARNING) {
            // Profile not active: re-apply it, then check again
            (application as? ZebraScannerApplication)?.configureDataWedgeProfile()
        }
        dataWedgeMonitor.check()
    }

    private companion object {
        const val BLUE = "#1565C0"
        const val GREEN = "#2E7D32"
        const val RED = "#C62828"
        const val AMBER = "#F9A825"
        const val GREY = "#757575"
    }
}
