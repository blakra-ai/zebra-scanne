package com.yourcompany.zebrascanapp.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.zebrascanapp.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity)

    @Query("SELECT * FROM scans ORDER BY id DESC")
    fun getAllScans(): Flow<List<ScanEntity>>
}
