package com.example.bpskota.bps.wilayah

import com.google.gson.annotations.SerializedName

data class WilayahResponse(
    @SerializedName("data")
    val data: List<WilayahItem>?
)

data class WilayahItem(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String
)