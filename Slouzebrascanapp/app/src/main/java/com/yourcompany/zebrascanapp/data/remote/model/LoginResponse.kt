package com.yourcompany.zebrascanapp.data.remote.model


data class LoginResponse(
    val token: String,
    val token_key: String,
    val message: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val full_name: String?,
    val username: String?,
    val email: String?,
    val role: RoleDto?
)

data class RoleDto(
    val id: Int?,
    val name: String?
)