package com.example.bpskota.bps.model

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class EkonomiListResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: JsonArray?
)

data class EkonomiTable(
    @SerializedName("id")
    val id: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("oldest_period")
    val oldestPeriod: Int?,

    @SerializedName("latest_period")
    val latestPeriod: Int?,

    @SerializedName("id_subject")
    val idSubject: Int?,

    @SerializedName("subject")
    val subject: String?,

    @SerializedName("id_subcat")
    val idSubcat: Int?,

    @SerializedName("subcat")
    val subcat: String?,

    @SerializedName("tablesource")
    val tableSource: String?,

    @SerializedName("last_update")
    val lastUpdate: String?
)