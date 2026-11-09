package com.example.bpskota.bpskp.model

import com.google.gson.annotations.SerializedName

/**
 * Model data tim.
 * Digunakan untuk response daftar tim, detail tim,
 * dan hasil operasi pembuatan atau perubahan tim.
 */
data class Tim(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nama_tim")
    val namaTim: String,

    @SerializedName("ketua_id")
    val ketuaId: Int,

    @SerializedName("nama_ketua")
    val namaKetua: String? = null,

    @SerializedName("role_ketua_sebelumnya")
    val roleKetuaSebelumnya: String? = null,

    @SerializedName("keterangan")
    val keterangan: String? = null,

    @SerializedName("status")
    val status: String,

    @SerializedName("anggota")
    val anggota: List<AnggotaTim> = emptyList(),

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * Model anggota tim.
 */
data class AnggotaTim(
    @SerializedName("id_keanggotaan")
    val idKeanggotaan: Int,

    @SerializedName("pegawai_id")
    val pegawaiId: Int,

    @SerializedName("name")
    val nama: String,

    @SerializedName("role")
    val role: String
)

/**
 * Response untuk GET /tim.
 */
data class TimListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<Tim> = emptyList()
)

/**
 * Response untuk GET /tim/{id}.
 */
data class TimDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: Tim? = null
)

/**
 * Response untuk operasi pembuatan, perubahan,
 * dan penghapusan tim maupun anggota.
 */
data class TimMutationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: Tim? = null
)

/**
 * Request untuk membuat tim.
 */
data class TimRequest(
    @SerializedName("nama_tim")
    val namaTim: String,

    @SerializedName("ketua_id")
    val ketuaId: Int,

    @SerializedName("keterangan")
    val keterangan: String? = null,

    @SerializedName("status")
    val status: String = "aktif"
)

/**
 * Request untuk memperbarui tim.
 */
data class TimUpdateRequest(
    @SerializedName("nama_tim")
    val namaTim: String? = null,

    @SerializedName("ketua_id")
    val ketuaId: Int? = null,

    @SerializedName("keterangan")
    val keterangan: String? = null,

    @SerializedName("status")
    val status: String? = null
)

/**
 * Request untuk menambahkan anggota tim.
 */
data class TambahAnggotaRequest(
    @SerializedName("pegawai_id")
    val pegawaiId: Int
)

/**
 * Response untuk GET /tim/pegawai-tersedia.
 */
data class PegawaiTersediaResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<PegawaiTersedia> = emptyList()
)

/**
 * Model pegawai yang tersedia untuk menjadi anggota tim.
 */
data class PegawaiTersedia(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val nama: String,

    @SerializedName("role")
    val role: String
)