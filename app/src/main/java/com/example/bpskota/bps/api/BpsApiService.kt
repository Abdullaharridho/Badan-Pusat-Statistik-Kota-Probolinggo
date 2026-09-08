package com.example.bpskota.bps.api

import com.example.bpskota.bps.model.*
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BpsApiService {

    // ============================================================
    // SIMDASI LIST
    // ============================================================

    @GET("interoperabilitas/datasource/simdasi/id/23/")
    fun getSimdasiTables(
        @Query("tahun")
        tahun: Int,

        @Query("wilayah")
        wilayah: String,

        @Query("key")
        key: String

    ): Call<SimdasiResponse>


    @GET(
        "interoperabilitas/datasource/simdasi/id/25/" +
                "tahun/{tahun}/" +
                "id_tabel/{idTabel}/" +
                "wilayah/{wilayah}/" +
                "key/{key}"
    )
    fun getSimdasiDetail(
        @Path("tahun")
        tahun: Int,

        @Path("idTabel")
        idTabel: String,

        @Path("wilayah")
        wilayah: String,

        @Path("key")
        key: String

    ): Call<SimdasiDetailResponse>

    // ============================================================
// KEPENDUDUKAN - LIST VARIABLE
// ============================================================

    @GET(
        "list/model/var/lang/ind/" +
                "domain/{domain}/" +
                "page/{page}/" +
                "key/{key}"
    )
    fun getKependudukanVariables(
        @Path("domain")
        domain: String,

        @Path("page")
        page: Int,

        @Path("key")
        key: String

    ): Call<KependudukanDataResponse>

    @GET("list/")
    fun getKependudukanTables(
        @Query("model") model: String,
        @Query("domain") domain: String,
        @Query("page") page: Int,
        @Query("key") key: String
    ): Call<KependudukanDataResponse>
    @GET("list/")
    fun getKependudukanData(
        @Query("model")
        model: String,

        @Query("domain")
        domain: String,

        @Query("page")
        page: Int,

        @Query("key")
        key: String
    ): Call<KependudukanDataResponse>
    @GET(
        "view/" +
                "domain/{domain}/" +
                "model/statictable/" +
                "lang/{lang}/" +
                "id/{id}/" +
                "key/{key}"
    )
    fun getKependudukanStaticTableDetail(
        @Path("domain")
        domain: String,

        @Path("lang")
        lang: String,

        @Path("id")
        id: Int,

        @Path("key")
        key: String

    ): Call<KependudukanStaticDetailResponse>
    @GET(
        "list/model/data/lang/ind/" +
                "domain/{domain}/" +
                "var/{var}/" +
                "th/{th}/" +
                "key/{key}"
    )
    fun getKependudukanDetail(
        @Path("domain")
        domain: String,

        @Path("var")
        variable: Int,

        @Path("th")
        tahun: Int,

        @Path("key")
        key: String

    ): Call<KependudukanDataResponse>

    // ============================================================
    // TENAGA KERJA
    // ============================================================

    @GET(
        "list/model/data/lang/ind/" +
                "domain/{domain}/" +
                "var/{var}/" +
                "th/{th}/" +
                "key/{key}"
    )
    fun getTenagaKerja(
        @Path("domain")
        domain: String,

        @Path("var")
        variable: Int,

        @Path("th")
        tahun: Int,

        @Path("key")
        key: String

    ): Call<TenagaKerjaResponse>


    // ============================================================
    // EKONOMI - LIST TABEL
    // ============================================================

    @GET("list")
    fun getEkonomiTables(
        @Query("model")
        model: String,

        @Query("domain")
        domain: String,

        @Query("subject")
        subject: Int,

        @Query("page")
        page: Int,

        @Query("perpage")
        perPage: Int,

        @Query("key")
        key: String

    ): Call<EkonomiListResponse>


    // ============================================================
    // EKONOMI - DETAIL DATA
    // ============================================================

    @GET("view")
    fun getEkonomiDetail(
        @Query("model")
        model: String,

        @Query("domain")
        domain: String,

        @Query("id")
        id: String,

        @Query("lang")
        lang: String,

        @Query("key")
        key: String

    ): Call<EkonomiDetailResponse>


    // ============================================================
    // TEMPAT TINGGAL - LIST VARIABLE
    // ============================================================

    @GET(
        "list/model/var/lang/ind/" +
                "domain/{domain}/" +
                "page/{page}/" +
                "key/{key}"
    )
    fun getTempatTinggalVariables(
        @Path("domain")
        domain: String,

        @Path("page")
        page: Int,

        @Path("key")
        key: String

    ): Call<TempatTinggalResponse>


    // ============================================================
    // TEMPAT TINGGAL - DATA
    // ============================================================

    @GET(
        "list/model/data/lang/ind/" +
                "domain/{domain}/" +
                "var/{var}/" +
                "th/{th}/" +
                "key/{key}"
    )
    fun getTempatTinggalData(
        @Path("domain")
        domain: String,

        @Path("var")
        variable: Int,

        @Path("th")
        tahun: Int,

        @Path("key")
        key: String

    ): Call<TempatTinggalDataResponse>

    // ============================================================
// KONSUMSI - LIST VARIABLE
// ============================================================

    @GET(
        "list/model/var/lang/ind/" +
                "domain/{domain}/" +
                "page/{page}/" +
                "key/{key}"
    )
    fun getKonsumsiVariables(
        @Path("domain")
        domain: String,

        @Path("page")
        page: Int,

        @Path("key")
        key: String

    ): Call<KonsumsiResponse>


// ============================================================
// KONSUMSI - DETAIL
// ============================================================

    @GET(
        "list/model/data/lang/ind/" +
                "domain/{domain}/" +
                "var/{var}/" +
                "th/{th}/" +
                "key/{key}"
    )
    fun getKonsumsiDetail(
        @Path("domain")
        domain: String,

        @Path("var")
        variable: Int,

        @Path("th")
        tahun: Int,

        @Path("key")
        key: String

    ): Call<KonsumsiDetailResponse>
    @GET("list")
    fun getWilayah(
        @Query("model") model: String = "vervar",
        @Query("domain") domain: String = "3574",
        @Query("page") page: Int,
        @Query("key") apiKey: String
    ): Call<WilayahResponse>
    @GET("list/")

    fun getInfographics(
        @Query("model")
        model: String = "infographic",

        @Query("lang")
        lang: String = "ind",

        @Query("domain")
        domain: String = "3574",

        @Query("page")
        page: Int = 1,

        @Query("perpage")
        perPage: Int = 10,

        @Query("key")
        apiKey: String

    ): Call<InfographicResponse>
    @GET("list/model/tahun/lang/ind/domain/{domain}/key/{key}")
    fun getTahunList(
        @Path("domain") domain: String,
        @Path("key") key: String
    ): Call<KependudukanDataResponse>
    @GET("list")
    fun getNews(
        @Query("model") model: String,
        @Query("domain") domain: String,
        @Query("page")page:String,
        @Query("per_page")per_page:String,
        @Query("key") key: String
    ): Call<JsonObject>

    @GET("view")
    fun getNewsDetail(
        @Query("domain") domain: String,
        @Query("model") model: String,
        @Query("lang") lang: String,
        @Query("id") id: String,
        @Query("key") key: String
    ): Call<JsonObject>
    @GET("list/")
    fun getPublikasi(
        @Query("model") model: String = "publication",
        @Query("domain") domain: String,
        @Query("page") page: Int? = null,
        @Query("key") key: String
    ): Call<PublikasiResponse>
    @GET("view")
    fun getPublikasiDetail(
        @Query("domain") domain: String,
        @Query("model") model: String = "publication",
        @Query("lang") lang: String = "ind",
        @Query("id") id: String,
        @Query("key") key: String
    ): Call<PublikasiDetailResponse>
    @GET(
        "list/model/data/lang/ind/" +
                "domain/{domain}/" +
                "var/{var}/" +
                "th/{th}/" +
                "key/{key}"
    )
    fun getHarga(
        @Path("domain") domain: String,
        @Path("var") variable: Int,
        @Path("th") tahun: Int,
        @Path("key") key: String
    ): Call<HargaResponse>

}