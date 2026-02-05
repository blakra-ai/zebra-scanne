package com.stankovic.zebrascanner.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE = "https://luna-api-dev.solutions-apps.com"

    val logging = HttpLoggingInterceptor { message ->
        Log.d("HTTP", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)

        .addInterceptor(Interceptor { chain ->
            val original = chain.request()
        val request = original.newBuilder()
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
        })
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
