package com.yourcompany.zabrascannerandroid.data.api

import com.yourcompany.zabrascannerandroid.data.local.TokenManager
import android.content.Context
import com.yourcompany.zabrascannerandroid.data.model.LoginRequest
import com.yourcompany.zabrascannerandroid.data.model.LoginResponse
import com.yourcompany.zabrascannerandroid.data.model.MarkShippedRequest
import com.yourcompany.zabrascannerandroid.data.model.MarkShippedResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://luna-api.solutions-apps.com/"
    private var tokenManager: TokenManager? = null

    fun init(context: Context) {
        tokenManager = TokenManager.getInstance(context)
    }

    private val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("accept", "application/json, text/plain, */*")
            .header("accept-language", "tr-TR,tr;q=0.9,en-TR;q=0.8,en;q=0.7,en-US;q=0.6")
            .header("cache-control", "no-cache")
            .header("content-type", "application/json")
            .header("http-accept-language", "de")
            .header("origin", "https://luna-admin-dev.solutions-apps.com")
            .header("pragma", "no-cache")
            .header("referer", "https://luna-admin-dev.solutions-apps.com/")
            .header("sec-ch-ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Google Chrome\";v=\"144\"")
            .header("sec-ch-ua-mobile", "?0")
            .header("sec-ch-ua-platform", "\"Windows\"")
            .header("sec-fetch-dest", "empty")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-site", "same-site")
            .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
            .header("x-panel", "true")

        // Add Authorization header if token exists
        tokenManager?.getToken()?.let { token ->
            if (token.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        val request = requestBuilder
            .method(original.method, original.body)
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    suspend fun login(username: String, password: String): LoginResponse {
        return apiService.login(LoginRequest(username, password))
    }

    suspend fun markShipped(trackingNumber: String): MarkShippedResponse {
        return apiService.markShipped(MarkShippedRequest(trackingNumber))
    }
}
