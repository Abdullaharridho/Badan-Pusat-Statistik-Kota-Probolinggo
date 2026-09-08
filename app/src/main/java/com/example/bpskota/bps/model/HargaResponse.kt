package com.example.bpskota.bps.model

import com.google.gson.annotations.SerializedName

data class HargaResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("last_update")
    val lastUpdate: String?,

    @SerializedName("subject")
    val subject: List<HargaSubject>?,

    @SerializedName("var")
    val variable: List<HargaVariable>?,

    @SerializedName("turvar")
    val turvar: List<HargaTurvar>?,

    @SerializedName("labelvervar")
    val labelVerVar: String?,

    @SerializedName("vervar")
    val vervar: List<HargaVerVar>?,

    @SerializedName("tahun")
    val tahun: List<HargaTahun>?,

    @SerializedName("turtahun")
    val turtahun: List<HargaTurTahun>?,

    @SerializedName("datacontent")
    val dataContent: Map<String, Double>?,

    @SerializedName("related")
    val related: List<Any>?
)

data class HargaSubject(
    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)

data class HargaVariable(
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

data class HargaTurvar(
    @SerializedName("val")
    val valId: String?,

    @SerializedName("label")
    val label: String?
)

data class HargaVerVar(
    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)

data class HargaTahun(
    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)

data class HargaTurTahun(
    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)