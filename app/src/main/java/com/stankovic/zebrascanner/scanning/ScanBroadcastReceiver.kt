package com.stankovic.zebrascanner.scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.stankovic.zebrascanner.config.ScanningConfig
import com.stankovic.zebrascanner.presentation.state.HistoryEntry
import com.stankovic.zebrascanner.presentation.state.StatusType
import com.stankovic.zebrascanner.viewmodel.ScannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stankovic.zebrascanner.network.ApiClient
import org.json.JSONObject
import org.json.JSONArray

class ScanBroadcastReceiver : BroadcastReceiver {
    private var scannerViewModel: ScannerViewModel? = null
    constructor()
    private val apiService by lazy {
        ApiClient.createApiService()
    }
    constructor(scannerViewModel: ScannerViewModel) {
        this.scannerViewModel = scannerViewModel
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        var tracking: String? = null

        // Runtime sender validation: accept broadcasts only from DataWedge (package: com.symbol.datawedge)
        val allowedSender = "com.symbol.datawedge"
        var validatedSender = false
        try {
            // sentFromUid exists only on Android 14+ and is -1 unless the sender opts in
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                val senderUid = sentFromUid
                if (senderUid >= 0) {
                    val senderPkgs = context.packageManager.getPackagesForUid(senderUid)
                    if (senderPkgs != null && senderPkgs.contains(allowedSender)) {
                        validatedSender = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ScanReceiver", "Sender validation exception: ${e.message}")
        }
        if (!validatedSender) {
            // Fallback checks: intent.package or known DataWedge extras (best-effort)
            val senderPkg = intent.`package`
            if (senderPkg == allowedSender) validatedSender = true
            val sourceExtra = intent.getStringExtra("com.symbol.datawedge.source")
            // DataWedge sets source to "scanner", "msr", "simulscan", etc.
            if (!validatedSender && sourceExtra != null) validatedSender = true
        }
        if (!validatedSender) {
            Log.w("ScanReceiver", "Rejected broadcast from unknown sender; possible spoofed intent.")
            Toast.makeText(context, "Scan ignored: unknown sender", Toast.LENGTH_SHORT).show()
            return
        }

        intent?.let {
            val data = it.getStringExtra("com.symbol.datawedge.data_string")
            val labelType = it.getStringExtra("com.symbol.datawedge.label_type")
            Log.d("ScanReceiver", "Data: $data, Type: $labelType")
            Toast.makeText(context, "Scanned: $data ($labelType)", Toast.LENGTH_SHORT).show()
            tracking = it.getStringExtra(ScanningConfig.ZEBRA_SCANNER_INTENT) ?: data
        }

        if (action != null && action == ScanningConfig.APP_SCANNER_INTENT) {
            val decodedData = intent.getStringExtra(ScanningConfig.ZEBRA_SCANNER_INTENT)
            tracking = decodedData ?: tracking
        }

        tracking?.trim()?.takeIf { it.isNotEmpty() }?.let { submit(context, it) }
    }

    /** Sends a tracking number to the backend; shared by DataWedge, manual entry and camera scans. */
    fun submit(context: Context, tn: String) {
        // Ignore the same code fired twice in quick succession (double trigger press / duplicate broadcast)
        val now = System.currentTimeMillis()
        synchronized(Companion) {
            if (tn == lastCode && now - lastCodeAt < DUPLICATE_WINDOW_MS) {
                Log.d("ScanReceiver", "Ignoring duplicate scan of $tn")
                return
            }
            lastCode = tn
            lastCodeAt = now
        }
        val appContext = context.applicationContext
        run {
            // update viewmodel if available
            scannerViewModel?.updateScannedData(tn)

            // call backend to mark shipped via Retrofit + Coroutines
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // get token from prefs if available
                    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("auth_token", null)

                    val authHeader = if (!token.isNullOrEmpty()) "Bearer $token" else null

                    val response = apiService.markShipped(authHeader, com.stankovic.zebrascanner.network.MarkShippedRequest(tn))
                    Log.d("ScanReceiver", "response: $response, response: ")

                    // increment persistent total
                    val totalPref = prefs.getInt("total_scans", 0) + 1
                    prefs.edit().putInt("total_scans", totalPref).apply()
                    scannerViewModel?.incrementTotal()

                    if (response.code() in 200..299) {
                            val markResp = response.body()
                        val msg = markResp?.data?.get("message") as? String ?: "Order has been marked as shipped."
                        // success
                        Log.d("ScanReceiver", "msg: $msg, response: ")

                        prefs.edit().putInt("success_scans", prefs.getInt("success_scans", 0) + 1).apply()
                        scannerViewModel?.incrementSuccess()
                        ScanFeedback.success(appContext)
                        scannerViewModel?.updateStatus("OK: $msg")
                        scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.SUCCESS, msg))
                        if (scannerViewModel == null) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        }
                    } else {
                        // read error body
                        val err = response.errorBody()?.string() ?: "Server returned ${response.code()}"
                        Log.d("ScanReceiver", "err: $err, response: ")

                        when (response.code()) {
                            401 -> {
                                // Token expired or revoked: clear it and send the user back to login
                                prefs.edit().remove("auth_token").apply()
                                prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                scannerViewModel?.incrementFailure()
                                scannerViewModel?.updateStatus("Error: Session expired, please log in again")
                                scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.ERROR, "Session expired — not marked"))
                                ScanFeedback.failure(appContext)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Session expired, please log in again", Toast.LENGTH_LONG).show()
                                    context.startActivity(
                                        Intent(context, com.stankovic.zebrascanner.login_form::class.java)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    )
                                }
                            }
                            422 -> {
                                // try to parse tracking_number array
                                        try {
                                            val j = JSONObject(err)
                                            val arr = if (j.has("tracking_number")) j.optJSONArray("tracking_number") else null
                                            val msg = if (arr != null && arr.length() > 0) arr.getString(0) else "Invalid tracking number."
                                    prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                    scannerViewModel?.incrementFailure()
                                    ScanFeedback.failure(appContext)
                                    scannerViewModel?.updateStatus("Warning: $msg")
                                    scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.WARNING, msg))
                                    if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                } catch (e: Exception) {
                                    prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                    scannerViewModel?.incrementFailure()
                                    ScanFeedback.failure(appContext)
                                    scannerViewModel?.updateStatus("Warning: Validation error")
                                    scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.WARNING, "Validation error"))
                                    if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, "Validation error", Toast.LENGTH_LONG).show() }
                                }
                            }
                            409 -> {
                                        val msg = try { JSONObject(err).optJSONObject("data")?.optString("message") ?: "This order has already been shipped." } catch (_: Exception) { "This order has already been shipped." }
                                Log.d("ScanReceiver", "409: $msg, response: ")

                                prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                scannerViewModel?.incrementFailure()
                                ScanFeedback.failure(appContext)
                                scannerViewModel?.updateStatus("Error: $msg")
                                scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.ERROR, msg))
                                if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                            else -> {
                                val msg = "Server returned ${response.code()}"
                                Log.d("ScanReceiver", "returned: $msg, response: ")

                                prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                scannerViewModel?.incrementFailure()
                                ScanFeedback.failure(appContext)
                                scannerViewModel?.updateStatus("Error: $msg")
                                scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.ERROR, msg))
                                if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    Log.d("ScanReceiver", "Exception: Exception, response: ")

                    prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                    scannerViewModel?.incrementFailure()
                    ScanFeedback.failure(appContext)
                    scannerViewModel?.updateStatus("Error: ${e.message}")
                    scannerViewModel?.addHistoryEntry(HistoryEntry(tn, StatusType.ERROR, e.message ?: "Network error"))
                    if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, e.message ?: "Network error", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    companion object {
        private const val DUPLICATE_WINDOW_MS = 2000L
        private var lastCode: String? = null
        private var lastCodeAt = 0L
    }
}
