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
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.bps.model.EkonomiDetailResponse
import com.example.bpskota.bps.model.EkonomiItem
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class EkonomiDetailActivity : AppCompatActivity() {

    private lateinit var activityTracker: ActivityTracker

    companion object {


        const val EXTRA_ID = "ekonomi_id"
        const val EXTRA_JUDUL = "ekonomi_judul"
        const val EXTRA_TAHUN = "ekonomi_tahun"


        private const val CHANNEL_ID = "bps_download_channel"
        private const val NOTIFICATION_ID = 2003
        private const val REQUEST_NOTIFICATION_PERMISSION = 1003
    }


    private val repository =
        BpsRepository()


    private val domain =
        "3574"

    private val apiKey =
        "008edaaae5d450b1913b31a2cef618c3"


    private lateinit var tvDetailJudul: TextView

    private lateinit var tvDetailTahun: TextView

    private lateinit var tvDetailData: LinearLayout

    private lateinit var progressLoading: LottieAnimationView


    private var tahunTerpilih =
        2025


    private val pdfData =
        mutableListOf<PdfEkonomiData>()

    private var pdfUnit =
        ""

    private var pdfSectionTitle =
        ""


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_ekonomi_detail
        )

        initView()

        setupButton()

        createNotificationChannel()

        requestNotificationPermission()


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

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "EkonomiDetail",
            metadata = mapOf(
                "table_id" to (id ?: ""),
                "tahun" to tahunTerpilih,
                "judul" to (judul ?: "")
            )
        )








        tvDetailJudul.text =
            judul ?: "Tanpa judul"

        tvDetailTahun.text =
            tahunTerpilih.toString()


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


        loadDetail(
            id = id,
            tahunDipilih = tahunTerpilih
        )
    }


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


        progressLoading.setAnimation(
            "Loading_Animation.json"
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


    private fun downloadPdf() {


        if (
            pdfData.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Data belum tersedia untuk diunduh",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        try {

            downloadPdfEkonomi()

        } catch (
            e: Exception
        ) {


            Toast.makeText(
                this,
                "Gagal membuat PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun downloadPdfEkonomi() {

        val document =
            PdfDocument()

        var pageNumber =
            1

        var page =
            createPdfPage(
                document,
                pageNumber
            )

        var canvas =
            page.canvas

        val paint =
            Paint().apply {

                isAntiAlias = true
                color = Color.BLACK
            }

        var y =
            45f


        paint.textSize =
            16f

        paint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        canvas.drawText(
            "DATA STATISTIK BPS KOTA PROBOLINGGO",
            40f,
            y,
            paint
        )

        y += 30f


        paint.textSize =
            14f

        canvas.drawText(
            bersihkanPdfText(
                tvDetailJudul.text.toString()
            ),
            40f,
            y,
            paint
        )

        y += 25f


        paint.textSize =
            11f

        paint.typeface =
            Typeface.DEFAULT

        canvas.drawText(
            "Tahun: ${tvDetailTahun.text}",
            40f,
            y,
            paint
        )

        y += 18f

        canvas.drawText(
            "Wilayah: Kota Probolinggo",
            40f,
            y,
            paint
        )

        y += 18f

        canvas.drawText(
            "Domain BPS: $domain",
            40f,
            y,
            paint
        )

        y += 18f

        if (
            pdfUnit.isNotBlank()
        ) {

            canvas.drawText(
                "Satuan: $pdfUnit",
                40f,
                y,
                paint
            )

            y += 18f
        }

        y += 12f


        if (
            pdfSectionTitle.isNotBlank()
        ) {

            paint.textSize =
                12f

            paint.typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

            canvas.drawText(
                bersihkanPdfText(
                    pdfSectionTitle
                ),
                40f,
                y,
                paint
            )

            y += 25f
        }


        paint.textSize =
            10f

        for (
        item in pdfData
        ) {


            if (
                y > 770f
            ) {

                document.finishPage(
                    page
                )

                pageNumber++

                page =
                    createPdfPage(
                        document,
                        pageNumber
                    )

                canvas =
                    page.canvas

                y =
                    45f

                paint.textSize =
                    10f
            }


            paint.typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

            canvas.drawText(
                bersihkanPdfText(
                    item.label
                ),
                40f,
                y,
                paint
            )

            y += 20f


            if (
                item.unit.isNotBlank()
            ) {

                paint.typeface =
                    Typeface.DEFAULT

                canvas.drawText(
                    "Satuan: ${item.unit}",
                    55f,
                    y,
                    paint
                )

                y += 18f
            }


            y =
                drawPdfRow(
                    canvas,
                    paint,
                    "Triwulan I",
                    item.q1,
                    y
                )


            y =
                drawPdfRow(
                    canvas,
                    paint,
                    "Triwulan II",
                    item.q2,
                    y
                )


            y =
                drawPdfRow(
                    canvas,
                    paint,
                    "Triwulan III",
                    item.q3,
                    y
                )


            y =
                drawPdfRow(
                    canvas,
                    paint,
                    "Triwulan IV",
                    item.q4,
                    y
                )


            y =
                drawPdfRow(
                    canvas,
                    paint,
                    "Tahunan",
                    item.tahunan,
                    y
                )

            y += 15f


            paint.strokeWidth =
                0.7f

            canvas.drawLine(
                40f,
                y,
                555f,
                y,
                paint
            )

            y += 18f
        }


        if (
            y > 800f
        ) {

            document.finishPage(
                page
            )

            pageNumber++

            page =
                createPdfPage(
                    document,
                    pageNumber
                )

            canvas =
                page.canvas

            y =
                45f
        }

        paint.textSize =
            9f

        paint.typeface =
            Typeface.DEFAULT

        canvas.drawText(
            "Sumber: Badan Pusat Statistik",
            40f,
            810f,
            paint
        )

        canvas.drawText(
            "BPS Kota Probolinggo",
            40f,
            825f,
            paint
        )


        document.finishPage(
            page
        )


        val judulFile =
            sanitasiNamaFile(
                tvDetailJudul.text.toString()
            )

        val namaFile =
            "Ekonomi_${judulFile}_${tvDetailTahun.text}.pdf"


        val uri =
            savePdfToDownloads(
                document,
                namaFile
            )

        document.close()

        if (
            uri == null
        ) {

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
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun drawPdfRow(
        canvas: android.graphics.Canvas,
        paint: Paint,
        label: String,
        value: String,
        y: Float
    ): Float {

        paint.typeface =
            Typeface.DEFAULT

        paint.textSize =
            10f

        canvas.drawText(
            label,
            65f,
            y,
            paint
        )

        paint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        canvas.drawText(
            if (
                value.isBlank()
            ) {
                "-"
            } else {
                value
            },
            400f,
            y,
            paint
        )

        return y + 18f
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

        return try {

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

                if (
                    uri == null
                ) {

                    return null
                }

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

                uri

            } else {

                val downloadDirectory =
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                val bpsDirectory =
                    File(
                        downloadDirectory,
                        "BPS Kota Probolinggo"
                    )

                if (
                    !bpsDirectory.exists()
                ) {

                    bpsDirectory.mkdirs()
                }

                val file =
                    File(
                        bpsDirectory,
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
                        "Notifikasi hasil download data BPS"
                }

            val notificationManager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.createNotificationChannel(
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


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
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
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "File tersimpan di Downloads/BPS Kota Probolinggo"
                        )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(
                    true
                )
                .build()

        val notificationManager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.notify(
            NOTIFICATION_ID,
            notification
        )
    }


    private fun loadDetail(
        id: String,
        tahunDipilih: Int
    ) {


        pdfData.clear()

        pdfUnit = ""

        pdfSectionTitle = ""


        progressLoading.visibility =
            View.VISIBLE

        progressLoading.playAnimation()

        tvDetailData.removeAllViews()




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


                        progressLoading.cancelAnimation()

                        progressLoading.visibility =
                            View.GONE



                        if (
                            !response.isSuccessful
                        ) {


                            Toast.makeText(
                                this@EkonomiDetailActivity,
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
                                this@EkonomiDetailActivity,
                                "Response kosong",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }












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


                        if (
                            body.variables.isNullOrEmpty()
                        ) {

                            tampilkanPesan(
                                "Variabel statistik tidak tersedia"
                            )

                            return
                        }


                        if (
                            body.vervar.isNullOrEmpty()
                        ) {

                            tampilkanPesan(
                                "Data wilayah tidak tersedia"
                            )

                            return
                        }


                        if (
                            body.tahun.isNullOrEmpty()
                        ) {

                            tampilkanPesan(
                                "Data tahun tidak tersedia"
                            )

                            return
                        }


                        if (
                            body.dataContent == null
                        ) {

                            tampilkanPesan(
                                "Data statistik tidak tersedia"
                            )

                            return
                        }


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


                        progressLoading.cancelAnimation()

                        progressLoading.visibility =
                            View.GONE


                        Toast.makeText(
                            this@EkonomiDetailActivity,
                            "Gagal terhubung ke server BPS: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }


    private fun tampilkanDetail(
        response: EkonomiDetailResponse,
        tahunDipilih: Int
    ) {

        tvDetailData.removeAllViews()

        val variable =
            response.variables
                ?.firstOrNull()


        val tahun =
            cariTahun(
                response.tahun,
                tahunDipilih
            )




        if (
            tahun == null
        ) {

            tampilkanPesan(
                "Data tahun $tahunDipilih tidak tersedia"
            )

            return
        }


        tvDetailTahun.text =
            tahun.label
                ?: tahunDipilih.toString()


        if (
            !variable?.unit.isNullOrBlank()
        ) {

            pdfUnit =
                variable?.unit ?: ""

            tvDetailData.addView(
                buatInfoUnit(
                    variable?.unit ?: ""
                )
            )
        }


        pdfSectionTitle =
            response.labelVervar
                ?: "Data"

        tvDetailData.addView(
            buatSectionTitle(
                response.labelVervar
                    ?: "Data"
            )
        )


        tampilkanData(
            body = response,
            tahun = tahun
        )
    }


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


        return daftarTahun.firstOrNull { item ->

            item.value?.toString() ==
                    target

        } ?: daftarTahun.firstOrNull { item ->

            item.valId?.toString() ==
                    target
        }
    }


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


        if (
            variable == null ||
            vervar.isNullOrEmpty() ||
            dataContent == null
        ) {

            tampilkanDataKosong()

            return
        }


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


        val tahunFormatted =
            tahunVal
                .toString()
                .padStart(
                    4,
                    '0'
                )









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


            val q1 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 31
                )


            val q2 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 32
                )


            val q3 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 33
                )


            val q4 =
                ambilNilaiTriwulan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turTahun = 34
                )


            val tahunan =
                ambilNilaiTahunan(
                    dataContent = dataContent,
                    vervarVal = vervarVal,
                    variableVal = variableVal,
                    tahunFormatted = tahunFormatted,
                    turtahun = body.turtahun
                )









            pdfData.add(
                PdfEkonomiData(
                    label = bersihkanHtml(label),
                    unit = variable.unit ?: "",
                    q1 = q1,
                    q2 = q2,
                    q3 = q3,
                    q4 = q4,
                    tahunan = tahunan
                )
            )


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


    private fun ambilNilaiTriwulan(
        dataContent: JsonObject,
        vervarVal: Int,
        variableVal: Int,
        tahunFormatted: String,
        turTahun: Int
    ): String {

        val key =
            "$vervarVal$variableVal$tahunFormatted$turTahun"


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


            "-"
        }
    }


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


        val kodeTahunan =
            itemTahunan?.value
                ?: itemTahunan?.valId
                ?: 30

        val key =
            "$vervarVal$variableVal$tahunFormatted$kodeTahunan"


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


            "-"
        }
    }


    private fun tambahGrupTriwulan(
        label: String,
        unit: String,
        q1: String,
        q2: String,
        q3: String,
        q4: String,
        tahunan: String
    ) {


        val card =
            layoutInflater.inflate(
                R.layout.card_statistik,
                tvDetailData,
                false
            ) as CardView


        val tvNamaWilayah =
            card.findViewById<TextView>(
                R.id.tvNamaWilayah
            )

        tvNamaWilayah.text =
            bersihkanHtml(
                label
            )


        val containerVariable =
            card.findViewById<LinearLayout>(
                R.id.containerVariable
            )


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
                dpToPx(8)
            )

            containerVariable.addView(
                tvUnit
            )
        }


        containerVariable.addView(
            buatBarisTriwulan(
                "Triwulan I",
                q1
            )
        )


        containerVariable.addView(
            buatBarisTriwulan(
                "Triwulan II",
                q2
            )
        )


        containerVariable.addView(
            buatBarisTriwulan(
                "Triwulan III",
                q3
            )
        )


        containerVariable.addView(
            buatBarisTriwulan(
                "Triwulan IV",
                q4
            )
        )


        containerVariable.addView(
            buatBarisTriwulan(
                "Tahunan",
                tahunan
            )
        )


        tvDetailData.addView(
            card
        )
    }


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


        row.addView(
            tvTriwulan
        )

        row.addView(
            tvNilai
        )

        return row
    }


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


    private fun bersihkanPdfText(
        text: String
    ): String {

        return text
            .replace(
                "\n",
                " "
            )
            .replace(
                "\r",
                " "
            )
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


    private fun sanitasiNamaFile(
        nama: String
    ): String {

        return nama
            .replace(
                Regex("[\\\\/:*?\"<>|]"),
                ""
            )
            .replace(
                Regex("\\s+"),
                "_"
            )
            .take(80)
            .ifBlank {
                "Data_Ekonomi"
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


    private data class PdfEkonomiData(

        val label: String,

        val unit: String,

        val q1: String,

        val q2: String,

        val q3: String,

        val q4: String,

        val tahunan: String
    )


    override fun onDestroy() {

        progressLoading.cancelAnimation()

        super.onDestroy()
    }
}
