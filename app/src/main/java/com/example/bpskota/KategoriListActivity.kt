package com.example.bpskota

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.AllSimdasiResponse
import com.example.bpskota.bps.model.AllStaticTableResponse
import com.example.bpskota.bps.model.AllVariableResponse
import com.example.bpskota.bps.repository.BpsAllDataRepository
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class KategoriListActivity : AppCompatActivity() {

    companion object {

        private const val TAG =
            "KategoriListActivity"

        private const val WILAYAH =
            "3574000"

        private const val DOMAIN =
            "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"
    }


    // =====================================================
    // VIEW
    // =====================================================

    private lateinit var kategoriContainer: LinearLayout

    private lateinit var etSearchKategori: EditText

    private lateinit var lottieLoading: LottieAnimationView


    // =====================================================
    // REPOSITORY
    // =====================================================

    private val repository =
        BpsAllDataRepository()


    // =====================================================
    // DATA KATEGORI
    // =====================================================

    private val daftarKategori =
        mutableListOf<KategoriItem>()


    /**
     * Menyimpan kombinasi:
     *
     * SIMDASI|pemerintahan
     * STATIC|hotel dan akomodasi
     * VARIABLE|masyarakat informasi
     *
     * Digunakan untuk mencegah duplikat.
     */
    private val kategoriSudahAda =
        mutableSetOf<String>()


    // =====================================================
    // STATUS LOAD
    // =====================================================

    /**
     * Karena 3 API dijalankan bersamaan,
     * kita perlu mengetahui kapan ketiganya selesai.
     */
    private var simdasiSelesai = false

    private var staticSelesai = false

    private var variableSelesai = false


    // =====================================================
    // MODEL
    // =====================================================

    data class KategoriItem(
        val nama: String,
        val sumber: String
    )


    // =====================================================
    // CREATE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_kategori_list
        )


        // =================================================
        // FIND VIEW
        // =================================================

        kategoriContainer =
            findViewById(
                R.id.kategoriContainer
            )

        etSearchKategori =
            findViewById(
                R.id.etSearchKategori
            )

        lottieLoading =
            findViewById(
                R.id.lottieLoading
            )


        val btnBack =
            findViewById<View>(
                R.id.btnBack
            )


        // =================================================
        // BACK
        // =================================================

        btnBack.setOnClickListener {

            finish()
        }


        // =================================================
        // LOTTIE
        // =================================================

        mulaiLoading()


        // =================================================
        // SEARCH
        // =================================================

        setupSearch()


        // =================================================
        // LOAD DATA
        // =================================================

        loadSemuaKategori()
    }


    // =====================================================
    // LOTTIE LOADING
    // =====================================================

    private fun mulaiLoading() {

        lottieLoading.visibility =
            View.VISIBLE

        lottieLoading.setAnimation(
            "Loading_Animation.json"
        )

        lottieLoading.repeatCount =
            -1

        lottieLoading.playAnimation()
    }


    private fun selesaiLoading() {

        lottieLoading.cancelAnimation()

        lottieLoading.visibility =
            View.GONE
    }


    // =====================================================
    // SEARCH
    // =====================================================

    private fun setupSearch() {

        etSearchKategori.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    filterKategori(
                        s?.toString()
                            ?: ""
                    )
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }


    // =====================================================
    // LOAD SEMUA KATEGORI
    //
    // 3 API DIJALANKAN BERSAMAAN
    // =====================================================

    private fun loadSemuaKategori() {

        daftarKategori.clear()

        kategoriSudahAda.clear()


        simdasiSelesai =
            false

        staticSelesai =
            false

        variableSelesai =
            false


        kategoriContainer.removeAllViews()


        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "MULAI LOAD SEMUA KATEGORI"
        )

        Log.d(
            TAG,
            "SIMDASI + STATIC + VARIABLE"
        )

        Log.d(
            TAG,
            "DIJALANKAN PARALEL"
        )

        Log.d(
            TAG,
            "================================"
        )


        /**
         * PENTING:
         *
         * Ketiga request pertama langsung
         * dijalankan tanpa menunggu yang lain.
         */

        loadSimdasiPage(1)

        loadStaticTablePage(1)

        loadVariablePage(1)
    }


    // =====================================================
    // SIMDASI
    // =====================================================

    private fun loadSimdasiPage(
        page: Int
    ) {

        Log.d(
            TAG,
            "SIMDASI request page=$page"
        )


        repository.getAllSimdasi(
            wilayah = WILAYAH,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (error != null) {

                    Log.e(
                        TAG,
                        "SIMDASI error page=$page",
                        error
                    )

                    /**
                     * Jangan menghentikan sumber lain.
                     * Tandai SIMDASI selesai.
                     */
                    simdasiSelesai = true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }


                if (response == null) {

                    Log.e(
                        TAG,
                        "SIMDASI response null"
                    )

                    simdasiSelesai = true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }


                // -----------------------------------------
                // PROSES DATA
                // -----------------------------------------

                prosesSimdasi(
                    response
                )


                // -----------------------------------------
                // CEK PAGE
                // -----------------------------------------

                val totalPages =
                    getSimdasiTotalPages(
                        response
                    )


                Log.d(
                    TAG,
                    "SIMDASI page=$page/$totalPages"
                )


                if (
                    page < totalPages
                ) {

                    loadSimdasiPage(
                        page + 1
                    )

                } else {

                    Log.d(
                        TAG,
                        "SIMDASI SELESAI"
                    )

                    simdasiSelesai =
                        true

                    cekSemuaSelesai()
                }
            }
        }
    }


    // =====================================================
    // PROSES SIMDASI
    // =====================================================

    private fun prosesSimdasi(
        response: AllSimdasiResponse
    ) {

        try {

            val rootData =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return


            if (
                rootData.size() < 2
            ) {
                return
            }


            val wrapper =
                rootData[1]
                    .takeIf {
                        it.isJsonObject
                    }
                    ?.asJsonObject
                    ?: return


            val data =
                wrapper
                    .get("data")
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return


            for (
            element in data
            ) {

                if (
                    !element.isJsonObject
                ) {
                    continue
                }


                val obj =
                    element.asJsonObject


                val mmsSubject =
                    getString(
                        obj,
                        "mms_subject"
                    )


                if (
                    mmsSubject.isBlank()
                ) {
                    continue
                }


                tambahKategori(
                    nama = mmsSubject,
                    sumber = "SIMDASI"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Gagal proses SIMDASI",
                e
            )
        }
    }


    // =====================================================
    // TOTAL PAGE SIMDASI
    // =====================================================

    private fun getSimdasiTotalPages(
        response: AllSimdasiResponse
    ): Int {

        return try {

            val data =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return 1


            if (
                data.isEmpty()
            ) {
                return 1
            }


            data[0]
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.get("pages")
                ?.asInt
                ?: 1

        } catch (
            e: Exception
        ) {

            1
        }
    }


    // =====================================================
    // STATIC TABLE
    // =====================================================

    private fun loadStaticTablePage(
        page: Int
    ) {

        Log.d(
            TAG,
            "STATIC request page=$page"
        )


        repository.getAllStaticTables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (error != null) {

                    Log.e(
                        TAG,
                        "STATIC error page=$page",
                        error
                    )

                    staticSelesai =
                        true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }


                if (response == null) {

                    staticSelesai =
                        true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }


                // -----------------------------------------
                // PROSES DATA
                // -----------------------------------------

                prosesStaticTable(
                    response
                )


                // -----------------------------------------
                // PAGE
                // -----------------------------------------

                val totalPages =
                    getStaticTotalPages(
                        response
                    )


                Log.d(
                    TAG,
                    "STATIC page=$page/$totalPages"
                )


                if (
                    page < totalPages
                ) {

                    loadStaticTablePage(
                        page + 1
                    )

                } else {

                    Log.d(
                        TAG,
                        "STATIC SELESAI"
                    )

                    staticSelesai =
                        true

                    cekSemuaSelesai()
                }
            }
        }
    }


    // =====================================================
    // PROSES STATIC TABLE
    // =====================================================

    private fun prosesStaticTable(
        response: AllStaticTableResponse
    ) {

        try {

            val rootData =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return


            if (
                rootData.size() < 2
            ) {
                return
            }


            val data =
                rootData[1]
                    .takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return


            for (
            element in data
            ) {

                if (
                    !element.isJsonObject
                ) {
                    continue
                }


                val obj =
                    element.asJsonObject


                val subj =
                    getString(
                        obj,
                        "subj"
                    )


                if (
                    subj.isBlank()
                ) {
                    continue
                }


                tambahKategori(
                    nama = subj,
                    sumber = "STATIC"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Gagal proses Static Table",
                e
            )
        }
    }


    // =====================================================
    // TOTAL PAGE STATIC
    // =====================================================

    private fun getStaticTotalPages(
        response: AllStaticTableResponse
    ): Int {

        return try {

            val data =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return 1


            if (
                data.isEmpty()
            ) {
                return 1
            }


            data[0]
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.get("pages")
                ?.asInt
                ?: 1

        } catch (
            e: Exception
        ) {

            1
        }
    }


    // =====================================================
    // VARIABLE
    // =====================================================

    private fun loadVariablePage(
        page: Int
    ) {

        Log.d(
            TAG,
            "VARIABLE request page=$page"
        )


        repository.getAllVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (error != null) {

                    Log.e(
                        TAG,
                        "VARIABLE error page=$page",
                        error
                    )

                    variableSelesai =
                        true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }


                if (response == null) {

                    variableSelesai =
                        true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }


                // -----------------------------------------
                // PROSES DATA
                // -----------------------------------------

                prosesVariable(
                    response
                )


                // -----------------------------------------
                // PAGE
                // -----------------------------------------

                val totalPages =
                    getVariableTotalPages(
                        response
                    )


                Log.d(
                    TAG,
                    "VARIABLE page=$page/$totalPages"
                )


                if (
                    page < totalPages
                ) {

                    loadVariablePage(
                        page + 1
                    )

                } else {

                    Log.d(
                        TAG,
                        "VARIABLE SELESAI"
                    )

                    variableSelesai =
                        true

                    cekSemuaSelesai()
                }
            }
        }
    }


    // =====================================================
    // PROSES VARIABLE
    // =====================================================

    private fun prosesVariable(
        response: AllVariableResponse
    ) {

        try {

            val rootData =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return


            if (
                rootData.size() < 2
            ) {
                return
            }


            val data =
                rootData[1]
                    .takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return


            for (
            element in data
            ) {

                if (
                    !element.isJsonObject
                ) {
                    continue
                }


                val obj =
                    element.asJsonObject


                /**
                 * VARIABLE:
                 *
                 * HANYA subcsa_name
                 */
                val subcsaName =
                    getString(
                        obj,
                        "subcsa_name"
                    )


                if (
                    subcsaName.isBlank()
                ) {
                    continue
                }


                tambahKategori(
                    nama = subcsaName,
                    sumber = "VARIABLE"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Gagal proses Variable",
                e
            )
        }
    }


    // =====================================================
    // TOTAL PAGE VARIABLE
    // =====================================================

    private fun getVariableTotalPages(
        response: AllVariableResponse
    ): Int {

        return try {

            val data =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return 1


            if (
                data.isEmpty()
            ) {
                return 1
            }


            data[0]
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.get("pages")
                ?.asInt
                ?: 1

        } catch (
            e: Exception
        ) {

            1
        }
    }


    // =====================================================
    // TAMBAH KATEGORI
    // =====================================================

    private fun tambahKategori(
        nama: String,
        sumber: String
    ) {

        val namaBersih =
            nama
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )


        if (
            namaBersih.isBlank()
        ) {
            return
        }


        /**
         * Duplikat dihitung dalam sumber yang sama.
         *
         * Contoh:
         *
         * SIMDASI|Pemerintahan
         * SIMDASI|Pemerintahan
         *
         * Yang kedua tidak masuk.
         */
        val key =
            "$sumber|${namaBersih.lowercase()}"


        if (
            !kategoriSudahAda.add(key)
        ) {

            Log.d(
                TAG,
                "Duplikat dilewati: $key"
            )

            return
        }


        daftarKategori.add(
            KategoriItem(
                nama = namaBersih,
                sumber = sumber
            )
        )


        Log.d(
            TAG,
            "Kategori baru: $sumber → $namaBersih"
        )
    }


    // =====================================================
    // CEK SEMUA API SELESAI
    // =====================================================

    private fun cekSemuaSelesai() {

        Log.d(
            TAG,
            "Status:"
        )

        Log.d(
            TAG,
            "SIMDASI = $simdasiSelesai"
        )

        Log.d(
            TAG,
            "STATIC = $staticSelesai"
        )

        Log.d(
            TAG,
            "VARIABLE = $variableSelesai"
        )


        if (
            !simdasiSelesai ||
            !staticSelesai ||
            !variableSelesai
        ) {
            return
        }


        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "SEMUA API SELESAI"
        )

        Log.d(
            TAG,
            "Total kategori = ${daftarKategori.size}"
        )

        Log.d(
            TAG,
            "================================"
        )


        selesaiLoading()

        tampilkanKategori()
    }


    // =====================================================
    // TAMPILKAN SEMUA KATEGORI
    // =====================================================

    private fun tampilkanKategori() {

        kategoriContainer.removeAllViews()


        if (
            daftarKategori.isEmpty()
        ) {

            tampilkanPesan(
                "Tidak ada kategori yang ditemukan."
            )

            return
        }


        for (
        kategori in daftarKategori
        ) {

            tambahCardKategori(
                kategori
            )
        }
    }


    // =====================================================
    // CARD KATEGORI
    // =====================================================

    private fun tambahCardKategori(item: KategoriItem) {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_statistik, kategoriContainer, false)

        val tvNama = view.findViewById<TextView>(R.id.tvJudul)
        val tvSumber = view.findViewById<TextView>(R.id.tvKategori)

        tvNama.text = item.nama
        tvSumber.text = item.sumber

        view.setOnClickListener {

            val intent = Intent(
                this,
                DataKategoriActivity::class.java
            )

            intent.putExtra(
                "NAMA_KATEGORI",
                item.nama
            )

            startActivity(intent)
        }

        kategoriContainer.addView(view)
    }
    // =====================================================
    // SEARCH KATEGORI
    // =====================================================

    private fun filterKategori(
        keyword: String
    ) {

        /**
         * Kalau data masih loading,
         * jangan tampilkan hasil search.
         */
        if (
            !simdasiSelesai ||
            !staticSelesai ||
            !variableSelesai
        ) {
            return
        }


        val query =
            keyword
                .trim()
                .lowercase()


        kategoriContainer.removeAllViews()


        // =================================================
        // SEARCH KOSONG
        // =================================================

        if (
            query.isBlank()
        ) {

            tampilkanKategori()

            return
        }


        var jumlahHasil =
            0


        for (
        kategori in daftarKategori
        ) {

            val cocok =
                kategori.nama
                    .lowercase()
                    .contains(query)


            if (
                cocok
            ) {

                tambahCardKategori(
                    kategori
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
                "Kategori tidak ditemukan."
            )
        }


        Log.d(
            TAG,
            "Search '$query' = $jumlahHasil hasil"
        )
    }


    // =====================================================
    // GET STRING
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
                ?.trim()
                ?: ""

        } catch (
            e: Exception
        ) {

            ""
        }
    }


    // =====================================================
    // PESAN
    // =====================================================

    private fun tampilkanPesan(
        pesan: String
    ) {

        val tv =
            TextView(this)


        tv.text =
            pesan


        tv.textSize =
            14f


        tv.setTextColor(
            Color.GRAY
        )


        tv.setPadding(
            8,
            24,
            8,
            24
        )


        kategoriContainer.addView(
            tv
        )
    }


    // =====================================================
    // CLEANUP
    // =====================================================

    override fun onDestroy() {

        if (
            ::lottieLoading.isInitialized
        ) {

            lottieLoading.cancelAnimation()
        }

        super.onDestroy()
    }
}