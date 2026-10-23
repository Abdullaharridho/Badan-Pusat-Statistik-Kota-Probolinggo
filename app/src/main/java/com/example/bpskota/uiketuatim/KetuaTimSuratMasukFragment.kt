package com.example.bpskota.uiketuatim

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.CreatePenugasanRequest
import com.example.bpskota.bpskp.model.LaporanData
import com.example.bpskota.bpskp.model.PenugasanResponse
import com.example.bpskota.bpskp.model.RekomendasiData
import com.example.bpskota.bpskp.model.SuratMasuk
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository

class KetuaTimSuratMasukFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private lateinit var adapter: SuratMasukAdapter
    private lateinit var bpskpRepository: BpskpRepository
    private lateinit var authSession: BpskpAuthSession

    private var filterStatus: String? = null
    private var isLoading = false
    private val semuaSurat = mutableListOf<SuratMasuk>()
    private var tvJumlahSuratBaru: TextView? = null
    private var tvJumlahSuratDiproses: TextView? = null
    private var btnFilterSemua: TextView? = null
    private var btnFilterBaru: TextView? = null
    private var btnFilterDiproses: TextView? = null
    private var isSubmitting = false

    private data class PenerimaOption(val id: Int, val nama: String) {
        override fun toString(): String = nama
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ketua_tim_surat_masuk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bpskpRepository = BpskpRepository()
        authSession = BpskpAuthSession(requireContext())
        recyclerView = view.findViewById(R.id.rvSuratMasukKetuaTim)
        tvJumlahSuratBaru = view.findViewById(R.id.tvJumlahSuratBaru)
        tvJumlahSuratDiproses = view.findViewById(R.id.tvJumlahSuratDiproses)
        btnFilterSemua = view.findViewById(R.id.btnFilterSemua)
        btnFilterBaru = view.findViewById(R.id.btnFilterBaru)
        btnFilterDiproses = view.findViewById(R.id.btnFilterDiproses)

        setupFilterStatus()
        setupRecyclerView()
        loadSuratMasuk()
    }

    private fun setupRecyclerView() {
        val rv = recyclerView ?: return
        adapter = SuratMasukAdapter(
            onDetailClick = { surat -> showDetailSuratDenganPenugasan(surat) },
            onActionClick = { surat ->
                when (surat.status?.lowercase()) {
                    "baru" -> showDialogPenugasan(surat)
                    "diproses" -> showPenugasan(surat)
                    else -> showDetailSurat(surat)
                }
            },
            onReportClick = { laporan -> bukaDetailLaporan(laporan) }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
    }

    private fun getAuthorization(): String? {
        val token = authSession.getToken()?.trim().orEmpty()
        if (token.isBlank()) return null
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    private fun setupFilterStatus() {
        btnFilterSemua?.setOnClickListener { filterSuratMasuk(null) }
        btnFilterBaru?.setOnClickListener { filterSuratMasuk("baru") }
        btnFilterDiproses?.setOnClickListener { filterSuratMasuk("diproses") }
        perbaruiTampilanFilter()
    }

    private fun perbaruiTampilanFilter() {
        listOf(
            btnFilterSemua to null,
            btnFilterBaru to "baru",
            btnFilterDiproses to "diproses"
        ).forEach { (view, status) ->
            view ?: return@forEach
            val aktif = filterStatus == status
            view.setBackgroundColor(if (aktif) Color.rgb(249, 115, 22) else Color.WHITE)
            view.setTextColor(if (aktif) Color.WHITE else Color.rgb(85, 85, 85))
            view.setTypeface(null, if (aktif) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            view.isSelected = aktif
        }
    }

    private fun perbaruiDaftarTerfilter() {
        val daftar = when (filterStatus) {
            "baru" -> semuaSurat.filter { it.status.equals("baru", ignoreCase = true) }
            "diproses" -> semuaSurat.filter { it.status.equals("diproses", ignoreCase = true) }
            else -> semuaSurat.toList()
        }
        adapter.submitList(daftar)
        perbaruiTampilanFilter()
    }

    private fun perbaruiRingkasan() {
        tvJumlahSuratBaru?.text = semuaSurat.count { it.status.equals("baru", ignoreCase = true) }.toString()
        tvJumlahSuratDiproses?.text = semuaSurat.count { it.status.equals("diproses", ignoreCase = true) }.toString()
    }

    private fun loadSuratMasuk() {
        if (!isAdded || isLoading) return
        val authorization = getAuthorization()
        if (authorization == null) {
            showMessage("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        isLoading = true
        bpskpRepository.getSuratMasuk(authorization = authorization, search = null, status = null) { data, error ->
            if (!isAdded || recyclerView == null) {
                isLoading = false
                return@getSuratMasuk
            }
            isLoading = false
            if (error != null) {
                showMessage(error)
                return@getSuratMasuk
            }

            semuaSurat.clear()
            semuaSurat.addAll(data.orEmpty())
            perbaruiRingkasan()
            perbaruiDaftarTerfilter()
            muatLaporanUntukSurat()
        }
    }

    fun filterSuratMasuk(status: String?) {
        filterStatus = status?.lowercase()?.takeIf { it == "baru" || it == "diproses" }
        perbaruiDaftarTerfilter()
    }

    private fun muatLaporanUntukSurat() {
        if (!isAdded) return
        bpskpRepository.getPenugasan(session = authSession, search = null, status = null) { hasilPenugasan, errorPenugasan ->
            if (!isAdded || recyclerView == null) return@getPenugasan
            if (errorPenugasan != null) {
                adapter.setLaporanPerSurat(emptyMap())
                return@getPenugasan
            }

            val daftarPenugasan = hasilPenugasan?.data.orEmpty()
            bpskpRepository.getLaporan(authSession) { daftarLaporan, errorLaporan ->
                if (!isAdded || recyclerView == null) return@getLaporan
                if (errorLaporan != null) {
                    adapter.setLaporanPerSurat(emptyMap())
                    return@getLaporan
                }

                val laporanPerSurat = mutableMapOf<Long, LaporanData>()
                semuaSurat.forEach { surat ->
                    val suratId = surat.id?.toString()?.toLongOrNull() ?: return@forEach
                    val idPenugasanMilikSurat = daftarPenugasan
                        .filter { it.suratMasukId == suratId }
                        .mapNotNull { it.id }
                        .toSet()

                    val laporanTerbaru = daftarLaporan.orEmpty()
                        .filter { laporan -> laporan.penugasanId?.toLong()?.let { it in idPenugasanMilikSurat } == true }
                        .maxByOrNull { it.id ?: 0 }

                    if (laporanTerbaru?.id != null) laporanPerSurat[suratId] = laporanTerbaru
                }
                adapter.setLaporanPerSurat(laporanPerSurat)
            }
        }
    }

    private fun bukaDetailLaporan(laporanRingkas: LaporanData) {
        val idLaporan = laporanRingkas.id
        if (idLaporan == null || idLaporan <= 0) {
            showMessage("ID laporan tidak valid.")
            return
        }

        val loading = AlertDialog.Builder(requireContext())
            .setTitle("Detail Laporan")
            .setMessage("Memuat detail laporan...")
            .setCancelable(false)
            .create()
        loading.show()
        styleDialog(loading)

        bpskpRepository.getDetailLaporan(authSession, idLaporan) { laporan, error ->
            if (!isAdded || recyclerView == null) {
                if (loading.isShowing) loading.dismiss()
                return@getDetailLaporan
            }
            if (loading.isShowing) loading.dismiss()

            if (error != null || laporan == null) {
                showMessage(error ?: "Detail laporan tidak ditemukan.")
                return@getDetailLaporan
            }

            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(8), dp(24), dp(16))
            }

            fun addSection(title: String, content: String?) {
                container.addView(TextView(requireContext()).apply {
                    text = title
                    textSize = 12f
                    setTextColor(Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, dp(12), 0, dp(4))
                })
                container.addView(TextView(requireContext()).apply {
                    text = content ?: "-"
                    textSize = 14f
                    setTextColor(Color.BLACK)
                })
            }

            addSection("Pelapor", laporan.pelapor)
            addSection("Tanggal Laporan", laporan.tanggalLaporan)
            addSection("Ringkasan", laporan.ringkasan)
            addSection("Hasil Kegiatan", laporan.hasilKegiatan)
            addSection("Catatan", laporan.catatan?.takeIf { it.isNotBlank() })

            // Section Dokumen Lampiran (Link bisa diklik)
            container.addView(TextView(requireContext()).apply {
                text = "Dokumen Lampiran"
                textSize = 12f
                setTextColor(Color.BLACK)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(4))
            })

            val docs = laporan.dokumen.orEmpty()
            if (docs.isEmpty()) {
                container.addView(TextView(requireContext()).apply {
                    text = "Tidak ada dokumen lampiran."
                    textSize = 14f
                    setTextColor(Color.BLACK)
                })
            } else {
                docs.forEachIndexed { index, dokumen ->
                    val tvDoc = TextView(requireContext()).apply {
                        text = "${index + 1}. ${dokumen.namaFile ?: dokumen.jenisDokumen ?: "Dokumen"}"
                        textSize = 14f
                        setTextColor(Color.parseColor("#0056B3")) // Biru Link
                        paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG // Underline
                        setPadding(0, dp(4), 0, dp(8))
                        setOnClickListener {
                            // PASTIKAN NAMA PROPERTI DI BAWAH INI SESUAI DENGAN MODEL LaporanData.Dokumen ANDA
                            // (Misal: file_url, fileUrl, atau url)
                            val pathDokumen = dokumen.pathFile

                            if (!pathDokumen.isNullOrBlank()) {

                                val urlDokumen = BpskpRetrofitClient.getDokumenUrl(pathDokumen)

                                Log.d("PreviewLaporan", "Path asli  : $pathDokumen")
                                Log.d("PreviewLaporan", "URL preview: $urlDokumen")

                                val imageView = android.widget.ImageView(requireContext()).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        dp(300)
                                    )
                                    adjustViewBounds = true
                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    setPadding(dp(16), dp(16), dp(16), dp(16))
                                }

                                val imageDialog = AlertDialog.Builder(requireContext())
                                    .setView(imageView)
                                    .setPositiveButton("Tutup", null)
                                    .create()

                                imageDialog.show()
                                styleDialog(imageDialog)

                                com.bumptech.glide.Glide.with(requireContext())
                                    .load(urlDokumen)
                                    .placeholder(android.R.drawable.ic_popup_sync)
                                    .error(R.drawable.ic_menu1)
                                    .into(imageView)

                            } else {
                                Log.e("PreviewLaporan", "Path dokumen kosong atau null")
                                showMessage("URL tautan dokumen tidak tersedia dari server.")
                            }
                        }
                    }
                    container.addView(tvDoc)
                }
            }

            val scroll = ScrollView(requireContext()).apply {
                addView(container)
            }

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Detail Laporan Kegiatan")
                .setView(scroll)
                .setPositiveButton("Tutup", null)
                .create()

            dialog.show()
            styleDialog(dialog)
        }
    }

    private fun showDetailSurat(surat: SuratMasuk) {
        if (!isAdded) return
        val detail = """
            Nomor surat
            ${surat.nomor_surat ?: "-"}

            Perihal
            ${surat.perihal ?: "-"}

            Asal surat
            ${surat.asal_surat ?: "-"}

            Tanggal surat
            ${surat.tanggal_surat ?: "-"}

            Tanggal diterima
            ${surat.tanggal_diterima ?: "-"}

            Status surat
            ${formatStatus(surat.status)}

            Ringkasan
            ${surat.isi_ringkas ?: "-"}
        """.trimIndent()
        AlertDialog.Builder(requireContext())
            .setTitle("Detail Surat Masuk")
            .setMessage(detail)
            .setPositiveButton("Tutup", null)
            .show().also { styleDialog(it) }
    }

    private fun ambilPilihanPenerima(suratId: Long, callback: (List<PenerimaOption>?, String?) -> Unit) {
        bpskpRepository.getUser(authSession) { response, userError ->
            if (!isAdded) return@getUser
            val user = response?.user
            if (user == null) {
                callback(null, userError ?: "Gagal mengambil data pengguna login.")
                return@getUser
            }
            val pilihan = mutableListOf(PenerimaOption(id = user.id, nama = "Saya sendiri — ${user.name}"))
            bpskpRepository.getTim(authSession) { daftarTim, timError ->
                if (!isAdded) return@getTim
                if (daftarTim == null) {
                    callback(null, timError ?: "Gagal mengambil daftar tim.")
                    return@getTim
                }
                val tim = daftarTim.firstOrNull { it.ketuaId == user.id }
                if (tim == null) {
                    periksaBentrokPenerima(suratId, pilihan, callback)
                    return@getTim
                }
                bpskpRepository.getDetailTim(authSession, tim.id) { detail, detailError ->
                    if (!isAdded) return@getDetailTim
                    if (detail == null) {
                        callback(null, detailError ?: "Gagal mengambil anggota tim.")
                        return@getDetailTim
                    }
                    detail.anggota.filter { it.pegawaiId != user.id }.forEach { anggota ->
                        pilihan.add(PenerimaOption(id = anggota.pegawaiId, nama = anggota.nama))
                    }
                    periksaBentrokPenerima(suratId, pilihan, callback)
                }
            }
        }
    }

    private fun periksaBentrokPenerima(suratId: Long, pilihan: List<PenerimaOption>, callback: (List<PenerimaOption>?, String?) -> Unit) {
        bpskpRepository.getPenerimaBentrok(session = authSession, suratMasukId = suratId) { userIdsBentrok, error ->
            if (!isAdded) return@getPenerimaBentrok
            if (userIdsBentrok == null) {
                callback(null, error ?: "Gagal memeriksa jadwal kegiatan pegawai.")
                return@getPenerimaBentrok
            }
            val idBentrok = userIdsBentrok.toSet()
            val tersedia = pilihan.filter { it.id !in idBentrok }
            callback(tersedia, null)
        }
    }

    private fun showDialogPenugasan(surat: SuratMasuk, penugasanUlang: Boolean = false) {
        if (!isAdded || isSubmitting) return
        if (!penugasanUlang && surat.status != "baru") {
            showMessage("Surat ini tidak lagi berstatus baru. Muat ulang daftar surat.")
            loadSuratMasuk()
            return
        }
        val suratId = surat.id?.toString()?.toLongOrNull()
        if (suratId == null || suratId <= 0L) {
            showMessage("ID surat tidak valid.")
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val labelPenerima = TextView(requireContext()).apply {
            text = "Memuat daftar penerima dan memeriksa jadwal..."
            textSize = 14f
            setTextColor(Color.BLACK)
        }
        val spinnerPenerima = Spinner(requireContext())
        val catatanInput = EditText(requireContext()).apply {
            hint = "Catatan penugasan (opsional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = Gravity.TOP
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
        }

        container.addView(labelPenerima)
        container.addView(spinnerPenerima, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
            bottomMargin = dp(16)
        })
        container.addView(TextView(requireContext()).apply {
            text = "Catatan penugasan (opsional)"
            textSize = 14f
            setTextColor(Color.BLACK)
        })
        container.addView(catatanInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (penugasanUlang) "Tugaskan Ulang Surat" else "Tugaskan Surat")
            .setMessage("Nomor: ${surat.nomor_surat ?: "-"}\nPerihal: ${surat.perihal ?: "-"}")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan Penugasan", null)
            .create()

        dialog.setOnShowListener {
            val tombolSimpan = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            tombolSimpan.isEnabled = false
            ambilPilihanPenerima(suratId) { pilihan, error ->
                if (!isAdded || !dialog.isShowing) return@ambilPilihanPenerima
                if (error != null) {
                    labelPenerima.text = "Gagal memeriksa jadwal pegawai."
                    tombolSimpan.isEnabled = false
                    showMessage(error)
                    return@ambilPilihanPenerima
                }
                if (pilihan.isNullOrEmpty()) {
                    labelPenerima.text = "Semua pegawai memiliki jadwal yang bentrok."
                    labelPenerima.setTextColor(Color.parseColor("#DC2626"))
                    tombolSimpan.isEnabled = false
                    showMessage("Tidak ada penerima yang tersedia pada jadwal acara ini.")
                    return@ambilPilihanPenerima
                }
                spinnerPenerima.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, pilihan).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                labelPenerima.text = "Pilih penerima tugas"
                labelPenerima.setTextColor(Color.BLACK)
                tombolSimpan.isEnabled = true
                tombolSimpan.setOnClickListener {
                    if (isSubmitting) return@setOnClickListener
                    val penerima = spinnerPenerima.selectedItem as? PenerimaOption
                    if (penerima == null || penerima.id <= 0) {
                        showMessage("Pilih penerima tugas terlebih dahulu.")
                        return@setOnClickListener
                    }
                    val request = CreatePenugasanRequest(
                        suratMasukId = suratId,
                        ditugaskanKepada = penerima.id.toLong(),
                        catatanPenugasan = catatanInput.text.toString().trim().ifBlank { null }
                    )
                    dialog.dismiss()
                    buatPenugasan(request)
                }
            }
        }
        dialog.show()
        styleDialog(dialog)
    }

    private fun styleDialog(dialog: AlertDialog) {
        val cornerRadius = dp(22).toFloat()
        dialog.window?.setBackgroundDrawable(
            GradientDrawable().apply {
                setColor(Color.WHITE)
                this.cornerRadius = cornerRadius
            }
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dp(520)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Menggunakan resource identifier dinamis agar terhindar dari Unresolved reference
        val titleId = resources.getIdentifier("alertTitle", "id", "android")
        if (titleId != 0) {
            dialog.findViewById<TextView>(titleId)?.apply {
                setTextColor(Color.BLACK)
                textSize = 19f
                setPadding(dp(24), dp(20), dp(24), dp(8))
            }
        }

        val messageId = resources.getIdentifier("message", "id", "android")
        if (messageId != 0) {
            dialog.findViewById<TextView>(messageId)?.apply {
                setTextColor(Color.BLACK)
                textSize = 14f
                setPadding(dp(24), dp(8), dp(24), dp(12))
            }
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(Color.rgb(249, 115, 22))
            isAllCaps = false
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(Color.rgb(100, 100, 100))
            isAllCaps = false
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            setTextColor(Color.rgb(249, 115, 22))
            isAllCaps = false
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buatPenugasan(request: CreatePenugasanRequest, setelahBerhasil: (() -> Unit)? = null) {
        if (!isAdded || isSubmitting) return
        isSubmitting = true
        bpskpRepository.buatPenugasan(session = authSession, request = request) { result, error ->
            if (!isAdded || recyclerView == null) {
                isSubmitting = false
                return@buatPenugasan
            }
            isSubmitting = false
            if (error != null) {
                showMessage(error)
                return@buatPenugasan
            }
            if (result?.status != "success") {
                showMessage(result?.message ?: "Penugasan belum berhasil disimpan.")
                return@buatPenugasan
            }
            showMessage(result.message ?: "Penugasan berhasil dibuat.")
            loadSuratMasuk()
            setelahBerhasil?.invoke()
        }
    }

    private fun showPenugasan(surat: SuratMasuk) {
        if (!isAdded) return
        val suratId = surat.id?.toString()?.toLongOrNull()
        if (suratId == null) {
            showMessage("ID surat tidak valid.")
            return
        }
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Penugasan Surat")
            .setMessage("Memuat data penugasan...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        styleDialog(loadingDialog)

        bpskpRepository.getPenugasan(session = authSession, search = null, status = null) { result, error ->
            if (!isAdded || recyclerView == null) {
                if (loadingDialog.isShowing) loadingDialog.dismiss()
                return@getPenugasan
            }
            if (loadingDialog.isShowing) loadingDialog.dismiss()
            if (error != null) {
                showMessage(error)
                return@getPenugasan
            }
            val daftar = result?.data.orEmpty().filter { it.suratMasukId == suratId }
            if (daftar.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Penugasan Surat")
                    .setMessage("Belum ada data penugasan untuk surat ini.")
                    .setPositiveButton("Tutup", null)
                    .show().also { styleDialog(it) }
                return@getPenugasan
            }
            tampilkanDaftarPenugasan(surat, daftar)
        }
    }

    private fun tampilkanDaftarPenugasan(surat: SuratMasuk, penugasan: List<PenugasanResponse>) {
        if (!isAdded) return
        val riwayat = penugasan.sortedByDescending { it.id ?: 0L }
        val terakhir = riwayat.firstOrNull()
        val bolehTugaskanUlang = terakhir?.status.equals("ditolak", ignoreCase = true)

        val detail = buildString {
            appendLine("Nomor surat\n${surat.nomor_surat ?: "-"}\n")
            appendLine("Perihal\n${surat.perihal ?: "-"}\n")
            appendLine("RIWAYAT PENUGASAN\n------------------------------")
            riwayat.asReversed().forEachIndexed { index, item ->
                val nama = item.namaPenerima?.takeIf { it.isNotBlank() } ?: item.ditugaskanKepada?.toString() ?: "-"
                appendLine("\nPenugasan ${index + 1}")
                appendLine("Pegawai: $nama")
                appendLine("Status: ${formatStatus(item.status)}")
                appendLine("Catatan: ${item.catatanPenugasan ?: "-"}")
                if (!item.waktuRespon.isNullOrBlank()) appendLine("Waktu respons: ${item.waktuRespon}")
                if (item.status.equals("ditolak", true)) {
                    appendLine("Alasan penolakan:\n${item.catatanRespon?.takeIf { it.isNotBlank() } ?: "Pegawai tidak memberikan keterangan."}")
                } else if (item.status.equals("diterima", true) && !item.catatanRespon.isNullOrBlank()) {
                    appendLine("Catatan respons: ${item.catatanRespon}")
                }
                appendLine("------------------------------")
            }
            when {
                bolehTugaskanUlang -> appendLine("\nPenugasan terakhir ditolak. Kamu dapat memilih pegawai lain.")
                terakhir?.status.equals("menunggu_respon", true) -> appendLine("\nPenugasan sedang menunggu respons pegawai.")
                terakhir?.status.equals("diterima", true) -> appendLine("\nPenugasan terakhir telah diterima.")
            }
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (bolehTugaskanUlang) "Riwayat Penugasan" else "Daftar Penugasan")
            .setMessage(detail.trim())
            .setNegativeButton("Tutup", null)

        if (bolehTugaskanUlang) {
            builder.setPositiveButton("Tugaskan Ulang") { _, _ -> showDialogPenugasan(surat, penugasanUlang = true) }
        }
        builder.setNeutralButton("Lihat Rekomendasi") { _, _ -> muatRekomendasiSurat(surat, riwayat) }
        builder.show().also { styleDialog(it) }
    }

    private fun muatRekomendasiSurat(surat: SuratMasuk, riwayat: List<PenugasanResponse>) {
        if (!isAdded || isSubmitting) return
        val idPenugasan = riwayat.mapNotNull { it.id }.toSet()
        if (idPenugasan.isEmpty()) {
            showMessage("Tidak ada penugasan untuk diperiksa.")
            return
        }
        val loading = AlertDialog.Builder(requireContext())
            .setTitle("Rekomendasi Pengganti")
            .setMessage("Memuat rekomendasi pegawai...")
            .setCancelable(false)
            .create()
        loading.show()
        styleDialog(loading)

        bpskpRepository.getRekomendasi(session = authSession, status = null) { daftar, error ->
            if (!isAdded || recyclerView == null) {
                if (loading.isShowing) loading.dismiss()
                return@getRekomendasi
            }
            if (loading.isShowing) loading.dismiss()
            if (error != null) {
                showMessage(error)
                return@getRekomendasi
            }
            val rekomendasiSurat = daftar.orEmpty().filter { it.penugasanId in idPenugasan }.sortedByDescending { it.id }
            if (rekomendasiSurat.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Rekomendasi Pengganti")
                    .setMessage("Belum ada rekomendasi pengganti untuk surat ${surat.nomor_surat ?: "-"}.")
                    .setPositiveButton("Tutup", null)
                    .show().also { styleDialog(it) }
                return@getRekomendasi
            }
            tampilkanPilihanRekomendasi(surat = surat, riwayat = riwayat, rekomendasi = rekomendasiSurat)
        }
    }

    private fun tampilkanPilihanRekomendasi(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: List<RekomendasiData>) {
        if (!isAdded) return
        val menunggu = rekomendasi.filter { !it.status.equals("diterima", true) && !it.status.equals("ditolak", true) }
        if (menunggu.isEmpty()) {
            val riwayatRekomendasi = rekomendasi.joinToString("\n\n") {
                "Pengaju: ${it.namaPegawaiAsal ?: "-"}\nPengganti: ${it.namaPegawaiRekomendasi ?: "-"}\nAlasan: ${it.alasan ?: "-"}\nStatus: ${formatStatus(it.status)}"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Riwayat Rekomendasi")
                .setMessage(riwayatRekomendasi)
                .setPositiveButton("Tutup", null)
                .show().also { styleDialog(it) }
            return
        }
        val label = menunggu.map { "${it.namaPegawaiAsal ?: "Pegawai"} → ${it.namaPegawaiRekomendasi ?: "Pengganti"}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Rekomendasi Menunggu Persetujuan")
            .setItems(label) { _, index -> tampilkanDetailRekomendasi(surat = surat, riwayat = riwayat, rekomendasi = menunggu[index]) }
            .setNegativeButton("Tutup", null)
            .show().also { styleDialog(it) }
    }

    private fun tampilkanDetailRekomendasi(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: RekomendasiData) {
        if (!isAdded) return
        val detail = """
            PENGAJU REKOMENDASI
            ${rekomendasi.namaPegawaiAsal ?: "-"}

            PEGAWAI YANG DIREKOMENDASIKAN
            ${rekomendasi.namaPegawaiRekomendasi ?: "-"}

            ALASAN REKOMENDASI
            ${rekomendasi.alasan ?: "-"}

            STATUS
            ${formatStatus(rekomendasi.status)}
        """.trimIndent()
        AlertDialog.Builder(requireContext())
            .setTitle("Detail Rekomendasi")
            .setMessage(detail)
            .setNegativeButton("Kembali") { _, _ -> tampilkanPilihanRekomendasi(surat, riwayat, listOf(rekomendasi)) }
            .setNeutralButton("Tolak") { _, _ -> dialogPilihPegawaiAlternatif(surat = surat, riwayat = riwayat, rekomendasi = rekomendasi) }
            .setPositiveButton("Setujui") { _, _ -> konfirmasiProsesRekomendasi(surat = surat, riwayat = riwayat, rekomendasi = rekomendasi, status = "diterima") }
            .show().also { styleDialog(it) }
    }

    private fun konfirmasiProsesRekomendasi(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: RekomendasiData, status: String) {
        if (!isAdded || isSubmitting) return
        val judul = if (status == "diterima") "Setujui Rekomendasi" else "Tolak Rekomendasi"
        val pesan = if (status == "diterima") "Setujui penggantian ke ${rekomendasi.namaPegawaiRekomendasi ?: "pegawai pilihan"}?" else "Tolak rekomendasi ini?"
        AlertDialog.Builder(requireContext())
            .setTitle(judul)
            .setMessage(pesan)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Konfirmasi") { _, _ -> prosesRekomendasi(surat = surat, riwayat = riwayat, rekomendasi = rekomendasi, status = status) }
            .show().also { styleDialog(it) }
    }

    private fun prosesRekomendasi(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: RekomendasiData, status: String) {
        if (!isAdded || isSubmitting) return
        isSubmitting = true
        bpskpRepository.prosesRekomendasi(session = authSession, id = rekomendasi.id, status = status) { result, error ->
            if (!isAdded || recyclerView == null) {
                isSubmitting = false
                return@prosesRekomendasi
            }
            isSubmitting = false
            if (error != null) {
                showMessage(error)
                return@prosesRekomendasi
            }
            if (result?.success != true) {
                showMessage(result?.message ?: "Rekomendasi gagal diproses.")
                return@prosesRekomendasi
            }
            showMessage(result.message ?: if (status == "diterima") "Rekomendasi berhasil disetujui." else "Rekomendasi berhasil ditolak.")
            loadSuratMasuk()
            if (status == "ditolak") tampilkanDaftarPenugasan(surat, riwayat)
        }
    }

    private fun dialogPilihPegawaiAlternatif(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: RekomendasiData) {
        if (!isAdded || isSubmitting) return
        val suratId = surat.id?.toString()?.toLongOrNull()
        if (suratId == null || suratId <= 0L) {
            showMessage("ID surat tidak valid.")
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        container.addView(TextView(requireContext()).apply { text = "Pegawai yang mengajukan"; textSize = 13f; setTextColor(Color.BLACK); setTypeface(null, android.graphics.Typeface.BOLD) })
        container.addView(TextView(requireContext()).apply { text = rekomendasi.namaPegawaiAsal ?: "-"; textSize = 16f; setTextColor(Color.BLACK) })

        container.addView(TextView(requireContext()).apply { text = "Pegawai yang direkomendasikan"; textSize = 13f; setPadding(0, dp(12), 0, 0); setTextColor(Color.BLACK); setTypeface(null, android.graphics.Typeface.BOLD) })
        container.addView(TextView(requireContext()).apply { text = rekomendasi.namaPegawaiRekomendasi ?: "-"; textSize = 16f; setTextColor(Color.BLACK) })

        container.addView(TextView(requireContext()).apply { text = "Alasan rekomendasi"; textSize = 13f; setPadding(0, dp(12), 0, 0); setTextColor(Color.BLACK); setTypeface(null, android.graphics.Typeface.BOLD) })
        container.addView(TextView(requireContext()).apply { text = rekomendasi.alasan ?: "-"; textSize = 14f; setTextColor(Color.BLACK) })

        container.addView(TextView(requireContext()).apply { text = "Pilih pegawai alternatif"; textSize = 14f; setPadding(0, dp(18), 0, dp(4)); setTextColor(Color.BLACK); setTypeface(null, android.graphics.Typeface.BOLD) })

        val spinner = Spinner(requireContext())
        container.addView(spinner)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tolak & Pilih Pengganti")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Tolak & Tugaskan", null)
            .create()

        dialog.setOnShowListener {
            val tombol = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            tombol.isEnabled = false
            ambilPilihanPenerima(suratId) { pilihan, error ->
                if (!isAdded || !dialog.isShowing) return@ambilPilihanPenerima
                if (error != null) {
                    showMessage(error)
                    tombol.isEnabled = false
                    return@ambilPilihanPenerima
                }
                val idTerpakai = riwayat.filter { it.status.equals("menunggu_respon", ignoreCase = true) || it.status.equals("diterima", ignoreCase = true) }.mapNotNull { it.ditugaskanKepada }.toSet()
                val kandidat = pilihan.orEmpty().filter { pegawai -> pegawai.id != rekomendasi.pegawaiAsalId && pegawai.id != rekomendasi.pegawaiRekomendasiId && pegawai.id.toLong() !in idTerpakai }
                if (kandidat.isEmpty()) {
                    showMessage("Tidak ada pegawai alternatif yang tersedia dan tidak bentrok jadwal.")
                    tombol.isEnabled = false
                    return@ambilPilihanPenerima
                }
                spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kandidat).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                tombol.isEnabled = true
                tombol.setOnClickListener {
                    if (isSubmitting) return@setOnClickListener
                    val alternatif = spinner.selectedItem as? PenerimaOption
                    if (alternatif == null) {
                        showMessage("Pilih pegawai alternatif.")
                        return@setOnClickListener
                    }
                    val request = CreatePenugasanRequest(
                        suratMasukId = suratId,
                        ditugaskanKepada = alternatif.id.toLong(),
                        catatanPenugasan = "Penugasan alternatif setelah rekomendasi ditolak. Pegawai yang direkomendasikan: ${rekomendasi.namaPegawaiRekomendasi ?: "-"}."
                    )
                    dialog.dismiss()
                    konfirmasiTolakDanTugaskan(surat = surat, riwayat = riwayat, rekomendasi = rekomendasi, request = request, namaAlternatif = alternatif.nama)
                }
            }
        }
        dialog.show()
        styleDialog(dialog)
    }

    private fun konfirmasiTolakDanTugaskan(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: RekomendasiData, request: CreatePenugasanRequest, namaAlternatif: String) {
        if (!isAdded || isSubmitting) return
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Penolakan")
            .setMessage("Rekomendasi dari ${rekomendasi.namaPegawaiAsal ?: "pegawai"} akan ditolak.\n\nPegawai alternatif: $namaAlternatif\n\nLanjutkan?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Konfirmasi") { _, _ -> tolakLaluBuatPenugasan(surat = surat, riwayat = riwayat, rekomendasi = rekomendasi, request = request) }
            .show().also { styleDialog(it) }
    }

    private fun tolakLaluBuatPenugasan(surat: SuratMasuk, riwayat: List<PenugasanResponse>, rekomendasi: RekomendasiData, request: CreatePenugasanRequest) {
        if (!isAdded || isSubmitting) return
        isSubmitting = true
        bpskpRepository.prosesRekomendasi(session = authSession, id = rekomendasi.id, status = "ditolak") { result, error ->
            if (!isAdded || recyclerView == null) {
                isSubmitting = false
                return@prosesRekomendasi
            }
            if (error != null || result?.success != true) {
                isSubmitting = false
                showMessage(error ?: result?.message ?: "Rekomendasi gagal ditolak. Penugasan alternatif tidak dibuat.")
                return@prosesRekomendasi
            }
            bpskpRepository.buatPenugasan(session = authSession, request = request) { hasilPenugasan, errorPenugasan ->
                if (!isAdded || recyclerView == null) {
                    isSubmitting = false
                    return@buatPenugasan
                }
                isSubmitting = false
                if (errorPenugasan != null || hasilPenugasan?.status != "success") {
                    showMessage("Rekomendasi berhasil ditolak, tetapi penugasan alternatif gagal dibuat. ${errorPenugasan ?: hasilPenugasan?.message ?: "Silakan periksa riwayat penugasan."}")
                    loadSuratMasuk()
                    tampilkanDaftarPenugasan(surat, riwayat)
                    return@buatPenugasan
                }
                showMessage("Rekomendasi ditolak dan penugasan alternatif berhasil dibuat.")
                loadSuratMasuk()
            }
        }
    }

    private fun showDetailSuratDenganPenugasan(surat: SuratMasuk) {
        if (!isAdded) return
        val suratId = surat.id?.toString()?.toLongOrNull()
        if (suratId == null) {
            showDetailSurat(surat)
            return
        }
        bpskpRepository.getPenugasan(session = authSession, search = null, status = null) { result, error ->
            if (!isAdded || recyclerView == null) return@getPenugasan
            if (error != null) {
                showMessage(error)
                return@getPenugasan
            }
            val daftar = result?.data.orEmpty().filter { it.suratMasukId == suratId }
            val informasiPenerima = if (daftar.isEmpty()) {
                "Belum ada penugasan"
            } else {
                daftar.joinToString("\n") { item ->
                    val nama = item.namaPenerima?.takeIf { it.isNotBlank() } ?: item.ditugaskanKepada?.toString() ?: "-"
                    "• $nama — ${formatStatus(item.status)}"
                }
            }
            val detail = """
                Nomor surat
                ${surat.nomor_surat ?: "-"}

                Perihal
                ${surat.perihal ?: "-"}

                Asal surat
                ${surat.asal_surat ?: "-"}

                Tanggal surat
                ${surat.tanggal_surat ?: "-"}

                Tanggal diterima
                ${surat.tanggal_diterima ?: "-"}

                Status surat
                ${formatStatus(surat.status)}

                Riwayat penugasan
                $informasiPenerima

                Ringkasan
                ${surat.isi_ringkas ?: "-"}
            """.trimIndent()
            val builder = AlertDialog.Builder(requireContext())
                .setTitle("Detail Surat Masuk")
                .setMessage(detail)
                .setNegativeButton("Tutup", null)
            if (daftar.isNotEmpty()) {
                builder.setPositiveButton("Lihat Penugasan") { _, _ -> tampilkanDaftarPenugasan(surat, daftar) }
            }
            builder.show().also { styleDialog(it) }
        }
    }

    private fun formatStatus(status: String?): String {
        return when (status?.lowercase()) {
            "baru" -> "Baru"
            "diproses" -> "Diproses"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            "menunggu_respon" -> "Menunggu respons"
            "diterima" -> "Diterima"
            "ditolak" -> "Ditolak"
            "direkomendasikan" -> "Direkomendasikan"
            else -> status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "-"
        }
    }

    private fun showMessage(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        recyclerView?.adapter = null
        recyclerView = null
        tvJumlahSuratBaru = null
        tvJumlahSuratDiproses = null
        btnFilterSemua = null
        btnFilterBaru = null
        btnFilterDiproses = null
        super.onDestroyView()
    }

    private class SuratMasukAdapter(
        private val onDetailClick: (SuratMasuk) -> Unit,
        private val onActionClick: (SuratMasuk) -> Unit,
        private val onReportClick: (LaporanData) -> Unit
    ) : RecyclerView.Adapter<SuratMasukAdapter.SuratViewHolder>() {

        private val items = mutableListOf<SuratMasuk>()
        private var laporanPerSurat: Map<Long, LaporanData> = emptyMap()

        fun submitList(data: List<SuratMasuk>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }

        fun setLaporanPerSurat(data: Map<Long, LaporanData>) {
            laporanPerSurat = data
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuratViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return SuratViewHolder(view)
        }

        override fun onBindViewHolder(holder: SuratViewHolder, position: Int) {
            val surat = items[position]
            val suratId = surat.id?.toString()?.toLongOrNull()
            val laporan = suratId?.let { laporanPerSurat[it] }

            holder.tvUserName.text = surat.perihal ?: "Tanpa perihal"
            holder.tvUserUsername.text = "Nomor: ${surat.nomor_surat ?: "-"}"
            holder.tvUserRole.text = when (surat.status?.lowercase()) {
                "baru" -> "SURAT BARU"
                "diproses" -> "DIPROSES"
                "selesai" -> "SELESAI"
                "dibatalkan" -> "DIBATALKAN"
                else -> surat.status?.uppercase() ?: "TIDAK DIKETAHUI"
            }

            holder.itemView.setOnClickListener { onDetailClick(surat) }
            holder.btnEditUser.text = "Detail"
            holder.btnEditUser.visibility = View.VISIBLE
            holder.btnEditUser.setOnClickListener { onDetailClick(surat) }

            when (surat.status?.lowercase()) {
                "baru" -> {
                    holder.btnDeleteUser.text = "Tugaskan"
                    holder.btnDeleteUser.visibility = View.VISIBLE
                    holder.btnDeleteUser.setOnClickListener { onActionClick(surat) }
                }
                "diproses" -> {
                    holder.btnDeleteUser.text = "Penugasan"
                    holder.btnDeleteUser.visibility = View.VISIBLE
                    holder.btnDeleteUser.setOnClickListener { onActionClick(surat) }
                }
                else -> {
                    holder.btnDeleteUser.visibility = View.GONE
                    holder.btnDeleteUser.setOnClickListener(null)
                }
            }

            if (laporan?.id != null) {
                holder.btnLaporan.text = "Lihat Laporan"
                holder.btnLaporan.visibility = View.VISIBLE
                holder.btnLaporan.setOnClickListener { onReportClick(laporan) }
            } else {
                holder.btnLaporan.visibility = View.GONE
                holder.btnLaporan.setOnClickListener(null)
            }
        }

        override fun getItemCount(): Int = items.size

        class SuratViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
            val tvUserUsername: TextView = itemView.findViewById(R.id.tvUserUsername)
            val tvUserRole: TextView = itemView.findViewById(R.id.tvUserRole)
            val btnEditUser: TextView = itemView.findViewById(R.id.btnEditUser)
            val btnDeleteUser: TextView = itemView.findViewById(R.id.btnDeleteUser)
            val btnLaporan: Button = buatTombolLaporan(itemView)

            companion object {
                private fun buatTombolLaporan(itemView: View): Button {
                    val context = itemView.context
                    val density = context.resources.displayMetrics.density
                    val button = Button(context).apply {
                        text = "Lihat Laporan"
                        textSize = 12f
                        isAllCaps = false
                        visibility = View.GONE
                        setTextColor(Color.WHITE)
                        background = GradientDrawable().apply {
                            setColor(Color.rgb(249, 115, 22))
                            cornerRadius = 12f * density
                        }
                        minHeight = (40 * density).toInt()
                        minimumHeight = minHeight
                        setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
                    }

                    val contentContainer = cariContainerVertikal(itemView)
                    if (contentContainer != null) {
                        contentContainer.addView(
                            button,
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = (8 * density).toInt() }
                        )
                    } else {
                        val parent = (itemView.findViewById<View>(R.id.btnDeleteUser)?.parent as? ViewGroup)
                            ?: (itemView as? ViewGroup)
                        parent?.addView(
                            button,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        )
                    }
                    return button
                }

                private fun cariContainerVertikal(root: View): LinearLayout? {
                    if (root is LinearLayout && root.orientation == LinearLayout.VERTICAL) {
                        val memilikiKeduaTombol = root.findViewById<View>(R.id.btnEditUser) != null && root.findViewById<View>(R.id.btnDeleteUser) != null
                        if (memilikiKeduaTombol) return root
                    }
                    if (root is ViewGroup) {
                        for (index in 0 until root.childCount) {
                            val found = cariContainerVertikal(root.getChildAt(index))
                            if (found != null) return found
                        }
                    }
                    return null
                }
            }
        }
    }
}