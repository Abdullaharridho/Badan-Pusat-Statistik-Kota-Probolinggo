package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        // Posisi DataFragment pada ViewPager2.
        private const val PAGE_INDEX = 4
    }


    // =====================================================
    // VIEW
    // =====================================================

    private lateinit var dataApiContainer: LinearLayout

    private lateinit var progressData: ProgressBar

    private lateinit var etSearchData: EditText


    // =====================================================
    // SCROLL
    //
    // Container scroll dicari otomatis dari XML.
    // Tidak membutuhkan ID XML baru.
    // =====================================================

    private var verticalScrollView: View? = null


    // =====================================================
    // REPOSITORY
    // =====================================================

    private val repository =
        BpsAllDataRepository()


    // =====================================================
    // SEMUA DATA
    // =====================================================

    private val semuaData =
        mutableListOf<JsonElement>()


    // =====================================================
    // SUMBER DATA
    //
    // SIMDASI
    // STATIC
    // VARIABLE
    // =====================================================

    private val sumberData =
        mutableListOf<String>()


    // =====================================================
    // CREATE VIEW
    // =====================================================

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


    // =====================================================
    // VIEW CREATED
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // =================================================
        // FIND VIEW
        // =================================================

        dataApiContainer =
            view.findViewById(
                R.id.dataApiContainer
            )

        progressData =
            view.findViewById(
                R.id.progressData
            )

        etSearchData =
            view.findViewById(
                R.id.etSearchData
            )


        // =================================================
        // CARI CONTAINER SCROLL
        // =================================================

        verticalScrollView =
            findVerticalScrollContainer(
                view
            )


        // =================================================
        // SETUP SWIPE REFRESH
        // =================================================

        setupSwipeRefreshState()


        // =================================================
        // KEPENDUDUKAN
        // =================================================

        view.findViewById<View>(
            R.id.cardDataPenduduk
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    KependudukanActivity::class.java
                )
            )
        }


        // =================================================
        // TENAGA KERJA
        // =================================================

        view.findViewById<View>(
            R.id.cardDataTenagaKerja
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    TenagakerjaActivity::class.java
                )
            )
        }


        // =================================================
        // EKONOMI
        // =================================================

        view.findViewById<View>(
            R.id.cardDataEkonomi
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    EkonomiActivity::class.java
                )
            )
        }


        // =================================================
        // LIHAT SEMUA KATEGORI
        // =================================================

        view.findViewById<View>(
            R.id.tvKategoriLihatSemua
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    KategoriListActivity::class.java
                )
            )
        }


        // =================================================
        // SOSIAL
        // =================================================

        view.findViewById<View>(
            R.id.cardDataSosial
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    GenderActivity::class.java
                )
            )
        }


        // =================================================
        // PERUMAHAN
        // =================================================

        view.findViewById<View>(
            R.id.cardDataPerumahan
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    TempattinggalActivity::class.java
                )
            )
        }


        // =================================================
        // PERTANIAN
        // =================================================

        view.findViewById<View>(
            R.id.cardDataPertanian
        ).setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    PertanianActivity::class.java
                )
            )
        }



        // =================================================
        // SEARCH
        // =================================================

        etSearchData.setOnEditorActionListener { _, _, _ ->

            filterData(
                etSearchData.text.toString()
            )

            true
        }


        // =================================================
        // LOAD DATA AWAL
        // =================================================

        loadAllData()
    }


    // =====================================================
    // SETUP SWIPE REFRESH STATE
    //
    // Refresh hanya boleh aktif ketika:
    //
    // 1. DataFragment sedang aktif.
    // 2. Scroll berada di posisi paling atas.
    //
    // Jika user masih berada di tengah/bawah,
    // SwipeRefreshLayout dinonaktifkan.
    // =====================================================

    private fun setupSwipeRefreshState() {

        val scrollView =
            verticalScrollView
                ?: return

        val swipeRefreshHome =
            requireActivity().findViewById<SwipeRefreshLayout>(
                R.id.swipeRefreshHome
            )

        // Listener dipasang pada container scroll utama.
        scrollView.setOnScrollChangeListener {

                _,
                _,
                _,
                _,
                _ ->

            updateRefreshState()
        }


        // Pastikan state awal diperiksa setelah layout selesai.
        scrollView.post {

            updateRefreshState()
        }
    }


    // =====================================================
    // UPDATE REFRESH STATE
    //
    // Dipanggil ketika posisi scroll berubah
    // atau ketika user berpindah halaman ViewPager2.
    // =====================================================

    override fun updateRefreshState() {

        if (!isAdded) {
            return
        }

        val scrollView =
            verticalScrollView
                ?: return

        val swipeRefreshHome =
            requireActivity().findViewById<SwipeRefreshLayout>(
                R.id.swipeRefreshHome
            )

        val homeActivity =
            activity as? HomeActivity
                ?: return


        // =================================================
        // DATA FRAGMENT HARUS SEDANG AKTIF
        // =================================================

        if (
            homeActivity.currentPage != PAGE_INDEX
        ) {
            return
        }


        // =================================================
        // CEK POSISI PALING ATAS
        // =================================================
        //
        // canScrollVertically(-1):
        //
        // true  = masih bisa scroll ke atas
        // false = sudah mentok paling atas
        // =================================================

        val isAtTop =
            !scrollView.canScrollVertically(-1)


        // =================================================
        // AKTIFKAN REFRESH HANYA DI PALING ATAS
        // =================================================

        swipeRefreshHome.isEnabled =
            isAtTop
    }


    // =====================================================
    // CARI CONTAINER VERTICAL SCROLL
    //
    // Fungsi ini mencari ScrollView, NestedScrollView,
    // atau RecyclerView di dalam layout fragment.
    //
    // Dengan cara ini kita tidak perlu menambahkan ID baru
    // pada XML.
    // =====================================================

    private fun findVerticalScrollContainer(
        root: View
    ): View? {

        // =================================================
        // SCROLLVIEW
        // =================================================

        if (
            root is ScrollView
        ) {
            return root
        }


        // =================================================
        // NESTED SCROLLVIEW
        // =================================================

        if (
            root is NestedScrollView
        ) {
            return root
        }


        // =================================================
        // RECYCLERVIEW
        // =================================================

        if (
            root is RecyclerView
        ) {
            return root
        }


        // =================================================
        // CARI SECARA REKURSIF
        // =================================================

        if (
            root is ViewGroup
        ) {

            for (
            i in 0 until root.childCount
            ) {

                val child =
                    root.getChildAt(i)

                val result =
                    findVerticalScrollContainer(
                        child
                    )

                if (
                    result != null
                ) {
                    return result
                }
            }
        }


        return null
    }


    // =====================================================
    // REFRESH DATA
    //
    // Dipanggil oleh HomeActivity ketika user melakukan
    // pull-to-refresh pada DataFragment.
    // =====================================================

    override fun refreshData() {

        if (!isAdded) {
            return
        }


        // =================================================
        // PASTIKAN DATA FRAGMENT SEDANG AKTIF
        // =================================================

        val homeActivity =
            activity as? HomeActivity
                ?: return

        if (
            homeActivity.currentPage != PAGE_INDEX
        ) {
            return
        }


        // =================================================
        // PENGAMAN POSISI SCROLL
        //
        // Walaupun SwipeRefreshLayout seharusnya sudah
        // disabled ketika belum di atas, tetap lakukan
        // pengecekan di sini agar data tidak reload
        // secara tidak sengaja.
        // =================================================

        val scrollView =
            verticalScrollView

        if (
            scrollView != null &&
            scrollView.canScrollVertically(-1)
        ) {

            finishSwipeRefresh()

            return
        }


        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "REFRESH DATA FRAGMENT"
        )

        Log.d(
            TAG,
            "================================"
        )


        // =================================================
        // LOAD ULANG SEMUA DATA
        // =================================================

        loadAllData()
    }


    // =====================================================
    // SELESAIKAN SWIPE REFRESH
    // =====================================================

    private fun finishSwipeRefresh() {

        if (!isAdded) {
            return
        }

        val homeActivity =
            activity as? HomeActivity
                ?: return

        homeActivity.finishSwipeRefresh()
    }


    // =====================================================
    // LOAD SEMUA DATA
    // =====================================================

    private fun loadAllData() {

        if (!isAdded) {
            return
        }


        progressData.visibility =
            View.VISIBLE

        dataApiContainer.removeAllViews()

        semuaData.clear()

        sumberData.clear()


        Log.d(
            TAG,
            "Mulai mengambil semua data BPS"
        )


        loadSimdasiPage(
            page = 1
        )
    }


    // =====================================================
    // LOAD SIMDASI
    // =====================================================

    private fun loadSimdasiPage(
        page: Int
    ) {

        if (!isAdded) {
            return
        }


        Log.d(
            TAG,
            "Mengambil SIMDASI page=$page"
        )


        repository.getAllSimdasi(
            wilayah = WILAYAH,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) {
                return@getAllSimdasi
            }


            requireActivity().runOnUiThread {

                if (error != null) {

                    Log.e(
                        TAG,
                        "Error SIMDASI",
                        error
                    )


                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil SIMDASI: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()


                    loadStaticTablePage(
                        page = 1
                    )

                    return@runOnUiThread
                }


                if (response == null) {

                    Log.e(
                        TAG,
                        "Response SIMDASI kosong"
                    )


                    loadStaticTablePage(
                        page = 1
                    )

                    return@runOnUiThread
                }


                val tables =
                    extractSimdasiTables(
                        response
                    )


                Log.d(
                    TAG,
                    "SIMDASI page $page = ${tables.size} tabel"
                )


                semuaData.addAll(
                    tables
                )


                repeat(
                    tables.size
                ) {

                    sumberData.add(
                        "SIMDASI"
                    )
                }


                val totalPages =
                    getSimdasiTotalPages(
                        response
                    )


                Log.d(
                    TAG,
                    "SIMDASI total page=$totalPages"
                )


                if (page < totalPages) {

                    loadSimdasiPage(
                        page = page + 1
                    )

                } else {

                    Log.d(
                        TAG,
                        "Semua SIMDASI selesai"
                    )


                    loadStaticTablePage(
                        page = 1
                    )
                }
            }
        }
    }


    // =====================================================
    // EXTRACT SIMDASI
    // =====================================================

    private fun extractSimdasiTables(
        response: AllSimdasiResponse
    ): List<JsonElement> {

        val hasil =
            mutableListOf<JsonElement>()


        val rootData =
            response.data


        if (
            rootData == null ||
            !rootData.isJsonArray
        ) {
            return hasil
        }


        val outerArray =
            rootData.asJsonArray


        if (
            outerArray.size() < 2
        ) {
            return hasil
        }


        val wrapper =
            outerArray[1]


        if (
            !wrapper.isJsonObject
        ) {
            return hasil
        }


        val wrapperObject =
            wrapper.asJsonObject


        val tableData =
            wrapperObject.get(
                "data"
            )


        if (
            tableData == null ||
            !tableData.isJsonArray
        ) {
            return hasil
        }


        for (
        table in tableData.asJsonArray
        ) {

            hasil.add(
                table
            )
        }


        return hasil
    }


    // =====================================================
    // TOTAL PAGE SIMDASI
    // =====================================================

    private fun getSimdasiTotalPages(
        response: AllSimdasiResponse
    ): Int {

        val rootData =
            response.data


        if (
            rootData == null ||
            !rootData.isJsonArray
        ) {
            return 1
        }


        val outerArray =
            rootData.asJsonArray


        if (
            outerArray.isEmpty()
        ) {
            return 1
        }


        val pagination =
            outerArray[0]


        if (
            !pagination.isJsonObject
        ) {
            return 1
        }


        return pagination
            .asJsonObject
            .get("pages")
            ?.asInt
            ?: 1
    }


    // =====================================================
    // LOAD STATIC TABLE
    // =====================================================

    private fun loadStaticTablePage(
        page: Int
    ) {

        if (!isAdded) {
            return
        }


        Log.d(
            TAG,
            "Mengambil STATIC TABLE page=$page"
        )


        repository.getAllStaticTables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) {
                return@getAllStaticTables
            }


            requireActivity().runOnUiThread {

                if (error != null) {

                    Log.e(
                        TAG,
                        "Error Static Table",
                        error
                    )


                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil Static Table: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()


                    loadVariablePage(
                        page = 1
                    )

                    return@runOnUiThread
                }


                if (response == null) {

                    Log.e(
                        TAG,
                        "Response Static Table kosong"
                    )


                    loadVariablePage(
                        page = 1
                    )

                    return@runOnUiThread
                }


                val tables =
                    extractStaticTables(
                        response
                    )


                Log.d(
                    TAG,
                    "Static Table page $page = ${tables.size} tabel"
                )


                semuaData.addAll(
                    tables
                )


                repeat(
                    tables.size
                ) {

                    sumberData.add(
                        "STATIC"
                    )
                }


                val totalPages =
                    getStaticTotalPages(
                        response
                    )


                Log.d(
                    TAG,
                    "Static Table total page=$totalPages"
                )


                if (page < totalPages) {

                    loadStaticTablePage(
                        page = page + 1
                    )

                } else {

                    Log.d(
                        TAG,
                        "Semua Static Table selesai"
                    )


                    loadVariablePage(
                        page = 1
                    )
                }
            }
        }
    }


    // =====================================================
    // EXTRACT STATIC TABLE
    // =====================================================

    private fun extractStaticTables(
        response: AllStaticTableResponse
    ): List<JsonElement> {

        val hasil =
            mutableListOf<JsonElement>()


        val rootData =
            response.data


        if (
            rootData == null ||
            !rootData.isJsonArray
        ) {
            return hasil
        }


        val outerArray =
            rootData.asJsonArray


        if (
            outerArray.size() < 2
        ) {
            return hasil
        }


        val tableData =
            outerArray[1]


        if (
            !tableData.isJsonArray
        ) {
            return hasil
        }


        for (
        table in tableData.asJsonArray
        ) {

            hasil.add(
                table
            )
        }


        return hasil
    }


    // =====================================================
    // TOTAL PAGE STATIC TABLE
    // =====================================================

    private fun getStaticTotalPages(
        response: AllStaticTableResponse
    ): Int {

        val rootData =
            response.data


        if (
            rootData == null ||
            !rootData.isJsonArray
        ) {
            return 1
        }


        val outerArray =
            rootData.asJsonArray


        if (
            outerArray.isEmpty()
        ) {
            return 1
        }


        val pagination =
            outerArray[0]


        if (
            !pagination.isJsonObject
        ) {
            return 1
        }


        return pagination
            .asJsonObject
            .get("pages")
            ?.asInt
            ?: 1
    }


    // =====================================================
    // LOAD VARIABLE
    // =====================================================

    private fun loadVariablePage(
        page: Int
    ) {

        if (!isAdded) {
            return
        }


        Log.d(
            TAG,
            "Mengambil VARIABLE page=$page"
        )


        repository.getAllVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) {
                return@getAllVariables
            }


            requireActivity().runOnUiThread {

                if (error != null) {

                    Log.e(
                        TAG,
                        "Error Variable",
                        error
                    )


                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil Variable: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()


                    selesaiLoadSemuaData()

                    return@runOnUiThread
                }


                if (response == null) {

                    Log.e(
                        TAG,
                        "Response Variable kosong"
                    )


                    selesaiLoadSemuaData()

                    return@runOnUiThread
                }


                val variables =
                    extractVariableTables(
                        response
                    )


                Log.d(
                    TAG,
                    "Variable page $page = ${variables.size} data"
                )


                semuaData.addAll(
                    variables
                )


                repeat(
                    variables.size
                ) {

                    sumberData.add(
                        "VARIABLE"
                    )
                }


                val totalPages =
                    getVariableTotalPages(
                        response
                    )


                Log.d(
                    TAG,
                    "Variable total page=$totalPages"
                )


                if (page < totalPages) {

                    loadVariablePage(
                        page = page + 1
                    )

                } else {

                    Log.d(
                        TAG,
                        "Semua Variable selesai"
                    )


                    selesaiLoadSemuaData()
                }
            }
        }
    }


    // =====================================================
    // EXTRACT VARIABLE
    // =====================================================

    private fun extractVariableTables(
        response: AllVariableResponse
    ): List<JsonElement> {

        val hasil =
            mutableListOf<JsonElement>()


        val rootData =
            response.data


        if (
            rootData == null ||
            !rootData.isJsonArray
        ) {
            return hasil
        }


        val outerArray =
            rootData.asJsonArray


        if (
            outerArray.size() < 2
        ) {
            return hasil
        }


        val variableData =
            outerArray[1]


        if (
            !variableData.isJsonArray
        ) {
            return hasil
        }


        for (
        variable in variableData.asJsonArray
        ) {

            hasil.add(
                variable
            )
        }


        return hasil
    }


    // =====================================================
    // TOTAL PAGE VARIABLE
    // =====================================================

    private fun getVariableTotalPages(
        response: AllVariableResponse
    ): Int {

        val rootData =
            response.data


        if (
            rootData == null ||
            !rootData.isJsonArray
        ) {
            return 1
        }


        val outerArray =
            rootData.asJsonArray


        if (
            outerArray.isEmpty()
        ) {
            return 1
        }


        val pagination =
            outerArray[0]


        if (
            !pagination.isJsonObject
        ) {
            return 1
        }


        return pagination
            .asJsonObject
            .get("pages")
            ?.asInt
            ?: 1
    }


    // =====================================================
    // SELESAI LOAD SEMUA
    // =====================================================

    private fun selesaiLoadSemuaData() {

        if (!isAdded) {
            return
        }


        progressData.visibility =
            View.GONE


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
            "Total data = ${semuaData.size}"
        )

        Log.d(
            TAG,
            "================================"
        )


        tampilkanDataTerbaru()


        // =================================================
        // MATIKAN SPINNER PULL TO REFRESH
        //
        // Hanya berpengaruh jika proses ini berasal
        // dari pull-to-refresh.
        // =================================================

        finishSwipeRefresh()


        // =================================================
        // PERBARUI STATUS REFRESH
        // =================================================

        verticalScrollView?.post {

            updateRefreshState()
        }
    }


    // =====================================================
    // TAMPILKAN DATA TERBARU
    //
    // MAKSIMAL 5 PER KATEGORI
    // =====================================================

    private fun tampilkanDataTerbaru() {

        dataApiContainer.removeAllViews()


        if (
            semuaData.isEmpty()
        ) {

            tampilkanPesan(
                "Tidak ada data yang tersedia."
            )

            return
        }


        val jumlahPerKategori =
            mutableMapOf<String, Int>()


        for (
        index in semuaData.indices
        ) {

            val table =
                semuaData[index]


            if (
                !table.isJsonObject
            ) {
                continue
            }


            val obj =
                table.asJsonObject


            val sumber =
                if (
                    index < sumberData.size
                ) {

                    sumberData[index]

                } else {

                    "SIMDASI"
                }


            val kategori =
                getKategori(
                    obj,
                    sumber
                )


            val key =
                "$sumber|$kategori"


            val jumlah =
                jumlahPerKategori[key]
                    ?: 0


            if (
                jumlah >= 5
            ) {
                continue
            }


            tambahCard(
                table,
                sumber
            )


            jumlahPerKategori[key] =
                jumlah + 1
        }
    }


    // =====================================================
    // GET KATEGORI
    // =====================================================

    private fun getKategori(
        obj: JsonObject,
        sumber: String
    ): String {

        return when (sumber) {

            "SIMDASI" -> {

                getString(
                    obj,
                    "bab"
                ).ifBlank {
                    "Lainnya"
                }
            }


            "STATIC" -> {

                getString(
                    obj,
                    "subj"
                ).ifBlank {
                    "Lainnya"
                }
            }


            "VARIABLE" -> {

                getString(
                    obj,
                    "sub_name"
                ).ifBlank {

                    getString(
                        obj,
                        "subcsa_name"
                    ).ifBlank {
                        "Lainnya"
                    }
                }
            }


            else -> {

                "Lainnya"
            }
        }
    }


    // =====================================================
    // TAMBAH CARD
    //
    // BAGIAN DETAIL KETIKA CARD DIKLIK:
    //
    // SIMDASI  -> StatistikDetailActivity
    // STATIC   -> DetailKategoriActivity
    // VARIABLE -> DetailKategoriActivity
    // =====================================================

    private fun tambahCard(
        table: JsonElement,
        sumber: String
    ) {

        if (
            !table.isJsonObject
        ) {
            return
        }


        val obj =
            table.asJsonObject


        val view =
            layoutInflater.inflate(
                R.layout.item_statistik,
                dataApiContainer,
                false
            )


        val tvKode =
            view.findViewById<TextView>(
                R.id.tvKode
            )


        val tvTahun =
            view.findViewById<TextView>(
                R.id.tvTahun
            )


        val tvKategori =
            view.findViewById<TextView>(
                R.id.tvKategori
            )


        val tvJudul =
            view.findViewById<TextView>(
                R.id.tvJudul
            )


        val tvNilai =
            view.findViewById<TextView>(
                R.id.tvNilai
            )


        // =================================================
        // SEMBUNYIKAN KODE
        // =================================================

        tvKode.visibility =
            View.GONE


        // =================================================
        // SEMBUNYIKAN NILAI
        // =================================================

        tvNilai.visibility =
            View.GONE


        // =================================================
        // SIMDASI
        // =================================================

        if (
            sumber == "SIMDASI"
        ) {

            val idTabel =
                getString(
                    obj,
                    "id_tabel"
                )


            val kode =
                getString(
                    obj,
                    "kode_tabel"
                )


            val judul =
                getString(
                    obj,
                    "judul"
                )


            val bab =
                getString(
                    obj,
                    "bab"
                )


            val tahunTersedia =
                getDaftarTahunSimdasi(
                    obj.get(
                        "ketersediaan_tahun"
                    )
                )


            val tahunTerbaru =
                tahunTersedia.maxOrNull()
                    ?: 0


            // ---------------------------------------------
            // TAMPILAN
            // ---------------------------------------------

            tvTahun.text =
                formatTahunSimdasi(
                    obj.get(
                        "ketersediaan_tahun"
                    )
                )


            tvKategori.text =
                bab.ifBlank {
                    "Statistik"
                }


            tvJudul.text =
                bersihkanHtml(
                    judul
                )


            // ---------------------------------------------
            // KLIK CARD SIMDASI
            // ---------------------------------------------

            view.setOnClickListener {

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "Klik CARD SIMDASI"
                )

                Log.d(
                    TAG,
                    "id_tabel=$idTabel"
                )

                Log.d(
                    TAG,
                    "kode=$kode"
                )

                Log.d(
                    TAG,
                    "tahunTerbaru=$tahunTerbaru"
                )

                Log.d(
                    TAG,
                    "tahunTersedia=$tahunTersedia"
                )

                Log.d(
                    TAG,
                    "================================"
                )


                if (
                    idTabel.isBlank()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "ID tabel tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }


                val intent =
                    Intent(
                        requireContext(),
                        StatistikDetailActivity::class.java
                    )


                intent.putExtra(
                    StatistikDetailActivity.EXTRA_ID_TABEL,
                    idTabel
                )


                intent.putExtra(
                    StatistikDetailActivity.EXTRA_JUDUL,
                    bersihkanHtml(
                        judul
                    )
                )


                intent.putExtra(
                    StatistikDetailActivity.EXTRA_KODE,
                    kode
                )


                if (
                    tahunTerbaru > 0
                ) {

                    intent.putExtra(
                        StatistikDetailActivity.EXTRA_TAHUN,
                        tahunTerbaru
                    )
                }


                intent.putIntegerArrayListExtra(
                    StatistikDetailActivity.EXTRA_KETERSEDIAAN_TAHUN,
                    ArrayList(
                        tahunTersedia
                    )
                )


                startActivity(
                    intent
                )
            }
        }


        // =================================================
        // STATIC TABLE
        // =================================================

        else if (
            sumber == "STATIC"
        ) {

            val id =
                getString(
                    obj,
                    "table_id"
                )


            val title =
                getString(
                    obj,
                    "title"
                )


            val subject =
                getString(
                    obj,
                    "subj"
                )


            val tahun =
                getTahunStatic(
                    obj
                )


            tvTahun.text =
                tahun


            tvKategori.text =
                subject.ifBlank {
                    "Statistik"
                }


            tvJudul.text =
                bersihkanHtml(
                    title
                )


            view.setOnClickListener {

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "Klik CARD STATIC"
                )

                Log.d(
                    TAG,
                    "table_id=$id"
                )

                Log.d(
                    TAG,
                    "================================"
                )


                if (
                    id.isBlank()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "ID tabel tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }


                val intent =
                    Intent(
                        requireContext(),
                        DetailKategoriActivity::class.java
                    )


                intent.putExtra(
                    "ID",
                    id
                )


                intent.putExtra(
                    "TYPE",
                    "STATIC"
                )


                intent.putExtra(
                    "JUDUL",
                    bersihkanHtml(
                        title
                    )
                )


                intent.putExtra(
                    "KATEGORI",
                    subject.ifBlank {
                        "Statistik"
                    }
                )


                intent.putExtra(
                    "SUMBER",
                    "BPS Kota Probolinggo"
                )


                startActivity(
                    intent
                )
            }
        }


        // =================================================
        // VARIABLE
        // =================================================

        else if (
            sumber == "VARIABLE"
        ) {

            val id =
                getString(
                    obj,
                    "var_id"
                )


            val title =
                getString(
                    obj,
                    "title"
                )


            val kategori =
                getString(
                    obj,
                    "sub_name"
                ).ifBlank {

                    getString(
                        obj,
                        "subcsa_name"
                    )
                }


            val tahun =
                getTahunVariable(
                    obj
                )


            tvTahun.text =
                tahun


            tvKategori.text =
                kategori.ifBlank {
                    "Variabel"
                }


            tvJudul.text =
                bersihkanHtml(
                    title
                )


            view.setOnClickListener {

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "Klik CARD VARIABLE"
                )

                Log.d(
                    TAG,
                    "var_id=$id"
                )

                Log.d(
                    TAG,
                    "tahun=$tahun"
                )

                Log.d(
                    TAG,
                    "================================"
                )


                if (
                    id.isBlank()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "ID variable tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }


                val intent =
                    Intent(
                        requireContext(),
                        DetailKategoriActivity::class.java
                    )


                intent.putExtra(
                    "ID",
                    id
                )


                intent.putExtra(
                    "TYPE",
                    "VARIABLE"
                )


                intent.putExtra(
                    "JUDUL",
                    bersihkanHtml(
                        title
                    )
                )


                intent.putExtra(
                    "KATEGORI",
                    kategori.ifBlank {
                        "Variabel"
                    }
                )


                intent.putExtra(
                    "SUMBER",
                    "BPS Kota Probolinggo"
                )


                val tahunInt =
                    getTahunVariableAsInt(
                        obj
                    )


                if (
                    tahunInt > 0
                ) {

                    intent.putExtra(
                        "TAHUN",
                        tahunInt
                    )
                }


                startActivity(
                    intent
                )
            }
        }


        // =================================================
        // MASUKKAN CARD
        // =================================================

        dataApiContainer.addView(
            view
        )
    }


    // =====================================================
    // DAFTAR TAHUN SIMDASI
    // =====================================================

    private fun getDaftarTahunSimdasi(
        tahunArray: JsonElement?
    ): List<Int> {

        if (
            tahunArray == null ||
            !tahunArray.isJsonArray
        ) {
            return emptyList()
        }


        return tahunArray
            .asJsonArray
            .mapNotNull { item ->

                try {

                    item.asInt

                } catch (
                    e: Exception
                ) {

                    null
                }
            }
            .filter {
                it > 0
            }
            .distinct()
            .sorted()
    }


    // =====================================================
    // FORMAT TAHUN SIMDASI
    // =====================================================

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


        val tahun =
            tahunArray
                .asJsonArray
                .mapNotNull { item ->

                    try {

                        item.asInt

                    } catch (
                        e: Exception
                    ) {

                        null
                    }
                }
                .sorted()


        if (
            tahun.isEmpty()
        ) {
            return "-"
        }


        return if (
            tahun.size == 1
        ) {

            tahun.first().toString()

        } else {

            "${tahun.first()} - ${tahun.last()}"
        }
    }


    // =====================================================
    // TAHUN STATIC TABLE
    // =====================================================

    private fun getTahunStatic(
        obj: JsonObject
    ): String {

        val tahun =
            getString(
                obj,
                "tahun"
            )


        if (
            tahun.isNotBlank()
        ) {

            return formatTahunBps(
                tahun
            )
        }


        val updateDate =
            getString(
                obj,
                "updt_date"
            )


        if (
            updateDate.isNotBlank()
        ) {

            return formatTahunBps(
                updateDate
            )
        }


        return "-"
    }


    // =====================================================
    // TAHUN VARIABLE
    // =====================================================

    private fun getTahunVariable(
        obj: JsonObject
    ): String {

        val kemungkinanField =
            listOf(
                "tahun",
                "year",
                "th",
                "periode"
            )


        for (
        field in kemungkinanField
        ) {

            val value =
                getString(
                    obj,
                    field
                )


            if (
                value.isNotBlank()
            ) {

                Log.d(
                    TAG,
                    "Variable tahun field=$field value=$value"
                )


                return formatTahunBps(
                    value
                )
            }
        }


        return "-"
    }


    // =====================================================
    // TAHUN VARIABLE -> INT
    // =====================================================

    private fun getTahunVariableAsInt(
        obj: JsonObject
    ): Int {

        val kemungkinanField =
            listOf(
                "tahun",
                "year",
                "th",
                "periode"
            )


        for (
        field in kemungkinanField
        ) {

            val value =
                getString(
                    obj,
                    field
                )


            if (
                value.isBlank()
            ) {
                continue
            }


            // ---------------------------------------------
            // ANGKA
            // ---------------------------------------------

            val angka =
                value
                    .trim()
                    .toIntOrNull()


            if (
                angka != null
            ) {

                return when {

                    angka in 0..199 ->
                        angka + 1900

                    angka in 1900..2100 ->
                        angka

                    else ->
                        0
                }
            }


            // ---------------------------------------------
            // TANGGAL
            // ---------------------------------------------

            if (
                value.matches(
                    Regex(
                        "^\\d{4}-\\d{2}-\\d{2}.*$"
                    )
                )
            ) {

                return value
                    .substring(
                        0,
                        4
                    )
                    .toIntOrNull()
                    ?: 0
            }
        }


        return 0
    }


    // =====================================================
    // FORMAT TAHUN BPS
    //
    // 125  -> 2025
    // 126  -> 2026
    //
    // 2025 -> 2025
    //
    // 2025-04-28 -> 2025
    // =====================================================

    private fun formatTahunBps(
        value: String?
    ): String {

        if (
            value.isNullOrBlank()
        ) {
            return "-"
        }


        val text =
            value.trim()


        // =================================================
        // TANGGAL
        // =================================================

        if (
            text.matches(
                Regex(
                    "^\\d{4}-\\d{2}-\\d{2}.*$"
                )
            )
        ) {

            return text.substring(
                0,
                4
            )
        }


        // =================================================
        // ANGKA
        // =================================================

        val angka =
            text.toIntOrNull()


        if (
            angka != null
        ) {

            return when {

                angka in 0..199 -> {

                    (angka + 1900).toString()
                }


                angka in 1900..2100 -> {

                    angka.toString()
                }


                else -> {

                    text
                }
            }
        }


        return text
    }


    // =====================================================
    // BERSIHKAN HTML
    // =====================================================

    private fun bersihkanHtml(
        text: String
    ): String {

        if (
            text.isBlank()
        ) {
            return ""
        }


        return text

            .replace(
                Regex(
                    "<sup[^>]*>.*?</sup>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )

            .replace(
                Regex(
                    "<sub[^>]*>.*?</sub>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )

            .replace(
                Regex(
                    "<[^>]*>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )

            .replace(
                "&nbsp;",
                " "
            )

            .replace(
                "&amp;",
                "&"
            )

            .replace(
                "&lt;",
                "<"
            )

            .replace(
                "&gt;",
                ">"
            )

            .replace(
                "&quot;",
                "\""
            )

            .replace(
                "&#39;",
                "'"
            )

            .replace(
                Regex("\\s+"),
                " "
            )

            .trim()
    }


    // =====================================================
    // GET STRING DARI JSON
    // =====================================================

    private fun getString(
        obj: JsonObject,
        key: String
    ): String {

        return try {

            obj.get(key)
                ?.takeIf {
                    !it.isJsonNull
                }
                ?.asString
                ?: ""

        } catch (
            e: Exception
        ) {

            ""
        }
    }


    // =====================================================
    // SEARCH
    // =====================================================

    private fun filterData(
        keyword: String
    ) {

        val query =
            keyword
                .trim()
                .lowercase()


        // =================================================
        // KALAU KOSONG
        // =================================================

        if (
            query.isEmpty()
        ) {

            tampilkanDataTerbaru()

            return
        }


        dataApiContainer.removeAllViews()


        var jumlahHasil =
            0


        // =================================================
        // LOOP SEMUA DATA
        // =================================================

        for (
        index in semuaData.indices
        ) {

            val table =
                semuaData[index]


            if (
                !table.isJsonObject
            ) {
                continue
            }


            val obj =
                table.asJsonObject


            val sumber =
                if (
                    index < sumberData.size
                ) {

                    sumberData[index]

                } else {

                    "SIMDASI"
                }


            val cocok =
                when (sumber) {

                    // =====================================
                    // SIMDASI
                    // =====================================

                    "SIMDASI" -> {

                        val judul =
                            getString(
                                obj,
                                "judul"
                            ).lowercase()


                        val kode =
                            getString(
                                obj,
                                "kode_tabel"
                            ).lowercase()


                        val bab =
                            getString(
                                obj,
                                "bab"
                            ).lowercase()


                        judul.contains(query) ||
                                kode.contains(query) ||
                                bab.contains(query)
                    }


                    // =====================================
                    // STATIC TABLE
                    // =====================================

                    "STATIC" -> {

                        val title =
                            getString(
                                obj,
                                "title"
                            ).lowercase()


                        val tableId =
                            getString(
                                obj,
                                "table_id"
                            ).lowercase()


                        val subject =
                            getString(
                                obj,
                                "subj"
                            ).lowercase()


                        title.contains(query) ||
                                tableId.contains(query) ||
                                subject.contains(query)
                    }


                    // =====================================
                    // VARIABLE
                    // =====================================

                    "VARIABLE" -> {

                        val title =
                            getString(
                                obj,
                                "title"
                            ).lowercase()


                        val varId =
                            getString(
                                obj,
                                "var_id"
                            ).lowercase()


                        val subName =
                            getString(
                                obj,
                                "sub_name"
                            ).lowercase()


                        val subcsaName =
                            getString(
                                obj,
                                "subcsa_name"
                            ).lowercase()


                        title.contains(query) ||
                                varId.contains(query) ||
                                subName.contains(query) ||
                                subcsaName.contains(query)
                    }


                    else -> {

                        false
                    }
                }


            // =================================================
            // KALAU COCOK
            // =================================================

            if (
                cocok
            ) {

                tambahCard(
                    table,
                    sumber
                )


                jumlahHasil++
            }
        }


        // =================================================
        // TIDAK ADA HASIL
        // =================================================

        if (
            jumlahHasil == 0
        ) {

            tampilkanPesan(
                "Data tidak ditemukan."
            )
        }


        Log.d(
            TAG,
            "Hasil pencarian=$jumlahHasil"
        )
    }


    // =====================================================
    // TAMPILKAN PESAN
    // =====================================================

    private fun tampilkanPesan(
        pesan: String
    ) {

        val tv =
            TextView(
                requireContext()
            )


        tv.text =
            pesan


        tv.textSize =
            14f


        tv.setTextColor(
            android.graphics.Color.GRAY
        )


        tv.setPadding(
            0,
            24,
            0,
            24
        )


        dataApiContainer.addView(
            tv
        )
    }


    // =====================================================
    // DESTROY VIEW
    // =====================================================

    override fun onDestroyView() {

        // Pastikan spinner tidak tertinggal aktif ketika
        // Fragment dihancurkan.
        if (isAdded) {
            finishSwipeRefresh()
        }

        verticalScrollView =
            null

        semuaData.clear()

        sumberData.clear()

        super.onDestroyView()
    }
}