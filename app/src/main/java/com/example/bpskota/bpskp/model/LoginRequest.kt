package com.example.bpskota.bpskp.model

data class LoginRequest(
    val username: String,
    val password: String
)
data class LoginResponse(
    val success: Boolean = false,
    val message: String? = null,
    val token: String? = null,
    val user: UserData? = null
)

data class UserData(
    val id: Int,
    val name: String,
    val username: String,
    val role: String
)