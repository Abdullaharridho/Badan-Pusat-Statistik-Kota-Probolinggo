package com.example.bpskota.bpskp.model

data class RuanganResponse(
    val success: Boolean?,
    val message: String?,
    val data: List<Ruangan>?
)

data class RuanganDetailResponse(
    val success: Boolean?,
    val message: String?,
    val data: Ruangan?
)

data class RuanganRequest(
    val nama_ruangan: String,
    val lokasi: String?,
    val kapasitas: Int?,
    val keterangan: String?,
    val status: String
)

data class Ruangan(
    val id: Int,
    val nama_ruangan: String?,
    val lokasi: String?,
    val kapasitas: Int?,
    val keterangan: String?,
    val status: String?,
    val created_at: String?,
    val updated_at: String?
)

data class RuanganMutationResponse(
    val success: Boolean?,
    val message: String?
)