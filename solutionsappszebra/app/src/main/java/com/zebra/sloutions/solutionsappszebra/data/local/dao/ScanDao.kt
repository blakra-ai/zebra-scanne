package com.zebra.sloutions.solutionsappszebra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Update
    suspend fun update(scan: ScanEntity)

    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE status = 'PENDING'")
    suspend fun getPending(): List<ScanEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM scans WHERE barcode = :barcode)")
    suspend fun exists(barcode: String): Boolean
}
