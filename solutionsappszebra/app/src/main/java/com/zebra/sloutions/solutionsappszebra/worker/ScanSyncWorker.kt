package com.zebra.sloutions.solutionsappszebra.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zebra.sloutions.solutionsappszebra.data.repository.ScanRepository

class ScanSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ScanRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val barcode = inputData.getString("barcode") ?: return Result.failure()

        repository.processScan(barcode)

        return Result.success()
    }
}
