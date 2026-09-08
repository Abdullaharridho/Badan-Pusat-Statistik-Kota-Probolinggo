package com.example.bpskota.bps.api

import com.example.bpskota.bps.model.AllSimdasiResponse
import com.example.bpskota.bps.model.AllStaticTableResponse
import com.example.bpskota.bps.model.AllVariableResponse
import com.example.bpskota.bps.model.SimdasiDetailResponse
import com.example.bpskota.bps.model.StaticTableDetailResponse
import com.example.bpskota.bps.model.VariableDetailResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BpsAllDataApiService {

    // =====================================================
    // LIST SIMDASI
    // =====================================================

    @GET("interoperabilitas/datasource/simdasi/id/23/")
    fun getAllSimdasi(
        @Query("wilayah") wilayah: String,
        @Query("page") page: Int,
        @Query("key") key: String
    ): Call<AllSimdasiResponse>


    // =====================================================
    // LIST STATIC TABLE
    // =====================================================

    @GET("list")
    fun getAllStaticTables(
        @Query("model") model: String = "statictable",
        @Query("domain") domain: String,
        @Query("page") page: Int,
        @Query("key") key: String
    ): Call<AllStaticTableResponse>


    // =====================================================
    // LIST VARIABLE
    // =====================================================

    @GET("list")
    fun getAllVariables(
        @Query("model") model: String = "var",
        @Query("domain") domain: String,
        @Query("page") page: Int,
        @Query("key") key: String
    ): Call<AllVariableResponse>


    // =====================================================
    // DETAIL SIMDASI
    // =====================================================

    @GET("interoperabilitas/datasource/simdasi/id/25/")
    fun getSimdasiDetail(
        @Query("wilayah") wilayah: String,
        @Query("tahun") tahun: Int,
        @Query("id_tabel") idTabel: String,
        @Query("key") key: String
    ): Call<SimdasiDetailResponse>


    // =====================================================
    // DETAIL STATIC TABLE
    // =====================================================

    @GET("view")
    fun getStaticTableDetail(
        @Query("domain") domain: String,
        @Query("model") model: String = "statictable",
        @Query("lang") lang: String = "ind",
        @Query("id") id: String,
        @Query("key") key: String
    ): Call<StaticTableDetailResponse>


    // =====================================================
    // DETAIL VARIABLE
    // =====================================================

    @GET("list/model/data/lang/ind/domain/{domain}/var/{varId}/th/{th}/key/{key}")
    fun getVariableDetail(
        @Path("domain") domain: String,
        @Path("varId") varId: String,
        @Path("th") th: Int,
        @Path("key") key: String
    ): Call<VariableDetailResponse>
}