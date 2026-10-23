
package com.example.bpskota.uipegawai

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.LaporanDokumenUpload
import com.example.bpskota.bpskp.model.PenugasanResponse
import com.example.bpskota.bpskp.model.SuratMasuk
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import java.util.Locale

class KegiatanDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_PENUGASAN = "extra_id_penugasan"

        private const val MAX_DOKUMEN = 5
        private const val MAX_UKURAN_FILE = 5L * 1024L * 1024L
    }

    private val repository = BpskpRepository()
    private lateinit var session: BpskpAuthSession

    private lateinit var tvPerihal: TextView
    private lateinit var tvNomorSurat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvPemberiTugas: TextView
    private lateinit var tvCatatan: TextView
    private lateinit var tvTanggalSurat: TextView
    private lateinit var tvTanggalAcara: TextView
    private lateinit var tvWaktuAcara: TextView
    private lateinit var tvLokasi: TextView
    private lateinit var tvIsiSurat: TextView

    private var penugasan: PenugasanResponse? = null
    private var surat: SuratMasuk? = null
    private var idPenugasanAktif: Long = -1L

    private var dialogLaporan: AlertDialog? = null
    private var sedangMengirimLaporan = false

    private data class DokumenForm(
        val uri: Uri,
        val namaFile: String,
        val ukuranBytes: Long?,
        var jenisDokumen: String = "dokumentasi"
    )

    private var callbackPilihDokumen: ((List<Uri>) -> Unit)? = null

    private val pemilihDokumen =
        registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { daftarUri ->
            callbackPilihDokumen?.invoke(daftarUri)
            callbackPilihDokumen = null
        }

    private val jenisDokumenValues = listOf(
        "materi",
        "dokumentasi",
        "notulen",
        "lainnya"
    )

    private val jenisDokumenLabels = listOf(
        "Materi",
        "Dokumentasi",
        "Notulen",
        "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kegiatan_detail)

        session = BpskpAuthSession(this)

        inisialisasiView()
        setupTombol()

        idPenugasanAktif = intent.getLongExtra(
            EXTRA_ID_PENUGASAN,
            -1L
        )

        if (idPenugasanAktif <= 0L) {
            Toast.makeText(
                this,
                "ID penugasan tidak valid.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        muatDetailPenugasan(idPenugasanAktif)
    }

    private fun inisialisasiView() {
        tvPerihal = findViewById(R.id.tvDetailPerihal)
        tvNomorSurat = findViewById(R.id.tvDetailNomorSurat)
        tvStatus = findViewById(R.id.tvDetailStatus)
        tvPemberiTugas = findViewById(R.id.tvDetailPemberiTugas)
        tvCatatan = findViewById(R.id.tvDetailCatatan)
        tvTanggalSurat = findViewById(R.id.tvDetailTanggalSurat)
        tvTanggalAcara = findViewById(R.id.tvDetailTanggalAcara)
        tvWaktuAcara = findViewById(R.id.tvDetailWaktuAcara)
        tvLokasi = findViewById(R.id.tvDetailLokasi)
        tvIsiSurat = findViewById(R.id.tvDetailIsiSurat)
    }

    private fun setupTombol() {
        findViewById<View>(R.id.btnBackKegiatanDetail)
            .setOnClickListener {
                finish()
                overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
            }

        findViewById<View>(R.id.btnLihatSurat)
            .setOnClickListener {
                bukaFileSurat()
            }

        findViewById<View>(R.id.btnIsiLaporanKegiatan)
            .setOnClickListener {
                bukaFormLaporan()
            }
    }

    // =========================================================
    // DETAIL PENUGASAN DAN SURAT
    // =========================================================

    private fun muatDetailPenugasan(idPenugasan: Long) {
        repository.getDetailPenugasan(session, idPenugasan) {
                response, error ->

            if (isFinishing || isDestroyed) {
                return@getDetailPenugasan
            }

            val dataPenugasan = response?.data

            if (dataPenugasan == null) {
                Toast.makeText(
                    this,
                    error ?: "Gagal mengambil detail penugasan.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@getDetailPenugasan
            }

            penugasan = dataPenugasan
            tampilkanInfoPenugasan(dataPenugasan)

            val idSurat = dataPenugasan.suratMasukId

            if (idSurat == null || idSurat <= 0L) {
                Toast.makeText(
                    this,
                    "ID surat tidak tersedia pada data penugasan.",
                    Toast.LENGTH_LONG
                ).show()
                return@getDetailPenugasan
            }

            if (idSurat > Int.MAX_VALUE) {
                Toast.makeText(
                    this,
                    "ID surat tidak valid.",
                    Toast.LENGTH_LONG
                ).show()
                return@getDetailPenugasan
            }

            muatDetailSurat(idSurat.toInt())
        }
    }

    private fun tampilkanInfoPenugasan(item: PenugasanResponse) {
        tvPerihal.text = item.perihal
            ?.takeIf { it.isNotBlank() }
            ?: "Detail kegiatan"

        tvNomorSurat.text =
            "Nomor surat: ${item.nomorSurat ?: "-"}"

        tvStatus.text = formatStatus(item.status)

        tvPemberiTugas.text =
            "Pemberi tugas: ${item.namaPengirim ?: "-"}"

        tvCatatan.text = item.catatanPenugasan
            ?.takeIf { it.isNotBlank() }
            ?: "Tidak ada catatan penugasan."

        tvTanggalSurat.text = "Memuat tanggal surat..."
        tvTanggalAcara.text = "Memuat tanggal kegiatan..."
        tvWaktuAcara.text = "Memuat waktu kegiatan..."
        tvLokasi.text = "Memuat lokasi kegiatan..."
        tvIsiSurat.text = "Memuat isi surat..."
    }

    private fun muatDetailSurat(idSurat: Int) {
        val token = session.getToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Sesi login tidak ditemukan. Silakan login kembali.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        repository.getSuratMasukDetail(
            authorization = "Bearer $token",
            id = idSurat
        ) { dataSurat, error ->

            if (isFinishing || isDestroyed) {
                return@getSuratMasukDetail
            }

            if (dataSurat == null) {
                Toast.makeText(
                    this,
                    error ?: "Gagal mengambil detail surat.",
                    Toast.LENGTH_LONG
                ).show()
                return@getSuratMasukDetail
            }

            surat = dataSurat
            tampilkanDetailSurat(dataSurat)
        }
    }

    private fun tampilkanDetailSurat(item: SuratMasuk) {
        tvPerihal.text = item.perihal
            ?.takeIf { it.isNotBlank() }
            ?: penugasan?.perihal
                    ?: "Detail kegiatan"

        tvNomorSurat.text =
            "Nomor surat: ${item.nomor_surat ?: penugasan?.nomorSurat ?: "-"}"

        tvTanggalSurat.text =
            item.tanggal_surat ?: "-"

        tvTanggalAcara.text =
            item.tanggal_acara ?: "Tidak ada tanggal kegiatan"

        val mulai = item.waktu_mulai.orEmpty()
        val selesai = item.waktu_selesai.orEmpty()

        tvWaktuAcara.text = when {
            mulai.isNotBlank() && selesai.isNotBlank() ->
                "$mulai - $selesai"

            mulai.isNotBlank() -> mulai
            selesai.isNotBlank() -> selesai
            else -> "Waktu belum ditentukan"
        }

        tvLokasi.text = item.lokasi
            ?.takeIf { it.isNotBlank() }
            ?: "Lokasi belum ditentukan"

        tvIsiSurat.text = item.isi_ringkas
            ?.takeIf { it.isNotBlank() }
            ?: "Tidak ada isi ringkas surat."

        tvPemberiTugas.text =
            "Pemberi tugas: ${penugasan?.namaPengirim ?: "-"}"
    }

    private fun bukaFileSurat() {
        val item = surat

        if (item == null) {
            Toast.makeText(
                this,
                "Detail surat belum selesai dimuat.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val url = item.file_url
            ?.takeIf { it.isNotBlank() }
            ?: item.file_surat?.takeIf { it.isNotBlank() }

        if (url.isNullOrBlank()) {
            Toast.makeText(
                this,
                "File surat tidak tersedia.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uri = Uri.parse(url)

        if (uri.scheme != "https" && uri.scheme != "http") {
            Toast.makeText(
                this,
                "URL file surat belum lengkap dari API.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Tidak ada aplikasi untuk membuka file surat.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // FORM LAPORAN KEGIATAN
    // =========================================================

    private fun bukaFormLaporan() {
        val item = penugasan

        if (item == null) {
            Toast.makeText(
                this,
                "Data penugasan belum tersedia.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        when (item.status?.lowercase(Locale.ROOT)) {
            "ditolak" -> {
                Toast.makeText(
                    this,
                    "Penugasan ditolak. Laporan tidak dapat dibuat.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            "selesai" -> {
                Toast.makeText(
                    this,
                    "Penugasan sudah selesai.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            "diterima" -> Unit

            else -> {
                Toast.makeText(
                    this,
                    "Terima penugasan terlebih dahulu sebelum membuat laporan.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        if (sedangMengirimLaporan) return

        tampilkanDialogLaporan()
    }

    private fun tampilkanDialogLaporan() {
        val dokumenTerpilih = mutableListOf<DokumenForm>()

        val konten = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(
                konten,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        konten.addView(
            buatTeks(
                "Laporan Kegiatan",
                22f,
                Typeface.BOLD,
                Color.rgb(35, 35, 35)
            ),
            parameterLebar()
        )

        tambahJarak(konten, 6)

        konten.addView(
            buatTeks(
                "Lengkapi laporan dan lampirkan 1–5 dokumen pendukung.",
                13f,
                Typeface.NORMAL,
                Color.rgb(105, 105, 105)
            ),
            parameterLebar()
        )

        tambahJarak(konten, 18)

        konten.addView(
            buatLabel("Ringkasan kegiatan *"),
            parameterLebar()
        )

        val etRingkasan = buatEditText(
            "Tuliskan ringkasan kegiatan...",
            multiline = true
        )
        etRingkasan.minLines = 3
        etRingkasan.maxLines = 5

        konten.addView(etRingkasan, parameterLebar())
        tambahJarak(konten, 14)

        konten.addView(
            buatLabel("Hasil kegiatan *"),
            parameterLebar()
        )

        val etHasil = buatEditText(
            "Tuliskan hasil yang dicapai...",
            multiline = true
        )
        etHasil.minLines = 3
        etHasil.maxLines = 6

        konten.addView(etHasil, parameterLebar())
        tambahJarak(konten, 14)

        konten.addView(
            buatLabel("Catatan tambahan (opsional)"),
            parameterLebar()
        )

        val etCatatan = buatEditText(
            "Tambahkan catatan bila diperlukan...",
            multiline = true
        )
        etCatatan.minLines = 2
        etCatatan.maxLines = 4

        konten.addView(etCatatan, parameterLebar())
        tambahJarak(konten, 20)

        konten.addView(
            buatTeks(
                "Dokumen Pendukung *",
                16f,
                Typeface.BOLD,
                Color.rgb(35, 35, 35)
            ),
            parameterLebar()
        )

        tambahJarak(konten, 4)

        konten.addView(
            buatTeks(
                "Format: JPG, JPEG, PNG, PDF, DOC, DOCX. Maksimal 5 MB per file.",
                12f,
                Typeface.NORMAL,
                Color.rgb(105, 105, 105)
            ),
            parameterLebar()
        )

        tambahJarak(konten, 10)

        val btnPilihDokumen = Button(this).apply {
            text = "＋ Pilih Dokumen"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(249, 115, 22))
        }

        konten.addView(
            btnPilihDokumen,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        tambahJarak(konten, 10)

        val tvJumlahDokumen = buatTeks(
            "Belum ada dokumen dipilih (0/$MAX_DOKUMEN)",
            12f,
            Typeface.NORMAL,
            Color.rgb(100, 100, 100)
        )

        konten.addView(tvJumlahDokumen, parameterLebar())

        tambahJarak(konten, 8)

        val kontainerDokumen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        konten.addView(kontainerDokumen, parameterLebar())

        fun renderDokumen() {
            kontainerDokumen.removeAllViews()

            tvJumlahDokumen.text =
                "Dokumen dipilih (${dokumenTerpilih.size}/$MAX_DOKUMEN)"

            dokumenTerpilih.forEachIndexed { index, dokumen ->
                val kartu = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    setBackgroundColor(Color.rgb(246, 242, 238))
                }

                val barisNama = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val infoNama = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val namaTeks = buatTeks(
                    dokumen.namaFile,
                    13f,
                    Typeface.BOLD,
                    Color.rgb(45, 45, 45)
                )

                val ukuranTeks = buatTeks(
                    dokumen.ukuranBytes?.let { formatUkuran(it) }
                        ?: "Ukuran akan diperiksa saat upload",
                    11f,
                    Typeface.NORMAL,
                    Color.rgb(100, 100, 100)
                )

                infoNama.addView(namaTeks, parameterLebar())
                infoNama.addView(ukuranTeks, parameterLebar())

                barisNama.addView(
                    infoNama,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )

                val btnHapus = TextView(this).apply {
                    text = "Hapus"
                    textSize = 12f
                    setTextColor(Color.rgb(200, 55, 45))
                    setPadding(dp(10), dp(8), dp(4), dp(8))
                    setOnClickListener {
                        dokumenTerpilih.removeAt(index)
                        renderDokumen()
                    }
                }

                barisNama.addView(btnHapus)
                kartu.addView(barisNama, parameterLebar())
                tambahJarak(kartu, 8)

                kartu.addView(
                    buatLabel("Jenis dokumen"),
                    parameterLebar()
                )

                val spinnerJenis = Spinner(this).apply {
                    adapter = ArrayAdapter(
                        this@KegiatanDetailActivity,
                        android.R.layout.simple_spinner_item,
                        jenisDokumenLabels
                    ).also {
                        it.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                    }

                    setSelection(
                        jenisDokumenValues.indexOf(dokumen.jenisDokumen)
                            .coerceAtLeast(0)
                    )

                    onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                dokumenTerpilih
                                    .getOrNull(index)
                                    ?.jenisDokumen = jenisDokumenValues[position]
                            }

                            override fun onNothingSelected(
                                parent: android.widget.AdapterView<*>?
                            ) = Unit
                        }
                }

                kartu.addView(spinnerJenis, parameterLebar())

                kontainerDokumen.addView(
                    kartu,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                )
            }

            btnPilihDokumen.isEnabled =
                dokumenTerpilih.size < MAX_DOKUMEN

            btnPilihDokumen.alpha =
                if (btnPilihDokumen.isEnabled) 1f else 0.5f
        }

        btnPilihDokumen.setOnClickListener {
            if (dokumenTerpilih.size >= MAX_DOKUMEN) {
                Toast.makeText(
                    this,
                    "Maksimal $MAX_DOKUMEN dokumen.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            callbackPilihDokumen = { daftarUri ->
                daftarUri.forEach { uri ->
                    if (dokumenTerpilih.size >= MAX_DOKUMEN) {
                        Toast.makeText(
                            this,
                            "Maksimal $MAX_DOKUMEN dokumen. File sisanya tidak ditambahkan.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@forEach
                    }

                    if (dokumenTerpilih.any { it.uri == uri }) {
                        return@forEach
                    }

                    val namaFile = ambilNamaFile(uri)
                    val ekstensi = namaFile
                        .substringAfterLast('.', "")
                        .lowercase(Locale.ROOT)

                    val ekstensiDiizinkan = setOf(
                        "jpg", "jpeg", "png",
                        "pdf", "doc", "docx"
                    )

                    if (ekstensi !in ekstensiDiizinkan) {
                        Toast.makeText(
                            this,
                            "Format file tidak didukung: $namaFile",
                            Toast.LENGTH_LONG
                        ).show()
                        return@forEach
                    }

                    val ukuran = ambilUkuranFile(uri)

                    if (ukuran != null && ukuran > MAX_UKURAN_FILE) {
                        Toast.makeText(
                            this,
                            "$namaFile melebihi batas 5 MB.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@forEach
                    }

                    dokumenTerpilih.add(
                        DokumenForm(
                            uri = uri,
                            namaFile = namaFile,
                            ukuranBytes = ukuran
                        )
                    )
                }

                renderDokumen()
            }

            pemilihDokumen.launch(
                arrayOf(
                    "image/jpeg",
                    "image/png",
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setView(scrollView)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Kirim Laporan", null)
            .create()

        dialogLaporan = dialog

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94f).toInt(),
                (resources.displayMetrics.heightPixels * 0.82f).toInt()
            )

            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val ringkasan = etRingkasan.text.toString().trim()
                    val hasil = etHasil.text.toString().trim()
                    val catatan = etCatatan.text.toString().trim()

                    if (ringkasan.isBlank()) {
                        etRingkasan.error = "Ringkasan wajib diisi."
                        etRingkasan.requestFocus()
                        return@setOnClickListener
                    }

                    if (hasil.isBlank()) {
                        etHasil.error = "Hasil kegiatan wajib diisi."
                        etHasil.requestFocus()
                        return@setOnClickListener
                    }

                    if (dokumenTerpilih.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Pilih minimal satu dokumen pendukung.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val daftarUpload = dokumenTerpilih.map {
                        LaporanDokumenUpload(
                            uri = it.uri,
                            jenisDokumen = it.jenisDokumen,
                            namaFile = it.namaFile
                        )
                    }

                    kirimLaporan(
                        dialog = dialog,
                        ringkasan = ringkasan,
                        hasilKegiatan = hasil,
                        catatan = catatan.ifBlank { null },
                        dokumen = daftarUpload
                    )
                }
        }

        dialog.setOnDismissListener {
            if (dialogLaporan === dialog) {
                dialogLaporan = null
            }
            callbackPilihDokumen = null
        }

        dialog.show()
    }

    private fun kirimLaporan(
        dialog: AlertDialog,
        ringkasan: String,
        hasilKegiatan: String,
        catatan: String?,
        dokumen: List<LaporanDokumenUpload>
    ) {
        if (sedangMengirimLaporan) return

        if (idPenugasanAktif <= 0L || idPenugasanAktif > Int.MAX_VALUE) {
            Toast.makeText(
                this,
                "ID penugasan tidak valid untuk laporan.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        sedangMengirimLaporan = true

        val tombolKirim = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val tombolBatal = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        tombolKirim.isEnabled = false
        tombolKirim.text = "Mengirim..."
        tombolBatal.isEnabled = false

        repository.submitLaporan(
            context = this,
            session = session,
            penugasanId = idPenugasanAktif.toInt(),
            ringkasan = ringkasan,
            hasilKegiatan = hasilKegiatan,
            catatan = catatan,
            dokumen = dokumen
        ) { response, error ->

            if (isFinishing || isDestroyed) return@submitLaporan

            sedangMengirimLaporan = false

            if (response?.success == true) {
                dialog.dismiss()

                Toast.makeText(
                    this,
                    response.message
                        ?: "Laporan kegiatan berhasil dikirim.",
                    Toast.LENGTH_LONG
                ).show()

                // Laravel memperbarui status penugasan menjadi selesai.
                // Muat ulang detail agar status terbaru tampil di layar.
                muatDetailPenugasan(idPenugasanAktif)
            } else {
                tombolKirim.isEnabled = true
                tombolKirim.text = "Kirim Laporan"
                tombolBatal.isEnabled = true

                Toast.makeText(
                    this,
                    error ?: response?.message
                    ?: "Gagal mengirim laporan kegiatan.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // UTILITAS FORM
    // =========================================================

    private fun ambilNamaFile(uri: Uri): String {
        var nama: String? = null

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    nama = cursor.getString(index)
                }
            }
        }

        return nama
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.contains('.') }
            ?: "dokumen_${System.currentTimeMillis()}.pdf"
    }

    private fun ambilUkuranFile(uri: Uri): Long? {
        return try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)

                    if (index >= 0 && !cursor.isNull(index)) {
                        cursor.getLong(index)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatUkuran(bytes: Long): String {
        return if (bytes < 1024L * 1024L) {
            String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
        } else {
            String.format(
                Locale.getDefault(),
                "%.2f MB",
                bytes / (1024.0 * 1024.0)
            )
        }
    }

    private fun buatTeks(
        teks: String,
        ukuran: Float,
        gaya: Int,
        warna: Int
    ): TextView {
        return TextView(this).apply {
            text = teks
            textSize = ukuran
            typeface = Typeface.create(Typeface.DEFAULT, gaya)
            setTextColor(warna)
        }
    }

    private fun buatLabel(teks: String): TextView {
        return buatTeks(
            teks,
            14f,
            Typeface.BOLD,
            Color.rgb(55, 55, 55)
        ).apply {
            setPadding(0, dp(2), 0, dp(6))
        }
    }

    private fun buatEditText(
        hint: String,
        multiline: Boolean
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 14f
            setTextColor(Color.rgb(40, 40, 40))
            setHintTextColor(Color.rgb(145, 145, 145))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(250, 250, 250))
            inputType = if (multiline) {
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            gravity = android.view.Gravity.TOP
            isSingleLine = false
        }
    }

    private fun parameterLebar(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun tambahJarak(parent: LinearLayout, dp: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                this.dp(dp)
            )
        )
    }

    private fun dp(nilai: Int): Int {
        return (nilai * resources.displayMetrics.density).toInt()
    }

    private fun formatStatus(status: String?): String {
        return when (status?.lowercase(Locale.ROOT)) {
            "menunggu_respon" -> "Menunggu Respons"
            "diterima" -> "Diterima"
            "ditolak" -> "Ditolak"
            "selesai" -> "Selesai"
            else -> status
                ?.replace("_", " ")
                ?.replaceFirstChar { it.uppercase() }
                ?: "Tidak diketahui"
        }
    }
}