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
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Html
import android.text.Spanned
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.example.bpskota.bps.model.SimdasiDetailData
import com.example.bpskota.bps.model.SimdasiDetailResponse
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class StatistikDetailActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_ID_TABEL = "id_tabel"
        const val EXTRA_TAHUN = "tahun"
        const val EXTRA_JUDUL = "judul"
        const val EXTRA_KODE = "kode"
        const val EXTRA_KETERSEDIAAN_TAHUN = "ketersediaan_tahun"

        private const val CHANNEL_ID =
            "bps_download_channel"

        private const val NOTIFICATION_ID =
            2001

        private const val REQUEST_NOTIFICATION_PERMISSION =
            1001
    }

    private val repository =
        BpsRepository()

    private val API_KEY =
        "008edaaae5d450b1913b31a2cef618c3"

    private val WILAYAH =
        "3574000"

    private lateinit var activityTracker:
            ActivityTracker

    private var detailData:
            SimdasiDetailData? = null

    private var tahunTerpilih =
        2025

    private var tahunTersedia =
        arrayListOf<Int>()

    private lateinit var progressLoading:
            ProgressBar

    private lateinit var containerStatistik:
            LinearLayout

    private lateinit var spinnerTahun:
            Spinner

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_statistik_detail
        )

        initView()

        val idTabel =
            intent.getStringExtra(
                EXTRA_ID_TABEL
            )

        val tahun =
            intent.getIntExtra(
                EXTRA_TAHUN,
                2025
            )

        tahunTerpilih =
            tahun

        tahunTersedia =
            intent.getIntegerArrayListExtra(
                EXTRA_KETERSEDIAAN_TAHUN
            ) ?: arrayListOf()

        val judul =
            intent.getStringExtra(
                EXTRA_JUDUL
            )

        val kode =
            intent.getStringExtra(
                EXTRA_KODE
            )

        if (idTabel.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "ID tabel tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        activityTracker =
            ActivityTracker(
                this,
                BpskpRetrofitClient.api
            )

        activityTracker.trackScreen(
            screen = "StatistikDetail",
            metadata = mapOf(
                "table_id" to idTabel,
                "tahun" to tahun,
                "judul" to (judul ?: ""),
                "kode" to (kode ?: "")
            )
        )

        setupButton()

        createNotificationChannel()

        requestNotificationPermission()

        findViewById<TextView>(
            R.id.tvDetailKode
        ).text =
            "Kode Tabel: ${kode ?: "-"}"

        findViewById<TextView>(
            R.id.tvDetailJudul
        ).text =
            judul
                ?: "Judul tidak tersedia"

        findViewById<TextView>(
            R.id.tvDetailTahun
        ).text =
            tahun.toString()

        setupSpinnerTahun(
            idTabel = idTabel,
            tahunAwal = tahun
        )

        loadDetail(
            tahun = tahun,
            idTabel = idTabel
        )
    }

    private fun initView() {

        progressLoading =
            findViewById(
                R.id.progressLoading
            )

        containerStatistik =
            findViewById(
                R.id.tvDetailData
            )

        spinnerTahun =
            findViewById(
                R.id.spinnerTahun
            )
    }

    private fun setupButton() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }

        findViewById<ImageView>(
            R.id.btnDownload
        ).setOnClickListener {

            tampilkanDialogDownload()
        }
    }

    private fun setupSpinnerTahun(
        idTabel: String,
        tahunAwal: Int
    ) {

        val daftarTahun =
            tahunTersedia
                .distinct()
                .sortedDescending()

        if (daftarTahun.isEmpty()) {

            spinnerTahun.visibility =
                View.GONE

            findViewById<TextView>(
                R.id.tvDetailTahun
            ).visibility =
                View.GONE

            return
        }

        spinnerTahun.visibility =
            View.VISIBLE

        findViewById<TextView>(
            R.id.tvDetailTahun
        ).visibility =
            View.GONE

        val adapter =
            object : ArrayAdapter<Int>(
                this,
                android.R.layout.simple_spinner_item,
                daftarTahun
            ) {

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view =
                        super.getView(
                            position,
                            convertView,
                            parent
                        )

                    val textView =
                        view.findViewById<TextView>(
                            android.R.id.text1
                        )

                    view.setBackgroundColor(
                        Color.WHITE
                    )

                    textView.text =
                        daftarTahun[position].toString()

                    textView.setTextColor(
                        Color.BLACK
                    )

                    textView.textSize =
                        15f

                    textView.setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    textView.gravity =
                        Gravity.CENTER

                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view =
                        super.getDropDownView(
                            position,
                            convertView,
                            parent
                        )

                    val textView =
                        view.findViewById<TextView>(
                            android.R.id.text1
                        )

                    view.setBackgroundColor(
                        Color.WHITE
                    )

                    textView.text =
                        daftarTahun[position].toString()

                    textView.setTextColor(
                        Color.BLACK
                    )

                    textView.textSize =
                        15f

                    textView.setPadding(
                        32,
                        24,
                        32,
                        24
                    )

                    return view
                }
            }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerTahun.adapter =
            adapter

        spinnerTahun.setBackgroundColor(
            Color.WHITE
        )

        val posisiAwal =
            daftarTahun.indexOf(
                tahunAwal
            )

        if (posisiAwal >= 0) {

            spinnerTahun.setSelection(
                posisiAwal,
                false
            )
        }

        spinnerTahun.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                private var pertamaKali =
                    true

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val tahunDipilih =
                        daftarTahun[position]

                    if (pertamaKali) {

                        pertamaKali =
                            false

                        if (
                            tahunDipilih ==
                            tahunAwal
                        ) {

                            return
                        }
                    }

                    if (
                        tahunDipilih ==
                        tahunTerpilih
                    ) {

                        return
                    }

                    tahunTerpilih =
                        tahunDipilih

                    detailData =
                        null

                    loadDetail(
                        tahun = tahunDipilih,
                        idTabel = idTabel
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    private fun tampilkanDialogDownload() {

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

    private fun tampilkanLoading() {

        progressLoading.visibility =
            View.VISIBLE
    }

    private fun sembunyikanLoading() {

        progressLoading.visibility =
            View.GONE
    }

    private fun loadDetail(
        tahun: Int,
        idTabel: String
    ) {

        tampilkanLoading()

        repository
            .getSimdasiDetail(
                tahun = tahun,
                idTabel = idTabel,
                wilayah = WILAYAH,
                apiKey = API_KEY
            )
            .enqueue(
                object :
                    Callback<SimdasiDetailResponse> {

                    override fun onResponse(
                        call:
                        Call<SimdasiDetailResponse>,

                        response:
                        Response<SimdasiDetailResponse>
                    ) {

                        sembunyikanLoading()

                        if (
                            !response.isSuccessful
                        ) {

                            Toast.makeText(
                                this@StatistikDetailActivity,
                                "HTTP Error: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        val body =
                            response.body()

                        if (body == null) {

                            Toast.makeText(
                                this@StatistikDetailActivity,
                                "Response kosong",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {

                            Toast.makeText(
                                this@StatistikDetailActivity,
                                "Status API: ${body.status}",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        if (
                            body.data.isNullOrEmpty()
                        ) {

                            Toast.makeText(
                                this@StatistikDetailActivity,
                                "Data detail tidak tersedia",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }

                        val detail =
                            body.data.firstOrNull {

                                !it.data.isNullOrEmpty()
                            }

                        if (detail == null) {

                            val errorObject =
                                body.data.firstOrNull {
                                    it.condition == "ERROR"
                                }

                            Toast.makeText(
                                this@StatistikDetailActivity,
                                errorObject?.message
                                    ?: "Detail tabel tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }

                        detailData =
                            detail

                        tahunTerpilih =
                            tahun

                        tampilkanDetail(
                            detail
                        )
                    }

                    override fun onFailure(
                        call:
                        Call<SimdasiDetailResponse>,

                        t: Throwable
                    ) {

                        sembunyikanLoading()

                        Toast.makeText(
                            this@StatistikDetailActivity,
                            "Gagal mengambil data: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun tampilkanDetail(
        detail: SimdasiDetailData
    ) {

        val tvKode =
            findViewById<TextView>(
                R.id.tvDetailKode
            )

        val tvJudul =
            findViewById<TextView>(
                R.id.tvDetailJudul
            )

        val tvTahun =
            findViewById<TextView>(
                R.id.tvDetailTahun
            )

        val tvBab =
            findViewById<TextView>(
                R.id.tvDetailBab
            )

        val tvSubject =
            findViewById<TextView>(
                R.id.tvDetailSubject
            )

        val tvWilayah =
            findViewById<TextView>(
                R.id.tvDetailWilayah
            )

        val tvSumber =
            findViewById<TextView>(
                R.id.tvDetailSumber
            )

        tvKode.text =
            renderHtml(
                "ID Subject: ${detail.idSubject ?: "-"}"
            )

        tvJudul.text =
            renderHtml(
                detail.judulTabel
                    ?: "Judul tabel tidak tersedia"
            )

        tvTahun.text =
            renderHtml(
                "${detail.tahunData ?: tahunTerpilih}"
            )

        tvBab.text =
            renderHtml(
                "Bab: ${detail.bab ?: "-"}"
            )

        tvSubject.text =
            renderHtml(
                "Subject: ${detail.subject ?: "-"}"
            )

        tvWilayah.text =
            renderHtml(
                "Wilayah: ${detail.wilayah ?: "-"}"
            )

        tvSumber.text =
            renderHtml(
                "Sumber: ${detail.sumber ?: "-"}"
            )

        tampilkanCardStatistik(
            detail
        )
    }

    private fun tampilkanCardStatistik(
        detail: SimdasiDetailData
    ) {

        containerStatistik.removeAllViews()

        if (
            detail.data.isNullOrEmpty()
        ) {

            tampilkanPesanKosong()

            return
        }

        val inflater =
            LayoutInflater.from(this)

        detail.data.forEach {
                wilayah ->

            val namaWilayah =
                wilayah.label
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Tidak diketahui"

            val card =
                inflater.inflate(
                    R.layout.card_statistik,
                    containerStatistik,
                    false
                )

            val tvNamaWilayah =
                card.findViewById<TextView>(
                    R.id.tvNamaWilayah
                )

            val containerVariable =
                card.findViewById<LinearLayout>(
                    R.id.containerVariable
                )

            tvNamaWilayah.text =
                renderHtml(
                    namaWilayah
                )

            containerVariable.removeAllViews()

            if (
                wilayah.variables.isNullOrEmpty()
            ) {

                val tvKosong =
                    TextView(this)

                tvKosong.text =
                    "Tidak ada data"

                tvKosong.textSize =
                    14f

                tvKosong.setTextColor(
                    android.graphics.Color.GRAY
                )

                containerVariable.addView(
                    tvKosong
                )

            } else {

                wilayah.variables.forEach {
                        (kodeVariable, variable) ->

                    val namaVariabel =
                        detail.kolom
                            ?.get(kodeVariable)
                            ?.namaVariabel
                            ?: kodeVariable

                    val nilai =
                        variable.value
                            ?: "-"

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
                        ).apply {

                            bottomMargin =
                                dpToPx(8)
                        }

                    val tvVariable =
                        TextView(this)

                    tvVariable.text =
                        renderHtml(
                            namaVariabel
                        )

                    tvVariable.textSize =
                        14f

                    tvVariable.setTextColor(
                        android.graphics.Color.rgb(
                            55,
                            65,
                            81
                        )
                    )

                    tvVariable.layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )

                    val tvNilai =
                        TextView(this)

                    tvNilai.text =
                        renderHtml(
                            nilai.toString()
                        )

                    tvNilai.textSize =
                        15f

                    tvNilai.setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    tvNilai.setTextColor(
                        android.graphics.Color.rgb(
                            249,
                            115,
                            22
                        )
                    )

                    tvNilai.gravity =
                        Gravity.END

                    tvNilai.layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                    row.addView(
                        tvVariable
                    )

                    row.addView(
                        tvNilai
                    )

                    containerVariable.addView(
                        row
                    )
                }
            }

            containerStatistik.addView(
                card
            )
        }

        if (
            containerStatistik.childCount == 0
        ) {

            tampilkanPesanKosong()
        }
    }

    private fun renderHtml(
        text: String
    ): Spanned {

        return HtmlCompat.fromHtml(
            text,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun getPlainText(
        text: String
    ): String {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {

            Html.fromHtml(
                text,
                Html.FROM_HTML_MODE_LEGACY
            ).toString()

        } else {

            @Suppress("DEPRECATION")
            Html.fromHtml(
                text
            ).toString()
        }
    }

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }

    private fun tampilkanPesanKosong() {

        val textView =
            TextView(this)

        textView.text =
            "Data tabel tidak tersedia"

        textView.textSize =
            14f

        textView.setTextColor(
            android.graphics.Color.GRAY
        )

        textView.setPadding(
            8,
            16,
            8,
            16
        )

        containerStatistik.addView(
            textView
        )
    }

    private fun downloadPdf() {

        val detail =
            detailData

        if (detail == null) {

            Toast.makeText(
                this,
                "Data belum selesai dimuat",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            detail.data.isNullOrEmpty()
        ) {

            Toast.makeText(
                this,
                "Tidak ada data untuk diunduh",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val judul =
                getPlainText(
                    detail.judulTabel
                        ?: "data_bps"
                )
                    .replace(
                        Regex("[^a-zA-Z0-9\\s]"),
                        ""
                    )
                    .replace(
                        Regex("\\s+"),
                        "_"
                    )
                    .take(60)
                    .ifEmpty {
                        "data_bps"
                    }

            val namaFile =
                "${judul}_${tahunTerpilih}.pdf"

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

            val pageWidth =
                page.info.pageWidth

            val pageHeight =
                page.info.pageHeight

            val margin =
                40f

            var y =
                50f

            val titlePaint =
                android.graphics.Paint().apply {

                    textSize =
                        18f

                    typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    isAntiAlias =
                        true
                }

            val headerPaint =
                android.graphics.Paint().apply {

                    textSize =
                        11f

                    typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    isAntiAlias =
                        true
                }

            val normalPaint =
                android.graphics.Paint().apply {

                    textSize =
                        10f

                    isAntiAlias =
                        true
                }

            val variablePaint =
                android.graphics.Paint().apply {

                    textSize =
                        10f

                    isAntiAlias =
                        true
                }

            val valuePaint =
                android.graphics.Paint().apply {

                    textSize =
                        10f

                    typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    isAntiAlias =
                        true
                }

            canvas.drawText(
                "DATA STATISTIK BPS KOTA PROBOLINGGO",
                margin,
                y,
                titlePaint
            )

            y +=
                28f

            canvas.drawText(
                getPlainText(
                    detail.judulTabel
                        ?: "Judul tabel tidak tersedia"
                ),
                margin,
                y,
                headerPaint
            )

            y +=
                20f

            canvas.drawText(
                "Tahun: ${
                    getPlainText(
                        detail.tahunData
                            ?.toString()
                            ?: tahunTerpilih.toString()
                    )
                }",
                margin,
                y,
                normalPaint
            )

            y +=
                16f

            canvas.drawText(
                "Wilayah: ${
                    getPlainText(
                        detail.wilayah ?: "-"
                    )
                }",
                margin,
                y,
                normalPaint
            )

            y +=
                16f

            canvas.drawText(
                "Bab: ${
                    getPlainText(
                        detail.bab ?: "-"
                    )
                }",
                margin,
                y,
                normalPaint
            )

            y +=
                16f

            canvas.drawText(
                "Subject: ${
                    getPlainText(
                        detail.subject ?: "-"
                    )
                }",
                margin,
                y,
                normalPaint
            )

            y +=
                16f

            canvas.drawText(
                "Sumber: ${
                    getPlainText(
                        detail.sumber ?: "-"
                    )
                }",
                margin,
                y,
                normalPaint
            )

            y +=
                28f

            canvas.drawLine(
                margin,
                y,
                pageWidth - margin,
                y,
                normalPaint
            )

            y +=
                25f

            detail.data.forEach {
                    wilayah ->

                val namaWilayah =
                    wilayah.label
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Tidak diketahui"

                if (
                    y >
                    pageHeight - 120
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
                    getPlainText(
                        namaWilayah
                    ),
                    margin,
                    y,
                    headerPaint
                )

                y +=
                    20f

                if (
                    wilayah.variables.isNullOrEmpty()
                ) {

                    canvas.drawText(
                        "Tidak ada data",
                        margin + 10f,
                        y,
                        normalPaint
                    )

                    y +=
                        18f

                } else {

                    wilayah.variables.forEach {
                            (kodeVariable, variable) ->

                        val namaVariabel =
                            detail.kolom
                                ?.get(kodeVariable)
                                ?.namaVariabel
                                ?: kodeVariable

                        val nilai =
                            variable.value
                                ?: "-"

                        if (
                            y >
                            pageHeight - 60
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
                            getPlainText(
                                namaVariabel
                            ),
                            margin + 10f,
                            y,
                            variablePaint
                        )

                        canvas.drawText(
                            getPlainText(
                                nilai.toString()
                            ),
                            pageWidth - margin - 80f,
                            y,
                            valuePaint
                        )

                        y +=
                            17f
                    }
                }

                y +=
                    5f

                canvas.drawLine(
                    margin,
                    y,
                    pageWidth - margin,
                    y,
                    normalPaint
                )

                y +=
                    20f
            }

            pdfDocument.finishPage(
                page
            )

            val uri =
                savePdfToDownloads(
                    pdfDocument,
                    namaFile
                )

            pdfDocument.close()

            if (uri == null) {

                Toast.makeText(
                    this,
                    "Gagal menyimpan PDF",
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
                "PDF berhasil diunduh",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

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

    private fun savePdfToDownloads(
        pdfDocument: PdfDocument,
        namaFile: String
    ): Uri? {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            val resolver =
                contentResolver

            val values =
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
                    values
                )

            if (uri == null) {
                return null
            }

            try {

                resolver.openOutputStream(
                    uri
                ).use { outputStream ->

                    if (outputStream == null) {

                        throw Exception(
                            "OutputStream tidak tersedia"
                        )
                    }

                    pdfDocument.writeTo(
                        outputStream
                    )
                }

                values.clear()

                values.put(
                    MediaStore.Downloads.IS_PENDING,
                    0
                )

                resolver.update(
                    uri,
                    values,
                    null,
                    null
                )

                return uri

            } catch (e: Exception) {

                resolver.delete(
                    uri,
                    null,
                    null
                )

                throw e
            }
        }

        val downloads =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        val folder =
            File(
                downloads,
                "BPS Kota Probolinggo"
            )

        if (!folder.exists()) {

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

        return Uri.fromFile(
            file
        )
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
                )

            channel.description =
                "Notifikasi hasil download data BPS"

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

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat
                .Builder(
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
                            "File $namaFile berhasil disimpan ke Download/BPS Kota Probolinggo"
                        )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(
                    true
                )
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
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

    override fun onDestroy() {

        if (
            ::progressLoading.isInitialized
        ) {

            progressLoading.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}