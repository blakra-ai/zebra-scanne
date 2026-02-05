package com.stankovic.zebrascanner.scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.stankovic.zebrascanner.config.ScanningConfig
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

    constructor(scannerViewModel: ScannerViewModel) {
        this.scannerViewModel = scannerViewModel
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        var tracking: String? = null

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

        tracking?.let { tn ->
            // update viewmodel if available
            scannerViewModel?.updateScannedData(tn)

            // call backend to mark shipped via Retrofit + Coroutines
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // get token from prefs if available
                    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("auth_token", null)
                    val authHeader = if (!token.isNullOrEmpty()) "Bearer $token" else null

                    val response = ApiClient.service.markShipped(authHeader, com.stankovic.zebrascanner.network.MarkShippedRequest(tn))
                    if (!response.isSuccessful) {
                        Log.d("ScanReceiver", "response: $response, response: ")

                        return@launch
}

val contentType = response.headers()["Content-Type"] ?: ""
if (!contentType.contains("application/json")) {
    throw IllegalStateException("Non-JSON response received (likely auth redirect)")
}
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
                        scannerViewModel?.updateStatus("OK: $msg")
                        scannerViewModel?.addHistoryEntry("$tn — OK: $msg")
                        if (scannerViewModel == null) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        }
                    } else {
                        // read error body
                        val err = response.errorBody()?.string() ?: "Server returned ${response.code()}"
                        Log.d("ScanReceiver", "err: $err, response: ")

                        when (response.code()) {
                            422 -> {
                                // try to parse tracking_number array
                                        try {
                                            val j = JSONObject(err)
                                            val arr = if (j.has("tracking_number")) j.optJSONArray("tracking_number") else null
                                            val msg = if (arr != null && arr.length() > 0) arr.getString(0) else "Invalid tracking number."
                                    prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                    scannerViewModel?.incrementFailure()
                                    scannerViewModel?.updateStatus("Warning: $msg")
                                    scannerViewModel?.addHistoryEntry("$tn — Warning: $msg")
                                    if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                } catch (e: Exception) {
                                    prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                    scannerViewModel?.incrementFailure()
                                    scannerViewModel?.updateStatus("Warning: Validation error")
                                    scannerViewModel?.addHistoryEntry("$tn — Warning: Validation error")
                                    if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, "Validation error", Toast.LENGTH_LONG).show() }
                                }
                            }
                            409 -> {
                                        val msg = try { JSONObject(err).optJSONObject("data")?.optString("message") ?: "This order has already been shipped." } catch (_: Exception) { "This order has already been shipped." }
                                Log.d("ScanReceiver", "409: $msg, response: ")

                                prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                scannerViewModel?.incrementFailure()
                                scannerViewModel?.updateStatus("Error: $msg")
                                scannerViewModel?.addHistoryEntry("$tn — Error: $msg")
                                if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                            else -> {
                                val msg = "Server returned ${response.code()}"
                                Log.d("ScanReceiver", "returned: $msg, response: ")

                                prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                                scannerViewModel?.incrementFailure()
                                scannerViewModel?.updateStatus("Error: $msg")
                                scannerViewModel?.addHistoryEntry("$tn — Error: $msg")
                                if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    Log.d("ScanReceiver", "Exception: Exception, response: ")

                    prefs.edit().putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1).apply()
                    scannerViewModel?.incrementFailure()
                    scannerViewModel?.updateStatus("Error: ${e.message}")
                    scannerViewModel?.addHistoryEntry("$tn — Error: ${e.message}")
                    if (scannerViewModel == null) withContext(Dispatchers.Main) { Toast.makeText(context, e.message ?: "Network error", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }
}
