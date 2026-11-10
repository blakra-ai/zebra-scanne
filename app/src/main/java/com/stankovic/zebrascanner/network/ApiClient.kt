package com.stankovic.zebrascanner.network

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Single-responsibility HTTP client builder
 */
class ApiClient @Singleton constructor(
    val service: ApiService
) {
    companion object {
        private const val BASE = "https://luna-api.solutions-apps.com"

        private fun createLoggingInterceptor(): HttpLoggingInterceptor {
            return HttpLoggingInterceptor { message ->
                Log.d("HTTP", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        }

        private fun createHeaderInterceptor(): Interceptor {
            return Interceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
        }

        private fun createOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createHeaderInterceptor())
                .build()
        }

        private fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        fun createApiService(): ApiService {
            val okHttpClient = createOkHttpClient()
            val retrofit = createRetrofit(okHttpClient)
            return retrofit.create(ApiService::class.java)
        }
    }
}

/**
 * Hilt dependency injection module for API client
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiClientModule {
    @Singleton
    @Provides
    fun provideApiClient(): ApiClient {
        val apiService = ApiClient.createApiService()
        return ApiClient(apiService)
    }
}
