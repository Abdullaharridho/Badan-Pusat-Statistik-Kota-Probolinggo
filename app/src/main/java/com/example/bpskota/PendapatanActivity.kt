package com.example.bpskota

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.KonsumsiDetailResponse
import com.example.bpskota.bps.model.KonsumsiResponse
import com.example.bpskota.bps.model.KonsumsiVariable
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PendapatanActivity : AppCompatActivity() {

    companion object {

        private const val DOMAIN = "3574"

        private const val SUBCSA_ID = 523

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val TAHUN_MULAI = 2026

        private const val TAHUN_MINIMUM = 2020
    }

    private val repository =
        BpsRepository()

    private val semuaVariabel =
        mutableListOf<KonsumsiVariable>()

    private val variabelTampil =
        mutableListOf<KonsumsiVariable>()

    /**
     * Menyimpan hasil pengecekan tahun.
     *
     * Contoh:
     *
     * 2026 -> [85]
     * 2025 -> [85, 86, 89]
     * 2024 -> [85, 89]
     */
    private val cacheTahun =
        mutableMapOf<Int, MutableList<Int>>()

    private var tahunTerpilih: Int? =
        null

    private lateinit var cardContainer: LinearLayout

    private lateinit var progressLoading: LottieAnimationView

    private lateinit var btnFilter: ImageView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_pendapatan
        )

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

        // ========================================================
        // LOAD LOTTIE ANIMATION
        // ========================================================

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        btnBack.setOnClickListener {

            finish()
        }

        btnFilter.setOnClickListener {

            tampilkanDialogTahun()
        }

        loadSemuaPage(
            1
        )
    }

    // ============================================================
    // LOAD VARIABLE
    // ============================================================

    private fun loadSemuaPage(
        page: Int
    ) {

        if (page == 1) {

            semuaVariabel.clear()

            variabelTampil.clear()

            cacheTahun.clear()

            tahunTerpilih = null

            tampilkanLoading(
                true
            )
        }

        repository.getKonsumsiVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<KonsumsiResponse> {

                override fun onResponse(
                    call: Call<KonsumsiResponse>,
                    response: Response<KonsumsiResponse>
                ) {

                    if (!response.isSuccessful) {

                        tampilkanLoading(
                            false
                        )

                        Toast.makeText(
                            this@PendapatanActivity,
                            "Gagal mengambil data",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        tampilkanLoading(
                            false
                        )

                        Toast.makeText(
                            this@PendapatanActivity,
                            "Response kosong",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    if (
                        !body.status.equals(
                            "OK",
                            ignoreCase = true
                        )
                    ) {

                        tampilkanLoading(
                            false
                        )

                        Toast.makeText(
                            this@PendapatanActivity,
                            "Status API tidak OK",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val variableList =
                        parseVariableList(
                            body
                        )

                    for (
                    item in variableList
                    ) {

                        if (
                            item.subcsaId ==
                            SUBCSA_ID
                        ) {

                            if (
                                semuaVariabel.none {
                                    it.varId ==
                                            item.varId
                                }
                            ) {

                                semuaVariabel.add(
                                    item
                                )
                            }
                        }
                    }

                    val totalPage =
                        parseTotalPage(
                            body
                        )

                    if (
                        page < totalPage
                    ) {

                        loadSemuaPage(
                            page + 1
                        )

                    } else {

                        selesaiLoadVariable()
                    }
                }

                override fun onFailure(
                    call: Call<KonsumsiResponse>,
                    t: Throwable
                ) {

                    tampilkanLoading(
                        false
                    )

                    Toast.makeText(
                        this@PendapatanActivity,
                        "Koneksi gagal: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // ============================================================
    // SELESAI LOAD VARIABLE
    // ============================================================

    private fun selesaiLoadVariable() {

        if (
            semuaVariabel.isEmpty()
        ) {

            tampilkanLoading(
                false
            )

            Toast.makeText(
                this,
                "Data Konsumsi dan Pendapatan tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        /*
         * Cari tahun terbaru yang benar-benar
         * memiliki data.
         *
         * Tidak langsung menggunakan 2026.
         */
        cariTahunTerbaru(
            TAHUN_MULAI
        )
    }

    // ============================================================
    // CARI TAHUN TERBARU
    // ============================================================

    private fun cariTahunTerbaru(
        tahun: Int
    ) {

        if (
            tahun < TAHUN_MINIMUM
        ) {

            tampilkanLoading(
                false
            )

            Toast.makeText(
                this,
                "Tidak ada data tahun tersedia",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        /*
         * Kalau tahun sudah pernah dicek,
         * langsung gunakan hasil cache.
         */
        if (
            cacheTahun.containsKey(
                tahun
            )
        ) {

            val hasil =
                cacheTahun[tahun]
                    ?: mutableListOf()

            if (
                hasil.isNotEmpty()
            ) {

                tahunTerpilih =
                    tahun

                filterDariCache(
                    tahun
                )

            } else {

                cariTahunTerbaru(
                    tahun - 1
                )
            }

            return
        }

        tampilkanLoading(
            true
        )

        cekSatuTahun(
            tahun = tahun
        ) { variableDenganData ->

            cacheTahun[tahun] =
                variableDenganData.toMutableList()

            if (
                variableDenganData.isNotEmpty()
            ) {

                tahunTerpilih =
                    tahun

                tampilkanLoading(
                    false
                )

                filterDariCache(
                    tahun
                )

            } else {

                cariTahunTerbaru(
                    tahun - 1
                )
            }
        }
    }

    // ============================================================
    // CEK SATU TAHUN
    // ============================================================

    private fun cekSatuTahun(
        tahun: Int,
        selesai: (
            MutableList<Int>
        ) -> Unit
    ) {

        val variables =
            semuaVariabel.toList()

        val variableDenganData =
            mutableListOf<Int>()

        if (
            variables.isEmpty()
        ) {

            selesai(
                variableDenganData
            )

            return
        }

        /*
         * Contoh:
         *
         * 2026 - 1900 = 126
         * 2025 - 1900 = 125
         * 2024 - 1900 = 124
         */
        val th =
            tahun - 1900

        var jumlahSelesai =
            0

        for (
        variable in variables
        ) {

            val variableId =
                variable.varId

            if (
                variableId == null
            ) {

                jumlahSelesai++

                if (
                    jumlahSelesai ==
                    variables.size
                ) {

                    selesai(
                        variableDenganData
                    )
                }

                continue
            }

            repository.getKonsumsiDetail(
                domain = DOMAIN,
                variable = variableId,
                tahun = th,
                apiKey = API_KEY
            ).enqueue(
                object :
                    Callback<KonsumsiDetailResponse> {

                    override fun onResponse(
                        call: Call<KonsumsiDetailResponse>,
                        response: Response<KonsumsiDetailResponse>
                    ) {

                        var adaData =
                            false

                        if (
                            response.isSuccessful
                        ) {

                            val body =
                                response.body()

                            if (
                                body != null &&
                                body.status.equals(
                                    "OK",
                                    ignoreCase = true
                                )
                            ) {

                                adaData =
                                    dataContentMemilikiData(
                                        body.dataContent
                                    )

                                if (
                                    adaData
                                ) {

                                    val tahunList =
                                        body.tahun

                                    if (
                                        tahunList.isNullOrEmpty()
                                    ) {

                                        adaData =
                                            false

                                    } else {

                                        val tahunAda =
                                            tahunList.any {

                                                it.label
                                                    ?.toIntOrNull() ==
                                                        tahun
                                            }

                                        if (
                                            !tahunAda
                                        ) {

                                            adaData =
                                                false
                                        }
                                    }
                                }
                            }
                        }

                        if (
                            adaData
                        ) {

                            synchronized(
                                variableDenganData
                            ) {

                                if (
                                    !variableDenganData.contains(
                                        variableId
                                    )
                                ) {

                                    variableDenganData.add(
                                        variableId
                                    )
                                }
                            }
                        }

                        jumlahSelesai++

                        if (
                            jumlahSelesai ==
                            variables.size
                        ) {

                            selesai(
                                variableDenganData
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<KonsumsiDetailResponse>,
                        t: Throwable
                    ) {

                        jumlahSelesai++

                        if (
                            jumlahSelesai ==
                            variables.size
                        ) {

                            selesai(
                                variableDenganData
                            )
                        }
                    }
                }
            )
        }
    }

    // ============================================================
    // PARSE VARIABLE
    // ============================================================

    private fun parseVariableList(
        response: KonsumsiResponse
    ): List<KonsumsiVariable> {

        val result =
            mutableListOf<KonsumsiVariable>()

        val data =
            response.data
                ?: return result

        if (
            data.size < 2
        ) {

            return result
        }

        val variableElement =
            data[1]

        if (
            !variableElement.isJsonArray
        ) {

            return result
        }

        val array: JsonArray =
            variableElement.asJsonArray

        val gson =
            Gson()

        for (
        element in array
        ) {

            try {

                val variable =
                    gson.fromJson(
                        element,
                        KonsumsiVariable::class.java
                    )

                result.add(
                    variable
                )

            } catch (_: Exception) {

                // Abaikan variable yang gagal diparse
            }
        }

        return result
    }

    private fun parseTotalPage(
        response: KonsumsiResponse
    ): Int {

        val data =
            response.data
                ?: return 1

        if (
            data.isEmpty()
        ) {

            return 1
        }

        val pageElement =
            data[0]

        if (
            !pageElement.isJsonObject
        ) {

            return 1
        }

        return try {

            pageElement
                .asJsonObject
                .get("pages")
                ?.asInt
                ?: 1

        } catch (_: Exception) {

            1
        }
    }

    // ============================================================
    // FILTER TAHUN
    // ============================================================

    private fun tampilkanDialogTahun() {

        val daftarTahun =
            mutableListOf<Int>()

        for (
        tahun in TAHUN_MULAI downTo TAHUN_MINIMUM
        ) {

            daftarTahun.add(
                tahun
            )
        }

        val tahunArray =
            daftarTahun.toTypedArray()

        var selectedIndex =
            tahunArray.indexOf(
                tahunTerpilih
            )

        if (
            selectedIndex < 0
        ) {

            selectedIndex = 0
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Pilih Tahun"
            )
            .setSingleChoiceItems(
                tahunArray
                    .map {
                        it.toString()
                    }
                    .toTypedArray(),
                selectedIndex
            ) { dialog, which ->

                val tahunDipilih =
                    tahunArray[which]

                dialog.dismiss()

                tahunTerpilih =
                    tahunDipilih

                loadDataTahun(
                    tahunDipilih
                )
            }
            .show()
    }

    // ============================================================
    // LOAD DATA TAHUN
    // ============================================================

    private fun loadDataTahun(
        tahun: Int
    ) {

        /*
         * Kalau tahun sudah pernah dicek,
         * langsung gunakan cache.
         */
        if (
            cacheTahun.containsKey(
                tahun
            )
        ) {

            filterDariCache(
                tahun
            )

            return
        }

        tampilkanLoading(
            true
        )

        cekSatuTahun(
            tahun = tahun
        ) { variableDenganData ->

            cacheTahun[tahun] =
                variableDenganData.toMutableList()

            runOnUiThread {

                tampilkanLoading(
                    false
                )

                filterDariCache(
                    tahun
                )
            }
        }
    }

    // ============================================================
    // FILTER DARI CACHE
    // ============================================================

    private fun filterDariCache(
        tahun: Int
    ) {

        variabelTampil.clear()

        val variableIdList =
            cacheTahun[tahun]
                ?: mutableListOf()

        for (
        variable in semuaVariabel
        ) {

            val variableId =
                variable.varId
                    ?: continue

            if (
                variableIdList.contains(
                    variableId
                )
            ) {

                variabelTampil.add(
                    variable
                )
            }
        }

        tampilkanDaftar()
    }

    // ============================================================
    // TAMPILKAN CARD
    // ============================================================

    private fun tampilkanDaftar() {

        cardContainer.removeAllViews()

        if (
            variabelTampil.isEmpty()
        ) {

            val textView =
                TextView(this)

            textView.text =
                "Data tidak tersedia pada tahun $tahunTerpilih"

            textView.textSize =
                16f

            textView.setPadding(
                24,
                32,
                24,
                32
            )

            cardContainer.addView(
                textView
            )

            return
        }

        for (
        item in variabelTampil
        ) {

            val card =
                LayoutInflater.from(this)
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
                    ?: "Tanpa Judul"

            tvTahun.text =
                tahunTerpilih?.toString()
                    ?: "-"

            card.setOnClickListener {

                val variableId =
                    item.varId

                if (
                    variableId == null
                ) {

                    Toast.makeText(
                        this,
                        "ID variabel tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val tahun =
                    tahunTerpilih

                if (
                    tahun == null
                ) {

                    Toast.makeText(
                        this,
                        "Tahun belum dipilih",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                // =================================================
                // MASUK KE DETAIL
                // =================================================

                val intent =
                    Intent(
                        this,
                        PendapatanDetailActivity::class.java
                    )

                intent.putExtra(
                    "VARIABLE_ID",
                    variableId
                )

                intent.putExtra(
                    "TAHUN",
                    tahun
                )

                intent.putExtra(
                    "JUDUL",
                    item.title
                        ?: "Data Pendapatan"
                )

                startActivity(
                    intent
                )
            }

            cardContainer.addView(
                card
            )
        }
    }

    // ============================================================
    // CEK DATA
    // ============================================================

    private fun dataContentMemilikiData(
        dataContent: JsonElement?
    ): Boolean {

        if (
            dataContent == null
        ) {

            return false
        }

        if (
            dataContent.isJsonNull
        ) {

            return false
        }

        return when {

            dataContent.isJsonObject -> {

                dataContent
                    .asJsonObject
                    .size() > 0
            }

            dataContent.isJsonArray -> {

                dataContent
                    .asJsonArray
                    .size() > 0
            }

            else -> {

                true
            }
        }
    }

    // ============================================================
    // LOADING - LOTTIE
    // ============================================================

    private fun tampilkanLoading(
        tampil: Boolean
    ) {

        if (tampil) {

            progressLoading.visibility =
                View.VISIBLE

            progressLoading.playAnimation()

        } else {

            progressLoading.cancelAnimation()

            progressLoading.visibility =
                View.GONE
        }
    }
}