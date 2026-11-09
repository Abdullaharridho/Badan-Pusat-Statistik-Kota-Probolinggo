package com.example.bpskota

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
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
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
        private val VARIABLE_LIST = listOf(134, 49, 47, 121)
        private val TAHUN_LIST = (2010..2026).toList()
    }

    private val repository = BpsRepository()
    private lateinit var activityTracker: ActivityTracker

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: LinearLayout

    private var tahunDipilih = 2025
    private var jumlahRequestSelesai = 0
    private lateinit var tvFilterTahun: TextView

    private val dataGender = mutableListOf<GenderData>()

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
        setContentView(R.layout.activity_gender)

        activityTracker = ActivityTracker(this, BpskpRetrofitClient.api)
        activityTracker.trackScreen(
            screen = "Gender",
            metadata = mapOf("tahun" to tahunDipilih)
        )

        initView()
        setupButton()
        loadSemuaGender()
    }

    private fun initView() {
        cardContainer = findViewById(R.id.cardContainer)

        progressLoading = findViewById(R.id.progressLoading)
        progressLoading.setAnimation("Loading_Animation.json")
        progressLoading.repeatCount = -1

        btnBack = findViewById(R.id.btnBack)
        btnFilter = findViewById(R.id.btnFilter)

        tvFilterTahun = findViewById(R.id.tvFilterTahun)
        tvFilterTahun.text = tahunDipilih.toString()
    }

    private fun setupButton() {
        btnBack.setOnClickListener { finish() }
        btnFilter.setOnClickListener { tampilkanFilterTahun() }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun tampilkanFilterTahun() {
        val tahunArray = TAHUN_LIST.reversed().map { it.toString() }

        val rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                setStroke(
                    dpToPx(2),
                    Color.parseColor("#F97316")
                )
                cornerRadius = dpToPx(24).toFloat()
            }

            setPadding(
                0,
                dpToPx(20),
                0,
                dpToPx(16)
            )
        }

        val titleText = TextView(this).apply {
            text = "Pilih Tahun Data"
            setTextColor(Color.parseColor("#F97316"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER

            setPadding(
                0,
                0,
                0,
                dpToPx(16)
            )
        }

        rootContainer.addView(titleText)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            isScrollbarFadingEnabled = false
        }

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dpToPx(20),
                0,
                dpToPx(20),
                dpToPx(8)
            )
        }

        var dialogInstance: AlertDialog? = null

        tahunArray.forEach { tahunStr ->

            val isSelected = tahunStr.toInt() == tahunDipilih

            val itemText = TextView(this).apply {
                text = tahunStr
                textSize = 14f
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dpToPx(12),
                    0,
                    dpToPx(12)
                )

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                params.setMargins(
                    0,
                    0,
                    0,
                    dpToPx(8)
                )

                layoutParams = params

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(12).toFloat()

                    if (isSelected) {
                        setColor(Color.parseColor("#F97316"))
                    } else {
                        setColor(Color.parseColor("#FFF7ED"))
                    }
                }

                setTextColor(
                    if (isSelected) {
                        Color.WHITE
                    } else {
                        Color.parseColor("#C2410C")
                    }
                )

                setTypeface(
                    null,
                    if (isSelected) {
                        Typeface.BOLD
                    } else {
                        Typeface.NORMAL
                    }
                )

                setOnClickListener {
                    dialogInstance?.dismiss()

                    val tahunBaru = tahunStr.toInt()

                    if (tahunBaru != tahunDipilih) {
                        tahunDipilih = tahunBaru
                        tvFilterTahun.text = tahunDipilih.toString()

                        activityTracker.trackScreen(
                            screen = "Gender",
                            metadata = mapOf("tahun" to tahunDipilih)
                        )

                        loadSemuaGender()
                    }
                }
            }

            listContainer.addView(itemText)
        }

        scrollView.addView(listContainer)
        rootContainer.addView(scrollView)

        val btnClose = TextView(this).apply {
            text = "Tutup"
            setTextColor(Color.parseColor("#9CA3AF"))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER

            setPadding(
                0,
                dpToPx(12),
                0,
                0
            )

            setOnClickListener {
                dialogInstance?.dismiss()
            }
        }

        rootContainer.addView(btnClose)

        val dialog = AlertDialog.Builder(this)
            .setView(rootContainer)
            .create()

        dialogInstance = dialog

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        // TAMPILKAN DIALOG
        dialog.show()

        // ATUR POSISI DAN ANIMASI DIALOG
        dialog.window?.let { window ->

            val displayMetrics = resources.displayMetrics

            val width = (displayMetrics.widthPixels * 0.70f).toInt()
            val height = (displayMetrics.heightPixels * 0.50f).toInt()

            // Atur posisi dan ukuran Window
            val params = window.attributes

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

                val decorView = window.decorView

                // Titik pusat animasi berada di tengah Window
                decorView.pivotX = decorView.width / 2f
                decorView.pivotY = decorView.height / 2f

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
                    .setInterpolator(DecelerateInterpolator(2f))
                    .start()
            }
        }
    }

    private fun getKodeTahun(tahun: Int): Int {
        return tahun - 1900
    }

    private fun loadSemuaGender() {
        progressLoading.visibility = View.VISIBLE
        progressLoading.playAnimation()

        cardContainer.removeAllViews()
        dataGender.clear()
        jumlahRequestSelesai = 0

        val kodeTahun = getKodeTahun(tahunDipilih)

        VARIABLE_LIST.forEach { variableId ->
            loadGenderVariable(variableId, kodeTahun)
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
            object : Callback<TenagaKerjaResponse> {

                override fun onResponse(
                    call: Call<TenagaKerjaResponse>,
                    response: Response<TenagaKerjaResponse>
                ) {
                    if (!response.isSuccessful) {
                        requestSelesai()
                        return
                    }

                    val body = response.body()

                    if (
                        body == null ||
                        body.status?.uppercase(Locale.ROOT) != "OK" ||
                        body.dataAvailability?.lowercase(Locale.ROOT) != "available"
                    ) {
                        requestSelesai()
                        return
                    }

                    prosesDataGender(variableId, body)
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
        val dataContent = body.dataContent ?: emptyMap()

        if (dataContent.isEmpty()) return

        val tahunApi = body.tahun?.firstOrNull()
        val tahunLabel = tahunApi?.label ?: tahunDipilih.toString()

        val variable = body.variable?.firstOrNull()
        val judul = variable?.label ?: getJudulVariable(variableId)
        val satuan = variable?.unit ?: ""
        val decimal = variable?.decimal ?: 2

        val nilai = dataContent.values.firstOrNull() ?: return

        val nilaiFormat = String.format(
            Locale.US,
            "%.${decimal}f",
            nilai
        )

        dataGender.add(
            GenderData(
                variableId,
                tahunLabel,
                "Gender",
                judul,
                nilaiFormat,
                satuan
            )
        )
    }

    private fun getJudulVariable(variableId: Int): String {
        return when (variableId) {
            134 -> "Indeks Ketimpangan Gender (IKG)"
            49 -> "Indeks Pembangunan Gender (IPG) Kota Probolinggo"
            47 -> "Indeks Pemberdayaan Gender (IDG) Kota Probolinggo"
            121 -> "Indeks Pembangunan Gender (menggunakan UHH hasil SP2020 LF)"
            else -> "Statistik Gender"
        }
    }

    private fun requestSelesai() {
        jumlahRequestSelesai++

        if (jumlahRequestSelesai >= VARIABLE_LIST.size) {
            runOnUiThread {
                tampilkanSemuaData()
            }
        }
    }

    private fun tampilkanSemuaData() {
        cardContainer.removeAllViews()

        val dataUrut = VARIABLE_LIST.mapNotNull { id ->
            dataGender.firstOrNull { it.variableId == id }
        }

        dataUrut.forEach { data ->

            val card = LayoutInflater.from(this)
                .inflate(
                    R.layout.item_statistik,
                    cardContainer,
                    false
                )

            val tvTahun = card.findViewById<TextView>(R.id.tvTahun)
            val tvKategori = card.findViewById<TextView>(R.id.tvKategori)
            val tvJudul = card.findViewById<TextView>(R.id.tvJudul)
            val tvNilai = card.findViewById<TextView>(R.id.tvNilai)

            tvTahun.text = data.tahun
            tvKategori.text = data.kategori
            tvJudul.text = data.judul

            tvNilai.text = if (data.satuan.isNotBlank()) {
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

            cardContainer.addView(card)
        }

        progressLoading.visibility = View.GONE

        if (dataUrut.isEmpty()) {
            Toast.makeText(
                this,
                "Tidak ada data Gender untuk tahun $tahunDipilih",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        if (::progressLoading.isInitialized) {
            progressLoading.cancelAnimation()
            progressLoading.visibility = View.GONE
        }

        super.onDestroy()
    }
}