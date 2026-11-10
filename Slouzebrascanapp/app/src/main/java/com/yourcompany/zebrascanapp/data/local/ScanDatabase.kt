package com.yourcompany.zebrascanapp.data.local


import androidx.room.Database
import androidx.room.RoomDatabase
import com.yourcompany.zebrascanapp.data.local.dao.ScanDao
import com.yourcompany.zebrascanapp.data.local.entity.ScanEntity

@Database(entities = [ScanEntity::class], version = 1)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}