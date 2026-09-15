package com.example.bpskota.bpskp.model

data class UserResponse(
    val success: Boolean?,
    val message: String?,
    val data: List<UserManagementData>?
)

data class UserDetailResponse(
    val success: Boolean?,
    val message: String?,
    val data: UserManagementData?
)

data class UserActionResponse(
    val success: Boolean?,
    val message: String?,
    val data: UserManagementData?
)

data class UserRequest(
    val name: String,
    val username: String,
    val password: String? = null,
    val role: String
)

data class UserManagementData(
    val id: Int?,
    val name: String?,
    val username: String?,
    val role: String?,
    val created_at: String?,
    val updated_at: String?
)