package com.example.bpskota

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView

class TempattinggalActivity : AppCompatActivity() {

    companion object {

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

    // FILTER TAHUN
    private lateinit var btnFilter: LinearLayout
    private lateinit var tvFilterTahun: TextView

    private lateinit var tvHeaderTitle: TextView

    private lateinit var activityTracker: ActivityTracker

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

        activityTracker =
            ActivityTracker(
                this,
                BpskpRetrofitClient.api
            )

        activityTracker.trackScreen(
            screen = "Tempattinggal"
        )

        updateHeader(
            tahunTerpilih
        )

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

        tvFilterTahun =
            findViewById(
                R.id.tvFilterTahun
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

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        // Label awal filter
        tvFilterTahun.text =
            tahunTerpilih.toString()

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

        progressLoading.visibility =
            View.VISIBLE

        progressLoading.playAnimation()

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

                        cekSemuaRequestSelesai()
                    }

                    override fun onFailure(
                        call: Call<TempatTinggalResponse>,
                        t: Throwable
                    ) {

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
                // Abaikan variabel yang gagal diparsing
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

        if (semuaVariable.isEmpty()) {
            Toast.makeText(
                this,
                "Data belum selesai dimuat",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Konversi dp tanpa membutuhkan fungsi dp() tambahan.
        fun dp(value: Int): Int {
            return (value * resources.displayMetrics.density).toInt()
        }

        val tahunList = (TAHUN_MINIMUM..TAHUN_MULAI)
            .toList()
            .sortedDescending()

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
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(18))
        }

        root.addView(
            tvJudul,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Daftar tahun yang dapat di-scroll
        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
        }

        val daftarTahun = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .create()

        for (tahun in tahunList) {

            val terpilih = tahun == tahunTerpilih

            val itemTahun = TextView(this).apply {
                text = tahun.toString()
                textSize = 15f
                gravity = Gravity.CENTER
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
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

                    if (tahun == tahunTerpilih) {
                        dialog.dismiss()
                        return@setOnClickListener
                    }

                    dialog.dismiss()

                    pilihTahun(tahun)
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                bottomMargin = dp(8)
            }

            daftarTahun.addView(
                itemTahun,
                params
            )
        }

        scrollView.addView(
            daftarTahun,
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
            setTextColor(Color.parseColor("#9CA3AF"))
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

            // Animasi dialog
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

        // Perbarui judul halaman
        tvHeaderTitle.text =
            "$SUBCSA_NAME ($tahun)"

        // Perbarui label tombol filter
        tvFilterTahun.text =
            tahun.toString()
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