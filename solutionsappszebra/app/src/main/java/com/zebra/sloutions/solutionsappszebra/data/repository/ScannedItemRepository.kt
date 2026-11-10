package com.zebra.sloutions.solutionsappszebra.data.repository


import MarkShippedRequest
import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScannedItemEntity
import com.zebra.sloutions.solutionsappszebra.data.remote.RetrofitClient
import com.zebra.solutionsappszebra.data.local.dao.ScannedItemDao
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannedItemRepository @Inject constructor(
    private val dao: ScannedItemDao
) {

    fun getAllScannedItems(): Flow<List<ScannedItemEntity>> = dao.getAll()

    suspend fun scanAndMarkShipped(trackingNumber: String): Result<ScannedItemEntity> {
        try {
            val response = RetrofitClient.apiService.markShipped(MarkShippedRequest(trackingNumber))

            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    val item = ScannedItemEntity(
                        trackingNumber = data.tracking_number ?: trackingNumber,
                        orderNumber = data.order_number ?: "",
                        shippedOn = data.shipped_on ?: ""
                    )
                    dao.insert(item)
                    return Result.success(item)
                } else {
                    return Result.failure(Exception("API returned null data"))
                }
            } else {
                // handle HTTP errors
                return Result.failure(HttpException(response))
            }

        } catch (e: IOException) {
            return Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}