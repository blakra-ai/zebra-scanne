package com.example.zebrascanner.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipped_orders")
data class ShippedOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val trackingNumber: String,
    val shippedOn: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
