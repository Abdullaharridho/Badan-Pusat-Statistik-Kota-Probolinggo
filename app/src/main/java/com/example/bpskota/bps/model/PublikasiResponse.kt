package com.example.bpskota.bps.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class PublikasiResponse(
        val status: String?,
        @SerializedName("data-availability")
        val dataAvailability: String?,
        val data: List<JsonElement>?
) {
        fun getPagination(): PublikasiPagination? {
                val element = data?.getOrNull(0) ?: return null

                return try {
                        Gson().fromJson(
                                element,
                                PublikasiPagination::class.java
                        )
                } catch (e: Exception) {
                        null
                }
        }

        fun getPublikasi(): List<Publikasi> {
                val element = data?.getOrNull(1) ?: return emptyList()

                return try {
                        val type = object : TypeToken<List<Publikasi>>() {}.type
                        Gson().fromJson(element, type)
                } catch (e: Exception) {
                        emptyList()
                }
        }
}

data class PublikasiPagination(
        val page: Int?,
        val pages: Int?,
        val per_page: Int?,
        val count: Int?,
        val total: Int?
)

data class Publikasi(
        val pub_id: String?,
        val title: String?,
        val abstract: String?,
        val id_subject_csa: List<Int>?,
        val subject_csa: List<String>?,
        val issn: String?,
        val sch_date: String?,
        val rl_date: String?,
        val updt_date: String?,
        val cover: String?,
        val pdf: String?,
        val size: String?
)

data class PublikasiDetailResponse(
        val status: String?,
        val request: PublikasiRequest?,
        @SerializedName("data-availability")
        val dataAvailability: String?,
        val data: PublikasiDetail?
)

data class PublikasiRequest(
        val domain: String?,
        val model: String?,
        val lang: String?,
        val id: String?,
        val time_access: String?
)

data class PublikasiDetail(
        val pub_id: String?,
        val title: String?,
        val id_subject_csa: List<Int>?,
        val subject_csa: List<String>?,
        val kat_no: String?,
        val pub_no: String?,
        val issn: String?,
        val abstract: String?,
        val sch_date: String?,
        val rl_date: String?,
        val updt_date: String?,
        val periode: String?,
        val bahasa: String?,
        val revisi: Int?,
        val cover: String?,
        val pdf: String?,
        val size: String?,
        val related: List<Publikasi>?
)