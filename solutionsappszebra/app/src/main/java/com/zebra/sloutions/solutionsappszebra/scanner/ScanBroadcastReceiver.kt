package com.zebra.sloutions.solutionsappszebra.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zebra.sloutions.solutionsappszebra.worker.ScanSyncWorker

class ScanBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val barcode = intent.getStringExtra("com.symbol.datawedge.data_string")
            ?: return

        val work = OneTimeWorkRequestBuilder<ScanSyncWorker>()
            .setInputData(workDataOf("barcode" to barcode))
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}
