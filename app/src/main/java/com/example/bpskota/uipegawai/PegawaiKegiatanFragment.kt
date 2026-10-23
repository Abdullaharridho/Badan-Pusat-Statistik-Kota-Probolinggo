package com.example.bpskota.uipegawai

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.LaporanData
import com.example.bpskota.bpskp.model.LaporanDokumenUpload
import com.example.bpskota.bpskp.model.PenugasanResponse
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentPegawaiKegiatanBinding
import java.util.Locale

class PegawaiKegiatanFragment : Fragment() {

    private var _binding: FragmentPegawaiKegiatanBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BpskpRepository
    private lateinit var session: BpskpAuthSession
    private val daftarKegiatan = mutableListOf<PenugasanResponse>()
    private lateinit var adapter: KegiatanAdapter

    private var sedangMemuat = false
    private var dialogLoading: AlertDialog? = null

    private val penugasanLaporanSedangDibuka = mutableSetOf<Long>()
    private val dokumenBaruDipilih = mutableListOf<LaporanDokumenUpload>()
    private var tvDokumenBaruTerpilih: TextView? = null

    // =========================================================
    // PEMILIH DOKUMEN ANDROID
    // =========================================================
    private val pemilihDokumen = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!isAdded || _binding == null || uris.isEmpty()) return@registerForActivityResult

        uris.forEach { uri ->
            if (dokumenBaruDipilih.none { it.uri == uri }) {
                dokumenBaruDipilih.add(
                    LaporanDokumenUpload(
                        uri = uri,
                        jenisDokumen = "lainnya",
                        namaFile = ambilNamaFile(uri)
                    )
                )
            }
        }
        perbaruiLabelDokumenBaru()
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPegawaiKegiatanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = BpskpRepository()
        session = BpskpAuthSession(requireContext())

        setupRecyclerView()
        tampilkanRingkasan()
        muatPenugasan()

        binding.cardKelolaKegiatanHarian.setOnClickListener {
            startActivity(Intent(requireContext(), KegiatanHarianActivity::class.java))
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized && !sedangMemuat) muatPenugasan()
    }

    override fun onDestroyView() {
        dialogLoading?.dismiss()
        dialogLoading = null
        if (_binding != null) binding.rvPegawaiKegiatan.adapter = null
        penugasanLaporanSedangDibuka.clear()
        dokumenBaruDipilih.clear()
        tvDokumenBaruTerpilih = null
        _binding = null
        super.onDestroyView()
    }

    // =========================================================
    // UTILITAS & ANIMASI
    // =========================================================
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun animateDialog(dialog: AlertDialog, view: View) {
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        view.alpha = 0f
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(OvershootInterpolator(1.15f)).start()
    }

    private fun tampilkanPesan(pesan: String) {
        if (!isAdded || _binding == null) return
        Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
    }

    private fun tampilkanLoading(tampilkan: Boolean) {
        if (!isAdded) return
        if (tampilkan) {
            if (dialogLoading?.isShowing == true) return
            dialogLoading = AlertDialog.Builder(requireContext())
                .setMessage("Memproses data...")
                .setCancelable(false)
                .create()
            dialogLoading?.show()
        } else {
            dialogLoading?.dismiss()
            dialogLoading = null
        }
    }

    // =========================================================
    // RECYCLERVIEW & DATA LOAD
    // =========================================================
    private fun setupRecyclerView() {
        adapter = KegiatanAdapter(
            items = daftarKegiatan,
            onDetailClick = { bukaDetail(it) },
            onLaporanClick = { bukaLaporan(it) }
        )
        binding.rvPegawaiKegiatan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PegawaiKegiatanFragment.adapter
            setHasFixedSize(false)
        }
    }

    private fun muatPenugasan() {
        if (!isAdded || sedangMemuat) return
        val token = session.getToken()?.trim().orEmpty()
        if (token.isBlank()) {
            tampilkanPesan("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        sedangMemuat = true
        tampilkanLoading(true)

        repository.getPenugasan(session = session, search = null, status = null) { response, error ->
            if (!isAdded || _binding == null) { sedangMemuat = false; return@getPenugasan }
            sedangMemuat = false
            tampilkanLoading(false)

            if (error != null) { tampilkanPesan(error); return@getPenugasan }

            daftarKegiatan.clear()
            daftarKegiatan.addAll(response?.data.orEmpty().filterNot { it.status.equals("ditolak", ignoreCase = true) })
            adapter.notifyDataSetChanged()
            tampilkanRingkasan()
        }
    }

    private fun tampilkanRingkasan() {
        val menunggu = daftarKegiatan.count { it.status.equals("menunggu_respon", ignoreCase = true) }
        val diterima = daftarKegiatan.count { it.status.equals("diterima", ignoreCase = true) }
        val selesai = daftarKegiatan.count { it.status.equals("selesai", ignoreCase = true) }

        binding.tvKegiatanAkanDatang.text = (menunggu + diterima).toString().padStart(2, '0')
        binding.tvKegiatanPerluLaporan.text = diterima.toString().padStart(2, '0')
        binding.tvKegiatanSelesai.text = selesai.toString().padStart(2, '0')
        binding.tvJumlahKegiatan.text = "${daftarKegiatan.size} penugasan"

        if (daftarKegiatan.isEmpty()) tampilkanPesan("Belum ada penugasan yang dapat ditampilkan.")
    }

    private fun bukaDetail(item: PenugasanResponse) {
        val id = item.id
        if (id == null || id <= 0L) { tampilkanPesan("ID penugasan tidak valid."); return }
        val intent = Intent(requireContext(), KegiatanDetailActivity::class.java).apply { putExtra(KegiatanDetailActivity.EXTRA_ID_PENUGASAN, id) }
        startActivity(intent)
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // =========================================================
    // BUKA LAPORAN & DETAIL
    // =========================================================
    private fun bukaLaporan(item: PenugasanResponse) {
        if (!item.status.equals("selesai", ignoreCase = true)) {
            tampilkanPesan("Laporan hanya tersedia untuk penugasan yang selesai.")
            return
        }

        val idPenugasan = item.id ?: return
        if (!penugasanLaporanSedangDibuka.add(idPenugasan)) return

        tampilkanLoading(true)

        repository.getLaporan(session) { daftarLaporan, error ->
            if (!isAdded || _binding == null) { penugasanLaporanSedangDibuka.remove(idPenugasan); return@getLaporan }
            if (error != null) { tampilkanLoading(false); penugasanLaporanSedangDibuka.remove(idPenugasan); tampilkanPesan(error); return@getLaporan }

            val laporan = daftarLaporan.orEmpty().firstOrNull { it.penugasanId?.toLong() == idPenugasan }
            val idLaporan = laporan?.id

            if (idLaporan == null || idLaporan <= 0) {
                tampilkanLoading(false); penugasanLaporanSedangDibuka.remove(idPenugasan)
                tampilkanPesan("Laporan untuk penugasan ini tidak ditemukan.")
                return@getLaporan
            }

            repository.getDetailLaporan(session = session, id = idLaporan) { detail, detailError ->
                if (!isAdded || _binding == null) { penugasanLaporanSedangDibuka.remove(idPenugasan); return@getDetailLaporan }
                tampilkanLoading(false); penugasanLaporanSedangDibuka.remove(idPenugasan)

                if (detailError != null) { tampilkanPesan(detailError); return@getDetailLaporan }
                if (detail == null) { tampilkanPesan("Data detail laporan tidak tersedia."); return@getDetailLaporan }
                tampilkanDetailLaporan(detail)
            }
        }
    }

    private fun tampilkanDetailLaporan(laporan: LaporanData) {
        if (!isAdded) return
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(22))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(24).toFloat() }
        }

        val scrollView = ScrollView(context).apply { addView(container); isFillViewport = true; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }

        container.addView(TextView(context).apply { text = "Detail Laporan Kegiatan"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#111827")); setPadding(0, 0, 0, dp(16)) })

        fun tambahBagian(judul: String, isi: String?) {
            container.addView(TextView(context).apply { text = judul; textSize = 13f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, dp(8), 0, dp(4)) })
            container.addView(TextView(context).apply { text = isi?.takeIf { it.isNotBlank() } ?: "-"; textSize = 15f; setTextColor(Color.parseColor("#1F2937")); setPadding(0, 0, 0, dp(4)) })
        }

        tambahBagian("Tanggal Laporan", laporan.tanggalLaporan ?: laporan.createdAt)
        tambahBagian("Pelapor", laporan.pelapor)
        tambahBagian("Ringkasan", laporan.ringkasan)
        tambahBagian("Hasil Kegiatan", laporan.hasilKegiatan)
        tambahBagian("Catatan", laporan.catatan)

        val dokumen = laporan.dokumen.orEmpty()
        container.addView(TextView(context).apply { text = "Dokumen Lampiran (${dokumen.size})"; textSize = 15f; setTextColor(Color.parseColor("#111827")); setTypeface(null, Typeface.BOLD); setPadding(0, dp(16), 0, dp(8)) })

        if (dokumen.isEmpty()) {
            container.addView(TextView(context).apply { text = "Tidak ada dokumen lampiran."; textSize = 14f; setTextColor(Color.parseColor("#6B7280")) })
        } else {
            dokumen.forEachIndexed { index, file ->
                val namaFile = file.namaFile?.takeIf { it.isNotBlank() } ?: "Dokumen ${index + 1}"
                val itemDokumen = TextView(context).apply {
                    text = "📎 $namaFile\n${formatUkuranFile(file.ukuranFile)}"
                    textSize = 14f; setTextColor(Color.parseColor("#EA580C")); setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = GradientDrawable().apply { setColor(Color.parseColor("#FFF7ED")); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.parseColor("#FED7AA")) }
                    isClickable = true; isFocusable = true
                    setOnClickListener { bukaDokumenLampiran(file.pathFile.orEmpty()) }
                }
                container.addView(itemDokumen, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setView(scrollView)
            .setPositiveButton("Tutup", null)
            .setNeutralButton("Edit", null)
            .setNegativeButton("Hapus", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply { setTextColor(Color.parseColor("#EA580C")); setOnClickListener { dialog.dismiss(); tampilkanFormEditLaporan(laporan) } }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply { setTextColor(Color.parseColor("#DC2626")); setOnClickListener { dialog.dismiss(); konfirmasiHapusLaporan(laporan) } }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#6B7280"))
        }

        dialog.show()
        animateDialog(dialog, scrollView)
    }

    private fun bukaDokumenLampiran(path: String) {
        if (!isAdded || path.isBlank()) { tampilkanPesan("Lokasi dokumen tidak tersedia."); return }
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(BpskpRetrofitClient.getDokumenUrl(path))), "Buka dokumen"))
        } catch (e: Exception) { tampilkanPesan("Tidak dapat membuka dokumen: ${e.message}") }
    }

    // =========================================================
    // EDIT LAPORAN
    // =========================================================
    private fun tampilkanFormEditLaporan(laporan: LaporanData) {
        if (!isAdded) return
        val idLaporan = laporan.id; val idPenugasan = laporan.penugasanId
        if (idLaporan == null || idLaporan <= 0 || idPenugasan == null || idPenugasan <= 0) { tampilkanPesan("ID laporan atau penugasan tidak valid."); return }

        val context = requireContext()
        dokumenBaruDipilih.clear()
        val dokumenLama = laporan.dokumen.orEmpty()
        val idDokumenHapus = mutableSetOf<Int>()

        val form = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(20), dp(22), dp(22)); background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(24).toFloat() } }
        form.addView(TextView(context).apply { text = "Edit Laporan Kegiatan"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#111827")); setPadding(0, 0, 0, dp(16)) })

        fun tambahLabel(teks: String) = form.addView(TextView(context).apply { text = teks; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#374151")); setPadding(0, dp(12), 0, dp(6)) })
        fun buatInput(hintText: String, isiAwal: String?) = EditText(context).apply { hint = hintText; setText(isiAwal.orEmpty()); textSize = 14f; setTextColor(Color.parseColor("#1F2937")); setHintTextColor(Color.parseColor("#9CA3AF")); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; gravity = Gravity.TOP; minLines = 3; maxLines = 6; setPadding(dp(12), dp(10), dp(12), dp(10)); background = GradientDrawable().apply { setColor(Color.parseColor("#F9FAFB")); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.parseColor("#E5E7EB")) } }

        val inputRingkasan = buatInput("Masukkan ringkasan kegiatan", laporan.ringkasan)
        val inputHasil = buatInput("Masukkan hasil kegiatan", laporan.hasilKegiatan)
        val inputCatatan = buatInput("Masukkan catatan (opsional)", laporan.catatan)

        tambahLabel("Ringkasan Kegiatan"); form.addView(inputRingkasan)
        tambahLabel("Hasil Kegiatan"); form.addView(inputHasil)
        tambahLabel("Catatan"); form.addView(inputCatatan)

        tambahLabel("Dokumen Lampiran Saat Ini")
        form.addView(TextView(context).apply { text = "Centang dokumen yang ingin dihapus. Sisanya akan dipertahankan."; textSize = 12f; setTextColor(Color.parseColor("#6B7280")) })

        if (dokumenLama.isEmpty()) {
            form.addView(TextView(context).apply { text = "Belum ada dokumen."; textSize = 13f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, dp(8), 0, dp(4)) })
        } else {
            dokumenLama.forEach { doc ->
                form.addView(CheckBox(context).apply {
                    text = "${doc.namaFile ?: "Dokumen"}\n${doc.jenisDokumen ?: "Dokumen"}"; textSize = 13f; setTextColor(Color.parseColor("#374151")); setPadding(dp(4), dp(4), dp(4), dp(4))
                    setOnCheckedChangeListener { _, checked -> if (doc.id != null) { if (checked) idDokumenHapus.add(doc.id) else idDokumenHapus.remove(doc.id) } }
                })
            }
        }

        tambahLabel("Tambah Dokumen Baru")
        form.addView(TextView(context).apply { text = "Maksimal 5 dokumen total, 5 MB/file."; textSize = 12f; setTextColor(Color.parseColor("#6B7280")) })

        val btnPilih = TextView(context).apply {
            text = "＋ Pilih Dokumen"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#EA580C")); gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(Color.parseColor("#FFF7ED")); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.parseColor("#FDBA74")) }
            setOnClickListener { pemilihDokumen.launch(arrayOf("image/jpeg", "image/png", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }
        }
        form.addView(btnPilih, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        tvDokumenBaruTerpilih = TextView(context).apply { text = "Belum ada dokumen baru."; textSize = 13f; setTextColor(Color.parseColor("#4B5563")); setPadding(0, dp(8), 0, dp(4)) }
        form.addView(tvDokumenBaruTerpilih)

        form.addView(TextView(context).apply { text = "Hapus Pilihan Baru"; textSize = 12f; setTextColor(Color.parseColor("#DC2626")); setPadding(0, dp(8), 0, dp(4)); setOnClickListener { dokumenBaruDipilih.clear(); perbaruiLabelDokumenBaru() } })

        val scrollView = ScrollView(context).apply { isFillViewport = false; addView(form); layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.6f).toInt()) }

        val dialog = AlertDialog.Builder(context).setView(scrollView).setNegativeButton("Batal", null).setPositiveButton("Simpan", null).create()
        dialog.setOnDismissListener { tvDokumenBaruTerpilih = null }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor("#F97316"))
                setOnClickListener {
                    val ringkasan = inputRingkasan.text.toString().trim()
                    val hasil = inputHasil.text.toString().trim()
                    if (ringkasan.isBlank()) { inputRingkasan.error = "Wajib diisi."; return@setOnClickListener }
                    if (hasil.isBlank()) { inputHasil.error = "Wajib diisi."; return@setOnClickListener }
                    val totalDok = dokumenLama.size - idDokumenHapus.size + dokumenBaruDipilih.size
                    if (totalDok > 5) { tampilkanPesan("Maksimal 5 dokumen."); return@setOnClickListener }

                    dialog.dismiss()
                    simpanEditLaporan(idLaporan, idPenugasan, ringkasan, hasil, inputCatatan.text.toString().trim(), dokumenBaruDipilih.toList(), idDokumenHapus.toList())
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#6B7280"))
        }
        dialog.show()
        animateDialog(dialog, scrollView)
    }

    private fun simpanEditLaporan(idLaporan: Int, idPenugasan: Int, ringkasan: String, hasilKegiatan: String, catatan: String, dokumenBaru: List<LaporanDokumenUpload>, dokumenHapus: List<Int>) {
        if (!isAdded) return
        tampilkanLoading(true)
        repository.updateLaporan(requireContext(), session, idLaporan, idPenugasan, ringkasan, hasilKegiatan, catatan, dokumenBaru, dokumenHapus) { response, error ->
            if (!isAdded || _binding == null) return@updateLaporan
            tampilkanLoading(false)
            if (error != null || response?.success != true) { tampilkanPesan(error ?: "Gagal."); return@updateLaporan }
            dokumenBaruDipilih.clear()
            Toast.makeText(requireContext(), "Berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            muatPenugasan()
        }
    }

    private fun ambilNamaFile(uri: Uri): String {
        var namaFile = "dokumen_${System.currentTimeMillis()}"
        try { requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) namaFile = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) ?: namaFile } } catch (_: Exception) {}
        return namaFile
    }

    private fun perbaruiLabelDokumenBaru() {
        tvDokumenBaruTerpilih?.text = if (dokumenBaruDipilih.isEmpty()) "Belum ada dokumen." else dokumenBaruDipilih.mapIndexed { i, d -> "${i + 1}. ${d.namaFile}" }.joinToString("\n")
    }

    // =========================================================
    // HAPUS LAPORAN
    // =========================================================
    private fun konfirmasiHapusLaporan(laporan: LaporanData) {
        if (!isAdded) return
        val id = laporan.id ?: return

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Hapus Laporan?")
            .setMessage("Tindakan ini tidak dapat dibatalkan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ -> hapusLaporan(id) }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#DC2626"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#6B7280"))
        }
        dialog.show()
    }

    private fun hapusLaporan(idLaporan: Int) {
        if (!isAdded) return
        tampilkanLoading(true)
        repository.deleteLaporan(session, idLaporan) { berhasil, error ->
            if (!isAdded || _binding == null) return@deleteLaporan
            tampilkanLoading(false)
            if (!berhasil) { tampilkanPesan(error ?: "Gagal."); return@deleteLaporan }
            Toast.makeText(requireContext(), "Laporan dihapus.", Toast.LENGTH_SHORT).show()
            muatPenugasan()
        }
    }

    private fun formatUkuranFile(ukuran: Long?): String {
        val b = ukuran ?: return "Ukuran unknown"
        if (b <= 0L) return "0 B"
        val kb = b / 1024.0; val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$b B"
        }
    }

    // =========================================================
    // ADAPTER RECYCLERVIEW MENGGUNAKAN item_user.xml
    // =========================================================
    private inner class KegiatanAdapter(
        private val items: List<PenugasanResponse>,
        private val onDetailClick: (PenugasanResponse) -> Unit,
        private val onLaporanClick: (PenugasanResponse) -> Unit
    ) : RecyclerView.Adapter<KegiatanAdapter.KegiatanViewHolder>() {

        inner class KegiatanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nama: TextView = itemView.findViewById(R.id.tvUserName)
            val nomor: TextView = itemView.findViewById(R.id.tvUserUsername)
            val status: TextView = itemView.findViewById(R.id.tvUserRole)
            val tanggal: TextView = itemView.findViewById(R.id.tvKegiatanTanggal)
            val jam: TextView = itemView.findViewById(R.id.tvKegiatanJam)
            val lokasi: TextView = itemView.findViewById(R.id.tvKegiatanLokasi)
            val tombolDetail: TextView = itemView.findViewById(R.id.btnEditUser)
            val tombolLaporan: TextView = itemView.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KegiatanViewHolder = KegiatanViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))

        override fun onBindViewHolder(holder: KegiatanViewHolder, position: Int) {
            val item = items[position]
            val statusStr = item.status.orEmpty().lowercase()

            holder.nama.text = item.perihal ?: "Perihal tidak tersedia"
            holder.nomor.text = "Nomor surat: ${item.nomorSurat ?: "-"}"
            holder.tanggal.text = "Ditugaskan: ${item.waktuPenugasan ?: "-"}"
            holder.tanggal.visibility = View.VISIBLE
            holder.jam.visibility = View.GONE
            holder.lokasi.visibility = View.GONE

            holder.status.text = formatStatus(statusStr).uppercase(Locale.ROOT)
            val (warnaLatar, warnaTeks) = when (statusStr) {
                "diterima" -> Color.parseColor("#DCFCE7") to Color.parseColor("#15803D")
                "ditolak" -> Color.parseColor("#FEE2E2") to Color.parseColor("#B91C1C")
                "selesai" -> Color.parseColor("#D1FAE5") to Color.parseColor("#065F46")
                "menunggu_respon" -> Color.parseColor("#FFF7ED") to Color.parseColor("#C2410C")
                else -> Color.parseColor("#F3F4F6") to Color.parseColor("#4B5563")
            }

            holder.status.background = GradientDrawable().apply { setColor(warnaLatar); cornerRadius = dp(6).toFloat() }
            holder.status.setTextColor(warnaTeks)

            holder.tombolDetail.text = "Detail"
            holder.tombolDetail.visibility = View.VISIBLE
            holder.tombolDetail.setOnClickListener { onDetailClick(item) }

            if (statusStr == "selesai") {
                holder.tombolLaporan.visibility = View.VISIBLE
                holder.tombolLaporan.text = "Laporan"
                holder.tombolLaporan.background = GradientDrawable().apply { setColor(Color.parseColor("#E0E7FF")); cornerRadius = dp(6).toFloat() }
                holder.tombolLaporan.setTextColor(Color.parseColor("#4338CA"))
                holder.tombolLaporan.setOnClickListener { onLaporanClick(item) }
            } else {
                holder.tombolLaporan.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onDetailClick(item) }

            // Animasi Masuk
            holder.itemView.animate().cancel()
            holder.itemView.alpha = 0f
            holder.itemView.translationY = dp(20).toFloat()
            holder.itemView.scaleX = 0.96f
            holder.itemView.scaleY = 0.96f
            holder.itemView.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(1.1f)).setStartDelay((position % 6) * 35L).start()
        }

        override fun getItemCount(): Int = items.size
    }

    private fun formatStatus(status: String): String = when (status.lowercase()) {
        "menunggu_respon" -> "Menunggu"
        else -> status.replace("_", " ").replaceFirstChar { it.uppercase() }.ifBlank { "Unknown" }
    }
}