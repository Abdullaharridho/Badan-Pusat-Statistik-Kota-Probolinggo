package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class AllSimdasiResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("data-availability")
    val dataAvailability: String? = null,

    @SerializedName("last_update")
    val lastUpdate: String? = null,

    @SerializedName("subject")
    val subject: JsonElement? = null,

    @SerializedName("var")
    val variable: JsonElement? = null,

    @SerializedName("data")
    val data: JsonElement? = null
)