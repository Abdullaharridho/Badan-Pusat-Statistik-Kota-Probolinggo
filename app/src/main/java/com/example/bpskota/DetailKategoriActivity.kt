package com.example.bpskota

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.example.bpskota.bps.repository.BpsAllDataRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.databinding.ActivityDetailKategoriBinding
import com.example.bpskota.tracking.ActivityTracker
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class DetailKategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailKategoriBinding

    private val repository = BpsAllDataRepository()

    private lateinit var activityTracker: ActivityTracker

    companion object {
        private const val TAG = "DetailKategoriActivity"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val DOMAIN = "3574"
        private const val WILAYAH = "3574000"

        private const val CHANNEL_ID = "bps_download_channel"
        private const val NOTIFICATION_ID = 2001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    private var idData = ""
    private var typeData = ""
    private var judulData = ""
    private var kategoriData = ""
    private var sumberData = ""

    private var tahunTerpilih = Calendar.getInstance()
        .get(Calendar.YEAR)

    /**
     * Data yang digunakan untuk menampilkan card
     * sekaligus membuat PDF.
     */
    private data class CardData(
        val wilayah: String,
        val dataBaris: List<Pair<String, String>>
    )

    /**
     * Menyimpan seluruh data hasil parsing.
     * Tidak perlu request API ulang saat download.
     */
    private val dataPdfCards = mutableListOf<CardData>()

    private var dataPdfSiap = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        judulData = intent.getStringExtra("JUDUL") ?: "Detail Data"
        kategoriData = intent.getStringExtra("KATEGORI") ?: "Kategori"
        sumberData = intent.getStringExtra("SUMBER") ?: "BPS Kota Probolinggo"
        idData = intent.getStringExtra("ID") ?: ""
        typeData = intent.getStringExtra("TYPE") ?: ""



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
        createNotificationChannel()
        requestNotificationPermission()

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
            tampilkanDialogDownload()
        }
    }

    // ================================================================
    // SPINNER TAHUN
    // ================================================================

    private fun setupSpinnerTahun() {

        val currentYear = Calendar.getInstance()
            .get(Calendar.YEAR)

        val tahunIntent = intent.getIntExtra(
            "TAHUN",
            currentYear
        )

        tahunTerpilih = tahunIntent

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

        val adapter = object : ArrayAdapter<Int>(
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

                val tv = view.findViewById<TextView>(
                    android.R.id.text1
                )

                tv.text = listTahun[position].toString()
                tv.setTextColor(Color.parseColor("#111827"))
                tv.textSize = 15f
                tv.setTypeface(null, Typeface.BOLD)
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

                val tv = view.findViewById<TextView>(
                    android.R.id.text1
                )

                tv.text = listTahun[position].toString()
                tv.setTextColor(Color.parseColor("#111827"))
                tv.textSize = 15f
                tv.setPadding(32, 24, 32, 24)

                view.setBackgroundColor(Color.WHITE)

                return view
            }
        }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerTahun.adapter = adapter

        var defaultIndex = listTahun.indexOf(tahunIntent)

        if (defaultIndex == -1) {
            defaultIndex = listTahun.indexOf(currentYear)

            if (defaultIndex == -1) {
                defaultIndex = 0
            }
        }

        binding.spinnerTahun.setSelection(defaultIndex)

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

                    val selectedTahun = listTahun[position]

                    binding.tvHeaderTahun.text =
                        selectedTahun.toString()

                    if (selectedTahun != tahunTerpilih ||
                        !dataPdfSiap
                    ) {
                        loadDataRoute(selectedTahun)
                    }
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
            showError("ID Data tidak valid.")
            return
        }

        tahunTerpilih = tahun

        // Data lama tidak boleh ikut masuk ke PDF tahun baru.
        dataPdfCards.clear()
        dataPdfSiap = false

        showLoading(true)

        binding.tvStatus.visibility = View.GONE
        binding.dataDetailContainer.removeAllViews()

        when (typeData.uppercase()) {

            "VARIABLE" -> loadVariableDetail(tahun)

            "SIMDASI" -> loadSimdasiDetail(tahun)

            "STATIC" -> loadStaticDetail()

            else -> {
                showLoading(false)
                showError("Tipe data ($typeData) tidak didukung.")
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
                    showError("Gagal mengambil data: ${error.message}")
                }
                return@getVariableDetail
            }

            if (
                response != null &&
                response.status == "OK" &&
                response.dataContent != null
            ) {
                processVariableDataInBackground(response)
            } else {
                runOnUiThread {
                    showLoading(false)
                    showError("Data tidak tersedia untuk tahun $tahun.")
                }
            }
        }
    }

    // ================================================================
    // PROSES VARIABLE
    // ================================================================

    private fun processVariableDataInBackground(
        response: com.example.bpskota.bps.model.VariableDetailResponse
    ) {

        Thread {

            try {

                val varInfo = response.variable?.firstOrNull()
                val turvarInfo = response.turvar?.firstOrNull()
                val tahunInfo = response.tahun?.firstOrNull()

                val vervarList = response.wilayah ?: emptyList()
                val turtahunList = response.turTahun ?: emptyList()
                val dataContent = response.dataContent ?: emptyMap()

                if (vervarList.isEmpty() || dataContent.isEmpty()) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Data kosong pada tahun tersebut.")
                    }
                    return@Thread
                }

                val cardList = mutableListOf<CardData>()

                for (vervar in vervarList) {

                    val dataBaris = mutableListOf<Pair<String, String>>()

                    val namaWilayah = vervar.label
                        ?.trim()
                        ?.ifBlank { "Wilayah" }
                        ?: "Wilayah"

                    for (turtahun in turtahunList) {

                        val key =
                            "${vervar.value}" +
                                    "${varInfo?.value}" +
                                    "${turvarInfo?.value}" +
                                    "${tahunInfo?.value}" +
                                    "${turtahun.value}"

                        val value = dataContent[key]

                        val formattedValue = value?.toString()
                            ?.trim()
                            ?.ifBlank { "-" }
                            ?: "-"

                        val labelPeriode = turtahun.label
                            ?.trim()
                            ?.ifBlank { "-" }
                            ?: "-"

                        dataBaris.add(
                            Pair(labelPeriode, formattedValue)
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

                runOnUiThread {

                    if (cardList.isEmpty()) {
                        showLoading(false)
                        showError("Tidak ada data yang dapat ditampilkan.")
                    } else {
                        dataPdfCards.clear()
                        dataPdfCards.addAll(cardList)
                        dataPdfSiap = true

                        renderVariableCards(cardList)
                    }
                }

            } catch (e: Exception) {


            }

        }.start()
    }

    // ================================================================
    // RENDER VARIABLE CARD
    // ================================================================

    private fun renderVariableCards(cards: List<CardData>) {

        binding.dataDetailContainer.removeAllViews()
        showLoading(false)

        val chunkSize = 20
        var currentIndex = 0

        fun renderNextChunk() {

            val endIndex = minOf(
                currentIndex + chunkSize,
                cards.size
            )

            for (i in currentIndex until endIndex) {

                val cardData = cards[i]

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

            if (error != null) {
                runOnUiThread {
                    showLoading(false)
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

                    showLoading(false)

                    val tvSimdasi = TextView(this).apply {
                        text =
                            "Data SIMDASI berhasil dimuat.\n" +
                                    "Format data SIMDASI dapat disesuaikan di sini."

                        setTextColor(Color.BLACK)
                        textSize = 14f
                    }

                    binding.dataDetailContainer.addView(tvSimdasi)

                    // Belum ada data tabel SIMDASI yang diproses.
                    dataPdfSiap = false
                }

            } else {

                runOnUiThread {
                    showLoading(false)
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

        binding.cardFilterTahun.visibility = View.GONE

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
                    showError("Data Static Table tidak tersedia.")
                }
                return@getStaticTableDetail
            }

            runOnUiThread {
                if (!response.data?.title.isNullOrBlank()) {
                    binding.tvJudul.text = response.data?.title
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

    private fun processStaticTableInBackground(html: String) {

        Thread {

            try {

                val decodedHtml = Parser.unescapeEntities(html, true)
                val document = Jsoup.parse(decodedHtml)
                val table = document.select("table").firstOrNull()

                if (table == null) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Tabel Static tidak ditemukan.")
                    }
                    return@Thread
                }

                val rows = table.select("tr")

                if (rows.isEmpty()) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Data Static Table kosong.")
                    }
                    return@Thread
                }

                var maxCols = 0

                for (row in rows) {

                    var jumlahKolom = 0

                    for (cell in row.select("th, td")) {
                        jumlahKolom +=
                            cell.attr("colspan").toIntOrNull() ?: 1
                    }

                    maxCols = maxOf(maxCols, jumlahKolom)
                }

                if (maxCols == 0) {
                    runOnUiThread {
                        showLoading(false)
                        showError("Kolom tabel Static tidak ditemukan.")
                    }
                    return@Thread
                }

                val grid = Array(rows.size) {
                    Array(maxCols) { "" }
                }

                for (r in rows.indices) {

                    val cells = rows[r].select("th, td")
                    var c = 0

                    for (cell in cells) {

                        while (
                            c < maxCols &&
                            grid[r][c].isNotBlank()
                        ) {
                            c++
                        }

                        if (c >= maxCols) break

                        val text = cell.text()
                            .trim()
                            .replace(Regex("\\s+"), " ")

                        val rowspan = cell.attr("rowspan")
                            .toIntOrNull() ?: 1

                        val colspan = cell.attr("colspan")
                            .toIntOrNull() ?: 1

                        for (rr in 0 until rowspan) {
                            for (cc in 0 until colspan) {

                                if (
                                    r + rr < rows.size &&
                                    c + cc < maxCols
                                ) {
                                    grid[r + rr][c + cc] = text
                                }
                            }
                        }

                        c += colspan
                    }
                }

                // Cari baris pertama yang berisi nama dan nilai.
                var dataStartRow = -1
                var kolomNama = 0

                for (r in grid.indices) {

                    var numericCount = 0
                    var firstTextCol = -1

                    for (c in 0 until maxCols) {

                        val cell = grid[r][c].trim()

                        if (cell.isBlank()) continue

                        val isYear = cell.matches(
                            Regex("^(19|20)\\d{2}$")
                        )

                        val isNumber =
                            cell.matches(Regex("^-?[0-9.,]+$")) &&
                                    cell.any { it.isDigit() } &&
                                    !isYear

                        if (isNumber || cell == "-") {
                            numericCount++
                        } else if (firstTextCol == -1 && !isYear) {
                            firstTextCol = c
                        }
                    }

                    if (numericCount > 0 && firstTextCol != -1) {
                        dataStartRow = r
                        kolomNama = firstTextCol
                        break
                    }
                }

                if (dataStartRow == -1) {
                    dataStartRow = 1
                    kolomNama = 0
                }

                var headerStartRow = 0

                if (grid.isNotEmpty() && maxCols > 1) {

                    val firstCell = grid[0][0]
                    var isBigTitle = true

                    for (c in 1 until maxCols) {
                        if (grid[0][c] != firstCell) {
                            isBigTitle = false
                            break
                        }
                    }

                    if (isBigTitle) headerStartRow = 1
                }

                val headers = Array(maxCols) { "" }

                for (c in 0 until maxCols) {

                    val parts = mutableListOf<String>()

                    for (r in headerStartRow until dataStartRow) {

                        val cell = grid[r][c].trim()

                        if (cell.isBlank() || parts.contains(cell)) {
                            continue
                        }

                        if (
                            !cell.equals("Kecamatan", true) &&
                            !cell.equals("Kabupaten/Kota", true)
                        ) {
                            parts.add(cell)
                        }
                    }

                    headers[c] = parts.joinToString(" ")
                        .ifBlank { "Data ${c + 1}" }
                }

                val parsedCardList = mutableListOf<CardData>()

                for (r in dataStartRow until grid.size) {

                    val nama = grid[r][kolomNama].trim()

                    if (
                        nama.isBlank() ||
                        nama.lowercase().startsWith("catatan") ||
                        nama.lowercase().startsWith("sumber")
                    ) {
                        continue
                    }

                    val rowData = mutableListOf<Pair<String, String>>()

                    for (c in 0 until maxCols) {

                        if (c == kolomNama) continue

                        val nilai = grid[r][c]
                            .trim()
                            .ifBlank { "-" }

                        rowData.add(
                            Pair(headers[c], nilai)
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

                runOnUiThread {

                    if (parsedCardList.isEmpty()) {
                        showLoading(false)
                        showError(
                            "Tidak ada data Static Table yang dapat ditampilkan."
                        )
                    } else {

                        dataPdfCards.clear()
                        dataPdfCards.addAll(parsedCardList)
                        dataPdfSiap = true

                        renderStaticCardsIncrementally(
                            parsedCardList
                        )
                    }
                }

            } catch (e: Exception) {


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

            val endIndex = minOf(
                currentIndex + chunkSize,
                cards.size
            )

            for (i in currentIndex until endIndex) {

                val cardData = cards[i]

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

    private fun tampilkanCardDataBaris(
        wilayah: String,
        dataBaris: List<Pair<String, String>>
    ) {

        val card = LayoutInflater
            .from(this)
            .inflate(
                R.layout.card_statistik,
                binding.dataDetailContainer,
                false
            )

        val tvNamaWilayah = card.findViewById<TextView>(
            R.id.tvNamaWilayah
        )

        val containerVariable = card.findViewById<LinearLayout>(
            R.id.containerVariable
        )

        tvNamaWilayah.text = wilayah.ifBlank { "Wilayah" }

        for ((label, nilai) in dataBaris) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                gravity = Gravity.CENTER_VERTICAL
                setPadding(32, 6, 16, 6)
            }

            val tvLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

                text = label.ifBlank { "-" }
                textSize = 14f
                setTextColor(Color.parseColor("#4B5563"))
            }

            val tvData = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 16, 0)
                }

                text = nilai.ifBlank { "-" }
                textSize = 14f
                gravity = Gravity.END
                setTextColor(Color.parseColor("#111827"))
                setTypeface(null, Typeface.BOLD)
            }

            row.addView(tvLabel)
            row.addView(tvData)

            containerVariable.addView(row)
        }

        binding.dataDetailContainer.addView(card)
    }

    // ================================================================
    // DIALOG DOWNLOAD
    // ================================================================

    private fun tampilkanDialogDownload() {

        if (!dataPdfSiap || dataPdfCards.isEmpty()) {

            val pesan = if (
                typeData.equals("SIMDASI", ignoreCase = true)
            ) {
                "Data SIMDASI belum tersedia dalam format tabel untuk diunduh."
            } else {
                "Data belum selesai dimuat atau tidak tersedia."
            }

            Toast.makeText(
                this,
                pesan,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Download PDF")
            .setMessage(
                "Apakah Anda ingin mengunduh data statistik ini dalam bentuk PDF?"
            )
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Ya") { _, _ ->
                downloadPdf()
            }
            .show()
    }

    // ================================================================
    // DOWNLOAD PDF
    // ================================================================

    private fun downloadPdf() {

        if (!dataPdfSiap || dataPdfCards.isEmpty()) {
            Toast.makeText(
                this,
                "Data belum tersedia untuk diunduh.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        var pdfDocument: PdfDocument? = null

        try {

            val namaJudul = getPlainText(
                binding.tvJudul.text.toString()
                    .ifBlank { judulData }
            )

            val namaFileDasar = namaJudul
                .replace(Regex("[^a-zA-Z0-9\\s]"), "")
                .replace(Regex("\\s+"), "_")
                .take(60)
                .ifBlank { "data_bps" }

            val namaFile = if (
                typeData.equals("STATIC", ignoreCase = true)
            ) {
                "${namaFileDasar}.pdf"
            } else {
                "${namaFileDasar}_${tahunTerpilih}.pdf"
            }

            pdfDocument = PdfDocument()

            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val contentWidth = pageWidth - margin * 2

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(17, 24, 39)
                textSize = 17f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            }

            val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(249, 115, 22)
                textSize = 12f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            }

            val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(75, 85, 99)
                textSize = 10f
            }

            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(17, 24, 39)
                textSize = 10f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            }

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(209, 213, 219)
                strokeWidth = 0.8f
            }

            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 0f

            fun newPage() {
                page?.let {
                    pdfDocument?.finishPage(it)
                }

                pageNumber++

                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()

                page = pdfDocument?.startPage(pageInfo)
                canvas = page?.canvas
                y = 45f

                // Header setiap halaman.
                canvas?.drawText(
                    "DATA STATISTIK BPS KOTA PROBOLINGGO",
                    margin,
                    y,
                    headingPaint
                )

                y += 20f

                canvas?.drawLine(
                    margin,
                    y,
                    pageWidth - margin,
                    y,
                    linePaint
                )

                y += 20f
            }
            fun ensureSpace(height: Float) {
                if (y + height > pageHeight - 45f) {
                    newPage()
                }
            }

            fun drawWrappedText(
                text: String,
                x: Float,
                paint: Paint,
                maxWidth: Float,
                lineHeight: Float
            ) {

                val cleanText = text
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifBlank { "-" }

                val words = cleanText.split(" ")
                var line = ""

                for (word in words) {

                    val candidate = if (line.isEmpty()) {
                        word
                    } else {
                        "$line $word"
                    }

                    if (
                        paint.measureText(candidate) > maxWidth &&
                        line.isNotEmpty()
                    ) {
                        ensureSpace(lineHeight)
                        canvas!!.drawText(line, x, y, paint)
                        y += lineHeight
                        line = word
                    } else {
                        line = candidate
                    }
                }

                if (line.isNotEmpty()) {
                    ensureSpace(lineHeight)
                    canvas!!.drawText(line, x, y, paint)
                    y += lineHeight
                }
            }

            newPage()

            // --------------------------------------------------------
            // INFORMASI DOKUMEN
            // --------------------------------------------------------

            drawWrappedText(
                namaJudul,
                margin,
                titlePaint,
                contentWidth,
                23f
            )

            y += 3f

            drawWrappedText(
                "Kategori: ${getPlainText(kategoriData)}",
                margin,
                normalPaint,
                contentWidth,
                14f
            )

            drawWrappedText(
                "Sumber: ${getPlainText(sumberData)}",
                margin,
                normalPaint,
                contentWidth,
                14f
            )

            if (!typeData.equals("STATIC", ignoreCase = true)) {
                drawWrappedText(
                    "Tahun: $tahunTerpilih",
                    margin,
                    normalPaint,
                    contentWidth,
                    14f
                )
            }

            drawWrappedText(
                "Jenis data: $typeData",
                margin,
                normalPaint,
                contentWidth,
                14f
            )

            y += 8f

            canvas!!.drawLine(
                margin,
                y,
                pageWidth - margin,
                y,
                linePaint
            )

            y += 20f

            // --------------------------------------------------------
            // DATA PER WILAYAH
            // --------------------------------------------------------

            dataPdfCards.forEachIndexed { index, cardData ->

                ensureSpace(35f)

                drawWrappedText(
                    "${index + 1}. ${getPlainText(cardData.wilayah)}",
                    margin,
                    headingPaint,
                    contentWidth,
                    17f
                )

                y += 3f

                // Header kolom.
                ensureSpace(20f)

                canvas!!.drawText(
                    "Indikator / Periode",
                    margin + 8f,
                    y,
                    boldPaint
                )

                val valueHeader = "Nilai"

                canvas!!.drawText(
                    valueHeader,
                    pageWidth - margin -
                            boldPaint.measureText(valueHeader),
                    y,
                    boldPaint
                )

                y += 8f

                canvas!!.drawLine(
                    margin,
                    y,
                    pageWidth - margin,
                    y,
                    linePaint
                )

                y += 15f

                cardData.dataBaris.forEach { (label, nilai) ->

                    val labelText = getPlainText(label)
                        .ifBlank { "-" }

                    val valueText = getPlainText(nilai)
                        .ifBlank { "-" }

                    val labelWidth = contentWidth * 0.68f
                    val valueWidth = contentWidth * 0.28f

                    val labelLines = splitTextToLines(
                        labelText,
                        normalPaint,
                        labelWidth
                    )

                    val valueLines = splitTextToLines(
                        valueText,
                        boldPaint,
                        valueWidth
                    )

                    val totalLines = maxOf(
                        labelLines.size,
                        valueLines.size
                    )

                    val rowHeight = totalLines * 14f + 5f

                    ensureSpace(rowHeight)

                    for (lineIndex in 0 until totalLines) {

                        if (lineIndex < labelLines.size) {
                            canvas!!.drawText(
                                labelLines[lineIndex],
                                margin + 8f,
                                y,
                                normalPaint
                            )
                        }

                        if (lineIndex < valueLines.size) {
                            val valueLine = valueLines[lineIndex]

                            canvas!!.drawText(
                                valueLine,
                                pageWidth - margin - 8f -
                                        boldPaint.measureText(valueLine),
                                y,
                                boldPaint
                            )
                        }

                        y += 14f
                    }

                    y += 5f

                    canvas!!.drawLine(
                        margin + 8f,
                        y,
                        pageWidth - margin - 8f,
                        y,
                        linePaint
                    )

                    y += 12f
                }

                y += 10f
            }

            // Footer halaman terakhir.
            ensureSpace(25f)

            y += 5f

            drawWrappedText(
                "Dokumen ini diunduh melalui aplikasi BPS Kota Probolinggo.",
                margin,
                normalPaint,
                contentWidth,
                14f
            )

            page?.let { currentPage ->
                pdfDocument?.let { document ->
                    document.finishPage(currentPage)
                }
                page = null
            }

            val uri = savePdfToDownloads(
                pdfDocument,
                namaFile
            )

            pdfDocument.close()
            pdfDocument = null

            if (uri == null) {
                Toast.makeText(
                    this,
                    "Gagal menyimpan PDF.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            showDownloadNotification(
                uri,
                namaFile
            )

            Toast.makeText(
                this,
                "PDF berhasil diunduh.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {



            try {
                pdfDocument?.close()
            } catch (_: Exception) {
            }

            Toast.makeText(
                this,
                "Gagal membuat PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ================================================================
    // MEMECAH TEKS AGAR TIDAK TERPOTONG DI PDF
    // ================================================================

    private fun splitTextToLines(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {

        val result = mutableListOf<String>()
        val words = text.split(" ")

        var line = ""

        for (word in words) {

            val candidate = if (line.isEmpty()) {
                word
            } else {
                "$line $word"
            }

            if (
                paint.measureText(candidate) > maxWidth &&
                line.isNotEmpty()
            ) {
                result.add(line)
                line = word
            } else {
                line = candidate
            }
        }

        if (line.isNotEmpty()) {
            result.add(line)
        }

        return result.ifEmpty {
            listOf("-")
        }
    }

    // ================================================================
    // MEMBUAT HALAMAN PDF
    // ================================================================

    private fun createPdfPage(
        document: PdfDocument,
        pageNumber: Int
    ): PdfDocument.Page {

        val pageInfo = PdfDocument.PageInfo.Builder(
            595,
            842,
            pageNumber
        ).create()

        return document.startPage(pageInfo)
    }

    // ================================================================
    // SIMPAN PDF KE DOWNLOAD
    // ================================================================

    private fun savePdfToDownloads(
        pdfDocument: PdfDocument,
        namaFile: String
    ): Uri? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val resolver = contentResolver

            val values = ContentValues().apply {

                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    namaFile
                )

                put(
                    MediaStore.Downloads.MIME_TYPE,
                    "application/pdf"
                )

                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/BPS Kota Probolinggo"
                )

                put(
                    MediaStore.Downloads.IS_PENDING,
                    1
                )
            }

            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: return null

            try {

                resolver.openOutputStream(uri).use { outputStream ->

                    if (outputStream == null) {
                        throw Exception(
                            "OutputStream tidak tersedia."
                        )
                    }

                    pdfDocument.writeTo(outputStream)
                }

                val updateValues = ContentValues().apply {
                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                }

                resolver.update(
                    uri,
                    updateValues,
                    null,
                    null
                )

                return uri

            } catch (e: Exception) {

                resolver.delete(uri, null, null)
                throw e
            }
        }

        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        val folder = File(
            downloads,
            "BPS Kota Probolinggo"
        )

        if (!folder.exists() && !folder.mkdirs()) {
            throw Exception("Folder Download tidak dapat dibuat.")
        }

        val file = File(folder, namaFile)

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        return Uri.fromFile(file)
    }

    // ================================================================
    // NOTIFICATION CHANNEL
    // ================================================================

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download BPS",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            channel.description =
                "Notifikasi hasil download data BPS"

            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    // ================================================================
    // IZIN NOTIFIKASI
    // ================================================================

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    // ================================================================
    // NOTIFIKASI DOWNLOAD SELESAI
    // ================================================================

    private fun showDownloadNotification(
        uri: Uri,
        namaFile: String
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(
            Intent.ACTION_VIEW
        ).apply {

            setDataAndType(
                uri,
                "application/pdf"
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download selesai")
            .setContentText(namaFile)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "File $namaFile berhasil disimpan ke Download/BPS Kota Probolinggo"
                    )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    // ================================================================
    // HTML KE PLAIN TEXT
    // ================================================================

    private fun getPlainText(text: String): String {

        return HtmlCompat.fromHtml(
            text,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ================================================================
    // LOADING
    // ================================================================

    private fun showLoading(isLoading: Boolean) {

        if (isLoading) {

            binding.progressLoading.visibility = View.VISIBLE
            binding.scrollDetail.visibility = View.GONE

        } else {

            binding.progressLoading.visibility = View.GONE
            binding.scrollDetail.visibility = View.VISIBLE
        }
    }

    // ================================================================
    // ERROR
    // ================================================================

    private fun showError(message: String) {

        binding.tvStatus.text = message
        binding.tvStatus.visibility = View.VISIBLE

        binding.dataDetailContainer.removeAllViews()

        dataPdfCards.clear()
        dataPdfSiap = false
    }
}