package com.example.bpskota.model

data class PemesananRuanganResponse(
    val success: Boolean?,
    val message: String?,
    val data: List<PemesananRuangan>?
)

data class PemesananRuanganDetailResponse(
    val success: Boolean?,
    val message: String?,
    val data: PemesananRuangan?
)

data class PemesananRuanganRequest(
    val ruangan_id: Int,
    val nama_pemesan: String,
    val instansi: String?,
    val judul_kegiatan: String,
    val tanggal: String,
    val waktu_mulai: String,
    val waktu_selesai: String,
    val catatan: String?
)

data class PemesananRuangan(
    val id: Int?,
    val ruangan_id: Int?,
    val nama_ruangan: String?,
    val lokasi: String?,
    val kapasitas: Int?,
    val nama_pemesan: String?,
    val instansi: String?,
    val judul_kegiatan: String?,
    val tanggal: String?,
    val waktu_mulai: String?,
    val waktu_selesai: String?,
    val status: String?,
    val catatan: String?,
    val created_at: String?,
    val updated_at: String?
)

data class PemesananRuanganMutationResponse(
    val success: Boolean?,
    val message: String?
)