package com.example.bpskota.bpskp.model

import com.google.gson.annotations.SerializedName

data class KegiatanHarian(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("tanggal")
    val tanggal: String,

    @SerializedName("nama_kegiatan")
    val namaKegiatan: String,

    @SerializedName("jam_mulai")
    val jamMulai: String,

    @SerializedName("jam_selesai")
    val jamSelesai: String
)

data class KegiatanHarianListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<KegiatanHarian>? = null
)

data class KegiatanHarianDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: KegiatanHarian? = null
)

data class KegiatanHarianMutationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: KegiatanHarian? = null,

    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)

data class KegiatanHarianRequest(
    @SerializedName("nama_kegiatan")
    val namaKegiatan: String,

    @SerializedName("jam_mulai")
    val jamMulai: String,

    @SerializedName("jam_selesai")
    val jamSelesai: String,

    @SerializedName("tanggal")
    val tanggal: String
)