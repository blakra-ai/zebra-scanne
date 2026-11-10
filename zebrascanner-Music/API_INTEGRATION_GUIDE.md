# Zebra Scanner - API Integration Guide

## Overview
This document explains how the authentication flow works and how to use the saved token for other API calls.

## Authentication Flow

### 1. Splash Screen (SplashActivity)
- App starts with SplashActivity
- Checks if user is logged in using `TokenManager.isLoggedIn()`
- If logged in → Navigate to HomeActivity
- If not logged in → Navigate to LoginActivity

### 2. Login (LoginActivity)
- User enters email and password
- App calls POST `/api/admin-portal/login-admin`
- On success (200):
  - Receives token from response
  - Saves token using `TokenManager.saveToken(context, token)`
  - Navigates to HomeActivity
- On error:
  - Displays error message to user

### 3. Home Screen (HomeActivity)
- Displays welcome message
- Shows partial token (for verification)
- Provides logout functionality
- Protected: redirects to login if not authenticated

## Token Storage

The `TokenManager` utility handles all token operations:

```kotlin
// Save token after successful login
TokenManager.saveToken(context, token)

// Get saved token
val token = TokenManager.getToken(context)

// Check if user is logged in
val isLoggedIn = TokenManager.isLoggedIn(context)

// Clear token (soft logout)
TokenManager.clearToken(context)

// Complete logout (clears all preferences)
TokenManager.logout(context)
```

## Using Token for Other API Calls

### Method 1: Manual Token Addition

```kotlin
// In your API service interface
@Headers("accept: application/json")
@GET("api/some-endpoint")
suspend fun getSomeData(
    @Header("Authorization") authorization: String
): Response<YourDataModel>

// When calling the API
val token = TokenManager.getToken(context)
val response = RetrofitClient.apiService.getSomeData("Bearer $token")
```

### Method 2: Using AuthInterceptor (Recommended)

The `AuthInterceptor` automatically adds the token to all requests:

```kotlin
// Update RetrofitClient.kt to use AuthInterceptor
private fun createOkHttpClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context)) // Adds token automatically
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}

// Then your API calls don't need manual token addition
@GET("api/some-endpoint")
suspend fun getSomeData(): Response<YourDataModel>
```

### Method 3: Context-Aware Retrofit Instance

Create a function to get Retrofit with context:

```kotlin
object RetrofitClient {
    private const val BASE_URL = "https://luna-api-dev.solutions-apps.com/"

    fun getApiService(context: Context): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// Usage
val response = RetrofitClient.getApiService(context).getSomeData()
```

## Example: Adding a New API Endpoint

### 1. Add to ApiService.kt

```kotlin
@GET("api/scanner/devices")
suspend fun getDevices(): Response<DevicesResponse>

@POST("api/scanner/scan")
suspend fun performScan(@Body scanRequest: ScanRequest): Response<ScanResponse>
```

### 2. Create Data Models

```kotlin
data class DevicesResponse(
    @SerializedName("data")
    val data: List<Device>
)

data class Device(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)
```

### 3. Call from Activity/Composable

```kotlin
scope.launch {
    try {
        val token = TokenManager.getToken(context)
        val response = RetrofitClient.apiService.getDevices()
        
        if (response.isSuccessful && response.code() == 200) {
            val devices = response.body()?.data
            // Use the devices data
        } else {
            // Handle error
        }
    } catch (e: Exception) {
        // Handle network error
    }
}
```

## Handling Token Expiration

If your API returns 401 (Unauthorized), the token may have expired:

```kotlin
if (response.code() == 401) {
    // Token expired - logout and redirect to login
    TokenManager.logout(context)
    context.startActivity(Intent(context, LoginActivity::class.java))
    (context as? Activity)?.finish()
}
```

## Security Best Practices

1. **Never log tokens in production** - Remove logging interceptor in release builds
2. **Use HTTPS only** - Already configured in BASE_URL
3. **Clear token on logout** - Use `TokenManager.logout()`
4. **Validate token before use** - Check if token exists before API calls
5. **Handle token refresh** - Implement if your API supports refresh tokens

## Current API Configuration

- **Base URL**: `https://luna-api-dev.solutions-apps.com/`
- **Login Endpoint**: `POST /api/admin-portal/login-admin`
- **Headers**: 
  - `accept: application/json, text/plain, */*`
  - `content-type: application/json`
  - `x-panel: true`
- **Token Type**: Bearer token
- **Storage**: SharedPreferences (encrypted recommended for production)

## Next Steps

1. Add more API endpoints to `ApiService.kt`
2. Implement token refresh mechanism if available
3. Add encrypted SharedPreferences for production
4. Implement proper error handling for all API calls
5. Add loading states and error messages in UI
