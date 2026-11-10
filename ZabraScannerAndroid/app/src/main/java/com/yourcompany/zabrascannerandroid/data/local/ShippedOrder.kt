package com.yourcompany.zabrascannerandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipped_orders")
data class ShippedOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val trackingNumber: String,
    val shippedOn: String,
    val createdAt: Long = System.currentTimeMillis()
)
