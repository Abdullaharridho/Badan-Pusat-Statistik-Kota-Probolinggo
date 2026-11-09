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
import com.example.bpskota.bps.model.*
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView

class KependudukanActivity : AppCompatActivity() {

    companion object {
        private const val DOMAIN = "3574"
        private const val WILAYAH = "3574000"
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
        private const val KATEGORI = "Kependudukan"

        private const val TAHUN_PROYEKSI_MULAI = 2035
        private const val TAHUN_PROYEKSI_AKHIR = 2000

        private const val EXTRA_TAHUN_TERSEDIA =
            "EXTRA_TAHUN_TERSEDIA"
    }
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private val repository = BpsRepository()

    private lateinit var activityTracker: ActivityTracker

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: LinearLayout
    private lateinit var tvFilterTahun: TextView

    private val semuaTabel =
        mutableListOf<KependudukanTable>()

    private val semuaTabelSimdasi =
        mutableListOf<SimdasiTable>()

    private val semuaVariabel =
        mutableListOf<DataVariabel>()

    private val semuaTahunTersedia =
        mutableListOf<Int>()

    private var tahunTerpilih: Int? = null

    private var totalHalaman = 0
    private var halamanSelesai = 0

    private var listSelesai = false
    private var simdasiLoadSelesai = false
    private var variableLoadSelesai = false
    private var semuaSumberSudahDitampilkan = false

    private val requestSimdasiAktif =
        mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_kependudukan)

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Kependudukan"
        )

        initView()
        setupButton()
        loadSemuaSumberData()
    }

    private fun initView() {

        cardContainer =
            findViewById(R.id.cardContainer)

        progressLoading =
            findViewById(R.id.progressLoading)

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        progressLoading.repeatCount = -1

        btnBack =
            findViewById(R.id.btnBack)

        btnFilter =
            findViewById(R.id.btnFilter)

        tvFilterTahun =
            findViewById(R.id.tvFilterTahun)

        tvFilterTahun.text =
            tahunTerpilih?.toString() ?: "Tahun"

        btnFilter.visibility =
            View.GONE
    }

    private fun setupButton() {

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {
            tampilkanDialogFilterTahun()
        }
    }

    private fun tampilkanLoading() {

        progressLoading.visibility =
            View.VISIBLE

        if (!progressLoading.isAnimating) {
            progressLoading.playAnimation()
        }
    }

    private fun sembunyikanLoading() {

        progressLoading.cancelAnimation()

        progressLoading.visibility =
            View.GONE
    }

    private fun loadSemuaSumberData() {

        tampilkanLoading()

        cardContainer.removeAllViews()

        semuaTabel.clear()
        semuaTabelSimdasi.clear()
        semuaVariabel.clear()
        semuaTahunTersedia.clear()

        tahunTerpilih = null
        tvFilterTahun.text = "Tahun"

        totalHalaman = 0
        halamanSelesai = 0

        listSelesai = false
        simdasiLoadSelesai = false
        variableLoadSelesai = false
        semuaSumberSudahDitampilkan = false

        requestSimdasiAktif.clear()

        btnFilter.visibility =
            View.GONE

        requestPagePertama()

        loadSemuaVariable()

        loadSimdasi()
    }

    private fun requestPagePertama() {

        repository.getKependudukanTables(
            domain = DOMAIN,
            page = 1,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<KependudukanDataResponse> {

                override fun onResponse(
                    call: Call<KependudukanDataResponse>,
                    response: Response<KependudukanDataResponse>
                ) {

                    if (!response.isSuccessful) {

                        listSelesai = true
                        cekSemuaSumberSelesai()

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        listSelesai = true
                        cekSemuaSumberSelesai()

                        return
                    }

                    if (
                        body.status
                            ?.uppercase() != "OK"
                    ) {

                        listSelesai = true
                        cekSemuaSumberSelesai()

                        return
                    }

                    prosesResponseKependudukan(
                        1,
                        body
                    )

                    val paging =
                        ambilInformasiPaging(body)

                    totalHalaman =
                        paging.pages

                    halamanSelesai = 1

                    if (totalHalaman <= 1) {

                        listSelesai = true
                        cekSemuaSumberSelesai()

                        return
                    }

                    requestHalamanBerikutnya(
                        totalHalaman
                    )
                }

                override fun onFailure(
                    call: Call<KependudukanDataResponse>,
                    t: Throwable
                ) {

                    listSelesai = true
                    cekSemuaSumberSelesai()
                }
            }
        )
    }

    private fun requestHalamanBerikutnya(
        totalPages: Int
    ) {

        if (totalPages <= 1) {

            listSelesai = true
            cekSemuaSumberSelesai()

            return
        }

        (2..totalPages).forEach { page ->

            repository.getKependudukanTables(
                domain = DOMAIN,
                page = page,
                apiKey = API_KEY
            ).enqueue(
                object :
                    Callback<KependudukanDataResponse> {

                    override fun onResponse(
                        call: Call<KependudukanDataResponse>,
                        response: Response<KependudukanDataResponse>
                    ) {

                        try {

                            if (!response.isSuccessful) {
                                return
                            }

                            val body =
                                response.body()
                                    ?: return

                            if (
                                body.status
                                    ?.uppercase() != "OK"
                            ) {
                                return
                            }

                            prosesResponseKependudukan(
                                page,
                                body
                            )

                        } finally {

                            halamanSelesai++

                            if (
                                halamanSelesai >= totalHalaman
                            ) {

                                listSelesai = true

                                runOnUiThread {
                                    cekSemuaSumberSelesai()
                                }
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<KependudukanDataResponse>,
                        t: Throwable
                    ) {

                        halamanSelesai++

                        if (
                            halamanSelesai >= totalHalaman
                        ) {

                            listSelesai = true

                            runOnUiThread {
                                cekSemuaSumberSelesai()
                            }
                        }
                    }
                }
            )
        }
    }

    private data class PagingInfo(
        val page: Int,
        val pages: Int,
        val perPage: Int,
        val count: Int,
        val total: Int
    )

    private fun ambilInformasiPaging(
        body: KependudukanDataResponse
    ): PagingInfo {

        val dataApi =
            body.data

        if (
            dataApi == null ||
            !dataApi.isJsonArray
        ) {
            return PagingInfo(
                1,
                1,
                10,
                0,
                0
            )
        }

        val array =
            dataApi.asJsonArray

        if (array.isEmpty()) {

            return PagingInfo(
                1,
                1,
                10,
                0,
                0
            )
        }

        val paging =
            array[0]

        if (!paging.isJsonObject) {

            return PagingInfo(
                1,
                1,
                10,
                0,
                0
            )
        }

        return try {

            val obj =
                paging.asJsonObject

            PagingInfo(
                page =
                obj["page"]?.asInt ?: 1,

                pages =
                obj["pages"]?.asInt ?: 1,

                perPage =
                obj["per_page"]?.asInt ?: 10,

                count =
                obj["count"]?.asInt ?: 0,

                total =
                obj["total"]?.asInt ?: 0
            )

        } catch (e: Exception) {

            PagingInfo(
                1,
                1,
                10,
                0,
                0
            )
        }
    }

    private fun prosesResponseKependudukan(
        page: Int,
        body: KependudukanDataResponse
    ) {

        val dataApi =
            body.data

        if (
            dataApi == null ||
            !dataApi.isJsonArray
        ) {
            return
        }

        val array =
            dataApi.asJsonArray

        if (array.size() < 2) {
            return
        }

        val dataTabel =
            array[1]

        if (!dataTabel.isJsonArray) {
            return
        }

        val gson =
            Gson()

        val tabelPage =
            mutableListOf<KependudukanTable>()

        dataTabel.asJsonArray.forEach { element ->

            try {

                val tabel =
                    gson.fromJson(
                        element,
                        KependudukanTable::class.java
                    )

                if (
                    adalahTabelKependudukan(tabel)
                ) {

                    tabelPage.add(tabel)
                }

            } catch (e: Exception) {
            }
        }

        synchronized(semuaTabel) {
            semuaTabel.addAll(tabelPage)
        }
    }

    private fun adalahTabelKependudukan(
        tabel: KependudukanTable
    ): Boolean {

        val subject =
            tabel.subject
                ?.trim()
                ?.lowercase()
                ?: ""

        val title =
            tabel.title
                ?.trim()
                ?.lowercase()
                ?: ""

        val subjectKependudukan =
            subject == "kependudukan" ||
                    subject.contains("kependudukan")

        if (subjectKependudukan) {
            return true
        }

        if (subject.isBlank()) {

            return title.startsWith("penduduk") ||
                    title.startsWith("proyeksi penduduk") ||
                    title.contains("kependudukan")
        }

        return false
    }

    private fun loadSemuaVariable() {

        loadVariablePage(1)
    }

    private fun loadVariablePage(
        page: Int
    ) {

        repository.getKependudukanVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<KependudukanDataResponse> {

                override fun onResponse(
                    call: Call<KependudukanDataResponse>,
                    response: Response<KependudukanDataResponse>
                ) {

                    if (!response.isSuccessful) {

                        variableLoadSelesai = true

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        variableLoadSelesai = true

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }

                        return
                    }

                    if (
                        body.status
                            ?.uppercase() != "OK"
                    ) {

                        variableLoadSelesai = true

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }

                        return
                    }

                    prosesVariablePage(
                        page,
                        body
                    )

                    val paging =
                        ambilInformasiPaging(body)

                    if (page < paging.pages) {

                        loadVariablePage(
                            page + 1
                        )

                    } else {

                        variableLoadSelesai = true

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<KependudukanDataResponse>,
                    t: Throwable
                ) {

                    variableLoadSelesai = true

                    runOnUiThread {
                        cekSemuaSumberSelesai()
                    }
                }
            }
        )
    }

    private fun prosesVariablePage(
        page: Int,
        body: KependudukanDataResponse
    ) {

        val dataApi =
            body.data

        if (
            dataApi == null ||
            !dataApi.isJsonArray
        ) {
            return
        }

        val array =
            dataApi.asJsonArray

        if (array.size() < 2) {
            return
        }

        val dataVariable =
            array[1]

        if (!dataVariable.isJsonArray) {
            return
        }

        val gson =
            Gson()

        val variablePage =
            mutableListOf<DataVariabel>()

        dataVariable.asJsonArray.forEach { element ->

            try {

                val variable =
                    gson.fromJson(
                        element,
                        DataVariabel::class.java
                    )

                if (variable.subcsaId == 519) {

                    variablePage.add(
                        variable
                    )
                }

            } catch (e: Exception) {
            }
        }

        synchronized(semuaVariabel) {
            semuaVariabel.addAll(
                variablePage
            )
        }
    }

    private fun loadSimdasi() {

        val tahunRequest =
            2035

        requestSimdasiAktif.add(
            tahunRequest
        )

        repository.getSimdasiTables(
            tahun = tahunRequest,
            wilayah = WILAYAH,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<SimdasiResponse> {

                override fun onResponse(
                    call: Call<SimdasiResponse>,
                    response: Response<SimdasiResponse>
                ) {

                    try {

                        if (!response.isSuccessful) {
                            return
                        }

                        val body =
                            response.body()
                                ?: return

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {
                            return
                        }

                        prosesSimdasiResponse(
                            body
                        )

                    } finally {

                        requestSimdasiAktif.remove(
                            tahunRequest
                        )

                        simdasiLoadSelesai =
                            requestSimdasiAktif.isEmpty()

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<SimdasiResponse>,
                    t: Throwable
                ) {

                    requestSimdasiAktif.remove(
                        tahunRequest
                    )

                    simdasiLoadSelesai =
                        requestSimdasiAktif.isEmpty()

                    runOnUiThread {
                        cekSemuaSumberSelesai()
                    }
                }
            }
        )
    }

    private fun prosesSimdasiResponse(
        body: SimdasiResponse
    ) {

        val dataSimdasi =
            body.data
                ?: return

        dataSimdasi.forEach { pageData ->

            val tables =
                pageData.tables
                    ?: emptyList()

            tables.forEach { tabel ->

                val subject =
                    tabel.subject
                        ?.trim()
                        ?.lowercase()
                        ?: ""

                val mmsSubject =
                    tabel.mmsSubject
                        ?.trim()
                        ?.lowercase()
                        ?: ""

                val judul =
                    tabel.judul
                        ?.trim()
                        ?.lowercase()
                        ?: ""

                val cocok =
                    subject == "kependudukan" ||
                            subject.contains("kependudukan") ||
                            mmsSubject == "kependudukan" ||
                            mmsSubject.contains("kependudukan") ||
                            judul.startsWith("penduduk") ||
                            judul.startsWith("proyeksi penduduk") ||
                            judul.contains("kependudukan")

                if (!cocok) {
                    return@forEach
                }

                synchronized(
                    semuaTabelSimdasi
                ) {

                    semuaTabelSimdasi.add(
                        tabel
                    )
                }
            }
        }
    }

    private fun cekSemuaSumberSelesai() {

        if (!listSelesai) {
            return
        }

        if (!variableLoadSelesai) {
            return
        }

        if (!simdasiLoadSelesai) {
            return
        }

        if (semuaSumberSudahDitampilkan) {
            return
        }

        semuaSumberSudahDitampilkan =
            true

        selesaiLoadSemuaSumber()
    }

    private fun selesaiLoadSemuaSumber() {

        val staticUnik =
            semuaTabel
                .filter {
                    it.tableId != null
                }
                .distinctBy {
                    it.tableId
                }

        semuaTabel.clear()
        semuaTabel.addAll(
            staticUnik
        )

        val simdasiUnik =
            semuaTabelSimdasi
                .filter {
                    !it.idTabel.isNullOrBlank()
                }
                .distinctBy {
                    it.idTabel
                }

        semuaTabelSimdasi.clear()
        semuaTabelSimdasi.addAll(
            simdasiUnik
        )

        val variableUnik =
            semuaVariabel
                .filter {
                    it.varId != null
                }
                .distinctBy {
                    it.varId
                }

        semuaVariabel.clear()
        semuaVariabel.addAll(
            variableUnik
        )

        bangunDaftarTahun()

        tampilkanSemuaData()
    }

    private fun bangunDaftarTahun() {

        val tahun =
            mutableSetOf<Int>()

        semuaTabel.forEach { tabel ->

            if (adalahProyeksi(tabel)) {

                tahun.addAll(
                    buatDaftarTahunProyeksi()
                )

            } else {

                tahun.addAll(
                    ambilTahunDariJudul(
                        tabel.title
                    )
                )
            }
        }

        if (semuaVariabel.isNotEmpty()) {

            tahun.addAll(
                buatDaftarTahunProyeksi()
            )
        }

        semuaTabelSimdasi.forEach { tabel ->

            tahun.addAll(
                tabel.ketersediaanTahun
                    ?.mapNotNull {
                        it
                    }
                    ?: emptyList()
            )
        }

        semuaTahunTersedia.clear()

        semuaTahunTersedia.addAll(
            tahun.sortedDescending()
        )

        tahunTerpilih =
            semuaTahunTersedia.firstOrNull()

        tvFilterTahun.text =
            tahunTerpilih?.toString() ?: "Tahun"
    }

    // =====================================================
// FILTER TAHUN - DESAIN DAN ANIMASI GENDERACTIVITY
// =====================================================

    private fun tampilkanDialogFilterTahun() {

        if (semuaTahunTersedia.isEmpty()) {

            Toast.makeText(
                this,
                "Tahun tersedia tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val daftarTahun = semuaTahunTersedia
            .distinct()
            .sortedDescending()

        // =================================================
        // CONTAINER UTAMA
        // =================================================

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

        // =================================================
        // JUDUL DIALOG
        // =================================================

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

        // =================================================
        // SCROLLVIEW DAFTAR TAHUN
        // =================================================

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

        // =================================================
        // DIALOG
        // Dibuat sebelum listener item tahun
        // =================================================

        val dialog =
            AlertDialog.Builder(this)
                .setView(root)
                .create()

        // =================================================
        // DAFTAR ITEM TAHUN
        // =================================================

        daftarTahun.forEach { tahunItem ->

            val terpilih =
                tahunItem == tahunTerpilih

            val item =
                TextView(this).apply {

                    text = tahunItem.toString()

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

                        // Simpan tahun yang dipilih
                        tahunTerpilih = tahunItem

                        // Perbarui label tahun
                        tvFilterTahun.text =
                            tahunTerpilih.toString()

                        // Tutup dialog
                        dialog.dismiss()

                        // Langsung tampilkan data sesuai tahun
                        tampilkanSemuaData()
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

        // =================================================
        // FOOTER - TOMBOL TUTUP
        // =================================================

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

        // =================================================
        // TAMPILKAN DIALOG
        // =================================================

        dialog.show()

        // =================================================
        // UKURAN DAN POSISI DIALOG
        // =================================================

        dialog.window?.let { window ->

            val displayMetrics =
                resources.displayMetrics

            val width =
                (displayMetrics.widthPixels * 0.70f).toInt()

            val height =
                (displayMetrics.heightPixels * 0.50f).toInt()

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

            // =================================================
            // ANIMASI FADE + SCALE
            // =================================================

            window.decorView.post {

                val decorView =
                    window.decorView

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

    private fun tampilkanSemuaData() {

        cardContainer.removeAllViews()

        val tahun =
            tahunTerpilih

        if (tahun == null) {

            semuaTabel.forEach { tabel ->
                tampilkanCardList(
                    tabel
                )
            }

            semuaVariabel.forEach { variabel ->
                tampilkanCardVariabel(
                    variabel
                )
            }

            semuaTabelSimdasi.forEach { tabel ->
                tampilkanCardSimdasi(
                    tabel
                )
            }

        } else {

            semuaTabel.forEach { tabel ->

                if (
                    tabelMemilikiTahun(
                        tabel,
                        tahun
                    )
                ) {

                    tampilkanCardList(
                        tabel
                    )
                }
            }

            semuaVariabel.forEach { variabel ->

                if (
                    tahun in
                    TAHUN_PROYEKSI_AKHIR..TAHUN_PROYEKSI_MULAI
                ) {

                    tampilkanCardVariabel(
                        variabel
                    )
                }
            }

            semuaTabelSimdasi.forEach { tabel ->

                val tahunTersedia =
                    tabel.ketersediaanTahun
                        ?.contains(tahun)
                        ?: false

                if (tahunTersedia) {

                    tampilkanCardSimdasi(
                        tabel
                    )
                }
            }
        }

        sembunyikanLoading()

        btnFilter.visibility =
            if (semuaTahunTersedia.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (cardContainer.childCount == 0) {

            val tvKosong =
                TextView(this)

            tvKosong.text =
                if (tahun != null) {
                    "Tidak ada data Kependudukan untuk tahun $tahun"
                } else {
                    "Tidak ada data Kependudukan"
                }

            tvKosong.textSize =
                16f

            tvKosong.setPadding(
                32,
                32,
                32,
                32
            )

            cardContainer.addView(
                tvKosong
            )
        }
    }

    private fun tampilkanCardList(
        tabel: KependudukanTable
    ) {

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    cardContainer,
                    false
                )

        val tvTahun =
            card.findViewById<TextView>(
                R.id.tvTahun
            )

        val tvKategori =
            card.findViewById<TextView>(
                R.id.tvKategori
            )

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        val proyeksi =
            adalahProyeksi(tabel)

        val daftarTahun =
            if (proyeksi) {

                buatDaftarTahunProyeksi()

            } else {

                ambilTahunDariJudul(
                    tabel.title
                )
                    .sortedDescending()
            }

        tvTahun.text =
            if (daftarTahun.isNotEmpty()) {

                daftarTahun.joinToString(
                    ", "
                )

            } else {

                "-"
            }

        tvKategori.text =
            tabel.subject
                ?: KATEGORI

        tvJudul.text =
            tabel.title
                ?: "Judul tidak tersedia"

        card.setOnClickListener {

            onListTableClicked(
                tabel = tabel,
                membutuhkanTahun = proyeksi
            )
        }

        cardContainer.addView(
            card
        )
    }

    private fun tampilkanCardVariabel(
        variabel: DataVariabel
    ) {

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    cardContainer,
                    false
                )

        val tvKode =
            card.findViewById<TextView>(
                R.id.tvKode
            )

        val tvTahun =
            card.findViewById<TextView>(
                R.id.tvTahun
            )

        val tvKategori =
            card.findViewById<TextView>(
                R.id.tvKategori
            )

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        tvKode.visibility =
            View.GONE

        tvTahun.text =
            tahunTerpilih?.toString()
                ?: "-"

        tvKategori.text =
            variabel.subName
                ?: KATEGORI

        tvJudul.text =
            variabel.title
                ?: "Variabel tidak tersedia"

        card.setOnClickListener {

            onVariabelClicked(
                variabel
            )
        }

        cardContainer.addView(
            card
        )
    }

    private fun tampilkanCardSimdasi(
        tabel: SimdasiTable
    ) {

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    cardContainer,
                    false
                )

        val tvKode =
            card.findViewById<TextView>(
                R.id.tvKode
            )

        val tvTahun =
            card.findViewById<TextView>(
                R.id.tvTahun
            )

        val tvKategori =
            card.findViewById<TextView>(
                R.id.tvKategori
            )

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        tvKode.text =
            tabel.kodeTabel
                ?: tabel.idTabel
                        ?: "-"

        val daftarTahun =
            tabel.ketersediaanTahun
                ?.distinct()
                ?.sortedDescending()
                ?: emptyList()

        tvTahun.text =
            if (tahunTerpilih != null) {

                tahunTerpilih.toString()

            } else if (
                daftarTahun.isNotEmpty()
            ) {

                daftarTahun.joinToString(
                    ", "
                )

            } else {

                "-"
            }

        tvKategori.text =
            tabel.mmsSubject
                ?: tabel.subject
                        ?: KATEGORI

        tvJudul.text =
            tabel.judul
                ?: "Judul tidak tersedia"

        card.setOnClickListener {

            onSimdasiTableClicked(
                tabel
            )
        }

        cardContainer.addView(
            card
        )
    }

    private fun tabelMemilikiTahun(
        tabel: KependudukanTable,
        tahun: Int
    ): Boolean {

        if (adalahProyeksi(tabel)) {

            return tahun in
                    TAHUN_PROYEKSI_AKHIR..TAHUN_PROYEKSI_MULAI
        }

        val daftarTahun =
            ambilTahunDariJudul(
                tabel.title
            )

        return daftarTahun.contains(
            tahun
        )
    }

    private fun onVariabelClicked(
        variabel: DataVariabel
    ) {

        val variableId =
            variabel.varId

        if (variableId == null) {

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
                KependudukanDetailActivity::class.java
            )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_VARIABLE,
            variableId
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_JUDUL,
            variabel.title
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_IS_SIMDASI,
            false
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_STATIC_BUTUH_TAHUN,
            true
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_IS_VARIABLE,
            true
        )

        val daftarTahun =
            buatDaftarTahunProyeksi()

        val tahun =
            tahunTerpilih
                ?: daftarTahun.firstOrNull()

        if (tahun != null) {

            intent.putExtra(
                KependudukanDetailActivity.EXTRA_TAHUN,
                tahun
            )

            intent.putIntegerArrayListExtra(
                EXTRA_TAHUN_TERSEDIA,
                ArrayList(
                    daftarTahun
                )
            )
        }

        startActivity(intent)
    }

    private fun onListTableClicked(
        tabel: KependudukanTable,
        membutuhkanTahun: Boolean
    ) {

        val tableId =
            tabel.tableId

        if (tableId == null) {

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
                KependudukanDetailActivity::class.java
            )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_VARIABLE,
            tableId
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_JUDUL,
            tabel.title
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_STATIC_BUTUH_TAHUN,
            membutuhkanTahun
        )

        intent.putExtra(
            KependudukanDetailActivity.EXTRA_IS_SIMDASI,
            false
        )

        if (membutuhkanTahun) {

            val daftarTahun =
                buatDaftarTahunProyeksi()

            val tahun =
                tahunTerpilih
                    ?: daftarTahun.firstOrNull()

            if (tahun == null) {

                Toast.makeText(
                    this,
                    "Tahun proyeksi tidak tersedia",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            intent.putExtra(
                KependudukanDetailActivity.EXTRA_TAHUN,
                tahun
            )

            intent.putIntegerArrayListExtra(
                EXTRA_TAHUN_TERSEDIA,
                ArrayList(
                    daftarTahun
                )
            )

        } else {

            val daftarTahun =
                ambilTahunDariJudul(
                    tabel.title
                )

            val tahun =
                tahunTerpilih
                    ?.takeIf {
                        daftarTahun.contains(it)
                    }
                    ?: daftarTahun.maxOrNull()

            if (tahun != null) {

                intent.putExtra(
                    KependudukanDetailActivity.EXTRA_TAHUN,
                    tahun
                )

                intent.putIntegerArrayListExtra(
                    EXTRA_TAHUN_TERSEDIA,
                    ArrayList(
                        daftarTahun.sortedDescending()
                    )
                )
            }
        }

        startActivity(intent)
    }

    private fun onSimdasiTableClicked(
        tabel: SimdasiTable
    ) {

        val tableId =
            tabel.idTabel

        if (tableId.isNullOrBlank()) {

            Toast.makeText(
                this,
                "ID tabel SIMDASI tidak tersedia",
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
            tableId
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_JUDUL,
            tabel.judul
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_KODE,
            tabel.kodeTabel
        )

        val daftarTahun =
            tabel.ketersediaanTahun
                ?.distinct()
                ?.sortedDescending()
                ?: emptyList()

        val tahun =
            tahunTerpilih
                ?.takeIf {
                    daftarTahun.contains(it)
                }
                ?: daftarTahun.firstOrNull()

        if (tahun != null) {

            intent.putExtra(
                StatistikDetailActivity.EXTRA_TAHUN,
                tahun
            )
        }

        intent.putIntegerArrayListExtra(
            EXTRA_TAHUN_TERSEDIA,
            ArrayList(
                daftarTahun
            )
        )

        startActivity(intent)
    }

    private fun adalahProyeksi(
        tabel: KependudukanTable
    ): Boolean {

        val judul =
            tabel.title
                ?.trim()
                ?.lowercase()
                ?: ""

        val subject =
            tabel.subject
                ?.trim()
                ?.lowercase()
                ?: ""

        return judul.contains("proyeksi") ||
                judul.contains("projection") ||
                subject.contains("proyeksi") ||
                subject.contains("projection")
    }

    private fun buatDaftarTahunProyeksi(): List<Int> {

        return (
                TAHUN_PROYEKSI_AKHIR..
                        TAHUN_PROYEKSI_MULAI
                )
            .toList()
            .sortedDescending()
    }

    private fun ambilTahunDariJudul(
        judul: String?
    ): List<Int> {

        if (judul.isNullOrBlank()) {
            return emptyList()
        }

        val regex =
            Regex(
                "(?<!\\d)(19\\d{2}|20\\d{2})(?!\\d)"
            )

        return regex
            .findAll(judul)
            .map {
                it.value.toInt()
            }
            .distinct()
            .sorted()
            .toList()
    }

    override fun onDestroy() {

        if (
            ::progressLoading.isInitialized
        ) {

            progressLoading.cancelAnimation()

            progressLoading.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}