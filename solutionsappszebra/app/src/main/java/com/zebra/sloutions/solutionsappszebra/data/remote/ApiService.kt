import retrofit2.http.Body
import retrofit2.http.POST

import retrofit2.Response


data class MarkShippedRequest(val tracking_number: String)

data class MarkShippedResponse(
    val data: MarkShippedData?
)

data class MarkShippedData(
    val message: String,
    val order_number: String?,
    val shipped_on: String?,
    val tracking_number: String?
)

interface ApiService {

    @POST("api/admin-portal/orders/mark-shipped")
    suspend fun markShipped(
        @Body body: MarkShippedRequest
    ): Response<MarkShippedResponse>
}