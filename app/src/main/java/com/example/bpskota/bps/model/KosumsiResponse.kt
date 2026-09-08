package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// ============================================================
// LIST VARIABLE
// ============================================================

data class KonsumsiResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: List<JsonElement>?
)


// ============================================================
// VARIABLE
// ============================================================

data class KonsumsiVariable(

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
// DETAIL RESPONSE
//
// Sesuai JSON API:
//
// {
//   "status": "OK",
//   "subject": [...],
//   "var": [...],
//   "turvar": [...],
//   "labelvervar": "...",
//   "vervar": [...],
//   "tahun": [...],
//   "turtahun": [...],
//   "datacontent": {...}
// }
// ============================================================

data class KonsumsiDetailResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("last_update")
    val lastUpdate: String?,

    @SerializedName("subject")
    val subject: List<KonsumsiSubject>?,

    @SerializedName("var")
    val variable: List<KonsumsiVariableDetail>?,

    @SerializedName("turvar")
    val turvar: List<KonsumsiTurvar>?,

    @SerializedName("labelvervar")
    val labelVervar: String?,

    @SerializedName("vervar")
    val vervar: List<KonsumsiVervar>?,

    @SerializedName("tahun")
    val tahun: List<KonsumsiTahun>?,

    @SerializedName("turtahun")
    val turtahun: List<KonsumsiTurTahun>?,

    @SerializedName("datacontent")
    val dataContent: JsonElement?,

    @SerializedName("related")
    val related: JsonElement?
)
// ============================================================
// SUBJECT
// ============================================================

data class KonsumsiSubject(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// VARIABLE DETAIL
// ============================================================

data class KonsumsiVariableDetail(

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
// TAHUN
// ============================================================

data class KonsumsiTahun(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// TURVAR
// ============================================================

data class KonsumsiTurvar(

    @SerializedName("val")
    val value: String?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// TAHUN TUR
// ============================================================

data class KonsumsiTurTahun(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)


// ============================================================
// VERVAR
// ============================================================

data class KonsumsiVervar(

    @SerializedName("val")
    val value: Int?,

    @SerializedName("label")
    val label: String?
)