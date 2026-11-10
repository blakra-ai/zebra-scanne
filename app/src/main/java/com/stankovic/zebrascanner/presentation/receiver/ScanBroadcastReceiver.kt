//package com.stankovic.zebrascanner.presentation.receiver
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import android.widget.Toast
//import com.stankovic.zebrascanner.config.ScanningConfig
//import com.stankovic.zebrascanner.core.AppException
//import com.stankovic.zebrascanner.core.Result
//import com.stankovic.zebrascanner.domain.usecase.MarkShippedUseCase
//import com.stankovic.zebrascanner.presentation.state.HistoryEntry
//import com.stankovic.zebrascanner.presentation.state.StatusType
//import com.stankovic.zebrascanner.viewmodel.ScannerViewModel
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import javax.inject.Inject
//
///**
// * Refactored BroadcastReceiver with Hilt DI and clean separation of concerns
// */
//@AndroidEntryPoint
//class ScanBroadcastReceiver : BroadcastReceiver() {
//
//    @Inject
//    lateinit var markShippedUseCase: MarkShippedUseCase
//
//    private var scannerViewModel: ScannerViewModel? = null
//
//    constructor()
//
//    constructor(scannerViewModel: ScannerViewModel) {
//        this.scannerViewModel = scannerViewModel
//    }
//
//    override fun onReceive(context: Context, intent: Intent) {
//        val trackingNumber = extractTrackingNumber(intent) ?: return
//
//        Log.d("ScanReceiver", "Scanned: $trackingNumber")
//        Toast.makeText(
//            context,
//            "Scanned: $trackingNumber",
//            Toast.LENGTH_SHORT
//        ).show()
//
//        scannerViewModel?.updateScannedData(trackingNumber)
//
//        // Process in background
//        CoroutineScope(Dispatchers.Main).launch {
//            processScan(context, trackingNumber)
//        }
//    }
//
//    private suspend fun processScan(context: Context, trackingNumber: String) {
//        try {
//            when (val result = markShippedUseCase(trackingNumber)) {
//                is Result.Success -> handleSuccess(trackingNumber, result.data.data?.get("message") as? String)
//                is Result.Error -> handleError(context, trackingNumber, result.exception)
//                is Result.Loading -> {} // Not expected in this flow
//            }
//        } catch (e: Exception) {
//            handleUnexpectedError(context, trackingNumber, e)
//        }
//    }
//
//    private suspend fun handleSuccess(trackingNumber: String, message: String?) {
//        val msg = message ?: "Order has been marked as shipped"
//        scannerViewModel?.updateStatus("OK: $msg")
//        scannerViewModel?.addHistoryEntry(
//            HistoryEntry(trackingNumber, StatusType.SUCCESS, msg)
//        )
//        withContext(Dispatchers.Main) {
//            scannerViewModel?.let {
//                Toast.makeText(it.getContext(), msg, Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private suspend fun handleError(context: Context, trackingNumber: String, exception: AppException) {
//        val statusType: StatusType
//        val message = exception.getMessage()
//
//        statusType = when (exception) {
//            is AppException.ValidationException -> StatusType.WARNING
//            is AppException.ConflictException -> StatusType.ERROR
//            else -> StatusType.ERROR
//        }
//
//        scannerViewModel?.updateStatus("${statusType.name}: $message")
//        scannerViewModel?.addHistoryEntry(
//            HistoryEntry(trackingNumber, statusType, message)
//        )
//
//        withContext(Dispatchers.Main) {
//            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private suspend fun handleUnexpectedError(context: Context, trackingNumber: String, exception: Throwable) {
//        val msg = exception.message ?: "Unknown error occurred"
//        scannerViewModel?.updateStatus("Error: $msg")
//        scannerViewModel?.addHistoryEntry(
//            HistoryEntry(trackingNumber, StatusType.ERROR, msg)
//        )
//        withContext(Dispatchers.Main) {
//            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun extractTrackingNumber(intent: Intent): String? {
//        return intent.getStringExtra("com.symbol.datawedge.data_string")
//            ?: intent.getStringExtra(ScanningConfig.ZEBRA_SCANNER_INTENT)
//    }
//}
