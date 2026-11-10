package com.zebra.sloutions.solutionsappszebra.di


import android.content.Context
import androidx.room.Room
import com.zebra.solutionsappszebra.data.local.AppDatabase
import com.zebra.solutionsappszebra.data.local.dao.ScannedItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "zebra_shipping_db"
        ).build()

    @Provides
    fun provideScannedItemDao(
        db: AppDatabase
    ): ScannedItemDao = db.scannedItemDao()
}
