
package com.example.bpskota

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var tvFilterPeriode: TextView

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
            findViewById<LinearLayout>(
                R.id.btnFilter
            )

        tvFilterPeriode =
            findViewById(R.id.tvFilterPeriode)

        tvFilterPeriode.text =
            tahunTerpilih?.toString() ?: "Semua"

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

    // =====================================================
    // FILTER TAHUN - DESAIN DAN ANIMASI SEPERTI GENDERACTIVITY
    // =====================================================

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

        // Container utama dialog
        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(14)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.WHITE
                        )

                        cornerRadius =
                            dp(24).toFloat()

                        setStroke(
                            dp(2),
                            Color.parseColor("#F97316")
                        )
                    }
            }

        // Judul dialog
        val tvJudul =
            TextView(this).apply {

                text =
                    "Pilih Tahun Data"

                textSize =
                    19f

                setTextColor(
                    Color.parseColor("#F97316")
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(16)
                )
            }

        root.addView(
            tvJudul,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // ScrollView daftar tahun
        val scrollView =
            ScrollView(this).apply {

                isFillViewport =
                    false

                clipToPadding =
                    false

                setPadding(
                    0,
                    dp(2),
                    0,
                    dp(4)
                )
            }

        val listContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        scrollView.addView(
            listContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // Daftar pilihan tahun
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

        val dialog =
            AlertDialog.Builder(this)
                .setView(root)
                .create()

        // Fungsi untuk memperbarui tampilan pilihan
        fun perbaruiPilihanTahun(
            selectedIndex: Int
        ) {

            listContainer.removeAllViews()

            pilihan.forEachIndexed { index, teks ->

                val terpilih =
                    index == selectedIndex

                val item =
                    TextView(this).apply {

                        text =
                            teks

                        textSize =
                            14f

                        gravity =
                            Gravity.CENTER_VERTICAL

                        setPadding(
                            dp(16),
                            dp(13),
                            dp(16),
                            dp(13)
                        )

                        setTypeface(
                            null,
                            if (terpilih) {
                                android.graphics.Typeface.BOLD
                            } else {
                                android.graphics.Typeface.NORMAL
                            }
                        )

                        setTextColor(
                            if (terpilih) {
                                Color.WHITE
                            } else {
                                Color.parseColor("#C2410C")
                            }
                        )

                        background =
                            GradientDrawable().apply {

                                cornerRadius =
                                    dp(12).toFloat()

                                setColor(
                                    if (terpilih) {
                                        Color.parseColor("#F97316")
                                    } else {
                                        Color.parseColor("#FFF7ED")
                                    }
                                )
                            }

                        setOnClickListener {

                            if (index == 0) {

                                tahunTerpilih =
                                    null

                                tvFilterPeriode.text =
                                    "Semua"

                                tampilkanSemuaTabel(
                                    cardContainer
                                )

                            } else {

                                tahunTerpilih =
                                    tahunList[index - 1]

                                tvFilterPeriode.text =
                                    tahunTerpilih.toString()

                                tampilkanTabelBerdasarkanTahun(
                                    cardContainer,
                                    tahunTerpilih!!
                                )
                            }

                            dialog.dismiss()
                        }
                    }

                val itemParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        bottomMargin =
                            dp(8)
                    }

                listContainer.addView(
                    item,
                    itemParams
                )
            }
        }

        // Tampilkan pilihan sesuai tahun aktif
        perbaruiPilihanTahun(
            pilihanSekarang
        )

        // Tombol Tutup
        val btnTutup =
            TextView(this).apply {

                text =
                    "Tutup"

                textSize =
                    14f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.parseColor("#9CA3AF")
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(4)
                )

                setOnClickListener {
                    dialog.dismiss()
                }
            }

        root.addView(
            btnTutup,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Tampilkan dialog terlebih dahulu
        dialog.show()

        // ATUR POSISI DAN ANIMASI DIALOG
        dialog.window?.let { window ->

            val displayMetrics =
                resources.displayMetrics

            val width =
                (displayMetrics.widthPixels * 0.70f).toInt()

            val height =
                (displayMetrics.heightPixels * 0.50f).toInt()

            // Atur posisi dan ukuran Window
            val params =
                window.attributes

            params.gravity =
                Gravity.CENTER

            params.x =
                0

            params.y =
                0

            params.width =
                width

            params.height =
                height

            // Matikan animasi bawaan Window
            params.windowAnimations =
                0

            window.attributes =
                params

            window.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            // Tunggu sampai Window selesai ditampilkan
            window.decorView.post {

                val decorView =
                    window.decorView

                // Titik pusat animasi berada di tengah Window
                decorView.pivotX =
                    decorView.width / 2f

                decorView.pivotY =
                    decorView.height / 2f

                // Kondisi awal
                decorView.alpha =
                    0f

                decorView.scaleX =
                    0.85f

                decorView.scaleY =
                    0.85f

                // Animasi muncul dari tengah
                decorView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(
                        DecelerateInterpolator(2f)
                    )
                    .start()
            }
        }
    }

    // =====================================================
    // MENAMPILKAN SEMUA TABEL
    // =====================================================

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

    // =====================================================
    // MENAMPILKAN TABEL BERDASARKAN TAHUN
    // =====================================================

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

    // =====================================================
    // CARD TABEL EKONOMI
    // =====================================================

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

    // =====================================================
    // KONVERSI DP
    // =====================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}
