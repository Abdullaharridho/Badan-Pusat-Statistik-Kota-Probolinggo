
package com.example.bpskota.bpskp.model

data class SuratMasukResponse(
    val status: String?,
    val message: String?,
    val data: SuratMasuk?
)

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

    // Informasi tim
    val tim_id: Int? = null,
    val nama_tim: String? = null,

    // Informasi ketua tim
    val ketua_id: Int? = null,
    val nama_ketua: String? = null,
    val hp_ketua: String? = null,

    // Informasi pencatat
    val pencatat: PencatatSurat? = null,

    // Tetap dipertahankan jika masih digunakan
    // oleh bagian Android yang lama.
    val dicatat_oleh: UserData? = null
)

data class PencatatSurat(
    val id: Int? = null,
    val nama: String? = null
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
    val status: String? = null,

    // Tim yang ditugaskan untuk menangani surat.
    val tim_id: Int
)

data class SuratMasukListResponse(
    val status: String?,
    val message: String?,
    val data: List<SuratMasuk>?
)
