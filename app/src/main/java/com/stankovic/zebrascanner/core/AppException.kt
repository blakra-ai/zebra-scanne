sealed class AppException(
    open val errorMessage: String? = null,
    cause: Throwable? = null
) : Exception(errorMessage, cause) {

    data class NetworkException(
        override val errorMessage: String
    ) : AppException(errorMessage)

    data class AuthenticationException(
        override val errorMessage: String
    ) : AppException(errorMessage)

    data class ValidationException(
        override val errorMessage: String
    ) : AppException(errorMessage)

    data class ConflictException(
        override val errorMessage: String
    ) : AppException(errorMessage)

    data class ServerException(
        val code: Int,
        override val errorMessage: String
    ) : AppException(errorMessage)

    data class UnknownException(
        val error: Throwable
    ) : AppException(error.message, error)


}
