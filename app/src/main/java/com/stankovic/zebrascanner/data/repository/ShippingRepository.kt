package com.stankovic.zebrascanner.data.repository

import AppException
import com.stankovic.zebrascanner.core.Result
import com.stankovic.zebrascanner.data.local.LocalDataSource
import com.stankovic.zebrascanner.network.ApiClient
import com.stankovic.zebrascanner.network.MarkShippedRequest
import com.stankovic.zebrascanner.network.MarkShippedResponse
import org.json.JSONObject
import javax.inject.Inject

/**
 * Repository for shipping-related API calls following the repository pattern
 */
interface ShippingRepository {
    suspend fun markShipped(trackingNumber: String): Result<MarkShippedResponse>
}

class ShippingRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val localDataSource: LocalDataSource
) : ShippingRepository {

    override suspend fun markShipped(trackingNumber: String): Result<MarkShippedResponse> {
        return try {
            val token = localDataSource.getAuthToken()
            val authHeader = if (!token.isNullOrEmpty()) "Bearer $token" else null

            val response = apiClient.service.markShipped(
                authHeader,
                MarkShippedRequest(trackingNumber)
            )

            when {
                response.isSuccessful && response.body() != null -> {
                    Result.Success(response.body()!!)
                }
                response.code() == 422 -> {
                    val errorBody = response.errorBody()?.string() ?: "Validation error"
                    try {
                        val json = JSONObject(errorBody)
                        val message = if (json.has("tracking_number")) {
                            json.optJSONArray("tracking_number")?.optString(0)
                                ?: "Invalid tracking number"
                        } else {
                            "Validation error"
                        }
                        Result.Error(AppException.ValidationException(message))
                    } catch (e: Exception) {
                        Result.Error(AppException.ValidationException("Validation error"))
                    }
                }
                response.code() == 409 -> {
                    val errorBody = response.errorBody()?.string() ?: "Conflict"
                    try {
                        val json = JSONObject(errorBody)
                        val message = json.optJSONObject("data")?.optString("message")
                            ?: "This order has already been shipped"
                        Result.Error(AppException.ConflictException(message))
                    } catch (e: Exception) {
                        Result.Error(AppException.ConflictException("This order has already been shipped"))
                    }
                }
                else -> {
                    val message = "Server returned ${response.code()}"
                    Result.Error(AppException.ServerException(response.code(), message))
                }
            }
        } catch (e: Exception) {
            Result.Error(
                if (e is AppException) e else AppException.NetworkException(e.message ?: "Network error")
            )
        }
    }
}
