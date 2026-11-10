package com.yourcompany.zabrascannerandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShippedOrderDao {
    @Query("SELECT * FROM shipped_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<ShippedOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: ShippedOrder)

    @Query("DELETE FROM shipped_orders")
    suspend fun deleteAllOrders()
}
