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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.bpskota.bps.model.TempatTinggalDataResponse
import com.example.bpskota.bps.repository.BpsRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

class TempatTinggalDetailActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "TEMPAT_TINGGAL_DETAIL"

        private const val DOMAIN = "3574"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        const val EXTRA_VAR_ID =
            "tempattinggal_var_id"

        const val EXTRA_JUDUL =
            "tempattinggal_judul"

        const val EXTRA_TAHUN =
            "tempattinggal_tahun"

        private const val CHANNEL_ID =
            "bps_tempat_tinggal_download_channel"

        private const val NOTIFICATION_ID = 3001

        private const val REQUEST_NOTIFICATION_PERMISSION = 3002
    }

    private lateinit var tvDetailJudul: TextView
    private lateinit var tvDetailTahun: TextView
    private lateinit var tvDetailKode: TextView
    private lateinit var tvDetailBab: TextView
    private lateinit var tvDetailSubject: TextView
    private lateinit var tvDetailWilayah: TextView
    private lateinit var tvDetailSumber: TextView
    private lateinit var tvDetailData: LinearLayout
    private lateinit var progressLoading: ProgressBar

    private val repository = BpsRepository()

    private var varId = -1

    private var tahunTerpilih = 2025

    private var detailData: TempatTinggalDataResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_tempat_tinggal_detail
        )

        initView()
        setupButton()
        createNotificationChannel()
        requestNotificationPermission()
        ambilIntent()

        if (varId <= 0) {
            Toast.makeText(
                this,
                "ID variabel tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        loadDetail()
    }

    private fun initView() {

        tvDetailJudul =
            findViewById(R.id.tvDetailJudul)

        tvDetailTahun =
            findViewById(R.id.tvDetailTahun)

        tvDetailKode =
            findViewById(R.id.tvDetailKode)

        tvDetailBab =
            findViewById(R.id.tvDetailBab)

        tvDetailSubject =
            findViewById(R.id.tvDetailSubject)

        tvDetailWilayah =
            findViewById(R.id.tvDetailWilayah)

        tvDetailSumber =
            findViewById(R.id.tvDetailSumber)

        tvDetailData =
            findViewById(R.id.tvDetailData)

        progressLoading =
            findViewById(R.id.progressLoading)
    }

    private fun ambilIntent() {

        varId =
            intent.getIntExtra(
                EXTRA_VAR_ID,
                -1
            )

        tahunTerpilih =
            intent.getIntExtra(
                EXTRA_TAHUN,
                2025
            )

        val judul =
            intent.getStringExtra(
                EXTRA_JUDUL
            )

        tvDetailJudul.text =
            judul ?: "Statistik"

        tvDetailTahun.text =
            tahunTerpilih.toString()

        Log.d(
            TAG,
            "VAR ID = $varId"
        )

        Log.d(
            TAG,
            "JUDUL = $judul"
        )

        Log.d(
            TAG,
            "TAHUN = $tahunTerpilih"
        )

        Log.d(
            TAG,
            "KODE TAHUN BPS = ${kodeTahunBps(tahunTerpilih)}"
        )
    }

    private fun kodeTahunBps(
        tahun: Int
    ): Int {
        return tahun - 1900
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

    private fun tampilkanDialogDownload() {

        AlertDialog.Builder(this)
            .setTitle("Download PDF")
            .setMessage(
                "Apakah Anda ingin mengunduh data statistik ini dalam bentuk PDF?"
            )
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Ya") { _, _ ->
                downloadPdf()
            }
            .show()
    }

    private fun loadDetail() {

        progressLoading.visibility =
            View.VISIBLE

        tvDetailData.removeAllViews()

        val kodeTahun =
            kodeTahunBps(
                tahunTerpilih
            )

        repository.getTempatTinggalData(
            domain = DOMAIN,
            variable = varId,
            tahun = kodeTahun,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<TempatTinggalDataResponse> {

                override fun onResponse(
                    call: Call<TempatTinggalDataResponse>,
                    response: Response<TempatTinggalDataResponse>
                ) {

                    progressLoading.visibility =
                        View.GONE

                    Log.d(
                        TAG,
                        "HTTP CODE = ${response.code()}"
                    )

                    if (!response.isSuccessful) {

                        tampilkanPesan(
                            "Gagal mengambil data: ${response.code()}"
                        )

                        return
                    }

                    val body =
                        response.body()

                    if (body == null) {

                        tampilkanPesan(
                            "Response kosong"
                        )

                        return
                    }

                    Log.d(
                        TAG,
                        "STATUS = ${body.status}"
                    )

                    Log.d(
                        TAG,
                        "DATA AVAILABILITY = ${body.dataAvailability}"
                    )

                    Log.d(
                        TAG,
                        "SUBJECT = ${body.subject}"
                    )

                    Log.d(
                        TAG,
                        "VARIABLE = ${body.variable}"
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

                    if (
                        body.status
                            ?.uppercase(Locale.ROOT) != "OK"
                    ) {

                        tampilkanPesan(
                            "Data BPS tidak tersedia"
                        )

                        return
                    }

                    if (
                        body.dataAvailability
                            ?.lowercase(Locale.ROOT) != "available"
                    ) {

                        tampilkanPesan(
                            "Data statistik tidak tersedia"
                        )

                        return
                    }

                    if (
                        body.dataContent.isNullOrEmpty()
                    ) {

                        tampilkanPesan(
                            "Data statistik tidak tersedia untuk tahun $tahunTerpilih"
                        )

                        return
                    }

                    detailData = body

                    tampilkanDetail(body)
                }

                override fun onFailure(
                    call: Call<TempatTinggalDataResponse>,
                    t: Throwable
                ) {

                    progressLoading.visibility =
                        View.GONE

                    Log.e(
                        TAG,
                        "REQUEST ERROR",
                        t
                    )

                    tampilkanPesan(
                        "Gagal terhubung ke server BPS"
                    )
                }
            }
        )
    }

    private fun tampilkanDetail(
        body: TempatTinggalDataResponse
    ) {

        tvDetailData.removeAllViews()

        val variable =
            body.variable?.firstOrNull()

        if (variable != null) {

            tvDetailJudul.text =
                variable.label
                    ?: tvDetailJudul.text

            if (!variable.unit.isNullOrBlank()) {

                tvDetailData.addView(
                    buatInfoUnit(
                        variable.unit!!
                    )
                )
            }
        }

        tvDetailTahun.text =
            tahunTerpilih.toString()

        if (body.vervar.isNullOrEmpty()) {

            tampilkanPesan(
                "Data wilayah tidak tersedia"
            )

            return
        }

        if (body.dataContent.isNullOrEmpty()) {

            tampilkanPesan(
                "Data statistik tidak tersedia untuk tahun $tahunTerpilih"
            )

            return
        }

        tvDetailData.addView(
            buatSectionTitle(
                body.labelVerVar ?: "Data"
            )
        )

        tampilkanData(body)
    }

    private fun tampilkanData(
        body: TempatTinggalDataResponse
    ) {

        val variable =
            body.variable?.firstOrNull()

        val dataContent =
            body.dataContent ?: return

        val variableVal =
            variable?.value

        if (variableVal == null) {

            tampilkanPesan(
                "Kode variabel tidak ditemukan"
            )

            return
        }

        val tahunVal =
            kodeTahunBps(
                tahunTerpilih
            )

        val turTahunVal =
            body.turtahun
                ?.firstOrNull()
                ?.value
                ?: 0

        var jumlahDitampilkan = 0

        for (item in body.vervar.orEmpty()) {

            val vervarVal =
                item.value ?: continue

            val label =
                item.label ?: "-"

            val key =
                "${vervarVal}${variableVal}0${tahunVal}${turTahunVal}"

            val nilai =
                dataContent[key]

            Log.d(
                TAG,
                "KEY = $key | LABEL = $label | NILAI = $nilai"
            )

            if (nilai != null) {

                tambahBarisData(
                    label = label,
                    nilai = formatNilai(nilai)
                )

                jumlahDitampilkan++
            }
        }

        if (jumlahDitampilkan == 0) {

            tampilkanPesan(
                "Data statistik tidak tersedia untuk tahun $tahunTerpilih"
            )
        }
    }

    private fun tambahBarisData(
        label: String,
        nilai: String
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
                    dpToPx(10)
            }

        val tvLabel =
            TextView(this)

        tvLabel.text =
            label

        tvLabel.textSize =
            14f

        tvLabel.setTextColor(
            Color.rgb(
                75,
                85,
                99
            )
        )

        val tvNilai =
            TextView(this)

        tvNilai.text =
            nilai

        tvNilai.textSize =
            18f

        tvNilai.setTypeface(
            null,
            Typeface.BOLD
        )

        tvNilai.setTextColor(
            Color.rgb(
                249,
                115,
                22
            )
        )

        tvNilai.setPadding(
            0,
            dpToPx(5),
            0,
            0
        )

        container.addView(tvLabel)
        container.addView(tvNilai)

        tvDetailData.addView(container)
    }

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

        container.addView(label)
        container.addView(value)

        return container
    }

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

        return textView
    }

    private fun formatNilai(
        value: Double
    ): String {

        return if (value % 1.0 == 0.0) {

            NumberFormat
                .getNumberInstance(
                    Locale("id", "ID")
                )
                .format(value)

        } else {

            String.format(
                Locale("id", "ID"),
                "%.2f",
                value
            )
        }
    }

    private fun tampilkanPesan(
        pesan: String
    ) {

        tvDetailData.removeAllViews()

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

        tvDetailData.addView(text)
    }

    private fun downloadPdf() {

        val body =
            detailData

        if (body == null) {

            Toast.makeText(
                this,
                "Data belum selesai dimuat",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (body.dataContent.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "Tidak ada data untuk diunduh",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val variable =
                body.variable?.firstOrNull()

            val judul =
                variable?.label
                    ?.replace(
                        Regex("[^a-zA-Z0-9\\s]"),
                        ""
                    )
                    ?.replace(
                        Regex("\\s+"),
                        "_"
                    )
                    ?.take(60)
                    ?: "data_tempat_tinggal"

            val namaFile =
                "${judul}_${tahunTerpilih}.pdf"

            val pdfDocument =
                PdfDocument()

            var pageNumber = 1

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

            val margin = 40f

            var y = 50f

            val titlePaint =
                Paint().apply {

                    textSize = 18f

                    typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    isAntiAlias = true
                }

            val headerPaint =
                Paint().apply {

                    textSize = 11f

                    typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    isAntiAlias = true
                }

            val normalPaint =
                Paint().apply {

                    textSize = 10f

                    isAntiAlias = true
                }

            val labelPaint =
                Paint().apply {

                    textSize = 10f

                    isAntiAlias = true
                }

            val valuePaint =
                Paint().apply {

                    textSize = 10f

                    typeface =
                        Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )

                    isAntiAlias = true
                }

            canvas.drawText(
                "DATA STATISTIK BPS KOTA PROBOLINGGO",
                margin,
                y,
                titlePaint
            )

            y += 28f

            canvas.drawText(
                variable?.label
                    ?: "Data Tempat Tinggal",
                margin,
                y,
                headerPaint
            )

            y += 20f

            canvas.drawText(
                "Tahun: $tahunTerpilih",
                margin,
                y,
                normalPaint
            )

            y += 16f

            canvas.drawText(
                "Subject: ${body.subject ?: "-"}",
                margin,
                y,
                normalPaint
            )

            y += 16f

            if (!variable?.unit.isNullOrBlank()) {

                canvas.drawText(
                    "Satuan: ${variable?.unit}",
                    margin,
                    y,
                    normalPaint
                )

                y += 16f
            }

            canvas.drawText(
                "Wilayah: ${body.labelVerVar ?: "-"}",
                margin,
                y,
                normalPaint
            )

            y += 28f

            canvas.drawLine(
                margin,
                y,
                pageWidth - margin,
                y,
                normalPaint
            )

            y += 25f

            val variableVal =
                variable?.value

            if (variableVal == null) {

                pdfDocument.finishPage(page)
                pdfDocument.close()

                Toast.makeText(
                    this,
                    "Kode variabel tidak ditemukan",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            val tahunVal =
                kodeTahunBps(
                    tahunTerpilih
                )

            val turTahunVal =
                body.turtahun
                    ?.firstOrNull()
                    ?.value
                    ?: 0

            val dataContent =
                body.dataContent ?: emptyMap()

            var jumlahData = 0

            for (item in body.vervar.orEmpty()) {

                val vervarVal =
                    item.value ?: continue

                val label =
                    item.label ?: "-"

                val key =
                    "${vervarVal}${variableVal}0${tahunVal}${turTahunVal}"

                val nilai =
                    dataContent[key]

                Log.d(
                    TAG,
                    "PDF KEY = $key | LABEL = $label | NILAI = $nilai"
                )

                if (nilai == null) {
                    continue
                }

                if (y > pageHeight - 70f) {

                    pdfDocument.finishPage(page)

                    pageNumber++

                    page =
                        createPdfPage(
                            pdfDocument,
                            pageNumber
                        )

                    canvas =
                        page.canvas

                    y = 50f
                }

                canvas.drawText(
                    label,
                    margin,
                    y,
                    labelPaint
                )

                canvas.drawText(
                    formatNilai(nilai),
                    pageWidth - margin - 100f,
                    y,
                    valuePaint
                )

                y += 20f

                jumlahData++
            }

            if (jumlahData == 0) {

                canvas.drawText(
                    "Data statistik tidak tersedia",
                    margin,
                    y,
                    normalPaint
                )
            }

            pdfDocument.finishPage(page)

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

            Log.d(
                TAG,
                "PDF berhasil disimpan: $uri"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Gagal membuat PDF",
                e
            )

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

        return document.startPage(pageInfo)
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
                    ?: return null

            try {

                resolver.openOutputStream(uri).use {
                        outputStream ->

                    if (outputStream == null) {

                        throw Exception(
                            "OutputStream tidak tersedia"
                        )
                    }

                    pdfDocument.writeTo(
                        outputStream
                    )
                }

                val updateValues =
                    ContentValues().apply {

                        put(
                            MediaStore.Downloads.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    uri,
                    updateValues,
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

        FileOutputStream(file).use {
                outputStream ->

            pdfDocument.writeTo(
                outputStream
            )
        }

        return Uri.fromFile(file)
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

                Log.w(
                    TAG,
                    "Permission notifikasi belum diberikan"
                )

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
                .setAutoCancel(true)
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

            progressLoading.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}