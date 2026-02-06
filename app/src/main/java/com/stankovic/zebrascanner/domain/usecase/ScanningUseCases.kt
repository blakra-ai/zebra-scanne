package com.stankovic.zebrascanner.domain.usecase

import com.stankovic.zebrascanner.core.Result
import com.stankovic.zebrascanner.data.local.LocalDataSource
import com.stankovic.zebrascanner.data.repository.ShippingRepository
import com.stankovic.zebrascanner.network.MarkShippedResponse
import javax.inject.Inject

/**
 * UseCase for marking an order as shipped - contains business logic
 */
class MarkShippedUseCase @Inject constructor(
    private val shippingRepository: ShippingRepository,
    private val localDataSource: LocalDataSource
) {
    suspend operator fun invoke(trackingNumber: String): Result<MarkShippedResponse> {
        return when (val result = shippingRepository.markShipped(trackingNumber)) {
            is Result.Success -> {
                localDataSource.incrementTotalScans()
                localDataSource.incrementSuccessScans()
                Result.Success(result.data)
            }
            is Result.Error -> {
                localDataSource.incrementTotalScans()
                localDataSource.incrementFailureScans()
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }
}

/**
 * UseCase for getting user information
 */
class GetUserInfoUseCase @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    operator fun invoke(): UserInfo {
        return UserInfo(
            name = localDataSource.getUserName() ?: "Unknown",
            email = localDataSource.getUserEmail() ?: "Unknown"
        )
    }
}

/**
 * UseCase for getting scan statistics
 */
class GetScanStatsUseCase @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    operator fun invoke(): ScanStats {
        val (total, success, failure) = localDataSource.getScanCount()
        return ScanStats(total, success, failure)
    }
}

/**
 * UseCase for clearing history and stats
 */
class ClearScanHistoryUseCase @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    operator fun invoke() {
        localDataSource.saveScanCount(0, 0, 0)
    }
}

/**
 * UseCase for initializing auth data
 */
class InitializeAuthUseCase @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    operator fun invoke(token: String, userName: String, email: String) {
        localDataSource.saveAuthToken(token)
        localDataSource.saveUserInfo(userName, email)
    }
}

// Data classes for UseCases
data class UserInfo(
    val name: String,
    val email: String
)

data class ScanStats(
    val total: Int,
    val success: Int,
    val failure: Int
)
