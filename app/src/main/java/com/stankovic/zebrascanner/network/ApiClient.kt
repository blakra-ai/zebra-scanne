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
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Single-responsibility HTTP client builder
 */
class ApiClient @Singleton constructor(
    val service: ApiService
) {
    companion object {
        private const val BASE = "https://luna-api.solutions-apps.com"

        /** Two-letter app language sent as X-Language so the backend answers in it; set by the Application. */
        @Volatile
        var language: () -> String = { "en" }

        private fun createLoggingInterceptor(): HttpLoggingInterceptor {
            return HttpLoggingInterceptor { message ->
                Log.d("HTTP", message)
            }.apply {
                // BODY would write passwords and tokens to logcat; keep it to request lines
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            }
        }

        private fun createHeaderInterceptor(): Interceptor {
            return Interceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("X-Language", language())
                    .build()
                chain.proceed(request)
            }
        }

        private fun createOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                // Warehouse Wi-Fi can be slow; fail fast enough that the operator can rescan
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
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
