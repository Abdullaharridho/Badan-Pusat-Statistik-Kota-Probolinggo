package com.example.bpskota

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.MasterWilayah
import com.example.bpskota.bpskp.model.MasterWilayahResponse
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MasterwilayahActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MasterWilayahAPI"
        private const val ORANGE = "#F97316"
        private const val TEXT_SECONDARY = "#78716C"

        private const val LEVEL_KECAMATAN = "kecamatan"
        private const val LEVEL_KELURAHAN = "kelurahan"
        private const val LEVEL_RTRW = "rtrw"

        private const val PER_PAGE = 20
    }

    // Data class untuk menampung item Spinner
    data class SpinnerItem(val id: Int?, val name: String) {
        override fun toString(): String = name
    }

    private lateinit var tvTotalKecamatan: TextView
    private lateinit var tvLabelTotalWilayah: TextView
    private lateinit var listDataUtama: LinearLayout
    private lateinit var activityTracker: ActivityTracker

    private lateinit var etCariWilayah: EditText
    private lateinit var spinnerKecamatan: Spinner
    private lateinit var spinnerKelurahan: Spinner

    private lateinit var tvLabelDaftar: TextView
    private lateinit var tvDeskripsiWilayah: TextView

    private lateinit var layoutEmptyWilayah: View
    private lateinit var layoutPaginationWilayah: View
    private lateinit var tvInfoPagination: TextView
    private lateinit var btnHalamanSebelumnya: View
    private lateinit var btnHalamanBerikutnya: View
    private lateinit var btnResetCari: View

    private var currentPage = 1
    private var lastPage = 1
    private var totalData = 0
    private var searchQuery = ""

    private var activeCall: Call<MasterWilayahResponse>? = null
    private var isInitializingSpinners = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_masterwilayah)

        activityTracker = ActivityTracker(this, BpskpRetrofitClient.api)
        activityTracker.trackScreen(screen = "Masterwilayah")

        bindViews()
        setupListeners()
        setupInitialSpinners()

        // Muat data awal untuk Spinner Kecamatan
        loadKecamatanForSpinner()

        // Muat data awal untuk List (Semua Kecamatan)
        loadMainData(resetPage = true)
    }

    private fun bindViews() {
        tvTotalKecamatan = findViewById(R.id.tvTotalKecamatan)
        tvLabelTotalWilayah = findViewById(R.id.tvLabelTotalWilayah)
        listDataUtama = findViewById(R.id.listKecamatan) // Re-use ID listKecamatan

        etCariWilayah = findViewById(R.id.etCariWilayah)
        btnResetCari = findViewById(R.id.btnResetCari)

        // Kita daur ulang ID XML yang ada untuk filter bertingkat
        spinnerKecamatan = findViewById(R.id.spinnerStatusWilayah)
        spinnerKelurahan = findViewById(R.id.spinnerSortWilayah)

        tvLabelDaftar = findViewById(R.id.tvLabelKecamatan)
        tvDeskripsiWilayah = findViewById(R.id.tvDeskripsiWilayah)

        layoutEmptyWilayah = findViewById(R.id.layoutEmptyWilayah)
        layoutPaginationWilayah = findViewById(R.id.layoutPaginationWilayah)
        tvInfoPagination = findViewById(R.id.tvInfoPagination)
        btnHalamanSebelumnya = findViewById(R.id.btnHalamanSebelumnya)
        btnHalamanBerikutnya = findViewById(R.id.btnHalamanBerikutnya)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnResetCari.setOnClickListener { etCariWilayah.setText("") }

        btnHalamanSebelumnya.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadMainData(resetPage = false)
            }
        }

        btnHalamanBerikutnya.setOnClickListener {
            if (currentPage < lastPage) {
                currentPage++
                loadMainData(resetPage = false)
            }
        }

        etCariWilayah.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                loadMainData(resetPage = true)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Listener Spinner Kecamatan
        spinnerKecamatan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializingSpinners) return

                val selected = parent?.getItemAtPosition(position) as? SpinnerItem
                if (selected?.id == null) {
                    // "Semua Kecamatan" dipilih -> Kunci/Kosongkan spinner Kelurahan
                    resetKelurahanSpinner()
                } else {
                    // Kecamatan tertentu dipilih -> Muat Kelurahan untuk dropdown
                    loadKelurahanForSpinner(selected.id)
                }

                loadMainData(resetPage = true)
            }
        }

        // Listener Spinner Kelurahan
        spinnerKelurahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializingSpinners) return
                loadMainData(resetPage = true)
            }
        }
    }

    /**
     * CUSTOM ADAPTER UNTUK SPINNER (Desain teks hitam, background putih)
     */
    private fun getCustomSpinnerAdapter(items: List<SpinnerItem>): ArrayAdapter<SpinnerItem> {
        return object : ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, items) {

            // Tampilan teks yang terpilih di dalam kotak spinner
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#111827")) // Teks hitam tegas
                view.setTypeface(null, Typeface.BOLD)
                view.textSize = 13f
                return view
            }

            // Tampilan daftar list saat dropdown terbuka
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#111827")) // Teks hitam
                view.setBackgroundColor(Color.parseColor("#FFFFFF")) // Latar belakang putih
                view.setPadding(dp(16), dp(12), dp(16), dp(12)) // Jarak lega agar rapi
                view.textSize = 14f
                return view
            }
        }
    }

    private fun setupInitialSpinners() {
        isInitializingSpinners = true
        val defaultKec = listOf(SpinnerItem(null, "Semua Kecamatan"))
        val defaultKel = listOf(SpinnerItem(null, "Semua Kelurahan"))

        spinnerKecamatan.adapter = getCustomSpinnerAdapter(defaultKec)
        spinnerKelurahan.adapter = getCustomSpinnerAdapter(defaultKel)

        spinnerKelurahan.isEnabled = false // Disabled secara default
        spinnerKelurahan.alpha = 0.5f
        isInitializingSpinners = false
    }

    private fun resetKelurahanSpinner() {
        isInitializingSpinners = true
        val defaultKel = listOf(SpinnerItem(null, "Semua Kelurahan"))

        spinnerKelurahan.adapter = getCustomSpinnerAdapter(defaultKel)
        spinnerKelurahan.isEnabled = false
        spinnerKelurahan.alpha = 0.5f
        isInitializingSpinners = false
    }

    // --- LOGIKA FETCH DATA DROPDOWN ---

    private fun loadKecamatanForSpinner() {
        BpskpRetrofitClient.api.getMasterWilayah(level = LEVEL_KECAMATAN, perPage = 100)
            .enqueue(object : Callback<MasterWilayahResponse> {
                override fun onResponse(call: Call<MasterWilayahResponse>, response: Response<MasterWilayahResponse>) {
                    val data = response.body()?.data ?: return
                    val items = mutableListOf(SpinnerItem(null, "Semua Kecamatan"))
                    items.addAll(data.map { SpinnerItem(it.id, it.getNama()) })

                    isInitializingSpinners = true
                    spinnerKecamatan.adapter = getCustomSpinnerAdapter(items)
                    isInitializingSpinners = false
                }
                override fun onFailure(call: Call<MasterWilayahResponse>, t: Throwable) {}
            })
    }

    private fun loadKelurahanForSpinner(kecamatanId: Int) {
        BpskpRetrofitClient.api.getMasterWilayah(level = LEVEL_KELURAHAN, kecamatanId = kecamatanId, perPage = 100)
            .enqueue(object : Callback<MasterWilayahResponse> {
                override fun onResponse(call: Call<MasterWilayahResponse>, response: Response<MasterWilayahResponse>) {
                    val data = response.body()?.data ?: return
                    val items = mutableListOf(SpinnerItem(null, "Semua Kelurahan"))
                    items.addAll(data.map { SpinnerItem(it.id, it.getNama()) })

                    isInitializingSpinners = true
                    spinnerKelurahan.adapter = getCustomSpinnerAdapter(items)
                    spinnerKelurahan.isEnabled = true
                    spinnerKelurahan.alpha = 1.0f
                    isInitializingSpinners = false
                }
                override fun onFailure(call: Call<MasterWilayahResponse>, t: Throwable) {}
            })
    }

    // --- LOGIKA FETCH & RENDER DATA UTAMA (LIST) ---

    private fun loadMainData(resetPage: Boolean) {
        if (resetPage) currentPage = 1
        activeCall?.cancel()

        TransitionManager.beginDelayedTransition(listDataUtama, AutoTransition())
        listDataUtama.removeAllViews()
        layoutEmptyWilayah.visibility = View.GONE
        layoutPaginationWilayah.visibility = View.GONE

        tvTotalKecamatan.text = "Memuat..."

        // Cek filter apa yang sedang aktif
        val selectedKecId = (spinnerKecamatan.selectedItem as? SpinnerItem)?.id
        val selectedKelId = (spinnerKelurahan.selectedItem as? SpinnerItem)?.id

        val targetLevel: String

        when {
            selectedKelId != null -> {
                targetLevel = LEVEL_RTRW
                tvLabelDaftar.text = "Daftar RT/RW"
                tvDeskripsiWilayah.text = "Menampilkan data RT/RW berdasarkan kelurahan terpilih."
                tvLabelTotalWilayah.text = "Total RT/RW"
            }
            selectedKecId != null -> {
                targetLevel = LEVEL_KELURAHAN
                tvLabelDaftar.text = "Daftar Kelurahan"
                tvDeskripsiWilayah.text = "Pilih kelurahan atau gunakan filter untuk melihat RT/RW."
                tvLabelTotalWilayah.text = "Total Kelurahan"
            }
            else -> {
                targetLevel = LEVEL_KECAMATAN
                tvLabelDaftar.text = "Daftar Kecamatan"
                tvDeskripsiWilayah.text = "Pilih kecamatan atau gunakan dropdown untuk memfilter data."
                tvLabelTotalWilayah.text = "Total Kecamatan"
            }
        }

        activeCall = BpskpRetrofitClient.api.getMasterWilayah(
            level = targetLevel,
            q = searchQuery.ifBlank { null },
            kecamatanId = if (targetLevel == LEVEL_KELURAHAN) selectedKecId else null,
            kelurahanId = if (targetLevel == LEVEL_RTRW) selectedKelId else null,
            perPage = PER_PAGE,
            page = currentPage
        )

        activeCall?.enqueue(object : Callback<MasterWilayahResponse> {
            override fun onResponse(call: Call<MasterWilayahResponse>, response: Response<MasterWilayahResponse>) {
                if (isFinishing || isDestroyed) return

                val body = response.body()
                val data = body?.data

                if (!response.isSuccessful || data == null) {
                    showMainError("Gagal memuat data wilayah.")
                    return
                }

                val pagination = body.pagination
                currentPage = pagination?.currentPage ?: currentPage
                lastPage = pagination?.lastPage ?: 1
                totalData = pagination?.total ?: data.size

                tvTotalKecamatan.text = "$totalData"

                if (data.isEmpty()) {
                    layoutEmptyWilayah.visibility = View.VISIBLE
                } else {
                    renderMainList(data, targetLevel)
                }

                updatePagination()
            }

            override fun onFailure(call: Call<MasterWilayahResponse>, t: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                showMainError("Koneksi gagal memuat data wilayah.")
            }
        })
    }

    private fun renderMainList(data: List<MasterWilayah>, level: String) {
        listDataUtama.removeAllViews()

        data.forEachIndexed { index, item ->
            val card = buatKartuStatistik(item = item, level = level)
            val containerChild = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dp(12), dp(4), dp(4), dp(12))
            }

            listDataUtama.addView(card)
            listDataUtama.addView(containerChild)

            // Animasi Masuk (Fade & Slide in)
            card.alpha = 0f
            card.translationY = 50f
            card.animate().alpha(1f).translationY(0f).setDuration(300)
                .setStartDelay((index * 40).toLong()).start()

            // Jika level adalah RT/RW, tidak perlu klik untuk expand
            if (level == LEVEL_RTRW) return@forEachIndexed

            card.setOnClickListener {
                TransitionManager.beginDelayedTransition(listDataUtama, AutoTransition())
                val ivPanah = card.findViewById<ImageView>(R.id.ivPanah)

                if (containerChild.visibility == View.VISIBLE) {
                    containerChild.visibility = View.GONE
                    ivPanah?.animate()?.rotation(0f)?.setDuration(250)?.start()
                    return@setOnClickListener
                }

                containerChild.visibility = View.VISIBLE
                ivPanah?.animate()?.rotation(90f)?.setDuration(250)?.start()

                // Load child items inside the card
                if (level == LEVEL_KECAMATAN) {
                    loadNestedData(LEVEL_KELURAHAN, item.id, containerChild)
                } else if (level == LEVEL_KELURAHAN) {
                    loadNestedData(LEVEL_RTRW, item.id, containerChild)
                }
            }
        }
    }

    private fun loadNestedData(targetLevel: String, parentId: Int, container: LinearLayout) {
        tampilkanPesan(container, "Memuat data...")

        val call = BpskpRetrofitClient.api.getMasterWilayah(
            level = targetLevel,
            kecamatanId = if (targetLevel == LEVEL_KELURAHAN) parentId else null,
            kelurahanId = if (targetLevel == LEVEL_RTRW) parentId else null,
            perPage = 100
        )

        call.enqueue(object : Callback<MasterWilayahResponse> {
            override fun onResponse(call: Call<MasterWilayahResponse>, response: Response<MasterWilayahResponse>) {
                if (isFinishing || isDestroyed) return
                TransitionManager.beginDelayedTransition(container, AutoTransition())
                container.removeAllViews()

                val data = response.body()?.data
                if (data.isNullOrEmpty()) {
                    tampilkanPesan(container, "Belum ada data.")
                    return
                }

                container.addView(buatRingkasan("${data.size} ${if (targetLevel == LEVEL_KELURAHAN) "Kelurahan" else "RT/RW"}"))

                data.forEach { childItem ->
                    val card = buatKartuStatistik(childItem, targetLevel)
                    container.addView(card)

                    // Jika yang sedang dimuat adalah Kelurahan (hasil expand dari Kecamatan),
                    // maka kita harus buatkan wadah (subContainer) dan fungsi klik agar bisa buka RT/RW
                    if (targetLevel == LEVEL_KELURAHAN) {
                        val subContainer = LinearLayout(this@MasterwilayahActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            visibility = View.GONE
                            // Tambahkan margin kiri agar list RT/RW terlihat menjorok ke dalam
                            setPadding(dp(16), dp(4), dp(4), dp(12))
                        }
                        container.addView(subContainer)

                        card.setOnClickListener {
                            TransitionManager.beginDelayedTransition(container, AutoTransition())
                            val ivPanah = card.findViewById<ImageView>(R.id.ivPanah)

                            if (subContainer.visibility == View.VISIBLE) {
                                subContainer.visibility = View.GONE
                                ivPanah?.animate()?.rotation(0f)?.setDuration(250)?.start()
                            } else {
                                subContainer.visibility = View.VISIBLE
                                ivPanah?.animate()?.rotation(90f)?.setDuration(250)?.start()

                                // Panggil API untuk memuat RT/RW ke dalam subContainer
                                loadNestedData(LEVEL_RTRW, childItem.id, subContainer)
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<MasterWilayahResponse>, t: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                tampilkanPesan(container, "Koneksi gagal.")
            }
        })
    }


    private fun buatKartuStatistik(item: MasterWilayah, level: String): View {

        // 1. KHUSUS RT/RW: Desain Card Modern dengan Aksen Oranye
        if (level == LEVEL_RTRW) {
            val cardContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(4), 0, dp(8)) // Margin antar item
                }
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                gravity = android.view.Gravity.CENTER_VERTICAL

                // Background putih dengan sudut melengkung dan border tipis
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), Color.parseColor("#E7E5E4")) // Garis tepi abu-abu sangat muda
                }
                elevation = 2f
            }

            // Garis vertikal aksen oranye di sisi kiri
            val accentLine = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    setMargins(0, 0, dp(12), 0)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor(ORANGE))
                    cornerRadius = dp(4).toFloat()
                }
            }

            // Container untuk Teks (Kode & Nama)
            val textContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
            }

            // Label Kode SLS di bagian atas (Kecil, tebal, oranye)
            val tvKode = TextView(this).apply {
                text = "KODE: ${item.getKode()}"
                textSize = 11f
                setTextColor(Color.parseColor(ORANGE))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, dp(2))
            }

            // Nama SLS / RT RW (Besar, gelap)
            val tvNama = TextView(this).apply {
                text = item.getNama()
                textSize = 14f
                setTextColor(Color.parseColor("#1F2937")) // Abu-abu gelap tegas
                setTypeface(null, Typeface.BOLD)
            }

            textContainer.addView(tvKode)
            textContainer.addView(tvNama)

            cardContainer.addView(accentLine)
            cardContainer.addView(textContainer)

            return cardContainer
        }

        // 2. DESAIN LAMA UNTUK KECAMATAN & KELURAHAN
        val card = LayoutInflater.from(this).inflate(R.layout.item_wilayah, listDataUtama, false)

        val tvKode = card.findViewById<TextView>(R.id.tvKode)
        val tvKategori = card.findViewById<TextView>(R.id.tvKategori)
        val tvJudul = card.findViewById<TextView>(R.id.tvJudul)
        val tvNilai = card.findViewById<TextView>(R.id.tvNilai)
        val divider = card.findViewById<View>(R.id.divider)
        val ivPanah = card.findViewById<ImageView>(R.id.ivPanah)

        tvKode.text = item.getKode()
        tvJudul.text = item.getNama()

        tvKategori.text = when (level) {
            LEVEL_KECAMATAN -> "KECAMATAN"
            LEVEL_KELURAHAN -> "KELURAHAN"
            else -> "RT/RW"
        }

        when (level) {
            LEVEL_KECAMATAN -> {
                val jumlah = item.jumlahKelurahan
                if (jumlah != null) {
                    tvNilai.text = "$jumlah Kelurahan"
                    tvNilai.visibility = View.VISIBLE
                    divider.visibility = View.VISIBLE
                    ivPanah?.visibility = View.VISIBLE
                } else hideFooter(tvNilai, divider, ivPanah)
            }
            LEVEL_KELURAHAN -> {
                val jumlah = item.jumlahRtrw
                if (jumlah != null) {
                    tvNilai.text = "$jumlah RT/RW"
                    tvNilai.visibility = View.VISIBLE
                    divider.visibility = View.VISIBLE
                    ivPanah?.visibility = View.VISIBLE
                } else hideFooter(tvNilai, divider, ivPanah)
            }
            else -> hideFooter(tvNilai, divider, ivPanah)
        }

        return card
    }
    private fun hideFooter(tvNilai: TextView, divider: View, ivPanah: ImageView?) {
        tvNilai.visibility = View.GONE
        divider.visibility = View.GONE
        ivPanah?.visibility = View.GONE
    }

    private fun buatRingkasan(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor(ORANGE))
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(4), dp(8), dp(4), dp(10))
        }
    }

    private fun tampilkanPesan(container: LinearLayout, message: String) {
        container.removeAllViews()
        container.addView(TextView(this).apply {
            text = message
            textSize = 12f
            setTextColor(Color.parseColor(TEXT_SECONDARY))
            setPadding(dp(8), dp(12), dp(8), dp(12))
        })
    }

    private fun showMainError(message: String) {
        listDataUtama.removeAllViews()
        layoutPaginationWilayah.visibility = View.GONE
        layoutEmptyWilayah.visibility = View.VISIBLE
        tvTotalKecamatan.text = "0"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updatePagination() {
        layoutPaginationWilayah.visibility = if (lastPage > 1) View.VISIBLE else View.GONE
        tvInfoPagination.text = "Halaman $currentPage dari $lastPage\n$totalData data"

        btnHalamanSebelumnya.isEnabled = currentPage > 1
        btnHalamanBerikutnya.isEnabled = currentPage < lastPage

        btnHalamanSebelumnya.alpha = if (currentPage > 1) 1f else 0.45f
        btnHalamanBerikutnya.alpha = if (currentPage < lastPage) 1f else 0.45f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        activeCall?.cancel()
        super.onDestroy()
    }
}