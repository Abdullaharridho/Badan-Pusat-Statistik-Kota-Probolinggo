package com.example.bpskota

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bps.repository.BpsAllDataRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.databinding.ActivityDetailKategoriBinding
import com.example.bpskota.tracking.ActivityTracker
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.Calendar

class DetailKategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailKategoriBinding

    private val repository = BpsAllDataRepository()

    private lateinit var activityTracker: ActivityTracker

    companion object {
        private const val TAG = "DetailKategoriActivity"

        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
        private const val DOMAIN = "3574"
        private const val WILAYAH = "3574000"
    }

    private var idData = ""
    private var typeData = ""
    private var judulData = ""
    private var kategoriData = ""
    private var sumberData = ""

    /**
     * Data untuk card.
     *
     * Contoh:
     *
     * wilayah = "Kota Probolinggo"
     *
     * dataBaris:
     * Januari  -> 110.4
     * Februari -> 111.64
     */
    private data class CardData(
        val wilayah: String,
        val dataBaris: List<Pair<String, String>>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ============================================================
        // TRACKING
        // ============================================================

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        // ============================================================
        // AMBIL DATA DARI INTENT
        // ============================================================

        judulData = intent.getStringExtra("JUDUL") ?: "Detail Data"
        kategoriData = intent.getStringExtra("KATEGORI") ?: "Kategori"
        sumberData = intent.getStringExtra("SUMBER") ?: "BPS Kota Probolinggo"
        idData = intent.getStringExtra("ID") ?: ""
        typeData = intent.getStringExtra("TYPE") ?: ""

        Log.d(
            TAG,
            "Menerima Intent -> ID: $idData, TYPE: $typeData"
        )

        // ============================================================
        // CATAT SCREEN VIEW + METADATA
        // ============================================================

        activityTracker.trackScreen(
            screen = "DetailKategori",
            metadata = mapOf(
                "type" to typeData,
                "data_id" to idData,
                "category" to kategoriData,
                "source" to sumberData
            )
        )

        setupUI()

        // ============================================================
        // ROUTE BERDASARKAN TYPE
        // ============================================================

        if (typeData.equals("STATIC", ignoreCase = true)) {

            binding.cardFilterTahun.visibility = View.GONE

            loadDataRoute(0)

        } else {

            setupSpinnerTahun()
        }
    }

    // ================================================================
    // SETUP UI
    // ================================================================

    private fun setupUI() {

        binding.tvJudul.text = judulData
        binding.tvKategori.text = kategoriData
        binding.tvSumber.text = "Sumber: $sumberData"

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDownload.setOnClickListener {
            Toast.makeText(
                this,
                "Fitur download belum tersedia",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ================================================================
    // SPINNER TAHUN
    // ================================================================

    private fun setupSpinnerTahun() {

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val tahunIntent = intent.getIntExtra(
            "TAHUN",
            currentYear
        )

        val isProyeksi = judulData.contains(
            "proyeksi",
            ignoreCase = true
        )

        val maxYear = if (isProyeksi) {
            2035
        } else {
            maxOf(currentYear, tahunIntent)
        }

        val listTahun = mutableListOf<Int>()

        for (year in maxYear downTo 2015) {
            listTahun.add(year)
        }

        val adapter =
            object : ArrayAdapter<Int>(
                this,
                android.R.layout.simple_spinner_item,
                listTahun
            ) {

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view = super.getView(
                        position,
                        convertView,
                        parent
                    )

                    val tv =
                        view.findViewById<TextView>(
                            android.R.id.text1
                        )

                    tv.text = listTahun[position].toString()

                    tv.setTextColor(
                        Color.parseColor("#111827")
                    )

                    tv.textSize = 15f

                    tv.setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    tv.gravity = Gravity.CENTER

                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view = super.getDropDownView(
                        position,
                        convertView,
                        parent
                    )

                    val tv =
                        view.findViewById<TextView>(
                            android.R.id.text1
                        )

                    tv.text = listTahun[position].toString()

                    tv.setTextColor(
                        Color.parseColor("#111827")
                    )

                    tv.textSize = 15f

                    tv.setPadding(
                        32,
                        24,
                        32,
                        24
                    )

                    view.setBackgroundColor(
                        Color.WHITE
                    )

                    return view
                }
            }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerTahun.adapter = adapter

        var defaultIndex =
            listTahun.indexOf(tahunIntent)

        if (defaultIndex == -1) {

            defaultIndex =
                listTahun.indexOf(currentYear)

            if (defaultIndex == -1) {
                defaultIndex = 0
            }
        }

        binding.spinnerTahun.setSelection(
            defaultIndex
        )

        binding.tvHeaderTahun.text =
            listTahun[defaultIndex].toString()

        binding.spinnerTahun.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val selectedTahun =
                        listTahun[position]

                    binding.tvHeaderTahun.text =
                        selectedTahun.toString()

                    loadDataRoute(
                        selectedTahun
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    // ================================================================
    // ROUTE LOAD DATA
    // ================================================================

    private fun loadDataRoute(tahun: Int) {

        if (idData.isEmpty()) {

            showError(
                "ID Data tidak valid."
            )

            return
        }

        showLoading(true)

        binding.tvStatus.visibility =
            View.GONE

        binding.dataDetailContainer.removeAllViews()

        when (typeData.uppercase()) {

            "VARIABLE" -> {
                loadVariableDetail(tahun)
            }

            "SIMDASI" -> {
                loadSimdasiDetail(tahun)
            }

            "STATIC" -> {
                loadStaticDetail()
            }

            else -> {

                showLoading(false)

                showError(
                    "Tipe data ($typeData) tidak didukung."
                )
            }
        }
    }

    // ================================================================
    // VARIABLE DETAIL
    // ================================================================

    private fun loadVariableDetail(tahun: Int) {

        repository.getVariableDetail(
            domain = DOMAIN,
            varId = idData,
            tahun = tahun,
            apiKey = API_KEY
        ) { response, error ->

            if (error != null) {

                runOnUiThread {

                    showLoading(false)

                    showError(
                        "Gagal mengambil data: ${error.message}"
                    )
                }

                return@getVariableDetail
            }

            if (
                response != null &&
                response.status == "OK" &&
                response.dataContent != null
            ) {

                processVariableDataInBackground(
                    response
                )

            } else {

                runOnUiThread {

                    showLoading(false)

                    showError(
                        "Data tidak tersedia untuk tahun $tahun."
                    )
                }
            }
        }
    }

    // ================================================================
    // PROSES VARIABLE DI BACKGROUND
    // ================================================================

    private fun processVariableDataInBackground(
        response: com.example.bpskota.bps.model.VariableDetailResponse
    ) {

        Thread {

            try {

                val varInfo =
                    response.variable?.firstOrNull()

                val turvarInfo =
                    response.turvar?.firstOrNull()

                val tahunInfo =
                    response.tahun?.firstOrNull()

                val vervarList =
                    response.wilayah ?: emptyList()

                val turtahunList =
                    response.turTahun ?: emptyList()

                val dataContent =
                    response.dataContent
                        ?: emptyMap()

                // ----------------------------------------------------
                // VALIDASI
                // ----------------------------------------------------

                if (
                    vervarList.isEmpty() ||
                    dataContent.isEmpty()
                ) {

                    runOnUiThread {

                        showLoading(false)

                        showError(
                            "Data kosong pada tahun tersebut."
                        )
                    }

                    return@Thread
                }

                val cardList =
                    mutableListOf<CardData>()

                // ----------------------------------------------------
                // LOOP WILAYAH
                // ----------------------------------------------------

                for (vervar in vervarList) {

                    val dataBaris =
                        mutableListOf<Pair<String, String>>()

                    // ------------------------------------------------
                    // PENTING:
                    // Jangan gunakan vervar.toString()
                    //
                    // Karena akan menghasilkan:
                    //
                    // VariableWilayah(
                    //     value=3574,
                    //     label=Kota Probolinggo
                    // )
                    //
                    // Kita hanya ingin:
                    //
                    // Kota Probolinggo
                    // ------------------------------------------------

                    val namaWilayah =
                        vervar.label
                            ?.trim()
                            ?.ifBlank {
                                "Wilayah"
                            }
                            ?: "Wilayah"

                    // ------------------------------------------------
                    // LOOP BULAN / PERIODE
                    // ------------------------------------------------

                    for (turtahun in turtahunList) {

                        val key =
                            "${vervar.value}" +
                                    "${varInfo?.value}" +
                                    "${turvarInfo?.value}" +
                                    "${tahunInfo?.value}" +
                                    "${turtahun.value}"

                        val value =
                            dataContent[key]

                        val formattedValue =
                            value?.toString()
                                ?.trim()
                                ?.ifBlank {
                                    "-"
                                }
                                ?: "-"

                        val labelPeriode =
                            turtahun.label
                                ?.trim()
                                ?.ifBlank {
                                    "-"
                                }
                                ?: "-"

                        dataBaris.add(
                            Pair(
                                labelPeriode,
                                formattedValue
                            )
                        )
                    }

                    if (dataBaris.isNotEmpty()) {

                        cardList.add(
                            CardData(
                                wilayah = namaWilayah,
                                dataBaris = dataBaris
                            )
                        )
                    }
                }

                // ----------------------------------------------------
                // KEMBALI KE UI THREAD
                // ----------------------------------------------------

                runOnUiThread {

                    if (cardList.isEmpty()) {

                        showLoading(false)

                        showError(
                            "Tidak ada data yang dapat ditampilkan."
                        )

                    } else {

                        renderVariableCards(
                            cardList
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error proses variable",
                    e
                )

                runOnUiThread {

                    showLoading(false)

                    showError(
                        "Gagal memproses data: ${e.message}"
                    )
                }
            }

        }.start()
    }

    // ================================================================
    // RENDER VARIABLE CARD
    // ================================================================

    private fun renderVariableCards(
        cards: List<CardData>
    ) {

        binding.dataDetailContainer.removeAllViews()

        showLoading(false)

        val chunkSize = 20

        var currentIndex = 0

        fun renderNextChunk() {

            val endIndex =
                minOf(
                    currentIndex + chunkSize,
                    cards.size
                )

            for (
            i in currentIndex until endIndex
            ) {

                val cardData =
                    cards[i]

                tampilkanCardDataBaris(
                    wilayah = cardData.wilayah,
                    dataBaris = cardData.dataBaris
                )
            }

            currentIndex = endIndex

            if (currentIndex < cards.size) {

                binding.dataDetailContainer.post {
                    renderNextChunk()
                }
            }
        }

        renderNextChunk()
    }

    // ================================================================
    // SIMDASI DETAIL
    // ================================================================

    private fun loadSimdasiDetail(tahun: Int) {

        repository.getSimdasiDetail(
            wilayah = WILAYAH,
            tahun = tahun,
            idTabel = idData,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {
                showLoading(false)
            }

            if (error != null) {

                runOnUiThread {

                    showError(
                        "Gagal mengambil data SIMDASI: ${error.message}"
                    )
                }

                return@getSimdasiDetail
            }

            if (
                response != null &&
                response.status == "OK"
            ) {

                runOnUiThread {

                    val tvSimdasi =
                        TextView(this).apply {

                            text =
                                "Data SIMDASI berhasil dimuat.\n" +
                                        "Format data SIMDASI dapat disesuaikan di sini."

                            setTextColor(
                                Color.BLACK
                            )

                            textSize = 14f
                        }

                    binding.dataDetailContainer
                        .addView(tvSimdasi)
                }

            } else {

                runOnUiThread {

                    showError(
                        "Data SIMDASI tidak tersedia untuk tahun $tahun."
                    )
                }
            }
        }
    }

    // ================================================================
    // STATIC TABLE
    // ================================================================

    private fun loadStaticDetail() {

        binding.cardFilterTahun.visibility =
            View.GONE

        repository.getStaticTableDetail(
            domain = DOMAIN,
            id = idData,
            apiKey = API_KEY
        ) { response, error ->

            if (error != null) {

                runOnUiThread {

                    showLoading(false)

                    showError(
                        "Gagal mengambil Static Table: ${error.message}"
                    )
                }

                return@getStaticTableDetail
            }

            if (
                response == null ||
                response.status?.uppercase() != "OK" ||
                response.data?.table.isNullOrBlank()
            ) {

                runOnUiThread {

                    showLoading(false)

                    showError(
                        "Data Static Table tidak tersedia."
                    )
                }

                return@getStaticTableDetail
            }

            runOnUiThread {

                if (
                    !response.data?.title
                        .isNullOrBlank()
                ) {

                    binding.tvJudul.text =
                        response.data?.title
                }
            }

            processStaticTableInBackground(
                response.data?.table!!
            )
        }
    }

    // ================================================================
    // PROSES STATIC TABLE
    // ================================================================

    private fun processStaticTableInBackground(
        html: String
    ) {

        Thread {

            try {

                val decodedHtml =
                    Parser.unescapeEntities(
                        html,
                        true
                    )

                val document =
                    Jsoup.parse(decodedHtml)

                val table =
                    document
                        .select("table")
                        .firstOrNull()

                if (table == null) {

                    runOnUiThread {

                        showLoading(false)

                        showError(
                            "Tabel Static tidak ditemukan."
                        )
                    }

                    return@Thread
                }

                val rows =
                    table.select("tr")

                if (rows.isEmpty()) {

                    runOnUiThread {

                        showLoading(false)

                        showError(
                            "Data Static Table kosong."
                        )
                    }

                    return@Thread
                }

                // ====================================================
                // CARI JUMLAH KOLOM
                // ====================================================

                var maxCols = 0

                for (row in rows) {

                    var jumlahKolom = 0

                    for (
                    cell in row.select("th, td")
                    ) {

                        jumlahKolom +=
                            cell.attr(
                                "colspan"
                            )
                                .toIntOrNull()
                                ?: 1
                    }

                    maxCols =
                        maxOf(
                            maxCols,
                            jumlahKolom
                        )
                }

                if (maxCols == 0) {

                    runOnUiThread {

                        showLoading(false)

                        showError(
                            "Kolom tabel Static tidak ditemukan."
                        )
                    }

                    return@Thread
                }

                // ====================================================
                // BENTUK GRID
                // ====================================================

                val grid =
                    Array(rows.size) {
                        Array(maxCols) {
                            ""
                        }
                    }

                for (r in rows.indices) {

                    val cells =
                        rows[r].select(
                            "th, td"
                        )

                    var c = 0

                    for (cell in cells) {

                        while (
                            c < maxCols &&
                            grid[r][c].isNotBlank()
                        ) {
                            c++
                        }

                        if (c >= maxCols) {
                            break
                        }

                        val text =
                            cell.text()
                                .trim()
                                .replace(
                                    Regex("\\s+"),
                                    " "
                                )

                        val rowspan =
                            cell.attr("rowspan")
                                .toIntOrNull()
                                ?: 1

                        val colspan =
                            cell.attr("colspan")
                                .toIntOrNull()
                                ?: 1

                        for (
                        rr in 0 until rowspan
                        ) {

                            for (
                            cc in 0 until colspan
                            ) {

                                if (
                                    r + rr < rows.size &&
                                    c + cc < maxCols
                                ) {

                                    grid[
                                            r + rr
                                    ][
                                            c + cc
                                    ] = text
                                }
                            }
                        }

                        c += colspan
                    }
                }

                // ====================================================
                // CARI BARIS DATA
                // ====================================================

                var dataStartRow = -1

                var kolomNama = 0

                for (r in grid.indices) {

                    var numericCount = 0

                    var firstTextCol = -1

                    for (c in 0 until maxCols) {

                        val cell =
                            grid[r][c].trim()

                        if (cell.isBlank()) {
                            continue
                        }

                        val isYear =
                            cell.matches(
                                Regex(
                                    "^(19|20)\\d{2}$"
                                )
                            )

                        val isNumber =
                            cell.matches(
                                Regex(
                                    "^-?[0-9.,]+$"
                                )
                            ) &&
                                    cell.any {
                                        it.isDigit()
                                    } &&
                                    !isYear

                        if (
                            isNumber ||
                            cell == "-"
                        ) {

                            numericCount++

                        } else if (
                            firstTextCol == -1 &&
                            !isYear
                        ) {

                            firstTextCol = c
                        }
                    }

                    if (
                        numericCount > 0 &&
                        firstTextCol != -1
                    ) {

                        dataStartRow = r

                        kolomNama =
                            firstTextCol

                        break
                    }
                }

                if (dataStartRow == -1) {

                    dataStartRow = 1

                    kolomNama = 0
                }

                // ====================================================
                // HEADER
                // ====================================================

                var headerStartRow = 0

                if (
                    grid.isNotEmpty() &&
                    maxCols > 1
                ) {

                    val firstCell =
                        grid[0][0]

                    var isBigTitle = true

                    for (
                    c in 1 until maxCols
                    ) {

                        if (
                            grid[0][c] != firstCell
                        ) {

                            isBigTitle = false

                            break
                        }
                    }

                    if (isBigTitle) {
                        headerStartRow = 1
                    }
                }

                val headers =
                    Array(maxCols) {
                        ""
                    }

                for (c in 0 until maxCols) {

                    val parts =
                        mutableListOf<String>()

                    for (
                    r in headerStartRow until dataStartRow
                    ) {

                        val cell =
                            grid[r][c].trim()

                        if (
                            cell.isNotBlank() &&
                            !parts.contains(cell)
                        ) {

                            if (
                                !cell.equals(
                                    "Kecamatan",
                                    ignoreCase = true
                                ) &&
                                !cell.equals(
                                    "Kabupaten/Kota",
                                    ignoreCase = true
                                )
                            ) {

                                parts.add(cell)
                            }
                        }
                    }

                    headers[c] =
                        parts.joinToString(" ")
                            .ifBlank {
                                "Data ${c + 1}"
                            }
                }

                // ====================================================
                // BENTUK CARD DATA
                // ====================================================

                val parsedCardList =
                    mutableListOf<CardData>()

                for (
                r in dataStartRow until grid.size
                ) {

                    val nama =
                        grid[r][kolomNama]
                            .trim()

                    if (
                        nama.isBlank() ||
                        nama.lowercase()
                            .startsWith("catatan") ||
                        nama.lowercase()
                            .startsWith("sumber")
                    ) {
                        continue
                    }

                    val rowData =
                        mutableListOf<Pair<String, String>>()

                    for (
                    c in 0 until maxCols
                    ) {

                        if (c == kolomNama) {
                            continue
                        }

                        val nilai =
                            grid[r][c]
                                .trim()
                                .ifBlank {
                                    "-"
                                }

                        rowData.add(
                            Pair(
                                headers[c],
                                nilai
                            )
                        )
                    }

                    if (rowData.isNotEmpty()) {

                        parsedCardList.add(
                            CardData(
                                wilayah = nama,
                                dataBaris = rowData
                            )
                        )
                    }
                }

                // ====================================================
                // RENDER
                // ====================================================

                runOnUiThread {

                    if (
                        parsedCardList.isEmpty()
                    ) {

                        showLoading(false)

                        showError(
                            "Tidak ada data Static Table yang dapat ditampilkan."
                        )

                    } else {

                        renderStaticCardsIncrementally(
                            parsedCardList
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error proses static table",
                    e
                )

                runOnUiThread {

                    showLoading(false)

                    showError(
                        "Gagal memproses tabel: ${e.message}"
                    )
                }
            }

        }.start()
    }

    // ================================================================
    // RENDER STATIC CARD
    // ================================================================

    private fun renderStaticCardsIncrementally(
        cards: List<CardData>
    ) {

        binding.dataDetailContainer.removeAllViews()

        showLoading(false)

        val chunkSize = 20

        var currentIndex = 0

        fun renderNextChunk() {

            val endIndex =
                minOf(
                    currentIndex + chunkSize,
                    cards.size
                )

            for (
            i in currentIndex until endIndex
            ) {

                val cardData =
                    cards[i]

                tampilkanCardDataBaris(
                    wilayah = cardData.wilayah,
                    dataBaris = cardData.dataBaris
                )
            }

            currentIndex = endIndex

            if (currentIndex < cards.size) {

                binding.dataDetailContainer.post {
                    renderNextChunk()
                }
            }
        }

        renderNextChunk()
    }

    // ================================================================
    // CARD DATA
    // ================================================================
    //
    // Bentuk akhirnya:
    //
    // ┌────────────────────────────────┐
    // │ Kota Probolinggo               │
    // │                                │
    // │ Januari                 110.4  │
    // │ Februari              111.64   │
    // │ Maret                 112.41   │
    // │ April                 111.68   │
    // │ Mei                   111.71   │
    // └────────────────────────────────┘
    //
    // ================================================================

    private fun tampilkanCardDataBaris(
        wilayah: String,
        dataBaris: List<Pair<String, String>>
    ) {

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.card_statistik,
                    binding.dataDetailContainer,
                    false
                )

        val tvNamaWilayah =
            card.findViewById<TextView>(
                R.id.tvNamaWilayah
            )

        val containerVariable =
            card.findViewById<LinearLayout>(
                R.id.containerVariable
            )

        // ------------------------------------------------------------
        // NAMA WILAYAH
        // ------------------------------------------------------------

        tvNamaWilayah.text =
            wilayah.ifBlank {
                "Wilayah"
            }

        // ------------------------------------------------------------
        // DATA BARIS
        // ------------------------------------------------------------

        for (
        (label, nilai) in dataBaris
        ) {

            val row =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        32,
                        6,
                        16,
                        6
                    )
                }

            // --------------------------------------------------------
            // LABEL
            // --------------------------------------------------------

            val tvLabel =
                TextView(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )

                    text =
                        label.ifBlank {
                            "-"
                        }

                    textSize = 14f

                    setTextColor(
                        Color.parseColor(
                            "#4B5563"
                        )
                    )
                }

            // --------------------------------------------------------
            // NILAI
            // --------------------------------------------------------

            val tvData =
                TextView(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {

                            setMargins(
                                16,
                                0,
                                16,
                                0
                            )
                        }

                    text =
                        nilai.ifBlank {
                            "-"
                        }

                    textSize = 14f

                    gravity =
                        Gravity.END

                    setTextColor(
                        Color.parseColor(
                            "#111827"
                        )
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }

            row.addView(tvLabel)
            row.addView(tvData)

            containerVariable.addView(row)
        }

        binding.dataDetailContainer
            .addView(card)
    }

    // ================================================================
    // LOADING
    // ================================================================

    private fun showLoading(
        isLoading: Boolean
    ) {

        if (isLoading) {

            binding.progressLoading.visibility =
                View.VISIBLE

            binding.scrollDetail.visibility =
                View.GONE

        } else {

            binding.progressLoading.visibility =
                View.GONE

            binding.scrollDetail.visibility =
                View.VISIBLE
        }
    }

    // ================================================================
    // ERROR
    // ================================================================

    private fun showError(
        message: String
    ) {

        binding.tvStatus.text =
            message

        binding.tvStatus.visibility =
            View.VISIBLE

        binding.dataDetailContainer
            .removeAllViews()
    }
}