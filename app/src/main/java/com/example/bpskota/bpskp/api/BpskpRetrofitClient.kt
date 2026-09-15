package com.example.bpskota.bpskp.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BpskpRetrofitClient {

    private const val BASE_URL =
        "http://172.16.16.42:8000/api/"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->

            val request = chain.request()
                .newBuilder()
                .addHeader("Accept", "application/json")
                .build()

            chain.proceed(request)
        }
        .build()

    val api: BpskpApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(BpskpApiService::class.java)
    }
}