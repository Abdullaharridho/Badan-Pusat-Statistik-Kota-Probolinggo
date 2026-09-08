package com.example.bpskota.bps.model

import com.google.gson.annotations.SerializedName

data class SimdasiResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: List<SimdasiData>?
)

data class SimdasiData(

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

    @SerializedName("data")
    val tables: List<SimdasiTable>?
)

data class SimdasiTable(

    @SerializedName("id_tabel")
    val idTabel: String?,

    @SerializedName("judul")
    val judul: String?,

    @SerializedName("judul_en")
    val judulEn: String?,

    @SerializedName("kode_tabel")
    val kodeTabel: String?,

    @SerializedName("ketersediaan_tahun")
    val ketersediaanTahun: List<Int>?,

    @SerializedName("latest_update")
    val latestUpdate: String?,

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
    val mmsSubject: String?
)