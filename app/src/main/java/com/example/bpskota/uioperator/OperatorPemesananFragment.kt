package com.example.bpskota.uioperator

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.Ruangan
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.model.PemesananRuangan
import com.example.bpskota.model.PemesananRuanganRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OperatorPemesananFragment : Fragment() {

    companion object {
        private const val ORANGE = "#F97316"
        private const val BACKGROUND = "#F6F2EE"
        private const val WHITE = "#FFFFFF"
        private const val TEXT_DARK = "#1F2937"
        private const val TEXT_GRAY = "#6B7280"
        private const val BORDER = "#E5E7EB"
        private const val RED = "#DC2626"

        private const val DROPDOWN_BG = "#FAFAF9"
        private const val DROPDOWN_SELECTED_BG = "#FFF7ED"
        private const val DROPDOWN_SELECTED_TEXT = "#EA580C"
    }

    private val repository by lazy {
        BpskpRepository()
    }

    private val authSession by lazy {
        BpskpAuthSession(requireContext())
    }

    private lateinit var rootLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotal: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilterTanggal: Button
    private lateinit var btnTambah: Button
    private lateinit var btnResetFilter: Button

    private lateinit var adapter: PemesananRuanganAdapter

    private val daftarPemesanan = mutableListOf<PemesananRuangan>()
    private val daftarRuanganAktif = mutableListOf<Ruangan>()

    private var tanggalFilter: String? = null
    private var pencarian: String? = null

    private val localeIndonesia = Locale("id", "ID")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        buatTampilan()
        return rootLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PemesananRuanganAdapter(
            onDetail = { data -> tampilkanDetail(data) },
            onEdit = { data -> tampilkanForm(data) },
            onHapus = { data -> konfirmasiHapus(data) },
            onMenu = { data -> tampilkanMenuAksi(data) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        pasangListener()
        muatPemesanan()
        muatRuanganAktif()
    }

    // =========================================================
    // TAMPILAN UTAMA
    // =========================================================

    private fun buatTampilan() {
        rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(BACKGROUND))
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }

        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvJudul = TextView(requireContext()).apply {
            text = "Pemesanan Ruangan"
            textSize = 22f
            setTextColor(Color.parseColor(TEXT_DARK))
            setTypeface(null, Typeface.BOLD)
        }

        val tvDeskripsi = TextView(requireContext()).apply {
            text = "Kelola jadwal penggunaan ruangan kantor"
            textSize = 13f
            setTextColor(Color.parseColor(TEXT_GRAY))
            setPadding(0, dp(4), 0, dp(14))
        }

        btnTambah = Button(requireContext()).apply {
            text = "+ Tambah Pemesanan"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBackground(ORANGE, 12)
        }

        header.addView(tvJudul)
        header.addView(tvDeskripsi)
        header.addView(
            btnTambah,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        rootLayout.addView(header)

        etSearch = EditText(requireContext()).apply {
            hint = "Cari kegiatan, pemesan, atau ruangan..."
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.parseColor(TEXT_DARK))
            setHintTextColor(Color.parseColor(TEXT_GRAY))
            setPadding(dp(14), 0, dp(14), 0)
            background = roundedBackground(WHITE, 12, BORDER)
        }

        rootLayout.addView(
            etSearch,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(14)
            }
        )

        val filterRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        btnFilterTanggal = Button(requireContext()).apply {
            text = "Pilih tanggal"
            isAllCaps = false
            setTextColor(Color.parseColor(ORANGE))
            background = roundedBackground(WHITE, 10, BORDER)
        }

        btnResetFilter = Button(requireContext()).apply {
            text = "Reset"
            isAllCaps = false
            setTextColor(Color.parseColor(TEXT_GRAY))
            background = roundedBackground(WHITE, 10, BORDER)
        }

        filterRow.addView(
            btnFilterTanggal,
            LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                marginEnd = dp(8)
            }
        )

        filterRow.addView(
            btnResetFilter,
            LinearLayout.LayoutParams(dp(90), dp(42))
        )

        rootLayout.addView(
            filterRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        )

        tvTotal = TextView(requireContext()).apply {
            text = "Daftar pemesanan"
            textSize = 13f
            setTextColor(Color.parseColor(TEXT_GRAY))
        }

        rootLayout.addView(
            tvTotal,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(36)
            ).apply {
                topMargin = dp(12)
            }
        )

        progressBar = ProgressBar(requireContext()).apply {
            visibility = View.GONE
        }

        rootLayout.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(44)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        tvEmpty = TextView(requireContext()).apply {
            text = "Belum ada data pemesanan ruangan."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(TEXT_GRAY))
            visibility = View.GONE
        }

        rootLayout.addView(
            tvEmpty,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(100)
            )
        )

        recyclerView = RecyclerView(requireContext()).apply {
            clipToPadding = false
            setPadding(0, dp(4), 0, dp(8))
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        rootLayout.addView(
            recyclerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun pasangListener() {
        btnTambah.setOnClickListener {
            if (daftarRuanganAktif.isEmpty()) {
                muatRuanganAktif {
                    if (daftarRuanganAktif.isNotEmpty()) {
                        tampilkanForm(null)
                    } else {
                        pesan("Tidak ada ruangan aktif yang dapat dipesan.")
                    }
                }
            } else {
                tampilkanForm(null)
            }
        }

        btnFilterTanggal.setOnClickListener {
            tampilkanPemilihTanggal()
        }

        btnResetFilter.setOnClickListener {
            tanggalFilter = null
            pencarian = null
            etSearch.setText("")
            btnFilterTanggal.text = "Pilih tanggal"
            muatPemesanan()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                pencarian = s?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                muatPemesanan()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    // =========================================================
    // API DAN DATA
    // =========================================================

    private fun tokenAuthorization(): String? {
        val token = authSession.getToken()
        return token
            ?.takeIf { it.isNotBlank() }
            ?.let { "Bearer $it" }
    }

    private fun muatPemesanan() {
        val authorization = tokenAuthorization()

        if (authorization == null) {
            pesan("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        repository.getPemesananRuangan(
            authorization = authorization,
            tanggal = tanggalFilter,
            ruanganId = null,
            status = null,
            search = pencarian
        ) { body, code, errorMsg ->
            if (!isAdded) return@getPemesananRuangan

            requireActivity().runOnUiThread {
                progressBar.visibility = View.GONE

                if (code in 200..299 && body?.success == true) {
                    daftarPemesanan.clear()
                    daftarPemesanan.addAll(body.data.orEmpty())
                    adapter.submitList(daftarPemesanan.toList())

                    tvTotal.text = "Total ${daftarPemesanan.size} pemesanan"
                    tvEmpty.visibility =
                        if (daftarPemesanan.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    tvEmpty.visibility =
                        if (daftarPemesanan.isEmpty()) View.VISIBLE else View.GONE

                    pesan(
                        errorMsg
                            ?: body?.message
                            ?: "Gagal memuat pemesanan ruangan."
                    )
                }
            }
        }
    }

    private fun muatRuanganAktif(onSelesai: (() -> Unit)? = null) {
        val authorization = tokenAuthorization()

        if (authorization == null) {
            pesan("Sesi login tidak ditemukan.")
            onSelesai?.invoke()
            return
        }

        repository.getRuangan(authorization) { body, code, errorMsg ->
            if (!isAdded) return@getRuangan

            requireActivity().runOnUiThread {
                if (code in 200..299 && body?.success == true) {
                    daftarRuanganAktif.clear()
                    daftarRuanganAktif.addAll(
                        body.data.orEmpty().filter {
                            it.status.equals("aktif", ignoreCase = true)
                        }
                    )
                } else {
                    pesan(
                        errorMsg
                            ?: body?.message
                            ?: "Gagal memuat daftar ruangan."
                    )
                }

                onSelesai?.invoke()
            }
        }
    }

    // =========================================================
    // FILTER TANGGAL
    // =========================================================

    private fun tampilkanPemilihTanggal() {
        val calendar = Calendar.getInstance()

        tanggalFilter?.let { tanggal ->
            try {
                val parsed = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(tanggal)

                if (parsed != null) calendar.time = parsed
            } catch (_: Exception) {
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                tanggalFilter = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    day
                )

                btnFilterTanggal.text = formatTanggal(tanggalFilter!!)
                muatPemesanan()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // =========================================================
    // FORM TAMBAH DAN EDIT
    // =========================================================

    private fun tampilkanForm(data: PemesananRuangan?) {
        if (daftarRuanganAktif.isEmpty()) {
            muatRuanganAktif {
                if (daftarRuanganAktif.isNotEmpty()) {
                    tampilkanForm(data)
                } else {
                    pesan("Tidak ada ruangan aktif.")
                }
            }
            return
        }

        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(8))
        }

        val scrollView = ScrollView(context).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(4))
            addView(container)
        }

        fun buatLabel(teks: String) {
            container.addView(TextView(context).apply {
                text = teks.uppercase(Locale.getDefault())
                textSize = 11f
                letterSpacing = 0.04f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(TEXT_GRAY))
                setPadding(dp(2), dp(10), 0, dp(5))
            })
        }

        fun buatInput(
            hintText: String,
            value: String = ""
        ): EditText {
            return EditText(context).apply {
                hint = hintText
                setText(value)
                textSize = 14f
                setTextColor(Color.parseColor(TEXT_DARK))
                setHintTextColor(Color.parseColor("#A8A29E"))
                setPadding(dp(12), dp(10), dp(12), dp(10))

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(12).toFloat()
                    setColor(Color.parseColor("#FAFAF9"))
                    setStroke(dp(1), Color.parseColor(BORDER))
                }
            }
        }

        buatLabel("Ruangan")

        // Dropdown utama: gaya field dengan sudut membulat.
        val spinnerRuangan = Spinner(
            context,
            Spinner.MODE_DROPDOWN
        ).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor(DROPDOWN_BG))
                setStroke(dp(1), Color.parseColor(BORDER))
            }

            setPadding(dp(10), 0, dp(10), 0)
            setPopupBackgroundDrawable(
                roundedBackground(WHITE, 14, BORDER)
            )
            dropDownVerticalOffset = dp(4)
        }

        val namaRuangan = daftarRuanganAktif.map {
            buildString {
                append(it.nama_ruangan)

                it.lokasi
                    ?.takeIf { lokasi -> lokasi.isNotBlank() }
                    ?.let { lokasi ->
                        append(" — ")
                        append(lokasi)
                    }
            }
        }

        // Adapter ini hanya mengubah tampilan teks.
        // Sumber data dan urutan item tetap namaRuangan yang sama.
        val adapterRuangan = object : ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            namaRuangan
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return buatTampilanItemRuangan(
                    teks = getItem(position).orEmpty(),
                    convertView = convertView,
                    dropdown = false,
                    terpilih = false
                )
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return buatTampilanItemRuangan(
                    teks = getItem(position).orEmpty(),
                    convertView = convertView,
                    dropdown = true,
                    terpilih = position == spinnerRuangan.selectedItemPosition
                )
            }
        }

        spinnerRuangan.adapter = adapterRuangan

        if (data?.ruangan_id != null) {
            val index = daftarRuanganAktif.indexOfFirst {
                it.id == data.ruangan_id
            }

            if (index >= 0) {
                spinnerRuangan.setSelection(index)
            }
        }

        container.addView(
            spinnerRuangan,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        buatLabel("Nama pemesan")
        val etNamaPemesan = buatInput(
            "Masukkan nama pemesan",
            data?.nama_pemesan.orEmpty()
        )
        container.addView(
            etNamaPemesan,
            inputLayoutParams()
        )

        buatLabel("Instansi")
        val etInstansi = buatInput(
            "Masukkan instansi (opsional)",
            data?.instansi.orEmpty()
        )
        container.addView(etInstansi, inputLayoutParams())

        buatLabel("Judul kegiatan")
        val etJudulKegiatan = buatInput(
            "Masukkan judul kegiatan",
            data?.judul_kegiatan.orEmpty()
        )
        container.addView(etJudulKegiatan, inputLayoutParams())

        buatLabel("Tanggal")
        val etTanggal = buatInput(
            "Pilih tanggal",
            data?.tanggal?.take(10) ?: tanggalHariIni()
        ).apply {
            isFocusable = false
            isClickable = true
        }
        container.addView(etTanggal, inputLayoutParams())

        etTanggal.setOnClickListener {
            val cal = Calendar.getInstance()

            try {
                val current = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(etTanggal.text.toString())

                if (current != null) cal.time = current
            } catch (_: Exception) {
            }

            DatePickerDialog(
                context,
                { _, year, month, day ->
                    etTanggal.setText(
                        String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day
                        )
                    )
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buatLabel("Waktu mulai")
        val etMulai = buatInput(
            "Pilih waktu mulai",
            data?.waktu_mulai?.take(5).orEmpty()
        ).apply {
            isFocusable = false
            isClickable = true
        }
        container.addView(etMulai, inputLayoutParams())

        etMulai.setOnClickListener {
            tampilkanPemilihWaktu(etMulai)
        }

        buatLabel("Waktu selesai")
        val etSelesai = buatInput(
            "Pilih waktu selesai",
            data?.waktu_selesai?.take(5).orEmpty()
        ).apply {
            isFocusable = false
            isClickable = true
        }
        container.addView(etSelesai, inputLayoutParams())

        etSelesai.setOnClickListener {
            tampilkanPemilihWaktu(etSelesai)
        }

        buatLabel("Catatan")
        val etCatatan = buatInput(
            "Catatan tambahan (opsional)",
            data?.catatan.orEmpty()
        ).apply {
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP
        }
        container.addView(
            etCatatan,
            inputLayoutParams().apply {
                height = dp(88)
            }
        )

        val judulPopup = if (data == null) {
            "Tambah Pemesanan"
        } else {
            "Edit Pemesanan"
        }

        val deskripsiPopup = if (data == null) {
            "Lengkapi jadwal dan informasi pemesanan."
        } else {
            "Perbarui informasi pemesanan ruangan."
        }

        val isiPopup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(4), dp(22), dp(16))

            addView(
                scrollView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    minOf(
                        dp(420),
                        (resources.displayMetrics.heightPixels * 0.48f).toInt()
                    )
                )
            )
        }

        val kontenPopup = buatKontainerPopup(
            judul = judulPopup,
            deskripsi = deskripsiPopup,
            isi = isiPopup
        )

        val dialog = AlertDialog.Builder(context)
            .setView(kontenPopup)
            .setNegativeButton("Batal", null)
            .setPositiveButton(
                if (data == null) "Simpan" else "Perbarui",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(Color.parseColor(ORANGE))
                setTypeface(null, Typeface.BOLD)

                setOnClickListener {
                    val namaPemesan =
                        etNamaPemesan.text.toString().trim()
                    val instansi =
                        etInstansi.text.toString().trim()
                    val judul =
                        etJudulKegiatan.text.toString().trim()
                    val tanggal =
                        etTanggal.text.toString().trim()
                    val mulai =
                        etMulai.text.toString().trim()
                    val selesai =
                        etSelesai.text.toString().trim()
                    val catatan =
                        etCatatan.text.toString().trim()

                    when {
                        namaPemesan.isBlank() ->
                            pesan("Nama pemesan wajib diisi.")

                        judul.isBlank() ->
                            pesan("Judul kegiatan wajib diisi.")

                        tanggal.isBlank() ->
                            pesan("Tanggal wajib dipilih.")

                        mulai.isBlank() || selesai.isBlank() ->
                            pesan("Waktu mulai dan selesai wajib dipilih.")

                        !validasiWaktu(mulai, selesai) ->
                            pesan("Waktu selesai harus setelah waktu mulai.")

                        else -> {
                            val posisi = spinnerRuangan.selectedItemPosition

                            if (posisi !in daftarRuanganAktif.indices) {
                                pesan("Silakan pilih ruangan.")
                                return@setOnClickListener
                            }

                            val ruangan = daftarRuanganAktif[posisi]

                            val request = PemesananRuanganRequest(
                                ruangan_id = ruangan.id,
                                nama_pemesan = namaPemesan,
                                instansi = instansi.ifBlank { null },
                                judul_kegiatan = judul,
                                tanggal = tanggal,
                                waktu_mulai = mulai,
                                waktu_selesai = selesai,
                                catatan = catatan.ifBlank { null }
                            )

                            simpanPemesanan(data, request, dialog)
                        }
                    }
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                setTextColor(Color.parseColor(TEXT_GRAY))
            }
        }

        tampilkanDialogBergaya(dialog)
    }

    // =========================================================
    // DESAIN ITEM DROPDOWN RUANGAN
    // =========================================================

    private fun buatTampilanItemRuangan(
        teks: String,
        convertView: View?,
        dropdown: Boolean,
        terpilih: Boolean
    ): TextView {
        val textView = (convertView as? TextView)
            ?: TextView(requireContext())

        textView.apply {
            text = teks
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(if (dropdown) 16 else 12),
                dp(if (dropdown) 12 else 8),
                dp(12),
                dp(if (dropdown) 12 else 8)
            )
            setTextColor(
                Color.parseColor(
                    if (dropdown && terpilih) {
                        DROPDOWN_SELECTED_TEXT
                    } else {
                        TEXT_DARK
                    }
                )
            )
            setTypeface(
                null,
                if (dropdown && terpilih) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )

            if (dropdown) {
                minHeight = dp(48)

                background = if (terpilih) {
                    roundedBackground(
                        DROPDOWN_SELECTED_BG,
                        8
                    )
                } else {
                    roundedBackground(WHITE, 8)
                }

                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            } else {
                minHeight = dp(48)
                background = null
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }

        return textView
    }

    private fun inputLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        ).apply {
            bottomMargin = dp(2)
        }
    }

    private fun simpanPemesanan(
        data: PemesananRuangan?,
        request: PemesananRuanganRequest,
        dialog: AlertDialog
    ) {
        val authorization = tokenAuthorization() ?: run {
            pesan("Sesi login tidak ditemukan.")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        if (data == null) {
            repository.tambahPemesananRuangan(
                authorization,
                request
            ) { body, code, errorMsg ->
                if (!isAdded) return@tambahPemesananRuangan

                requireActivity().runOnUiThread {
                    if (code in 200..299 && body?.success == true) {
                        dialog.dismiss()
                        pesan(body.message ?: "Pemesanan berhasil ditambahkan.")
                        muatPemesanan()
                    } else {
                        dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled = true

                        pesan(
                            errorMsg
                                ?: body?.message
                                ?: "Pemesanan gagal disimpan."
                        )
                    }
                }
            }
        } else {
            val id = data.id

            if (id == null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                pesan("ID pemesanan tidak ditemukan.")
                return
            }

            repository.updatePemesananRuangan(
                authorization,
                id,
                request
            ) { body, code, errorMsg ->
                if (!isAdded) return@updatePemesananRuangan

                requireActivity().runOnUiThread {
                    if (code in 200..299 && body?.success == true) {
                        dialog.dismiss()
                        pesan(body.message ?: "Pemesanan berhasil diperbarui.")
                        muatPemesanan()
                    } else {
                        dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled = true

                        pesan(
                            errorMsg
                                ?: body?.message
                                ?: "Pemesanan gagal diperbarui."
                        )
                    }
                }
            }
        }
    }

    // =========================================================
    // DETAIL PEMESANAN
    // =========================================================

    private fun tampilkanDetail(data: PemesananRuangan) {
        val authorization = tokenAuthorization() ?: run {
            pesan("Sesi login tidak ditemukan.")
            return
        }

        val id = data.id ?: return

        progressBar.visibility = View.VISIBLE

        repository.getDetailPemesananRuangan(
            authorization,
            id
        ) { body, code, errorMsg ->
            if (!isAdded) return@getDetailPemesananRuangan

            requireActivity().runOnUiThread {
                progressBar.visibility = View.GONE

                if (code in 200..299 && body?.success == true) {
                    val detail = body.data ?: data

                    val info = """
                        Kegiatan: ${detail.judul_kegiatan.orEmpty()}

                        Ruangan: ${detail.nama_ruangan.orEmpty()}

                        Lokasi: ${detail.lokasi.orEmpty()}

                        Pemesan: ${detail.nama_pemesan.orEmpty()}

                        Instansi: ${detail.instansi ?: "-"}

                        Tanggal: ${detail.tanggal?.let {
                        formatTanggal(it.take(10))
                    } ?: "-"}

                        Waktu: ${detail.waktu_mulai.orEmpty()} - ${detail.waktu_selesai.orEmpty()}

                        Status: ${detail.status.orEmpty()}

                        Catatan: ${detail.catatan ?: "-"}
                    """.trimIndent()

                    val isiDetail = ScrollView(requireContext()).apply {
                        addView(
                            TextView(requireContext()).apply {
                                text = info
                                textSize = 14f
                                setTextColor(Color.parseColor(TEXT_DARK))
                                setLineSpacing(dp(4).toFloat(), 1f)
                                setPadding(
                                    dp(22),
                                    dp(18),
                                    dp(22),
                                    dp(22)
                                )
                            }
                        )
                    }

                    val kontenDetail = buatKontainerPopup(
                        judul = "Detail Pemesanan",
                        deskripsi = "Informasi lengkap penggunaan ruangan.",
                        isi = isiDetail
                    )

                    val dialogDetail = AlertDialog.Builder(requireContext())
                        .setView(kontenDetail)
                        .setPositiveButton("Tutup", null)
                        .setNeutralButton("Edit", null)
                        .create()

                    dialogDetail.setOnShowListener {
                        dialogDetail.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).setTextColor(Color.parseColor(TEXT_GRAY))

                        dialogDetail.getButton(
                            AlertDialog.BUTTON_NEUTRAL
                        ).apply {
                            setTextColor(Color.parseColor(ORANGE))
                            setTypeface(null, Typeface.BOLD)

                            setOnClickListener {
                                dialogDetail.dismiss()
                                tampilkanForm(detail)
                            }
                        }
                    }

                    tampilkanDialogBergaya(dialogDetail)
                } else {
                    pesan(
                        errorMsg
                            ?: body?.message
                            ?: "Gagal memuat detail pemesanan."
                    )
                }
            }
        }
    }

    // =========================================================
    // MENU AKSI
    // =========================================================

    private fun tampilkanMenuAksi(data: PemesananRuangan) {
        val pilihan = arrayOf(
            "Lihat detail",
            "Edit pemesanan",
            "Batalkan pemesanan",
            "Hapus data"
        )

        val isi = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))

            pilihan.forEachIndexed { index, teks ->
                addView(
                    TextView(requireContext()).apply {
                        text = teks
                        textSize = 14f
                        setTextColor(
                            when (index) {
                                2, 3 -> Color.parseColor(RED)
                                else -> Color.parseColor(TEXT_DARK)
                            }
                        )
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(14), dp(12), dp(14))
                        isClickable = true
                        isFocusable = true

                        setOnClickListener {
                            when (index) {
                                0 -> tampilkanDetail(data)
                                1 -> tampilkanForm(data)
                                2 -> konfirmasiBatal(data)
                                3 -> konfirmasiHapus(data)
                            }
                        }
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )

                if (index < pilihan.lastIndex) {
                    addView(
                        View(requireContext()).apply {
                            setBackgroundColor(Color.parseColor(BORDER))
                        },
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(1)
                        )
                    )
                }
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(
                buatKontainerPopup(
                    judul = "Aksi Pemesanan",
                    deskripsi = "Pilih tindakan untuk data ini.",
                    isi = isi,
                    label = "MENU PEMESANAN"
                )
            )
            .setNegativeButton("Tutup", null)
            .create()

        tampilkanDialogBergaya(dialog, 0.90f)
    }

    // =========================================================
    // BATALKAN PEMESANAN
    // =========================================================

    private fun konfirmasiBatal(data: PemesananRuangan) {
        if (data.status.equals("dibatalkan", true)) {
            pesan("Pemesanan ini sudah dibatalkan.")
            return
        }

        val isi = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(22))

            addView(TextView(requireContext()).apply {
                text = "Pemesanan \"${data.judul_kegiatan.orEmpty()}\" akan dibatalkan."
                textSize = 14f
                setTextColor(Color.parseColor(TEXT_DARK))
            })

            addView(TextView(requireContext()).apply {
                text = "Jadwal ini akan berstatus dibatalkan."
                textSize = 12f
                setTextColor(Color.parseColor(RED))
                setPadding(0, dp(12), 0, 0)
            })
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(
                buatKontainerPopup(
                    judul = "Batalkan Pemesanan?",
                    deskripsi = "Konfirmasi perubahan status pemesanan.",
                    isi = isi,
                    label = "KONFIRMASI"
                )
            )
            .setNegativeButton("Kembali", null)
            .setPositiveButton("Ya, Batalkan", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.parseColor(TEXT_GRAY))

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(Color.parseColor(RED))
                setTypeface(null, Typeface.BOLD)

                setOnClickListener {
                    dialog.dismiss()
                    batalPemesanan(data)
                }
            }
        }

        tampilkanDialogBergaya(
            dialog,
            0.90f,
            tombolHapus = true
        )
    }

    private fun batalPemesanan(data: PemesananRuangan) {
        val authorization = tokenAuthorization() ?: run {
            pesan("Sesi login tidak ditemukan.")
            return
        }

        val id = data.id ?: return

        repository.batalPemesananRuangan(
            authorization,
            id
        ) { body, code, errorMsg ->
            if (!isAdded) return@batalPemesananRuangan

            requireActivity().runOnUiThread {
                if (code in 200..299 && body?.success == true) {
                    pesan(body.message ?: "Pemesanan berhasil dibatalkan.")
                    muatPemesanan()
                } else {
                    pesan(
                        errorMsg
                            ?: body?.message
                            ?: "Gagal membatalkan pemesanan."
                    )
                }
            }
        }
    }

    // =========================================================
    // HAPUS PEMESANAN
    // =========================================================

    private fun konfirmasiHapus(data: PemesananRuangan) {
        val isi = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(22))

            addView(TextView(requireContext()).apply {
                text = "Data \"${data.judul_kegiatan.orEmpty()}\" akan dihapus secara permanen."
                textSize = 14f
                setTextColor(Color.parseColor(TEXT_DARK))
            })

            addView(TextView(requireContext()).apply {
                text = "Tindakan ini tidak dapat dibatalkan."
                textSize = 12f
                setTextColor(Color.parseColor(RED))
                setPadding(0, dp(12), 0, 0)
            })
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(
                buatKontainerPopup(
                    judul = "Hapus Pemesanan?",
                    deskripsi = "Pastikan data yang dipilih sudah benar.",
                    isi = isi,
                    label = "KONFIRMASI HAPUS"
                )
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.parseColor(TEXT_GRAY))

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(Color.parseColor(RED))
                setTypeface(null, Typeface.BOLD)

                setOnClickListener {
                    dialog.dismiss()
                    hapusPemesanan(data)
                }
            }
        }

        tampilkanDialogBergaya(
            dialog,
            0.90f,
            tombolHapus = true
        )
    }

    private fun hapusPemesanan(data: PemesananRuangan) {
        val authorization = tokenAuthorization() ?: run {
            pesan("Sesi login tidak ditemukan.")
            return
        }

        val id = data.id ?: return

        repository.hapusPemesananRuangan(
            authorization,
            id
        ) { body, code, errorMsg ->
            if (!isAdded) return@hapusPemesananRuangan

            requireActivity().runOnUiThread {
                if (code in 200..299 && body?.success == true) {
                    pesan(body.message ?: "Pemesanan berhasil dihapus.")
                    muatPemesanan()
                } else {
                    pesan(
                        errorMsg
                            ?: body?.message
                            ?: "Gagal menghapus pemesanan."
                    )
                }
            }
        }
    }

    // =========================================================
    // KOMPONEN DESAIN POPUP
    // =========================================================

    private fun buatHeaderPopup(
        judul: String,
        deskripsi: String,
        label: String = "PEMESANAN RUANGAN"
    ): View {
        val context = requireContext()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(20))

            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.rgb(255, 247, 237),
                    Color.WHITE
                )
            )

            addView(TextView(context).apply {
                text = label
                textSize = 10f
                letterSpacing = 0.15f
                setTextColor(Color.parseColor(ORANGE))
                setTypeface(null, Typeface.BOLD)
            })

            addView(TextView(context).apply {
                text = judul
                textSize = 21f
                setTextColor(Color.parseColor(TEXT_DARK))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(3))
            })

            addView(TextView(context).apply {
                text = deskripsi
                textSize = 13f
                setTextColor(Color.parseColor(TEXT_GRAY))
            })

            addView(
                View(context).apply {
                    background = roundedBackground(ORANGE, 10)
                },
                LinearLayout.LayoutParams(dp(42), dp(4)).apply {
                    topMargin = dp(15)
                }
            )
        }
    }

    private fun buatKontainerPopup(
        judul: String,
        deskripsi: String,
        isi: View,
        label: String = "PEMESANAN RUANGAN"
    ): LinearLayout {
        val context = requireContext()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(Color.WHITE)
            }

            clipToOutline = true

            addView(
                buatHeaderPopup(judul, deskripsi, label),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                isi,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun tampilkanDialogBergaya(
        dialog: AlertDialog,
        lebarProporsi: Float = 0.92f,
        tombolHapus: Boolean = false
    ) {
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(
                Color.parseColor(
                    if (tombolHapus) RED else ORANGE
                )
            )
            setTypeface(null, Typeface.BOLD)
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(Color.parseColor(TEXT_GRAY))
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            setTextColor(Color.parseColor(ORANGE))
        }

        dialog.window?.apply {
            setBackgroundDrawable(
                roundedBackground(WHITE, 22)
            )

            val lebarLayar = resources.displayMetrics.widthPixels
            val lebarMaksimum = dp(480)
            val lebarDialog = minOf(
                (lebarLayar * lebarProporsi).toInt(),
                lebarLayar - dp(32),
                lebarMaksimum
            )

            setLayout(
                lebarDialog,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.window?.decorView?.apply {
            alpha = 0f
            scaleX = 0.94f
            scaleY = 0.94f
            translationY = dp(12).toFloat()

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    // =========================================================
    // PEMILIH WAKTU DAN FORMAT TANGGAL
    // =========================================================

    private fun tampilkanPemilihWaktu(target: EditText) {
        val calendar = Calendar.getInstance()

        val nilaiSaatIni = target.text.toString().trim()
        if (nilaiSaatIni.matches(Regex("\\d{2}:\\d{2}"))) {
            try {
                val bagian = nilaiSaatIni.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, bagian[0].toInt())
                calendar.set(Calendar.MINUTE, bagian[1].toInt())
            } catch (_: Exception) {
            }
        }

        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                target.setText(
                    String.format(Locale.US, "%02d:%02d", hour, minute)
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validasiWaktu(
        mulai: String,
        selesai: String
    ): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.US).apply {
                isLenient = false
            }

            val waktuMulai = format.parse(mulai)
            val waktuSelesai = format.parse(selesai)

            waktuMulai != null &&
                    waktuSelesai != null &&
                    waktuSelesai.after(waktuMulai)
        } catch (_: Exception) {
            false
        }
    }

    private fun tanggalHariIni(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun formatTanggal(tanggal: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = SimpleDateFormat("dd MMM yyyy", localeIndonesia)
            output.format(input.parse(tanggal)!!)
        } catch (_: Exception) {
            tanggal
        }
    }

    private fun pesan(message: String) {
        if (isAdded) {
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun roundedBackground(
        color: String,
        radiusDp: Int,
        strokeColor: String? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radiusDp).toFloat()

            if (strokeColor != null) {
                setStroke(dp(1), Color.parseColor(strokeColor))
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

// =============================================================
// ADAPTER: TETAP MENGGUNAKAN item_user.xml DAN ID YANG SUDAH ADA
// =============================================================

private class PemesananRuanganAdapter(
    private val onDetail: (PemesananRuangan) -> Unit,
    private val onEdit: (PemesananRuangan) -> Unit,
    private val onHapus: (PemesananRuangan) -> Unit,
    private val onMenu: (PemesananRuangan) -> Unit
) : RecyclerView.Adapter<PemesananRuanganAdapter.ViewHolder>() {

    private val items = mutableListOf<PemesananRuangan>()

    fun submitList(data: List<PemesananRuangan>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvNama =
            itemView.findViewById<TextView>(R.id.tvUserName)

        private val tvUsername =
            itemView.findViewById<TextView>(R.id.tvUserUsername)

        private val tvStatus =
            itemView.findViewById<TextView>(R.id.tvUserRole)

        private val tvTanggal =
            itemView.findViewById<TextView>(R.id.tvKegiatanTanggal)

        private val tvJam =
            itemView.findViewById<TextView>(R.id.tvKegiatanJam)

        private val tvLokasi =
            itemView.findViewById<TextView>(R.id.tvKegiatanLokasi)

        private val btnEdit =
            itemView.findViewById<TextView>(R.id.btnEditUser)

        private val btnHapus =
            itemView.findViewById<TextView>(R.id.btnDeleteUser)

        private val tvMenu: TextView? = cariTitikTiga(itemView)

        fun bind(data: PemesananRuangan) {
            tvNama.text =
                data.judul_kegiatan ?: "Tanpa judul kegiatan"

            tvUsername.text = buildString {
                append(data.nama_pemesan ?: "Pemesan tidak diketahui")

                data.instansi
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        append(" • ")
                        append(it)
                    }
            }

            tvStatus.text = (data.status ?: "dipesan")
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale.getDefault())
                    } else {
                        it.toString()
                    }
                }
                .uppercase(Locale.getDefault())

            tvTanggal.text = data.tanggal?.take(10) ?: "-"

            tvJam.text =
                "${data.waktu_mulai ?: "--:--"} - ${data.waktu_selesai ?: "--:--"}"

            tvLokasi.text = buildString {
                append(data.nama_ruangan ?: "Ruangan tidak diketahui")

                data.lokasi
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        append(", ")
                        append(it)
                    }
            }

            btnEdit.text = "Edit"
            btnHapus.text = "Hapus"

            itemView.setOnClickListener {
                onDetail(data)
            }

            btnEdit.setOnClickListener {
                onEdit(data)
            }

            btnHapus.setOnClickListener {
                onHapus(data)
            }

            tvMenu?.setOnClickListener {
                onMenu(data)
            }

            val status = data.status.orEmpty().lowercase(Locale.ROOT)

            val warnaStatus = when (status) {
                "digunakan" -> "#DBEAFE"
                "selesai" -> "#DCFCE7"
                "dibatalkan" -> "#FEE2E2"
                else -> "#FFF7ED"
            }

            val warnaTeks = when (status) {
                "digunakan" -> "#1D4ED8"
                "selesai" -> "#15803D"
                "dibatalkan" -> "#B91C1C"
                else -> "#E65100"
            }

            tvStatus.background = GradientDrawable().apply {
                setColor(Color.parseColor(warnaStatus))
                cornerRadius = 12f
            }

            tvStatus.setTextColor(Color.parseColor(warnaTeks))
        }

        private fun cariTitikTiga(view: View): TextView? {
            if (
                view is TextView &&
                view.text.toString().trim() == "⋮"
            ) {
                return view
            }

            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    val hasil = cariTitikTiga(view.getChildAt(index))
                    if (hasil != null) return hasil
                }
            }

            return null
        }
    }
}