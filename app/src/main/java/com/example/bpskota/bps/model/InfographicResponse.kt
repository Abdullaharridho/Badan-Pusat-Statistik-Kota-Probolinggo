package com.example.bpskota.bps.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class InfographicResponse(
    val status: String?,
    @SerializedName("data-availability")
    val dataAvailability: String?,
    val data: JsonArray?
)

data class InfographicPagination(
    val page: Int?,
    val pages: Int?,

    @SerializedName("per_page")
    val perPage: Int?,

    val count: Int?,
    val total: Int?
)
data class Infografik(
    @SerializedName("inf_id")
    val infId: Int,

    val title: String?,
    val img: String?,
    val date: String?,
    val desc: String?,
    val category: Int?,
    val dl: String?
)