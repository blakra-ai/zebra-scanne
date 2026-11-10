package com.example.zebrascanner.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShippedOrderDao {
    @Insert
    suspend fun insertOrder(order: ShippedOrderEntity)
    
    @Query("SELECT * FROM shipped_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<ShippedOrderEntity>>
    
    @Query("SELECT * FROM shipped_orders WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getOrderByTrackingNumber(trackingNumber: String): ShippedOrderEntity?
    
    @Query("DELETE FROM shipped_orders WHERE id = :id")
    suspend fun deleteOrder(id: Long)
    
    @Query("DELETE FROM shipped_orders")
    suspend fun deleteAllOrders()
    
    @Query("SELECT COUNT(*) FROM shipped_orders")
    suspend fun getOrderCount(): Int
}
