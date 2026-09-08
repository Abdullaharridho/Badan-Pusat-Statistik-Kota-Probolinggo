package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ============================================================
// RESPONSE LIST VARIABLE
// ============================================================

data class TempatTinggalResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: List<JsonElement>?
)


// ============================================================
// VARIABLE TEMPAT TINGGAL
// ============================================================

data class TempatTinggalVariable(

    @SerializedName("var_id")
    val varId: Int?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("sub_id")
    val subId: Int?,

    @SerializedName("sub_name")
    val subName: String?,

    @SerializedName("subcsa_id")
    val subcsaId: Int?,

    @SerializedName("subcsa_name")
    val subcsaName: String?,

    @SerializedName("def")
    val definition: String?,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("vertical")
    val vertical: Int?,

    @SerializedName("unit")
    val unit: String?,

    @SerializedName("graph_id")
    val graphId: Int?,

    @SerializedName("graph_name")
    val graphName: String?
)


// ============================================================
// DETAIL DATA TEMPAT TINGGAL
// ============================================================

data class TempatTinggalDataResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("last_update")
    val lastUpdate: String?,

    @SerializedName("subject")
    val subject: List<TempatTinggalSubject>?,

    @SerializedName("var")
    val variable: List<TempatTinggalDataVariable>?,

    @SerializedName("turvar")
    val turvar: List<TempatTinggalTurvar>?,

    @SerializedName("labelvervar")
    val labelVerVar: String?,

    @SerializedName("vervar")
    val vervar: List<TempatTinggalVerVar>?,

    @SerializedName("tahun")
    val tahun: List<TempatTinggalTahun>?,

    @SerializedName("turtahun")
    val turtahun: List<TempatTinggalTurTahun>?,

    @SerializedName("datacontent")
    val dataContent: Map<String, Double>?,

    @SerializedName("related")
    val related: List<JsonElement>?
)


// ============================================================
// SUBJECT
// ============================================================

data class TempatTinggalSubject(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// VARIABLE DETAIL
// ============================================================

data class TempatTinggalDataVariable(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?,

    @SerializedName("unit")
    val unit: String?,

    @SerializedName("subj")
    val subject: String?,

    @SerializedName("def")
    val definition: String?,

    @SerializedName("decimal")
    val decimal: Int?,

    @SerializedName("note")
    val note: String?
)


// ============================================================
// TURVAR
// ============================================================

data class TempatTinggalTurvar(

    @SerializedName("val")
    val value: String?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// VERVAR
// ============================================================

data class TempatTinggalVerVar(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// TAHUN
// ============================================================

data class TempatTinggalTahun(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// TURTAHUN
// ============================================================

data class TempatTinggalTurTahun(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)