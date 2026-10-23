package com.example.bpskota.bpskp.model

import com.google.gson.annotations.SerializedName

// =====================================================
// MODEL DATA KODE RUMAH
// =====================================================

data class KodeRumah(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nama_kecamatan")
    val namaKecamatan: String? = null,

    @SerializedName("nama_kelurahan")
    val namaKelurahan: String? = null,

    @SerializedName("nama_rt_rw")
    val namaRtRw: String? = null,

    @SerializedName("idsubsls")
    val idsubsls: String? = null,

    @SerializedName("no_bangunan")
    val noBangunan: String? = null,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("nama_kk")
    val namaKk: String? = null,

    @SerializedName("nama_usaha")
    val namaUsaha: String? = null,

    @SerializedName("status_data")
    val statusData: String? = null,

    @SerializedName("created_by")
    val createdBy: Int? = null,

    @SerializedName("updated_by")
    val updatedBy: Int? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

// =====================================================
// REQUEST TAMBAH DAN UBAH KODE RUMAH
// =====================================================

data class KodeRumahRequest(
    @SerializedName("nama_kecamatan")
    val namaKecamatan: String? = null,

    @SerializedName("nama_kelurahan")
    val namaKelurahan: String? = null,

    @SerializedName("nama_rt_rw")
    val namaRtRw: String? = null,

    @SerializedName("idsubsls")
    val idsubsls: String? = null,

    @SerializedName("no_bangunan")
    val noBangunan: String? = null,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("nama_kk")
    val namaKk: String? = null,

    @SerializedName("nama_usaha")
    val namaUsaha: String? = null
)

// =====================================================
// REQUEST RETAG KOORDINAT
// =====================================================

data class KodeRumahRetagRequest(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)

// =====================================================
// PAGINATION BERSAMA
// Dipakai daftar kode rumah dan daftar log
// =====================================================

data class KodeRumahPagination(
    @SerializedName("current_page")
    val currentPage: Int? = null,

    @SerializedName("per_page")
    val perPage: Int? = null,

    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("last_page")
    val lastPage: Int? = null
)

// =====================================================
// RESPONSE DAFTAR KODE RUMAH
// JSON: success, message, data[], pagination{}
// =====================================================

data class KodeRumahListResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<KodeRumah> = emptyList(),

    @SerializedName("pagination")
    val pagination: KodeRumahPagination? = null
)

// =====================================================
// RESPONSE DETAIL KODE RUMAH
// =====================================================

data class KodeRumahDetailResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: KodeRumah? = null
)

// =====================================================
// RESPONSE MUTASI
// Tambah, ubah, retag, dan hapus
// =====================================================

data class KodeRumahMutationResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: KodeRumah? = null,

    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)

// =====================================================
// MODEL LOG / RIWAYAT KODE RUMAH
// =====================================================

data class KodeRumahLog(
    @SerializedName("id")
    val id: Int,

    @SerializedName("kode_rumah_id")
    val kodeRumahId: Int? = null,

    @SerializedName("idsubsls")
    val idsubsls: String? = null,

    @SerializedName("no_bangunan")
    val noBangunan: String? = null,

    @SerializedName("aksi")
    val aksi: String? = null,

    @SerializedName("data_sebelum")
    val dataSebelum: String? = null,

    @SerializedName("data_sesudah")
    val dataSesudah: String? = null,

    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("nama_pengguna")
    val namaPengguna: String? = null,

    @SerializedName("keterangan")
    val keterangan: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

// =====================================================
// RESPONSE DAFTAR RIWAYAT KODE RUMAH
// JSON: success, message, data[], pagination{}
// =====================================================

data class KodeRumahLogListResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<KodeRumahLog> = emptyList(),

    @SerializedName("pagination")
    val pagination: KodeRumahPagination? = null
)

// =====================================================
// RESPONSE DETAIL RIWAYAT
// =====================================================

data class KodeRumahLogDetailResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: KodeRumahLog? = null
)