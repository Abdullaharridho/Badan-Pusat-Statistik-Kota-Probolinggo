package com.example.bpskota.bpskp.model

import com.google.gson.annotations.SerializedName

/**
 * Respons GET daftar wilayah.
 */
data class MasterWilayahResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<MasterWilayah>? = null,

    @SerializedName("pagination")
    val pagination: MasterWilayahPagination? = null,

    @SerializedName("filters")
    val filters: MasterWilayahFilters? = null
)

/**
 * Respons GET detail, POST, dan PUT wilayah.
 */
data class MasterWilayahDetailResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: MasterWilayah? = null
)

/**
 * Model untuk kecamatan, kelurahan, dan RT/RW.
 */
data class MasterWilayah(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("kode_kecamatan")
    val kodeKecamatan: String? = null,

    @SerializedName("nama_kecamatan")
    val namaKecamatan: String? = null,

    @SerializedName("kode_kelurahan")
    val kodeKelurahan: String? = null,

    @SerializedName("nama_kelurahan")
    val namaKelurahan: String? = null,

    @SerializedName("kode_wilayah")
    val kodeWilayah: String? = null,

    @SerializedName("nama_wilayah")
    val namaWilayah: String? = null,

    @SerializedName("kecamatan_id")
    val kecamatanId: Int? = null,

    @SerializedName("kelurahan_id")
    val kelurahanId: Int? = null,

    @SerializedName("is_active")
    val isActive: Int? =null,

    @SerializedName("jumlah_kelurahan")
    val jumlahKelurahan: Int? = null,

    @SerializedName("jumlah_rtrw")
    val jumlahRtrw: Int? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    /**
     * Mengambil kode yang sesuai dengan level wilayah.
     */
    fun getKode(): String {
        return kodeKecamatan
            ?: kodeKelurahan
            ?: kodeWilayah
            ?: "-"
    }

    /**
     * Mengambil nama yang sesuai dengan level wilayah.
     */
    fun getNama(): String {
        return namaKecamatan
            ?: namaKelurahan
            ?: namaWilayah
            ?: "-"
    }

    /**
     * Jumlah anak:
     * Kecamatan -> jumlah kelurahan
     * Kelurahan -> jumlah RT/RW
     * RT/RW -> null
     */
    fun getJumlahAnak(): Int? {
        return jumlahKelurahan ?: jumlahRtrw
    }
}

/**
 * Request POST dan PUT.
 *
 * Gunakan field sesuai level:
 * kecamatan, kelurahan, atau rtrw.
 */
data class MasterWilayahRequest(
    @SerializedName("kode_kecamatan")
    val kodeKecamatan: String? = null,

    @SerializedName("nama_kecamatan")
    val namaKecamatan: String? = null,

    @SerializedName("kode_kelurahan")
    val kodeKelurahan: String? = null,

    @SerializedName("nama_kelurahan")
    val namaKelurahan: String? = null,

    @SerializedName("kode_wilayah")
    val kodeWilayah: String? = null,

    @SerializedName("nama_wilayah")
    val namaWilayah: String? = null,

    @SerializedName("kecamatan_id")
    val kecamatanId: Int? = null,

    @SerializedName("kelurahan_id")
    val kelurahanId: Int? = null,

    @SerializedName("is_active")
    val isActive: Boolean = true
)

/**
 * Respons DELETE.
 */
data class MasterWilayahDeleteResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Informasi pagination dari API.
 */
data class MasterWilayahPagination(
    @SerializedName("current_page")
    val currentPage: Int = 1,

    @SerializedName("per_page")
    val perPage: Int = 10,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("last_page")
    val lastPage: Int = 1
)

/**
 * Filter yang dikembalikan oleh API.
 */
data class MasterWilayahFilters(
    @SerializedName("q")
    val q: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("sort")
    val sort: String? = null,

    @SerializedName("order")
    val order: String? = null
)