package com.example.bpskota.bpskp.model

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class LaporanListResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<LaporanData>? = null
)

data class LaporanDetailResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: LaporanData? = null
)

data class LaporanActionResponse(
    val success: Boolean = false,
    val message: String? = null
)

data class LaporanData(
    val id: Int? = null,

    @SerializedName("penugasan_id")
    val penugasanId: Int? = null,

    val ringkasan: String? = null,

    @SerializedName("hasil_kegiatan")
    val hasilKegiatan: String? = null,

    val catatan: String? = null,

    @SerializedName("dilaporkan_oleh")
    val dilaporkanOleh: Int? = null,

    @SerializedName("tanggal_laporan")
    val tanggalLaporan: String? = null,

    val pelapor: String? = null,

    val dokumen: List<LaporanDokumenData>? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class LaporanDokumenData(
    val id: Int? = null,

    @SerializedName("laporan_kegiatan_id")
    val laporanKegiatanId: Int? = null,

    @SerializedName("diunggah_oleh")
    val diunggahOleh: Int? = null,

    @SerializedName("jenis_dokumen")
    val jenisDokumen: String? = null,

    @SerializedName("nama_file")
    val namaFile: String? = null,

    @SerializedName("path_file")
    val pathFile: String? = null,

    @SerializedName("mime_type")
    val mimeType: String? = null,

    @SerializedName("ukuran_file")
    val ukuranFile: Long? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * Data file dari Android untuk proses upload laporan.
 * Ini model request lokal, bukan respons dari Laravel.
 */
data class LaporanDokumenUpload(
    val uri: Uri,
    val jenisDokumen: String,
    val namaFile: String? = null
)