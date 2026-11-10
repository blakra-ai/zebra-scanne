package com.yourcompany.zebrascanapp.data.repository
import com.yourcompany.zebrascanapp.data.remote.ScanApi
import com.yourcompany.zebrascanapp.data.remote.model.ScanRequest
import com.yourcompany.zebrascanapp.data.remote.model.ScanResponse
import javax.inject.Inject

class ScanRepository @Inject constructor(
    private val scanApi: ScanApi
) {

    suspend fun markShipped(trackingNumber: String): Result<ScanResponse> {
        return try {
            val response = scanApi.markShipped(ScanRequest(trackingNumber))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
