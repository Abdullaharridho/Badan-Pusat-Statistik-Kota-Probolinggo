package com.example.bpskota.bpskp.model

data class SuratMasukResponse (
    val status: String?,
    val message: String?,
    val data: SuratMasuk?)
data class SuratMasukActionResponse(
    val status: String?,
    val message: String?,
    val data: SuratMasuk? = null
)
data class SuratMasukStatusRequest(
    val status: String
)


data class SuratMasuk(
    val id: Int? = null,
    val nomor_surat: String? = null,
    val tanggal_surat: String? = null,
    val tanggal_diterima: String? = null,
    val asal_surat: String? = null,
    val perihal: String? = null,
    val isi_ringkas: String? = null,
    val tanggal_acara: String? = null,
    val waktu_mulai: String? = null,
    val waktu_selesai: String? = null,
    val lokasi: String? = null,
    val file_surat: String? = null,
    val file_url: String? = null,
    val status: String? = null,
    val dicatat_oleh: UserData? = null
)
data class SuratMasukRequest(
    val nomor_surat: String,
    val tanggal_surat: String,
    val tanggal_diterima: String,
    val asal_surat: String,
    val perihal: String,
    val isi_ringkas: String? = null,
    val tanggal_acara: String? = null,
    val waktu_mulai: String? = null,
    val waktu_selesai: String? = null,
    val lokasi: String? = null,
    val status: String? = null
)
data class SuratMasukListResponse(
    val status: String?,
    val message: String?,
    val data: List<SuratMasuk>?
)