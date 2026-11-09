
package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bpskota.bps.model.AllSimdasiResponse
import com.example.bpskota.bps.model.AllStaticTableResponse
import com.example.bpskota.bps.model.AllVariableResponse
import com.example.bpskota.bps.repository.BpsAllDataRepository
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class DataFragment : Fragment(), RefreshableFragment {

    companion object {
        private const val TAG = "DataFragment"
        private const val WILAYAH = "3574000"
        private const val DOMAIN = "3574"
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
        private const val PAGE_INDEX = 3
    }

    // =========================================================
    // VIEW
    // =========================================================

    private lateinit var dataApiContainer: LinearLayout
    private lateinit var progressData: ProgressBar
    private lateinit var etSearchData: EditText

    private var verticalScrollView: View? = null

    // =========================================================
    // REPOSITORY DAN DATA
    // =========================================================

    private val repository = BpsAllDataRepository()

    private val semuaData = mutableListOf<JsonElement>()
    private val sumberData = mutableListOf<String>()

    // =========================================================
    // STATUS LOADING
    // =========================================================

    private var sudahMemuatData = false
    private var sedangMemuatData = false

    // =========================================================
    // CREATE VIEW
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_data,
            container,
            false
        )
    }

    // =========================================================
    // VIEW CREATED
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        dataApiContainer =
            view.findViewById(R.id.dataApiContainer)

        progressData =
            view.findViewById(R.id.progressData)

        etSearchData =
            view.findViewById(R.id.etSearchData)

        verticalScrollView =
            findVerticalScrollContainer(view)

        setupSwipeRefreshState()

        // =====================================================
        // NAVIGASI MENU
        // =====================================================

        view.findViewById<View>(R.id.cardDataPenduduk).setOnClickListener {
            startActivity(
                Intent(requireContext(), KependudukanActivity::class.java)
            )
        }

        view.findViewById<View>(R.id.cardDataTenagaKerja).setOnClickListener {
            startActivity(
                Intent(requireContext(), TenagakerjaActivity::class.java)
            )
        }

        view.findViewById<View>(R.id.cardDataEkonomi).setOnClickListener {
            startActivity(
                Intent(requireContext(), EkonomiActivity::class.java)
            )
        }

        view.findViewById<View>(R.id.tvKategoriLihatSemua).setOnClickListener {
            startActivity(
                Intent(requireContext(), KategoriListActivity::class.java)
            )
        }

        view.findViewById<View>(R.id.cardDataSosial).setOnClickListener {
            startActivity(
                Intent(requireContext(), GenderActivity::class.java)
            )
        }

        view.findViewById<View>(R.id.cardDataPerumahan).setOnClickListener {
            startActivity(
                Intent(requireContext(), TempattinggalActivity::class.java)
            )
        }

        view.findViewById<View>(R.id.cardDataPertanian).setOnClickListener {
            startActivity(
                Intent(requireContext(), PertanianActivity::class.java)
            )
        }

        // =====================================================
        // SEARCH
        // =====================================================

        etSearchData.setOnEditorActionListener { _, _, _ ->

            filterData(etSearchData.text.toString())

            true
        }

        // =====================================================
        // PULIHKAN TAMPILAN JIKA DATA SUDAH PERNAH DIMUAT
        // =====================================================

        when {

            sudahMemuatData -> {

                progressData.visibility = View.GONE

                tampilkanDataTerbaru()
            }

            sedangMemuatData -> {

                progressData.visibility = View.VISIBLE
            }

            else -> {

                progressData.visibility = View.GONE
            }
        }
    }

    // =========================================================
    // LOAD KETIKA HALAMAN DATA AKTIF
    // =========================================================

    override fun onResume() {
        super.onResume()

        val homeActivity = activity as? HomeActivity ?: return

        // Hanya load ketika halaman Data dipilih.
        if (homeActivity.currentPage != PAGE_INDEX) return

        // Hindari request API berulang.
        if (sudahMemuatData || sedangMemuatData) return

        loadAllData()
    }

    // =========================================================
    // SETUP SWIPE REFRESH
    // =========================================================

    private fun setupSwipeRefreshState() {

        val scrollView = verticalScrollView ?: return

        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            updateRefreshState()
        }

        scrollView.post {
            updateRefreshState()
        }
    }

    // =========================================================
    // UPDATE STATUS SWIPE REFRESH
    // =========================================================

    override fun updateRefreshState() {

        if (!isAdded) return

        val scrollView = verticalScrollView ?: return

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        val isAtTop =
            !scrollView.canScrollVertically(-1)

        homeActivity.setSwipeRefreshEnabled(isAtTop)
    }

    // =========================================================
    // CARI KOMPONEN SCROLL
    // =========================================================

    private fun findVerticalScrollContainer(root: View): View? {

        if (root is ScrollView) return root

        if (root is NestedScrollView) return root

        if (root is RecyclerView) return root

        if (root is ViewGroup) {

            for (i in 0 until root.childCount) {

                val child = root.getChildAt(i)

                val result = findVerticalScrollContainer(child)

                if (result != null) return result
            }
        }

        return null
    }

    // =========================================================
    // REFRESH DATA
    // =========================================================

    override fun refreshData() {

        if (!isAdded) return

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        val scrollView = verticalScrollView

        if (scrollView != null && scrollView.canScrollVertically(-1)) {

            finishSwipeRefresh()

            return
        }

        // Jangan menjalankan refresh bersamaan dengan request aktif.
        if (sedangMemuatData) {

            finishSwipeRefresh()

            return
        }

        // Refresh memaksa pengambilan data terbaru.
        sudahMemuatData = false

        loadAllData()
    }

    // =========================================================
    // SELESAIKAN SWIPE REFRESH
    // =========================================================

    private fun finishSwipeRefresh() {

        if (!isAdded) return

        val homeActivity = activity as? HomeActivity ?: return

        homeActivity.finishSwipeRefresh()
    }

    // =========================================================
    // LOAD SEMUA DATA
    // =========================================================

    private fun loadAllData() {

        if (!isAdded) return

        if (sedangMemuatData || sudahMemuatData) return

        sedangMemuatData = true

        progressData.visibility = View.VISIBLE

        dataApiContainer.removeAllViews()

        semuaData.clear()

        sumberData.clear()

        // Mulai dari SIMDASI.
        loadSimdasiPage(page = 1)
    }

    // =========================================================
    // LOAD SIMDASI
    // =========================================================

    private fun loadSimdasiPage(page: Int) {

        if (!isAdded) return

        repository.getAllSimdasi(
            wilayah = WILAYAH,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) return@getAllSimdasi

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (error != null) {

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil SIMDASI: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    loadStaticTablePage(page = 1)

                    return@runOnUiThread
                }

                if (response == null) {

                    loadStaticTablePage(page = 1)

                    return@runOnUiThread
                }

                val tables = extractSimdasiTables(response)

                semuaData.addAll(tables)

                repeat(tables.size) {
                    sumberData.add("SIMDASI")
                }

                val totalPages =
                    getSimdasiTotalPages(response).coerceAtLeast(1)

                if (page < totalPages) {

                    loadSimdasiPage(page = page + 1)

                } else {

                    loadStaticTablePage(page = 1)
                }
            }
        }
    }

    // =========================================================
    // EKSTRAK DATA SIMDASI
    // =========================================================

    private fun extractSimdasiTables(
        response: AllSimdasiResponse
    ): List<JsonElement> {

        val hasil = mutableListOf<JsonElement>()

        val rootData = response.data

        if (rootData == null || !rootData.isJsonArray) return hasil

        val outerArray = rootData.asJsonArray

        if (outerArray.size() < 2) return hasil

        val wrapper = outerArray[1]

        if (!wrapper.isJsonObject) return hasil

        val wrapperObject = wrapper.asJsonObject

        val tableData = wrapperObject.get("data")

        if (tableData == null || !tableData.isJsonArray) return hasil

        for (table in tableData.asJsonArray) {
            hasil.add(table)
        }

        return hasil
    }

    // =========================================================
    // TOTAL HALAMAN SIMDASI
    // =========================================================

    private fun getSimdasiTotalPages(
        response: AllSimdasiResponse
    ): Int {

        val rootData = response.data

        if (rootData == null || !rootData.isJsonArray) return 1

        val outerArray = rootData.asJsonArray

        if (outerArray.isEmpty()) return 1

        val pagination = outerArray[0]

        if (!pagination.isJsonObject) return 1

        return pagination.asJsonObject
            .get("pages")
            ?.asInt ?: 1
    }

    // =========================================================
    // LOAD STATIC TABLE
    // =========================================================

    private fun loadStaticTablePage(page: Int) {

        if (!isAdded) return

        repository.getAllStaticTables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) return@getAllStaticTables

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (error != null) {

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil Static Table: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    loadVariablePage(page = 1)

                    return@runOnUiThread
                }

                if (response == null) {

                    loadVariablePage(page = 1)

                    return@runOnUiThread
                }

                val tables = extractStaticTables(response)

                semuaData.addAll(tables)

                repeat(tables.size) {
                    sumberData.add("STATIC")
                }

                val totalPages =
                    getStaticTotalPages(response).coerceAtLeast(1)

                if (page < totalPages) {

                    loadStaticTablePage(page = page + 1)

                } else {

                    loadVariablePage(page = 1)
                }
            }
        }
    }

    // =========================================================
    // EKSTRAK STATIC TABLE
    // =========================================================

    private fun extractStaticTables(
        response: AllStaticTableResponse
    ): List<JsonElement> {

        val hasil = mutableListOf<JsonElement>()

        val rootData = response.data

        if (rootData == null || !rootData.isJsonArray) return hasil

        val outerArray = rootData.asJsonArray

        if (outerArray.size() < 2) return hasil

        val tableData = outerArray[1]

        if (!tableData.isJsonArray) return hasil

        for (table in tableData.asJsonArray) {
            hasil.add(table)
        }

        return hasil
    }

    // =========================================================
    // TOTAL HALAMAN STATIC TABLE
    // =========================================================

    private fun getStaticTotalPages(
        response: AllStaticTableResponse
    ): Int {

        val rootData = response.data

        if (rootData == null || !rootData.isJsonArray) return 1

        val outerArray = rootData.asJsonArray

        if (outerArray.isEmpty()) return 1

        val pagination = outerArray[0]

        if (!pagination.isJsonObject) return 1

        return pagination.asJsonObject
            .get("pages")
            ?.asInt ?: 1
    }

    // =========================================================
    // LOAD VARIABLE
    // =========================================================

    private fun loadVariablePage(page: Int) {

        if (!isAdded) return

        repository.getAllVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) return@getAllVariables

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (error != null) {

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil Variable: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    selesaiLoadSemuaData()

                    return@runOnUiThread
                }

                if (response == null) {

                    selesaiLoadSemuaData()

                    return@runOnUiThread
                }

                val variables = extractVariableTables(response)

                semuaData.addAll(variables)

                repeat(variables.size) {
                    sumberData.add("VARIABLE")
                }

                val totalPages =
                    getVariableTotalPages(response).coerceAtLeast(1)

                if (page < totalPages) {

                    loadVariablePage(page = page + 1)

                } else {

                    selesaiLoadSemuaData()
                }
            }
        }
    }

    // =========================================================
    // EKSTRAK VARIABLE
    // =========================================================

    private fun extractVariableTables(
        response: AllVariableResponse
    ): List<JsonElement> {

        val hasil = mutableListOf<JsonElement>()

        val rootData = response.data

        if (rootData == null || !rootData.isJsonArray) return hasil

        val outerArray = rootData.asJsonArray

        if (outerArray.size() < 2) return hasil

        val variableData = outerArray[1]

        if (!variableData.isJsonArray) return hasil

        for (variable in variableData.asJsonArray) {
            hasil.add(variable)
        }

        return hasil
    }

    // =========================================================
    // TOTAL HALAMAN VARIABLE
    // =========================================================

    private fun getVariableTotalPages(
        response: AllVariableResponse
    ): Int {

        val rootData = response.data

        if (rootData == null || !rootData.isJsonArray) return 1

        val outerArray = rootData.asJsonArray

        if (outerArray.isEmpty()) return 1

        val pagination = outerArray[0]

        if (!pagination.isJsonObject) return 1

        return pagination.asJsonObject
            .get("pages")
            ?.asInt ?: 1
    }

    // =========================================================
    // SEMUA DATA SELESAI DIMUAT
    // =========================================================

    private fun selesaiLoadSemuaData() {

        if (!isAdded) return

        sedangMemuatData = false

        sudahMemuatData = true

        progressData.visibility = View.GONE

        tampilkanDataTerbaru()

        finishSwipeRefresh()

        verticalScrollView?.post {
            updateRefreshState()
        }
    }

    // =========================================================
    // TAMPILKAN DATA TERBARU
    // =========================================================

    private fun tampilkanDataTerbaru() {

        dataApiContainer.removeAllViews()

        if (semuaData.isEmpty()) {

            tampilkanPesan("Tidak ada data yang tersedia.")

            return
        }

        val jumlahPerKategori = mutableMapOf<String, Int>()

        for (index in semuaData.indices) {

            val table = semuaData[index]

            if (!table.isJsonObject) continue

            val obj = table.asJsonObject

            val sumber =
                if (index < sumberData.size) {
                    sumberData[index]
                } else {
                    "SIMDASI"
                }

            val kategori = getKategori(obj, sumber)

            val key = "$sumber|$kategori"

            val jumlah = jumlahPerKategori[key] ?: 0

            if (jumlah >= 5) continue

            tambahCard(table, sumber)

            jumlahPerKategori[key] = jumlah + 1
        }
    }

    // =========================================================
    // KATEGORI DATA
    // =========================================================

    private fun getKategori(
        obj: JsonObject,
        sumber: String
    ): String {

        return when (sumber) {

            "SIMDASI" ->
                getString(obj, "bab").ifBlank { "Lainnya" }

            "STATIC" ->
                getString(obj, "subj").ifBlank { "Lainnya" }

            "VARIABLE" -> {
                getString(obj, "sub_name").ifBlank {
                    getString(obj, "subcsa_name").ifBlank {
                        "Lainnya"
                    }
                }
            }

            else -> "Lainnya"
        }
    }

    // =========================================================
    // TAMBAH CARD
    // =========================================================

    private fun tambahCard(
        table: JsonElement,
        sumber: String
    ) {

        if (!table.isJsonObject) return

        val obj = table.asJsonObject

        val view = layoutInflater.inflate(
            R.layout.item_statistik,
            dataApiContainer,
            false
        )

        val tvKode = view.findViewById<TextView>(R.id.tvKode)
        val tvTahun = view.findViewById<TextView>(R.id.tvTahun)
        val tvKategori = view.findViewById<TextView>(R.id.tvKategori)
        val tvJudul = view.findViewById<TextView>(R.id.tvJudul)
        val tvNilai = view.findViewById<TextView>(R.id.tvNilai)

        tvKode.visibility = View.GONE
        tvNilai.visibility = View.GONE

        if (sumber == "SIMDASI") {

            val idTabel = getString(obj, "id_tabel")
            val kode = getString(obj, "kode_tabel")
            val judul = getString(obj, "judul")
            val bab = getString(obj, "bab")

            val tahunTersedia =
                getDaftarTahunSimdasi(obj.get("ketersediaan_tahun"))

            val tahunTerbaru = tahunTersedia.maxOrNull() ?: 0

            tvTahun.text =
                formatTahunSimdasi(obj.get("ketersediaan_tahun"))

            tvKategori.text =
                bab.ifBlank { "Statistik" }

            tvJudul.text =
                bersihkanHtml(judul)

            view.setOnClickListener {

                if (idTabel.isBlank()) {

                    Toast.makeText(
                        requireContext(),
                        "ID tabel tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val intent = Intent(
                    requireContext(),
                    StatistikDetailActivity::class.java
                ).apply {

                    putExtra(
                        StatistikDetailActivity.EXTRA_ID_TABEL,
                        idTabel
                    )

                    putExtra(
                        StatistikDetailActivity.EXTRA_JUDUL,
                        bersihkanHtml(judul)
                    )

                    putExtra(
                        StatistikDetailActivity.EXTRA_KODE,
                        kode
                    )

                    if (tahunTerbaru > 0) {

                        putExtra(
                            StatistikDetailActivity.EXTRA_TAHUN,
                            tahunTerbaru
                        )
                    }

                    putIntegerArrayListExtra(
                        StatistikDetailActivity.EXTRA_KETERSEDIAAN_TAHUN,
                        ArrayList(tahunTersedia)
                    )
                }

                startActivity(intent)
            }

        } else if (sumber == "STATIC") {

            val id = getString(obj, "table_id")
            val title = getString(obj, "title")
            val subject = getString(obj, "subj")
            val tahun = getTahunStatic(obj)

            tvTahun.text = tahun

            tvKategori.text =
                subject.ifBlank { "Statistik" }

            tvJudul.text =
                bersihkanHtml(title)

            view.setOnClickListener {

                if (id.isBlank()) {

                    Toast.makeText(
                        requireContext(),
                        "ID tabel tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val intent = Intent(
                    requireContext(),
                    DetailKategoriActivity::class.java
                ).apply {

                    putExtra("ID", id)
                    putExtra("TYPE", "STATIC")
                    putExtra("JUDUL", bersihkanHtml(title))

                    putExtra(
                        "KATEGORI",
                        subject.ifBlank { "Statistik" }
                    )

                    putExtra(
                        "SUMBER",
                        "BPS Kota Probolinggo"
                    )
                }

                startActivity(intent)
            }

        } else if (sumber == "VARIABLE") {

            val id = getString(obj, "var_id")
            val title = getString(obj, "title")

            val kategori = getString(obj, "sub_name")
                .ifBlank {
                    getString(obj, "subcsa_name")
                }

            val tahun = getTahunVariable(obj)

            tvTahun.text = tahun

            tvKategori.text =
                kategori.ifBlank { "Variabel" }

            tvJudul.text =
                bersihkanHtml(title)

            view.setOnClickListener {

                if (id.isBlank()) {

                    Toast.makeText(
                        requireContext(),
                        "ID variable tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val intent = Intent(
                    requireContext(),
                    DetailKategoriActivity::class.java
                ).apply {

                    putExtra("ID", id)
                    putExtra("TYPE", "VARIABLE")
                    putExtra("JUDUL", bersihkanHtml(title))

                    putExtra(
                        "KATEGORI",
                        kategori.ifBlank { "Variabel" }
                    )

                    putExtra(
                        "SUMBER",
                        "BPS Kota Probolinggo"
                    )

                    val tahunInt = getTahunVariableAsInt(obj)

                    if (tahunInt > 0) {
                        putExtra("TAHUN", tahunInt)
                    }
                }

                startActivity(intent)
            }
        }

        dataApiContainer.addView(view)
    }

    // =========================================================
    // DAFTAR TAHUN SIMDASI
    // =========================================================

    private fun getDaftarTahunSimdasi(
        tahunArray: JsonElement?
    ): List<Int> {

        if (tahunArray == null || !tahunArray.isJsonArray) {
            return emptyList()
        }

        return tahunArray.asJsonArray.mapNotNull { item ->

            try {
                item.asInt
            } catch (e: Exception) {
                null
            }

        }.filter {
            it > 0
        }.distinct().sorted()
    }

    // =========================================================
    // FORMAT TAHUN SIMDASI
    // =========================================================

    private fun formatTahunSimdasi(
        tahunArray: JsonElement?
    ): String {

        if (
            tahunArray == null ||
            !tahunArray.isJsonArray ||
            tahunArray.asJsonArray.isEmpty()
        ) {
            return "-"
        }

        val tahun = tahunArray.asJsonArray.mapNotNull { item ->

            try {
                item.asInt
            } catch (e: Exception) {
                null
            }

        }.sorted()

        if (tahun.isEmpty()) return "-"

        return if (tahun.size == 1) {
            tahun.first().toString()
        } else {
            "${tahun.first()} - ${tahun.last()}"
        }
    }

    // =========================================================
    // TAHUN STATIC
    // =========================================================

    private fun getTahunStatic(obj: JsonObject): String {

        val tahun = getString(obj, "tahun")

        if (tahun.isNotBlank()) {
            return formatTahunBps(tahun)
        }

        val updateDate = getString(obj, "updt_date")

        if (updateDate.isNotBlank()) {
            return formatTahunBps(updateDate)
        }

        return "-"
    }

    // =========================================================
    // TAHUN VARIABLE
    // =========================================================

    private fun getTahunVariable(obj: JsonObject): String {

        val kemungkinanField =
            listOf("tahun", "year", "th", "periode")

        for (field in kemungkinanField) {

            val value = getString(obj, field)

            if (value.isNotBlank()) {
                return formatTahunBps(value)
            }
        }

        return "-"
    }

    // =========================================================
    // TAHUN VARIABLE KE INTEGER
    // =========================================================

    private fun getTahunVariableAsInt(obj: JsonObject): Int {

        val kemungkinanField =
            listOf("tahun", "year", "th", "periode")

        for (field in kemungkinanField) {

            val value = getString(obj, field)

            if (value.isBlank()) continue

            val angka = value.trim().toIntOrNull()

            if (angka != null) {

                return when {

                    angka in 0..199 -> angka + 1900

                    angka in 1900..2100 -> angka

                    else -> 0
                }
            }

            if (value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*$"))) {

                return value.substring(0, 4).toIntOrNull() ?: 0
            }
        }

        return 0
    }

    // =========================================================
    // FORMAT TAHUN BPS
    // =========================================================

    private fun formatTahunBps(value: String?): String {

        if (value.isNullOrBlank()) return "-"

        val text = value.trim()

        if (text.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*$"))) {
            return text.substring(0, 4)
        }

        val angka = text.toIntOrNull()

        if (angka != null) {

            return when {

                angka in 0..199 -> (angka + 1900).toString()

                angka in 1900..2100 -> angka.toString()

                else -> text
            }
        }

        return text
    }

    // =========================================================
    // BERSIHKAN HTML
    // =========================================================

    private fun bersihkanHtml(text: String): String {

        if (text.isBlank()) return ""

        return text
            .replace(
                Regex("<sup[^>]*>.*?</sup>", RegexOption.IGNORE_CASE),
                ""
            )
            .replace(
                Regex("<sub[^>]*>.*?</sup>", RegexOption.IGNORE_CASE),
                ""
            )
            .replace(
                Regex("<[^>]*>", RegexOption.IGNORE_CASE),
                ""
            )
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // =========================================================
    // AMBIL STRING JSON
    // =========================================================

    private fun getString(
        obj: JsonObject,
        key: String
    ): String {

        return try {

            obj.get(key)
                ?.takeIf { !it.isJsonNull }
                ?.asString ?: ""

        } catch (e: Exception) {

            ""
        }
    }

    // =========================================================
    // FILTER DATA
    // =========================================================

    private fun filterData(keyword: String) {

        val query = keyword.trim().lowercase()

        if (query.isEmpty()) {

            tampilkanDataTerbaru()

            return
        }

        dataApiContainer.removeAllViews()

        var jumlahHasil = 0

        for (index in semuaData.indices) {

            val table = semuaData[index]

            if (!table.isJsonObject) continue

            val obj = table.asJsonObject

            val sumber =
                if (index < sumberData.size) {
                    sumberData[index]
                } else {
                    "SIMDASI"
                }

            val cocok = when (sumber) {

                "SIMDASI" -> {

                    val judul =
                        getString(obj, "judul").lowercase()

                    val kode =
                        getString(obj, "kode_tabel").lowercase()

                    val bab =
                        getString(obj, "bab").lowercase()

                    judul.contains(query) ||
                            kode.contains(query) ||
                            bab.contains(query)
                }

                "STATIC" -> {

                    val title =
                        getString(obj, "title").lowercase()

                    val tableId =
                        getString(obj, "table_id").lowercase()

                    val subject =
                        getString(obj, "subj").lowercase()

                    title.contains(query) ||
                            tableId.contains(query) ||
                            subject.contains(query)
                }

                "VARIABLE" -> {

                    val title =
                        getString(obj, "title").lowercase()

                    val varId =
                        getString(obj, "var_id").lowercase()

                    val subName =
                        getString(obj, "sub_name").lowercase()

                    val subcsaName =
                        getString(obj, "subcsa_name").lowercase()

                    title.contains(query) ||
                            varId.contains(query) ||
                            subName.contains(query) ||
                            subcsaName.contains(query)
                }

                else -> false
            }

            if (cocok) {

                tambahCard(table, sumber)

                jumlahHasil++
            }
        }

        if (jumlahHasil == 0) {
            tampilkanPesan("Data tidak ditemukan.")
        }
    }

    // =========================================================
    // TAMPILKAN PESAN
    // =========================================================

    private fun tampilkanPesan(pesan: String) {

        val tv = TextView(requireContext()).apply {

            text = pesan

            textSize = 14f

            setTextColor(android.graphics.Color.GRAY)

            setPadding(0, 24, 0, 24)
        }

        dataApiContainer.addView(tv)
    }

    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {

        if (isAdded) {
            finishSwipeRefresh()
        }

        // Data tidak dihapus agar bisa digunakan kembali
        // ketika pengguna kembali ke halaman Data.
        verticalScrollView = null

        super.onDestroyView()
    }
}
