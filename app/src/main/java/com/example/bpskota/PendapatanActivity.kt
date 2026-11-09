
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
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
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

class PendapatanActivity : AppCompatActivity() {

    companion object {
        private const val DOMAIN = "3574"
        private const val SUBCSA_ID = 523

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val TAHUN_MULAI = 2026
        private const val TAHUN_MINIMUM = 2020
    }

    private val repository = BpsRepository()

    private val semuaVariabel =
        mutableListOf<KonsumsiVariable>()

    private val variabelTampil =
        mutableListOf<KonsumsiVariable>()

    private val cacheTahun =
        mutableMapOf<Int, MutableList<Int>>()

    private var tahunTerpilih: Int? = null

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView

    // btnFilter sekarang LinearLayout sesuai XML terbaru.
    private lateinit var btnFilter: LinearLayout
    private lateinit var tvFilterTahun: TextView

    private lateinit var activityTracker: ActivityTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pendapatan)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        btnFilter =
            findViewById(R.id.btnFilter)

        tvFilterTahun =
            findViewById(R.id.tvFilterTahun)

        cardContainer =
            findViewById(R.id.cardContainer)

        progressLoading =
            findViewById(R.id.progressLoading)

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Pendapatan"
        )

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {
            tampilkanDialogTahun()
        }

        perbaruiLabelTahun()

        loadSemuaPage(1)
    }

    // Memperbarui label tahun pada tombol filter.
    private fun perbaruiLabelTahun() {
        tvFilterTahun.text =
            tahunTerpilih?.toString() ?: "Tahun"
    }

    private fun loadSemuaPage(page: Int) {

        if (page == 1) {
            semuaVariabel.clear()
            variabelTampil.clear()
            cacheTahun.clear()

            tahunTerpilih = null
            perbaruiLabelTahun()

            tampilkanLoading(true)
        }

        repository.getKonsumsiVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ).enqueue(
            object : Callback<KonsumsiResponse> {

                override fun onResponse(
                    call: Call<KonsumsiResponse>,
                    response: Response<KonsumsiResponse>
                ) {

                    if (!response.isSuccessful) {
                        tampilkanLoading(false)

                        Toast.makeText(
                            this@PendapatanActivity,
                            "Gagal mengambil data",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val body = response.body()

                    if (body == null) {
                        tampilkanLoading(false)

                        Toast.makeText(
                            this@PendapatanActivity,
                            "Response kosong",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    if (!body.status.equals("OK", ignoreCase = true)) {
                        tampilkanLoading(false)

                        Toast.makeText(
                            this@PendapatanActivity,
                            "Status API tidak OK",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    val variableList =
                        parseVariableList(body)

                    for (item in variableList) {

                        if (item.subcsaId == SUBCSA_ID) {

                            if (
                                semuaVariabel.none {
                                    it.varId == item.varId
                                }
                            ) {
                                semuaVariabel.add(item)
                            }
                        }
                    }

                    val totalPage =
                        parseTotalPage(body)

                    if (page < totalPage) {
                        loadSemuaPage(page + 1)
                    } else {
                        selesaiLoadVariable()
                    }
                }

                override fun onFailure(
                    call: Call<KonsumsiResponse>,
                    t: Throwable
                ) {
                    tampilkanLoading(false)

                    Toast.makeText(
                        this@PendapatanActivity,
                        "Koneksi gagal: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun selesaiLoadVariable() {

        if (semuaVariabel.isEmpty()) {
            tampilkanLoading(false)

            Toast.makeText(
                this,
                "Data Konsumsi dan Pendapatan tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        cariTahunTerbaru(TAHUN_MULAI)
    }

    private fun cariTahunTerbaru(tahun: Int) {

        if (tahun < TAHUN_MINIMUM) {
            tampilkanLoading(false)

            Toast.makeText(
                this,
                "Tidak ada data tahun tersedia",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (cacheTahun.containsKey(tahun)) {

            val hasil =
                cacheTahun[tahun] ?: mutableListOf()

            if (hasil.isNotEmpty()) {
                tahunTerpilih = tahun
                perbaruiLabelTahun()

                filterDariCache(tahun)
            } else {
                cariTahunTerbaru(tahun - 1)
            }

            return
        }

        tampilkanLoading(true)

        cekSatuTahun(tahun = tahun) { variableDenganData ->

            cacheTahun[tahun] =
                variableDenganData.toMutableList()

            if (variableDenganData.isNotEmpty()) {
                tahunTerpilih = tahun
                perbaruiLabelTahun()

                tampilkanLoading(false)
                filterDariCache(tahun)
            } else {
                cariTahunTerbaru(tahun - 1)
            }
        }
    }

    private fun cekSatuTahun(
        tahun: Int,
        selesai: (MutableList<Int>) -> Unit
    ) {

        val variables = semuaVariabel.toList()

        val variableDenganData =
            mutableListOf<Int>()

        if (variables.isEmpty()) {
            selesai(variableDenganData)
            return
        }

        val th = tahun - 1900

        var jumlahSelesai = 0

        for (variable in variables) {

            val variableId = variable.varId

            if (variableId == null) {

                jumlahSelesai++

                if (jumlahSelesai == variables.size) {
                    selesai(variableDenganData)
                }

                continue
            }

            repository.getKonsumsiDetail(
                domain = DOMAIN,
                variable = variableId,
                tahun = th,
                apiKey = API_KEY
            ).enqueue(
                object : Callback<KonsumsiDetailResponse> {

                    override fun onResponse(
                        call: Call<KonsumsiDetailResponse>,
                        response: Response<KonsumsiDetailResponse>
                    ) {

                        var adaData = false

                        if (response.isSuccessful) {

                            val body = response.body()

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

                                if (adaData) {

                                    val tahunList = body.tahun

                                    if (tahunList.isNullOrEmpty()) {
                                        adaData = false
                                    } else {

                                        val tahunAda =
                                            tahunList.any {
                                                it.label?.toIntOrNull() == tahun
                                            }

                                        if (!tahunAda) {
                                            adaData = false
                                        }
                                    }
                                }
                            }
                        }

                        if (adaData) {
                            synchronized(variableDenganData) {
                                if (!variableDenganData.contains(variableId)) {
                                    variableDenganData.add(variableId)
                                }
                            }
                        }

                        jumlahSelesai++

                        if (jumlahSelesai == variables.size) {
                            selesai(variableDenganData)
                        }
                    }

                    override fun onFailure(
                        call: Call<KonsumsiDetailResponse>,
                        t: Throwable
                    ) {

                        jumlahSelesai++

                        if (jumlahSelesai == variables.size) {
                            selesai(variableDenganData)
                        }
                    }
                }
            )
        }
    }

    private fun parseVariableList(
        response: KonsumsiResponse
    ): List<KonsumsiVariable> {

        val result =
            mutableListOf<KonsumsiVariable>()

        val data =
            response.data ?: return result

        if (data.size < 2) {
            return result
        }

        val variableElement = data[1]

        if (!variableElement.isJsonArray) {
            return result
        }

        val array: JsonArray =
            variableElement.asJsonArray

        val gson = Gson()

        for (element in array) {
            try {
                val variable =
                    gson.fromJson(
                        element,
                        KonsumsiVariable::class.java
                    )

                result.add(variable)

            } catch (_: Exception) {
            }
        }

        return result
    }

    private fun parseTotalPage(
        response: KonsumsiResponse
    ): Int {

        val data =
            response.data ?: return 1

        if (data.isEmpty()) {
            return 1
        }

        val pageElement = data[0]

        if (!pageElement.isJsonObject) {
            return 1
        }

        return try {
            pageElement
                .asJsonObject
                .get("pages")
                ?.asInt ?: 1
        } catch (_: Exception) {
            1
        }
    }

    private fun tampilkanDialogTahun() {

        fun dp(value: Int): Int {
            return (value * resources.displayMetrics.density).toInt()
        }

        val daftarTahun = (TAHUN_MULAI downTo TAHUN_MINIMUM).toList()

        // Container utama
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

        // Judul
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

                    // Tahun yang sama tidak perlu memuat ulang.
                    if (tahun == tahunTerpilih) {
                        dialog.dismiss()
                        return@setOnClickListener
                    }

                    dialog.dismiss()

                    tahunTerpilih = tahun

                    perbaruiLabelTahun()

                    loadDataTahun(tahun)
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

    private fun loadDataTahun(tahun: Int) {

        if (cacheTahun.containsKey(tahun)) {
            filterDariCache(tahun)
            return
        }

        tampilkanLoading(true)

        cekSatuTahun(tahun = tahun) { variableDenganData ->

            cacheTahun[tahun] =
                variableDenganData.toMutableList()

            runOnUiThread {

                tampilkanLoading(false)

                filterDariCache(tahun)
            }
        }
    }

    private fun filterDariCache(tahun: Int) {

        variabelTampil.clear()

        val variableIdList =
            cacheTahun[tahun] ?: mutableListOf()

        for (variable in semuaVariabel) {

            val variableId =
                variable.varId ?: continue

            if (variableIdList.contains(variableId)) {
                variabelTampil.add(variable)
            }
        }

        tampilkanDaftar()
    }

    private fun tampilkanDaftar() {

        cardContainer.removeAllViews()

        if (variabelTampil.isEmpty()) {

            val textView = TextView(this)

            textView.text =
                "Data tidak tersedia pada tahun $tahunTerpilih"

            textView.textSize = 16f

            textView.setPadding(
                24,
                32,
                24,
                32
            )

            cardContainer.addView(textView)

            return
        }

        for (item in variabelTampil) {

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
                item.title ?: "Tanpa Judul"

            tvTahun.text =
                tahunTerpilih?.toString() ?: "-"

            card.setOnClickListener {

                val variableId = item.varId

                if (variableId == null) {
                    Toast.makeText(
                        this,
                        "ID variabel tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val tahun = tahunTerpilih

                if (tahun == null) {
                    Toast.makeText(
                        this,
                        "Tahun belum dipilih",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

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
                    item.title ?: "Data Pendapatan"
                )

                startActivity(intent)
            }

            cardContainer.addView(card)
        }
    }

    private fun dataContentMemilikiData(
        dataContent: JsonElement?
    ): Boolean {

        if (dataContent == null || dataContent.isJsonNull) {
            return false
        }

        return when {

            dataContent.isJsonObject -> {
                dataContent.asJsonObject.size() > 0
            }

            dataContent.isJsonArray -> {
                dataContent.asJsonArray.size() > 0
            }

            else -> {
                true
            }
        }
    }

    private fun tampilkanLoading(tampil: Boolean) {

        if (tampil) {
            progressLoading.visibility = View.VISIBLE
            progressLoading.playAnimation()
        } else {
            progressLoading.cancelAnimation()
            progressLoading.visibility = View.GONE
        }
    }
}
