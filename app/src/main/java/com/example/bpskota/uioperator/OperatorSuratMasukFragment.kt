package com.example.bpskota.uioperator

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.SuratMasuk
import com.example.bpskota.bpskp.model.SuratMasukActionResponse
import com.example.bpskota.bpskp.model.Tim
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.databinding.FragmentOperatorSuratMasukBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OperatorSuratMasukFragment : Fragment() {

    private var _binding: FragmentOperatorSuratMasukBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var suratAdapter: SuratAdapter

    private var selectedStatus: String? = null
    private var selectedFileUri: Uri? = null
    private var currentSurat: SuratMasuk? = null
    private var formMode: FormMode? = null

    private var formDialog: AlertDialog? = null
    private var formScrollView: ScrollView? = null
    private var formContainer: LinearLayout? = null

    private var etNomorSurat: EditText? = null
    private var etTanggalSurat: EditText? = null
    private var etTanggalDiterima: EditText? = null
    private var etAsalSurat: EditText? = null
    private var etPerihal: EditText? = null
    private var etIsiRingkas: EditText? = null
    private var etTanggalAcara: EditText? = null
    private var etWaktuMulai: EditText? = null
    private var etWaktuSelesai: EditText? = null
    private var etLokasi: EditText? = null

    private var spinnerStatus: Spinner? = null
    private var spinnerTim: Spinner? = null
    private var daftarTimAktif: List<Tim> = emptyList()

    private var tvFileName: TextView? = null
    private var btnPilihFile: TextView? = null
    private var btnSimpanForm: TextView? = null
    private var btnBatalForm: TextView? = null

    private enum class FormMode {
        CREATE,
        EDIT,
        DETAIL
    }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && _binding != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Beberapa provider tidak mendukung persistable permission.
                }

                selectedFileUri = uri
                tvFileName?.text = getFileName(uri)
                tvFileName?.setTextColor(Color.parseColor("#F97316"))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorSuratMasukBinding.inflate(
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

        setupLoading()
        setupRecyclerView()
        setupActions()
        loadSuratMasuk()
    }

    override fun onDestroyView() {
        removeFormView()
        _binding = null
        super.onDestroyView()
    }

    // =========================================================
    // SETUP
    // =========================================================

    private fun setupLoading() {
        binding.loadingAnimation.setAnimation("Loading_Animation.json")
    }

    private fun setupRecyclerView() {
        suratAdapter = SuratAdapter()

        binding.rvSuratMasuk.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvSuratMasuk.adapter = suratAdapter
        binding.rvSuratMasuk.setHasFixedSize(true)
    }

    private fun setupActions() {
        binding.btnTambahSurat.setOnClickListener {
            showCreateForm()
        }

        binding.etSearchSurat.setOnEditorActionListener { _, _, _ ->
            loadSuratMasuk(
                search = binding.etSearchSurat.text
                    .toString()
                    .trim()
                    .ifBlank { null }
            )
            true
        }

        binding.filterSemua.setOnClickListener {
            selectFilter(it, null)
        }

        binding.filterBaru.setOnClickListener {
            selectFilter(it, "baru")
        }

        binding.filterDiproses.setOnClickListener {
            selectFilter(it, "diproses")
        }

        binding.filterSelesai.setOnClickListener {
            selectFilter(it, "selesai")
        }

        selectFilter(
            selectedView = binding.filterSemua,
            status = null,
            reload = false
        )
    }

    private fun selectFilter(
        selectedView: View,
        status: String?,
        reload: Boolean = true
    ) {
        selectedStatus = status

        val filters = listOf(
            binding.filterSemua,
            binding.filterBaru,
            binding.filterDiproses,
            binding.filterSelesai
        )

        filters.forEach { view ->
            val selected = view == selectedView

            view.animate()
                .scaleX(if (selected) 1.04f else 1f)
                .scaleY(if (selected) 1.04f else 1f)
                .setDuration(160)
                .setInterpolator(DecelerateInterpolator())
                .start()

            (view as? TextView)?.setTextColor(
                if (selected) Color.WHITE
                else Color.parseColor("#4B5563")
            )

            view.background = GradientDrawable().apply {
                setColor(
                    if (selected) Color.parseColor("#F97316")
                    else Color.parseColor("#F3F4F6")
                )
                cornerRadius = dp(24).toFloat()
            }
        }

        if (reload) {
            loadSuratMasuk()
        }
    }

    // =========================================================
    // LOAD DATA
    // =========================================================

    private fun loadSuratMasuk(search: String? = null) {
        if (_binding == null) return

        showLoading(true)

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            showLoading(false)
            suratAdapter.updateData(emptyList())
            showEmptyState(true)
            toast("Sesi login tidak ditemukan.")
            return
        }

        repository.getSuratMasuk(
            authorization = "Bearer $token",
            search = search ?: binding.etSearchSurat.text
                .toString()
                .trim()
                .ifBlank { null },
            status = selectedStatus
        ) { data, error ->
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread

                showLoading(false)

                if (error != null) {
                    suratAdapter.updateData(emptyList())
                    showEmptyState(true)
                    toast(error)
                    return@runOnUiThread
                }

                val items = data ?: emptyList()

                suratAdapter.updateData(items)
                showEmptyState(items.isEmpty())
            }
        }
    }

    // =========================================================
    // CUSTOM SPINNER ADAPTER
    // Memaksa agar teks spinner dan dropdown menjadi hitam
    // =========================================================

    private fun createSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#111827")) // Teks hitam
                view.textSize = 14f
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#111827")) // Teks dropdown hitam
                view.setBackgroundColor(Color.WHITE)           // Latar belakang putih bersih
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    // =========================================================
    // FORM TAMBAH / EDIT / DETAIL
    // =========================================================

    private fun showCreateForm() {
        currentSurat = null
        selectedFileUri = null
        formMode = FormMode.CREATE

        createFormView(
            title = "Tambah Surat Masuk",
            subtitle = "Isi data surat masuk yang akan dicatat.",
            saveText = "Simpan"
        )

        val today = todayDate()

        etTanggalSurat?.setText(today)
        etTanggalDiterima?.setText(today)

        setStatus("baru")
        setFormEditable(true)

        loadTimOptions()
    }

    private fun showEditForm(surat: SuratMasuk) {
        currentSurat = surat
        selectedFileUri = null
        formMode = FormMode.EDIT

        createFormView(
            title = "Edit Surat Masuk",
            subtitle = "Perbarui informasi surat masuk.",
            saveText = "Simpan Perubahan"
        )

        fillForm(surat)
        setFormEditable(true)

        loadTimOptions(surat.tim_id)
    }

    private fun showDetailForm(surat: SuratMasuk) {
        currentSurat = surat
        selectedFileUri = null
        formMode = FormMode.DETAIL

        createFormView(
            title = "Detail Surat Masuk",
            subtitle = "Informasi lengkap surat masuk.",
            saveText = "Tutup"
        )

        fillForm(surat)
        setFormEditable(false)

        loadTimOptions(surat.tim_id)

        btnPilihFile?.visibility = View.GONE
        btnBatalForm?.visibility = View.GONE
        btnSimpanForm?.text = "Tutup"
    }

    private fun createFormView(
        title: String,
        subtitle: String,
        saveText: String
    ) {
        removeFormView()

        val context = context ?: return

        formScrollView = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        formContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        formScrollView?.addView(
            formContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addLabel(title, topMargin = 0, textSize = 22f)
        addLabel(
            subtitle,
            topMargin = 4,
            textSize = 13f,
            bold = false
        )

        addLabel("Nomor Surat")
        etNomorSurat = createTextInput("Masukkan nomor surat")

        addLabel("Tanggal Surat")
        etTanggalSurat = createDateInput("Pilih tanggal surat")

        addLabel("Tanggal Diterima")
        etTanggalDiterima = createDateInput("Pilih tanggal diterima")

        addLabel("Asal Surat")
        etAsalSurat = createTextInput("Masukkan asal surat")

        addLabel("Perihal")
        etPerihal = createTextInput("Masukkan perihal")

        addLabel("Tim Tujuan")

        spinnerTim = Spinner(context).apply {
            adapter = createSpinnerAdapter(listOf("Memuat daftar tim..."))
            background = inputBackground()
            isEnabled = false
        }

        addInputView(spinnerTim!!)

        addLabel("Isi Ringkas")
        etIsiRingkas = createTextInput(
            "Masukkan isi ringkas surat",
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
        ).apply {
            minLines = 4
            gravity = Gravity.TOP
        }

        addLabel("Tanggal Acara")
        etTanggalAcara = createDateInput("Pilih tanggal acara")

        addLabel("Waktu Mulai")
        etWaktuMulai = createTimeInput("Pilih waktu mulai")

        addLabel("Waktu Selesai")
        etWaktuSelesai = createTimeInput("Pilih waktu selesai")

        addLabel("Lokasi")
        etLokasi = createTextInput("Masukkan lokasi acara")

        addLabel("Status")

        spinnerStatus = Spinner(context).apply {
            adapter = createSpinnerAdapter(listOf("baru", "diproses", "selesai"))
            background = inputBackground()
            isEnabled = false
            isClickable = false
            alpha = 0.7f
        }

        addInputView(spinnerStatus!!)

        addLabel("File Surat")

        val fileRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = inputBackground()
        }

        formContainer?.addView(
            fileRow,
            matchWrapParams(top = 4)
        )

        tvFileName = TextView(context).apply {
            text = "Belum ada file dipilih"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7280"))

            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        fileRow.addView(tvFileName)

        btnPilihFile = TextView(context).apply {
            text = "Cari"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#F97316"))
            setPadding(dp(12), dp(8), dp(12), dp(8))

            setOnClickListener {
                if (formMode != FormMode.DETAIL) {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/jpeg",
                            "image/png"
                        )
                    )
                }
            }
        }

        fileRow.addView(btnPilihFile)

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        formContainer?.addView(
            actions,
            matchWrapParams(top = 24)
        )

        btnBatalForm = createActionButton(
            label = "Batal",
            primary = false
        ) {
            returnToList()
        }

        actions.addView(
            btnBatalForm,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
            )
        )

        btnSimpanForm = createActionButton(
            label = saveText,
            primary = true
        ) {
            when (formMode) {
                FormMode.CREATE -> createSuratMasuk()
                FormMode.EDIT -> updateSuratMasuk()
                FormMode.DETAIL -> returnToList()
                null -> Unit
            }
        }

        actions.addView(
            btnSimpanForm,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
            ).apply {
                marginStart = dp(8)
            }
        )

        formDialog = AlertDialog.Builder(context)
            .setView(formScrollView)
            .create()

        formDialog?.setOnDismissListener {
            formDialog = null
            clearFormReferences()
        }

        formDialog?.show()

        formDialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            val screenWidth = resources.displayMetrics.widthPixels
            val dialogWidth = (screenWidth * 0.94f).toInt()

            setLayout(
                dialogWidth,
                (resources.displayMetrics.heightPixels * 0.82f).toInt()
            )
        }
    }

    // =========================================================
    // FORM COMPONENTS
    // =========================================================

    private fun addLabel(
        text: String,
        topMargin: Int = 16,
        textSize: Float = 13f,
        bold: Boolean = true
    ) {
        val label = TextView(requireContext()).apply {
            this.text = text
            this.textSize = textSize

            setTextColor(
                if (textSize >= 20f) {
                    Color.parseColor("#111827")
                } else {
                    Color.parseColor("#4B5563")
                }
            )

            if (bold) {
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }
        }

        formContainer?.addView(
            label,
            matchWrapParams(top = topMargin)
        )
    }

    private fun createTextInput(
        hintText: String,
        type: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {
        return EditText(requireContext()).apply {
            hint = hintText
            inputType = type
            textSize = 14f

            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setPadding(dp(14), dp(8), dp(14), dp(8))

            background = inputBackground()

            if (type and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0) {
                setSingleLine(true)
            }

            addInputView(this)
        }
    }

    private fun createDateInput(hintText: String): EditText {
        return createTextInput(hintText).apply {
            isFocusable = false
            isClickable = true
            setTextColor(Color.parseColor("#111827")) // Pastikan teks hitam

            setOnClickListener {
                if (formMode != FormMode.DETAIL) {
                    showDatePicker(this)
                }
            }
        }
    }

    private fun createTimeInput(hintText: String): EditText {
        return createTextInput(hintText).apply {
            isFocusable = false
            isClickable = true
            setTextColor(Color.parseColor("#111827")) // Pastikan teks hitam

            setOnClickListener {
                if (formMode != FormMode.DETAIL) {
                    showTimePicker(this)
                }
            }
        }
    }

    private fun addInputView(view: View) {
        formContainer?.addView(
            view,
            matchWrapParams(height = 50, top = 4)
        )
    }

    private fun matchWrapParams(
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        top: Int = 0
    ) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        if (height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            height
        } else {
            dp(height)
        }
    ).apply {
        topMargin = dp(top)
    }

    private fun inputBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#F9FAFB"))
            setStroke(dp(1), Color.parseColor("#E5E7EB"))
            cornerRadius = dp(12).toFloat()
        }
    }

    private fun createActionButton(
        label: String,
        primary: Boolean,
        action: () -> Unit
    ): TextView {
        return TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 13f

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            setTextColor(
                if (primary) {
                    Color.WHITE
                } else {
                    Color.parseColor("#6B7280")
                }
            )

            setPadding(dp(18), dp(12), dp(18), dp(12))

            background = GradientDrawable().apply {
                setColor(
                    if (primary) {
                        Color.parseColor("#F97316")
                    } else {
                        Color.parseColor("#F3F4F6")
                    }
                )

                cornerRadius = dp(12).toFloat()
            }

            setOnClickListener {
                action()
            }
        }
    }

    private fun showDatePicker(target: EditText) {
        val calendar = Calendar.getInstance()
        val current = target.text.toString().trim()

        if (current.isNotBlank()) {
            try {
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(current)?.let {
                    calendar.time = it
                }
            } catch (_: Exception) {
                // Gunakan tanggal saat ini jika format tidak sesuai.
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day)
                }

                target.setText(
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).format(selected.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(target: EditText) {
        val calendar = Calendar.getInstance()
        val current = target.text.toString().trim()

        if (current.isNotBlank()) {
            try {
                SimpleDateFormat(
                    "HH:mm",
                    Locale.US
                ).parse(current)?.let {
                    calendar.time = it
                }
            } catch (_: Exception) {
                // Gunakan waktu saat ini jika format tidak sesuai.
            }
        }

        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                target.setText(
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        hour,
                        minute
                    )
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun fillForm(surat: SuratMasuk) {
        etNomorSurat?.setText(surat.nomor_surat.orEmpty())
        etTanggalSurat?.setText(surat.tanggal_surat.orEmpty())
        etTanggalDiterima?.setText(surat.tanggal_diterima.orEmpty())
        etAsalSurat?.setText(surat.asal_surat.orEmpty())
        etPerihal?.setText(surat.perihal.orEmpty())
        etIsiRingkas?.setText(surat.isi_ringkas.orEmpty())
        etTanggalAcara?.setText(surat.tanggal_acara.orEmpty())
        etWaktuMulai?.setText(surat.waktu_mulai.orEmpty())
        etWaktuSelesai?.setText(surat.waktu_selesai.orEmpty())
        etLokasi?.setText(surat.lokasi.orEmpty())

        setStatus(surat.status ?: "baru")

        tvFileName?.text = surat.file_surat
            ?.substringAfterLast("/")
            ?.ifBlank { "File surat tersedia" }
            ?: "Belum ada file"

        tvFileName?.setTextColor(
            Color.parseColor("#F97316")
        )
    }

    private fun setStatus(status: String) {
        val spinner = spinnerStatus ?: return
        val adapter = spinner.adapter ?: return

        for (i in 0 until adapter.count) {
            if (
                adapter.getItem(i)
                    .toString()
                    .equals(status, ignoreCase = true)
            ) {
                spinner.setSelection(i)
                return
            }
        }
    }

    // =========================================================
    // TIM TUJUAN
    // =========================================================

    private fun loadTimOptions(selectedTimId: Int? = null) {
        spinnerTim?.isEnabled = false

        repository.getTim(
            session = authSession,
            search = null,
            status = "aktif"
        ) { data, error ->

            activity?.runOnUiThread {
                if (_binding == null || spinnerTim == null || formMode == null) {
                    return@runOnUiThread
                }

                if (error != null) {
                    daftarTimAktif = emptyList()
                    spinnerTim?.adapter = createSpinnerAdapter(listOf("Gagal memuat daftar tim"))
                    spinnerTim?.isEnabled = false
                    toast(error)
                    return@runOnUiThread
                }

                daftarTimAktif = data ?: emptyList()

                val labels = mutableListOf("Pilih tim")
                labels.addAll(
                    daftarTimAktif.map { it.namaTim.orEmpty() }
                )

                // Gunakan custom adapter agar dropdown selalu berwarna hitam di atas putih
                spinnerTim?.adapter = createSpinnerAdapter(labels)

                val selectedPosition = daftarTimAktif.indexOfFirst {
                    it.id == selectedTimId
                }

                spinnerTim?.setSelection(
                    if (selectedPosition >= 0) selectedPosition + 1 else 0
                )

                spinnerTim?.isEnabled =
                    formMode != FormMode.DETAIL &&
                            daftarTimAktif.isNotEmpty()

                if (daftarTimAktif.isEmpty()) {
                    toast("Belum ada tim aktif yang tersedia.")
                }
            }
        }
    }

    private fun selectedTimId(): Int? {
        val position = spinnerTim?.selectedItemPosition ?: 0

        if (position <= 0) return null

        return daftarTimAktif.getOrNull(position - 1)?.id
    }

    private fun setFormEditable(editable: Boolean) {
        listOf(
            etNomorSurat,
            etTanggalSurat,
            etTanggalDiterima,
            etAsalSurat,
            etPerihal,
            etIsiRingkas,
            etTanggalAcara,
            etWaktuMulai,
            etWaktuSelesai,
            etLokasi
        ).forEach { field ->
            field?.isEnabled = editable
        }

        spinnerStatus?.isEnabled = false
        spinnerStatus?.isClickable = false
        spinnerStatus?.alpha = 0.7f

        spinnerTim?.isEnabled =
            editable && daftarTimAktif.isNotEmpty()

        btnPilihFile?.isEnabled = editable
    }

    // =========================================================
    // CREATE
    // =========================================================

    private fun createSuratMasuk() {
        if (!validateForm()) return

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            toast("Sesi login tidak ditemukan.")
            return
        }

        val filePart = createMultipartFile()

        if (selectedFileUri != null && filePart == null) {
            return
        }

        setFormLoading(true)

        repository.createSuratMasuk(
            authorization = "Bearer $token",
            nomorSurat = value(etNomorSurat),
            tanggalSurat = value(etTanggalSurat),
            tanggalDiterima = value(etTanggalDiterima),
            asalSurat = value(etAsalSurat),
            perihal = value(etPerihal),
            timId = selectedTimId()!!,
            isiRingkas = nullableValue(etIsiRingkas),
            tanggalAcara = nullableValue(etTanggalAcara),
            waktuMulai = nullableValue(etWaktuMulai),
            waktuSelesai = nullableValue(etWaktuSelesai),
            lokasi = nullableValue(etLokasi),
            fileSurat = filePart
        ) { _, error ->
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread

                setFormLoading(false)

                if (error != null) {
                    toast(error)
                    return@runOnUiThread
                }

                toast("Surat masuk berhasil disimpan.")

                returnToList()
                loadSuratMasuk()
            }
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    private fun updateSuratMasuk() {
        val surat = currentSurat
        val id = surat?.id

        if (id == null) {
            toast("ID surat tidak ditemukan.")
            return
        }

        if (!validateForm()) return

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            toast("Sesi login tidak ditemukan.")
            return
        }

        val filePart = createMultipartFile()

        if (selectedFileUri != null && filePart == null) {
            return
        }

        setFormLoading(true)

        val statusTetap = (surat.status ?: "baru").toTextRequestBody()

        BpskpRetrofitClient.api.updateSuratMasuk(
            authorization = "Bearer $token",
            id = id,
            nomorSurat = value(etNomorSurat).toTextRequestBody(),
            tanggalSurat = value(etTanggalSurat).toTextRequestBody(),
            tanggalDiterima = value(etTanggalDiterima).toTextRequestBody(),
            asalSurat = value(etAsalSurat).toTextRequestBody(),
            perihal = value(etPerihal).toTextRequestBody(),
            timId = selectedTimId()!!.toString().toTextRequestBody(),
            isiRingkas = nullableValue(etIsiRingkas)?.toTextRequestBody(),
            tanggalAcara = nullableValue(etTanggalAcara)?.toTextRequestBody(),
            waktuMulai = nullableValue(etWaktuMulai)?.toTextRequestBody(),
            waktuSelesai = nullableValue(etWaktuSelesai)?.toTextRequestBody(),
            lokasi = nullableValue(etLokasi)?.toTextRequestBody(),
            status = statusTetap,
            fileSurat = filePart
        ).enqueue(
            object : Callback<SuratMasukActionResponse> {

                override fun onResponse(
                    call: Call<SuratMasukActionResponse>,
                    response: Response<SuratMasukActionResponse>
                ) {
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread

                        setFormLoading(false)

                        if (response.isSuccessful) {
                            toast(
                                response.body()?.message
                                    ?: "Surat berhasil diperbarui."
                            )

                            returnToList()
                            loadSuratMasuk()
                        } else {
                            toast(
                                "Gagal memperbarui surat. " +
                                        "Periksa data dan respons API."
                            )
                        }
                    }
                }

                override fun onFailure(
                    call: Call<SuratMasukActionResponse>,
                    t: Throwable
                ) {
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread

                        setFormLoading(false)
                        toast(t.message ?: "Koneksi gagal.")
                    }
                }
            }
        )
    }

    // =========================================================
    // DELETE
    // =========================================================

    private fun deleteSuratMasuk(surat: SuratMasuk) {
        val id = surat.id ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Surat")
            .setMessage(
                "Apakah Anda yakin ingin menghapus surat " +
                        "${surat.nomor_surat ?: "-"}?"
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                val token = authSession.getToken()

                if (token.isNullOrBlank()) {
                    toast("Sesi login tidak ditemukan.")
                    return@setPositiveButton
                }

                showLoading(true)

                repository.deleteSuratMasuk(
                    authorization = "Bearer $token",
                    id = id
                ) { success, error ->
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread

                        showLoading(false)

                        when {
                            error != null -> toast(error)

                            success -> {
                                toast("Surat berhasil dihapus.")
                                loadSuratMasuk()
                            }

                            else -> toast("Gagal menghapus surat.")
                        }
                    }
                }
            }
            .show()
    }

    // =========================================================
    // VALIDASI
    // =========================================================

    private fun validateForm(): Boolean {
        val required = listOf(
            etNomorSurat to "Nomor surat wajib diisi.",
            etTanggalSurat to "Tanggal surat wajib diisi.",
            etTanggalDiterima to "Tanggal diterima wajib diisi.",
            etAsalSurat to "Asal surat wajib diisi.",
            etPerihal to "Perihal wajib diisi."
        )

        for ((field, message) in required) {
            if (value(field).isBlank()) {
                field?.error = message
                field?.requestFocus()
                toast(message)
                return false
            }
        }

        if (selectedTimId() == null) {
            toast("Tim tujuan wajib dipilih.")
            spinnerTim?.requestFocus()
            return false
        }

        return true
    }

    // =========================================================
    // FILE
    // =========================================================

    private fun createMultipartFile(): MultipartBody.Part? {
        val uri = selectedFileUri ?: return null

        return try {
            val file = copyUriToCache(uri)

            val mime = requireContext()
                .contentResolver
                .getType(uri)
                ?: "application/octet-stream"

            MultipartBody.Part.createFormData(
                "file_surat",
                file.name,
                file.asRequestBody(mime.toMediaType())
            )
        } catch (e: Exception) {
            toast("Gagal membaca file: ${e.message}")
            null
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val fileName = getFileName(uri)

        val safeFileName = fileName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "surat" }

        val file = File(
            requireContext().cacheDir,
            "${System.currentTimeMillis()}_$safeFileName"
        )

        val input = requireContext()
            .contentResolver
            .openInputStream(uri)
            ?: throw IllegalStateException("File tidak dapat dibaca.")

        input.use { source ->
            FileOutputStream(file).use { output ->
                source.copyTo(output)
            }
        }

        return file
    }

    private fun getFileName(uri: Uri): String {
        var name = "surat"

        requireContext().contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index) ?: name
            }
        }

        return name
    }

    // =========================================================
    // DIALOG & TAMPILAN DAFTAR
    // =========================================================

    private fun returnToList() {
        formMode = null
        currentSurat = null
        selectedFileUri = null

        formDialog?.setOnDismissListener(null)
        formDialog?.dismiss()
        formDialog = null

        clearFormReferences()

        if (_binding != null) {
            showListControls(true)
            showEmptyState(suratAdapter.itemCount == 0)
        }
    }

    private fun removeFormView() {
        formDialog?.setOnDismissListener(null)
        formDialog?.dismiss()
        formDialog = null

        clearFormReferences()
    }

    private fun clearFormReferences() {
        formScrollView = null
        formContainer = null

        etNomorSurat = null
        etTanggalSurat = null
        etTanggalDiterima = null
        etAsalSurat = null
        etPerihal = null
        etIsiRingkas = null
        etTanggalAcara = null
        etWaktuMulai = null
        etWaktuSelesai = null
        etLokasi = null

        spinnerStatus = null
        spinnerTim = null
        tvFileName = null
        btnPilihFile = null
        btnSimpanForm = null
        btnBatalForm = null
    }

    private fun showListControls(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE

        binding.cardSearchSurat.visibility = visibility
        binding.statusFilterScroll.visibility = visibility
        binding.btnTambahSurat.visibility = visibility

        if (!show) {
            binding.rvSuratMasuk.visibility = View.GONE
            binding.tvEmptySurat.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        if (_binding == null) return

        if (show) {
            binding.loadingAnimation.visibility = View.VISIBLE
            binding.loadingAnimation.playAnimation()
        } else {
            binding.loadingAnimation.cancelAnimation()
            binding.loadingAnimation.visibility = View.GONE
        }
    }

    private fun setFormLoading(loading: Boolean) {
        btnSimpanForm?.isEnabled = !loading
        btnBatalForm?.isEnabled = !loading
        btnPilihFile?.isEnabled = !loading

        spinnerStatus?.isEnabled = false

        spinnerTim?.isEnabled =
            !loading &&
                    formMode != FormMode.DETAIL &&
                    daftarTimAktif.isNotEmpty()

        showLoading(loading)
    }

    private fun showEmptyState(show: Boolean) {
        if (_binding == null || formMode != null) return

        binding.tvEmptySurat.visibility =
            if (show) View.VISIBLE else View.GONE

        binding.rvSuratMasuk.visibility =
            if (show) View.GONE else View.VISIBLE
    }

    // =========================================================
    // UTILITAS
    // =========================================================

    private fun todayDate(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Calendar.getInstance().time)
    }

    private fun value(editText: EditText?): String {
        return editText?.text?.toString()?.trim().orEmpty()
    }

    private fun nullableValue(editText: EditText?): String? {
        return value(editText).ifBlank { null }
    }

    private fun String.toTextRequestBody(): RequestBody {
        return toRequestBody("text/plain".toMediaType())
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        context?.let {
            Toast.makeText(
                it,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // ADAPTER
    // =========================================================

    private inner class SuratAdapter :
        RecyclerView.Adapter<SuratAdapter.SuratViewHolder>() {

        private val items = mutableListOf<SuratMasuk>()

        inner class SuratViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            val nomor: TextView =
                view.findViewById(R.id.tvNomorSurat)

            val asal: TextView =
                view.findViewById(R.id.tvAsalSurat)

            val status: TextView =
                view.findViewById(R.id.tvStatusSurat)

            val perihal: TextView =
                view.findViewById(R.id.tvPerihalSurat)

            val tanggalDiterima: TextView =
                view.findViewById(R.id.tvTanggalDiterima)

            val tanggalAcara: TextView =
                view.findViewById(R.id.tvTanggalAcara)

            val detail: TextView =
                view.findViewById(R.id.btnDetailSurat)

            val edit: TextView =
                view.findViewById(R.id.btnEditSurat)

            val hapus: TextView =
                view.findViewById(R.id.btnHapusSurat)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SuratViewHolder {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_surat_masuk,
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

            holder.nomor.text =
                surat.nomor_surat ?: "-"

            holder.asal.text =
                surat.asal_surat ?: "-"

            holder.perihal.text =
                surat.perihal ?: "-"

            holder.status.text =
                (surat.status ?: "baru").uppercase(Locale.ROOT)

            holder.tanggalDiterima.text =
                "Diterima: ${surat.tanggal_diterima ?: "-"}"

            holder.tanggalAcara.text =
                "Acara: ${surat.tanggal_acara ?: "-"}"

            holder.detail.setOnClickListener {
                showDetailForm(surat)
            }

            holder.edit.setOnClickListener {
                showEditForm(surat)
            }

            holder.hapus.setOnClickListener {
                deleteSuratMasuk(surat)
            }

            holder.itemView.animate().cancel()
            holder.itemView.alpha = 0f
            holder.itemView.translationY = dp(18).toFloat()

            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator())
                .setStartDelay(
                    position.coerceAtMost(6) * 25L
                )
                .start()
        }

        override fun getItemCount(): Int = items.size

        fun updateData(data: List<SuratMasuk>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }
}