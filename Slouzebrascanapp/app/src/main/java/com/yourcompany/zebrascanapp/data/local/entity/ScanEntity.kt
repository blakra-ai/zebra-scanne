package com.yourcompany.zebrascanapp.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val trackingNumber: String,
    val shippedOn: String,
    val message: String
)
