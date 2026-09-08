package com.example.bpskota

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.KependudukanDataResponse
import com.example.bpskota.bps.model.KependudukanItem
import com.example.bpskota.bps.model.KependudukanStaticDetailData
import com.example.bpskota.bps.model.KependudukanStaticDetailResponse
import com.example.bpskota.bps.repository.BpsRepository
import org.jsoup.Jsoup

class KependudukanDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KependudukanDetail"
        private const val DOMAIN = "3574"
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"

        const val EXTRA_VARIABLE = "EXTRA_VARIABLE"
        const val EXTRA_TAHUN = "EXTRA_TAHUN"
        const val EXTRA_JUDUL = "EXTRA_JUDUL"
        const val EXTRA_STATIC_BUTUH_TAHUN = "EXTRA_STATIC_BUTUH_TAHUN"
        const val EXTRA_IS_SIMDASI = "EXTRA_IS_SIMDASI"
        const val EXTRA_TAHUN_TERSEDIA = "EXTRA_TAHUN_TERSEDIA"
        const val EXTRA_IS_VARIABLE = "EXTRA_IS_VARIABLE"
    }

    private val repository = BpsRepository()

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var tvJudul: TextView
    private lateinit var tvTahun: TextView

    private var variableId = 0
    private var tahun = 0
    private var judul = ""

    private var staticButuhTahun = false
    private var isSimdasi = false
    private var isVariable = false

    private val daftarTahun = linkedMapOf<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kependudukan_detail)

        initView()
        ambilIntent()
        setupButton()
        loadDetail()
    }

    private fun initView() {
        cardContainer = findViewById(R.id.cardContainer)
        progressLoading = findViewById(R.id.progressLoading)
        progressLoading.setAnimation("Loading_Animation.json")
        progressLoading.repeatCount = -1

        btnBack = findViewById(R.id.btnBack)
        tvJudul = findViewById(R.id.tvJudul)
        tvTahun = findViewById(R.id.tvTahun)
    }

    private fun ambilIntent() {
        variableId = intent.getIntExtra(EXTRA_VARIABLE, 0)
        tahun = intent.getIntExtra(EXTRA_TAHUN, 0)
        judul = intent.getStringExtra(EXTRA_JUDUL) ?: ""
        staticButuhTahun = intent.getBooleanExtra(EXTRA_STATIC_BUTUH_TAHUN, false)
        isSimdasi = intent.getBooleanExtra(EXTRA_IS_SIMDASI, false)
        isVariable = intent.getBooleanExtra(EXTRA_IS_VARIABLE, false)

        val tahunTersedia = intent.getIntegerArrayListExtra(EXTRA_TAHUN_TERSEDIA)
        if (tahunTersedia != null) {
            tahunTersedia
                .distinct()
                .sortedDescending()
                .forEach { tahunItem ->
                    daftarTahun[tahunItem] = 0
                }
        }

        tvJudul.text = judul.ifBlank { "Data Kependudukan" }
        tvTahun.text = if (tahun > 0) {
            tahun.toString()
        } else {
            "-"
        }
    }

    private fun setupButton() {
        btnBack.setOnClickListener {
            finish()
        }

        tvTahun.setOnClickListener {
            if (daftarTahun.size > 1) {
                tampilkanFilterTahun()
            }
        }
    }

    private fun tampilkanLoading() {
        progressLoading.visibility = View.VISIBLE
        if (!progressLoading.isAnimating) {
            progressLoading.playAnimation()
        }
    }

    private fun sembunyikanLoading() {
        progressLoading.cancelAnimation()
        progressLoading.visibility = View.GONE
    }

    private fun loadDetail() {
        if (variableId <= 0) {
            tampilkanPesan("ID tabel tidak tersedia")
            return
        }

        tampilkanLoading()
        cardContainer.removeAllViews()

        if (isVariable) {
            if (tahun <= 0 && daftarTahun.isNotEmpty()) {
                tahun = daftarTahun.keys.maxOrNull() ?: 0
            }
            loadVariableData()
            return
        }

        if (!staticButuhTahun) {
            loadStaticTable()
            return
        }

        if (tahun <= 0 && daftarTahun.isNotEmpty()) {
            tahun = daftarTahun.keys.maxOrNull() ?: 0
        }

        if (tahun <= 0) {
            sembunyikanLoading()
            tampilkanPesan("Tahun tidak tersedia")
            return
        }

        loadStaticTable()
    }

    // =====================================================================
    // LOGIKA UNTUK DYNAMIC VARIABLE (API: model=data)
    // =====================================================================

    private fun loadVariableData() {
        Log.d(TAG, "=== MEMULAI PROSES LOAD VARIABLE ===")
        Log.d(TAG, "Tahun kalender yang dipilih user: $tahun")

        // Rumus otomatis: Kurangi 1900 dari tahun kalender
        // Contoh: 2020 - 1900 = 120, 2021 - 1900 = 121, dst.
        val tahunIdBps = tahun - 1900

        Log.d(TAG, "Menerjemahkan Tahun Kalender: $tahun -> Menjadi ID BPS: $tahunIdBps")

        // Langsung tembak URL target dengan ID hasil pengurangan
        fetchDataVariabelDenganIdTahun(tahunIdBps)
    }

    private fun fetchDataVariabelDenganIdTahun(tahunIdBps: Int) {
        repository
            .getKependudukanDetail(
                domain = DOMAIN,
                variable = variableId,
                tahun = tahunIdBps,
                apiKey = API_KEY
            )
            .enqueue(object : retrofit2.Callback<KependudukanDataResponse> {
                override fun onResponse(
                    call: retrofit2.Call<KependudukanDataResponse>,
                    response: retrofit2.Response<KependudukanDataResponse>
                ) {
                    sembunyikanLoading()

                    // --- TAMBAHAN LOG API DATA VARIABLE ---
                    Log.d(TAG, "=== DETAIL API DATA VARIABLE ===")
                    Log.d(TAG, "URL API: ${call.request().url}")

                    if (response.isSuccessful) {
                        // Mencetak JSON utuh yang dikirim oleh server
                        val rawJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(response.body())
                        Log.d(TAG, "HASIL JSON:\n$rawJson")
                    } else {
                        Log.e(TAG, "GAGAL: HTTP ${response.code()}")
                    }
                    Log.d(TAG, "===================================")
                    // --------------------------------------

                    if (!response.isSuccessful) {
                        tampilkanPesan("Gagal mengambil data Variabel (${response.code()})")
                        return
                    }

                    val body = response.body()
                    if (body == null || body.status?.uppercase() != "OK") {
                        tampilkanPesan("Data Variabel tidak tersedia")
                        return
                    }

                    val dataContent = body.dataContent
                    val vervarList = body.vervar

                    if (dataContent.isNullOrEmpty()) {
                        tampilkanPesanKosong("Data dari BPS belum tersedia untuk tahun $tahun.\nSilakan pilih tahun lain.")
                        return
                    }

                    if (vervarList.isNullOrEmpty()) {
                        tampilkanPesan("Format wilayah tidak dikenali")
                        return
                    }

                    val unit = body.variables?.firstOrNull()?.unit ?: ""

                    vervarList.forEach { wilayah ->
                        val namaWilayah = wilayah.label ?: "Wilayah Tidak Diketahui"
                        val idWilayah = wilayah.valId.toString()

                        val kecocokanKunci = dataContent.entries.find { it.key.contains(idWilayah) }
                        var nilai = kecocokanKunci?.value?.toString() ?: "-"

                        if (nilai != "-" && unit.isNotBlank()) {
                            nilai = "$nilai $unit"
                        }

                        val rowData = listOf(Pair(tvJudul.text.toString(), nilai))
                        tampilkanCardDataBaris(namaWilayah, rowData)
                    }
                }

                override fun onFailure(call: retrofit2.Call<KependudukanDataResponse>, t: Throwable) {
                    sembunyikanLoading()
                    Log.e(TAG, "LOAD VARIABLE DATA GAGAL", t)
                    tampilkanPesan("Terjadi kesalahan koneksi")
                }
            })
    }

    // =====================================================================
    // LOGIKA UNTUK STATIC TABLE (API: model=statictable)
    // =====================================================================

    private fun loadStaticTable() {
        repository
            .getKependudukanStaticTableDetail(
                domain = DOMAIN,
                id = variableId,
                apiKey = API_KEY
            )
            .enqueue(
                object : retrofit2.Callback<KependudukanStaticDetailResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<KependudukanStaticDetailResponse>,
                        response: retrofit2.Response<KependudukanStaticDetailResponse>
                    ) {
                        sembunyikanLoading()

                        // --- TAMBAHAN LOG API STATIC TABLE ---
                        Log.d(TAG, "=== DETAIL API STATIC TABLE ===")
                        Log.d(TAG, "URL API: ${call.request().url}")
                        Log.d(TAG, "===============================")
                        // -------------------------------------

                        if (!response.isSuccessful) {
                            tampilkanPesan("Gagal mengambil data (${response.code()})")
                            return
                        }

                        val body = response.body()
                        if (body == null) {
                            tampilkanPesan("Data tidak tersedia")
                            return
                        }

                        if (body.status?.uppercase() != "OK") {
                            tampilkanPesan("Response API tidak valid")
                            return
                        }

                        if (body.dataAvailability?.lowercase() != "available") {
                            tampilkanPesan("Data statistik tidak tersedia")
                            return
                        }

                        val dataElement = body.data
                        if (dataElement == null || !dataElement.isJsonObject) {
                            tampilkanPesan("Format isi tabel tidak sesuai atau kosong")
                            return
                        }

                        val data = com.google.gson.Gson().fromJson(
                            dataElement,
                            KependudukanStaticDetailData::class.java
                        )

                        if (data == null || data.table.isNullOrBlank()) {
                            tampilkanPesan("Isi tabel tidak tersedia")
                            return
                        }

                        if (!data.title.isNullOrBlank() && judul.isBlank()) {
                            tvJudul.text = data.title
                        }

                        tampilkanTableHtml(data.table)
                    }

                    override fun onFailure(
                        call: retrofit2.Call<KependudukanStaticDetailResponse>,
                        t: Throwable
                    ) {
                        sembunyikanLoading()
                        Log.e(TAG, "LOAD STATIC TABLE GAGAL", t)
                        tampilkanPesan("Gagal mengambil data: ${t.message ?: "Unknown error"}")
                    }
                }
            )
    }

    private fun tampilkanTableHtml(html: String) {
        cardContainer.removeAllViews()

        val decodedHtml = html
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)

        val document = Jsoup.parse(decodedHtml)
        val table = document.select("table").firstOrNull()

        if (table == null) {
            tampilkanPesan("Tabel tidak ditemukan")
            return
        }

        val rows = table.select("tr")
        if (rows.isEmpty()) {
            tampilkanPesan("Baris tabel tidak ditemukan")
            return
        }

        var maxCols = 0
        for (row in rows) {
            var jumlahKolom = 0
            for (cell in row.select("th, td")) {
                val colspan = cell.attr("colspan").toIntOrNull() ?: 1
                jumlahKolom += colspan
            }
            maxCols = maxOf(maxCols, jumlahKolom)
        }

        val grid = Array(rows.size) { Array(maxCols) { "" } }

        for (r in rows.indices) {
            val cells = rows[r].select("th, td")
            var c = 0
            for (cell in cells) {
                while (c < maxCols && grid[r][c].isNotBlank()) {
                    c++
                }
                if (c >= maxCols) break

                val text = cell.text().trim().replace(Regex("\\s+"), " ")
                val rowspan = cell.attr("rowspan").toIntOrNull() ?: 1
                val colspan = cell.attr("colspan").toIntOrNull() ?: 1

                for (rr in 0 until rowspan) {
                    for (cc in 0 until colspan) {
                        if (r + rr < rows.size && c + cc < maxCols) {
                            grid[r + rr][c + cc] = text
                        }
                    }
                }
                c += colspan
            }
        }

        var dataStartRow = -1
        var kolomWilayah = 0

        for (r in grid.indices) {
            var numericCount = 0
            var firstTextCol = -1
            for (c in 0 until maxCols) {
                val cell = grid[r][c].trim()
                if (cell.isNotBlank()) {
                    val isYear = cell.matches(Regex("^(19|20)\\d{2}$"))
                    val isNumber = cell.matches(Regex("^-?[0-9.,]+$")) && cell.any { it.isDigit() } && !isYear

                    if (isNumber || cell == "-") {
                        numericCount++
                    } else if (firstTextCol == -1 && !isYear) {
                        firstTextCol = c
                    }
                }
            }

            if (numericCount > 0 && firstTextCol != -1) {
                dataStartRow = r
                kolomWilayah = firstTextCol
                break
            }
        }

        if (dataStartRow == -1) {
            dataStartRow = 1
            kolomWilayah = 0
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
            if (isBigTitle) {
                headerStartRow = 1
            }
        }

        val headers = Array(maxCols) { "" }
        for (c in 0 until maxCols) {
            val parts = mutableListOf<String>()
            for (r in headerStartRow until dataStartRow) {
                val cell = grid[r][c].trim()
                if (cell.isNotBlank() && !parts.contains(cell)) {
                    if (!cell.equals("Kecamatan", ignoreCase = true) &&
                        !cell.equals("Kabupaten/Kota", ignoreCase = true)) {
                        parts.add(cell)
                    }
                }
            }
            headers[c] = parts.joinToString(" ")
            if (headers[c].isBlank()) {
                headers[c] = "Data ${c + 1}"
            }
        }

        for (r in dataStartRow until grid.size) {
            val wilayah = grid[r][kolomWilayah].trim()
            if (wilayah.isBlank() ||
                wilayah.lowercase().startsWith("catatan") ||
                wilayah.lowercase().startsWith("sumber")) {
                continue
            }

            val rowData = mutableListOf<Pair<String, String>>()
            for (c in 0 until maxCols) {
                if (c == kolomWilayah) continue

                val headerName = headers[c]
                val nilai = grid[r][c].trim().ifBlank { "-" }

                rowData.add(Pair(headerName, nilai))
            }

            if (rowData.isNotEmpty()) {
                tampilkanCardDataBaris(wilayah, rowData)
            }
        }
    }

    private fun tampilkanCardDataBaris(
        wilayah: String,
        dataBaris: List<Pair<String, String>>
    ) {
        val card = LayoutInflater.from(this).inflate(R.layout.card_statistik, cardContainer, false)
        val tvNamaWilayah = card.findViewById<TextView>(R.id.tvNamaWilayah)
        val containerVariable = card.findViewById<LinearLayout>(R.id.containerVariable)

        tvNamaWilayah.text = wilayah

        for ((label, nilai) in dataBaris) {
            tambahBarisData(containerVariable, label, nilai)
        }

        cardContainer.addView(card)
    }

    private fun tambahBarisData(
        container: LinearLayout,
        label: String,
        nilai: String
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 3, 0, 3)
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
            maxLines = 1
            isSingleLine = true
            ellipsize = null
            setTextColor(Color.parseColor("#111827"))
            setTypeface(null, Typeface.BOLD)
        }

        row.addView(tvLabel)
        row.addView(tvData)
        container.addView(row)
    }

    private fun tampilkanPesanKosong(pesan: String) {
        val tvKosong = TextView(this@KependudukanDetailActivity).apply {
            text = pesan
            textSize = 15f
            setTextColor(Color.GRAY)
            setPadding(32, 64, 32, 32)
            gravity = Gravity.CENTER
        }
        cardContainer.addView(tvKosong)
    }

    private fun tampilkanFilterTahun() {
        val tahunList = daftarTahun.keys.sortedDescending()
        if (tahunList.isEmpty()) {
            return
        }

        val labels = tahunList.map { it.toString() }.toTypedArray()
        var posisiTerpilih = tahunList.indexOf(tahun)
        if (posisiTerpilih < 0) {
            posisiTerpilih = 0
        }

        AlertDialog.Builder(this)
            .setTitle("Pilih Tahun")
            .setSingleChoiceItems(labels, posisiTerpilih) { dialog, which ->
                val tahunDipilih = tahunList[which]
                if (tahunDipilih == tahun) {
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }

                tahun = tahunDipilih
                tvTahun.text = tahun.toString()
                dialog.dismiss()
                loadDetail()
            }
            .show()
    }

    private fun tampilkanPesan(pesan: String) {
        Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        if (::progressLoading.isInitialized) {
            progressLoading.cancelAnimation()
        }
        super.onDestroy()
    }
}