package com.example.bpskota

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.JsonObject

class KategoriListActivity : AppCompatActivity() {

    companion object {

        private const val WILAYAH =
            "3574000"

        private const val DOMAIN =
            "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"
    }

    private lateinit var kategoriContainer: LinearLayout

    private lateinit var etSearchKategori: EditText

    private lateinit var lottieLoading: LottieAnimationView

    private lateinit var activityTracker: ActivityTracker

    private val repository =
        BpsAllDataRepository()

    private val daftarKategori =
        mutableListOf<KategoriItem>()

    private val kategoriSudahAda =
        mutableSetOf<String>()

    private var simdasiSelesai = false

    private var staticSelesai = false

    private var variableSelesai = false

    data class KategoriItem(
        val nama: String,
        val sumber: String
    )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_kategori_list
        )

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

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "KategoriList"
        )

        btnBack.setOnClickListener {

            finish()
        }

        mulaiLoading()

        setupSearch()

        loadSemuaKategori()
    }

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

        loadSimdasiPage(1)

        loadStaticTablePage(1)

        loadVariablePage(1)
    }

    private fun loadSimdasiPage(
        page: Int
    ) {

        repository.getAllSimdasi(
            wilayah = WILAYAH,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (error != null) {

                    simdasiSelesai =
                        true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }

                if (response == null) {

                    simdasiSelesai =
                        true

                    cekSemuaSelesai()

                    return@runOnUiThread
                }

                prosesSimdasi(
                    response
                )

                val totalPages =
                    getSimdasiTotalPages(
                        response
                    )

                if (
                    page < totalPages
                ) {

                    loadSimdasiPage(
                        page + 1
                    )

                } else {

                    simdasiSelesai =
                        true

                    cekSemuaSelesai()
                }
            }
        }
    }

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
        }
    }

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

    private fun loadStaticTablePage(
        page: Int
    ) {

        repository.getAllStaticTables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (error != null) {

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

                prosesStaticTable(
                    response
                )

                val totalPages =
                    getStaticTotalPages(
                        response
                    )

                if (
                    page < totalPages
                ) {

                    loadStaticTablePage(
                        page + 1
                    )

                } else {

                    staticSelesai =
                        true

                    cekSemuaSelesai()
                }
            }
        }
    }

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
        }
    }

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

    private fun loadVariablePage(
        page: Int
    ) {

        repository.getAllVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (error != null) {

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

                prosesVariable(
                    response
                )

                val totalPages =
                    getVariableTotalPages(
                        response
                    )

                if (
                    page < totalPages
                ) {

                    loadVariablePage(
                        page + 1
                    )

                } else {

                    variableSelesai =
                        true

                    cekSemuaSelesai()
                }
            }
        }
    }

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
        }
    }

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

        val key =
            "$sumber|${namaBersih.lowercase()}"

        if (
            !kategoriSudahAda.add(key)
        ) {
            return
        }

        daftarKategori.add(
            KategoriItem(
                nama = namaBersih,
                sumber = sumber
            )
        )
    }

    private fun cekSemuaSelesai() {

        if (
            !simdasiSelesai ||
            !staticSelesai ||
            !variableSelesai
        ) {
            return
        }

        selesaiLoading()

        activityTracker.trackScreen(
            screen = "KategoriList",
            metadata = mapOf(
                "total_kategori" to daftarKategori.size
            )
        )

        tampilkanKategori()
    }

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

    private fun tambahCardKategori(
        item: KategoriItem
    ) {

        val view =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    kategoriContainer,
                    false
                )

        val tvNama =
            view.findViewById<TextView>(
                R.id.tvJudul
            )

        val tvSumber =
            view.findViewById<TextView>(
                R.id.tvKategori
            )

        tvNama.text =
            item.nama

        tvSumber.text =
            item.sumber

        view.setOnClickListener {

            val intent =
                Intent(
                    this,
                    DataKategoriActivity::class.java
                )

            intent.putExtra(
                "NAMA_KATEGORI",
                item.nama
            )

            startActivity(intent)
        }

        kategoriContainer.addView(
            view
        )
    }

    private fun filterKategori(
        keyword: String
    ) {

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

        if (
            jumlahHasil == 0
        ) {

            tampilkanPesan(
                "Kategori tidak ditemukan."
            )
        }
    }

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

    override fun onDestroy() {

        if (
            ::lottieLoading.isInitialized
        ) {

            lottieLoading.cancelAnimation()
        }

        super.onDestroy()
    }
}