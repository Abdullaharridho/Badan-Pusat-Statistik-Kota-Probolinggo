package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.EkonomiListResponse
import com.example.bpskota.bps.model.EkonomiTable
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EkonomiActivity : AppCompatActivity() {

    private val repository = BpsRepository()

    private val domain = "3574"

    private val subject = 531

    private val apiKey =
        "008edaaae5d450b1913b31a2cef618c3"

    private val semuaTabel =
        mutableListOf<EkonomiTable>()

    private var tahunTerpilih: Int? = 2026

    private lateinit var activityTracker: ActivityTracker

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_ekonomi
        )

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Ekonomi",
            metadata = mapOf(
                "subject" to subject,
                "domain" to domain
            )
        )

        val btnBack =
            findViewById<ImageView>(
                R.id.btnBack
            )

        val cardContainer =
            findViewById<LinearLayout>(
                R.id.cardContainer
            )

        val btnFilter =
            findViewById<ImageView>(
                R.id.btnFilter
            )

        val lottieLoading =
            findViewById<LottieAnimationView>(
                R.id.lottieLoading
            )

        lottieLoading.setAnimation(
            "Loading_Animation.json"
        )

        lottieLoading.repeatCount =
            android.view.animation.Animation.INFINITE

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {
            tampilkanFilterTahun(
                cardContainer
            )
        }

        loadEkonomiTables(
            cardContainer,
            lottieLoading
        )
    }

    private fun loadEkonomiTables(
        cardContainer: LinearLayout,
        lottieLoading: LottieAnimationView
    ) {

        cardContainer.removeAllViews()

        lottieLoading.visibility =
            View.VISIBLE

        lottieLoading.playAnimation()

        repository.getEkonomiTables(
            domain = domain,
            subject = subject,
            page = 1,
            perPage = 100,
            apiKey = apiKey
        ).enqueue(
            object :
                Callback<EkonomiListResponse> {

                override fun onResponse(
                    call: Call<EkonomiListResponse>,
                    response: Response<EkonomiListResponse>
                ) {

                    lottieLoading.cancelAnimation()

                    lottieLoading.visibility =
                        View.GONE

                    if (!response.isSuccessful) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Gagal mengambil data BPS",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Response kosong",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    if (
                        body.status != "OK"
                    ) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "API BPS mengembalikan error",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val data =
                        body.data

                    if (data == null) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Data tidak ditemukan",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    if (
                        data.size() < 2
                    ) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Struktur data tidak sesuai",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val tableArray =
                        data[1].asJsonArray

                    if (
                        tableArray.size() == 0
                    ) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Tidak ada tabel Neraca Ekonomi",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val gson =
                        Gson()

                    semuaTabel.clear()

                    for (element in tableArray) {

                        try {

                            val table =
                                gson.fromJson(
                                    element,
                                    EkonomiTable::class.java
                                )

                            semuaTabel.add(
                                table
                            )

                            val tahunAwal =
                                table.oldestPeriod
                                    ?.toString()
                                    ?.toIntOrNull()

                            val tahunAkhir =
                                table.latestPeriod
                                    ?.toString()
                                    ?.toIntOrNull()

                            if (
                                tahunAwal != null &&
                                tahunAkhir != null &&
                                tahunTerpilih != null &&
                                tahunTerpilih!! in tahunAwal..tahunAkhir
                            ) {

                                addTableCard(
                                    cardContainer,
                                    table,
                                    tahunTerpilih
                                )
                            }

                        } catch (_: Exception) {
                        }
                    }

                    if (
                        cardContainer.childCount == 0
                    ) {

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Tidak ada tabel untuk tahun 2026",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<EkonomiListResponse>,
                    t: Throwable
                ) {

                    lottieLoading.cancelAnimation()

                    lottieLoading.visibility =
                        View.GONE

                    Toast.makeText(
                        this@EkonomiActivity,
                        "Gagal terhubung ke server BPS",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun tampilkanFilterTahun(
        cardContainer: LinearLayout
    ) {

        if (
            semuaTabel.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Data belum tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val daftarTahun =
            mutableSetOf<Int>()

        for (table in semuaTabel) {

            val tahunAwal =
                table.oldestPeriod
                    ?.toString()
                    ?.toIntOrNull()

            val tahunAkhir =
                table.latestPeriod
                    ?.toString()
                    ?.toIntOrNull()

            if (
                tahunAwal != null &&
                tahunAkhir != null
            ) {

                for (
                tahun in tahunAwal..tahunAkhir
                ) {

                    daftarTahun.add(
                        tahun
                    )
                }
            }
        }

        val tahunList =
            daftarTahun.sortedDescending()

        if (
            tahunList.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Tahun tidak tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pilihan =
            mutableListOf<String>()

        pilihan.add(
            "Semua Tahun"
        )

        tahunList.forEach { tahun ->
            pilihan.add(
                tahun.toString()
            )
        }

        var pilihanSekarang =
            0

        if (
            tahunTerpilih != null
        ) {

            val index =
                tahunList.indexOf(
                    tahunTerpilih
                )

            if (
                index >= 0
            ) {

                pilihanSekarang =
                    index + 1
            }
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Filter Tahun"
            )
            .setSingleChoiceItems(
                pilihan.toTypedArray(),
                pilihanSekarang
            ) { dialog, which ->

                if (
                    which == 0
                ) {

                    tahunTerpilih =
                        null

                    tampilkanSemuaTabel(
                        cardContainer
                    )

                } else {

                    tahunTerpilih =
                        tahunList[
                                which - 1
                        ]

                    tampilkanTabelBerdasarkanTahun(
                        cardContainer,
                        tahunTerpilih!!
                    )
                }

                dialog.dismiss()
            }
            .show()
    }

    private fun tampilkanSemuaTabel(
        cardContainer: LinearLayout
    ) {

        cardContainer.removeAllViews()

        for (table in semuaTabel) {

            addTableCard(
                cardContainer,
                table
            )
        }
    }

    private fun tampilkanTabelBerdasarkanTahun(
        cardContainer: LinearLayout,
        tahun: Int
    ) {

        cardContainer.removeAllViews()

        var jumlah =
            0

        for (table in semuaTabel) {

            val tahunAwal =
                table.oldestPeriod
                    ?.toString()
                    ?.toIntOrNull()

            val tahunAkhir =
                table.latestPeriod
                    ?.toString()
                    ?.toIntOrNull()

            if (
                tahunAwal != null &&
                tahunAkhir != null &&
                tahun in tahunAwal..tahunAkhir
            ) {

                addTableCard(
                    cardContainer,
                    table,
                    tahun
                )

                jumlah++
            }
        }

        if (
            jumlah == 0
        ) {

            Toast.makeText(
                this,
                "Tidak ada tabel untuk tahun $tahun",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addTableCard(
        container: LinearLayout,
        table: EkonomiTable,
        tahunTampilan: Int? = null
    ) {

        val card =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_statistik,
                    container,
                    false
                )

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        tvJudul.text =
            table.title
                ?: "Tanpa judul"

        val tvKategori =
            card.findViewById<TextView>(
                R.id.tvKategori
            )

        tvKategori.text =
            "Neraca Ekonomi"

        val tahunCard =
            tahunTampilan
                ?: table.latestPeriod
                    ?.toString()
                    ?.toIntOrNull()

        val tvTahun =
            card.findViewById<TextView>(
                R.id.tvTahun
            )

        tvTahun.text =
            tahunCard
                ?.toString()
                ?: "-"

        card.setOnClickListener {

            val tahunDetail =
                tahunCard
                    ?: 2025

            activityTracker.trackScreen(
                screen = "EkonomiDetail",
                metadata = mapOf(
                    "table_id" to (table.id ?: ""),
                    "tahun" to tahunDetail,
                    "kategori" to "Neraca Ekonomi"
                )
            )

            val intent =
                Intent(
                    this@EkonomiActivity,
                    EkonomiDetailActivity::class.java
                )

            intent.putExtra(
                EkonomiDetailActivity.EXTRA_ID,
                table.id
            )

            intent.putExtra(
                EkonomiDetailActivity.EXTRA_JUDUL,
                table.title
            )

            intent.putExtra(
                EkonomiDetailActivity.EXTRA_TAHUN,
                tahunDetail
            )

            startActivity(
                intent
            )
        }

        container.addView(
            card
        )
    }
}