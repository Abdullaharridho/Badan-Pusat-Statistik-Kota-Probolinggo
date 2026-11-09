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
    val jenis_kelamin: String? = null,
    val id_jabatan: Int? = null,
    val role: String,
    val nip: String? = null,
    val no_hp: String? = null,
    val golongan: String? = null,
    val status: String? = null
)

data class UserManagementData(
    val id: Int?,
    val name: String?,
    val username: String?,
    val jenis_kelamin: String?,
    val id_jabatan: Int?,
    val nama_jabatan: String?,
    val role: String?,
    val nip: String?,
    val no_hp: String?,
    val golongan: String?,
    val status: String?,
    val created_at: String?,
    val updated_at: String?
)

// ===============================
// RESPONSE DATA JABATAN
// ===============================

data class JabatanResponse(
    val success: Boolean?,
    val message: String?,
    val data: List<JabatanData>?
)

data class JabatanData(
    val id: Int?,
    val nama_jabatan: String?
)