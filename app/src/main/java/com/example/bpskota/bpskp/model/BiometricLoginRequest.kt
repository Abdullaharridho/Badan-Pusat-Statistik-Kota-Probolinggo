package com.example.bpskota.bpskp.model

data class BiometricLoginRequest(
    val credential_id: String
)
data class BiometricRegisterRequest(
    val credential_id: String,
    val device_name: String?
)
data class BiometricRegisterResponse(
    val success: Boolean,
    val message: String?,
    val credential: BiometricCredentialResponse?
)

data class BiometricCredentialResponse(
    val id: Int,
    val credential_id: String,
    val device_name: String?,
    val is_active: Boolean
)