package com.example.bpskota.bpskp.api

import android.net.Uri
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BpskpRetrofitClient {

    private const val SERVER_URL = "http://192.168.8.159:8000"

    private const val BASE_URL = "$SERVER_URL/api/"

    /**
     * Mengubah path dokumen Laravel menjadi URL yang dapat dibuka.
     */
    fun getDokumenUrl(path: String): String {
        val cleanPath = path
            .trim()
            .removePrefix("/")
            .removePrefix("storage/")

        val encodedPath = Uri.encode(cleanPath, "/")

        return "$SERVER_URL/storage/$encodedPath"
    }

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