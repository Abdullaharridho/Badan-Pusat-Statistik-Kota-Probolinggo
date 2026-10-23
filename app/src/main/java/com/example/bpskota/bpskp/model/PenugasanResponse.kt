package com.example.bpskota.bpskp.model

import com.google.gson.annotations.SerializedName

/**
 * Pembungkus respons standar Laravel.
 *
 * Digunakan untuk respons daftar, detail, dan pembuatan penugasan.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null
)

/**
 * Model utama penugasan.
 *
 * Memuat kolom tb_penugasan serta informasi surat
 * dan nama pengguna hasil JOIN pada endpoint daftar/detail.
 *
 * Field nullable karena tidak semua endpoint mengembalikan
 * seluruh informasi JOIN.
 */
data class PenugasanResponse(

    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("surat_masuk_id")
    val suratMasukId: Long? = null,

    @SerializedName("ditugaskan_oleh")
    val ditugaskanOleh: Long? = null,

    @SerializedName("ditugaskan_kepada")
    val ditugaskanKepada: Long? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("catatan_penugasan")
    val catatanPenugasan: String? = null,

    @SerializedName("waktu_penugasan")
    val waktuPenugasan: String? = null,

    @SerializedName("waktu_respon")
    val waktuRespon: String? = null,

    @SerializedName("catatan_respon")
    val catatanRespon: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("tanggal_acara")
    val tanggalAcara: String? = null,

    @SerializedName("waktu_mulai")
    val waktuMulai: String? = null,

    @SerializedName("waktu_selesai")
    val waktuSelesai: String? = null,

    @SerializedName("lokasi")
    val lokasi: String? = null,

    // Informasi hasil JOIN dengan tb_surat_masuk
    @SerializedName("nomor_surat")
    val nomorSurat: String? = null,

    @SerializedName("perihal")
    val perihal: String? = null,

    // Informasi hasil JOIN dengan tabel users
    @SerializedName("nama_pengirim")
    val namaPengirim: String? = null,

    @SerializedName("nama_penerima")
    val namaPenerima: String? = null



)

/**
 * Request untuk membuat penugasan surat.
 *
 * Backend akan menentukan:
 * - ditugaskan_oleh berdasarkan pengguna yang login
 * - status penugasan = menunggu_respon
 * - waktu_penugasan
 * - status surat = diproses
 */
data class CreatePenugasanRequest(

    @SerializedName("surat_masuk_id")
    val suratMasukId: Long,

    @SerializedName("ditugaskan_kepada")
    val ditugaskanKepada: Long,

    @SerializedName("catatan_penugasan")
    val catatanPenugasan: String? = null
)
data class ResponPenugasanRequest(
    @SerializedName("status")
    val status: String,

    @SerializedName("catatan_respon")
    val catatanRespon: String? = null
)
data class PenerimaBentrokResponse(
    val success: Boolean,
    val message: String,
    val data: PenerimaBentrokData?
)

data class PenerimaBentrokData(
    @SerializedName("user_ids")
    val userIds: List<Int> = emptyList()
)