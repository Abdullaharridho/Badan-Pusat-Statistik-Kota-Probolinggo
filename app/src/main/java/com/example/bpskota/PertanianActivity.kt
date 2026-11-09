package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.bpskota.bps.model.SimdasiResponse
import com.example.bpskota.bps.model.SimdasiTable
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView

class PertanianActivity : AppCompatActivity() {

    private lateinit var cardContainer: LinearLayout
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var btnBack: ImageView

    // FILTER TAHUN
    private lateinit var btnFilter: LinearLayout
    private lateinit var tvFilterTahun: TextView

    private lateinit var tvHeaderTitle: TextView

    private lateinit var adapter: PertanianTableAdapter
    private lateinit var activityTracker: ActivityTracker

    private val repository =
        BpsRepository()

    private val semuaPertanian =
        mutableListOf<PertanianTableAdapter.Item>()

    private var tahunTerpilih =
        2025

    companion object {

        private const val WILAYAH =
            "3574000"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val BAB_PERTANIAN =
            "Pertanian, Kehutanan, Peternakan, dan Perikanan"

        private const val TAHUN_MULAI =
            2026

        private const val TAHUN_MINIMUM =
            2020
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_pertanian
        )

        initView()

        setupLoading()

        setupAdapter()

        activityTracker =
            ActivityTracker(
                this,
                BpskpRetrofitClient.api
            )

        activityTracker.trackScreen(
            screen = "Pertanian",
            metadata = mapOf(
                "tahun" to tahunTerpilih
            )
        )

        setupButton()

        tahunTerpilih = 2025

        updateHeaderTahun()

        loadTahun(
            tahunTerpilih
        )
    }

    private fun initView() {

        cardContainer =
            findViewById(
                R.id.cardContainer
            )

        loadingAnimation =
            findViewById(
                R.id.loadingAnimation
            )

        btnBack =
            findViewById(
                R.id.btnBack
            )

        btnFilter =
            findViewById(
                R.id.btnFilter
            )

        tvFilterTahun =
            findViewById(
                R.id.tvFilterTahun
            )

        tvHeaderTitle =
            findViewById(
                R.id.tvHeaderTitle
            )
    }

    private fun updateHeaderTahun() {

        tvFilterTahun.text =
            tahunTerpilih.toString()

        tvHeaderTitle.text =
            "$BAB_PERTANIAN ($tahunTerpilih)"
    }

    private fun setupLoading() {

        loadingAnimation.setAnimation(
            "Loading_Animation.json"
        )

        loadingAnimation.repeatCount =
            LottieDrawable.INFINITE

        loadingAnimation.visibility =
            View.GONE
    }

    private fun showLoading() {

        loadingAnimation.visibility =
            View.VISIBLE

        loadingAnimation.playAnimation()
    }

    private fun hideLoading() {

        loadingAnimation.cancelAnimation()

        loadingAnimation.visibility =
            View.GONE
    }

    private fun setupAdapter() {

        adapter =
            PertanianTableAdapter(

                container = cardContainer,

                kategori =
                "Pertanian, Kehutanan, Peternakan, dan Perikanan",

                onItemClick = { table, tahun ->

                    onTableClicked(
                        table,
                        tahun
                    )
                }
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

        // Konversi dp lokal agar tidak membutuhkan fungsi dp() tambahan.
        fun dp(value: Int): Int {
            return (value * resources.displayMetrics.density).toInt()
        }

        val daftarTahun = (TAHUN_MULAI downTo TAHUN_MINIMUM).toList()

        // Container utama dialog
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(16)
            )

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = dp(24).toFloat()
                setStroke(
                    dp(2),
                    Color.parseColor("#F97316")
                )
            }
        }

        // Judul dialog
        val tvJudul = TextView(this).apply {
            text = "Pilih Tahun Data"
            textSize = 18f
            setTextColor(Color.parseColor("#F97316"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(4),
                0,
                dp(18)
            )
        }

        root.addView(
            tvJudul,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Scroll daftar tahun
        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
        }

        val daftarTahunContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .create()

        daftarTahun.forEach { tahun ->

            val terpilih = tahun == tahunTerpilih

            val itemTahun = TextView(this).apply {

                text = tahun.toString()
                textSize = 15f
                gravity = Gravity.CENTER

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (terpilih) {
                        Color.WHITE
                    } else {
                        Color.parseColor("#C2410C")
                    }
                )

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(12).toFloat()

                    setColor(
                        if (terpilih) {
                            Color.parseColor("#F97316")
                        } else {
                            Color.parseColor("#FFF7ED")
                        }
                    )
                }

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )

                setOnClickListener {

                    // Jika memilih tahun yang sedang aktif,
                    // cukup tutup dialog tanpa memuat ulang.
                    if (tahun == tahunTerpilih) {
                        dialog.dismiss()
                        return@setOnClickListener
                    }

                    dialog.dismiss()

                    tahunTerpilih = tahun

                    updateHeaderTahun()

                    loadTahun(tahunTerpilih)
                }
            }

            val itemParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                bottomMargin = dp(8)
            }

            daftarTahunContainer.addView(
                itemTahun,
                itemParams
            )
        }

        scrollView.addView(
            daftarTahunContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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

        // Tombol tutup
        val tvTutup = TextView(this).apply {
            text = "Tutup"
            textSize = 14f
            gravity = Gravity.CENTER

            setTextColor(
                Color.parseColor("#9CA3AF")
            )

            setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(4)
            )

            setOnClickListener {
                dialog.dismiss()
            }
        }

        root.addView(
            tvTutup,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Tampilkan dialog
        dialog.show()

        dialog.window?.let { window ->

            val displayMetrics = resources.displayMetrics

            val width = (displayMetrics.widthPixels * 0.70f).toInt()
            val height = (displayMetrics.heightPixels * 0.50f).toInt()

            val params = window.attributes

            params.gravity = Gravity.CENTER
            params.x = 0
            params.y = 0
            params.width = width
            params.height = height
            params.windowAnimations = 0

            window.attributes = params

            window.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            // Animasi zoom dan fade
            window.decorView.post {

                val decorView = window.decorView

                decorView.pivotX = decorView.width / 2f
                decorView.pivotY = decorView.height / 2f

                decorView.alpha = 0f
                decorView.scaleX = 0.85f
                decorView.scaleY = 0.85f

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

    private fun loadTahun(
        tahun: Int
    ) {

        showLoading()

        semuaPertanian.clear()

        adapter.tampilkan(
            semuaPertanian
        )

        repository
            .getSimdasiTables(

                tahun = tahun,

                wilayah = WILAYAH,

                apiKey = API_KEY

            )
            .enqueue(

                object :
                    Callback<SimdasiResponse> {

                    override fun onResponse(

                        call:
                        Call<SimdasiResponse>,

                        response:
                        Response<SimdasiResponse>

                    ) {

                        if (
                            !response.isSuccessful
                        ) {

                            hideLoading()

                            Toast.makeText(
                                this@PertanianActivity,
                                "Gagal mengambil data tahun $tahun",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        val body =
                            response.body()

                        if (
                            body == null
                        ) {

                            hideLoading()

                            Toast.makeText(
                                this@PertanianActivity,
                                "Response kosong",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {

                            hideLoading()

                            Toast.makeText(
                                this@PertanianActivity,
                                "Data tahun $tahun tidak tersedia",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        val allTables =
                            body.data
                                ?.flatMap { page ->

                                    page.tables
                                        ?: emptyList()
                                }
                                ?: emptyList()

                        val pertanianTables =
                            allTables.filter { table ->

                                table.bab
                                    ?.trim()
                                    ?.equals(
                                        BAB_PERTANIAN,
                                        ignoreCase = true
                                    ) == true
                            }

                        val itemsTahunIni =
                            pertanianTables.map { table ->

                                PertanianTableAdapter.Item(

                                    tahun = tahun,

                                    table = table
                                )
                            }

                        semuaPertanian.addAll(
                            itemsTahunIni
                        )

                        adapter.tampilkan(
                            semuaPertanian
                        )

                        hideLoading()

                        if (
                            itemsTahunIni.isEmpty()
                        ) {

                            Toast.makeText(
                                this@PertanianActivity,
                                "Data Pertanian tahun $tahun tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<SimdasiResponse>,

                        t: Throwable

                    ) {

                        hideLoading()

                        Toast.makeText(
                            this@PertanianActivity,
                            "Gagal mengambil data: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun onTableClicked(

        table: SimdasiTable,

        tahun: Int

    ) {

        val idTabel =
            table.idTabel

        if (
            idTabel.isNullOrEmpty()
        ) {

            Toast.makeText(
                this,
                "ID tabel tidak tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                this,
                StatistikDetailActivity::class.java
            )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_ID_TABEL,
            idTabel
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_TAHUN,
            tahun
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_JUDUL,
            table.judul
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_KODE,
            table.kodeTabel
        )

        startActivity(intent)
    }

    override fun onDestroy() {

        if (
            ::loadingAnimation.isInitialized
        ) {

            loadingAnimation.cancelAnimation()

            loadingAnimation.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}