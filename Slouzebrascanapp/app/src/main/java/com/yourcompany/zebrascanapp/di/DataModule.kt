package com.yourcompany.zebrascanapp.di

import android.content.Context
import androidx.room.Room
import com.yourcompany.zebrascanapp.data.local.ScanDatabase
import com.yourcompany.zebrascanapp.data.local.dao.ScanDao
import com.yourcompany.zebrascanapp.data.remote.ScanApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideScanApi(retrofit: Retrofit): ScanApi = retrofit.create(ScanApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanDatabase =
        Room.databaseBuilder(context, ScanDatabase::class.java, "scan_db").build()

    @Provides
    fun provideScanDao(db: ScanDatabase): ScanDao = db.scanDao()
}
