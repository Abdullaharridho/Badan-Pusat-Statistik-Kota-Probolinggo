package com.example.bpskota

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.TempatTinggalResponse
import com.example.bpskota.bps.model.TempatTinggalVariable
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TempattinggalActivity : AppCompatActivity() {

    companion object {

        private const val TAG =
            "TEMPAT_TINGGAL"

        private const val DOMAIN =
            "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val SUBCSA_ID =
            563

        private const val SUBCSA_NAME =
            "Kondisi Tempat Tinggal, Kemiskinan, dan Permasalahan Sosial Lintas Sektor"

        private const val TOTAL_PAGE =
            12

        private const val TAHUN_DEFAULT =
            2025

        private const val TAHUN_MULAI =
            2025

        private const val TAHUN_MINIMUM =
            2020
    }

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnFilter: ImageView
    private lateinit var tvHeaderTitle: TextView

    private val repository =
        BpsRepository()

    private val gson =
        Gson()

    private val hasilPerPage =
        mutableMapOf<Int, List<TempatTinggalVariable>>()

    private val semuaVariable =
        mutableListOf<TempatTinggalVariable>()

    private var requestSelesai =
        0

    private var requestGagal =
        0

    private var tahunTerpilih =
        TAHUN_DEFAULT

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_tempattinggal
        )

        initView()

        setupButton()

        tvHeaderTitle.text =
            "$SUBCSA_NAME ($TAHUN_DEFAULT)"

        loadSemuaHalaman()
    }

    private fun initView() {

        val btnBack =
            findViewById<ImageView>(
                R.id.btnBack
            )

        btnFilter =
            findViewById(
                R.id.btnFilter
            )

        cardContainer =
            findViewById(
                R.id.cardContainer
            )

        progressLoading =
            findViewById(
                R.id.progressLoading
            )

        tvHeaderTitle =
            findViewById(
                R.id.tvHeaderTitle
            )

        // ========================================================
        // LOTTIE LOADING
        // ========================================================

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        btnBack.setOnClickListener {

            finish()
        }
    }

    private fun setupButton() {

        btnFilter.setOnClickListener {

            tampilkanDialogFilterTahun()
        }
    }

    private fun loadSemuaHalaman() {

        hasilPerPage.clear()

        semuaVariable.clear()

        requestSelesai = 0

        requestGagal = 0

        cardContainer.removeAllViews()

        // ========================================================
        // MULAI LOTTIE
        // ========================================================

        progressLoading.visibility =
            View.VISIBLE

        progressLoading.playAnimation()

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "MULAI LOAD $TOTAL_PAGE HALAMAN"
        )

        Log.d(
            TAG,
            "========================================"
        )

        for (page in 1..TOTAL_PAGE) {

            loadPage(page)
        }
    }

    private fun loadPage(
        page: Int
    ) {

        repository
            .getTempatTinggalVariables(
                domain = DOMAIN,
                page = page,
                apiKey = API_KEY
            )
            .enqueue(
                object :
                    Callback<TempatTinggalResponse> {

                    override fun onResponse(
                        call: Call<TempatTinggalResponse>,
                        response: Response<TempatTinggalResponse>
                    ) {

                        if (!response.isSuccessful) {

                            requestGagal++

                            hasilPerPage[page] =
                                emptyList()

                            requestSelesai++

                            cekSemuaRequestSelesai()

                            return
                        }

                        val body =
                            response.body()

                        if (body == null) {

                            requestGagal++

                            hasilPerPage[page] =
                                emptyList()

                            requestSelesai++

                            cekSemuaRequestSelesai()

                            return
                        }

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {

                            requestGagal++

                            hasilPerPage[page] =
                                emptyList()

                            requestSelesai++

                            cekSemuaRequestSelesai()

                            return
                        }

                        val hasil =
                            parsePage(body)

                        hasilPerPage[page] =
                            hasil

                        requestSelesai++

                        Log.d(
                            TAG,
                            "PAGE $page = ${hasil.size}"
                        )

                        cekSemuaRequestSelesai()
                    }

                    override fun onFailure(
                        call: Call<TempatTinggalResponse>,
                        t: Throwable
                    ) {

                        Log.e(
                            TAG,
                            "ERROR PAGE $page",
                            t
                        )

                        requestGagal++

                        hasilPerPage[page] =
                            emptyList()

                        requestSelesai++

                        cekSemuaRequestSelesai()
                    }
                }
            )
    }

    private fun parsePage(
        response: TempatTinggalResponse
    ): List<TempatTinggalVariable> {

        val hasil =
            mutableListOf<TempatTinggalVariable>()

        val data =
            response.data
                ?: return hasil

        if (data.size < 2) {
            return hasil
        }

        val variableElement =
            data[1]

        if (!variableElement.isJsonArray) {
            return hasil
        }

        val array =
            variableElement.asJsonArray

        for (element in array) {

            if (!element.isJsonObject) {
                continue
            }

            val obj =
                element.asJsonObject

            val subcsaId =
                obj
                    .get("subcsa_id")
                    ?.asInt

            if (subcsaId != SUBCSA_ID) {
                continue
            }

            try {

                val variable =
                    gson.fromJson(
                        obj,
                        TempatTinggalVariable::class.java
                    )

                hasil.add(
                    variable
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "GAGAL PARSE VARIABLE",
                    e
                )
            }
        }

        return hasil
    }

    private fun cekSemuaRequestSelesai() {

        if (
            requestSelesai < TOTAL_PAGE
        ) {
            return
        }

        runOnUiThread {

            // ====================================================
            // STOP LOTTIE
            // ====================================================

            progressLoading.cancelAnimation()

            progressLoading.visibility =
                View.GONE

            semuaVariable.clear()

            for (page in 1..TOTAL_PAGE) {

                semuaVariable.addAll(
                    hasilPerPage[page]
                        ?: emptyList()
                )
            }

            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "SEMUA PAGE SELESAI"
            )

            Log.d(
                TAG,
                "TOTAL VARIABLE = ${semuaVariable.size}"
            )

            Log.d(
                TAG,
                "REQUEST GAGAL = $requestGagal"
            )

            Log.d(
                TAG,
                "========================================"
            )

            if (
                semuaVariable.isEmpty()
            ) {

                tampilkanPesan(
                    "Tidak ada statistik pada kategori ini"
                )

                return@runOnUiThread
            }

            tampilkanHasil()
        }
    }

    private fun tampilkanHasil() {

        cardContainer.removeAllViews()

        updateHeader(
            tahunTerpilih
        )

        if (
            semuaVariable.isEmpty()
        ) {

            tampilkanPesan(
                "Tidak ada statistik pada kategori ini"
            )

            return
        }

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "MENAMPILKAN CARD"
        )

        Log.d(
            TAG,
            "TAHUN = $tahunTerpilih"
        )

        Log.d(
            TAG,
            "JUMLAH CARD = ${semuaVariable.size}"
        )

        Log.d(
            TAG,
            "========================================"
        )

        for (item in semuaVariable) {

            tambahCard(
                item
            )
        }
    }

    private fun tambahCard(
        item: TempatTinggalVariable
    ) {

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    cardContainer,
                    false
                )

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        val tvTahun =
            card.findViewById<TextView>(
                R.id.tvTahun
            )

        tvJudul.text =
            item.title
                ?: "Judul tidak tersedia"

        tvTahun.text =
            tahunTerpilih.toString()

        card.setOnClickListener {

            bukaDetail(
                item
            )
        }

        cardContainer.addView(
            card
        )
    }

    private fun bukaDetail(
        item: TempatTinggalVariable
    ) {

        val varId =
            item.varId

        if (varId == null) {

            Toast.makeText(
                this,
                "ID variabel tidak tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "BUKA DETAIL"
        )

        Log.d(
            TAG,
            "VAR ID = $varId"
        )

        Log.d(
            TAG,
            "JUDUL = ${item.title}"
        )

        Log.d(
            TAG,
            "TAHUN = $tahunTerpilih"
        )

        Log.d(
            TAG,
            "KODE TAHUN = ${kodeTahunBps(tahunTerpilih)}"
        )

        Log.d(
            TAG,
            "========================================"
        )

        val intent =
            Intent(
                this,
                TempatTinggalDetailActivity::class.java
            )

        intent.putExtra(
            TempatTinggalDetailActivity.EXTRA_VAR_ID,
            varId
        )

        intent.putExtra(
            TempatTinggalDetailActivity.EXTRA_JUDUL,
            item.title
        )

        intent.putExtra(
            TempatTinggalDetailActivity.EXTRA_TAHUN,
            tahunTerpilih
        )

        startActivity(
            intent
        )
    }

    private fun tampilkanDialogFilterTahun() {

        if (
            semuaVariable.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Data belum selesai dimuat",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val tahunList =
            (TAHUN_MINIMUM..TAHUN_MULAI)
                .toList()
                .sortedDescending()

        val tahunArray =
            tahunList
                .map {
                    it.toString()
                }
                .toTypedArray()

        val checkedIndex =
            tahunList
                .indexOf(tahunTerpilih)
                .coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(
                "Pilih Tahun"
            )
            .setSingleChoiceItems(
                tahunArray,
                checkedIndex
            ) { dialog, which ->

                val tahunBaru =
                    tahunList[which]

                dialog.dismiss()

                pilihTahun(
                    tahunBaru
                )
            }
            .show()
    }

    private fun pilihTahun(
        tahun: Int
    ) {

        if (
            tahun == tahunTerpilih
        ) {
            return
        }

        tahunTerpilih =
            tahun

        updateHeader(
            tahun
        )

        cardContainer.removeAllViews()

        // ========================================================
        // TIDAK ADA REQUEST BARU
        //
        // Card tetap ditampilkan.
        // Ketersediaan data dicek ketika detail dibuka.
        // ========================================================

        tampilkanHasil()
    }

    private fun kodeTahunBps(
        tahun: Int
    ): Int {

        return tahun - 1900
    }

    private fun updateHeader(
        tahun: Int
    ) {

        tvHeaderTitle.text =
            "$SUBCSA_NAME ($tahun)"
    }

    private fun tampilkanPesan(
        pesan: String
    ) {

        cardContainer.removeAllViews()

        val tv =
            TextView(this)

        tv.text =
            pesan

        tv.textSize =
            15f

        tv.gravity =
            Gravity.CENTER

        tv.setTextColor(
            Color.DKGRAY
        )

        tv.setPadding(
            20,
            40,
            20,
            40
        )

        cardContainer.addView(
            tv
        )
    }
}