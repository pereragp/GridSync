package com.gridsync.mobile.data.remote.dto

data class LoginRequestDto(
    val email: String,
    val password: String,
)

data class LoginResponseDto(
    val token: String = "",
    val tokenType: String = "Bearer",
    val expiresAt: String? = null,
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "",
    val nic: String? = null,
)

data class RegisterProsumerRequestDto(
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val address: String? = null,
)

data class UserResponseDto(
    val id: String = "",
    val nic: String? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val status: String = "",
    val address: String? = null,
)

data class ApiMessageDto(
    val message: String? = null,
)
