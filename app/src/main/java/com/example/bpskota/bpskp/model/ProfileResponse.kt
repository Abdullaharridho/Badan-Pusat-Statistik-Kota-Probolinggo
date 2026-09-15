package com.example.bpskota.bpskp.model


data class ProfileRequest(
    val name: String,
    val username: String
)


// Data yang dikirim ketika user mengubah password
data class PasswordUpdateRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String
)


// Response dari endpoint profile
data class ProfileResponse(
    val success: Boolean?,
    val message: String?,
    val requires_login: Boolean?,
    val user: ProfileUserData?
)


// Data user yang dikembalikan endpoint profile
data class ProfileUserData(
    val id: Int?,
    val name: String?,
    val username: String?,
    val role: String?
)