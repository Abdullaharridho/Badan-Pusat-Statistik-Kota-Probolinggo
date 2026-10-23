package com.example.bpskota.uipegawai

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.KegiatanHarian
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityKegiatanHarianBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KegiatanHarianActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKegiatanHarianBinding
    private lateinit var repository: BpskpRepository
    private lateinit var session: BpskpAuthSession
    private lateinit var adapter: KegiatanHarianAdapter
    private val daftarKegiatan = mutableListOf<KegiatanHarian>()
    private var sedangMemuat = false
    private var dialogLoading: AlertDialog? = null
    private var dialogForm: AlertDialog? = null

    private val warnaOranye = Color.parseColor("#F97316")
    private val warnaTeks = Color.parseColor("#1F2937")
    private val warnaSekunder = Color.parseColor("#6B7280")
    private val warnaBorder = Color.parseColor("#E7E5E4")
    private val warnaLatarInput = Color.WHITE
    private val warnaMerah = Color.parseColor("#DC2626")

    private val tanggalHariIni: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKegiatanHarianBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = BpskpRepository()
        session = BpskpAuthSession(this)

        setupRecyclerView()
        setupListener()
        tampilkanTanggal()
        muatKegiatanHarian()
    }

    private fun setupListener() {
        binding.btnBackKegiatanHarian.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.btnTambahKegiatanHarian.setOnClickListener { tampilkanFormKegiatan() }
        binding.btnTambahKegiatanKosong.setOnClickListener { tampilkanFormKegiatan() }
    }

    private fun setupRecyclerView() {
        adapter = KegiatanHarianAdapter(
            items = daftarKegiatan,
            onEdit = { tampilkanFormKegiatan(it) },
            onDelete = { konfirmasiHapus(it) }
        )
        binding.rvKegiatanHarian.apply {
            layoutManager = LinearLayoutManager(this@KegiatanHarianActivity)
            adapter = this@KegiatanHarianActivity.adapter
            setHasFixedSize(false)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }
    }

    private fun tampilkanTanggal() {
        binding.tvTanggalKegiatanHarian.text = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
    }

    private fun muatKegiatanHarian() {
        if (sedangMemuat || isFinishing || isDestroyed) return
        val token = session.getToken()?.trim().orEmpty()
        if (token.isBlank()) { tampilkanPesan("Sesi login tidak ditemukan. Silakan login kembali."); return }

        sedangMemuat = true
        tampilkanLoading(true, "Memuat kegiatan harian...")

        repository.getKegiatanHarian(session = session, tanggal = tanggalHariIni) { response, error ->
            if (isFinishing || isDestroyed) { sedangMemuat = false; return@getKegiatanHarian }
            sedangMemuat = false
            tampilkanLoading(false)

            if (error != null) {
                tampilkanPesan(error)
                perbaruiTampilanDaftar()
                return@getKegiatanHarian
            }

            daftarKegiatan.clear()
            daftarKegiatan.addAll(response.orEmpty())
            daftarKegiatan.sortBy { it.jamMulai }
            adapter.notifyDataSetChanged()
            perbaruiTampilanDaftar()
        }
    }

    private fun perbaruiTampilanDaftar() {
        binding.tvTotalKegiatanHarian.text = daftarKegiatan.size.toString()
        binding.tvSubjudulKegiatanHarian.text = if (daftarKegiatan.isEmpty()) "Belum ada aktivitas yang tercatat hari ini" else "Aktivitas yang tercatat hari ini"
        val kosong = daftarKegiatan.isEmpty()
        binding.rvKegiatanHarian.visibility = if (kosong) View.GONE else View.VISIBLE
        binding.layoutKosongKegiatanHarian.visibility = if (kosong) View.VISIBLE else View.GONE
    }

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

    private fun tampilkanFormKegiatan(kegiatan: KegiatanHarian? = null) {
        if (isFinishing || isDestroyed) return
        val sedangEdit = kegiatan != null
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(22))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(24).toFloat() }
        }

        container.addView(TextView(this).apply {
            text = if (sedangEdit) "Edit Kegiatan" else "Tambah Kegiatan"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#111827"))
            setPadding(0, 0, 0, dp(12))
        })

        val inputNama = buatInput("Contoh: Rapat koordinasi", kegiatan?.namaKegiatan.orEmpty(), true)
        val inputJamMulai = buatInput("Pilih jam mulai", kegiatan?.jamMulai?.take(5).orEmpty(), false).apply { isFocusable = false; isClickable = true; inputType = InputType.TYPE_NULL; setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_lock_idle_alarm, 0); compoundDrawablePadding = dp(8) }
        val inputJamSelesai = buatInput("Pilih jam selesai", kegiatan?.jamSelesai?.take(5).orEmpty(), false).apply { isFocusable = false; isClickable = true; inputType = InputType.TYPE_NULL; setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_lock_idle_alarm, 0); compoundDrawablePadding = dp(8) }
        val inputTanggal = buatInput("Pilih tanggal kegiatan", kegiatan?.tanggal ?: tanggalHariIni, false).apply { isFocusable = false; isClickable = true; inputType = InputType.TYPE_NULL; setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_my_calendar, 0); compoundDrawablePadding = dp(8) }

        tambahLabel(container, "Nama Kegiatan"); container.addView(inputNama)
        tambahLabel(container, "Jam Mulai"); container.addView(inputJamMulai)
        tambahLabel(container, "Jam Selesai"); container.addView(inputJamSelesai)
        tambahLabel(container, "Tanggal Kegiatan"); container.addView(inputTanggal)

        container.addView(TextView(this).apply { text = "Pilih tanggal melalui kalender yang tersedia."; textSize = 12f; setTextColor(warnaSekunder); setPadding(0, dp(6), 0, dp(4)) })

        inputJamMulai.setOnClickListener { tampilkanPemilihWaktu(inputJamMulai) }
        inputJamSelesai.setOnClickListener { tampilkanPemilihWaktu(inputJamSelesai) }
        inputTanggal.setOnClickListener { tampilkanPemilihTanggal(inputTanggal) }

        val scrollView = android.widget.ScrollView(this).apply { isFillViewport = false; addView(container) }
        val dialog = AlertDialog.Builder(this).setView(scrollView).setNegativeButton("Batal", null).setPositiveButton(if (sedangEdit) "Simpan Perubahan" else "Simpan", null).create()
        dialogForm = dialog

        dialog.setOnDismissListener { if (dialogForm === dialog) dialogForm = null }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(warnaOranye)
                setOnClickListener {
                    val nama = inputNama.text.toString().trim()
                    val mulai = inputJamMulai.text.toString().trim()
                    val selesai = inputJamSelesai.text.toString().trim()
                    val tanggal = inputTanggal.text.toString().trim()

                    if (nama.isBlank() || nama.length > 255) { inputNama.error = "Nama wajib diisi (max 255 char)."; inputNama.requestFocus(); return@setOnClickListener }
                    if (!validasiWaktu(mulai, selesai)) { tampilkanPesan("Pilih jam mulai dan selesai valid."); return@setOnClickListener }
                    if (selesai <= mulai) { tampilkanPesan("Jam selesai harus setelah jam mulai."); return@setOnClickListener }
                    if (!validasiTanggal(tanggal)) { inputTanggal.error = "Tanggal tidak valid."; inputTanggal.requestFocus(); return@setOnClickListener }

                    dialog.dismiss()
                    if (sedangEdit) updateKegiatan(kegiatan!!.id, nama, mulai, selesai, tanggal) else tambahKegiatan(nama, mulai, selesai, tanggal)
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(warnaSekunder)
        }
        dialog.show()
        animateDialog(dialog, container)
    }

    private fun buatInput(hintText: String, isiAwal: String, multiLine: Boolean): EditText {
        return EditText(this).apply {
            setText(isiAwal)
            hint = hintText
            setTextColor(warnaTeks)
            setHintTextColor(Color.parseColor("#9CA3AF"))
            textSize = 14f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(warnaLatarInput, 12).apply { setStroke(dp(1), warnaBorder) }
            if (multiLine) { minLines = 2; maxLines = 4; gravity = Gravity.TOP or Gravity.START; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
            else { minLines = 1; maxLines = 1; inputType = InputType.TYPE_CLASS_TEXT }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4) }
        }
    }

    private fun tambahLabel(parent: LinearLayout, teks: String) {
        parent.addView(TextView(this).apply { text = teks; textSize = 13f; setTextColor(warnaTeks); setTypeface(null, Typeface.BOLD); setPadding(0, dp(14), 0, dp(8)) })
    }

    private fun validasiTanggal(tanggal: String): Boolean = try { val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }; f.format(f.parse(tanggal)!!) == tanggal } catch (_: Exception) { false }
    private fun validasiWaktu(mulai: String, selesai: String): Boolean = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").let { it.matches(mulai) && it.matches(selesai) }

    private fun tampilkanPemilihWaktu(target: EditText) {
        val w = target.text.toString().trim()
        val jam = if (w.matches(Regex("^\\d{2}:\\d{2}$"))) w.substring(0, 2).toInt() else Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val mnt = if (w.matches(Regex("^\\d{2}:\\d{2}$"))) w.substring(3, 5).toInt() else Calendar.getInstance().get(Calendar.MINUTE)
        TimePickerDialog(this, { _, j, m -> target.setText(String.format(Locale.US, "%02d:%02d", j, m)); target.error = null }, jam, mnt, true).show()
    }

    private fun tampilkanPemilihTanggal(target: EditText) {
        val cal = Calendar.getInstance()
        if (validasiTanggal(target.text.toString().trim())) try { cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(target.text.toString().trim())!! } catch (_: Exception) {}
        DatePickerDialog(this, { _, t, b, h -> target.setText(String.format(Locale.US, "%04d-%02d-%02d", t, b + 1, h)); target.error = null }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun tambahKegiatan(nama: String, mulai: String, selesai: String, tanggal: String) {
        tampilkanLoading(true, "Menyimpan kegiatan...")
        repository.tambahKegiatanHarian(session, nama, mulai, selesai, tanggal) { response, error ->
            if (isFinishing || isDestroyed) { tampilkanLoading(false); return@tambahKegiatanHarian }
            tampilkanLoading(false)
            if (error != null || response?.success != true) { tampilkanPesan(error ?: "Gagal menyimpan."); return@tambahKegiatanHarian }
            tampilkanPesan(response.message.ifBlank { "Berhasil ditambahkan." })
            muatKegiatanHarian()
        }
    }

    private fun updateKegiatan(id: Int, nama: String, mulai: String, selesai: String, tanggal: String) {
        tampilkanLoading(true, "Memperbarui kegiatan...")
        repository.updateKegiatanHarian(session, id, nama, mulai, selesai, tanggal) { response, error ->
            if (isFinishing || isDestroyed) { tampilkanLoading(false); return@updateKegiatanHarian }
            tampilkanLoading(false)
            if (error != null || response?.success != true) { tampilkanPesan(error ?: "Gagal memperbarui."); return@updateKegiatanHarian }
            tampilkanPesan(response.message.ifBlank { "Berhasil diperbarui." })
            muatKegiatanHarian()
        }
    }

    private fun konfirmasiHapus(kegiatan: KegiatanHarian) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Kegiatan?")
            .setMessage("Kegiatan \"${kegiatan.namaKegiatan}\" akan dihapus. Tidak dapat dibatalkan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(roundedBackground(Color.WHITE, 20))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply { setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply { setTextColor(warnaMerah); setTypeface(null, Typeface.BOLD); setOnClickListener { dialog.dismiss(); hapusKegiatan(kegiatan) } }
        }
        dialog.show()
    }

    private fun hapusKegiatan(kegiatan: KegiatanHarian) {
        tampilkanLoading(true, "Menghapus kegiatan...")
        repository.hapusKegiatanHarian(session, kegiatan.id) { berhasil, error ->
            if (isFinishing || isDestroyed) { tampilkanLoading(false); return@hapusKegiatanHarian }
            tampilkanLoading(false)
            if (!berhasil) { tampilkanPesan(error ?: "Gagal menghapus."); return@hapusKegiatanHarian }
            tampilkanPesan("Berhasil dihapus.")
            muatKegiatanHarian()
        }
    }

    private fun tampilkanLoading(tampilkan: Boolean, pesan: String = "Memproses...") {
        if (tampilkan) {
            if (dialogLoading?.isShowing == true) return
            val teks = TextView(this).apply { text = pesan; textSize = 15f; setTextColor(Color.BLACK); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(24), dp(22), dp(24), dp(22)) }
            dialogLoading = AlertDialog.Builder(this).setView(teks).setCancelable(false).create()
            dialogLoading?.show()
            dialogLoading?.window?.setBackgroundDrawable(roundedBackground(Color.WHITE, 16))
        } else {
            dialogLoading?.dismiss(); dialogLoading = null
        }
    }

    private fun tampilkanPesan(pesan: String) { if (!isFinishing && !isDestroyed) Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show() }
    private fun roundedBackground(warna: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply { setColor(warna); cornerRadius = dp(radiusDp).toFloat() }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        dialogForm?.setOnDismissListener(null); dialogForm?.dismiss(); dialogForm = null
        dialogLoading?.dismiss(); dialogLoading = null
        binding.rvKegiatanHarian.adapter = null
        super.onDestroy()
    }

    private inner class KegiatanHarianAdapter(
        private val items: List<KegiatanHarian>,
        private val onEdit: (KegiatanHarian) -> Unit,
        private val onDelete: (KegiatanHarian) -> Unit
    ) : RecyclerView.Adapter<KegiatanHarianAdapter.KegiatanViewHolder>() {

        inner class KegiatanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nama: TextView = itemView.findViewById(R.id.tvUserName)
            val username: TextView = itemView.findViewById(R.id.tvUserUsername)
            val status: TextView = itemView.findViewById(R.id.tvUserRole)
            val tanggal: TextView = itemView.findViewById(R.id.tvKegiatanTanggal)
            val jam: TextView = itemView.findViewById(R.id.tvKegiatanJam)
            val lokasi: TextView = itemView.findViewById(R.id.tvKegiatanLokasi)
            val tombolEdit: TextView = itemView.findViewById(R.id.btnEditUser)
            val tombolHapus: TextView = itemView.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KegiatanViewHolder = KegiatanViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))

        override fun onBindViewHolder(holder: KegiatanViewHolder, position: Int) {
            val kegiatan = items[position]
            holder.nama.text = kegiatan.namaKegiatan
            holder.username.text = "Kegiatan Harian"

            holder.status.text = "TERCATAT"
            holder.status.setTextColor(Color.parseColor("#C2410C"))
            holder.status.background = roundedBackground(Color.parseColor("#FFF7ED"), 6)

            holder.tanggal.text = "Tanggal: ${formatTanggal(kegiatan.tanggal)}"
            holder.tanggal.visibility = View.VISIBLE

            holder.jam.text = "Waktu: ${kegiatan.jamMulai.take(5)} - ${kegiatan.jamSelesai.take(5)}"
            holder.jam.visibility = View.VISIBLE

            holder.lokasi.visibility = View.GONE

            holder.tombolEdit.text = "Edit"
            holder.tombolEdit.visibility = View.VISIBLE
            holder.tombolEdit.setTextColor(warnaOranye)
            holder.tombolEdit.setOnClickListener { onEdit(kegiatan) }

            holder.tombolHapus.text = "Hapus"
            holder.tombolHapus.visibility = View.VISIBLE
            holder.tombolHapus.setTextColor(warnaMerah)
            holder.tombolHapus.setOnClickListener { onDelete(kegiatan) }

            holder.itemView.setOnClickListener { onEdit(kegiatan) }

            holder.itemView.animate().cancel()
            holder.itemView.alpha = 0f
            holder.itemView.translationY = dp(20).toFloat()
            holder.itemView.scaleX = 0.96f
            holder.itemView.scaleY = 0.96f
            holder.itemView.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(1.1f)).setStartDelay((position % 6) * 35L).start()
        }

        override fun getItemCount(): Int = items.size
    }

    private fun formatTanggal(tanggal: String): String = try { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(tanggal)!!) } catch (_: Exception) { tanggal }
}