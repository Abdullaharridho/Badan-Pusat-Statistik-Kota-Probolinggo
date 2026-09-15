package com.example.bpskota

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bps.api.BpsRetrofitClient
import com.example.bpskota.bps.model.Wilayah
import com.example.bpskota.bps.model.WilayahResponse
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MasterwilayahActivity : AppCompatActivity() {

    companion object {
        private const val DOMAIN = "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"
    }

    private lateinit var tvTotalKecamatan: TextView
    private lateinit var listKecamatan: LinearLayout

    private lateinit var activityTracker: ActivityTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_masterwilayah)

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Masterwilayah"
        )

        findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        tvTotalKecamatan =
            findViewById(R.id.tvTotalKecamatan)

        listKecamatan =
            findViewById(R.id.listKecamatan)

        loadWilayah()
    }

    private fun loadWilayah() {

        BpsRetrofitClient.api
            .getWilayah(
                model = "vervar",
                domain = DOMAIN,
                page = 1,
                apiKey = API_KEY
            )
            .enqueue(object : Callback<WilayahResponse> {

                override fun onResponse(
                    call: Call<WilayahResponse>,
                    response: Response<WilayahResponse>
                ) {

                    if (!response.isSuccessful) {

                        showError(
                            "Gagal mengambil data wilayah (${response.code()})"
                        )

                        return
                    }

                    val body = response.body()

                    if (body == null) {

                        showError(
                            "Response API kosong"
                        )

                        return
                    }

                    if (body.status != "OK") {

                        showError(
                            "Status API tidak OK"
                        )

                        return
                    }

                    val wilayahList =
                        parseWilayah(body)

                    val kecamatanList =
                        wilayahList.filter { wilayah ->

                            wilayah.groupVerId == 1 &&
                                    wilayah.kodeVerId.toString()
                                        .length == 7 &&
                                    wilayah.kodeVerId != 3574000
                        }

                    tampilkanKecamatan(
                        kecamatanList
                    )
                }

                override fun onFailure(
                    call: Call<WilayahResponse>,
                    t: Throwable
                ) {

                    showError(
                        "Gagal terhubung ke BPS: ${t.message}"
                    )
                }
            })
    }

    private fun parseWilayah(
        response: WilayahResponse
    ): List<Wilayah> {

        val result =
            mutableListOf<Wilayah>()

        val data =
            response.data ?: return result

        val gson =
            Gson()

        for (element in data) {

            if (element.isJsonArray) {

                val array =
                    element.asJsonArray

                for (item in array) {

                    try {

                        val wilayah =
                            gson.fromJson(
                                item,
                                Wilayah::class.java
                            )

                        result.add(wilayah)

                    } catch (_: Exception) {
                    }
                }
            }
        }

        return result
    }

    private fun tampilkanKecamatan(
        kecamatanList: List<Wilayah>
    ) {

        listKecamatan.removeAllViews()

        tvTotalKecamatan.text =
            "${kecamatanList.size} Kecamatan"

        for (kecamatan in kecamatanList) {

            val card =
                LayoutInflater.from(this)
                    .inflate(
                        R.layout.item_statistik,
                        listKecamatan,
                        false
                    )

            val tvNama =
                card.findViewById<TextView>(
                    R.id.tvJudul
                )

            val tvKode =
                card.findViewById<TextView>(
                    R.id.tvKode
                )

            tvNama.text =
                kecamatan.vervar

            tvKode.text =
                kecamatan.kodeVerId.toString()

            card.setOnClickListener {

                Toast.makeText(
                    this,
                    "${kecamatan.vervar}\nKode: ${kecamatan.kodeVerId}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            listKecamatan.addView(card)
        }
    }

    private fun showError(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}