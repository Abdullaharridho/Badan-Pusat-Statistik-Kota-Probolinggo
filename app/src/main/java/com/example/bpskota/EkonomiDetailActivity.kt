package com.example.bpskota

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bps.model.EkonomiDetailResponse
import com.example.bpskota.bps.model.EkonomiItem
import com.example.bpskota.bps.model.EkonomiVervar
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EkonomiDetailActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "EKONOMI_DETAIL"

        const val EXTRA_ID = "ekonomi_id"

        const val EXTRA_JUDUL = "ekonomi_judul"

        const val EXTRA_TAHUN = "ekonomi_tahun"
    }

    // ============================================================
    // REPOSITORY
    // ============================================================

    private val repository =
        BpsRepository()

    // ============================================================
    // BPS
    // ============================================================

    private val domain =
        "3574"

    private val apiKey =
        "008edaaae5d450b1913b31a2cef618c3"

    // ============================================================
    // VIEW
    // ============================================================

    private lateinit var tvDetailJudul: TextView

    private lateinit var tvDetailTahun: TextView

    private lateinit var tvDetailData: LinearLayout

    private lateinit var progressLoading: ProgressBar

    // ============================================================
    // TAHUN YANG DIKLIK
    // ============================================================

    private var tahunTerpilih =
        2025

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_ekonomi_detail
        )

        initView()

        setupButton()

        // ========================================================
        // AMBIL INTENT
        // ========================================================

        val id =
            intent.getStringExtra(
                EXTRA_ID
            )

        val judul =
            intent.getStringExtra(
                EXTRA_JUDUL
            )

        tahunTerpilih =
            intent.getIntExtra(
                EXTRA_TAHUN,
                2025
            )

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "EKONOMI DETAIL"
        )

        Log.d(
            TAG,
            "ID = $id"
        )

        Log.d(
            TAG,
            "JUDUL = $judul"
        )

        Log.d(
            TAG,
            "TAHUN DIPILIH = $tahunTerpilih"
        )

        Log.d(
            TAG,
            "========================================"
        )

        // ========================================================
        // HEADER
        // ========================================================

        tvDetailJudul.text =
            judul ?: "Tanpa judul"

        tvDetailTahun.text =
            tahunTerpilih.toString()

        // ========================================================
        // VALIDASI ID
        // ========================================================

        if (
            id.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "ID tabel tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        // ========================================================
        // LOAD DETAIL
        // ========================================================

        loadDetail(
            id = id,
            tahunDipilih = tahunTerpilih
        )
    }

    // ============================================================
    // INIT VIEW
    // ============================================================

    private fun initView() {

        tvDetailJudul =
            findViewById(
                R.id.tvDetailJudul
            )

        tvDetailTahun =
            findViewById(
                R.id.tvDetailTahun
            )

        tvDetailData =
            findViewById(
                R.id.containerData
            )

        progressLoading =
            findViewById(
                R.id.progressLoading
            )
    }

    // ============================================================
    // BUTTON
    // ============================================================

    private fun setupButton() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }

        findViewById<ImageView>(
            R.id.btnDownload
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Fitur download belum tersedia",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // LOAD DETAIL
    // ============================================================

    private fun loadDetail(
        id: String,
        tahunDipilih: Int
    ) {

        progressLoading.visibility =
            ProgressBar.VISIBLE

        tvDetailData.removeAllViews()

        Log.d(
            TAG,
            "REQUEST DETAIL"
        )

        Log.d(
            TAG,
            "ID = $id"
        )

        Log.d(
            TAG,
            "TAHUN DIPILIH = $tahunDipilih"
        )

        repository
            .getEkonomiDetail(
                domain = domain,
                id = id,
                tahun = tahunDipilih,
                apiKey = apiKey
            )
            .enqueue(
                object :
                    Callback<EkonomiDetailResponse> {

                    override fun onResponse(
                        call:
                        Call<EkonomiDetailResponse>,

                        response:
                        Response<EkonomiDetailResponse>
                    ) {

                        progressLoading.visibility =
                            ProgressBar.GONE

                        Log.d(
                            TAG,
                            "HTTP CODE = ${response.code()}"
                        )

                        // ====================================================
                        // HTTP ERROR
                        // ====================================================

                        if (
                            !response.isSuccessful
                        ) {

                            Log.e(
                                TAG,
                                "HTTP ERROR = ${response.code()}"
                            )

                            Toast.makeText(
                                this@EkonomiDetailActivity,
                                "Gagal mengambil data: ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }

                        // ====================================================
                        // BODY
                        // ====================================================

                        val body =
                            response.body()

                        if (
                            body == null
                        ) {

                            Log.e(
                                TAG,
                                "BODY NULL"
                            )

                            Toast.makeText(
                                this@EkonomiDetailActivity,
                                "Response kosong",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }

                        // ====================================================
                        // DEBUG
                        // ====================================================

                        Log.d(
                            TAG,
                            "========================================"
                        )

                        Log.d(
                            TAG,
                            "STATUS = ${body.status}"
                        )

                        Log.d(
                            TAG,
                            "AVAILABLE YEARS = ${body.availableYears}"
                        )

                        Log.d(
                            TAG,
                            "VARIABLE = ${body.variables}"
                        )

                        Log.d(
                            TAG,
                            "VERVAR = ${body.vervar}"
                        )

                        Log.d(
                            TAG,
                            "TAHUN = ${body.tahun}"
                        )

                        Log.d(
                            TAG,
                            "TURTAHUN = ${body.turtahun}"
                        )

                        Log.d(
                            TAG,
                            "DATACONTENT = ${body.dataContent}"
                        )

                        Log.d(
                            TAG,
                            "========================================"
                        )

                        // ====================================================
                        // STATUS
                        // ====================================================

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {

                            Toast.makeText(
                                this@EkonomiDetailActivity,
                                "Data BPS tidak tersedia",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }

                        // ====================================================
                        // VALIDASI VARIABLE
                        // ====================================================

                        if (
                            body.variables.isNullOrEmpty()
                        ) {

                            tampilkanPesan(
                                "Variabel statistik tidak tersedia"
                            )

                            return
                        }

                        // ====================================================
                        // VALIDASI VERVAR
                        // ====================================================

                        if (
                            body.vervar.isNullOrEmpty()
                        ) {

                            tampilkanPesan(
                                "Data wilayah tidak tersedia"
                            )

                            return
                        }

                        // ====================================================
                        // VALIDASI TAHUN
                        // ====================================================

                        if (
                            body.tahun.isNullOrEmpty()
                        ) {

                            tampilkanPesan(
                                "Data tahun tidak tersedia"
                            )

                            return
                        }

                        // ====================================================
                        // VALIDASI DATA
                        // ====================================================

                        if (
                            body.dataContent == null
                        ) {

                            tampilkanPesan(
                                "Data statistik tidak tersedia"
                            )

                            return
                        }

                        // ====================================================
                        // TAMPILKAN
                        // ====================================================

                        tampilkanDetail(
                            body,
                            tahunTerpilih
                        )
                    }

                    override fun onFailure(
                        call:
                        Call<EkonomiDetailResponse>,

                        t: Throwable
                    ) {

                        progressLoading.visibility =
                            ProgressBar.GONE

                        Log.e(
                            TAG,
                            "REQUEST ERROR",
                            t
                        )

                        Toast.makeText(
                            this@EkonomiDetailActivity,
                            "Gagal terhubung ke server BPS: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    // ============================================================
    // TAMPILKAN DETAIL
    // ============================================================

    private fun tampilkanDetail(
        response: EkonomiDetailResponse,
        tahunDipilih: Int
    ) {

        tvDetailData.removeAllViews()

        val variable =
            response.variables
                ?.firstOrNull()

        // ========================================================
        // CARI TAHUN YANG SESUAI
        // ========================================================

        val tahun =
            cariTahun(
                response.tahun,
                tahunDipilih
            )

        Log.d(
            TAG,
            "TAHUN YANG DIPILIH = $tahunDipilih"
        )

        Log.d(
            TAG,
            "OBJECT TAHUN = $tahun"
        )

        // ========================================================
        // JIKA TAHUN TIDAK DITEMUKAN
        // ========================================================

        if (
            tahun == null
        ) {

            tampilkanPesan(
                "Data tahun $tahunDipilih tidak tersedia"
            )

            return
        }

        // ========================================================
        // TAMPILKAN TAHUN
        // ========================================================

        tvDetailTahun.text =
            tahun.label
                ?: tahunDipilih.toString()

        // ========================================================
        // UNIT
        // ========================================================

        if (
            !variable?.unit.isNullOrBlank()
        ) {

            tvDetailData.addView(
                buatInfoUnit(
                    variable?.unit ?: ""
                )
            )
        }

        // ========================================================
        // SECTION TITLE
        // ========================================================

        tvDetailData.addView(
            buatSectionTitle(
                response.labelVervar
                    ?: "Data"
            )
        )

        // ========================================================
        // TAMPILKAN DATA
        // ========================================================

        tampilkanData(
            body = response,
            tahun = tahun
        )
    }

    // ============================================================
    // CARI TAHUN
    // ============================================================

    private fun cariTahun(
        daftarTahun: List<EkonomiItem>?,
        tahunDipilih: Int
    ): EkonomiItem? {

        if (
            daftarTahun.isNullOrEmpty()
        ) {

            return null
        }

        val target =
            tahunDipilih.toString()

        // ========================================================
        // COCOKKAN LABEL
        // ========================================================

        val berdasarkanLabel =
            daftarTahun.firstOrNull { item ->

                val label =
                    item.label
                        ?.trim()

                label == target ||
                        label?.contains(
                            target
                        ) == true
            }

        if (
            berdasarkanLabel != null
        ) {

            return berdasarkanLabel
        }

        // ========================================================
        // COBA COCOKKAN VALUE
        //
        // Contoh BPS:
        //
        // 126 = 2026
        // 125 = 2025
        // 124 = 2024
        //
        // Tidak kita asumsikan langsung.
        // Hanya digunakan jika label tidak ditemukan.
        // ========================================================

        return daftarTahun.firstOrNull { item ->

            item.value?.toString() ==
                    target

        } ?: daftarTahun.firstOrNull { item ->

            item.valId?.toString() ==
                    target
        }
    }

    // ============================================================
    // TAMPILKAN DATA
    // ============================================================

    private fun tampilkanData(
        body: EkonomiDetailResponse,
        tahun: EkonomiItem
    ) {

        val variable =
            body.variables
                ?.firstOrNull()

        val vervar =
            body.vervar

        val dataContent =
            body.dataContent

        // ========================================================
        // VALIDASI
        // ========================================================

        if (
            variable == null ||
            vervar.isNullOrEmpty() ||
            dataContent == null
        ) {

            tampilkanDataKosong()

            return
        }

        // ========================================================
        // VARIABLE
        // ========================================================

        val variableVal =
            variable.value
                ?: variable.valId

        if (
            variableVal == null
        ) {

            tampilkanPesan(
                "Variabel data tidak ditemukan"
            )

            return
        }

        // ========================================================
        // TAHUN VALUE
        // ========================================================

        val tahunVal =
            tahun.value
                ?: tahun.valId

        if (
            tahunVal == null
        ) {

            tampilkanPesan(
                "Kode tahun tidak ditemukan"
            )

            return
        }

        // ========================================================
        // FORMAT TAHUN
        // ========================================================

        val tahunFormatted =
            tahunVal
                .toString()
                .padStart(
                    4,
                    '0'
                )

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "PEMBENTUKAN DATA"
        )

        Log.d(
            TAG,
            "TAHUN LABEL = ${tahun.label}"
        )

        Log.d(
            TAG,
            "TAHUN VALUE = $tahunVal"
        )

        Log.d(
            TAG,
            "TAHUN FORMAT = $tahunFormatted"
        )

        Log.d(
            TAG,
            "VARIABLE = $variableVal"
        )

        Log.d(
            TAG,
            "========================================"
        )

        // ========================================================
        // LOOP VERVAR
        // ========================================================

        for (
        item in vervar
        ) {

            val vervarVal =
                item.value
                    ?: item.valId
                    ?: continue

            val label =
                item.label
                    ?: "-"

            // ====================================================
            // TRIWULAN I
            // ====================================================

            val q1 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 31
                )

            // ====================================================
            // TRIWULAN II
            // ====================================================

            val q2 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 32
                )

            // ====================================================
            // TRIWULAN III
            // ====================================================

            val q3 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 33
                )

            // ====================================================
            // TRIWULAN IV
            // ====================================================

            val q4 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 34
                )

            // ====================================================
            // TAHUNAN
            // ====================================================

            val tahunan =
                ambilNilaiTahunan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turtahun = body.turtahun
                )

            Log.d(
                TAG,
                "----------------------------------------"
            )

            Log.d(
                TAG,
                "LABEL = $label"
            )

            Log.d(
                TAG,
                "Q1 = $q1"
            )

            Log.d(
                TAG,
                "Q2 = $q2"
            )

            Log.d(
                TAG,
                "Q3 = $q3"
            )

            Log.d(
                TAG,
                "Q4 = $q4"
            )

            Log.d(
                TAG,
                "TAHUNAN = $tahunan"
            )

            // ====================================================
            // TAMBAHKAN KE UI
            // ====================================================

            tambahGrupTriwulan(
                label = label,
                unit = variable.unit ?: "",
                q1 = q1,
                q2 = q2,
                q3 = q3,
                q4 = q4,
                tahunan = tahunan
            )
        }
    }

    // ============================================================
    // AMBIL NILAI TRIWULAN
    // ============================================================

    private fun ambilNilaiTriwulan(
        dataContent: JsonObject,
        vervarVal: Int,
        variableVal: Int,
        tahunFormatted: String,
        turTahun: Int
    ): String {

        val key =
            "$vervarVal$variableVal$tahunFormatted$turTahun"

        Log.d(
            TAG,
            "CEK KEY = $key"
        )

        val jsonElement =
            dataContent.get(
                key
            )

        if (
            jsonElement == null ||
            jsonElement.isJsonNull
        ) {

            return "-"
        }

        return try {

            val value =
                jsonElement.asString

            if (
                value.isBlank()
            ) {
                "-"
            } else {
                formatNilai(
                    value
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Gagal membaca key=$key",
                e
            )

            "-"
        }
    }

    // ============================================================
    // AMBIL NILAI TAHUNAN
    // ============================================================

    private fun ambilNilaiTahunan(
        dataContent: JsonObject,
        vervarVal: Int,
        variableVal: Int,
        tahunFormatted: String,
        turtahun: List<EkonomiItem>?
    ): String {

        if (
            turtahun.isNullOrEmpty()
        ) {

            return "-"
        }

        // ========================================================
        // CARI KODE TAHUNAN
        //
        // Kita tidak langsung menganggap 30 sebagai tahunan.
        // Dicari dari label API.
        // ========================================================

        val itemTahunan =
            turtahun.firstOrNull { item ->

                val label =
                    item.label
                        ?.lowercase()
                        ?.trim()
                        ?: ""

                label.contains("tahun") ||
                        label.contains("tahunan") ||
                        label.contains("annual")
            }

        // ========================================================
        // KALAU TIDAK KETEMU DARI LABEL
        //
        // Pada struktur BPS yang kamu gunakan,
        // kode tahunan umumnya berada sebelum kode triwulan.
        //
        // Kita cek kandidat 30 terlebih dahulu.
        // ========================================================

        val kodeTahunan =
            itemTahunan?.value
                ?: itemTahunan?.valId
                ?: 30

        val key =
            "$vervarVal$variableVal$tahunFormatted$kodeTahunan"

        Log.d(
            TAG,
            "KEY TAHUNAN = $key"
        )

        val jsonElement =
            dataContent.get(
                key
            )

        if (
            jsonElement == null ||
            jsonElement.isJsonNull
        ) {

            return "-"
        }

        return try {

            val value =
                jsonElement.asString

            if (
                value.isBlank()
            ) {
                "-"
            } else {
                formatNilai(
                    value
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Gagal membaca tahunan key=$key",
                e
            )

            "-"
        }
    }

    // ============================================================
    // TAMBAH GRUP
    // ============================================================

    private fun tambahGrupTriwulan(
        label: String,
        unit: String,
        q1: String,
        q2: String,
        q3: String,
        q4: String,
        tahunan: String
    ) {

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.setPadding(
            dpToPx(16),
            dpToPx(14),
            dpToPx(16),
            dpToPx(14)
        )

        container.setBackgroundColor(
            Color.WHITE
        )

        container.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    dpToPx(12)
            }

        // ========================================================
        // LABEL
        // ========================================================

        val tvLabel =
            TextView(this)

        tvLabel.text =
            label

        tvLabel.textSize =
            15f

        tvLabel.setTypeface(
            null,
            Typeface.BOLD
        )

        tvLabel.setTextColor(
            Color.rgb(
                17,
                24,
                39
            )
        )

        tvLabel.setPadding(
            0,
            0,
            0,
            dpToPx(6)
        )

        container.addView(
            tvLabel
        )

        // ========================================================
        // SATUAN
        // ========================================================

        if (
            unit.isNotBlank()
        ) {

            val tvUnit =
                TextView(this)

            tvUnit.text =
                "Satuan: $unit"

            tvUnit.textSize =
                12f

            tvUnit.setTextColor(
                Color.GRAY
            )

            tvUnit.setPadding(
                0,
                0,
                0,
                dpToPx(10)
            )

            container.addView(
                tvUnit
            )
        }

        // ========================================================
        // Q1
        // ========================================================

        container.addView(
            buatBarisTriwulan(
                "Triwulan I",
                q1
            )
        )

        // ========================================================
        // Q2
        // ========================================================

        container.addView(
            buatBarisTriwulan(
                "Triwulan II",
                q2
            )
        )

        // ========================================================
        // Q3
        // ========================================================

        container.addView(
            buatBarisTriwulan(
                "Triwulan III",
                q3
            )
        )

        // ========================================================
        // Q4
        // ========================================================

        container.addView(
            buatBarisTriwulan(
                "Triwulan IV",
                q4
            )
        )

        // ========================================================
        // TAHUNAN
        // ========================================================

        container.addView(
            buatBarisTriwulan(
                "Tahunan",
                tahunan
            )
        )

        // ========================================================
        // ADD
        // ========================================================

        tvDetailData.addView(
            container
        )
    }

    // ============================================================
    // BARIS
    // ============================================================

    private fun buatBarisTriwulan(
        triwulan: String,
        nilai: String
    ): LinearLayout {

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        row.setPadding(
            0,
            dpToPx(7),
            0,
            dpToPx(7)
        )

        // ========================================================
        // NAMA
        // ========================================================

        val tvTriwulan =
            TextView(this)

        tvTriwulan.text =
            triwulan

        tvTriwulan.textSize =
            14f

        tvTriwulan.setTextColor(
            Color.rgb(
                75,
                85,
                99
            )
        )

        tvTriwulan.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        // ========================================================
        // NILAI
        // ========================================================

        val tvNilai =
            TextView(this)

        tvNilai.text =
            if (
                nilai.isBlank()
            ) {
                "-"
            } else {
                nilai
            }

        tvNilai.textSize =
            14f

        tvNilai.setTypeface(
            null,
            Typeface.BOLD
        )

        tvNilai.setTextColor(
            Color.rgb(
                17,
                24,
                39
            )
        )

        tvNilai.gravity =
            Gravity.END

        // ========================================================
        // ADD
        // ========================================================

        row.addView(
            tvTriwulan
        )

        row.addView(
            tvNilai
        )

        return row
    }

    // ============================================================
    // FORMAT NILAI
    // ============================================================

    private fun formatNilai(
        value: String
    ): String {

        if (
            value == "-" ||
            value.isBlank()
        ) {

            return "-"
        }

        val number =
            value
                .replace(",", "")
                .toDoubleOrNull()

        if (
            number == null
        ) {

            return value
        }

        return if (
            number % 1.0 == 0.0
        ) {

            String.format(
                "%,.0f",
                number
            )

        } else {

            String.format(
                "%,.2f",
                number
            )
        }
    }

    // ============================================================
    // INFO UNIT
    // ============================================================

    private fun buatInfoUnit(
        unit: String
    ): LinearLayout {

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.HORIZONTAL

        container.gravity =
            Gravity.CENTER_VERTICAL

        container.setPadding(
            dpToPx(16),
            dpToPx(12),
            dpToPx(16),
            dpToPx(12)
        )

        container.setBackgroundColor(
            Color.WHITE
        )

        container.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    dpToPx(16)
            }

        val label =
            TextView(this)

        label.text =
            "Satuan"

        label.textSize =
            13f

        label.setTextColor(
            Color.rgb(
                107,
                114,
                128
            )
        )

        label.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        val value =
            TextView(this)

        value.text =
            unit

        value.textSize =
            14f

        value.setTypeface(
            null,
            Typeface.BOLD
        )

        value.setTextColor(
            Color.rgb(
                249,
                115,
                22
            )
        )

        value.gravity =
            Gravity.END

        container.addView(
            label
        )

        container.addView(
            value
        )

        return container
    }

    // ============================================================
    // SECTION TITLE
    // ============================================================

    private fun buatSectionTitle(
        title: String
    ): TextView {

        val textView =
            TextView(this)

        textView.text =
            title

        textView.textSize =
            15f

        textView.setTypeface(
            null,
            Typeface.BOLD
        )

        textView.setTextColor(
            Color.rgb(
                17,
                24,
                39
            )
        )

        textView.setPadding(
            dpToPx(4),
            dpToPx(4),
            dpToPx(4),
            dpToPx(10)
        )

        textView.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        return textView
    }

    // ============================================================
    // DATA KOSONG
    // ============================================================

    private fun tampilkanDataKosong() {

        val textView =
            TextView(this)

        textView.text =
            "Data statistik tidak tersedia"

        textView.textSize =
            14f

        textView.setTextColor(
            Color.GRAY
        )

        textView.gravity =
            Gravity.CENTER

        textView.setPadding(
            dpToPx(8),
            dpToPx(30),
            dpToPx(8),
            dpToPx(30)
        )

        tvDetailData.addView(
            textView
        )
    }

    // ============================================================
    // PESAN
    // ============================================================

    private fun tampilkanPesan(
        pesan: String
    ) {

        val text =
            TextView(this)

        text.text =
            pesan

        text.textSize =
            14f

        text.setTextColor(
            Color.GRAY
        )

        text.gravity =
            Gravity.CENTER

        text.setPadding(
            dpToPx(16),
            dpToPx(24),
            dpToPx(16),
            dpToPx(24)
        )

        tvDetailData.addView(
            text
        )
    }

    // ============================================================
    // DP TO PX
    // ============================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }
}