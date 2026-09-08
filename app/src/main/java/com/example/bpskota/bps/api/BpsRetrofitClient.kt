package com.example.bpskota.bps.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BpsRetrofitClient {

    private const val BASE_URL =
        "https://webapi.bps.go.id/v1/api/"

    val api: BpsApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(BpsApiService::class.java)

    }

    val allDataApi: BpsAllDataApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(BpsAllDataApiService::class.java)

    }

}