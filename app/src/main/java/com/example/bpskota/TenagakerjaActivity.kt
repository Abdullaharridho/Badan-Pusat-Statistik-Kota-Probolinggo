package com.example.bpskota

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
import com.airbnb.lottie.LottieDrawable
import com.example.bpskota.bps.model.TenagaKerjaResponse
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class TenagakerjaActivity : AppCompatActivity() {

    companion object {

        private const val DOMAIN = "3574"
        private const val VARIABLE = 88

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val TAHUN_AWAL = 2010
        private const val TAHUN_AKHIR = 2025

        private const val KODE_TAHUN_AWAL = 110
        private const val KODE_TAHUN_AKHIR = 125
    }

    private val repository = BpsRepository()

    private lateinit var activityTracker: ActivityTracker

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: LinearLayout
    private lateinit var tvFilterPeriode: TextView

    private var tahunTerpilih = 2025

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tenagakerja)

        initView()
        setupLoading()
        setupButton()

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Tenagakerja",
            metadata = mapOf(
                "tahun" to tahunTerpilih
            )
        )

        ambilDataTahun(
            tahun = tahunTerpilih,
            kodeTahun = getKodeTahun(tahunTerpilih)!!
        )
    }

    private fun initView() {

        cardContainer = findViewById(R.id.cardContainer)

        progressLoading = findViewById(R.id.progressLoading)

        btnBack = findViewById(R.id.btnBack)

        btnFilter = findViewById(R.id.btnFilter)

        tvFilterPeriode = findViewById(R.id.tvFilterPeriode)

        tvFilterPeriode.text = tahunTerpilih.toString()
    }

    private fun setupLoading() {

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        progressLoading.repeatCount = LottieDrawable.INFINITE

        progressLoading.visibility = View.GONE
    }

    private fun mulaiLoading() {

        progressLoading.visibility = View.VISIBLE
        progressLoading.playAnimation()
    }

    private fun selesaiLoading() {

        progressLoading.cancelAnimation()
        progressLoading.visibility = View.GONE
    }

    private fun setupButton() {

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {
            tampilkanFilterTahun()
        }
    }

    // =====================================================
    // FILTER TAHUN - DESAIN DAN ANIMASI GENDERACTIVITY
    // =====================================================

    private fun tampilkanFilterTahun() {

        val daftarTahun = (TAHUN_AWAL..TAHUN_AKHIR)
            .reversed()
            .map {
                it.toString()
            }

        // Container utama dialog
        val root =
            LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(14)
                )

                background =
                    GradientDrawable().apply {

                        setColor(Color.WHITE)

                        cornerRadius = dp(24).toFloat()

                        setStroke(
                            dp(2),
                            Color.parseColor("#F97316")
                        )
                    }
            }

        // Judul dialog
        val tvJudul =
            TextView(this).apply {

                text = "Pilih Tahun Data"

                textSize = 19f

                setTextColor(
                    Color.parseColor("#F97316")
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                gravity = Gravity.CENTER

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

                isFillViewport = false

                clipToPadding = false

                setPadding(
                    0,
                    dp(2),
                    0,
                    dp(4)
                )
            }

        val listContainer =
            LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL
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

        // Dialog dibuat sebelum listener item tahun
        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .create()

        // Fungsi memperbarui tampilan pilihan tahun
        fun perbaruiPilihanTahun() {

            listContainer.removeAllViews()

            daftarTahun.forEach { teks ->

                val tahunItem = teks.toInt()

                val terpilih = tahunItem == tahunTerpilih

                val item =
                    TextView(this).apply {

                        text = teks

                        textSize = 14f

                        gravity = Gravity.CENTER_VERTICAL

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

                                cornerRadius = dp(12).toFloat()

                                setColor(
                                    if (terpilih) {
                                        Color.parseColor("#F97316")
                                    } else {
                                        Color.parseColor("#FFF7ED")
                                    }
                                )
                            }

                        setOnClickListener {

                            // Jika tahun sama, cukup tutup dialog
                            if (tahunItem == tahunTerpilih) {

                                dialog.dismiss()

                                return@setOnClickListener
                            }

                            // Ambil kode tahun BPS
                            val kodeTahun = getKodeTahun(tahunItem)

                            if (kodeTahun == null) {

                                Toast.makeText(
                                    this@TenagakerjaActivity,
                                    "Kode tahun $tahunItem tidak tersedia",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@setOnClickListener
                            }

                            // Simpan tahun yang dipilih
                            tahunTerpilih = tahunItem

                            // Perbarui label tahun
                            tvFilterPeriode.text =
                                tahunTerpilih.toString()

                            // Tutup dialog
                            dialog.dismiss()

                            // Langsung ambil data tanpa tombol Tampilkan
                            ambilDataTahun(
                                tahun = tahunItem,
                                kodeTahun = kodeTahun
                            )
                        }
                    }

                val itemParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        bottomMargin = dp(8)
                    }

                listContainer.addView(
                    item,
                    itemParams
                )
            }
        }

        perbaruiPilihanTahun()

        // Footer tombol Tutup
        val footer =
            LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
            }

        val btnTutup =
            TextView(this).apply {

                text = "Tutup"

                textSize = 14f

                gravity = Gravity.CENTER

                setTextColor(
                    Color.parseColor("#9CA3AF")
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                setPadding(
                    dp(18),
                    dp(12),
                    dp(18),
                    dp(12)
                )

                setOnClickListener {
                    dialog.dismiss()
                }
            }

        footer.addView(
            btnTutup,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            footer,
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

            params.gravity = Gravity.CENTER

            params.x = 0

            params.y = 0

            params.width = width

            params.height = height

            // Matikan animasi bawaan Window
            params.windowAnimations = 0

            window.attributes = params

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
                decorView.alpha = 0f

                decorView.scaleX = 0.85f

                decorView.scaleY = 0.85f

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
    // KODE TAHUN BPS
    // =====================================================

    private fun getKodeTahun(tahun: Int): Int? {

        if (
            tahun < TAHUN_AWAL ||
            tahun > TAHUN_AKHIR
        ) {
            return null
        }

        val kode = tahun - 1900

        if (
            kode < KODE_TAHUN_AWAL ||
            kode > KODE_TAHUN_AKHIR
        ) {
            return null
        }

        return kode
    }

    // =====================================================
    // AMBIL DATA TENAGA KERJA
    // =====================================================

    private fun ambilDataTahun(
        tahun: Int,
        kodeTahun: Int
    ) {

        mulaiLoading()

        cardContainer.removeAllViews()

        repository.getTenagaKerja(
            domain = DOMAIN,
            variable = VARIABLE,
            tahun = kodeTahun,
            apiKey = API_KEY
        ).enqueue(
            object : Callback<TenagaKerjaResponse> {

                override fun onResponse(
                    call: Call<TenagaKerjaResponse>,
                    response: Response<TenagaKerjaResponse>
                ) {

                    if (!response.isSuccessful) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Gagal mengambil data tahun $tahun",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    val body = response.body()

                    if (body == null) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Response tahun $tahun kosong",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    if (
                        body.status?.uppercase(Locale.ROOT) != "OK"
                    ) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Status API: ${body.status}",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    if (
                        body.dataAvailability?.lowercase(Locale.ROOT) != "available"
                    ) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Data tahun $tahun tidak tersedia",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    tampilkanData(
                        body = body,
                        tahun = tahun,
                        kodeTahun = kodeTahun
                    )
                }

                override fun onFailure(
                    call: Call<TenagaKerjaResponse>,
                    t: Throwable
                ) {

                    selesaiLoading()

                    Toast.makeText(
                        this@TenagakerjaActivity,
                        "Gagal mengambil data tahun $tahun: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // =====================================================
    // TAMPILKAN DATA
    // =====================================================

    private fun tampilkanData(
        body: TenagaKerjaResponse,
        tahun: Int,
        kodeTahun: Int
    ) {

        val dataContent = body.dataContent ?: emptyMap()

        val prefix = "$DOMAIN$VARIABLE"

        var nilai: Double? = null

        val entryUtama = dataContent.entries
            .firstOrNull { entry ->

                entry.key.startsWith(prefix) &&
                        entry.key.contains(kodeTahun.toString())
            }

        if (entryUtama != null) {
            nilai = entryUtama.value
        }

        if (nilai == null) {

            val entryFallback = dataContent.entries
                .firstOrNull { entry ->

                    entry.key.endsWith(kodeTahun.toString())
                }

            if (entryFallback != null) {
                nilai = entryFallback.value
            }
        }

        if (
            nilai == null &&
            dataContent.size == 1
        ) {

            val entry = dataContent.entries.first()

            nilai = entry.value
        }

        if (nilai == null) {

            selesaiLoading()

            Toast.makeText(
                this,
                "Nilai tahun $tahun tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val variable = body.variable?.firstOrNull()

        val judul = variable?.label
            ?: "Tingkat Pengangguran Terbuka (TPT)"

        val satuan = variable?.unit
            ?: "Persen"

        val decimal = variable?.decimal
            ?: 2

        val nilaiFormat = String.format(
            Locale.US,
            "%.${decimal}f",
            nilai
        )

        val card = LayoutInflater
            .from(this)
            .inflate(
                R.layout.item_statistik,
                cardContainer,
                false
            )

        val tvTahun = card.findViewById<TextView>(
            R.id.tvTahun
        )

        val tvKategori = card.findViewById<TextView>(
            R.id.tvKategori
        )

        val tvJudul = card.findViewById<TextView>(
            R.id.tvJudul
        )

        val tvNilai = card.findViewById<TextView>(
            R.id.tvNilai
        )

        tvTahun.text = tahun.toString()

        tvKategori.text = "Tenaga Kerja"

        tvJudul.text = judul

        tvNilai.text = if (satuan.isNotBlank()) {
            "$nilaiFormat $satuan"
        } else {
            nilaiFormat
        }

        card.setOnClickListener {

            Toast.makeText(
                this,
                "$nilaiFormat $satuan\nTahun $tahun",
                Toast.LENGTH_LONG
            ).show()
        }

        cardContainer.addView(card)

        selesaiLoading()
    }

    // =====================================================
    // KONVERSI DP
    // =====================================================

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroy() {

        if (::progressLoading.isInitialized) {

            progressLoading.cancelAnimation()

            progressLoading.visibility = View.GONE
        }

        super.onDestroy()
    }
}