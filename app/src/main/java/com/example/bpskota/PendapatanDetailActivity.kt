package com.example.bpskota

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.bpskota.bps.model.KonsumsiDetailResponse
import com.example.bpskota.bps.model.KonsumsiVariableDetail
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PendapatanDetailActivity : AppCompatActivity() {

    companion object {

        private const val TAG =
            "PENDAPATAN_DETAIL"

        private const val DOMAIN =
            "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val EXTRA_VARIABLE_ID =
            "VARIABLE_ID"

        private const val EXTRA_TAHUN =
            "TAHUN"

        private const val EXTRA_JUDUL =
            "JUDUL"
    }

    // ============================================================
    // REPOSITORY
    // ============================================================

    private val repository =
        BpsRepository()

    // ============================================================
    // DATA
    // ============================================================

    private var variableId =
        -1

    private var tahun =
        -1

    private var judul =
        ""

    // ============================================================
    // VIEW
    // ============================================================

    private lateinit var btnBack: ImageView

    private lateinit var progressLoading: ProgressBar

    private lateinit var tvJudul: TextView

    private lateinit var tvTahun: TextView

    private lateinit var cardContainer: LinearLayout

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_pendapatan_detail
        )

        initView()

        setupButton()

        // ========================================================
        // INTENT
        // ========================================================

        variableId =
            intent.getIntExtra(
                EXTRA_VARIABLE_ID,
                -1
            )

        tahun =
            intent.getIntExtra(
                EXTRA_TAHUN,
                -1
            )

        judul =
            intent.getStringExtra(
                EXTRA_JUDUL
            ) ?: "Data Pendapatan"

        // ========================================================
        // VALIDASI
        // ========================================================

        if (
            variableId == -1
        ) {

            Toast.makeText(
                this,
                "ID variabel tidak tersedia",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        if (
            tahun == -1
        ) {

            Toast.makeText(
                this,
                "Tahun tidak tersedia",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        // ========================================================
        // HEADER
        // ========================================================

        tvJudul.text =
            judul

        tvTahun.text =
            tahun.toString()

        // ========================================================
        // LOAD
        // ========================================================

        loadDetail()
    }

    // ============================================================
    // INIT VIEW
    // ============================================================

    private fun initView() {

        btnBack =
            findViewById(
                R.id.btnBack
            )

        progressLoading =
            findViewById(
                R.id.progressLoading
            )

        tvJudul =
            findViewById(
                R.id.tvJudul
            )

        tvTahun =
            findViewById(
                R.id.tvTahun
            )

        cardContainer =
            findViewById(
                R.id.cardContainer
            )
    }

    // ============================================================
    // BUTTON
    // ============================================================

    private fun setupButton() {

        btnBack.setOnClickListener {

            finish()
        }
    }

    // ============================================================
    // LOAD DETAIL
    // ============================================================

    private fun loadDetail() {

        /*
         * Contoh:
         *
         * Tahun aplikasi = 2026
         * Tahun API = 126
         */

        val th =
            tahun - 1900

        progressLoading.visibility =
            View.VISIBLE

        cardContainer.removeAllViews()

        repository.getKonsumsiDetail(
            domain = DOMAIN,
            variable = variableId,
            tahun = th,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<KonsumsiDetailResponse> {

                override fun onResponse(
                    call:
                    Call<KonsumsiDetailResponse>,

                    response:
                    Response<KonsumsiDetailResponse>
                ) {

                    progressLoading.visibility =
                        View.GONE

                    // ====================================================
                    // HTTP
                    // ====================================================

                    if (
                        !response.isSuccessful
                    ) {

                        Toast.makeText(
                            this@PendapatanDetailActivity,
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

                        Toast.makeText(
                            this@PendapatanDetailActivity,
                            "Response kosong",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    // ====================================================
                    // STATUS
                    // ====================================================

                    if (
                        !body.status.equals(
                            "OK",
                            ignoreCase = true
                        )
                    ) {

                        Toast.makeText(
                            this@PendapatanDetailActivity,
                            "Data BPS tidak tersedia",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    // ====================================================
                    // TAMPILKAN
                    // ====================================================

                    tampilkanDetail(
                        body
                    )
                }

                override fun onFailure(
                    call:
                    Call<KonsumsiDetailResponse>,

                    t: Throwable
                ) {

                    progressLoading.visibility =
                        View.GONE

                    Toast.makeText(
                        this@PendapatanDetailActivity,
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
        response: KonsumsiDetailResponse
    ) {

        cardContainer.removeAllViews()

        val variables =
            response.variable

        val vervar =
            response.vervar

        val dataContent =
            response.dataContent

        // ========================================================
        // VALIDASI VARIABLE
        // ========================================================

        if (
            variables.isNullOrEmpty()
        ) {

            tampilkanPesan(
                "Variabel statistik tidak tersedia"
            )

            return
        }

        // ========================================================
        // VALIDASI WILAYAH
        // ========================================================

        if (
            vervar.isNullOrEmpty()
        ) {

            tampilkanPesan(
                "Data wilayah tidak tersedia"
            )

            return
        }

        // ========================================================
        // VALIDASI DATA
        // ========================================================

        if (
            dataContent == null ||
            dataContent.isJsonNull
        ) {

            tampilkanPesan(
                "Data statistik tidak tersedia"
            )

            return
        }

        // ========================================================
        // VARIABLE
        // ========================================================

        val variable =
            variables.firstOrNull()

        if (
            variable == null
        ) {

            tampilkanPesan(
                "Variabel statistik tidak tersedia"
            )

            return
        }

        // ========================================================
        // CEK KHUSUS KELOMPOK
        // ========================================================

        val adaKelompok =
            vervar.any {

                val label =
                    it.label
                        ?.replace(
                            "<b>",
                            "",
                            ignoreCase = true
                        )
                        ?.replace(
                            "</b>",
                            "",
                            ignoreCase = true
                        )
                        ?.trim()
                        ?: ""

                label.equals(
                    "Makanan",
                    ignoreCase = true
                ) ||
                        label.equals(
                            "Bukan Makanan",
                            ignoreCase = true
                        )
            }

        // ========================================================
        // KHUSUS DATA KELOMPOK
        // ========================================================

        if (
            adaKelompok
        ) {

            tampilkanCardKelompok(
                vervar =
                vervar,

                dataContent =
                dataContent
            )

            return
        }

        // ========================================================
        // DATA BIASA
        // ========================================================

        for (
        wilayah in vervar
        ) {

            val wilayahVal =
                wilayah.value
                    ?.toString()
                    ?: ""

            tampilkanCardWilayah(
                wilayah =
                bersihkanHtml(
                    wilayah.label ?: "-"
                ),

                wilayahVal =
                wilayahVal,

                dataContent =
                dataContent
            )
        }
    }

    // ============================================================
    // CARD KELOMPOK
    // ============================================================

    private fun tampilkanCardKelompok(
        vervar: List<com.example.bpskota.bps.model.KonsumsiVervar>,
        dataContent: JsonElement
    ) {

        // ========================================================
        // OBJECT DATA
        // ========================================================

        if (
            !dataContent.isJsonObject
        ) {

            tampilkanPesan(
                "Data statistik tidak tersedia"
            )

            return
        }

        val jsonObject =
            dataContent.asJsonObject

        var cardSedangBerjalan: LinearLayout? =
            null

        for (
        wilayah in vervar
        ) {

            val wilayahVal =
                wilayah.value
                    ?.toString()
                    ?: ""

            val label =
                bersihkanHtml(
                    wilayah.label ?: "-"
                )

            // ====================================================
            // KELOMPOK
            // ====================================================

            if (
                label.equals(
                    "Makanan",
                    ignoreCase = true
                ) ||
                label.equals(
                    "Bukan Makanan",
                    ignoreCase = true
                )
            ) {

                // -----------------------------------------------
                // BUAT CARD BARU
                // -----------------------------------------------

                val card =
                    buatCardDasar()

                val cardContent =
                    LinearLayout(this)

                cardContent.orientation =
                    LinearLayout.VERTICAL

                cardContent.setPadding(
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16)
                )

                // -----------------------------------------------
                // JUDUL KELOMPOK
                // -----------------------------------------------

                val tvKelompok =
                    TextView(this)

                tvKelompok.text =
                    label

                tvKelompok.textSize =
                    18f

                tvKelompok.setTypeface(
                    null,
                    Typeface.BOLD
                )

                tvKelompok.setTextColor(
                    Color.rgb(
                        17,
                        24,
                        39
                    )
                )

                cardContent.addView(
                    tvKelompok
                )

                // -----------------------------------------------
                // DIVIDER
                // -----------------------------------------------

                val divider =
                    buatDivider()

                cardContent.addView(
                    divider
                )

                // -----------------------------------------------
                // SIMPAN CARD CONTENT
                // -----------------------------------------------

                card.addView(
                    cardContent
                )

                cardContainer.addView(
                    card
                )

                cardSedangBerjalan =
                    cardContent

                continue
            }

            // ====================================================
            // ITEM DALAM KELOMPOK
            // ====================================================

            if (
                cardSedangBerjalan != null
            ) {

                var nilai =
                    "-"

                // -----------------------------------------------
                // CARI DATA BERDASARKAN VAL
                // -----------------------------------------------

                for (
                entry in jsonObject.entrySet()
                ) {

                    if (
                        entry.key.startsWith(
                            wilayahVal
                        )
                    ) {

                        nilai =
                            formatJsonValue(
                                entry.value
                            )

                        break
                    }
                }

                // -----------------------------------------------
                // TAMBAHKAN BARIS
                // -----------------------------------------------

                val row =
                    buatBarisData(
                        key =
                        label,

                        value =
                        nilai
                    )

                cardSedangBerjalan.addView(
                    row
                )
            }
        }
    }

    // ============================================================
    // CARD WILAYAH
    // ============================================================

    private fun tampilkanCardWilayah(
        wilayah: String,
        wilayahVal: String,
        dataContent: JsonElement
    ) {

        val card =
            buatCardDasar()

        // ========================================================
        // CONTAINER CARD
        // ========================================================

        val cardContent =
            LinearLayout(this)

        cardContent.orientation =
            LinearLayout.VERTICAL

        cardContent.setPadding(
            dpToPx(16),
            dpToPx(16),
            dpToPx(16),
            dpToPx(16)
        )

        // ========================================================
        // NAMA WILAYAH
        // ========================================================

        val tvWilayah =
            TextView(this)

        tvWilayah.text =
            wilayah

        tvWilayah.textSize =
            18f

        tvWilayah.setTypeface(
            null,
            Typeface.BOLD
        )

        tvWilayah.setTextColor(
            Color.rgb(
                17,
                24,
                39
            )
        )

        tvWilayah.maxLines =
            2

        tvWilayah.ellipsize =
            android.text.TextUtils.TruncateAt.END

        cardContent.addView(
            tvWilayah
        )

        // ========================================================
        // PEMBATAS
        // ========================================================

        val divider =
            buatDivider()

        cardContent.addView(
            divider
        )

        // ========================================================
        // DATA
        // ========================================================

        tampilkanNilaiData(
            cardContent =
            cardContent,

            wilayah =
            wilayah,

            wilayahVal =
            wilayahVal,

            dataContent =
            dataContent
        )

        // ========================================================
        // MASUKKAN CARD
        // ========================================================

        card.addView(
            cardContent
        )

        cardContainer.addView(
            card
        )
    }

    // ============================================================
    // TAMPILKAN NILAI DATA
    // ============================================================

    private fun tampilkanNilaiData(
        cardContent: LinearLayout,
        wilayah: String,
        wilayahVal: String,
        dataContent: JsonElement
    ) {

        // ========================================================
        // OBJECT
        // ========================================================

        if (
            dataContent.isJsonObject
        ) {

            val jsonObject =
                dataContent.asJsonObject

            for (
            entry in jsonObject.entrySet()
            ) {

                if (
                    wilayahVal.isNotBlank() &&
                    entry.key.startsWith(
                        wilayahVal
                    )
                ) {

                    val row =
                        buatBarisData(
                            key =
                            wilayah,

                            value =
                            formatJsonValue(
                                entry.value
                            )
                        )

                    cardContent.addView(
                        row
                    )

                    return
                }
            }

            // ====================================================
            // FALLBACK
            // ====================================================

            val firstEntry =
                jsonObject.entrySet()
                    .firstOrNull()

            if (
                firstEntry != null
            ) {

                val row =
                    buatBarisData(
                        key =
                        wilayah,

                        value =
                        formatJsonValue(
                            firstEntry.value
                        )
                    )

                cardContent.addView(
                    row
                )
            }

            return
        }

        // ========================================================
        // ARRAY
        // ========================================================

        if (
            dataContent.isJsonArray
        ) {

            val jsonArray =
                dataContent.asJsonArray

            for (
            item in jsonArray
            ) {

                val row =
                    buatBarisData(
                        key =
                        wilayah,

                        value =
                        formatJsonValue(
                            item
                        )
                    )

                cardContent.addView(
                    row
                )
            }

            return
        }

        // ========================================================
        // VALUE BIASA
        // ========================================================

        val row =
            buatBarisData(
                key =
                wilayah,

                value =
                formatJsonValue(
                    dataContent
                )
            )

        cardContent.addView(
            row
        )
    }

    // ============================================================
    // BARIS DATA
    // ============================================================

    private fun buatBarisData(
        key: String,
        value: String
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
        // KEY
        // ========================================================

        val tvKey =
            TextView(this)

        tvKey.text =
            bersihkanHtml(
                key
            )

        tvKey.textSize =
            14f

        tvKey.setTextColor(
            Color.rgb(
                75,
                85,
                99
            )
        )

        tvKey.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        // ========================================================
        // VALUE
        // ========================================================

        val tvValue =
            TextView(this)

        tvValue.text =
            if (
                value.isBlank()
            ) {
                "-"
            } else {
                value
            }

        tvValue.textSize =
            14f

        tvValue.setTypeface(
            null,
            Typeface.BOLD
        )

        tvValue.setTextColor(
            Color.rgb(
                17,
                24,
                39
            )
        )

        tvValue.gravity =
            Gravity.END

        // ========================================================
        // ADD
        // ========================================================

        row.addView(
            tvKey
        )

        row.addView(
            tvValue
        )

        return row
    }

    // ============================================================
    // FORMAT JSON
    // ============================================================

    private fun formatJsonValue(
        element: JsonElement
    ): String {

        if (
            element.isJsonNull
        ) {

            return "-"
        }

        return try {

            if (
                element.isJsonPrimitive
            ) {

                element.asJsonPrimitive
                    .toString()
                    .replace(
                        "\"",
                        ""
                    )

            } else {

                element.toString()
            }

        } catch (
            e: Exception
        ) {

            element.toString()
        }
    }

    // ============================================================
    // BERSIHKAN HTML
    // ============================================================

    private fun bersihkanHtml(
        text: String
    ): String {

        return text
            .replace(
                "<b>",
                "",
                ignoreCase = true
            )
            .replace(
                "</b>",
                "",
                ignoreCase = true
            )
            .replace(
                "<strong>",
                "",
                ignoreCase = true
            )
            .replace(
                "</strong>",
                "",
                ignoreCase = true
            )
            .trim()
    }

    // ============================================================
    // CARD DASAR
    // ============================================================

    private fun buatCardDasar(): CardView {

        val card =
            CardView(this)

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    dpToPx(14)
            }

        card.radius =
            dpToPx(18).toFloat()

        card.cardElevation =
            dpToPx(2).toFloat()

        card.setCardBackgroundColor(
            Color.WHITE
        )

        return card
    }

    // ============================================================
    // DIVIDER
    // ============================================================

    private fun buatDivider(): View {

        val divider =
            View(this)

        divider.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {

                topMargin =
                    dpToPx(12)

                bottomMargin =
                    dpToPx(12)
            }

        divider.setBackgroundColor(
            Color.rgb(
                229,
                231,
                235
            )
        )

        return divider
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
            dpToPx(30),
            dpToPx(16),
            dpToPx(30)
        )

        cardContainer.addView(
            text
        )
    }

    // ============================================================
    // LOADING
    // ============================================================

    private fun tampilkanLoading(
        tampil: Boolean
    ) {

        progressLoading.visibility =
            if (
                tampil
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }
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