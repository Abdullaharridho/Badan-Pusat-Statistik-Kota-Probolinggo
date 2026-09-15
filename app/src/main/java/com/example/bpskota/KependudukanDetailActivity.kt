package com.example.bpskota

import android.Manifest
import android.app.AlertDialog
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
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.KependudukanDataResponse
import com.example.bpskota.bps.model.KependudukanStaticDetailData
import com.example.bpskota.bps.model.KependudukanStaticDetailResponse
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream

class KependudukanDetailActivity : AppCompatActivity() {

    companion object {
        private const val DOMAIN = "3574"
        private const val API_KEY = "008edaaae5d450b1913b31a2cef618c3"

        const val EXTRA_VARIABLE = "EXTRA_VARIABLE"
        const val EXTRA_TAHUN = "EXTRA_TAHUN"
        const val EXTRA_JUDUL = "EXTRA_JUDUL"
        const val EXTRA_STATIC_BUTUH_TAHUN = "EXTRA_STATIC_BUTUH_TAHUN"
        const val EXTRA_IS_SIMDASI = "EXTRA_IS_SIMDASI"
        const val EXTRA_TAHUN_TERSEDIA = "EXTRA_TAHUN_TERSEDIA"
        const val EXTRA_IS_VARIABLE = "EXTRA_IS_VARIABLE"

        private const val CHANNEL_ID = "bps_download_channel"
        private const val NOTIFICATION_ID = 2002
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002
    }

    private val repository = BpsRepository()

    private lateinit var activityTracker: ActivityTracker

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnDownload: ImageView
    private lateinit var tvJudul: TextView
    private lateinit var tvTahun: TextView

    private var variableId = 0
    private var tahun = 0
    private var judul = ""

    private var staticButuhTahun = false
    private var isSimdasi = false
    private var isVariable = false

    private val daftarTahun = linkedMapOf<Int, Int>()

    private var variablePdfData: MutableList<PdfWilayahData> =
        mutableListOf()

    private var staticPdfRows: MutableList<PdfStaticRow> =
        mutableListOf()

    private var staticPdfTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kependudukan_detail)

        initView()
        ambilIntent()
        setupButton()

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        val tipeData = when {
            isSimdasi -> "SIMDASI"
            isVariable -> "VARIABLE"
            else -> "STATIC"
        }

        activityTracker.trackScreen(
            screen = "KependudukanDetail",
            metadata = mapOf(
                "data_id" to variableId,
                "tahun" to tahun,
                "judul" to judul,
                "type" to tipeData
            )
        )

        createNotificationChannel()
        requestNotificationPermission()

        loadDetail()
    }

    private fun initView() {
        cardContainer = findViewById(R.id.cardContainer)

        progressLoading = findViewById(R.id.progressLoading)
        progressLoading.setAnimation("Loading_Animation.json")
        progressLoading.repeatCount = -1

        btnBack = findViewById(R.id.btnBack)
        btnDownload = findViewById(R.id.btnDownload)

        tvJudul = findViewById(R.id.tvJudul)
        tvTahun = findViewById(R.id.tvTahun)
    }

    private fun ambilIntent() {
        variableId = intent.getIntExtra(EXTRA_VARIABLE, 0)
        tahun = intent.getIntExtra(EXTRA_TAHUN, 0)
        judul = intent.getStringExtra(EXTRA_JUDUL) ?: ""

        staticButuhTahun = intent.getBooleanExtra(
            EXTRA_STATIC_BUTUH_TAHUN,
            false
        )

        isSimdasi = intent.getBooleanExtra(
            EXTRA_IS_SIMDASI,
            false
        )

        isVariable = intent.getBooleanExtra(
            EXTRA_IS_VARIABLE,
            false
        )

        val tahunTersedia =
            intent.getIntegerArrayListExtra(
                EXTRA_TAHUN_TERSEDIA
            )

        if (tahunTersedia != null) {
            tahunTersedia
                .distinct()
                .sortedDescending()
                .forEach { tahunItem ->
                    daftarTahun[tahunItem] = 0
                }
        }

        tvJudul.text = judul.ifBlank {
            "Data Kependudukan"
        }

        tvTahun.text = if (tahun > 0) {
            tahun.toString()
        } else {
            "-"
        }
    }

    private fun setupButton() {
        btnBack.setOnClickListener {
            finish()
        }

        tvTahun.setOnClickListener {
            if (daftarTahun.size > 1) {
                tampilkanFilterTahun()
            }
        }

        btnDownload.setOnClickListener {
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

    private fun downloadPdf() {
        if (isVariable) {
            if (variablePdfData.isEmpty()) {
                Toast.makeText(
                    this,
                    "Data belum selesai dimuat atau tidak tersedia",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            downloadPdfVariable()
            return
        }

        if (staticPdfRows.isEmpty()) {
            Toast.makeText(
                this,
                "Data belum selesai dimuat atau tidak tersedia",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        downloadPdfStatic()
    }

    private fun downloadPdfVariable() {
        try {
            val judulFile = sanitasiNamaFile(
                tvJudul.text.toString()
            )

            val namaFile = "${judulFile}_${tahun}.pdf"

            val pdfDocument = PdfDocument()

            var pageNumber = 1

            var page = createPdfPage(
                pdfDocument,
                pageNumber
            )

            var canvas = page.canvas

            val pageWidth = page.info.pageWidth
            val pageHeight = page.info.pageHeight

            val margin = 40f
            var y = 50f

            val titlePaint = Paint().apply {
                textSize = 18f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                textSize = 11f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
                isAntiAlias = true
            }

            val normalPaint = Paint().apply {
                textSize = 10f
                isAntiAlias = true
            }

            val valuePaint = Paint().apply {
                textSize = 10f
                typeface = Typeface.create(
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
                tvJudul.text.toString(),
                margin,
                y,
                headerPaint
            )

            y += 20f

            canvas.drawText(
                "Tahun: $tahun",
                margin,
                y,
                normalPaint
            )

            y += 16f

            canvas.drawText(
                "Jenis Data: Variabel Statistik",
                margin,
                y,
                normalPaint
            )

            y += 16f

            canvas.drawText(
                "Domain: $DOMAIN",
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

            variablePdfData.forEach { item ->
                if (y > pageHeight - 100) {
                    pdfDocument.finishPage(page)

                    pageNumber++

                    page = createPdfPage(
                        pdfDocument,
                        pageNumber
                    )

                    canvas = page.canvas

                    y = 50f
                }

                canvas.drawText(
                    item.wilayah,
                    margin,
                    y,
                    headerPaint
                )

                y += 20f

                canvas.drawText(
                    "Nilai:",
                    margin + 10f,
                    y,
                    normalPaint
                )

                canvas.drawText(
                    item.nilai,
                    pageWidth - margin - 100f,
                    y,
                    valuePaint
                )

                y += 20f

                canvas.drawLine(
                    margin,
                    y,
                    pageWidth - margin,
                    y,
                    normalPaint
                )

                y += 20f
            }

            pdfDocument.finishPage(page)

            val uri = savePdfToDownloads(
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

    private fun downloadPdfStatic() {
        try {
            val judulPdf = staticPdfTitle.ifBlank {
                tvJudul.text.toString()
            }

            val judulFile = sanitasiNamaFile(
                judulPdf
            )

            val tahunFile = if (tahun > 0) {
                tahun.toString()
            } else {
                "data"
            }

            val namaFile = "${judulFile}_${tahunFile}.pdf"

            val pdfDocument = PdfDocument()

            var pageNumber = 1

            var page = createPdfPage(
                pdfDocument,
                pageNumber
            )

            var canvas = page.canvas

            val pageWidth = page.info.pageWidth
            val pageHeight = page.info.pageHeight

            val margin = 40f
            var y = 50f

            val titlePaint = Paint().apply {
                textSize = 18f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                textSize = 11f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
                isAntiAlias = true
            }

            val normalPaint = Paint().apply {
                textSize = 10f
                isAntiAlias = true
            }

            val valuePaint = Paint().apply {
                textSize = 10f
                typeface = Typeface.create(
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
                judulPdf,
                margin,
                y,
                headerPaint
            )

            y += 20f

            if (tahun > 0) {
                canvas.drawText(
                    "Tahun: $tahun",
                    margin,
                    y,
                    normalPaint
                )

                y += 16f
            }

            canvas.drawText(
                "Jenis Data: Tabel Statistik",
                margin,
                y,
                normalPaint
            )

            y += 16f

            canvas.drawText(
                "Domain: $DOMAIN",
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

            staticPdfRows.forEach { row ->
                if (y > pageHeight - 120) {
                    pdfDocument.finishPage(page)

                    pageNumber++

                    page = createPdfPage(
                        pdfDocument,
                        pageNumber
                    )

                    canvas = page.canvas

                    y = 50f
                }

                canvas.drawText(
                    row.wilayah,
                    margin,
                    y,
                    headerPaint
                )

                y += 20f

                row.data.forEach { dataItem ->
                    if (y > pageHeight - 60) {
                        pdfDocument.finishPage(page)

                        pageNumber++

                        page = createPdfPage(
                            pdfDocument,
                            pageNumber
                        )

                        canvas = page.canvas

                        y = 50f
                    }

                    canvas.drawText(
                        dataItem.first,
                        margin + 10f,
                        y,
                        normalPaint
                    )

                    canvas.drawText(
                        dataItem.second,
                        pageWidth - margin - 100f,
                        y,
                        valuePaint
                    )

                    y += 17f
                }

                y += 5f

                canvas.drawLine(
                    margin,
                    y,
                    pageWidth - margin,
                    y,
                    normalPaint
                )

                y += 20f
            }

            pdfDocument.finishPage(page)

            val uri = savePdfToDownloads(
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
        val pageInfo = PdfDocument.PageInfo.Builder(
            595,
            842,
            pageNumber
        ).create()

        return document.startPage(pageInfo)
    }

    private fun sanitasiNamaFile(
        nama: String
    ): String {
        val hasil = nama
            .replace(
                Regex("[^a-zA-Z0-9\\s]"),
                ""
            )
            .replace(
                Regex("\\s+"),
                "_"
            )
            .trim('_')
            .take(60)

        return hasil.ifBlank {
            "data_kependudukan"
        }
    }

    private fun savePdfToDownloads(
        pdfDocument: PdfDocument,
        namaFile: String
    ): Uri? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver

            val values = ContentValues().apply {
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

            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: return null

            try {
                resolver.openOutputStream(uri).use { outputStream ->
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

        val folder = File(
            downloads,
            "BPS Kota Probolinggo"
        )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(
            folder,
            namaFile
        )

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        return Uri.fromFile(file)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(
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
                    NotificationCompat.BigTextStyle().bigText(
                        "File $namaFile berhasil disimpan ke " +
                                "Download/BPS Kota Probolinggo"
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

    private fun tampilkanLoading() {
        progressLoading.visibility =
            View.VISIBLE

        if (!progressLoading.isAnimating) {
            progressLoading.playAnimation()
        }
    }

    private fun sembunyikanLoading() {
        progressLoading.cancelAnimation()

        progressLoading.visibility =
            View.GONE
    }

    private fun loadDetail() {
        if (variableId <= 0) {
            tampilkanPesan(
                "ID tabel tidak tersedia"
            )

            return
        }

        tampilkanLoading()

        cardContainer.removeAllViews()

        variablePdfData.clear()
        staticPdfRows.clear()
        staticPdfTitle = ""

        if (isVariable) {
            if (
                tahun <= 0 &&
                daftarTahun.isNotEmpty()
            ) {
                tahun =
                    daftarTahun.keys.maxOrNull()
                        ?: 0
            }

            loadVariableData()

            return
        }

        if (!staticButuhTahun) {
            loadStaticTable()

            return
        }

        if (
            tahun <= 0 &&
            daftarTahun.isNotEmpty()
        ) {
            tahun =
                daftarTahun.keys.maxOrNull()
                    ?: 0
        }

        if (tahun <= 0) {
            sembunyikanLoading()

            tampilkanPesan(
                "Tahun tidak tersedia"
            )

            return
        }

        loadStaticTable()
    }

    private fun loadVariableData() {
        val tahunIdBps =
            tahun - 1900

        fetchDataVariabelDenganIdTahun(
            tahunIdBps
        )
    }

    private fun fetchDataVariabelDenganIdTahun(
        tahunIdBps: Int
    ) {
        repository
            .getKependudukanDetail(
                domain = DOMAIN,
                variable = variableId,
                tahun = tahunIdBps,
                apiKey = API_KEY
            )
            .enqueue(
                object :
                    retrofit2.Callback<KependudukanDataResponse> {

                    override fun onResponse(
                        call: retrofit2.Call<KependudukanDataResponse>,
                        response: retrofit2.Response<KependudukanDataResponse>
                    ) {
                        sembunyikanLoading()

                        if (!response.isSuccessful) {
                            tampilkanPesan(
                                "Gagal mengambil data Variabel (${response.code()})"
                            )

                            return
                        }

                        val body =
                            response.body()

                        if (
                            body == null ||
                            body.status?.uppercase() != "OK"
                        ) {
                            tampilkanPesan(
                                "Data Variabel tidak tersedia"
                            )

                            return
                        }

                        val dataContent =
                            body.dataContent

                        val vervarList =
                            body.vervar

                        if (
                            dataContent.isNullOrEmpty()
                        ) {
                            tampilkanPesanKosong(
                                "Data dari BPS belum tersedia untuk tahun $tahun.\n" +
                                        "Silakan pilih tahun lain."
                            )

                            return
                        }

                        if (
                            vervarList.isNullOrEmpty()
                        ) {
                            tampilkanPesan(
                                "Format wilayah tidak dikenali"
                            )

                            return
                        }

                        val unit =
                            body.variables
                                ?.firstOrNull()
                                ?.unit
                                ?: ""

                        variablePdfData.clear()

                        vervarList.forEach { wilayah ->
                            val namaWilayah =
                                wilayah.label
                                    ?: "Wilayah Tidak Diketahui"

                            val idWilayah =
                                wilayah.valId.toString()

                            val kecocokanKunci =
                                dataContent.entries.find {
                                    it.key.contains(
                                        idWilayah
                                    )
                                }

                            var nilai =
                                kecocokanKunci
                                    ?.value
                                    ?.toString()
                                    ?: "-"

                            if (
                                nilai != "-" &&
                                unit.isNotBlank()
                            ) {
                                nilai =
                                    "$nilai $unit"
                            }

                            variablePdfData.add(
                                PdfWilayahData(
                                    wilayah = namaWilayah,
                                    nilai = nilai
                                )
                            )

                            val rowData =
                                listOf(
                                    Pair(
                                        tvJudul.text.toString(),
                                        nilai
                                    )
                                )

                            tampilkanCardDataBaris(
                                namaWilayah,
                                rowData
                            )
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<KependudukanDataResponse>,
                        t: Throwable
                    ) {
                        sembunyikanLoading()

                        tampilkanPesan(
                            "Terjadi kesalahan koneksi"
                        )
                    }
                }
            )
    }

    private fun loadStaticTable() {
        repository
            .getKependudukanStaticTableDetail(
                domain = DOMAIN,
                id = variableId,
                apiKey = API_KEY
            )
            .enqueue(
                object :
                    retrofit2.Callback<KependudukanStaticDetailResponse> {

                    override fun onResponse(
                        call: retrofit2.Call<KependudukanStaticDetailResponse>,
                        response: retrofit2.Response<KependudukanStaticDetailResponse>
                    ) {
                        sembunyikanLoading()

                        if (!response.isSuccessful) {
                            tampilkanPesan(
                                "Gagal mengambil data (${response.code()})"
                            )

                            return
                        }

                        val body =
                            response.body()

                        if (body == null) {
                            tampilkanPesan(
                                "Data tidak tersedia"
                            )

                            return
                        }

                        if (
                            body.status?.uppercase() != "OK"
                        ) {
                            tampilkanPesan(
                                "Response API tidak valid"
                            )

                            return
                        }

                        if (
                            body.dataAvailability
                                ?.lowercase() != "available"
                        ) {
                            tampilkanPesan(
                                "Data statistik tidak tersedia"
                            )

                            return
                        }

                        val dataElement =
                            body.data

                        if (
                            dataElement == null ||
                            !dataElement.isJsonObject
                        ) {
                            tampilkanPesan(
                                "Format isi tabel tidak sesuai atau kosong"
                            )

                            return
                        }

                        val data =
                            com.google.gson.Gson().fromJson(
                                dataElement,
                                KependudukanStaticDetailData::class.java
                            )

                        if (
                            data == null ||
                            data.table.isNullOrBlank()
                        ) {
                            tampilkanPesan(
                                "Isi tabel tidak tersedia"
                            )

                            return
                        }

                        if (
                            !data.title.isNullOrBlank() &&
                            judul.isBlank()
                        ) {
                            tvJudul.text =
                                data.title
                        }

                        staticPdfTitle =
                            data.title
                                ?: tvJudul.text.toString()

                        tampilkanTableHtml(
                            data.table
                        )
                    }

                    override fun onFailure(
                        call: retrofit2.Call<KependudukanStaticDetailResponse>,
                        t: Throwable
                    ) {
                        sembunyikanLoading()

                        tampilkanPesan(
                            "Gagal mengambil data: " +
                                    "${t.message ?: "Unknown error"}"
                        )
                    }
                }
            )
    }

    private fun tampilkanTableHtml(
        html: String
    ) {
        cardContainer.removeAllViews()

        staticPdfRows.clear()

        val decodedHtml =
            html
                .replace(
                    "&lt;",
                    "<",
                    ignoreCase = true
                )
                .replace(
                    "&gt;",
                    ">",
                    ignoreCase = true
                )
                .replace(
                    "&quot;",
                    "\"",
                    ignoreCase = true
                )
                .replace(
                    "&#39;",
                    "'",
                    ignoreCase = true
                )
                .replace(
                    "&amp;",
                    "&",
                    ignoreCase = true
                )

        val document =
            Jsoup.parse(decodedHtml)

        val table =
            document
                .select("table")
                .firstOrNull()

        if (table == null) {
            tampilkanPesan(
                "Tabel tidak ditemukan"
            )

            return
        }

        val rows =
            table.select("tr")

        if (rows.isEmpty()) {
            tampilkanPesan(
                "Baris tabel tidak ditemukan"
            )

            return
        }

        var maxCols = 0

        for (row in rows) {
            var jumlahKolom = 0

            for (cell in row.select("th, td")) {
                val colspan =
                    cell.attr(
                        "colspan"
                    ).toIntOrNull()
                        ?: 1

                jumlahKolom += colspan
            }

            maxCols =
                maxOf(
                    maxCols,
                    jumlahKolom
                )
        }

        val grid =
            Array(rows.size) {
                Array(maxCols) {
                    ""
                }
            }

        for (r in rows.indices) {
            val cells =
                rows[r].select(
                    "th, td"
                )

            var c = 0

            for (cell in cells) {
                while (
                    c < maxCols &&
                    grid[r][c].isNotBlank()
                ) {
                    c++
                }

                if (c >= maxCols) {
                    break
                }

                val text =
                    cell
                        .text()
                        .trim()
                        .replace(
                            Regex("\\s+"),
                            " "
                        )

                val rowspan =
                    cell.attr(
                        "rowspan"
                    ).toIntOrNull()
                        ?: 1

                val colspan =
                    cell.attr(
                        "colspan"
                    ).toIntOrNull()
                        ?: 1

                for (
                rr in 0 until rowspan
                ) {
                    for (
                    cc in 0 until colspan
                    ) {
                        if (
                            r + rr < rows.size &&
                            c + cc < maxCols
                        ) {
                            grid[r + rr][c + cc] =
                                text
                        }
                    }
                }

                c += colspan
            }
        }

        var dataStartRow = -1
        var kolomWilayah = 0

        for (r in grid.indices) {
            var numericCount = 0
            var firstTextCol = -1

            for (c in 0 until maxCols) {
                val cell =
                    grid[r][c].trim()

                if (cell.isNotBlank()) {
                    val isYear =
                        cell.matches(
                            Regex(
                                "^(19|20)\\d{2}$"
                            )
                        )

                    val isNumber =
                        cell.matches(
                            Regex(
                                "^-?[0-9.,]+$"
                            )
                        ) &&
                                cell.any {
                                    it.isDigit()
                                } &&
                                !isYear

                    if (
                        isNumber ||
                        cell == "-"
                    ) {
                        numericCount++
                    } else if (
                        firstTextCol == -1 &&
                        !isYear
                    ) {
                        firstTextCol = c
                    }
                }
            }

            if (
                numericCount > 0 &&
                firstTextCol != -1
            ) {
                dataStartRow = r
                kolomWilayah = firstTextCol

                break
            }
        }

        if (dataStartRow == -1) {
            dataStartRow = 1
            kolomWilayah = 0
        }

        var headerStartRow = 0

        if (
            grid.isNotEmpty() &&
            maxCols > 1
        ) {
            val firstCell =
                grid[0][0]

            var isBigTitle = true

            for (c in 1 until maxCols) {
                if (
                    grid[0][c] != firstCell
                ) {
                    isBigTitle = false
                    break
                }
            }

            if (isBigTitle) {
                headerStartRow = 1
            }
        }

        val headers =
            Array(maxCols) {
                ""
            }

        for (c in 0 until maxCols) {
            val parts =
                mutableListOf<String>()

            for (
            r in headerStartRow until dataStartRow
            ) {
                val cell =
                    grid[r][c].trim()

                if (
                    cell.isNotBlank() &&
                    !parts.contains(cell)
                ) {
                    if (
                        !cell.equals(
                            "Kecamatan",
                            ignoreCase = true
                        ) &&
                        !cell.equals(
                            "Kabupaten/Kota",
                            ignoreCase = true
                        )
                    ) {
                        parts.add(cell)
                    }
                }
            }

            headers[c] =
                parts.joinToString(" ")

            if (
                headers[c].isBlank()
            ) {
                headers[c] =
                    "Data ${c + 1}"
            }
        }

        for (
        r in dataStartRow until grid.size
        ) {
            val wilayah =
                grid[r][kolomWilayah]
                    .trim()

            if (
                wilayah.isBlank() ||
                wilayah.lowercase()
                    .startsWith("catatan") ||
                wilayah.lowercase()
                    .startsWith("sumber")
            ) {
                continue
            }

            val rowData =
                mutableListOf<Pair<String, String>>()

            for (
            c in 0 until maxCols
            ) {
                if (
                    c == kolomWilayah
                ) {
                    continue
                }

                val headerName =
                    headers[c]

                val nilai =
                    grid[r][c]
                        .trim()
                        .ifBlank {
                            "-"
                        }

                rowData.add(
                    Pair(
                        headerName,
                        nilai
                    )
                )
            }

            if (
                rowData.isNotEmpty()
            ) {
                staticPdfRows.add(
                    PdfStaticRow(
                        wilayah = wilayah,
                        data = rowData
                    )
                )

                tampilkanCardDataBaris(
                    wilayah,
                    rowData
                )
            }
        }
    }

    private fun tampilkanCardDataBaris(
        wilayah: String,
        dataBaris: List<Pair<String, String>>
    ) {
        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.card_statistik,
                    cardContainer,
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
            wilayah

        for (
        (label, nilai) in dataBaris
        ) {
            tambahBarisData(
                containerVariable,
                label,
                nilai
            )
        }

        cardContainer.addView(
            card
        )
    }

    private fun tambahBarisData(
        container: LinearLayout,
        label: String,
        nilai: String
    ) {
        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                setPadding(
                    32,
                    3,
                    0,
                    3
                )
            }

        val tvLabel =
            TextView(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                text =
                    label.ifBlank {
                        "-"
                    }

                textSize =
                    14f

                setTextColor(
                    Color.parseColor(
                        "#4B5563"
                    )
                )
            }

        val tvData =
            TextView(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(
                            16,
                            0,
                            16,
                            0
                        )
                    }

                text =
                    nilai.ifBlank {
                        "-"
                    }

                textSize =
                    14f

                gravity =
                    Gravity.END

                maxLines = 1

                isSingleLine = true

                ellipsize = null

                setTextColor(
                    Color.parseColor(
                        "#111827"
                    )
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        row.addView(
            tvLabel
        )

        row.addView(
            tvData
        )

        container.addView(
            row
        )
    }

    private fun tampilkanPesanKosong(
        pesan: String
    ) {
        val tvKosong =
            TextView(
                this@KependudukanDetailActivity
            ).apply {
                text = pesan

                textSize = 15f

                setTextColor(
                    Color.GRAY
                )

                setPadding(
                    32,
                    64,
                    32,
                    32
                )

                gravity =
                    Gravity.CENTER
            }

        cardContainer.addView(
            tvKosong
        )
    }

    private fun tampilkanFilterTahun() {
        val tahunList =
            daftarTahun.keys
                .sortedDescending()

        if (tahunList.isEmpty()) {
            return
        }

        val labels =
            tahunList
                .map {
                    it.toString()
                }
                .toTypedArray()

        var posisiTerpilih =
            tahunList.indexOf(tahun)

        if (posisiTerpilih < 0) {
            posisiTerpilih = 0
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Pilih Tahun"
            )
            .setSingleChoiceItems(
                labels,
                posisiTerpilih
            ) { dialog, which ->
                val tahunDipilih =
                    tahunList[which]

                if (
                    tahunDipilih == tahun
                ) {
                    dialog.dismiss()

                    return@setSingleChoiceItems
                }

                tahun =
                    tahunDipilih

                tvTahun.text =
                    tahun.toString()

                dialog.dismiss()

                loadDetail()
            }
            .show()
    }

    private fun tampilkanPesan(
        pesan: String
    ) {
        Toast.makeText(
            this,
            pesan,
            Toast.LENGTH_LONG
        ).show()
    }

    private data class PdfWilayahData(
        val wilayah: String,
        val nilai: String
    )

    private data class PdfStaticRow(
        val wilayah: String,
        val data: List<Pair<String, String>>
    )

    override fun onDestroy() {
        if (
            ::progressLoading.isInitialized
        ) {
            progressLoading.cancelAnimation()
        }

        super.onDestroy()
    }
}