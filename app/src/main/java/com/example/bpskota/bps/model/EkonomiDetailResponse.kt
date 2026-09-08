package com.example.bpskota.bps.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class EkonomiDetailResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("last_update")
    val lastUpdate: String?,

    @SerializedName("subject")
    val subject: List<EkonomiSubject>?,

    @SerializedName("var")
    val variables: List<EkonomiVariable>?,

    @SerializedName("turvar")
    val turvar: List<EkonomiItem>?,

    @SerializedName("labelvervar")
    val labelVervar: String?,

    @SerializedName("vervar")
    val vervar: List<EkonomiVervar>?,

    @SerializedName("tahun")
    val tahun: List<EkonomiItem>?,

    @SerializedName("turtahun")
    val turtahun: List<EkonomiItem>?,

    @SerializedName("datacontent")
    val dataContent: JsonObject?,

    @SerializedName("available_years")
    val availableYears: List<String>?,

    @SerializedName("metadata")
    val metadata: EkonomiMetadata?
)


// ============================================================
// SUBJECT
// ============================================================

data class EkonomiSubject(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// VARIABLE
// ============================================================

data class EkonomiVariable(

    @SerializedName("value")
    val value: Int?,

    @SerializedName("val")
    val valId: Int?,

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
// TURVAR / TAHUN / TURTAHUN
// ============================================================

data class EkonomiItem(

    @SerializedName("value")
    val value: Int?,

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// VERVAR
// ============================================================

data class EkonomiVervar(

    @SerializedName("value")
    val value: Int?,

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// METADATA
// ============================================================

data class EkonomiMetadata(

    @SerializedName("indikator")
    val indikator: List<Any>?
)