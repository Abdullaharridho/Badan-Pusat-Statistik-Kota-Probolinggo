package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName


data class WilayahResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: List<JsonElement>?
)
data class Wilayah(
    @SerializedName("kode_ver_id")
    val kodeVerId: Int,

    @SerializedName("vervar")
    val vervar: String,

    @SerializedName("item_ver_id")
    val itemVerId: Int,

    @SerializedName("group_ver_id")
    val groupVerId: Int,

    @SerializedName("name_group_ver_id")
    val nameGroupVerId: String
)