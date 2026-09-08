package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ============================================================
// LIST VARIABLE
// ============================================================

data class AllVariableResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("data-availability")
    val dataAvailability: String? = null,

    @SerializedName("last_update")
    val lastUpdate: String? = null,

    @SerializedName("data")
    val data: JsonElement? = null
)

// ============================================================
// DETAIL VARIABLE
// ============================================================

data class VariableDetailResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("data-availability")
    val dataAvailability: String? = null,

    @SerializedName("last_update")
    val lastUpdate: String? = null,

    @SerializedName("subject")
    val subject: List<VariableSubject>? = null,

    @SerializedName("var")
    val variable: List<VariableInfo>? = null,

    @SerializedName("turvar")
    val turvar: List<VariableTurvar>? = null,

    @SerializedName("labelvervar")
    val labelVerVar: String? = null,

    @SerializedName("vervar")
    val wilayah: List<VariableWilayah>? = null,

    @SerializedName("tahun")
    val tahun: List<VariableTahun>? = null,

    @SerializedName("turtahun")
    val turTahun: List<VariableTurvar>? = null, // Menggunakan class yg sama dengan turvar (val berupa String)

    @SerializedName("datacontent")
    val dataContent: Map<String, Double>? = null,

    @SerializedName("related")
    val related: List<JsonElement>? = null
)

// ============================================================
// SUBJECT
// ============================================================

data class VariableSubject(
    @SerializedName("val")
    val value: String? = null, // DIUBAH KE STRING

    @SerializedName("label")
    val label: String? = null
)

// ============================================================
// VARIABLE INFO
// ============================================================

data class VariableInfo(
    @SerializedName("val")
    val value: String? = null, // DIUBAH KE STRING

    @SerializedName("label")
    val label: String? = null,

    @SerializedName("unit")
    val unit: String? = null,

    @SerializedName("subj")
    val subject: String? = null,

    @SerializedName("def")
    val definition: String? = null,

    @SerializedName("decimal")
    val decimal: Int? = null, // Decimal tetap Int karena ini murni untuk format angka

    @SerializedName("note")
    val note: String? = null
)

// ============================================================
// TURVAR
// ============================================================

data class VariableTurvar(
    @SerializedName("val")
    val value: String? = null,

    @SerializedName("label")
    val label: String? = null
)

// ============================================================
// VERVAR (Wilayah / Kategori)
// ============================================================

data class VariableWilayah(
    @SerializedName("val")
    val value: String? = null, // DIUBAH KE STRING

    @SerializedName("label")
    val label: String? = null
)

// ============================================================
// TAHUN
// ============================================================

data class VariableTahun(
    @SerializedName("val")
    val value: String? = null, // DIUBAH KE STRING

    @SerializedName("label")
    val label: String? = null
)