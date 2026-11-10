package com.zebra.solutionsappszebra.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScannedItemEntity
import com.zebra.solutionsappszebra.data.local.dao.ScannedItemDao

@Database(
    entities = [ScannedItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scannedItemDao(): ScannedItemDao
}
