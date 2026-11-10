package com.zebra.sloutions.solutionsappszebra.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zebra.sloutions.solutionsappszebra.data.remote.models.ScanStatus

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val barcode: String,

    val orderNumber: String?,
    val trackingNumber: String?,
    val shippedOn: String?,

    val status: ScanStatus,
    val errorMessage: String?,

    val createdAt: Long = System.currentTimeMillis()
)
