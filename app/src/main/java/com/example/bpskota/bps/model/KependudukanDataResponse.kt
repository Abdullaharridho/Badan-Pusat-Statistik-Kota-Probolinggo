package com.example.bpskota.bps.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class KependudukanDataResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("last_update")
    val lastUpdate: String?,

    @SerializedName("subject")
    val subject: List<KependudukanSubject>?,

    @SerializedName("var")
    val variables: List<KependudukanVariable>?,

    @SerializedName("turvar")
    val turvar: List<KependudukanItem>?,

    @SerializedName("labelvervar")
    val labelVervar: String?,

    @SerializedName("vervar")
    val vervar: List<KependudukanItem>?,

    @SerializedName("tahun")
    val tahun: List<KependudukanItem>?,

    @SerializedName("turtahun")
    val turtahun: List<KependudukanItem>?,

    @SerializedName("datacontent")
    val dataContent: Map<String, Double>?,

    @SerializedName("related")
    val related: List<Any>?,

    @SerializedName("data")
    val data: JsonElement?
)

data class KependudukanSubject(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)

data class KependudukanVariable(

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

data class KependudukanItem(

    @SerializedName("val")
    val valId: Int?,

    @SerializedName("label")
    val label: String?
)

data class KependudukanPageInfo(

    @SerializedName("page")
    val page: Int?,

    @SerializedName("pages")
    val pages: Int?,

    @SerializedName("per_page")
    val perPage: Int?,

    @SerializedName("count")
    val count: Int?,

    @SerializedName("total")
    val total: Int?
)

data class KependudukanTable(

    @SerializedName("table_id")
    val tableId: Int?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("subj_id")
    val subjectId: Int?,

    @SerializedName("subj")
    val subject: String?,

    @SerializedName("updt_date")
    val updateDate: String?,

    @SerializedName("size")
    val size: String?,

    @SerializedName("excel")
    val excel: String?
)
data class KependudukanStaticDetailResponse(

    @SerializedName("status")
    val status: String?,

    @SerializedName("data-availability")
    val dataAvailability: String?,

    @SerializedName("data")
    val data: JsonElement?
)

data class KependudukanStaticDetailData(

    @SerializedName("table_id")
    val tableId: Int?,

    @SerializedName("sub_id")
    val subId: Int?,

    @SerializedName("subcsa_id")
    val subcsaId: Int?,

    @SerializedName("subcsa")
    val subcsa: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("table")
    val table: String?,

    @SerializedName("cr_date")
    val crDate: String?,

    @SerializedName("updt_date")
    val updateDate: String?
)
data class DataVariabel(
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