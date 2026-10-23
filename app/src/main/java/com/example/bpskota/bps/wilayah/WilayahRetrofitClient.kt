package com.example.bpskota.bps.wilayah

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WilayahRetrofitClient {

    private const val BASE_URL = "https://wilayah.id/api/"

    val api: WilayahApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(WilayahApiService::class.java)
    }
}