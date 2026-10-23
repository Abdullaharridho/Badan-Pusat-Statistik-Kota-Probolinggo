
package com.example.bpskota.uipimpinan

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.SuratMasuk
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityPimpinanSuratMasukBinding
import java.util.Calendar
import java.util.Locale

class PimpinanSuratMasukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPimpinanSuratMasukBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var suratAdapter: SuratAdapter

    // Semua data surat yang diterima dari API.
    private var daftarSurat = listOf<SuratMasuk>()

    // Filter dan pencarian.
    private var kataKunci = ""
    private var statusTerpilih: String? = null
    private var tanggalTerpilih: String? = null

    private var requestId = 0
    private var dataPernahDimuat = false

    private val pilihanStatus = listOf(
        "Semua Status",
        "Baru",
        "Diproses",
        "Selesai",
        "Dibatalkan"
    )

    private val warnaOrange = Color.parseColor("#F97316")
    private val warnaTeks = Color.parseColor("#292524")
    private val warnaMuted = Color.parseColor("#78716C")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPimpinanSuratMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        setupRecyclerView()
        setupFilterSurat()
        setupPencarian()

        muatSuratMasuk()
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private fun setupRecyclerView() {
        suratAdapter = SuratAdapter()

        binding.rvSuratPimpinan.apply {
            layoutManager = LinearLayoutManager(
                this@PimpinanSuratMasukActivity
            )
            adapter = suratAdapter
            setHasFixedSize(false)
        }
    }

    // =========================================================
    // FILTER STATUS DAN TANGGAL
    // =========================================================

    private fun setupFilterSurat() {
        val adapterStatus = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            pilihanStatus
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return (super.getView(position, convertView, parent) as TextView)
                    .apply {
                        setTextColor(Color.BLACK)
                        textSize = 13f
                        typeface = Typeface.create(
                            "sans-serif-medium",
                            Typeface.NORMAL
                        )
                        setPadding(dp(12), dp(8), dp(8), dp(8))
                    }
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return (super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView).apply {
                    setTextColor(Color.BLACK)
                    textSize = 14f
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setBackgroundColor(
                        if (position == binding.spFilterStatusSuratPimpinan.selectedItemPosition) {
                            Color.parseColor("#FFF0E5")
                        } else {
                            Color.WHITE
                        }
                    )
                }
            }
        }.apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        binding.spFilterStatusSuratPimpinan.apply {
            adapter = adapterStatus
            setPopupBackgroundDrawable(bentuk(Color.WHITE, 16))
        }
        binding.spFilterStatusSuratPimpinan.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    statusTerpilih = when (position) {
                        1 -> "baru"
                        2 -> "diproses"
                        3 -> "selesai"
                        4 -> "dibatalkan"
                        else -> null
                    }

                    if (dataPernahDimuat) {
                        terapkanFilterLokal()
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) = Unit
            }

        binding.btnFilterTanggalSuratPimpinan.setOnClickListener {
            tampilkanPemilihTanggal()
        }

        binding.btnResetFilterSuratPimpinan.setOnClickListener {
            resetFilter()
        }
    }

    private fun tampilkanPemilihTanggal() {
        val kalender = Calendar.getInstance()

        // Jika tanggal sebelumnya sudah dipilih, gunakan tanggal itu.
        tanggalTerpilih?.let { tanggal ->
            val bagian = tanggal.split("-")

            if (bagian.size == 3) {
                val tahun = bagian[0].toIntOrNull()
                val bulan = bagian[1].toIntOrNull()
                val hari = bagian[2].toIntOrNull()

                if (
                    tahun != null &&
                    bulan != null &&
                    hari != null
                ) {
                    kalender.set(
                        tahun,
                        bulan - 1,
                        hari
                    )
                }
            }
        }

        DatePickerDialog(
            this,
            { _, tahun, bulan, hari ->
                // Simpan tanggal dalam format yyyy-MM-dd.
                tanggalTerpilih = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    tahun,
                    bulan + 1,
                    hari
                )

                binding.btnFilterTanggalSuratPimpinan.text =
                    String.format(
                        Locale("id", "ID"),
                        "%02d/%02d/%04d",
                        hari,
                        bulan + 1,
                        tahun
                    )

                if (dataPernahDimuat) {
                    terapkanFilterLokal()
                }
            },
            kalender.get(Calendar.YEAR),
            kalender.get(Calendar.MONTH),
            kalender.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Pilih tanggal surat")
            show()
        }
    }

    private fun resetFilter() {
        tanggalTerpilih = null
        statusTerpilih = null

        binding.btnFilterTanggalSuratPimpinan.text = "Pilih tanggal"

        if (binding.spFilterStatusSuratPimpinan.selectedItemPosition != 0) {
            binding.spFilterStatusSuratPimpinan.setSelection(0)
        }

        if (dataPernahDimuat) {
            terapkanFilterLokal()
        }
    }

    private fun terapkanFilterLokal() {
        val hasilFilter = daftarSurat.filter { surat ->

            val cocokStatus = statusTerpilih == null ||
                    surat.status
                        ?.trim()
                        ?.equals(statusTerpilih, ignoreCase = true) == true

            val tanggalSurat = normalisasiTanggal(surat.tanggal_surat)

            val cocokTanggal = tanggalTerpilih == null ||
                    tanggalSurat == tanggalTerpilih

            cocokStatus && cocokTanggal
        }

        suratAdapter.updateData(hasilFilter)

        // Angka ringkasan tetap menunjukkan total data API.
        binding.tvJumlahSuratPimpinan.text =
            daftarSurat.size.toString()

        // Jumlah hasil mengikuti filter aktif.
        binding.tvLabelJumlahHasilSuratPimpinan.text =
            if (statusTerpilih == null && tanggalTerpilih == null) {
                "${hasilFilter.size} surat ditemukan"
            } else {
                "${hasilFilter.size} dari ${daftarSurat.size} surat"
            }

        binding.tvErrorSuratPimpinan.visibility = View.GONE

        val tidakAdaHasil = hasilFilter.isEmpty()

        binding.layoutKosongSuratPimpinan.visibility =
            if (tidakAdaHasil) View.VISIBLE else View.GONE

        binding.rvSuratPimpinan.visibility =
            if (tidakAdaHasil) View.GONE else View.VISIBLE

        if (tidakAdaHasil) {
            val adaFilterAktif =
                statusTerpilih != null || tanggalTerpilih != null

            val pesanKosong = binding.layoutKosongSuratPimpinan
                .findViewsByText("Belum ada surat masuk")

            pesanKosong?.text = if (adaFilterAktif) {
                "Surat tidak ditemukan"
            } else {
                "Belum ada surat masuk"
            }
        }
    }

    /**
     * Menormalkan tanggal agar bisa dibandingkan dengan tanggal DatePicker.
     * Mendukung yyyy-MM-dd, ISO dengan bagian waktu, dd/MM/yyyy,
     * dan dd-MM-yyyy.
     */
    private fun normalisasiTanggal(tanggal: String?): String? {
        val nilai = tanggal
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val tanggalAwal = nilai.take(10)

        // Format yyyy-MM-dd atau ISO yyyy-MM-ddTHH:mm:ss...
        val polaIso = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        if (polaIso.matches(tanggalAwal)) {
            return tanggalAwal
        }

        // Format dd/MM/yyyy atau dd-MM-yyyy.
        val bagian = tanggalAwal.split("/", "-")

        if (bagian.size == 3) {
            val hari = bagian[0].toIntOrNull()
            val bulan = bagian[1].toIntOrNull()
            val tahun = bagian[2].toIntOrNull()

            if (
                hari != null &&
                bulan != null &&
                tahun != null &&
                tahun >= 1000 &&
                bulan in 1..12 &&
                hari in 1..31
            ) {
                return String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    tahun,
                    bulan,
                    hari
                )
            }
        }

        return null
    }

    // =========================================================
    // PENCARIAN
    // =========================================================

    private fun setupPencarian() {
        binding.etCariSuratPimpinan.addTextChangedListener(
            object : android.text.TextWatcher {

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
                    val keyword = s?.toString()?.trim().orEmpty()

                    if (keyword != kataKunci) {
                        kataKunci = keyword
                        muatSuratMasuk()
                    }
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) = Unit
            }
        )
    }

    // =========================================================
    // MEMUAT DATA API
    // =========================================================

    private fun muatSuratMasuk() {
        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            tampilkanError(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        val requestSekarang = ++requestId

        binding.progressSuratPimpinan.visibility = View.VISIBLE
        binding.tvErrorSuratPimpinan.visibility = View.GONE
        binding.layoutKosongSuratPimpinan.visibility = View.GONE

        repository.getSuratMasuk(
            authorization = "Bearer $token",
            search = kataKunci.ifBlank { null },
            status = null
        ) { data, error ->

            runOnUiThread {
                if (
                    isFinishing ||
                    isDestroyed ||
                    requestSekarang != requestId
                ) {
                    return@runOnUiThread
                }

                binding.progressSuratPimpinan.visibility = View.GONE

                if (error != null) {
                    tampilkanError(error)
                    return@runOnUiThread
                }

                val hasil = data ?: emptyList()

                daftarSurat = hasil
                dataPernahDimuat = true

                binding.tvLabelStatusSuratPimpinan.text =
                    "PEMANTAUAN"

                binding.tvErrorSuratPimpinan.visibility = View.GONE

                terapkanFilterLokal()
            }
        }
    }

    private fun tampilkanError(pesan: String) {
        binding.progressSuratPimpinan.visibility = View.GONE

        binding.tvErrorSuratPimpinan.apply {
            text = pesan
            visibility = View.VISIBLE
        }

        binding.layoutKosongSuratPimpinan.visibility = View.GONE

        if (daftarSurat.isEmpty()) {
            binding.rvSuratPimpinan.visibility = View.GONE
            binding.tvJumlahSuratPimpinan.text = "0"
            binding.tvLabelJumlahHasilSuratPimpinan.text =
                "Data belum tersedia"
        }

        Toast.makeText(
            this,
            pesan,
            Toast.LENGTH_LONG
        ).show()
    }

    // =========================================================
    // DETAIL SURAT
    // =========================================================

    private fun tampilkanDetailSurat(surat: SuratMasuk) {
        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Sesi login tidak ditemukan.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val idSurat = surat.id

        if (idSurat == null) {
            Toast.makeText(
                this,
                "ID surat tidak ditemukan.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val loadingView = TextView(this).apply {
            text = "Memuat detail surat..."
            textSize = 14f
            setTextColor(warnaTeks)
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(28), dp(32), dp(28))
        }

        val dialogLoading = AlertDialog.Builder(this)
            .setView(loadingView)
            .setCancelable(false)
            .create()

        dialogLoading.show()

        repository.getSuratMasukDetail(
            authorization = "Bearer $token",
            id = idSurat
        ) { detail, error ->

            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    dialogLoading.dismiss()
                    return@runOnUiThread
                }

                dialogLoading.dismiss()

                if (error != null) {
                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }

                if (detail != null) {
                    tampilkanDialogDetail(detail)
                } else {
                    Toast.makeText(
                        this,
                        "Detail surat tidak ditemukan.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun tampilkanDialogDetail(surat: SuratMasuk) {
        val kontainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
            background = bentuk(Color.WHITE, 22)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = bentuk(
                Color.parseColor("#FFF0E5"),
                16
            )
        }

        header.addView(TextView(this).apply {
            text = "DETAIL SURAT MASUK"
            textSize = 10f
            letterSpacing = 0.1f
            setTextColor(warnaOrange)
            setTypeface(null, Typeface.BOLD)
        })

        header.addView(TextView(this).apply {
            text = surat.perihal.orEmpty()
                .ifBlank { "Tanpa perihal" }
            textSize = 19f
            setTextColor(warnaTeks)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(7), 0, dp(5))
        })

        header.addView(TextView(this).apply {
            text = surat.nomor_surat.orEmpty()
                .ifBlank { "Nomor surat tidak tersedia" }
            textSize = 13f
            setTextColor(warnaMuted)
        })

        kontainer.addView(header)

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
        }

        val isi = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(4))
        }

        tambahBarisDetail(isi, "ASAL SURAT", surat.asal_surat)
        tambahBarisDetail(isi, "TANGGAL SURAT", surat.tanggal_surat)
        tambahBarisDetail(isi, "TANGGAL DITERIMA", surat.tanggal_diterima)
        tambahBarisDetail(isi, "PERIHAL", surat.perihal)
        tambahBarisDetail(isi, "ISI RINGKAS", surat.isi_ringkas)

        tambahBarisStatusDetail(isi, surat.status)

        tambahBarisDetail(isi, "TANGGAL ACARA", surat.tanggal_acara)

        val waktu = listOfNotNull(
            surat.waktu_mulai
                ?.takeIf { it.isNotBlank() }
                ?.let { formatWaktu(it) },
            surat.waktu_selesai
                ?.takeIf { it.isNotBlank() }
                ?.let { formatWaktu(it) }
        ).joinToString(" - ")

        tambahBarisDetail(isi, "WAKTU ACARA", waktu)
        tambahBarisDetail(isi, "LOKASI", surat.lokasi)

        val namaFile = surat.file_surat
            ?.takeIf { it.isNotBlank() }
            ?.substringAfterLast("/")
            ?.ifBlank { null }

        tambahBarisDetail(
            isi,
            "FILE SURAT",
            namaFile ?: if (!surat.file_url.isNullOrBlank()) {
                surat.file_url
                    .substringAfterLast("/")
                    .ifBlank { "File tersedia" }
            } else {
                "Tidak ada file"
            }
        )

        scrollView.addView(isi)

        kontainer.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(340)
            )
        )

        val dialog = AlertDialog.Builder(this)
            .setView(kontainer)
            .setPositiveButton("Tutup", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(warnaOrange)
            setTypeface(null, Typeface.BOLD)
        }
    }

    private fun tambahBarisDetail(
        parent: LinearLayout,
        label: String,
        nilai: String?
    ) {
        val bagian = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }

        bagian.addView(TextView(this).apply {
            text = label
            textSize = 10f
            letterSpacing = 0.08f
            setTextColor(warnaMuted)
            setTypeface(null, Typeface.BOLD)
        })

        bagian.addView(TextView(this).apply {
            text = nilai?.takeIf { it.isNotBlank() } ?: "—"
            textSize = 14f
            setTextColor(warnaTeks)
            setPadding(0, dp(4), 0, 0)
        })

        parent.addView(bagian)

        parent.addView(
            View(this).apply {
                setBackgroundColor(Color.parseColor("#F5F5F4"))
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )
    }

    private fun tambahBarisStatusDetail(
        parent: LinearLayout,
        status: String?
    ) {
        val bagian = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }

        bagian.addView(TextView(this).apply {
            text = "STATUS"
            textSize = 10f
            letterSpacing = 0.08f
            setTextColor(warnaMuted)
            setTypeface(null, Typeface.BOLD)
        })

        val statusText = TextView(this).apply {
            text = status?.trim()?.ifBlank { null } ?: "Tidak diketahui"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = bentukStatus(status)
            setTextColor(warnaStatus(status).first)
        }

        bagian.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
            }
        )

        parent.addView(bagian)

        parent.addView(
            View(this).apply {
                setBackgroundColor(Color.parseColor("#F5F5F4"))
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )
    }

    // =========================================================
    // STATUS
    // =========================================================

    private fun warnaStatus(status: String?): Pair<Int, Int> {
        return when (status?.trim()?.lowercase(Locale.ROOT)) {
            "baru" ->
                Color.parseColor("#1D4ED8") to
                        Color.parseColor("#DBEAFE")

            "diproses", "dalam proses", "sedang diproses" ->
                Color.parseColor("#C2410C") to
                        Color.parseColor("#FFEDD5")

            "selesai", "diselesaikan" ->
                Color.parseColor("#15803D") to
                        Color.parseColor("#DCFCE7")

            "ditolak", "dibatalkan", "ditolak pimpinan" ->
                Color.parseColor("#B91C1C") to
                        Color.parseColor("#FEE2E2")

            else ->
                Color.parseColor("#57534E") to
                        Color.parseColor("#F5F5F4")
        }
    }

    private fun bentukStatus(status: String?): GradientDrawable {
        val (_, warnaLatar) = warnaStatus(status)

        return GradientDrawable().apply {
            setColor(warnaLatar)
            cornerRadius = dp(50).toFloat()
        }
    }

    private fun formatWaktu(waktu: String): String {
        val nilai = waktu.trim()
        val polaWaktu = Regex("^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$")

        return if (polaWaktu.matches(nilai)) {
            nilai.substring(0, 5)
        } else {
            nilai
        }
    }

    private fun bentuk(
        warna: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(warna)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(nilai: Int): Int {
        return (nilai * resources.displayMetrics.density).toInt()
    }

    // =========================================================
    // ADAPTER SURAT
    // =========================================================

    private inner class SuratAdapter :
        RecyclerView.Adapter<SuratAdapter.SuratViewHolder>() {

        private val items = mutableListOf<SuratMasuk>()

        inner class SuratViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            val nama: TextView =
                view.findViewById(R.id.tvUserName)

            val username: TextView =
                view.findViewById(R.id.tvUserUsername)

            val role: TextView =
                view.findViewById(R.id.tvUserRole)

            val tanggalAcara: TextView =
                view.findViewById(R.id.tvKegiatanTanggal)

            val waktuAcara: TextView =
                view.findViewById(R.id.tvKegiatanJam)

            val lokasiAcara: TextView =
                view.findViewById(R.id.tvKegiatanLokasi)

            init {
                sembunyikanAksi(view)

                view.setOnClickListener {
                    val posisi = adapterPosition

                    if (posisi != RecyclerView.NO_POSITION) {
                        tampilkanDetailSurat(items[posisi])
                    }
                }
            }

            private fun sembunyikanAksi(view: View) {
                if (
                    view is TextView &&
                    view.text.toString().trim() == "⋮"
                ) {
                    view.visibility = View.GONE
                }

                if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        sembunyikanAksi(view.getChildAt(i))
                    }
                }

                if (
                    view.id == R.id.btnEditUser ||
                    view.id == R.id.btnDeleteUser
                ) {
                    view.visibility = View.GONE
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SuratViewHolder {
            val view = layoutInflater.inflate(
                R.layout.item_user,
                parent,
                false
            )

            return SuratViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: SuratViewHolder,
            position: Int
        ) {
            val surat = items[position]

            holder.nama.text = surat.perihal
                .orEmpty()
                .ifBlank { "Tanpa perihal" }

            holder.username.text = buildString {
                append("Nomor: ")
                append(
                    surat.nomor_surat
                        .orEmpty()
                        .ifBlank { "—" }
                )

                append("\nAsal: ")
                append(
                    surat.asal_surat
                        .orEmpty()
                        .ifBlank { "—" }
                )

                append("\nTanggal surat: ")
                append(
                    surat.tanggal_surat
                        .orEmpty()
                        .ifBlank { "—" }
                )
            }
            // Tipografi kartu agar lebih bersih dan modern.
            gayaTeksKartu(
                holder.nama,
                15f,
                "#171717",
                true
            )

            gayaTeksKartu(
                holder.username,
                12f,
                "#57534E"
            )

            gayaTeksKartu(
                holder.tanggalAcara,
                12f,
                "#292524"
            )

            gayaTeksKartu(
                holder.waktuAcara,
                12f,
                "#292524"
            )

            gayaTeksKartu(
                holder.lokasiAcara,
                12f,
                "#292524"
            )

// Status tetap menggunakan warna sesuai kategorinya.
            holder.role.apply {
                textSize = 10f
                typeface = Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
                setPadding(dp(12), dp(6), dp(12), dp(6))
            }

            // Status berbentuk kapsul.
            val status = surat.status?.trim().orEmpty()

            holder.role.apply {
                text = status.ifBlank { "Tidak diketahui" }
                background = bentukStatus(status)
                setTextColor(warnaStatus(status).first)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                visibility = View.VISIBLE
            }

            // Tanggal acara.
            val tanggalAcara = surat.tanggal_acara
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            holder.tanggalAcara.apply {
                text = "Tanggal acara: ${tanggalAcara.orEmpty()}"
                visibility =
                    if (tanggalAcara != null) View.VISIBLE else View.GONE
            }

            // Waktu acara.
            val waktuMulai = surat.waktu_mulai
                ?.takeIf { it.isNotBlank() }
                ?.let { formatWaktu(it) }

            val waktuSelesai = surat.waktu_selesai
                ?.takeIf { it.isNotBlank() }
                ?.let { formatWaktu(it) }

            val waktuAcara = listOfNotNull(
                waktuMulai,
                waktuSelesai
            ).joinToString(" - ")

            holder.waktuAcara.apply {
                text = "Waktu acara: $waktuAcara"
                visibility =
                    if (waktuAcara.isNotBlank()) View.VISIBLE else View.GONE
            }

            // Lokasi acara.
            val lokasi = surat.lokasi
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            holder.lokasiAcara.apply {
                text = "Lokasi: ${lokasi.orEmpty()}"
                visibility =
                    if (lokasi != null) View.VISIBLE else View.GONE
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateData(data: List<SuratMasuk>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }

    // Helper untuk mencari TextView berdasarkan teks.
    private fun ViewGroup.findViewsByText(
        teks: String
    ): TextView? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (child is TextView && child.text.toString() == teks) {
                return child
            }

            if (child is ViewGroup) {
                val ditemukan = child.findViewsByText(teks)

                if (ditemukan != null) {
                    return ditemukan
                }
            }
        }

        return null
    }
    private fun gayaTeksKartu(
        textView: TextView,
        ukuran: Float,
        warna: String,
        tebal: Boolean = false
    ) {
        textView.textSize = ukuran
        textView.setTextColor(Color.parseColor(warna))
        textView.typeface = Typeface.create(
            "sans-serif",
            if (tebal) Typeface.BOLD else Typeface.NORMAL
        )
    }
}
