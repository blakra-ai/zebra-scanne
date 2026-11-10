package com.stankovic.zebrascanner.scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.stankovic.zebrascanner.R
import com.stankovic.zebrascanner.config.AppLanguage
import com.stankovic.zebrascanner.config.ScanningConfig
import com.stankovic.zebrascanner.presentation.state.HistoryEntry
import com.stankovic.zebrascanner.presentation.state.StatusType
import com.stankovic.zebrascanner.viewmodel.ScannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stankovic.zebrascanner.network.ApiClient
import com.stankovic.zebrascanner.network.MarkShippedRequest
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

class ScanBroadcastReceiver : BroadcastReceiver {
    private var scannerViewModel: ScannerViewModel? = null

    /** Called when a barcode genuinely arrives from DataWedge (proves the scanner -> app link works). */
    var onHardwareScan: (() -> Unit)? = null
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
            Toast.makeText(context, AppLanguage.localized(context).getString(R.string.scan_ignored_sender), Toast.LENGTH_SHORT).show()
            return
        }

        intent?.let {
            val data = it.getStringExtra("com.symbol.datawedge.data_string")
            val labelType = it.getStringExtra("com.symbol.datawedge.label_type")
            Log.d("ScanReceiver", "Data: $data, Type: $labelType")
            tracking = it.getStringExtra(ScanningConfig.ZEBRA_SCANNER_INTENT) ?: data
        }

        if (action != null && action == ScanningConfig.APP_SCANNER_INTENT) {
            val decodedData = intent.getStringExtra(ScanningConfig.ZEBRA_SCANNER_INTENT)
            tracking = decodedData ?: tracking
        }

        onHardwareScan?.invoke()
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
        scannerViewModel?.updateScannedData(tn)
        scannerViewModel?.markProcessing(tn)

        // Strings in the app language, not the phone's
        val res = AppLanguage.localized(appContext)

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val outcome = try {
                val token = prefs.getString("auth_token", null)
                val authHeader = if (!token.isNullOrEmpty()) "Bearer $token" else null
                val response = apiService.markShipped(authHeader, MarkShippedRequest(tn))
                Log.d("ScanReceiver", "mark-shipped $tn -> ${response.code()}")
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    ScanOutcome(
                        StatusType.SUCCESS,
                        data?.get("message") as? String ?: res.getString(R.string.scan_marked_default),
                        orderNumber = when (val n = data?.get("order_number")) {
                            // Gson reads JSON numbers inside Map<String, Any> as Double (123 -> 123.0)
                            is Double -> if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
                            null -> null
                            else -> n.toString()
                        }
                    )
                } else {
                    outcomeForError(res, response.code(), response.errorBody()?.string())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                ScanOutcome(StatusType.ERROR, res.getString(R.string.scan_timeout))
            } catch (e: IOException) {
                ScanOutcome(StatusType.ERROR, res.getString(R.string.scan_no_connection))
            } catch (e: Exception) {
                Log.e("ScanReceiver", "Unexpected error for $tn", e)
                ScanOutcome(StatusType.ERROR, res.getString(R.string.scan_unexpected, e.message ?: e.javaClass.simpleName))
            }

            // Persist and show the result
            prefs.edit()
                .putInt("total_scans", prefs.getInt("total_scans", 0) + 1)
                .apply {
                    if (outcome.status == StatusType.SUCCESS) putInt("success_scans", prefs.getInt("success_scans", 0) + 1)
                    else putInt("failure_scans", prefs.getInt("failure_scans", 0) + 1)
                }
                .apply()
            scannerViewModel?.incrementTotal()
            if (outcome.status == StatusType.SUCCESS) {
                scannerViewModel?.incrementSuccess()
                ScanFeedback.success(appContext)
            } else {
                scannerViewModel?.incrementFailure()
                ScanFeedback.failure(appContext)
            }
            val label = when (outcome.status) {
                StatusType.SUCCESS -> "OK"
                StatusType.WARNING -> "Warning"
                else -> "Error"
            }
            val shown = outcome.orderNumber?.let { "${outcome.message} (${res.getString(R.string.order_label, it)})" } ?: outcome.message
            scannerViewModel?.updateStatus("$label: $shown")
            scannerViewModel?.addHistoryEntry(HistoryEntry(tn, outcome.status, outcome.message, outcome.orderNumber))

            withContext(Dispatchers.Main) {
                if (scannerViewModel == null) Toast.makeText(appContext, shown, Toast.LENGTH_LONG).show()
                if (outcome.sessionExpired) {
                    prefs.edit().remove("auth_token").apply()
                    Toast.makeText(appContext, res.getString(R.string.scan_session_toast), Toast.LENGTH_LONG).show()
                    appContext.startActivity(
                        Intent(appContext, com.stankovic.zebrascanner.login_form::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
            }
        }
    }

    private data class ScanOutcome(
        val status: StatusType,
        val message: String,
        val orderNumber: String? = null,
        val sessionExpired: Boolean = false
    )

    /**
     * Maps backend error responses to a user-facing outcome. Backend shapes:
     * - business errors (incl. "already shipped", sent as 400 not 409): {"data":{"message":"..."}}
     * - validation 422: {"tracking_number":["..."]}
     * - 401/403 from Laravel: {"message":"..."}
     */
    private fun outcomeForError(res: Context, code: Int, body: String?): ScanOutcome {
        val json = try { body?.let { JSONObject(it) } } catch (_: Exception) { null }
        val serverMessage = json?.optJSONObject("data")?.optString("message")?.takeIf { it.isNotBlank() }
            ?: json?.optJSONArray("tracking_number")?.optString(0)?.takeIf { it.isNotBlank() }
            ?: json?.optString("message")?.takeIf { it.isNotBlank() }

        return when {
            code == 401 -> ScanOutcome(StatusType.ERROR, res.getString(R.string.scan_session_expired), sessionExpired = true)
            code == 403 -> ScanOutcome(StatusType.ERROR, res.getString(R.string.scan_forbidden))
            code == 422 -> ScanOutcome(StatusType.WARNING, serverMessage ?: res.getString(R.string.scan_not_found))
            code == 409 || (serverMessage != null && serverMessage in ALREADY_SHIPPED_MESSAGES) ->
                ScanOutcome(StatusType.WARNING, serverMessage ?: res.getString(R.string.scan_already))
            code >= 500 -> ScanOutcome(StatusType.ERROR, res.getString(R.string.scan_server_error, code))
            else -> ScanOutcome(StatusType.ERROR, serverMessage ?: res.getString(R.string.scan_server_returned, code))
        }
    }

    companion object {
        // Backend shipping.already_shipped in each language; the backend sends it as HTTP 400, not 409
        private val ALREADY_SHIPPED_MESSAGES = setOf(
            "This order has already been shipped.",
            "Diese Bestellung wurde bereits versendet.",
            "Este pedido ya se ha enviado.",
            "Ezt a rendelést már kiszállítottuk.",
        )
        private const val DUPLICATE_WINDOW_MS = 2000L
        private var lastCode: String? = null
        private var lastCodeAt = 0L
    }
}
