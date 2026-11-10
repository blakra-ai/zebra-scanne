package com.stankovic.zebrascanner.di

import android.content.Context
import android.content.SharedPreferences
import com.stankovic.zebrascanner.data.local.LocalDataSource
import com.stankovic.zebrascanner.data.local.SharedPreferencesDataSource
import com.stankovic.zebrascanner.data.repository.ShippingRepository
import com.stankovic.zebrascanner.data.repository.ShippingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Singleton
    @Provides
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Singleton
    @Provides
    fun provideLocalDataSource(
        sharedPreferences: SharedPreferences
    ): LocalDataSource {
        return SharedPreferencesDataSource(sharedPreferences)
    }

    @Singleton
    @Provides
    fun provideShippingRepository(
        apiClient: com.stankovic.zebrascanner.network.ApiClient,
        localDataSource: LocalDataSource
    ): ShippingRepository {
        return ShippingRepositoryImpl(apiClient, localDataSource)
    }
}
