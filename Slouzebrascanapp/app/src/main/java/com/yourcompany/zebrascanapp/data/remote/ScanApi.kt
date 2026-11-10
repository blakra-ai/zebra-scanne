package com.yourcompany.zebrascanapp.data.remote


import com.yourcompany.zebrascanapp.data.remote.model.ScanRequest
import com.yourcompany.zebrascanapp.data.remote.model.ScanResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ScanApi {

    @POST("api/admin-portal/orders/mark-shipped")
    suspend fun markShipped(
        @Body request: ScanRequest
    ): Response<ScanResponse>
}
