package com.example.bpskota.bps.model

import com.google.gson.annotations.SerializedName

/**
 * ============================================================
 * RESPONSE UTAMA SIMDASI
 * ============================================================
 */
data class SimdasiDetailResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: List<SimdasiDetailData>?
)


/**
 * ============================================================
 * DATA DETAIL / PAGINATION SIMDASI
 * ============================================================
 *
 * API BPS mengembalikan dua jenis object di dalam "data":
 *
 * Object 1:
 * {
 *   "page": 1,
 *   "pages": 1,
 *   "per_page": 1,
 *   "count": 1,
 *   "total": 1
 * }
 *
 * Object 2:
 * {
 *   "judul_tabel": "...",
 *   "kolom": {...},
 *   "data": [...]
 * }
 *
 * Karena keduanya berada dalam array "data",
 * field dibuat nullable.
 */
data class SimdasiDetailData(

    // ========================================================
    // PAGINATION
    // ========================================================

    @SerializedName("page")
    val page: Int?,

    @SerializedName("pages")
    val pages: Int?,

    @SerializedName("per_page")
    val perPage: Int?,

    @SerializedName("count")
    val count: Int?,

    @SerializedName("total")
    val total: Int?,


    // ========================================================
    // DETAIL TABEL
    // ========================================================

    @SerializedName("change_log")
    val changeLog: List<Any>?,

    @SerializedName("judul_tabel")
    val judulTabel: String?,

    @SerializedName("judul_tabel_en")
    val judulTabelEn: String?,

    @SerializedName("lingkup")
    val lingkup: String?,

    @SerializedName("lingkup_id")
    val lingkupId: String?,

    @SerializedName("lingkup_en")
    val lingkupEn: String?,

    @SerializedName("tahun_data")
    val tahunData: Int?,

    @SerializedName("wilayah")
    val wilayah: String?,

    @SerializedName("penanggung_jawab")
    val penanggungJawab: String?,

    @SerializedName("show_satuan")
    val showSatuan: Boolean?,

    @SerializedName("id_subject")
    val idSubject: String?,

    @SerializedName("bab")
    val bab: String?,

    @SerializedName("bab_en")
    val babEn: String?,

    @SerializedName("subject")
    val subject: String?,

    @SerializedName("subject_en")
    val subjectEn: String?,

    @SerializedName("mms_id")
    val mmsId: Int?,

    @SerializedName("mms_subject")
    val mmsSubject: String?,

    @SerializedName("keterangan_data")
    val keteranganData: Map<String, String>?,

    @SerializedName("kolom")
    val kolom: Map<String, SimdasiKolom>?,

    @SerializedName("data")
    val data: List<SimdasiWilayahData>?,

    @SerializedName("catatan")
    val catatan: String?,

    @SerializedName("sumber")
    val sumber: String?,

    @SerializedName("versi")
    val versi: Int?,

    @SerializedName("table_created")
    val tableCreated: String?,

    @SerializedName("status")
    val status: Int?,

    @SerializedName("condition")
    val condition: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("created")
    val created: String?
)


/**
 * ============================================================
 * KOLOM / VARIABLE METADATA
 * ============================================================
 */
data class SimdasiKolom(

    @SerializedName("nama_variabel")
    val namaVariabel: String?,

    @SerializedName("nama_variabel_en")
    val namaVariabelEn: String?,

    @SerializedName("tipe")
    val tipe: String?,

    @SerializedName("angka_desimal_dibelakang_koma")
    val angkaDesimal: Int?,

    @SerializedName("satuan")
    val satuan: String?,

    @SerializedName("satuan_en")
    val satuanEn: String?,

    @SerializedName("metadata_indikator")
    val metadataIndikator: String?,

    @SerializedName("metadata_konsep_definisi")
    val metadataKonsepDefinisi: String?,

    @SerializedName("metadata_kegunaan")
    val metadataKegunaan: String?,

    @SerializedName("metadata_keterangan_tambahan")
    val metadataKeteranganTambahan: String?,

    @SerializedName("metadata_interpretasi")
    val metadataInterpretasi: String?,

    @SerializedName("metadata_rumusan")
    val metadataRumusan: List<Any>?,

    @SerializedName("metadata_rumus_html")
    val metadataRumusHtml: String?,

    @SerializedName("metadata_dihasilkan_oleh")
    val metadataDihasilkanOleh: List<Any>?,

    @SerializedName("tanggal_cut_off")
    val tanggalCutOff: String?
)


/**
 * ============================================================
 * DATA PER WILAYAH / KECAMATAN
 * ============================================================
 */
data class SimdasiWilayahData(

    @SerializedName("label")
    val label: String?,

    @SerializedName("label_raw")
    val labelRaw: String?,

    @SerializedName("satuan")
    val satuan: String?,

    @SerializedName("kode_wilayah")
    val kodeWilayah: Int?,

    @SerializedName("variables")
    val variables: Map<String, SimdasiVariable>?
)


/**
 * ============================================================
 * NILAI VARIABLE
 * ============================================================
 */
data class SimdasiVariable(

    @SerializedName("value")
    val value: String?,

    @SerializedName("value_raw")
    val valueRaw: String?,

    @SerializedName("value_code")
    val valueCode: String?
)