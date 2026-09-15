package com.example.bpskota

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.KonsumsiDetailResponse
import com.example.bpskota.bps.model.KonsumsiVervar
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class PendapatanDetailActivity : AppCompatActivity() {

    companion object {

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

        private const val CHANNEL_ID =
            "bps_download_channel"

        private const val NOTIFICATION_ID =
            3003

        private const val REQUEST_NOTIFICATION_PERMISSION =
            3003
    }

    private val repository =
        BpsRepository()

    private lateinit var activityTracker: ActivityTracker

    private var variableId =
        -1

    private var tahun =
        -1

    private var judul =
        ""

    private var detailData:
            KonsumsiDetailResponse? =
        null

    private lateinit var btnBack: ImageView

    private lateinit var btnDownload: ImageView

    private lateinit var progressLoading:
            LottieAnimationView

    private lateinit var tvJudul:
            TextView

    private lateinit var tvTahun:
            TextView

    private lateinit var cardContainer:
            LinearLayout

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

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "PendapatanDetail",
            metadata = mapOf(
                "data_id" to variableId,
                "tahun" to tahun,
                "judul" to judul
            )
        )

        setupButton()

        createNotificationChannel()

        requestNotificationPermission()

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

        tvJudul.text =
            judul

        tvTahun.text =
            tahun.toString()

        loadDetail()
    }

    private fun initView() {

        btnBack =
            findViewById(
                R.id.btnBack
            )

        btnDownload =
            findViewById(
                R.id.btnDownload
            )

        progressLoading =
            findViewById(
                R.id.progressLoading
            )

        progressLoading.setAnimation(
            "Loading_Animation.json"
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

    private fun setupButton() {

        btnBack.setOnClickListener {

            finish()
        }

        btnDownload.setOnClickListener {

            tampilkanDialogDownload()
        }
    }

    private fun tampilkanDialogDownload() {

        if (
            detailData == null
        ) {

            Toast.makeText(
                this,
                "Data belum tersedia untuk diunduh",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Download PDF"
            )
            .setMessage(
                "Apakah Anda ingin mengunduh data statistik ini dalam bentuk PDF?"
            )
            .setNegativeButton(
                "Tidak"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .setPositiveButton(
                "Ya"
            ) { _, _ ->

                downloadPdf()
            }
            .show()
    }

    private fun loadDetail() {

        val th =
            tahun - 1900

        tampilkanLoading(
            true
        )

        cardContainer.removeAllViews()

        detailData =
            null

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

                    tampilkanLoading(
                        false
                    )

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

                    detailData =
                        body

                    tampilkanDetail(
                        body
                    )
                }

                override fun onFailure(
                    call:
                    Call<KonsumsiDetailResponse>,

                    t: Throwable
                ) {

                    tampilkanLoading(
                        false
                    )

                    Toast.makeText(
                        this@PendapatanDetailActivity,
                        "Gagal terhubung ke server BPS: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun tampilkanDetail(
        response:
        KonsumsiDetailResponse
    ) {

        cardContainer.removeAllViews()

        val variables =
            response.variable

        val vervar =
            response.vervar

        val dataContent =
            response.dataContent

        if (
            variables.isNullOrEmpty()
        ) {

            tampilkanPesan(
                "Variabel statistik tidak tersedia"
            )

            return
        }

        if (
            vervar.isNullOrEmpty()
        ) {

            tampilkanPesan(
                "Data wilayah tidak tersedia"
            )

            return
        }

        if (
            dataContent == null ||
            dataContent.isJsonNull
        ) {

            tampilkanPesan(
                "Data statistik tidak tersedia"
            )

            return
        }

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

    private fun tampilkanCardKelompok(
        vervar:
        List<KonsumsiVervar>,

        dataContent:
        JsonElement
    ) {

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

        var cardSedangBerjalan:
                LinearLayout? =
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

                val card =
                    layoutInflater.inflate(
                        R.layout.card_statistik,
                        cardContainer,
                        false
                    ) as CardView

                val tvNamaWilayah =
                    card.findViewById<TextView>(
                        R.id.tvNamaWilayah
                    )

                tvNamaWilayah.text =
                    label

                val containerVariable =
                    card.findViewById<LinearLayout>(
                        R.id.containerVariable
                    )

                cardContainer.addView(
                    card
                )

                cardSedangBerjalan =
                    containerVariable

                continue
            }

            if (
                cardSedangBerjalan != null
            ) {

                var nilai =
                    "-"

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

    private fun tampilkanCardWilayah(
        wilayah: String,
        wilayahVal: String,
        dataContent: JsonElement
    ) {

        val card =
            layoutInflater.inflate(
                R.layout.card_statistik,
                cardContainer,
                false
            ) as CardView

        val tvNamaWilayah =
            card.findViewById<TextView>(
                R.id.tvNamaWilayah
            )

        tvNamaWilayah.text =
            wilayah

        val containerVariable =
            card.findViewById<LinearLayout>(
                R.id.containerVariable
            )

        tampilkanNilaiData(
            cardContent =
            containerVariable,

            wilayah =
            wilayah,

            wilayahVal =
            wilayahVal,

            dataContent =
            dataContent
        )

        cardContainer.addView(
            card
        )
    }

    private fun tampilkanNilaiData(
        cardContent: LinearLayout,
        wilayah: String,
        wilayahVal: String,
        dataContent: JsonElement
    ) {

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

        row.addView(
            tvKey
        )

        row.addView(
            tvValue
        )

        return row
    }

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

    private fun tampilkanLoading(
        tampil: Boolean
    ) {

        if (
            tampil
        ) {

            progressLoading.visibility =
                View.VISIBLE

            progressLoading.playAnimation()

        } else {

            progressLoading.cancelAnimation()

            progressLoading.visibility =
                View.GONE
        }
    }

    private fun downloadPdf() {

        val body =
            detailData

        if (
            body == null
        ) {

            Toast.makeText(
                this,
                "Data belum tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            body.vervar.isNullOrEmpty()
        ) {

            Toast.makeText(
                this,
                "Data wilayah tidak tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            body.dataContent == null ||
            body.dataContent.isJsonNull
        ) {

            Toast.makeText(
                this,
                "Data statistik tidak tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val pdfDocument =
                PdfDocument()

            var pageNumber =
                1

            var page =
                createPdfPage(
                    pdfDocument,
                    pageNumber
                )

            var canvas =
                page.canvas

            val paint =
                Paint().apply {

                    isAntiAlias =
                        true
                }

            paint.textSize =
                18f

            paint.typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

            paint.color =
                Color.BLACK

            canvas.drawText(
                "DATA STATISTIK BPS KOTA PROBOLINGGO",
                40f,
                50f,
                paint
            )

            paint.textSize =
                14f

            canvas.drawText(
                bersihkanHtml(
                    judul
                ),
                40f,
                78f,
                paint
            )

            paint.textSize =
                11f

            paint.typeface =
                Typeface.DEFAULT

            canvas.drawText(
                "Tahun: $tahun",
                40f,
                100f,
                paint
            )

            canvas.drawText(
                "Sumber: Badan Pusat Statistik",
                40f,
                118f,
                paint
            )

            canvas.drawText(
                "BPS Kota Probolinggo",
                40f,
                136f,
                paint
            )

            paint.strokeWidth =
                1f

            canvas.drawLine(
                40f,
                150f,
                555f,
                150f,
                paint
            )

            var y =
                180f

            val vervar =
                body.vervar

            val dataContent =
                body.dataContent

            if (
                dataContent.isJsonObject
            ) {

                val jsonObject =
                    dataContent.asJsonObject

                var cardKelompok:
                        String? =
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

                    val isKelompok =
                        label.equals(
                            "Makanan",
                            ignoreCase = true
                        ) ||
                                label.equals(
                                    "Bukan Makanan",
                                    ignoreCase = true
                                )

                    if (
                        isKelompok
                    ) {

                        cardKelompok =
                            label

                        if (
                            y > 780f
                        ) {

                            pdfDocument.finishPage(
                                page
                            )

                            pageNumber++

                            page =
                                createPdfPage(
                                    pdfDocument,
                                    pageNumber
                                )

                            canvas =
                                page.canvas

                            y =
                                50f
                        }

                        paint.typeface =
                            Typeface.create(
                                Typeface.DEFAULT,
                                Typeface.BOLD
                            )

                        paint.textSize =
                            14f

                        canvas.drawText(
                            label,
                            40f,
                            y,
                            paint
                        )

                        y +=
                            28f

                        continue
                    }

                    var nilai =
                        "-"

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

                    if (
                        y > 780f
                    ) {

                        pdfDocument.finishPage(
                            page
                        )

                        pageNumber++

                        page =
                            createPdfPage(
                                pdfDocument,
                                pageNumber
                            )

                        canvas =
                            page.canvas

                        y =
                            50f
                    }

                    paint.typeface =
                        Typeface.DEFAULT

                    paint.textSize =
                        11f

                    canvas.drawText(
                        label,
                        50f,
                        y,
                        paint
                    )

                    paint.typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    canvas.drawText(
                        nilai,
                        400f,
                        y,
                        paint
                    )

                    y +=
                        24f
                }

            } else {

                paint.typeface =
                    Typeface.DEFAULT

                paint.textSize =
                    11f

                if (
                    y > 780f
                ) {

                    pdfDocument.finishPage(
                        page
                    )

                    pageNumber++

                    page =
                        createPdfPage(
                            pdfDocument,
                            pageNumber
                        )

                    canvas =
                        page.canvas

                    y =
                        50f
                }

                canvas.drawText(
                    formatJsonValue(
                        dataContent
                    ),
                    40f,
                    y,
                    paint
                )
            }

            paint.typeface =
                Typeface.DEFAULT

            paint.textSize =
                9f

            paint.color =
                Color.DKGRAY

            canvas.drawText(
                "BPS Kota Probolinggo",
                40f,
                815f,
                paint
            )

            pdfDocument.finishPage(
                page
            )

            val namaFile =
                sanitasiNamaFile(
                    "Data_Pendapatan_${judul}_${tahun}.pdf"
                )

            val uri =
                savePdfToDownloads(
                    pdfDocument,
                    namaFile
                )

            pdfDocument.close()

            if (
                uri != null
            ) {

                showDownloadNotification(
                    uri,
                    namaFile
                )

                Toast.makeText(
                    this,
                    "PDF berhasil disimpan di Downloads/BPS Kota Probolinggo",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Gagal menyimpan PDF",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Gagal membuat PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createPdfPage(
        document: PdfDocument,
        pageNumber: Int
    ): PdfDocument.Page {

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                595,
                842,
                pageNumber
            ).create()

        return document.startPage(
            pageInfo
        )
    }

    private fun sanitasiNamaFile(
        nama: String
    ): String {

        return nama
            .replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            )
            .replace(
                Regex("\\s+"),
                "_"
            )
    }

    private fun savePdfToDownloads(
        pdfDocument: PdfDocument,
        namaFile: String
    ): Uri? {

        return try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val resolver =
                    contentResolver

                val contentValues =
                    ContentValues().apply {

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

                val uri =
                    resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                        ?: return null

                resolver.openOutputStream(
                    uri
                ).use { outputStream ->

                    if (
                        outputStream == null
                    ) {

                        resolver.delete(
                            uri,
                            null,
                            null
                        )

                        return null
                    }

                    pdfDocument.writeTo(
                        outputStream
                    )
                }

                contentValues.clear()

                contentValues.put(
                    MediaStore.Downloads.IS_PENDING,
                    0
                )

                resolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )

                uri

            } else {

                val downloadDir =
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                val folder =
                    File(
                        downloadDir,
                        "BPS Kota Probolinggo"
                    )

                if (
                    !folder.exists()
                ) {

                    folder.mkdirs()
                }

                val file =
                    File(
                        folder,
                        namaFile
                    )

                FileOutputStream(
                    file
                ).use { outputStream ->

                    pdfDocument.writeTo(
                        outputStream
                    )
                }

                Uri.fromFile(
                    file
                )
            }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            null
        }
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Download BPS",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {

                    description =
                        "Notifikasi hasil download PDF BPS"
                }

            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
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

    private fun showDownloadNotification(
        uri: Uri,
        namaFile: String
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
        }

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                setDataAndType(
                    uri,
                    "application/pdf"
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M
                ) {
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.ic_download
                )
                .setContentTitle(
                    "Download selesai"
                )
                .setContentText(
                    namaFile
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "File PDF tersimpan di Downloads/BPS Kota Probolinggo"
                        )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(
                    true
                )
                .build()

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroy() {

        if (
            ::progressLoading.isInitialized
        ) {

            progressLoading.cancelAnimation()
        }

        super.onDestroy()
    }
}