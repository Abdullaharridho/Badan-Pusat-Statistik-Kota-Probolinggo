package com.example.bpskota.bps.wilayah

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface WilayahApiService {

    // Daftar kecamatan berdasarkan kode kota/kabupaten
    @GET("districts/{regencyCode}.json")
    fun getKecamatan(
        @Path("regencyCode") regencyCode: String
    ): Call<WilayahResponse>

    // Daftar kelurahan berdasarkan kode kecamatan
    @GET("villages/{districtCode}.json")
    fun getKelurahan(
        @Path("districtCode") districtCode: String
    ): Call<WilayahResponse>
}