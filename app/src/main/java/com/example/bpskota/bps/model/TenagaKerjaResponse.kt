package com.example.bpskota.bps.model

import com.google.gson.annotations.SerializedName

data class TenagaKerjaResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("last_update")
    val lastUpdate: String?,

    @SerializedName("subject")
    val subject: List<TenagaKerjaSubject>?,

    @SerializedName("var")
    val variable: List<TenagaKerjaVariable>?,

    @SerializedName("turvar")
    val turvar: List<TenagaKerjaTurvar>?,

    @SerializedName("labelvervar")
    val labelVerVar: String?,

    @SerializedName("vervar")
    val verVar: List<TenagaKerjaVerVar>?,

    @SerializedName("tahun")
    val tahun: List<TenagaKerjaTahun>?,

    @SerializedName("turtahun")
    val turTahun: List<TenagaKerjaTurTahun>?,

    @SerializedName("datacontent")
    val dataContent: Map<String, Double>?,

    @SerializedName("related")
    val related: List<Any>?
)


data class TenagaKerjaSubject(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)


data class TenagaKerjaVariable(

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


data class TenagaKerjaTurvar(

    @SerializedName("val")
    val valId: String?,

    @SerializedName("label")
    val label: String?
)


data class TenagaKerjaVerVar(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)


data class TenagaKerjaTahun(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)



data class TenagaKerjaTurTahun(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)