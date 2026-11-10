package com.zebra.sloutions.solutionsappszebra.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_items")
data class ScannedItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val trackingNumber: String,
    val orderNumber: String,
    val shippedOn: String,
    val createdAt: Long = System.currentTimeMillis()
)
