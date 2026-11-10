package com.stankovic.zebrascanner.scanning

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.stankovic.zebrascanner.R
import com.stankovic.zebrascanner.config.AppLanguage

/**
 * Asks DataWedge (Zebra's scanning service) whether the hardware scanner can actually reach this app.
 *
 * Checks, in order: DataWedge installed -> responds -> enabled -> our profile is active -> scanner state.
 * APIs (techdocs.zebra.com/datawedge/latest/guide/api/): GET_VERSION_INFO (6.4+), GET_DATAWEDGE_STATUS (6.4+),
 * GET_ACTIVE_PROFILE, GET_SCANNER_STATUS (6.5+). All reply on RESULT_ACTION.
 * A real scan arriving is the strongest proof, so [onScanReceived] upgrades the state to VERIFIED.
 */
class DataWedgeMonitor(
    private val context: Context,
    private val profileName: String,
    private val onState: (ScannerHealth) -> Unit
) {
    enum class Level { CHECKING, OK, WARNING, ERROR, UNAVAILABLE }

    data class ScannerHealth(val level: Level, val title: String, val detail: String)

    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var verified = false

    // Answers collected for the current check round
    private var version: String? = null
    private var dwEnabled: String? = null
    private var activeProfile: String? = null
    private var scannerStatus: String? = null

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val extras = intent.extras ?: return
            when {
                extras.containsKey(RESULT_VERSION) ->
                    version = intent.getBundleExtra(RESULT_VERSION)?.getString("DATAWEDGE") ?: "unknown"
                extras.containsKey(RESULT_DW_STATUS) ->
                    // Docs list the type as both String and Bundle; read whatever arrives
                    dwEnabled = extras.get(RESULT_DW_STATUS)?.toString()
                extras.containsKey(RESULT_ACTIVE_PROFILE) ->
                    activeProfile = intent.getStringExtra(RESULT_ACTIVE_PROFILE)
                extras.containsKey(RESULT_SCANNER_STATUS) ->
                    scannerStatus = intent.getStringExtra(RESULT_SCANNER_STATUS)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun start() {
        if (!registered) {
            val filter = IntentFilter(RESULT_ACTION).apply { addCategory(Intent.CATEGORY_DEFAULT) }
            // DataWedge is another app, so the receiver must be exported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(resultReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(resultReceiver, filter)
            }
            registered = true
        }
        check()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        if (registered) {
            try {
                context.unregisterReceiver(resultReceiver)
            } catch (_: IllegalArgumentException) {
            }
            registered = false
        }
    }

    /** Runs a fresh check round; the verdict is published after the replies had time to arrive. */
    fun check() {
        handler.removeCallbacksAndMessages(null)
        if (!isDataWedgeInstalled()) {
            publish(ScannerHealth(Level.UNAVAILABLE, s(R.string.dw_none_title),
                s(R.string.dw_none_detail)))
            return
        }
        if (!verified) publish(ScannerHealth(Level.CHECKING, s(R.string.dw_checking_title), s(R.string.dw_checking_detail)))
        version = null; dwEnabled = null; activeProfile = null; scannerStatus = null

        // DataWedge switches to our profile when our screen comes to the front; give it a moment first
        handler.postDelayed({
            query(GET_VERSION)
            query(GET_DW_STATUS)
            query(GET_ACTIVE_PROFILE)
            query(GET_SCANNER_STATUS)
        }, QUERY_DELAY_MS)
        handler.postDelayed({ evaluate() }, QUERY_DELAY_MS + ANSWER_TIMEOUT_MS)
    }

    /** A barcode really arrived from DataWedge: the whole chain works. */
    fun onScanReceived() {
        verified = true
        publish(ScannerHealth(Level.OK, s(R.string.dw_connected_title), s(R.string.dw_connected_detail)))
    }

    private fun evaluate() {
        val health = when {
            // An explicit "off" answer always wins, even after earlier successful scans
            dwEnabled != null && dwEnabled.equals("disabled", ignoreCase = true) -> ScannerHealth(Level.ERROR,
                s(R.string.dw_off_title), s(R.string.dw_off_detail))
            // A real scan already proved the link; a missing answer doesn't override that
            verified -> ScannerHealth(Level.OK, s(R.string.dw_connected_title), s(R.string.dw_connected_detail))
            // No answer: could be an old DataWedge (< 6.4) or a stuck service, so don't claim it's broken
            version == null -> ScannerHealth(Level.WARNING, s(R.string.dw_unconfirmed_title),
                s(R.string.dw_unconfirmed_detail))
            activeProfile != null && activeProfile != profileName -> ScannerHealth(Level.WARNING,
                s(R.string.dw_not_linked_title),
                s(R.string.dw_not_linked_detail, activeProfile ?: ""))
            scannerStatus.equals("DISABLED", ignoreCase = true) -> ScannerHealth(Level.ERROR,
                s(R.string.dw_disabled_title), s(R.string.dw_disabled_detail))
            scannerStatus.equals("DISCONNECTED", ignoreCase = true) -> ScannerHealth(Level.ERROR,
                s(R.string.dw_disconnected_title), s(R.string.dw_disconnected_detail))
            else -> ScannerHealth(Level.OK, s(R.string.dw_ready_title),
                s(R.string.dw_ready_detail) + (version?.let { " · DataWedge $it" } ?: ""))
        }
        Log.d(TAG, "version=$version dw=$dwEnabled profile=$activeProfile scanner=$scannerStatus -> ${health.title}")
        publish(health)
    }

    private fun query(extra: String) {
        try {
            context.sendBroadcast(Intent(DW_ACTION).putExtra(extra, ""))
        } catch (e: Exception) {
            Log.w(TAG, "Query $extra failed: ${e.message}")
        }
    }

    private fun isDataWedgeInstalled(): Boolean = try {
        // Needs <queries> for com.symbol.datawedge on Android 11+
        context.packageManager.getPackageInfo(DATAWEDGE_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun s(id: Int, vararg args: Any): String = AppLanguage.localized(context).getString(id, *args)

    private fun publish(health: ScannerHealth) = handler.post { onState(health) }

    companion object {
        private const val TAG = "DataWedgeMonitor"
        private const val DATAWEDGE_PACKAGE = "com.symbol.datawedge"
        private const val DW_ACTION = "com.symbol.datawedge.api.ACTION"
        private const val RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION"
        private const val GET_VERSION = "com.symbol.datawedge.api.GET_VERSION_INFO"
        private const val GET_DW_STATUS = "com.symbol.datawedge.api.GET_DATAWEDGE_STATUS"
        private const val GET_ACTIVE_PROFILE = "com.symbol.datawedge.api.GET_ACTIVE_PROFILE"
        private const val GET_SCANNER_STATUS = "com.symbol.datawedge.api.GET_SCANNER_STATUS"
        private const val RESULT_VERSION = "com.symbol.datawedge.api.RESULT_GET_VERSION_INFO"
        private const val RESULT_DW_STATUS = "com.symbol.datawedge.api.RESULT_GET_DATAWEDGE_STATUS"
        private const val RESULT_ACTIVE_PROFILE = "com.symbol.datawedge.api.RESULT_GET_ACTIVE_PROFILE"
        private const val RESULT_SCANNER_STATUS = "com.symbol.datawedge.api.RESULT_SCANNER_STATUS"
        private const val QUERY_DELAY_MS = 700L
        private const val ANSWER_TIMEOUT_MS = 2500L
    }
}
