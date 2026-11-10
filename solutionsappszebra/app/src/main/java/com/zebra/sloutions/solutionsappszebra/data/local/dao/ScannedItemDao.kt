package com.zebra.solutionsappszebra.data.local.dao

import androidx.room.*
import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScannedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScannedItemEntity)

    @Query("SELECT * FROM scanned_items ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ScannedItemEntity>>

    @Query("DELETE FROM scanned_items")
    suspend fun clearAll()
}
