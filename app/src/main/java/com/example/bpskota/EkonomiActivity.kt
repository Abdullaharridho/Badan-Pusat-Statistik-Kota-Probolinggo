package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    // Default ketika Activity pertama dibuka
    private var tahunTerpilih: Int? = 2026

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_ekonomi
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

        // ============================================================
        // LOAD ANIMASI LOTTIE
        // ============================================================

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

    // ============================================================
    // LOAD LIST EKONOMI
    // ============================================================

    private fun loadEkonomiTables(
        cardContainer: LinearLayout,
        lottieLoading: LottieAnimationView
    ) {

        cardContainer.removeAllViews()

        // ============================================================
        // MULAI LOADING
        // ============================================================

        lottieLoading.visibility =
            View.VISIBLE

        lottieLoading.playAnimation()

        Log.d(
            "EKONOMI",
            "Mulai mengambil list Neraca Ekonomi"
        )

        Log.d(
            "EKONOMI",
            "DEFAULT TAHUN = $tahunTerpilih"
        )

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

                    // ====================================================
                    // REQUEST SELESAI
                    // ====================================================

                    lottieLoading.cancelAnimation()

                    lottieLoading.visibility =
                        View.GONE

                    if (!response.isSuccessful) {

                        Log.e(
                            "EKONOMI",
                            "HTTP ERROR = ${response.code()}"
                        )

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

                        Log.e(
                            "EKONOMI",
                            "Response body kosong"
                        )

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Response kosong",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    Log.d(
                        "EKONOMI",
                        "STATUS = ${body.status}"
                    )

                    if (
                        body.status != "OK"
                    ) {

                        Log.e(
                            "EKONOMI",
                            "Status API bukan OK"
                        )

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

                        Log.e(
                            "EKONOMI",
                            "Data response null"
                        )

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

                        Log.e(
                            "EKONOMI",
                            "Struktur data BPS tidak sesuai"
                        )

                        Toast.makeText(
                            this@EkonomiActivity,
                            "Struktur data tidak sesuai",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val tableArray =
                        data[1].asJsonArray

                    Log.d(
                        "EKONOMI",
                        "JUMLAH TABEL = ${tableArray.size()}"
                    )

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

                    // ========================================================
                    // BACA SEMUA TABEL
                    // ========================================================

                    for (
                    element in tableArray
                    ) {

                        try {

                            val table =
                                gson.fromJson(
                                    element,
                                    EkonomiTable::class.java
                                )

                            semuaTabel.add(
                                table
                            )

                            Log.d(
                                "EKONOMI",
                                "ID = ${table.id}"
                            )

                            Log.d(
                                "EKONOMI",
                                "TITLE = ${table.title}"
                            )

                            Log.d(
                                "EKONOMI",
                                "OLDEST = ${table.oldestPeriod}"
                            )

                            Log.d(
                                "EKONOMI",
                                "LATEST = ${table.latestPeriod}"
                            )

                            // ====================================================
                            // TAMPILKAN HANYA YANG TERSEDIA DI TAHUN 2026
                            // ====================================================

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

                                Log.d(
                                    "EKONOMI",
                                    "TAMPIL 2026 = ${table.title}"
                                )
                            }

                        } catch (e: Exception) {

                            Log.e(
                                "EKONOMI",
                                "Gagal membaca tabel",
                                e
                            )
                        }
                    }

                    Log.d(
                        "EKONOMI",
                        "Total semua tabel = ${semuaTabel.size}"
                    )

                    Log.d(
                        "EKONOMI",
                        "Tabel yang ditampilkan tahun 2026 = ${cardContainer.childCount}"
                    )

                    // ====================================================
                    // JIKA TIDAK ADA DATA 2026
                    // ====================================================

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

                    // ====================================================
                    // REQUEST GAGAL
                    // ====================================================

                    lottieLoading.cancelAnimation()

                    lottieLoading.visibility =
                        View.GONE

                    Log.e(
                        "EKONOMI",
                        "REQUEST ERROR",
                        t
                    )

                    Toast.makeText(
                        this@EkonomiActivity,
                        "Gagal terhubung ke server BPS",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // ============================================================
    // FILTER TAHUN
    // ============================================================

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

        // ========================================================
        // AMBIL SEMUA TAHUN YANG TERSEDIA
        // ========================================================

        for (
        table in semuaTabel
        ) {

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

        // ========================================================
        // TENTUKAN PILIHAN SAAT INI
        // ========================================================

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

                // ====================================================
                // SEMUA TAHUN
                // ====================================================

                if (
                    which == 0
                ) {

                    tahunTerpilih =
                        null

                    tampilkanSemuaTabel(
                        cardContainer
                    )

                    Log.d(
                        "EKONOMI",
                        "Filter = Semua Tahun"
                    )

                } else {

                    // ====================================================
                    // TAHUN TERTENTU
                    // ====================================================

                    tahunTerpilih =
                        tahunList[
                                which - 1
                        ]

                    Log.d(
                        "EKONOMI",
                        "Filter tahun = $tahunTerpilih"
                    )

                    tampilkanTabelBerdasarkanTahun(
                        cardContainer,
                        tahunTerpilih!!
                    )
                }

                dialog.dismiss()
            }
            .show()
    }

    // ============================================================
    // TAMPILKAN SEMUA
    // ============================================================

    private fun tampilkanSemuaTabel(
        cardContainer: LinearLayout
    ) {

        cardContainer.removeAllViews()

        for (
        table in semuaTabel
        ) {

            addTableCard(
                cardContainer,
                table
            )
        }

        Log.d(
            "EKONOMI",
            "Menampilkan semua tabel = ${semuaTabel.size}"
        )
    }

    // ============================================================
    // TAMPILKAN BERDASARKAN TAHUN
    // ============================================================

    private fun tampilkanTabelBerdasarkanTahun(
        cardContainer: LinearLayout,
        tahun: Int
    ) {

        cardContainer.removeAllViews()

        var jumlah =
            0

        for (
        table in semuaTabel
        ) {

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

        Log.d(
            "EKONOMI",
            "Tahun filter = $tahun"
        )

        Log.d(
            "EKONOMI",
            "Jumlah tabel = $jumlah"
        )

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

    // ============================================================
    // CARD
    // ============================================================

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

        // ========================================================
        // JUDUL
        // ========================================================

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        tvJudul.text =
            table.title
                ?: "Tanpa judul"

        // ========================================================
        // KATEGORI
        // ========================================================

        val tvKategori =
            card.findViewById<TextView>(
                R.id.tvKategori
            )

        tvKategori.text =
            "Neraca Ekonomi"

        // ========================================================
        // TAHUN
        // ========================================================

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

        // ========================================================
        // KLIK CARD
        // ========================================================

        card.setOnClickListener {

            // Tahun yang ditampilkan pada card
            // HARUS menjadi tahun detail
            val tahunDetail =
                tahunCard
                    ?: 2025

            Log.d(
                "EKONOMI",
                "================================"
            )

            Log.d(
                "EKONOMI",
                "TABEL DIKLIK"
            )

            Log.d(
                "EKONOMI",
                "ID = ${table.id}"
            )

            Log.d(
                "EKONOMI",
                "JUDUL = ${table.title}"
            )

            Log.d(
                "EKONOMI",
                "TAHUN CARD = $tahunDetail"
            )

            Log.d(
                "EKONOMI",
                "================================"
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

            // ====================================================
            // INI YANG PENTING
            // ====================================================

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