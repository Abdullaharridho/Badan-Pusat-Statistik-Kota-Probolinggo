package com.example.bpskota

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.airbnb.lottie.LottieAnimationView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bps.model.TenagaKerjaResponse
import com.example.bpskota.bps.repository.BpsRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class GenderActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "GenderAPI"

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

        initView()

        setupButton()

        loadSemuaGender()
    }

    // =========================================================
    // INIT VIEW
    // =========================================================

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

    // =========================================================
    // BUTTON
    // =========================================================

    private fun setupButton() {

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {

            tampilkanFilterTahun()
        }
    }

    // =========================================================
    // FILTER TAHUN
    // =========================================================

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

                    loadSemuaGender()
                }
            }
            .setNegativeButton(
                "Batal",
                null
            )
            .show()
    }

    // =========================================================
    // KONVERSI TAHUN KE KODE BPS
    // =========================================================

    private fun getKodeTahun(
        tahun: Int
    ): Int {

        return tahun - 1900
    }

    // =========================================================
    // LOAD SEMUA GENDER
    // =========================================================

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

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "AMBIL DATA GENDER"
        )

        Log.d(
            TAG,
            "TAHUN DIPILIH = $tahunDipilih"
        )

        Log.d(
            TAG,
            "KODE TAHUN = $kodeTahun"
        )

        Log.d(
            TAG,
            "VARIABLE = $VARIABLE_LIST"
        )

        Log.d(
            TAG,
            "========================================"
        )

        VARIABLE_LIST.forEach { variableId ->

            loadGenderVariable(
                variableId,
                kodeTahun
            )
        }
    }

    // =========================================================
    // REQUEST SATU VARIABLE
    // =========================================================

    private fun loadGenderVariable(
        variableId: Int,
        kodeTahun: Int
    ) {

        Log.d(
            TAG,
            "REQUEST VARIABLE = $variableId"
        )

        Log.d(
            TAG,
            "TAHUN = $tahunDipilih"
        )

        Log.d(
            TAG,
            "KODE TAHUN = $kodeTahun"
        )

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

                    Log.d(
                        TAG,
                        "----------------------------------------"
                    )

                    Log.d(
                        TAG,
                        "VARIABLE = $variableId"
                    )

                    Log.d(
                        TAG,
                        "URL = ${call.request().url}"
                    )

                    Log.d(
                        TAG,
                        "HTTP = ${response.code()}"
                    )

                    if (!response.isSuccessful) {

                        Log.e(
                            TAG,
                            "HTTP ERROR VARIABLE $variableId"
                        )

                        requestSelesai()

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        Log.e(
                            TAG,
                            "BODY KOSONG VARIABLE $variableId"
                        )

                        requestSelesai()

                        return
                    }

                    Log.d(
                        TAG,
                        "STATUS = ${body.status}"
                    )

                    Log.d(
                        TAG,
                        "AVAILABILITY = ${body.dataAvailability}"
                    )

                    Log.d(
                        TAG,
                        "TAHUN = ${body.tahun}"
                    )

                    Log.d(
                        TAG,
                        "VARIABLE RESPONSE = ${body.variable}"
                    )

                    Log.d(
                        TAG,
                        "DATA CONTENT = ${body.dataContent}"
                    )

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

                        Log.d(
                            TAG,
                            "DATA TIDAK TERSEDIA VARIABLE $variableId"
                        )

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

                    Log.e(
                        TAG,
                        "GAGAL VARIABLE $variableId",
                        t
                    )

                    requestSelesai()
                }
            }
        )
    }

    // =========================================================
    // PROSES DATA GENDER
    // =========================================================

    private fun prosesDataGender(
        variableId: Int,
        body: TenagaKerjaResponse
    ) {

        val dataContent =
            body.dataContent
                ?: emptyMap()

        if (dataContent.isEmpty()) {

            Log.e(
                TAG,
                "DATA CONTENT KOSONG VARIABLE $variableId"
            )

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

        /*
         * Karena endpoint yang dipanggil sudah
         * menggunakan kode tahun yang dipilih,
         * dataContent biasanya hanya berisi
         * data untuk tahun tersebut.
         *
         * Jadi kita ambil data yang tersedia.
         */

        val nilai =
            dataContent.values.firstOrNull()

        if (nilai == null) {

            Log.e(
                TAG,
                "NILAI NULL VARIABLE $variableId"
            )

            return
        }

        val nilaiFormat =
            String.format(
                Locale.US,
                "%.${decimal}f",
                nilai
            )

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "HASIL GENDER"
        )

        Log.d(
            TAG,
            "VARIABLE = $variableId"
        )

        Log.d(
            TAG,
            "TAHUN = $tahunLabel"
        )

        Log.d(
            TAG,
            "JUDUL = $judul"
        )

        Log.d(
            TAG,
            "NILAI = $nilaiFormat"
        )

        Log.d(
            TAG,
            "SATUAN = $satuan"
        )

        Log.d(
            TAG,
            "========================================"
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

    // =========================================================
    // JUDUL VARIABLE
    // =========================================================

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

    // =========================================================
    // REQUEST SELESAI
    // =========================================================

    private fun requestSelesai() {

        jumlahRequestSelesai++

        Log.d(
            TAG,
            "REQUEST SELESAI = $jumlahRequestSelesai/${VARIABLE_LIST.size}"
        )

        if (
            jumlahRequestSelesai >=
            VARIABLE_LIST.size
        ) {

            runOnUiThread {

                tampilkanSemuaData()
            }
        }
    }

    // =========================================================
    // TAMPILKAN SEMUA DATA
    // =========================================================

    private fun tampilkanSemuaData() {

        cardContainer.removeAllViews()

        /*
         * Urutan:
         *
         * 1. IKG
         * 2. IPG
         * 3. IDG
         * 4. IPG UHH SP2020 LF
         */

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

        Log.d(
            TAG,
            "TOTAL CARD = ${dataUrut.size}"
        )
    }

    // =========================================================
    // DESTROY
    // =========================================================

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