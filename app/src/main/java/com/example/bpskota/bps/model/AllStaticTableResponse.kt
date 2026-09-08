package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class AllStaticTableResponse(

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
// STATIC TABLE DETAIL RESPONSE
// ============================================================

data class StaticTableDetailResponse(

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("data-availability")
    val dataAvailability: String? = null,

    @SerializedName("data")
    val data: StaticTableDetail? = null
)


// ============================================================
// STATIC TABLE DETAIL
// ============================================================

data class StaticTableDetail(

    @SerializedName("table_id")
    val tableId: Int? = null,

    @SerializedName("sub_id")
    val subId: Int? = null,

    @SerializedName("subcsa_id")
    val subcsaId: Int? = null,

    @SerializedName("subcsa")
    val subcsa: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("table")
    val table: String? = null,

    @SerializedName("cr_date")
    val createdDate: String? = null,

    @SerializedName("updt_date")
    val updatedDate: String? = null,

    @SerializedName("excel")
    val excel: String? = null,

    @SerializedName("size")
    val size: String? = null,

    @SerializedName("related")
    val related: List<JsonElement>? = null
)