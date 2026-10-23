
package com.example.bpskota.uioperator

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.Ruangan
import com.example.bpskota.bpskp.model.RuanganRequest
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentOperatorRuanganBinding

class OperatorRuanganFragment : Fragment() {

    private var _binding: FragmentOperatorRuanganBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var ruanganAdapter: RuanganAdapter

    private var sedangMemuat = false
    private var permintaanSedangDiproses = false

    companion object {
        private const val ORANGE = 0xFFF97316.toInt()
        private const val DARK = 0xFF292524.toInt()
        private const val MUTED = 0xFF78716C.toInt()
        private const val BORDER = 0xFFE7E5E4.toInt()
        private const val GREEN = 0xFF15803D.toInt()
        private const val RED = 0xFFDC2626.toInt()
        private const val WHITE = Color.WHITE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorRuanganBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authSession = BpskpAuthSession(requireContext())
        repository = BpskpRepository()

        setupRecyclerView()

        binding.btnTambahRuangan.setOnClickListener {
            tampilkanFormRuangan()
        }

        muatDaftarRuangan()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::repository.isInitialized &&
            _binding != null &&
            !sedangMemuat
        ) {
            muatDaftarRuangan()
        }
    }

    // =========================================================
    // DAFTAR RUANGAN
    // =========================================================

    private fun setupRecyclerView() {
        ruanganAdapter = RuanganAdapter(
            onEdit = { ruangan ->
                tampilkanFormRuangan(ruangan)
            },
            onDelete = { ruangan ->
                konfirmasiHapus(ruangan)
            }
        )

        binding.rvDaftarRuangan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ruanganAdapter
            setHasFixedSize(false)

            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 220
                removeDuration = 180
                moveDuration = 200
                changeDuration = 180
            }
        }
    }

    private fun muatDaftarRuangan() {
        if (sedangMemuat || _binding == null) return

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            tampilkanPesan(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        sedangMemuat = true
        binding.progressRuangan.visibility = View.VISIBLE
        binding.tvRuanganKosong.visibility = View.GONE

        repository.getRuangan("Bearer $token") { response, _, error ->
            if (_binding == null) return@getRuangan

            sedangMemuat = false
            binding.progressRuangan.visibility = View.GONE

            if (response?.success == true) {
                val data = response.data.orEmpty()
                ruanganAdapter.submitList(data)

                binding.tvRuanganKosong.visibility =
                    if (data.isEmpty()) View.VISIBLE else View.GONE
            } else {
                tampilkanPesan(
                    error ?: response?.message
                    ?: "Gagal memuat daftar ruangan."
                )

                binding.tvRuanganKosong.visibility =
                    if (ruanganAdapter.itemCount == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }
    }

    // =========================================================
    // FORM TAMBAH / EDIT RUANGAN
    // =========================================================

    private fun tampilkanFormRuangan(ruangan: Ruangan? = null) {
        val context = requireContext()

        val formContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(10), dp(22), dp(16))
            setBackgroundColor(WHITE)
        }

        fun tambahLabel(teks: String) {
            formContainer.addView(
                TextView(context).apply {
                    text = teks
                    textSize = 11f
                    letterSpacing = 0.06f
                    setTextColor(MUTED)
                    setTypeface(null, Typeface.BOLD)
                    setPadding(dp(2), dp(8), 0, dp(5))
                }
            )
        }

        fun buatInput(
            hintText: String,
            value: String = "",
            inputType: Int = InputType.TYPE_CLASS_TEXT,
            multiline: Boolean = false
        ): EditText {
            val editText = EditText(context).apply {
                hint = hintText
                setText(value)
                this.inputType = inputType
                textSize = 14f
                setTextColor(DARK)
                setHintTextColor(0xFFA8A29E.toInt())
                setSingleLine(!multiline)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = bentukInput()

                if (multiline) {
                    minLines = 2
                    gravity = Gravity.TOP or Gravity.START
                }
            }

            formContainer.addView(
                editText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    if (multiline) dp(78) else dp(48)
                ).apply {
                    bottomMargin = dp(4)
                }
            )

            return editText
        }

        tambahLabel("NAMA RUANGAN *")

        val inputNama = buatInput(
            hintText = "Contoh: Aula",
            value = ruangan?.nama_ruangan.orEmpty()
        )

        tambahLabel("LOKASI")

        val inputLokasi = buatInput(
            hintText = "Lokasi ruangan",
            value = ruangan?.lokasi.orEmpty()
        )

        tambahLabel("KAPASITAS")

        val inputKapasitas = buatInput(
            hintText = "Jumlah orang",
            value = ruangan?.kapasitas?.toString().orEmpty(),
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        tambahLabel("KETERANGAN")

        val inputKeterangan = buatInput(
            hintText = "Informasi tambahan ruangan",
            value = ruangan?.keterangan.orEmpty(),
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            multiline = true
        )

        // -----------------------------------------------------
        // STATUS RUANGAN: PILIHAN CHIP AKTIF / NONAKTIF
        // -----------------------------------------------------

        tambahLabel("STATUS RUANGAN")

        var statusTerpilih = ruangan?.status
            ?.lowercase()
            ?.takeIf { it == "aktif" || it == "nonaktif" }
            ?: "aktif"

        val pilihanStatus = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val chipAktif = TextView(context)
        val chipNonaktif = TextView(context)

        fun aturTampilanChip(
            chip: TextView,
            dipilih: Boolean,
            warnaTeks: Int,
            warnaLatar: Int
        ) {
            chip.apply {
                textSize = 13f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)

                setPadding(
                    dp(18),
                    dp(11),
                    dp(18),
                    dp(11)
                )

                isClickable = true
                isFocusable = true

                setTextColor(
                    if (dipilih) warnaTeks else MUTED
                )

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(24).toFloat()

                    setColor(
                        if (dipilih) {
                            warnaLatar
                        } else {
                            0xFFF5F5F4.toInt()
                        }
                    )

                    setStroke(
                        dp(1),
                        if (dipilih) warnaTeks else BORDER
                    )
                }

                animate()
                    .scaleX(if (dipilih) 1f else 0.97f)
                    .scaleY(if (dipilih) 1f else 0.97f)
                    .setDuration(150)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        fun perbaruiPilihanStatus() {
            aturTampilanChip(
                chipAktif,
                statusTerpilih == "aktif",
                GREEN,
                0xFFDCFCE7.toInt()
            )

            aturTampilanChip(
                chipNonaktif,
                statusTerpilih == "nonaktif",
                RED,
                0xFFFEE2E2.toInt()
            )
        }

        chipAktif.apply {
            text = "● Aktif"

            setOnClickListener {
                if (statusTerpilih != "aktif") {
                    statusTerpilih = "aktif"
                    perbaruiPilihanStatus()
                }
            }
        }

        chipNonaktif.apply {
            text = "● Nonaktif"

            setOnClickListener {
                if (statusTerpilih != "nonaktif") {
                    statusTerpilih = "nonaktif"
                    perbaruiPilihanStatus()
                }
            }
        }

        pilihanStatus.addView(
            chipAktif,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(10)
            }
        )

        pilihanStatus.addView(
            chipNonaktif,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        perbaruiPilihanStatus()

        formContainer.addView(
            pilihanStatus,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(2)
                bottomMargin = dp(8)
            }
        )

        // -----------------------------------------------------
        // SCROLL FORM
        // -----------------------------------------------------

        val tinggiScroll = minOf(
            dp(420),
            (resources.displayMetrics.heightPixels * 0.48f).toInt()
        )

        val scrollView = ScrollView(context).apply {
            isFillViewport = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

            addView(
                formContainer,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val judul = if (ruangan == null) {
            "Tambah Ruangan"
        } else {
            "Edit Ruangan"
        }

        val deskripsi = if (ruangan == null) {
            "Lengkapi informasi ruangan baru."
        } else {
            "Perbarui informasi ruangan yang dipilih."
        }

        val dialogContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = bentukKartu(WHITE, 22)
            clipToOutline = true

            addView(
                buatHeaderDialog(judul, deskripsi),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                scrollView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    tinggiScroll
                )
            )
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogContent)
            .setNegativeButton("Batal", null)
            .setPositiveButton(
                if (ruangan == null) "Simpan" else "Perbarui",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(ORANGE)
                setTypeface(null, Typeface.BOLD)

                setOnClickListener {
                    val nama = inputNama.text.toString().trim()
                    val lokasi = inputLokasi.text.toString().trim()
                    val kapasitasText =
                        inputKapasitas.text.toString().trim()
                    val keterangan =
                        inputKeterangan.text.toString().trim()

                    // Status diambil dari chip yang dipilih.
                    val status = statusTerpilih

                    if (nama.isBlank()) {
                        inputNama.error = "Nama ruangan wajib diisi."
                        inputNama.requestFocus()
                        return@setOnClickListener
                    }

                    val kapasitas = if (kapasitasText.isBlank()) {
                        null
                    } else {
                        kapasitasText.toIntOrNull()
                    }

                    if (
                        kapasitasText.isNotBlank() &&
                        (kapasitas == null || kapasitas < 1)
                    ) {
                        inputKapasitas.error =
                            "Kapasitas harus berupa angka minimal 1."
                        inputKapasitas.requestFocus()
                        return@setOnClickListener
                    }

                    val request = RuanganRequest(
                        nama_ruangan = nama,
                        lokasi = lokasi.ifBlank { null },
                        kapasitas = kapasitas,
                        keterangan = keterangan.ifBlank { null },
                        status = status
                    )

                    simpanRuangan(ruangan, request, dialog)
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                setTextColor(MUTED)
            }
        }

        dialog.show()
        aturTampilanDialog(dialog, 0.92f)
        animasikanDialog(dialog)
    }

    // =========================================================
    // HEADER POPUP
    // =========================================================

    private fun buatHeaderDialog(
        judul: String,
        deskripsi: String
    ): View {
        val context = requireContext()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(20))

            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.rgb(255, 247, 237),
                    WHITE
                )
            )

            addView(
                TextView(context).apply {
                    text = "RUANGAN"
                    textSize = 10f
                    letterSpacing = 0.16f
                    setTextColor(ORANGE)
                    setTypeface(null, Typeface.BOLD)
                }
            )

            addView(
                TextView(context).apply {
                    text = judul
                    textSize = 21f
                    setTextColor(DARK)
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, dp(6), 0, dp(2))
                }
            )

            addView(
                TextView(context).apply {
                    text = deskripsi
                    textSize = 13f
                    setTextColor(MUTED)
                }
            )

            addView(
                View(context).apply {
                    background = bentukKartu(ORANGE, 10)
                },
                LinearLayout.LayoutParams(dp(42), dp(4)).apply {
                    topMargin = dp(15)
                }
            )
        }
    }

    // =========================================================
    // BENTUK INPUT DAN KARTU
    // =========================================================

    private fun bentukInput(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(0xFFFAFAF9.toInt())
            setStroke(dp(1), BORDER)
        }
    }

    private fun bentukKartu(
        warna: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(warna)
        }
    }

    // =========================================================
    // SIMPAN / PERBARUI RUANGAN
    // =========================================================

    private fun simpanRuangan(
        ruangan: Ruangan?,
        request: RuanganRequest,
        dialog: AlertDialog
    ) {
        if (permintaanSedangDiproses) return

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            tampilkanPesan(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        if (ruangan != null && ruangan.id == null) {
            tampilkanPesan("ID ruangan tidak valid.")
            return
        }

        permintaanSedangDiproses = true

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            isEnabled = false
            text = "Memproses..."
        }

        if (ruangan == null) {
            repository.tambahRuangan(
                "Bearer $token",
                request
            ) { response, code, error ->
                if (_binding == null) return@tambahRuangan

                permintaanSedangDiproses = false

                if (response?.success == true) {
                    dialog.dismiss()

                    tampilkanPesan(
                        response.message ?: "Ruangan berhasil ditambahkan."
                    )

                    muatDaftarRuangan()
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        isEnabled = true
                        text = "Simpan"
                    }

                    tampilkanPesan(
                        error ?: response?.message
                        ?: "Gagal menambahkan ruangan. Kode: ${code ?: "-"}"
                    )
                }
            }
        } else {
            repository.updateRuangan(
                "Bearer $token",
                ruangan.id!!,
                request
            ) { response, code, error ->
                if (_binding == null) return@updateRuangan

                permintaanSedangDiproses = false

                if (response?.success == true) {
                    dialog.dismiss()

                    tampilkanPesan(
                        response.message
                            ?: "Data ruangan berhasil diperbarui."
                    )

                    muatDaftarRuangan()
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        isEnabled = true
                        text = "Perbarui"
                    }

                    tampilkanPesan(
                        error ?: response?.message
                        ?: "Gagal memperbarui ruangan. Kode: ${code ?: "-"}"
                    )
                }
            }
        }
    }

    // =========================================================
    // KONFIRMASI HAPUS
    // =========================================================

    private fun konfirmasiHapus(ruangan: Ruangan) {
        if (ruangan.id == null) {
            tampilkanPesan("ID ruangan tidak valid.")
            return
        }

        val context = requireContext()

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(8))
            background = bentukKartu(WHITE, 22)

            addView(
                TextView(context).apply {
                    text = "HAPUS RUANGAN"
                    textSize = 10f
                    letterSpacing = 0.14f
                    setTextColor(RED)
                    setTypeface(null, Typeface.BOLD)
                }
            )

            addView(
                TextView(context).apply {
                    text = "Yakin ingin menghapus?"
                    textSize = 20f
                    setTextColor(DARK)
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, dp(8), 0, dp(8))
                }
            )

            addView(
                TextView(context).apply {
                    text = "Ruangan \"${ruangan.nama_ruangan ?: "-"}\" akan dihapus. Pastikan pilihanmu sudah benar."
                    textSize = 14f
                    setTextColor(MUTED)
                    setLineSpacing(dp(3).toFloat(), 1f)
                }
            )

            addView(
                TextView(context).apply {
                    text = "Tindakan ini tidak dapat dibatalkan."
                    textSize = 12f
                    setTextColor(RED)
                    setPadding(0, dp(12), 0, dp(4))
                }
            )
        }

        val dialog = AlertDialog.Builder(context)
            .setView(content)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(RED)
                setTypeface(null, Typeface.BOLD)

                setOnClickListener {
                    if (permintaanSedangDiproses) {
                        return@setOnClickListener
                    }

                    hapusRuangan(ruangan)
                    dialog.dismiss()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                setTextColor(MUTED)
            }
        }

        dialog.show()
        aturTampilanDialog(dialog, 0.90f)
        animasikanDialog(dialog)
    }

    private fun hapusRuangan(ruangan: Ruangan) {
        if (permintaanSedangDiproses) return

        val id = ruangan.id ?: run {
            tampilkanPesan("ID ruangan tidak valid.")
            return
        }

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            tampilkanPesan(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        permintaanSedangDiproses = true

        repository.hapusRuangan(
            "Bearer $token",
            id
        ) { response, _, error ->
            if (_binding == null) return@hapusRuangan

            permintaanSedangDiproses = false

            if (response?.success == true) {
                tampilkanPesan(
                    response.message ?: "Ruangan berhasil dihapus."
                )
                muatDaftarRuangan()
            } else {
                tampilkanPesan(
                    error ?: response?.message
                    ?: "Gagal menghapus ruangan."
                )
            }
        }
    }

    // =========================================================
    // ANIMASI DAN UKURAN DIALOG
    // =========================================================

    private fun aturTampilanDialog(
        dialog: AlertDialog,
        lebarProporsi: Float
    ) {
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            val lebarLayar = resources.displayMetrics.widthPixels
            val margin = dp(16) * 2
            val lebarMaksimum = dp(480)

            val lebarDialog = minOf(
                (lebarLayar * lebarProporsi).toInt(),
                lebarLayar - margin,
                lebarMaksimum
            )

            setLayout(
                lebarDialog,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun animasikanDialog(dialog: AlertDialog) {
        val decorView = dialog.window?.decorView ?: return

        decorView.alpha = 0f
        decorView.scaleX = 0.94f
        decorView.scaleY = 0.94f
        decorView.translationY = dp(12).toFloat()

        decorView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // =========================================================
    // UTILITAS
    // =========================================================

    private fun tampilkanPesan(pesan: String) {
        val context = context ?: return
        Toast.makeText(context, pesan, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        _binding?.rvDaftarRuangan?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // =========================================================
    // ADAPTER
    // item_user.xml TIDAK DIUBAH
    // =========================================================

    private class RuanganAdapter(
        private val onEdit: (Ruangan) -> Unit,
        private val onDelete: (Ruangan) -> Unit
    ) : RecyclerView.Adapter<RuanganAdapter.RuanganViewHolder>() {

        private val daftarRuangan = mutableListOf<Ruangan>()

        fun submitList(data: List<Ruangan>) {
            daftarRuangan.clear()
            daftarRuangan.addAll(data)
            notifyDataSetChanged()
        }

        inner class RuanganViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {

            private val tvNama =
                itemView.findViewById<TextView>(R.id.tvUserName)

            private val tvLokasi =
                itemView.findViewById<TextView>(R.id.tvUserUsername)

            private val tvStatus =
                itemView.findViewById<TextView>(R.id.tvUserRole)

            private val tvKapasitas =
                itemView.findViewById<TextView>(R.id.tvKegiatanTanggal)

            private val tvKeterangan =
                itemView.findViewById<TextView>(R.id.tvKegiatanJam)

            private val tvLokasiTambahan =
                itemView.findViewById<View>(R.id.tvKegiatanLokasi)

            private val btnEdit =
                itemView.findViewById<View>(R.id.btnEditUser)

            private val btnHapus =
                itemView.findViewById<View>(R.id.btnDeleteUser)

            private var animasiSudahDijalankan = false

            fun bind(ruangan: Ruangan) {
                tvNama.text = ruangan.nama_ruangan
                    ?.takeIf { it.isNotBlank() }
                    ?: "Ruangan tanpa nama"

                tvLokasi.text = ruangan.lokasi
                    ?.takeIf { it.isNotBlank() }
                    ?: "Lokasi belum diisi"

                tvStatus.text = when (ruangan.status?.lowercase()) {
                    "aktif" -> "AKTIF"
                    "nonaktif" -> "NONAKTIF"
                    else -> ruangan.status.orEmpty().uppercase()
                }

                tvStatus.visibility =
                    if (ruangan.status.isNullOrBlank()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                val statusAktif = ruangan.status.equals(
                    "aktif",
                    ignoreCase = true
                )

                tvStatus.setTextColor(
                    if (statusAktif) GREEN else 0xFFB91C1C.toInt()
                )

                tvStatus.backgroundTintList = null
                tvStatus.setBackgroundColor(Color.TRANSPARENT)

                tvKapasitas.text = ruangan.kapasitas?.let {
                    "Kapasitas: $it orang"
                } ?: "Kapasitas belum ditentukan"

                tvKeterangan.text = ruangan.keterangan
                    ?.takeIf { it.isNotBlank() }
                    ?: "Tidak ada keterangan"

                tvLokasiTambahan.visibility = View.GONE

                tvNama.setTextColor(DARK)
                tvLokasi.setTextColor(0xFF57534E.toInt())
                tvKapasitas.setTextColor(0xFF57534E.toInt())
                tvKeterangan.setTextColor(MUTED)

                btnEdit.setOnClickListener {
                    onEdit(ruangan)
                }

                btnHapus.setOnClickListener {
                    onDelete(ruangan)
                }

                if (!animasiSudahDijalankan) {
                    animasiSudahDijalankan = true

                    itemView.alpha = 0f
                    itemView.translationY = dp(12, itemView)

                    itemView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(260)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }

            private fun dp(value: Int, view: View): Float {
                return value * view.resources.displayMetrics.density
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RuanganViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)

            return RuanganViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: RuanganViewHolder,
            position: Int
        ) {
            holder.bind(daftarRuangan[position])
        }

        override fun getItemCount(): Int = daftarRuangan.size
    }
}
