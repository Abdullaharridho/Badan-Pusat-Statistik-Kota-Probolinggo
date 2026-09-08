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
import com.example.bpskota.bps.model.*
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KependudukanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KependudukanAPI"
        private const val DOMAIN = "3574"
        private const val WILAYAH = "3574000"
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
        private const val KATEGORI = "Kependudukan"

        private const val TAHUN_PROYEKSI_MULAI = 2035
        private const val TAHUN_PROYEKSI_AKHIR = 2000

        private const val EXTRA_TAHUN_TERSEDIA =
            "EXTRA_TAHUN_TERSEDIA"
    }

    private val repository = BpsRepository()

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView

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

    // =========================================================
    // STATIC TABLE
    // =========================================================

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

                        Log.e(
                            TAG,
                            "GAGAL PAGE 1 HTTP ${response.code()}"
                        )

                        listSelesai = true
                        cekSemuaSumberSelesai()

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        Log.e(
                            TAG,
                            "BODY PAGE 1 KOSONG"
                        )

                        listSelesai = true
                        cekSemuaSumberSelesai()

                        return
                    }

                    if (
                        body.status
                            ?.uppercase() != "OK"
                    ) {

                        Log.e(
                            TAG,
                            "STATUS PAGE 1 ${body.status}"
                        )

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

                    Log.d(
                        TAG,
                        "TOTAL HALAMAN STATIC = $totalHalaman"
                    )

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

                    Log.e(
                        TAG,
                        "GAGAL LIST PAGE 1",
                        t
                    )

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

                                Log.e(
                                    TAG,
                                    "PAGE $page HTTP ${response.code()}"
                                )

                                return
                            }

                            val body =
                                response.body()
                                    ?: return

                            if (
                                body.status
                                    ?.uppercase() != "OK"
                            ) {

                                Log.e(
                                    TAG,
                                    "PAGE $page STATUS ${body.status}"
                                )

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

                        Log.e(
                            TAG,
                            "GAGAL PAGE $page",
                            t
                        )

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

            Log.e(
                TAG,
                "GAGAL PARSE PAGING",
                e
            )

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

            Log.d(
                TAG,
                "PAGE $page DATA BUKAN ARRAY"
            )

            return
        }

        val array =
            dataApi.asJsonArray

        if (array.size() < 2) {

            Log.d(
                TAG,
                "PAGE $page DATA KURANG DARI 2"
            )

            return
        }

        val dataTabel =
            array[1]

        if (!dataTabel.isJsonArray) {

            Log.d(
                TAG,
                "PAGE $page DATA TABEL BUKAN ARRAY"
            )

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

                    Log.d(
                        TAG,
                        "STATIC LOLOS FILTER = " +
                                "${tabel.tableId} | " +
                                "${tabel.title}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "GAGAL PARSE PAGE $page",
                    e
                )
            }
        }

        synchronized(semuaTabel) {
            semuaTabel.addAll(tabelPage)
        }

        Log.d(
            TAG,
            "PAGE $page -> " +
                    "${tabelPage.size} " +
                    "tabel Kependudukan"
        )
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

    // =========================================================
    // VARIABLE
    // =========================================================

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

                        Log.e(
                            TAG,
                            "VARIABLE PAGE $page HTTP ${response.code()}"
                        )

                        variableLoadSelesai = true

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        Log.e(
                            TAG,
                            "VARIABLE PAGE $page BODY KOSONG"
                        )

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

                        Log.e(
                            TAG,
                            "VARIABLE PAGE $page STATUS ${body.status}"
                        )

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

                    Log.d(
                        TAG,
                        "VARIABLE PAGE = " +
                                "$page / ${paging.pages}"
                    )

                    if (page < paging.pages) {

                        loadVariablePage(
                            page + 1
                        )

                    } else {

                        variableLoadSelesai = true

                        Log.d(
                            TAG,
                            "================================"
                        )

                        Log.d(
                            TAG,
                            "SEMUA VARIABLE SELESAI"
                        )

                        Log.d(
                            TAG,
                            "TOTAL VARIABLE KEPEENDUDUKAN = " +
                                    semuaVariabel.size
                        )

                        Log.d(
                            TAG,
                            "================================"
                        )

                        runOnUiThread {
                            cekSemuaSumberSelesai()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<KependudukanDataResponse>,
                    t: Throwable
                ) {

                    Log.e(
                        TAG,
                        "GAGAL VARIABLE PAGE $page",
                        t
                    )

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

            Log.d(
                TAG,
                "VARIABLE PAGE $page DATA BUKAN ARRAY"
            )

            return
        }

        val array =
            dataApi.asJsonArray

        if (array.size() < 2) {

            Log.d(
                TAG,
                "VARIABLE PAGE $page DATA KURANG DARI 2"
            )

            return
        }

        val dataVariable =
            array[1]

        if (!dataVariable.isJsonArray) {

            Log.d(
                TAG,
                "VARIABLE PAGE $page DATA VARIABLE BUKAN ARRAY"
            )

            return
        }

        Log.d(
            TAG,
            "VARIABLE PAGE $page JUMLAH DATA = " +
                    "${dataVariable.asJsonArray.size()}"
        )

        dataVariable
            .asJsonArray
            .take(3)
            .forEachIndexed { index, element ->

                Log.d(
                    TAG,
                    "VARIABLE PAGE $page SAMPLE $index = $element"
                )
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

                Log.d(
                    TAG,
                    "VARIABLE RAW = " +
                            "var_id=${variable.varId} | " +
                            "title=${variable.title} | " +
                            "subcsa_name='${variable.subcsaName}' | " +
                            "subcsa_id=${variable.subcsaId}"
                )

                if (variable.subcsaId == 519) {

                    variablePage.add(
                        variable
                    )

                    Log.d(
                        TAG,
                        "VARIABLE KE PENDUDUKAN = " +
                                "var_id=${variable.varId} | " +
                                "title=${variable.title}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "GAGAL PARSE VARIABLE PAGE $page",
                    e
                )
            }
        }

        synchronized(semuaVariabel) {
            semuaVariabel.addAll(
                variablePage
            )
        }

        Log.d(
            TAG,
            "VARIABLE PAGE $page -> " +
                    "${variablePage.size} " +
                    "variable Kependudukan"
        )
    }

    // =========================================================
    // SIMDASI
    // =========================================================

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

                            Log.e(
                                TAG,
                                "SIMDASI HTTP ${response.code()}"
                            )

                            return
                        }

                        val body =
                            response.body()
                                ?: return

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {

                            Log.e(
                                TAG,
                                "SIMDASI STATUS = ${body.status}"
                            )

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

                    Log.e(
                        TAG,
                        "SIMDASI GAGAL",
                        t
                    )

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
        Log.d(
            TAG,
            "SIMDASI BODY = ${Gson().toJson(body)}"
        )


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

        Log.d(
            TAG,
            "SIMDASI DITEMUKAN = " +
                    semuaTabelSimdasi.size
        )
    }

    // =========================================================
    // SELESAI LOAD
    // =========================================================

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

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "SEMUA DATA SELESAI"
        )

        Log.d(
            TAG,
            "STATIC = ${semuaTabel.size}"
        )

        Log.d(
            TAG,
            "VARIABLE = ${semuaVariabel.size}"
        )

        Log.d(
            TAG,
            "SIMDASI = ${semuaTabelSimdasi.size}"
        )

        Log.d(
            TAG,
            "================================"
        )

        bangunDaftarTahun()

        tampilkanSemuaData()
    }

    // =========================================================
    // TAHUN
    // =========================================================

    private fun bangunDaftarTahun() {

        val tahun =
            mutableSetOf<Int>()

        // -----------------------------------------------------
        // STATIC
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // VARIABLE
        // -----------------------------------------------------
        //
        // Variable Kependudukan yang sudah ditemukan
        // tetap menggunakan rentang tahun proyeksi.
        //
        // -----------------------------------------------------

        if (semuaVariabel.isNotEmpty()) {

            tahun.addAll(
                buatDaftarTahunProyeksi()
            )
        }

        // -----------------------------------------------------
        // SIMDASI
        // -----------------------------------------------------

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

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "TAHUN TERSEDIA = $semuaTahunTersedia"
        )

        Log.d(
            TAG,
            "TAHUN TERPILIH = $tahunTerpilih"
        )

        Log.d(
            TAG,
            "================================"
        )
    }

    private fun tampilkanDialogFilterTahun() {

        if (semuaTahunTersedia.isEmpty()) {

            Toast.makeText(
                this,
                "Tahun tersedia tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val tahunArray =
            semuaTahunTersedia
                .map {
                    it.toString()
                }
                .toTypedArray()

        val posisiTerpilih =
            semuaTahunTersedia
                .indexOf(
                    tahunTerpilih
                )

        AlertDialog.Builder(this)
            .setTitle("Pilih Tahun")
            .setSingleChoiceItems(
                tahunArray,
                posisiTerpilih
            ) { dialog, which ->

                val tahun =
                    semuaTahunTersedia[
                            which
                    ]

                tahunTerpilih =
                    tahun

                Log.d(
                    TAG,
                    "FILTER TAHUN DIPILIH = $tahun"
                )

                dialog.dismiss()

                tampilkanSemuaData()
            }
            .setNegativeButton(
                "Batal",
                null
            )
            .show()
    }

    // =========================================================
    // TAMPIL DATA
    // =========================================================

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

            // STATIC
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

            // VARIABLE
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

            // SIMDASI
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

    // =========================================================
    // STATIC CARD
    // =========================================================

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

    // =========================================================
    // VARIABLE CARD
    // =========================================================

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

        tvKode.text =
            variabel.varId?.toString()
                ?: "-"

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

    // =========================================================
    // SIMDASI CARD
    // =========================================================

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

    // =========================================================
    // CEK TAHUN STATIC
    // =========================================================

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

    // =========================================================
    // CLICK VARIABLE
    // =========================================================

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
        intent.putExtra(KependudukanDetailActivity.EXTRA_IS_VARIABLE, true)

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

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "CLICK VARIABLE"
        )

        Log.d(
            TAG,
            "VARIABLE ID = $variableId"
        )

        Log.d(
            TAG,
            "TITLE = ${variabel.title}"
        )

        Log.d(
            TAG,
            "SUB_NAME = ${variabel.subName}"
        )

        Log.d(
            TAG,
            "TAHUN DEFAULT = $tahun"
        )

        Log.d(
            TAG,
            "================================"
        )

        startActivity(intent)
    }

    // =========================================================
    // CLICK STATIC
    // =========================================================

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

            Log.d(
                TAG,
                "================================"
            )

            Log.d(
                TAG,
                "CLICK STATIC PROYEKSI"
            )

            Log.d(
                TAG,
                "ID = $tableId"
            )

            Log.d(
                TAG,
                "TAHUN TERPILIH = $tahun"
            )

            Log.d(
                TAG,
                "RENTANG = " +
                        "$TAHUN_PROYEKSI_MULAI - " +
                        "$TAHUN_PROYEKSI_AKHIR"
            )

            Log.d(
                TAG,
                "================================"
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

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "CLICK STATIC BIASA"
                )

                Log.d(
                    TAG,
                    "ID = $tableId"
                )

                Log.d(
                    TAG,
                    "TAHUN TERPILIH = $tahun"
                )

                Log.d(
                    TAG,
                    "TAHUN TERSEDIA = $daftarTahun"
                )

                Log.d(
                    TAG,
                    "================================"
                )

            } else {

                Log.d(
                    TAG,
                    "STATIC TANPA TAHUN"
                )
            }
        }

        startActivity(intent)
    }

    // =========================================================
    // CLICK SIMDASI
    // =========================================================

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

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "CLICK SIMDASI"
        )

        Log.d(
            TAG,
            "ID TABEL = $tableId"
        )

        Log.d(
            TAG,
            "KODE TABEL = ${tabel.kodeTabel}"
        )

        Log.d(
            TAG,
            "JUDUL = ${tabel.judul}"
        )

        Log.d(
            TAG,
            "TAHUN TERPILIH GLOBAL = $tahunTerpilih"
        )

        Log.d(
            TAG,
            "TAHUN YANG DIKIRIM = $tahun"
        )

        Log.d(
            TAG,
            "TAHUN TERSEDIA = $daftarTahun"
        )

        Log.d(
            TAG,
            "================================"
        )

        startActivity(intent)
    }

    // =========================================================
    // PROYEKSI
    // =========================================================

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

    // =========================================================
    // AMBIL TAHUN DARI JUDUL
    // =========================================================

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

    // =========================================================
    // DESTROY
    // =========================================================

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