package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.bpskota.bps.repository.BpsAllDataRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker

class DataKategoriActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DataKategoriActivity"
        private const val WILAYAH = "3574000"
        private const val DOMAIN = "3574"
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
    }

    private lateinit var tvJudulKategori: TextView
    private lateinit var etSearchData: EditText
    private lateinit var dataContainer: LinearLayout

    private lateinit var activityTracker: ActivityTracker

    private val repository = BpsAllDataRepository()
    private var namaKategori = ""

    private val semuaData = mutableListOf<DataItem>()
    private val dataTampil = mutableListOf<DataItem>()

    data class DataItem(
        val judul: String,
        val kategori: String,
        val sumber: String,
        val id: String,
        val tahun: Int? = null,
        val ketersediaanTahun: List<Int> = emptyList(),
        val type: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_kategori)

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen("DataKategori")

        namaKategori = intent.getStringExtra("NAMA_KATEGORI") ?: ""

        if (namaKategori.isEmpty()) {
            Log.e(TAG, "NAMA_KATEGORI kosong!")
            return
        }

        initView()
        setupHeader()
        setupSearch()
        loadSemuaData()
    }

    private fun initView() {
        tvJudulKategori = findViewById(R.id.tvJudulKategori)
        etSearchData = findViewById(R.id.etSearchData)
        dataContainer = findViewById(R.id.dataContainer)
        tvJudulKategori.text = namaKategori
    }

    private fun setupHeader() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupSearch() {
        etSearchData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterData(s?.toString()?.trim() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadSemuaData() {
        semuaData.clear()
        dataTampil.clear()
        dataContainer.removeAllViews()

        // Panggil data halaman 1 secara paralel
        loadSimdasiPage(1)
        loadStaticTablePage(1)
        loadVariablePage(1)
    }

    private fun loadSimdasiPage(page: Int) {
        repository.getAllSimdasi(
            wilayah = WILAYAH,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (
                error != null ||
                response?.data == null ||
                response.status != "OK"
            ) {
                return@getAllSimdasi
            }

            // Parsing dilakukan di Background Thread agar UI tidak freeze
            val rootData = response.data

            if (!rootData.isJsonArray) {
                return@getAllSimdasi
            }

            val dataArray = rootData.asJsonArray

            val pagination =
                dataArray
                    .firstOrNull()
                    ?.takeIf { it.isJsonObject }
                    ?.asJsonObject

            val totalPages =
                pagination
                    ?.get("pages")
                    ?.takeIf { !it.isJsonNull }
                    ?.let {
                        runCatching {
                            it.asInt
                        }.getOrNull()
                    }
                    ?: 1

            val wrapper =
                if (
                    dataArray.size() > 1 &&
                    dataArray[1].isJsonObject
                ) {
                    dataArray[1].asJsonObject
                } else {
                    null
                }

            val dataList =
                wrapper
                    ?.get("data")
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray

            val newItems = mutableListOf<DataItem>()

            dataList?.forEach { element ->

                if (!element.isJsonObject) {
                    return@forEach
                }

                val obj = element.asJsonObject

                val kategori =
                    obj.get("mms_subject")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString
                        ?.trim()
                        ?: ""

                if (
                    !kategori.equals(
                        namaKategori,
                        ignoreCase = true
                    )
                ) {
                    return@forEach
                }

                val judul =
                    obj.get("judul")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString
                        ?.trim()
                        ?: ""

                val idTabel =
                    obj.get("id_tabel")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString
                        ?.trim()
                        ?: ""

                if (
                    judul.isEmpty() ||
                    idTabel.isEmpty()
                ) {
                    return@forEach
                }

                val tahunTersedia =
                    obj.get("ketersediaan_tahun")
                        ?.takeIf { it.isJsonArray }
                        ?.asJsonArray
                        ?.mapNotNull {
                            runCatching {
                                if (it.isJsonPrimitive) {
                                    it.asInt
                                } else {
                                    null
                                }
                            }.getOrNull()
                        }
                        ?.filter { it > 0 }
                        ?.distinct()
                        ?.sortedDescending()
                        ?: emptyList()

                if (tahunTersedia.isEmpty()) {
                    return@forEach
                }

                newItems.add(
                    DataItem(
                        judul = judul,
                        kategori = kategori,
                        sumber = "SIMDASI",
                        id = idTabel,
                        tahun = tahunTersedia.first(),
                        ketersediaanTahun = tahunTersedia,
                        type = "SIMDASI"
                    )
                )
            }

            // Kembalikan ke Main Thread HANYA untuk update UI
            runOnUiThread {
                tambahDataBaru(newItems)

                if (page < totalPages) {
                    loadSimdasiPage(page + 1)
                }
            }
        }
    }

    private fun loadStaticTablePage(page: Int) {
        repository.getAllStaticTables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (
                error != null ||
                response?.data == null ||
                response.status != "OK"
            ) {
                return@getAllStaticTables
            }

            val rootData = response.data
            val dataArray = rootData.asJsonArray

            val pagination =
                dataArray
                    .firstOrNull()
                    ?.takeIf { it.isJsonObject }
                    ?.asJsonObject

            val totalPages =
                pagination
                    ?.get("pages")
                    ?.asInt
                    ?: 1

            val dataList =
                if (dataArray.size() > 1) {
                    dataArray[1].asJsonArray
                } else {
                    null
                }

            val newItems = mutableListOf<DataItem>()

            dataList?.forEach { element ->

                if (!element.isJsonObject) {
                    return@forEach
                }

                val obj = element.asJsonObject

                val kategori =
                    obj.get("subj")
                        ?.asString
                        ?.trim()
                        ?: ""

                if (
                    !kategori.equals(
                        namaKategori,
                        ignoreCase = true
                    )
                ) {
                    return@forEach
                }

                val judul =
                    obj.get("title")
                        ?.asString
                        ?.trim()
                        ?: ""

                val tableId =
                    obj.get("table_id")
                        ?.asString
                        ?.trim()
                        ?: ""

                if (judul.isNotEmpty()) {
                    newItems.add(
                        DataItem(
                            judul,
                            kategori,
                            "Static Table",
                            tableId,
                            null,
                            emptyList(),
                            "STATIC"
                        )
                    )
                }
            }

            runOnUiThread {
                tambahDataBaru(newItems)

                if (page < totalPages) {
                    loadStaticTablePage(page + 1)
                }
            }
        }
    }

    private fun loadVariablePage(page: Int) {
        repository.getAllVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            if (
                error != null ||
                response?.data == null ||
                response.status != "OK"
            ) {
                return@getAllVariables
            }

            val rootData = response.data
            val dataArray = rootData.asJsonArray

            val pagination =
                dataArray
                    .firstOrNull()
                    ?.takeIf { it.isJsonObject }
                    ?.asJsonObject

            val totalPages =
                pagination
                    ?.get("pages")
                    ?.asInt
                    ?: 1

            val dataList =
                if (dataArray.size() > 1) {
                    dataArray[1].asJsonArray
                } else {
                    null
                }

            val newItems = mutableListOf<DataItem>()

            dataList?.forEach { element ->

                if (!element.isJsonObject) {
                    return@forEach
                }

                val obj = element.asJsonObject

                val kategori =
                    obj.get("subcsa_name")
                        ?.asString
                        ?.trim()
                        ?: ""

                if (
                    !kategori.equals(
                        namaKategori,
                        ignoreCase = true
                    )
                ) {
                    return@forEach
                }

                val judul =
                    obj.get("title")
                        ?.asString
                        ?.trim()
                        ?: ""

                val varId =
                    obj.get("var_id")
                        ?.asString
                        ?.trim()
                        ?: ""

                if (judul.isNotEmpty()) {
                    newItems.add(
                        DataItem(
                            judul,
                            kategori,
                            "Variable",
                            varId,
                            null,
                            emptyList(),
                            "VARIABLE"
                        )
                    )
                }
            }

            runOnUiThread {
                tambahDataBaru(newItems)

                if (page < totalPages) {
                    loadVariablePage(page + 1)
                }
            }
        }
    }

    // Fungsi baru untuk merender data secara instan saat data berhasil di-parse
    private fun tambahDataBaru(
        newItems: List<DataItem>
    ) {
        if (newItems.isEmpty()) {
            return
        }

        semuaData.addAll(newItems)

        val keyword =
            etSearchData.text
                .toString()
                .trim()

        val filtered =
            if (keyword.isEmpty()) {
                newItems
            } else {
                newItems.filter {
                    it.judul.contains(
                        keyword,
                        ignoreCase = true
                    )
                }
            }

        if (filtered.isNotEmpty()) {
            dataTampil.addAll(filtered)

            for (data in filtered) {
                tambahCard(data)
            }
        }
    }

    private fun filterData(keyword: String) {
        dataTampil.clear()
        dataContainer.removeAllViews()

        val filtered =
            if (keyword.isEmpty()) {
                semuaData
            } else {
                semuaData.filter {
                    it.judul.contains(
                        keyword,
                        ignoreCase = true
                    )
                }
            }

        dataTampil.addAll(filtered)

        for (data in dataTampil) {
            tambahCard(data)
        }
    }

    // Merapikan parameter dengan memanggil Object DataItem langsung
    private fun tambahCard(data: DataItem) {

        val view =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    dataContainer,
                    false
                )

        val tvJudul =
            view.findViewById<TextView>(
                R.id.tvJudul
            )

        val tvKategori =
            view.findViewById<TextView>(
                R.id.tvKategori
            )

        val tvKode =
            view.findViewById<TextView>(
                R.id.tvKode
            )

        val tvTahun =
            view.findViewById<TextView>(
                R.id.tvTahun
            )

        val tvNilai =
            view.findViewById<TextView>(
                R.id.tvNilai
            )

        tvJudul.text = data.judul

        tvKategori.visibility =
            View.GONE

        tvKode.visibility =
            View.GONE

        tvNilai.visibility =
            View.GONE

        if (
            data.type.equals(
                "SIMDASI",
                ignoreCase = true
            ) &&
            data.tahun != null
        ) {
            tvTahun.text =
                data.tahun.toString()

            tvTahun.visibility =
                View.GONE
        } else {
            tvTahun.visibility =
                View.GONE
        }

        val kategoriParams =
            tvKategori.layoutParams
                    as ConstraintLayout.LayoutParams

        kategoriParams.startToStart =
            ConstraintLayout.LayoutParams.UNSET

        kategoriParams.startToEnd =
            R.id.viewAccent

        kategoriParams.topToTop =
            ConstraintLayout.LayoutParams.PARENT_ID

        kategoriParams.topToBottom =
            ConstraintLayout.LayoutParams.UNSET

        kategoriParams.marginStart =
            20

        kategoriParams.topMargin =
            16

        tvKategori.layoutParams =
            kategoriParams

        val judulParams =
            tvJudul.layoutParams
                    as ConstraintLayout.LayoutParams

        judulParams.topToTop =
            ConstraintLayout.LayoutParams.UNSET

        judulParams.topToBottom =
            R.id.tvKategori

        judulParams.topMargin =
            8

        tvJudul.layoutParams =
            judulParams

        view.setOnClickListener {

            if (
                data.type.equals(
                    "SIMDASI",
                    ignoreCase = true
                )
            ) {
                if (
                    data.tahun == null ||
                    data.ketersediaanTahun.isEmpty()
                ) {
                    return@setOnClickListener
                }

                val intent =
                    Intent(
                        this,
                        StatistikDetailActivity::class.java
                    ).apply {

                        putExtra(
                            StatistikDetailActivity.EXTRA_ID_TABEL,
                            data.id
                        )

                        putExtra(
                            StatistikDetailActivity.EXTRA_TAHUN,
                            data.tahun
                        )

                        putExtra(
                            StatistikDetailActivity.EXTRA_JUDUL,
                            data.judul
                        )

                        putExtra(
                            StatistikDetailActivity.EXTRA_KODE,
                            ""
                        )

                        putIntegerArrayListExtra(
                            StatistikDetailActivity.EXTRA_KETERSEDIAAN_TAHUN,
                            ArrayList(
                                data.ketersediaanTahun
                            )
                        )
                    }

                startActivity(intent)

            } else {

                val intent =
                    Intent(
                        this,
                        DetailKategoriActivity::class.java
                    ).apply {

                        putExtra(
                            "JUDUL",
                            data.judul
                        )

                        putExtra(
                            "KATEGORI",
                            data.kategori
                        )

                        // Sumber dikirim ke intent tapi tidak di set ke XML manapun
                        putExtra(
                            "SUMBER",
                            data.sumber
                        )

                        putExtra(
                            "ID",
                            data.id
                        )

                        putExtra(
                            "TYPE",
                            data.type
                        )

                        if (data.tahun != null) {
                            putExtra(
                                "TAHUN",
                                data.tahun
                            )
                        }
                    }

                startActivity(intent)
            }
        }

        dataContainer.addView(view)
    }
}