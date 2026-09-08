package com.example.bpskota.bps.repository

import android.util.Log
import com.example.bpskota.bps.api.BpsRetrofitClient
import com.example.bpskota.bps.api.BpsRetrofitClient.api
import com.example.bpskota.bps.model.*
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BpsRepository {

    fun getSimdasiTables(
        tahun: Int,
        wilayah: String,
        apiKey: String
    ): Call<SimdasiResponse> {

        return BpsRetrofitClient.api.getSimdasiTables(
            tahun = tahun,
            wilayah = wilayah,
            key = apiKey
        )
    }


    fun getSimdasiDetail(
        tahun: Int,
        idTabel: String,
        wilayah: String,
        apiKey: String
    ): Call<SimdasiDetailResponse> {

        return BpsRetrofitClient.api.getSimdasiDetail(
            tahun = tahun,
            idTabel = idTabel,
            wilayah = wilayah,
            key = apiKey
        )
    }


    fun getTenagaKerja(
        domain: String,
        variable: Int,
        tahun: Int,
        apiKey: String
    ): Call<TenagaKerjaResponse> {

        return BpsRetrofitClient.api.getTenagaKerja(
            domain = domain,
            variable = variable,
            tahun = tahun,
            key = apiKey
        )
    }


    fun getEkonomiTables(
        domain: String,
        subject: Int,
        page: Int,
        perPage: Int,
        apiKey: String
    ): Call<EkonomiListResponse> {

        return BpsRetrofitClient.api.getEkonomiTables(
            model = "tablestatistic",
            domain = domain,
            subject = subject,
            page = page,
            perPage = perPage,
            key = apiKey
        )
    }


    // ============================================================
    // EKONOMI DETAIL
    // ============================================================

    fun getEkonomiDetail(
        domain: String,
        id: String,
        tahun: Int,
        apiKey: String
    ): Call<EkonomiDetailResponse> {


        return BpsRetrofitClient.api.getEkonomiDetail(
            model = "tablestatistic",
            domain = domain,
            id = id,
            lang = "ind",
            key = apiKey
        )
    }
    fun getTempatTinggalVariables(
        domain: String,
        page: Int,
        apiKey: String
    ): Call<TempatTinggalResponse> {

        return BpsRetrofitClient.api.getTempatTinggalVariables(
            domain = domain,
            page = page,
            key = apiKey
        )
    }
    fun getTempatTinggalData(
        domain: String,
        variable: Int,
        tahun: Int,
        apiKey: String
    ): Call<TempatTinggalDataResponse> {

        return BpsRetrofitClient.api.getTempatTinggalData(
            domain = domain,
            variable = variable,
            tahun = tahun,
            key = apiKey
        )
    }
    // ============================================================
// KONSUMSI - LIST
// ============================================================

    fun getKonsumsiVariables(
        domain: String,
        page: Int,
        apiKey: String
    ): Call<KonsumsiResponse> {

        return BpsRetrofitClient.api.getKonsumsiVariables(
            domain = domain,
            page = page,
            key = apiKey
        )
    }


// ============================================================
// KONSUMSI - DETAIL
// ============================================================

    fun getKonsumsiDetail(
        domain: String,
        variable: Int,
        tahun: Int,
        apiKey: String
    ): Call<KonsumsiDetailResponse> {

        return BpsRetrofitClient.api.getKonsumsiDetail(
            domain = domain,
            variable = variable,
            tahun = tahun,
            key = apiKey
        )
    }
    fun getInfographics(
        apiKey: String,
        callback: (List<Infografik>) -> Unit,
        onError: (Throwable) -> Unit
    ) {

        val semuaData = mutableListOf<Infografik>()

        fun loadPage(page: Int) {

            BpsRetrofitClient.api.getInfographics(
                model = "infographic",
                lang = "ind",
                domain = "3574",
                page = page,
                perPage = 10,
                apiKey = apiKey
            ).enqueue(object : retrofit2.Callback<InfographicResponse> {

                override fun onResponse(
                    call: Call<InfographicResponse>,
                    response: Response<InfographicResponse>
                ) {

                    if (!response.isSuccessful) {
                        onError(
                            Exception(
                                "Gagal mengambil halaman $page"
                            )
                        )
                        return
                    }

                    val body = response.body()

                    if (body == null || body.status != "OK") {
                        onError(
                            Exception(
                                "Response halaman $page tidak valid"
                            )
                        )
                        return
                    }

                    val rawData = body.data

                    if (rawData != null) {

                        val gson = com.google.gson.Gson()

                        rawData.forEach { element ->

                            try {

                                if (element.isJsonObject) {

                                    val item =
                                        gson.fromJson(
                                            element,
                                            Infografik::class.java
                                        )

                                    semuaData.add(item)
                                }

                                else if (element.isJsonArray) {

                                    element.asJsonArray.forEach { child ->

                                        if (child.isJsonObject) {

                                            val item =
                                                gson.fromJson(
                                                    child,
                                                    Infografik::class.java
                                                )

                                            semuaData.add(item)
                                        }
                                    }
                                }

                            } catch (_: Exception) {
                            }
                        }
                    }

                    // Lanjut ke halaman berikutnya
                    if (page < 10) {

                        loadPage(page + 1)

                    } else {

                        callback(
                            semuaData
                                .distinctBy {
                                    it.infId
                                }
                                .sortedByDescending {
                                    it.date ?: ""
                                }
                        )
                    }
                }

                override fun onFailure(
                    call: Call<InfographicResponse>,
                    t: Throwable
                ) {

                    onError(t)
                }
            })
        }

        loadPage(1)
    }
    fun getInfographicsHome(
        apiKey: String
    ): Call<InfographicResponse> {

        return BpsRetrofitClient.api.getInfographics(
            model = "infographic",
            lang = "ind",
            domain = "3574",
            page = 1,
            perPage = 10,
            apiKey = apiKey
        )
    }
    fun getKependudukanData(
        domain: String,
        page: Int,
        apiKey: String
    ): Call<KependudukanDataResponse> {

        return BpsRetrofitClient.api.getKependudukanData(
            model = "statictable",
            domain = domain,
            page = page,
            key = apiKey
        )
    }
    fun getKependudukanTables(
        domain: String,
        page: Int,
        apiKey: String
    ): Call<KependudukanDataResponse> {

        return BpsRetrofitClient.api.getKependudukanTables(
            model = "statictable",
            domain = domain,
            page = page,
            key = apiKey
        )
    }
    fun getKependudukanStaticTableDetail(
        domain: String,
        id: Int,
        apiKey: String
    ): Call<KependudukanStaticDetailResponse> {

        return BpsRetrofitClient.api.getKependudukanStaticTableDetail(
            domain = domain,
            lang = "ind",
            id = id,
            key = apiKey
        )
    }
    fun getKependudukanDetail(
        domain: String,
        variable: Int,
        tahun: Int,
        apiKey: String
    ): Call<KependudukanDataResponse> {

        return BpsRetrofitClient.api.getKependudukanDetail(
            domain = domain,
            variable = variable,
            tahun = tahun,
            key = apiKey
        )
    }
    fun getKependudukanVariables(
        domain: String,
        page: Int,
        apiKey: String
    ): Call<KependudukanDataResponse> {

        return BpsRetrofitClient.api.getKependudukanVariables(
            domain = domain,
            page = page,
            key = apiKey
        )
    }
    fun getTahunList(
        domain: String,
        apiKey: String
    ): Call<KependudukanDataResponse> {
        return BpsRetrofitClient.api.getTahunList(
            domain = domain,
            key = apiKey
        )
    }
    fun getNews(
        domain: String,
        page: Int,
        perPage: Int,
        apiKey: String,
        callback: (JsonObject?, Throwable?) -> Unit
    ) {
        BpsRetrofitClient.api.getNews(
            model = "news",
            domain = domain,
            page = page.toString(),
            per_page = perPage.toString(),
            key = apiKey
        ).enqueue(object : Callback<JsonObject> {

            override fun onResponse(
                call: Call<JsonObject>,
                response: Response<JsonObject>
            ) {
                if (response.isSuccessful) {
                    callback(response.body(), null)
                } else {
                    callback(
                        null,
                        Exception("HTTP ${response.code()}")
                    )
                }
            }

            override fun onFailure(
                call: Call<JsonObject>,
                t: Throwable
            ) {
                callback(null, t)
            }
        })
    }
    fun getNewsDetail(
        domain: String,
        newsId: String,
        apiKey: String,
        callback: (JsonObject?, Throwable?) -> Unit
    ) {
        BpsRetrofitClient.api.getNewsDetail(
            domain = domain,
            model = "news",
            lang = "ind",
            id = newsId,
            key = apiKey
        ).enqueue(object : Callback<JsonObject> {

            override fun onResponse(
                call: Call<JsonObject>,
                response: Response<JsonObject>
            ) {
                if (response.isSuccessful) {
                    callback(
                        response.body(),
                        null
                    )
                } else {
                    callback(
                        null,
                        Exception(
                            "HTTP ${response.code()}"
                        )
                    )
                }
            }

            override fun onFailure(
                call: Call<JsonObject>,
                t: Throwable
            ) {
                callback(
                    null,
                    t
                )
            }
        })
    }
    fun getPublikasi(
        page: Int,
        domain: String,
        apiKey: String,
        callback: (PublikasiResponse?) -> Unit
    ) {
        BpsRetrofitClient.api.getPublikasi(
            domain = domain,
            page = page,
            key = apiKey
        ).enqueue(object : Callback<PublikasiResponse> {

            override fun onResponse(
                call: Call<PublikasiResponse>,
                response: Response<PublikasiResponse>
            ) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(
                call: Call<PublikasiResponse>,
                t: Throwable
            ) {
                callback(null)
            }
        })
    }
    fun getPublikasiDetail(
        id: String,
        domain: String,
        apiKey: String,
        callback: (PublikasiDetailResponse?) -> Unit
    ) {
        BpsRetrofitClient.api.getPublikasiDetail(
            domain = domain,
            id = id,
            key = apiKey
        ).enqueue(object : Callback<PublikasiDetailResponse> {

            override fun onResponse(
                call: Call<PublikasiDetailResponse>,
                response: Response<PublikasiDetailResponse>
            ) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(
                call: Call<PublikasiDetailResponse>,
                t: Throwable
            ) {
                callback(null)
            }
        })
    }
    fun getHarga(
        domain: String,
        variable: Int,
        tahun: Int,
        apiKey: String
    ): Call<HargaResponse> {

        return BpsRetrofitClient.api.getHarga(
            domain = domain,
            variable = variable,
            tahun = tahun,
            key = apiKey
        )
    }
}
