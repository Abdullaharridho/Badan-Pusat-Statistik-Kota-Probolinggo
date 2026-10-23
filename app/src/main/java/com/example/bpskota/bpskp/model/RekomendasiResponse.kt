package com.example.bpskota.bpskp.model

import com.google.gson.annotations.SerializedName

/**
 * Data utama rekomendasi pengalihan penugasan.
 */
data class RekomendasiData(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("penugasan_id")
    val penugasanId: Long = 0L,

    @SerializedName("pegawai_asal_id")
    val pegawaiAsalId: Int = 0,

    @SerializedName("pegawai_rekomendasi_id")
    val pegawaiRekomendasiId: Int = 0,

    @SerializedName("alasan")
    val alasan: String? = null,

    @SerializedName("status")
    val status: String = "menunggu_review",

    @SerializedName("diproses_oleh")
    val diprosesOleh: Int? = null,

    @SerializedName("diproses_pada")
    val diprosesPada: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("catatan_penugasan")
    val catatanPenugasan: String? = null,

    @SerializedName("nama_pegawai_asal")
    val namaPegawaiAsal: String? = null,

    @SerializedName("nama_pegawai_rekomendasi")
    val namaPegawaiRekomendasi: String? = null
)

/**
 * Request untuk mengajukan rekomendasi pengalihan penugasan.
 */
data class RekomendasiRequest(
    @SerializedName("penugasan_id")
    val penugasanId: Long,

    @SerializedName("pegawai_rekomendasi_id")
    val pegawaiRekomendasiId: Int,

    @SerializedName("alasan")
    val alasan: String
)

/**
 * Request untuk mengubah rekomendasi yang masih menunggu review.
 * Field dapat dikirim sebagian sesuai kebutuhan endpoint update.
 */
data class RekomendasiUpdateRequest(
    @SerializedName("pegawai_rekomendasi_id")
    val pegawaiRekomendasiId: Int? = null,

    @SerializedName("alasan")
    val alasan: String? = null
)

/**
 * Request ketua tim untuk menerima atau menolak rekomendasi.
 */
data class RekomendasiProsesRequest(
    @SerializedName("status")
    val status: String
)

/**
 * Response daftar rekomendasi.
 */
data class RekomendasiListResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<RekomendasiData>? = null
)

/**
 * Response detail atau hasil pengajuan rekomendasi.
 */
data class RekomendasiDetailResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: RekomendasiData? = null
)

/**
 * Response umum untuk create, update, proses, dan delete.
 * Data dibuat nullable karena endpoint proses/delete dapat hanya
 * mengembalikan success dan message.
 */
data class RekomendasiMutationResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: RekomendasiData? = null
)