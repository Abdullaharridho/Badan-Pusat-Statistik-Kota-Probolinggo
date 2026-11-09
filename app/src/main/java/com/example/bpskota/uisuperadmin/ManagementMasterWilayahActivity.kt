package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.MasterWilayah
import com.example.bpskota.bpskp.model.MasterWilayahRequest
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.google.android.material.button.MaterialButton

class ManagementMasterWilayahActivity : AppCompatActivity() {

    private val repository by lazy { BpskpRepository() }
    private lateinit var authSession: BpskpAuthSession

    private lateinit var spinnerLevel: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerUrutan: Spinner
    private lateinit var etCari: EditText
    private lateinit var rvWilayah: RecyclerView
    private lateinit var loading: LottieAnimationView
    private lateinit var layoutKosong: View
    private lateinit var layoutPagination: View
    private lateinit var tvJudulDaftar: TextView
    private lateinit var tvJumlahData: TextView
    private lateinit var tvHalaman: TextView
    private lateinit var tvIndikator: TextView
    private lateinit var tvJumlahKecamatan: TextView
    private lateinit var tvJumlahKelurahan: TextView
    private lateinit var tvJumlahRtRw: TextView
    private lateinit var btnTambah: MaterialButton
    private lateinit var btnSebelumnya: MaterialButton
    private lateinit var btnBerikutnya: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var changingSpinnerInternally = false
    private var isLoading = false

    private var currentLevel = "kecamatan"
    private var currentPage = 1
    private var lastPage = 1
    private val perPage = 20

    private var selectedKecamatanId: Int? = null
    private var selectedKelurahanId: Int? = null
    private var selectedKecamatanName: String? = null
    private var selectedKelurahanName: String? = null

    private var currentItems: List<MasterWilayah> = emptyList()

    private val adapter by lazy {
        MasterWilayahAdapter(
            onClick = { item -> handleItemClick(item) },
            onLongClick = { item -> showActionDialog(item) }
        )
    }

    private val levels = listOf(
        "Kecamatan",
        "Kelurahan",
        "RT/RW"
    )

    private val statusOptions = listOf(
        "Semua status",
        "Aktif",
        "Nonaktif"
    )

    private val sortOptions = listOf(
        "Nama A-Z",
        "Kode",
        "Terbaru"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_management_master_wilayah)

        authSession = BpskpAuthSession(this)

        // Pemeriksaan awal sesi dan role.
        if (!authSession.isLoggedIn() ||
            authSession.getRole() != "super_admin"
        ) {
            Toast.makeText(
                this,
                "Silakan login sebagai Super Admin.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        initViews()
        setupSpinners()
        setupRecyclerView()
        setupListeners()

        updateLevelDescription()
        loadData()
        loadSummary()
    }

    private fun initViews() {
        spinnerLevel = findViewById(R.id.spinnerLevelWilayah)
        spinnerStatus = findViewById(R.id.spinnerStatusMasterWilayah)
        spinnerUrutan = findViewById(R.id.spinnerUrutanMasterWilayah)

        etCari = findViewById(R.id.etCariMasterWilayah)
        rvWilayah = findViewById(R.id.rvMasterWilayah)
        loading = findViewById(R.id.loadingMasterWilayah)
        layoutKosong = findViewById(R.id.layoutKosongMasterWilayah)
        layoutPagination = findViewById(R.id.layoutPaginationMasterWilayah)

        tvJudulDaftar = findViewById(R.id.tvJudulDaftarWilayah)
        tvJumlahData = findViewById(R.id.tvJumlahDataWilayah)
        tvHalaman = findViewById(R.id.tvHalamanMasterWilayah)
        tvIndikator = findViewById(R.id.tvIndikatorHierarkiWilayah)

        tvJumlahKecamatan = findViewById(R.id.tvJumlahKecamatan)
        tvJumlahKelurahan = findViewById(R.id.tvJumlahKelurahan)
        tvJumlahRtRw = findViewById(R.id.tvJumlahRtRw)

        btnTambah = findViewById(R.id.btnTambahMasterWilayah)
        btnSebelumnya = findViewById(R.id.btnHalamanSebelumnya)
        btnBerikutnya = findViewById(R.id.btnHalamanBerikutnya)
    }

    private fun setupSpinners() {
        setupSpinner(spinnerLevel, levels)
        setupSpinner(spinnerStatus, statusOptions)
        setupSpinner(spinnerUrutan, sortOptions)

        spinnerLevel.setSelection(0)
    }

    private fun setupSpinner(
        spinner: Spinner,
        items: List<String>
    ) {
        val spinnerAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.BLACK)
                view.setBackgroundColor(android.graphics.Color.WHITE)
                return view
            }
        }

        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = spinnerAdapter
    }

    private fun setupRecyclerView() {
        rvWilayah.layoutManager = LinearLayoutManager(this)
        rvWilayah.adapter = adapter
        rvWilayah.isNestedScrollingEnabled = false
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnKembaliMasterWilayah)
            .setOnClickListener {
                navigateBack()
            }

        spinnerLevel.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (changingSpinnerInternally) return

                    when (position) {
                        0 -> {
                            selectedKecamatanId = null
                            selectedKelurahanId = null
                            selectedKecamatanName = null
                            selectedKelurahanName = null
                            changeLevel("kecamatan")
                        }

                        1 -> selectKecamatanParent()

                        2 -> selectRtRwParents()
                    }
                }
            }

        spinnerStatus.onItemSelectedListener =
            simpleSelectionListener {
                currentPage = 1
                loadData()
            }

        spinnerUrutan.onItemSelectedListener =
            simpleSelectionListener {
                currentPage = 1
                loadData()
            }

        etCari.addTextChangedListener(object : TextWatcher {
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
                searchRunnable?.let(handler::removeCallbacks)

                searchRunnable = Runnable {
                    currentPage = 1
                    loadData()
                }

                handler.postDelayed(searchRunnable!!, 400)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnTambah.setOnClickListener {
            showFormDialog(null)
        }

        findViewById<View>(R.id.btnTambahDariKosong)
            .setOnClickListener {
                showFormDialog(null)
            }

        btnSebelumnya.setOnClickListener {
            if (currentPage > 1 && !isLoading) {
                currentPage--
                loadData()
            }
        }

        btnBerikutnya.setOnClickListener {
            if (currentPage < lastPage && !isLoading) {
                currentPage++
                loadData()
            }
        }
    }

    private fun simpleSelectionListener(
        action: () -> Unit
    ) = object :
        android.widget.AdapterView.OnItemSelectedListener {

        override fun onNothingSelected(
            parent: android.widget.AdapterView<*>?
        ) = Unit

        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            action()
        }
    }

    // =====================================================
    // LEVEL DAN HIERARKI
    // =====================================================

    private fun changeLevel(level: String) {
        currentLevel = level
        currentPage = 1

        changingSpinnerInternally = true
        spinnerLevel.setSelection(
            when (level) {
                "kecamatan" -> 0
                "kelurahan" -> 1
                else -> 2
            }
        )
        changingSpinnerInternally = false

        updateLevelDescription()
        loadData()
    }

    private fun updateLevelDescription() {
        when (currentLevel) {
            "kecamatan" -> {
                tvJudulDaftar.text = "Daftar Kecamatan"
                tvIndikator.text = "Menampilkan seluruh kecamatan"
            }

            "kelurahan" -> {
                tvJudulDaftar.text = "Daftar Kelurahan"
                tvIndikator.text =
                    "Kecamatan: ${selectedKecamatanName ?: "-"}"
            }

            "rtrw" -> {
                tvJudulDaftar.text = "Daftar RT/RW"
                tvIndikator.text =
                    "${selectedKecamatanName ?: "-"}" +
                            " • ${selectedKelurahanName ?: "-"}"
            }
        }
    }

    private fun handleItemClick(item: MasterWilayah) {
        when (currentLevel) {
            "kecamatan" -> {
                selectedKecamatanId = item.id
                selectedKecamatanName = item.getNama()
                selectedKelurahanId = null
                selectedKelurahanName = null
                changeLevel("kelurahan")
            }

            "kelurahan" -> {
                selectedKelurahanId = item.id
                selectedKelurahanName = item.getNama()
                changeLevel("rtrw")
            }

            "rtrw" -> showActionDialog(item)
        }
    }

    private fun navigateBack() {
        when (currentLevel) {
            "rtrw" -> {
                selectedKelurahanId = null
                selectedKelurahanName = null
                changeLevel("kelurahan")
            }

            "kelurahan" -> {
                selectedKecamatanId = null
                selectedKecamatanName = null
                changeLevel("kecamatan")
            }

            else -> finish()
        }
    }

    override fun onBackPressed() {
        if (currentLevel != "kecamatan") {
            navigateBack()
        } else {
            super.onBackPressed()
        }
    }

    // =====================================================
    // MEMILIH INDUK KELURAHAN / RT-RW
    // =====================================================

    private fun selectKecamatanParent() {
        loadParentOptions(
            level = "kecamatan",
            kecamatanId = null,
            kelurahanId = null
        ) { item ->
            selectedKecamatanId = item.id
            selectedKecamatanName = item.getNama()
            selectedKelurahanId = null
            selectedKelurahanName = null
            changeLevel("kelurahan")
        }
    }

    private fun selectRtRwParents() {
        loadParentOptions(
            level = "kecamatan",
            kecamatanId = null,
            kelurahanId = null
        ) { kecamatan ->
            selectedKecamatanId = kecamatan.id
            selectedKecamatanName = kecamatan.getNama()
            selectedKelurahanId = null
            selectedKelurahanName = null

            loadParentOptions(
                level = "kelurahan",
                kecamatanId = kecamatan.id,
                kelurahanId = null
            ) { kelurahan ->
                selectedKelurahanId = kelurahan.id
                selectedKelurahanName = kelurahan.getNama()
                changeLevel("rtrw")
            }
        }
    }

    private fun loadParentOptions(
        level: String,
        kecamatanId: Int?,
        kelurahanId: Int?,
        onSelected: (MasterWilayah) -> Unit
    ) {
        repository.getMasterWilayah(
            level = level,
            perPage = 100,
            page = 1,
            kecamatanId = kecamatanId,
            kelurahanId = kelurahanId
        ) { response, error ->

            if (isFinishing || isDestroyed) return@getMasterWilayah

            val items = response?.data.orEmpty()

            if (error != null || items.isEmpty()) {
                showMessage(
                    error ?: "Data induk wilayah belum tersedia."
                )
                return@getMasterWilayah
            }

            val labels = items.map {
                "${it.getKode()}\n${it.getNama()}"
            }

            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(
                    if (level == "kecamatan")
                        "Pilih Kecamatan"
                    else
                        "Pilih Kelurahan"
                )
                .setItems(labels.toTypedArray()) { _, which ->
                    onSelected(items[which])
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    // =====================================================
    // GET DATA DAN PAGINATION
    // =====================================================

    private fun loadData() {
        if (isFinishing || isDestroyed) return

        isLoading = true
        loading.visibility = View.VISIBLE
        loading.playAnimation()
        rvWilayah.visibility = View.GONE
        layoutKosong.visibility = View.GONE

        val status = when (spinnerStatus.selectedItemPosition) {
            1 -> "aktif"
            2 -> "nonaktif"
            else -> null
        }

        val sort = when (spinnerUrutan.selectedItemPosition) {
            1 -> "kode"
            2 -> "terbaru"
            else -> "nama"
        }

        val order = if (sort == "terbaru") "desc" else "asc"

        repository.getMasterWilayah(
            level = currentLevel,
            q = etCari.text.toString().trim().ifBlank { null },
            status = status,
            sort = sort,
            order = order,
            perPage = perPage,
            page = currentPage,
            kecamatanId = if (currentLevel != "kecamatan")
                selectedKecamatanId else null,
            kelurahanId = if (currentLevel == "rtrw")
                selectedKelurahanId else null
        ) { response, error ->

            if (isFinishing || isDestroyed) return@getMasterWilayah

            isLoading = false
            loading.cancelAnimation()
            loading.visibility = View.GONE

            if (response == null) {
                showMessage(error ?: "Gagal memuat data wilayah.")
                rvWilayah.visibility = View.GONE
                layoutKosong.visibility = View.VISIBLE
                layoutPagination.visibility = View.GONE
                return@getMasterWilayah
            }

            currentItems = response.data.orEmpty()
            adapter.submitList(currentItems)

            tvJumlahData.text =
                "${response.pagination?.total ?: currentItems.size} data"

            currentPage = response.pagination?.currentPage ?: currentPage
            lastPage = response.pagination?.lastPage ?: 1

            tvHalaman.text = "$currentPage / $lastPage"

            btnSebelumnya.isEnabled = currentPage > 1
            btnBerikutnya.isEnabled = currentPage < lastPage

            val isEmpty = currentItems.isEmpty()

            rvWilayah.visibility = if (isEmpty) View.GONE else View.VISIBLE
            layoutKosong.visibility = if (isEmpty) View.VISIBLE else View.GONE

            layoutPagination.visibility =
                if (lastPage > 1) View.VISIBLE else View.GONE
        }
    }

    private fun loadSummary() {
        fun loadCount(
            level: String,
            callback: (Int) -> Unit
        ) {
            repository.getMasterWilayah(
                level = level,
                perPage = 1,
                page = 1
            ) { response, _ ->
                if (!isFinishing && !isDestroyed) {
                    callback(response?.pagination?.total
                        ?: response?.data?.size
                        ?: 0)
                }
            }
        }

        loadCount("kecamatan") {
            tvJumlahKecamatan.text = it.toString()
        }

        loadCount("kelurahan") {
            tvJumlahKelurahan.text = it.toString()
        }

        loadCount("rtrw") {
            tvJumlahRtRw.text = it.toString()
        }
    }

    // =====================================================
    // MENU EDIT / HAPUS
    // =====================================================

    private fun showActionDialog(item: MasterWilayah) {
        val options = arrayOf("Edit data", "Hapus data")

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle(item.getNama())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFormDialog(item)
                    1 -> confirmDelete(item)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // =====================================================
    // FORM CREATE DAN UPDATE
    // =====================================================

    private fun showFormDialog(item: MasterWilayah?) {
        val isEdit = item != null

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }

        fun createInput(
            hintText: String,
            initialValue: String = ""
        ): EditText {
            val input = EditText(this).apply {
                hint = hintText
                setText(initialValue)
                setSingleLine(true)
                textSize = 14f
                setTextColor(android.graphics.Color.rgb(17, 24, 39))
                setHintTextColor(android.graphics.Color.rgb(156, 163, 175))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(
                    android.graphics.Color.rgb(249, 250, 251)
                )
            }

            container.addView(
                input,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(50)
                ).apply {
                    bottomMargin = dp(12)
                }
            )

            return input
        }

        val codeInput = createInput(
            "Kode wilayah",
            item?.getKode()?.takeIf { it != "-" } ?: ""
        )

        val nameInput = createInput(
            "Nama wilayah",
            item?.getNama()?.takeIf { it != "-" } ?: ""
        )

        val activeCheck = CheckBox(this).apply {
            text = "Wilayah aktif"
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(55, 65, 81))
            isChecked = item?.isActive?.let { it != 0 } ?: true
        }

        container.addView(activeCheck)

        val title = when (currentLevel) {
            "kecamatan" -> if (isEdit)
                "Edit Kecamatan" else "Tambah Kecamatan"

            "kelurahan" -> if (isEdit)
                "Edit Kelurahan" else "Tambah Kelurahan"

            else -> if (isEdit)
                "Edit RT/RW" else "Tambah RT/RW"
        }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle(title)
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton(
                if (isEdit) "Simpan Perubahan" else "Simpan",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val kode = codeInput.text.toString().trim()
                    val nama = nameInput.text.toString().trim()

                    if (kode.isBlank()) {
                        codeInput.error = "Kode wajib diisi"
                        return@setOnClickListener
                    }

                    if (nama.isBlank()) {
                        nameInput.error = "Nama wajib diisi"
                        return@setOnClickListener
                    }

                    val request = when (currentLevel) {
                        "kecamatan" -> MasterWilayahRequest(
                            kodeKecamatan = kode,
                            namaKecamatan = nama,
                            isActive = activeCheck.isChecked
                        )

                        "kelurahan" -> {
                            val parentId = selectedKecamatanId

                            if (parentId == null) {
                                showMessage("Pilih kecamatan terlebih dahulu.")
                                return@setOnClickListener
                            }

                            MasterWilayahRequest(
                                kodeKelurahan = kode,
                                namaKelurahan = nama,
                                kecamatanId = parentId,
                                isActive = activeCheck.isChecked
                            )
                        }

                        else -> {
                            val parentId = selectedKelurahanId

                            if (parentId == null) {
                                showMessage("Pilih kelurahan terlebih dahulu.")
                                return@setOnClickListener
                            }

                            MasterWilayahRequest(
                                kodeWilayah = kode,
                                namaWilayah = nama,
                                kelurahanId = parentId,
                                isActive = activeCheck.isChecked
                            )
                        }
                    }

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                    if (isEdit) {
                        val wilayahId = item?.id

                        if (wilayahId == null) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            showMessage("Data wilayah tidak valid.")
                            return@setOnClickListener
                        }

                        repository.ubahMasterWilayah(
                            context = this,
                            level = currentLevel,
                            id = wilayahId,
                            request = request
                        ) { result, error ->
                            if (isFinishing || isDestroyed) {
                                return@ubahMasterWilayah
                            }

                            if (result != null) {
                                dialog.dismiss()
                                showMessage("Data wilayah berhasil diperbarui.")
                                loadData()
                                loadSummary()
                            } else {
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                showMessage(error ?: "Gagal memperbarui data.")
                            }
                        }

                    } else {
                        repository.tambahMasterWilayah(
                            context = this,
                            level = currentLevel,
                            request = request
                        ) { result, error ->

                            if (isFinishing || isDestroyed) {
                                return@tambahMasterWilayah
                            }

                            if (result != null) {
                                dialog.dismiss()
                                showMessage("Data wilayah berhasil ditambahkan.")
                                loadData()
                                loadSummary()
                            } else {
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                showMessage(error ?: "Gagal menambahkan data.")
                            }
                        }
                    }
                }
        }

        dialog.show()
    }

    // =====================================================
    // DELETE
    // =====================================================

    private fun confirmDelete(item: MasterWilayah) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle("Hapus Wilayah")
            .setMessage(
                "Apakah kamu yakin ingin menghapus ${item.getNama()}?"
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repository.hapusMasterWilayah(
                    context = this,
                    level = currentLevel,
                    id = item.id
                ) { success, message ->

                    if (isFinishing || isDestroyed) {
                        return@hapusMasterWilayah
                    }

                    showMessage(
                        message ?: if (success) {
                            "Data berhasil dihapus."
                        } else {
                            "Gagal menghapus data."
                        }
                    )

                    if (success) {
                        loadData()
                        loadSummary()
                    }
                }
            }
            .show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        searchRunnable?.let(handler::removeCallbacks)
        loading.cancelAnimation()
        super.onDestroy()
    }

    // =====================================================
    // ADAPTER ITEM_WILAYAH
    // =====================================================

    private class MasterWilayahAdapter(
        private val onClick: (MasterWilayah) -> Unit,
        private val onLongClick: (MasterWilayah) -> Unit
    ) : RecyclerView.Adapter<MasterWilayahAdapter.ViewHolder>() {

        private var items: List<MasterWilayah> = emptyList()
        private var level: String = "kecamatan"

        fun submitList(data: List<MasterWilayah>) {
            items = data
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            private val card: CardView = view as CardView
            private val tvJudul: TextView =
                view.findViewById(R.id.tvJudul)
            private val tvKategori: TextView =
                view.findViewById(R.id.tvKategori)
            private val tvKode: TextView =
                view.findViewById(R.id.tvKode)
            private val tvNilai: TextView =
                view.findViewById(R.id.tvNilai)
            private val ivPanah: View =
                view.findViewById(R.id.ivPanah)

            fun bind(item: MasterWilayah) {
                tvJudul.text = item.getNama()
                tvKode.text = item.getKode()

                tvKategori.text = when {
                    item.kodeKecamatan != null -> "KECAMATAN"
                    item.kodeKelurahan != null -> "KELURAHAN"
                    else -> "RT/RW"
                }

                tvNilai.text = when {
                    item.jumlahKelurahan != null ->
                        "${item.jumlahKelurahan} Kelurahan"

                    item.jumlahRtrw != null ->
                        "${item.jumlahRtrw} RT/RW"

                    item.isActive != null ->
                        if (item.isActive != 0) "Aktif" else "Nonaktif"

                    else -> "Data wilayah"
                }

                card.setOnClickListener {
                    onClick(item)
                }

                card.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wilayah, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}