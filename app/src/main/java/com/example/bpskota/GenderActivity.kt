package com.example.bpskota

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bps.model.TenagaKerjaResponse
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class GenderActivity : AppCompatActivity() {

    companion object {

        private const val DOMAIN = "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private val VARIABLE_LIST = listOf(
            134,
            49,
            47,
            121
        )

        private val TAHUN_LIST = (2010..2026).toList()
    }

    private val repository =
        BpsRepository()

    private lateinit var activityTracker: ActivityTracker

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView

    private var tahunDipilih = 2025

    private var jumlahRequestSelesai = 0

    private val dataGender =
        mutableListOf<GenderData>()

    data class GenderData(
        val variableId: Int,
        val tahun: String,
        val kategori: String,
        val judul: String,
        val nilai: String,
        val satuan: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_gender
        )

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Gender",
            metadata = mapOf(
                "tahun" to tahunDipilih
            )
        )

        initView()

        setupButton()

        loadSemuaGender()
    }

    private fun initView() {

        cardContainer =
            findViewById(
                R.id.cardContainer
            )

        progressLoading =
            findViewById(
                R.id.progressLoading
            )

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        progressLoading.repeatCount = -1

        btnBack =
            findViewById(
                R.id.btnBack
            )

        btnFilter =
            findViewById(
                R.id.btnFilter
            )
    }

    private fun setupButton() {

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {

            tampilkanFilterTahun()
        }
    }

    private fun tampilkanFilterTahun() {

        val tahunArray =
            TAHUN_LIST
                .reversed()
                .map {
                    it.toString()
                }
                .toTypedArray()

        var posisiTerpilih =
            tahunArray.indexOf(
                tahunDipilih.toString()
            )

        if (posisiTerpilih < 0) {
            posisiTerpilih = 0
        }

        AlertDialog.Builder(this)
            .setTitle("Pilih Tahun")
            .setSingleChoiceItems(
                tahunArray,
                posisiTerpilih
            ) { dialog, which ->

                val tahunBaru =
                    tahunArray[which].toInt()

                dialog.dismiss()

                if (
                    tahunBaru != tahunDipilih
                ) {

                    tahunDipilih =
                        tahunBaru

                    activityTracker.trackScreen(
                        screen = "Gender",
                        metadata = mapOf(
                            "tahun" to tahunDipilih
                        )
                    )

                    loadSemuaGender()
                }
            }
            .setNegativeButton(
                "Batal",
                null
            )
            .show()
    }

    private fun getKodeTahun(
        tahun: Int
    ): Int {

        return tahun - 1900
    }

    private fun loadSemuaGender() {

        progressLoading.visibility =
            View.VISIBLE

        progressLoading.playAnimation()

        cardContainer.removeAllViews()

        dataGender.clear()

        jumlahRequestSelesai = 0

        val kodeTahun =
            getKodeTahun(
                tahunDipilih
            )

        VARIABLE_LIST.forEach { variableId ->

            loadGenderVariable(
                variableId,
                kodeTahun
            )
        }
    }

    private fun loadGenderVariable(
        variableId: Int,
        kodeTahun: Int
    ) {

        repository.getTenagaKerja(
            domain = DOMAIN,
            variable = variableId,
            tahun = kodeTahun,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<TenagaKerjaResponse> {

                override fun onResponse(
                    call: Call<TenagaKerjaResponse>,
                    response: Response<TenagaKerjaResponse>
                ) {

                    if (!response.isSuccessful) {

                        requestSelesai()

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        requestSelesai()

                        return
                    }

                    if (
                        body.status
                            ?.uppercase(Locale.ROOT) !=
                        "OK"
                    ) {

                        requestSelesai()

                        return
                    }

                    if (
                        body.dataAvailability
                            ?.lowercase(Locale.ROOT) !=
                        "available"
                    ) {

                        requestSelesai()

                        return
                    }

                    prosesDataGender(
                        variableId,
                        body
                    )

                    requestSelesai()
                }

                override fun onFailure(
                    call: Call<TenagaKerjaResponse>,
                    t: Throwable
                ) {

                    requestSelesai()
                }
            }
        )
    }

    private fun prosesDataGender(
        variableId: Int,
        body: TenagaKerjaResponse
    ) {

        val dataContent =
            body.dataContent
                ?: emptyMap()

        if (dataContent.isEmpty()) {
            return
        }

        val tahunApi =
            body.tahun?.firstOrNull()

        val tahunLabel =
            tahunApi?.label
                ?: tahunDipilih.toString()

        val variable =
            body.variable?.firstOrNull()

        val judul =
            variable?.label
                ?: getJudulVariable(
                    variableId
                )

        val satuan =
            variable?.unit
                ?: ""

        val decimal =
            variable?.decimal
                ?: 2

        val nilai =
            dataContent.values.firstOrNull()

        if (nilai == null) {
            return
        }

        val nilaiFormat =
            String.format(
                Locale.US,
                "%.${decimal}f",
                nilai
            )

        dataGender.add(
            GenderData(
                variableId = variableId,
                tahun = tahunLabel,
                kategori = "Gender",
                judul = judul,
                nilai = nilaiFormat,
                satuan = satuan
            )
        )
    }

    private fun getJudulVariable(
        variableId: Int
    ): String {

        return when (variableId) {

            134 ->
                "Indeks Ketimpangan Gender (IKG)"

            49 ->
                "Indeks Pembangunan Gender (IPG) Kota Probolinggo"

            47 ->
                "Indeks Pemberdayaan Gender (IDG) Kota Probolinggo"

            121 ->
                "Indeks Pembangunan Gender (menggunakan UHH hasil SP2020 LF)"

            else ->
                "Statistik Gender"
        }
    }

    private fun requestSelesai() {

        jumlahRequestSelesai++

        if (
            jumlahRequestSelesai >=
            VARIABLE_LIST.size
        ) {

            runOnUiThread {

                tampilkanSemuaData()
            }
        }
    }

    private fun tampilkanSemuaData() {

        cardContainer.removeAllViews()

        val dataUrut =
            VARIABLE_LIST.mapNotNull { id ->

                dataGender.firstOrNull {
                    it.variableId == id
                }
            }

        dataUrut.forEachIndexed { index, data ->

            val card =
                LayoutInflater
                    .from(this)
                    .inflate(
                        R.layout.item_statistik,
                        cardContainer,
                        false
                    )

            val tvKode =
                card.findViewById<TextView>(
                    R.id.tvKode
                )

            val tvTahun =
                card.findViewById<TextView>(
                    R.id.tvTahun
                )

            val tvKategori =
                card.findViewById<TextView>(
                    R.id.tvKategori
                )

            val tvJudul =
                card.findViewById<TextView>(
                    R.id.tvJudul
                )

            val tvNilai =
                card.findViewById<TextView>(
                    R.id.tvNilai
                )

            tvTahun.text =
                data.tahun

            tvKategori.text =
                data.kategori

            tvJudul.text =
                data.judul

            tvNilai.text =
                if (data.satuan.isNotBlank()) {

                    "${data.nilai} ${data.satuan}"

                } else {

                    data.nilai
                }

            card.setOnClickListener {

                Toast.makeText(
                    this,
                    "${data.judul}\nTahun: ${data.tahun}\nNilai: ${data.nilai}",
                    Toast.LENGTH_LONG
                ).show()
            }

            cardContainer.addView(
                card
            )
        }

        progressLoading.visibility =
            View.GONE

        if (dataUrut.isEmpty()) {

            Toast.makeText(
                this,
                "Tidak ada data Gender untuk tahun $tahunDipilih",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {

        if (
            ::progressLoading.isInitialized
        ) {

            progressLoading.cancelAnimation()

            progressLoading.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}