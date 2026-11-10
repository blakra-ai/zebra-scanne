package com.zebra.sloutions.solutionsappszebra.data.repository

import ApiService
import android.content.Context
import com.zebra.sloutions.solutionsappszebra.data.local.dao.ScanDao
import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScanEntity
import com.zebra.sloutions.solutionsappszebra.data.model.ScannedItem
import com.zebra.sloutions.solutionsappszebra.data.remote.models.ShipRequest
import java.io.File

class ScanRepository(private val context: Context) {

    private val fileName = "scanned_items.json"

    private fun getFile(): File = File(context.filesDir, fileName)

    fun saveScannedItem(item: ScannedItem) {
        val list = loadScannedItems().toMutableList()
        list.add(item)
        getFile().writeText(Json.encodeToString(list))
    }

    fun loadScannedItems(): List<ScannedItem> {
        val file = getFile()
        if (!file.exists()) return emptyList()
        return try {
            Json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun exportToCsv(): File {
        val file = File(context.filesDir, "scanned_items.csv")
        val header = "TrackingNumber,OrderNumber,ShippedOn,Status"
        val lines = loadScannedItems().map {
            "${it.trackingNumber},${it.orderNumber ?: ""},${it.shippedOn ?: ""},${it.status}"
        }
        file.writeText((listOf(header) + lines).joinToString("\n"))
        return file
    }
}

