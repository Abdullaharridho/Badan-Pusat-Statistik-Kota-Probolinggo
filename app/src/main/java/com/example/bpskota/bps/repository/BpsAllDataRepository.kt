package com.example.bpskota.bps.repository

import com.example.bpskota.bps.api.BpsRetrofitClient
import com.example.bpskota.bps.model.AllSimdasiResponse
import com.example.bpskota.bps.model.AllStaticTableResponse
import com.example.bpskota.bps.model.AllVariableResponse
import com.example.bpskota.bps.model.SimdasiDetailResponse
import com.example.bpskota.bps.model.StaticTableDetailResponse
import com.example.bpskota.bps.model.VariableDetailResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BpsAllDataRepository {

    private val api = BpsRetrofitClient.allDataApi


    // =====================================================
    // LIST SIMDASI
    // =====================================================

    fun getAllSimdasi(
        wilayah: String,
        page: Int,
        apiKey: String,
        callback: (AllSimdasiResponse?, Throwable?) -> Unit
    ) {

        api.getAllSimdasi(
            wilayah = wilayah,
            page = page,
            key = apiKey
        ).enqueue(
            object : Callback<AllSimdasiResponse> {

                override fun onResponse(
                    call: Call<AllSimdasiResponse>,
                    response: Response<AllSimdasiResponse>
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
                                "HTTP ${response.code()}: ${response.message()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<AllSimdasiResponse>,
                    t: Throwable
                ) {

                    callback(
                        null,
                        t
                    )
                }
            }
        )
    }


    // =====================================================
    // LIST STATIC TABLE
    // =====================================================

    fun getAllStaticTables(
        domain: String,
        page: Int,
        apiKey: String,
        callback: (AllStaticTableResponse?, Throwable?) -> Unit
    ) {

        api.getAllStaticTables(
            model = "statictable",
            domain = domain,
            page = page,
            key = apiKey
        ).enqueue(
            object : Callback<AllStaticTableResponse> {

                override fun onResponse(
                    call: Call<AllStaticTableResponse>,
                    response: Response<AllStaticTableResponse>
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
                                "HTTP ${response.code()}: ${response.message()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<AllStaticTableResponse>,
                    t: Throwable
                ) {

                    callback(
                        null,
                        t
                    )
                }
            }
        )
    }


    // =====================================================
    // LIST VARIABLE
    // =====================================================

    fun getAllVariables(
        domain: String,
        page: Int,
        apiKey: String,
        callback: (AllVariableResponse?, Throwable?) -> Unit
    ) {

        api.getAllVariables(
            model = "var",
            domain = domain,
            page = page,
            key = apiKey
        ).enqueue(
            object : Callback<AllVariableResponse> {

                override fun onResponse(
                    call: Call<AllVariableResponse>,
                    response: Response<AllVariableResponse>
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
                                "HTTP ${response.code()}: ${response.message()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<AllVariableResponse>,
                    t: Throwable
                ) {

                    callback(
                        null,
                        t
                    )
                }
            }
        )
    }


    // =====================================================
    // DETAIL SIMDASI
    // =====================================================

    fun getSimdasiDetail(
        wilayah: String,
        tahun: Int,
        idTabel: String,
        apiKey: String,
        callback: (SimdasiDetailResponse?, Throwable?) -> Unit
    ) {

        api.getSimdasiDetail(
            wilayah = wilayah,
            tahun = tahun,
            idTabel = idTabel,
            key = apiKey
        ).enqueue(
            object : Callback<SimdasiDetailResponse> {

                override fun onResponse(
                    call: Call<SimdasiDetailResponse>,
                    response: Response<SimdasiDetailResponse>
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
                                "HTTP ${response.code()}: ${response.message()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<SimdasiDetailResponse>,
                    t: Throwable
                ) {

                    callback(
                        null,
                        t
                    )
                }
            }
        )
    }


    // =====================================================
    // DETAIL STATIC TABLE
    // =====================================================

    fun getStaticTableDetail(
        domain: String,
        id: String,
        apiKey: String,
        callback: (StaticTableDetailResponse?, Throwable?) -> Unit
    ) {

        api.getStaticTableDetail(
            domain = domain,
            model = "statictable",
            lang = "ind",
            id = id,
            key = apiKey
        ).enqueue(
            object : Callback<StaticTableDetailResponse> {

                override fun onResponse(
                    call: Call<StaticTableDetailResponse>,
                    response: Response<StaticTableDetailResponse>
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
                                "HTTP ${response.code()}: ${response.message()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<StaticTableDetailResponse>,
                    t: Throwable
                ) {

                    callback(
                        null,
                        t
                    )
                }
            }
        )
    }


    // =====================================================
    // DETAIL VARIABLE
    // =====================================================

    fun getVariableDetail(
        domain: String,
        varId: String,
        tahun: Int,
        apiKey: String,
        callback: (VariableDetailResponse?, Throwable?) -> Unit
    ) {

        // Contoh:
        // tahun = 2025
        // th = 125

        val th = tahun - 1900

        api.getVariableDetail(
            domain = domain,
            varId = varId,
            th = th,
            key = apiKey
        ).enqueue(
            object : Callback<VariableDetailResponse> {

                override fun onResponse(
                    call: Call<VariableDetailResponse>,
                    response: Response<VariableDetailResponse>
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
                                "HTTP ${response.code()}: ${response.message()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<VariableDetailResponse>,
                    t: Throwable
                ) {

                    callback(
                        null,
                        t
                    )
                }
            }
        )
    }
}