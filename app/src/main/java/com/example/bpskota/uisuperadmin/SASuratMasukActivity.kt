package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.SuratMasuk
import com.example.bpskota.bpskp.model.SuratMasukActionResponse
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivitySasuratMasukBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class SASuratMasukActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySasuratMasukBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    private lateinit var adapter: SuratMasukAdapter

    private var firstLoad = true

    // =========================================================
    // FORM MODE
    // =========================================================

    private enum class FormMode {
        CREATE, EDIT, SHOW
    }

    private var currentFormMode: FormMode? = null
    private var currentSurat: SuratMasuk? = null

    // =========================================================
    // FORM VIEW
    // =========================================================

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

    private var tvFileName: TextView? = null
    private var btnPilihFile: TextView? = null

    private var btnCancelForm: TextView? = null
    private var btnSaveForm: TextView? = null

    private var selectedFileUri: Uri? = null

    // =========================================================
    // FILE EXECUTOR
    // =========================================================

    private val fileExecutor = Executors.newSingleThreadExecutor()

    // =========================================================
    // FILE PICKER
    // =========================================================

    private val filePickerLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) return@registerForActivityResult

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            selectedFileUri = uri

            tvFileName?.text = getFileName(uri)
            tvFileName?.setTextColor(Color.parseColor("#F97316"))

            // File baru dipilih, jangan gunakan listener file lama.
            tvFileName?.setOnClickListener(null)
            tvFileName?.isClickable = false
        }

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySasuratMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        setupLoading()
        setupRecyclerView()
        setupActions()

        updateFilterUI(binding.filterSemua)
        loadSuratMasuk()
        findViewById<ImageView>(R.id.btnBackSuratMasuk).setOnClickListener {
            finish()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        try {
            fileExecutor.shutdownNow()
        } catch (_: Exception) {
        }
    }

    // =========================================================
    // LOADING
    // =========================================================

    private fun setupLoading() {
        binding.loadingAnimation.setAnimation("Loading_Animation.json")
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private fun setupRecyclerView() {

        adapter = SuratMasukAdapter()

        binding.rvSuratMasuk.layoutManager = LinearLayoutManager(this)
        binding.rvSuratMasuk.adapter = adapter
        binding.rvSuratMasuk.setHasFixedSize(true)
    }

    // =========================================================
    // ACTIONS & FILTER
    // =========================================================

    private fun setupActions() {

        binding.btnTambahSurat.setOnTouchListener { v, event ->

            when (event.action) {

                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .start()
                }

                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
            }

            false
        }

        binding.btnTambahSurat.setOnClickListener {
            showCreateForm()
        }

        binding.etSearchSurat.setOnEditorActionListener { _, _, _ ->

            val search =
                binding.etSearchSurat.text
                    .toString()
                    .trim()
                    .ifBlank { null }

            loadSuratMasuk(search = search)

            true
        }

        binding.filterSemua.setOnClickListener {

            updateFilterUI(it)
            loadSuratMasuk()
        }

        binding.filterBaru.setOnClickListener {

            updateFilterUI(it)
            loadSuratMasuk(status = "baru")
        }

        binding.filterDiproses.setOnClickListener {

            updateFilterUI(it)
            loadSuratMasuk(status = "diproses")
        }

        binding.filterSelesai.setOnClickListener {

            updateFilterUI(it)
            loadSuratMasuk(status = "selesai")
        }
    }

    private fun updateFilterUI(selectedView: View) {

        val filters = listOf(
            binding.filterSemua,
            binding.filterBaru,
            binding.filterDiproses,
            binding.filterSelesai
        )

        filters.forEach { view ->

            if (view == selectedView) {

                view.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator())
                    .start()

                if (view is TextView) {
                    view.setTextColor(Color.WHITE)
                }

                view.background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#F97316"))
                        cornerRadius = dp(24).toFloat()
                    }

            } else {

                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                if (view is TextView) {
                    view.setTextColor(Color.parseColor("#4B5563"))
                }

                view.background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#F3F4F6"))
                        cornerRadius = dp(24).toFloat()
                    }
            }
        }
    }

    // =========================================================
    // LOAD LIST
    // =========================================================

    private fun loadSuratMasuk(
        search: String? = null,
        status: String? = null
    ) {

        removeFormView()
        showLoading(true)

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {

            showLoading(false)

            adapter.updateData(emptyList())
            showEmptyState(true)

            Toast.makeText(
                this,
                "Sesi login tidak ditemukan.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        repository.getSuratMasuk(
            authorization = "Bearer $token",
            search = search,
            status = status
        ) { data, error ->

            runOnUiThread {

                showLoading(false)

                if (error != null) {

                    adapter.updateData(emptyList())
                    showEmptyState(true)

                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()

                    return@runOnUiThread
                }

                val suratList = data ?: emptyList()

                adapter.updateData(suratList)
                showEmptyState(suratList.isEmpty())
            }
        }
    }

    // =========================================================
    // CREATE / EDIT / SHOW
    // =========================================================

    private fun showCreateForm() {

        currentFormMode = FormMode.CREATE
        currentSurat = null
        selectedFileUri = null

        createFormView(
            title = "Tambah Surat Masuk",
            subtitle = "Isi data surat masuk yang akan dicatat.",
            saveText = "Simpan"
        )

        clearForm()

        val today = getTodayDate()

        etTanggalSurat?.setText(today)
        etTanggalDiterima?.setText(today)

        setStatus("baru")
        setFormEditable(true)
    }

    private fun showEditForm(surat: SuratMasuk) {

        currentFormMode = FormMode.EDIT
        currentSurat = surat
        selectedFileUri = null

        createFormView(
            title = "Edit Surat Masuk",
            subtitle = "Perbarui informasi surat masuk.",
            saveText = "Simpan Perubahan"
        )

        fillForm(surat)

        setFormEditable(true)

        /*
         * File lama tetap dapat dibuka dari mode edit.
         * Tetapi tombol "Cari" tetap aktif untuk mengganti file.
         */
        setupExistingFileClick(surat)
    }

    private fun showDetailForm(surat: SuratMasuk) {

        currentFormMode = FormMode.SHOW
        currentSurat = surat
        selectedFileUri = null

        createFormView(
            title = "Detail Surat Masuk",
            subtitle = "Informasi lengkap surat masuk.",
            saveText = "Kembali"
        )

        fillForm(surat)
        setFormEditable(false)

        btnPilihFile?.visibility = View.GONE
        btnSaveForm?.text = "Kembali"

        setupExistingFileClick(surat)
    }

    // =========================================================
    // EXISTING FILE CLICK
    // =========================================================

    private fun setupExistingFileClick(surat: SuratMasuk) {

        val fileUrl = surat.file_url

        if (fileUrl.isNullOrBlank()) {

            tvFileName?.isClickable = false
            tvFileName?.setOnClickListener(null)

            return
        }

        tvFileName?.isClickable = true
        tvFileName?.setTextColor(Color.parseColor("#F97316"))

        tvFileName?.setOnClickListener {

            showFileDialog(surat)
        }
    }

    // =========================================================
    // BUILD FORM
    // =========================================================

    private fun createFormView(
        title: String,
        subtitle: String,
        saveText: String
    ) {

        hideListViews()
        removeFormView()

        val root = binding.root as ViewGroup

        // =====================================================
        // SCROLL VIEW
        // =====================================================

        formScrollView = ScrollView(this).apply {

            layoutParams =
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
                ).apply {

                    topToBottom = R.id.cardHeaderSuratMasuk
                    bottomToBottom =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                }

            setFillViewport(true)

            alpha = 0f
            translationY = dp(50).toFloat()

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        formContainer = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(24),
                dp(24),
                dp(24),
                dp(40)
            )

            setBackgroundColor(Color.WHITE)
        }

        formScrollView!!.addView(formContainer)

        root.addView(formScrollView)

        // =====================================================
        // TITLE
        // =====================================================

        val tvTitle = TextView(this).apply {

            text = title
            textSize = 24f

            setTextColor(
                Color.parseColor("#111827")
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
        }

        formContainer!!.addView(tvTitle)

        // =====================================================
        // SUBTITLE
        // =====================================================

        val tvSubtitle = TextView(this).apply {

            text = subtitle
            textSize = 13f

            setTextColor(
                Color.parseColor("#6B7280")
            )

            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

            params.topMargin = dp(4)
            params.bottomMargin = dp(16)

            formContainer!!.addView(
                this,
                params
            )
        }

        // =====================================================
        // INPUT
        // =====================================================

        addLabel("Nomor Surat")

        etNomorSurat =
            createTextInput(
                hint = "Masukkan nomor surat",
                inputType = InputType.TYPE_CLASS_TEXT
            )

        addLabel("Tanggal Surat")

        etTanggalSurat =
            createDateInput("Pilih tanggal surat")

        addLabel("Tanggal Diterima")

        etTanggalDiterima =
            createDateInput("Pilih tanggal diterima")

        addLabel("Asal Surat")

        etAsalSurat =
            createTextInput(
                hint = "Masukkan asal surat",
                inputType = InputType.TYPE_CLASS_TEXT
            )

        addLabel("Perihal")

        etPerihal =
            createTextInput(
                hint = "Masukkan perihal",
                inputType = InputType.TYPE_CLASS_TEXT
            )

        addLabel("Isi Ringkas")

        etIsiRingkas =
            createTextInput(
                hint = "Masukkan isi ringkas surat",
                inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
            ).apply {

                minLines = 4

                gravity = Gravity.TOP

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(16)
                )
            }

        addLabel("Tanggal Acara")

        etTanggalAcara =
            createDateInput("Pilih tanggal acara")

        addLabel("Waktu Mulai")

        etWaktuMulai =
            createTimeInput("Pilih waktu mulai")

        addLabel("Waktu Selesai")

        etWaktuSelesai =
            createTimeInput("Pilih waktu selesai")

        addLabel("Lokasi")

        etLokasi =
            createTextInput(
                hint = "Masukkan lokasi acara",
                inputType = InputType.TYPE_CLASS_TEXT
            )

        addLabel("Status")

        spinnerStatus = createStatusSpinner()

        // =====================================================
        // FILE SURAT
        // =====================================================

        addLabel("File Surat")

        val fileContainer =
            LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER_VERTICAL

                val params =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                params.topMargin = dp(8)

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        setColor(
                            Color.parseColor("#F9FAFB")
                        )

                        setStroke(
                            dp(1),
                            Color.parseColor("#E5E7EB")
                        )

                        cornerRadius =
                            dp(12).toFloat()
                    }

                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )

                formContainer!!.addView(
                    this,
                    params
                )
            }

        tvFileName =
            TextView(this).apply {

                text = "Belum ada file dipilih"
                textSize = 13f

                setTextColor(
                    Color.parseColor("#6B7280")
                )

                val params =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                fileContainer.addView(
                    this,
                    params
                )
            }

        btnPilihFile =
            TextView(this).apply {

                text = "Cari"
                textSize = 12f

                setTextColor(
                    Color.parseColor("#F97316")
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                gravity = Gravity.CENTER

                setPadding(
                    dp(16),
                    dp(8),
                    dp(16),
                    dp(8)
                )

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        setColor(
                            Color.parseColor("#FFF7ED")
                        )

                        cornerRadius =
                            dp(8).toFloat()
                    }

                setOnClickListener {

                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/jpeg",
                            "image/png"
                        )
                    )
                }

                val params =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                params.leftMargin = dp(8)

                fileContainer.addView(
                    this,
                    params
                )
            }

        // =====================================================
        // ACTION BUTTONS
        // =====================================================

        val actionContainer =
            LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.END

                val params =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                params.topMargin = dp(32)

                formContainer!!.addView(
                    this,
                    params
                )
            }

        btnCancelForm =
            TextView(this).apply {

                text = "Batal"
                textSize = 14f

                setTextColor(
                    Color.parseColor("#6B7280")
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                gravity = Gravity.CENTER

                setPadding(
                    dp(20),
                    dp(14),
                    dp(20),
                    dp(14)
                )

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        setColor(Color.TRANSPARENT)

                        cornerRadius =
                            dp(12).toFloat()
                    }

                setOnClickListener {

                    showList()
                }
            }

        actionContainer.addView(
            btnCancelForm
        )

        btnSaveForm =
            TextView(this).apply {

                text = saveText
                textSize = 14f

                setTextColor(Color.WHITE)

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                gravity = Gravity.CENTER

                setPadding(
                    dp(24),
                    dp(14),
                    dp(24),
                    dp(14)
                )

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        setColor(
                            Color.parseColor("#F97316")
                        )

                        cornerRadius =
                            dp(12).toFloat()
                    }

                val params =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                params.leftMargin = dp(12)

                actionContainer.addView(
                    this,
                    params
                )

                setOnClickListener {

                    animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction {

                            animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()

                            when (currentFormMode) {

                                FormMode.CREATE ->
                                    createSuratMasuk()

                                FormMode.EDIT ->
                                    updateSuratMasuk()

                                FormMode.SHOW ->
                                    showList()

                                null -> Unit
                            }
                        }
                        .start()
                }
            }
    }

    // =========================================================
    // FORM HELPERS
    // =========================================================

    private fun addLabel(text: String) {

        val label = TextView(this).apply {

            this.text = text
            textSize = 13f

            setTextColor(
                Color.parseColor("#4B5563")
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

            params.topMargin = dp(16)
            params.bottomMargin = dp(6)

            formContainer!!.addView(
                this,
                params
            )
        }
    }

    private fun createInputBackground():
            android.graphics.drawable.Drawable {

        return android.graphics.drawable.GradientDrawable().apply {

            setColor(
                Color.parseColor("#F9FAFB")
            )

            setStroke(
                dp(1),
                Color.parseColor("#E5E7EB")
            )

            cornerRadius =
                dp(12).toFloat()
        }
    }

    private fun createTextInput(
        hint: String,
        inputType: Int
    ): EditText {

        val editText =
            EditText(this).apply {

                this.hint = hint
                textSize = 14f

                this.inputType = inputType

                setSingleLine(
                    inputType and
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0
                )

                setTextColor(
                    Color.parseColor("#111827")
                )

                setHintTextColor(
                    Color.parseColor("#9CA3AF")
                )

                setPadding(
                    dp(16),
                    0,
                    dp(16),
                    0
                )

                background =
                    createInputBackground()
            }

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )

        formContainer!!.addView(
            editText,
            params
        )

        return editText
    }

    private fun createDateInput(
        hint: String
    ): EditText {

        val editText =
            EditText(this).apply {

                this.hint = hint
                textSize = 14f

                isFocusable = false
                isClickable = true

                setTextColor(
                    Color.parseColor("#111827")
                )

                setHintTextColor(
                    Color.parseColor("#9CA3AF")
                )

                setPadding(
                    dp(16),
                    0,
                    dp(16),
                    0
                )

                background =
                    createInputBackground()

                setOnClickListener {

                    if (currentFormMode != FormMode.SHOW) {
                        showDatePicker(this)
                    }
                }
            }

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )

        formContainer!!.addView(
            editText,
            params
        )

        return editText
    }

    private fun createTimeInput(
        hint: String
    ): EditText {

        val editText =
            EditText(this).apply {

                this.hint = hint
                textSize = 14f

                isFocusable = false
                isClickable = true

                setTextColor(
                    Color.parseColor("#111827")
                )

                setHintTextColor(
                    Color.parseColor("#9CA3AF")
                )

                setPadding(
                    dp(16),
                    0,
                    dp(16),
                    0
                )

                background =
                    createInputBackground()

                setOnClickListener {

                    if (currentFormMode != FormMode.SHOW) {
                        showTimePicker(this)
                    }
                }
            }

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )

        formContainer!!.addView(
            editText,
            params
        )

        return editText
    }

    private fun createStatusSpinner(): Spinner {

        val spinner = Spinner(this)

        val statuses =
            listOf(
                "baru",
                "diproses",
                "selesai"
            )

        val spinnerAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                statuses
            )

        spinner.adapter = spinnerAdapter

        spinner.setPadding(
            dp(10),
            0,
            dp(10),
            0
        )

        spinner.background =
            createInputBackground()

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )

        formContainer!!.addView(
            spinner,
            params
        )

        return spinner
    }

    // =========================================================
    // DATE / TIME
    // =========================================================

    private fun showDatePicker(target: EditText) {

        val calendar = Calendar.getInstance()

        val currentText =
            target.text.toString().trim()

        if (currentText.isNotBlank()) {

            try {

                val date =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).parse(currentText)

                if (date != null) {
                    calendar.time = date
                }

            } catch (_: Exception) {
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->

                val selected =
                    Calendar.getInstance()

                selected.set(
                    year,
                    month,
                    day
                )

                val formatted =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).format(selected.time)

                target.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(target: EditText) {

        val calendar = Calendar.getInstance()

        val currentText =
            target.text.toString().trim()

        if (currentText.isNotBlank()) {

            try {

                val time =
                    SimpleDateFormat(
                        "HH:mm",
                        Locale.US
                    ).parse(currentText)

                if (time != null) {
                    calendar.time = time
                }

            } catch (_: Exception) {
            }
        }

        TimePickerDialog(
            this,
            { _, hour, minute ->

                val formatted =
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        hour,
                        minute
                    )

                target.setText(formatted)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun getTodayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Calendar.getInstance().time)
    }

    // =========================================================
    // FILL / CLEAR FORM
    // =========================================================

    private fun fillForm(surat: SuratMasuk) {

        etNomorSurat?.setText(
            surat.nomor_surat ?: ""
        )

        etTanggalSurat?.setText(
            surat.tanggal_surat ?: ""
        )

        etTanggalDiterima?.setText(
            surat.tanggal_diterima ?: ""
        )

        etAsalSurat?.setText(
            surat.asal_surat ?: ""
        )

        etPerihal?.setText(
            surat.perihal ?: ""
        )

        etIsiRingkas?.setText(
            surat.isi_ringkas ?: ""
        )

        etTanggalAcara?.setText(
            surat.tanggal_acara ?: ""
        )

        etWaktuMulai?.setText(
            surat.waktu_mulai ?: ""
        )

        etWaktuSelesai?.setText(
            surat.waktu_selesai ?: ""
        )

        etLokasi?.setText(
            surat.lokasi ?: ""
        )

        setStatus(
            surat.status ?: "baru"
        )

        /*
         * Sekarang tampilkan nama file sebenarnya.
         */
        if (surat.file_surat.isNullOrBlank()) {

            tvFileName?.text = "Belum ada file"
            tvFileName?.setTextColor(
                Color.parseColor("#6B7280")
            )

        } else {

            val fileName =
                surat.file_surat
                    .substringAfterLast("/")

            tvFileName?.text =
                if (fileName.isBlank()) {
                    "File surat tersedia"
                } else {
                    fileName
                }

            tvFileName?.setTextColor(
                Color.parseColor("#F97316")
            )
        }
    }

    private fun clearForm() {

        etNomorSurat?.setText("")
        etTanggalSurat?.setText("")
        etTanggalDiterima?.setText("")
        etAsalSurat?.setText("")
        etPerihal?.setText("")
        etIsiRingkas?.setText("")
        etTanggalAcara?.setText("")
        etWaktuMulai?.setText("")
        etWaktuSelesai?.setText("")
        etLokasi?.setText("")

        tvFileName?.text =
            "Belum ada file dipilih"

        tvFileName?.setTextColor(
            Color.parseColor("#6B7280")
        )

        tvFileName?.setOnClickListener(null)
        tvFileName?.isClickable = false
    }

    private fun setStatus(status: String) {

        val spinner = spinnerStatus ?: return
        val adapter = spinner.adapter ?: return

        for (index in 0 until adapter.count) {

            if (
                adapter.getItem(index)
                    .toString()
                    .equals(
                        status,
                        ignoreCase = true
                    )
            ) {

                spinner.setSelection(index)
                break
            }
        }
    }

    private fun getSelectedStatus(): String {

        return spinnerStatus
            ?.selectedItem
            ?.toString()
            ?: "baru"
    }

    private fun setFormEditable(editable: Boolean) {

        listOf(
            etNomorSurat,
            etAsalSurat,
            etPerihal,
            etIsiRingkas,
            etLokasi
        ).forEach {

            it?.isEnabled = editable
        }

        etTanggalSurat?.isEnabled = editable
        etTanggalDiterima?.isEnabled = editable
        etTanggalAcara?.isEnabled = editable
        etWaktuMulai?.isEnabled = editable
        etWaktuSelesai?.isEnabled = editable

        spinnerStatus?.isEnabled = editable

        btnPilihFile?.isEnabled = editable

        if (!editable) {
            btnCancelForm?.text = "Kembali"
        }
    }

    // =========================================================
    // API - CREATE
    // =========================================================

    private fun createSuratMasuk() {

        if (!validateForm()) return

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Sesi login tidak ditemukan.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        showFormLoading(true)

        val filePart =
            createMultipartFile()

        repository.createSuratMasuk(
            authorization = "Bearer $token",
            nomorSurat = value(etNomorSurat),
            tanggalSurat = value(etTanggalSurat),
            tanggalDiterima = value(etTanggalDiterima),
            asalSurat = value(etAsalSurat),
            perihal = value(etPerihal),
            isiRingkas = nullableValue(etIsiRingkas),
            tanggalAcara = nullableValue(etTanggalAcara),
            waktuMulai = nullableValue(etWaktuMulai),
            waktuSelesai = nullableValue(etWaktuSelesai),
            lokasi = nullableValue(etLokasi),
            fileSurat = filePart
        ) { _, error ->

            runOnUiThread {

                showFormLoading(false)

                if (error != null) {

                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()

                    return@runOnUiThread
                }

                Toast.makeText(
                    this,
                    "Surat masuk berhasil disimpan.",
                    Toast.LENGTH_SHORT
                ).show()

                showList()
                loadSuratMasuk()
            }
        }
    }

    // =========================================================
    // API - UPDATE
    // =========================================================

    private fun updateSuratMasuk() {

        val surat = currentSurat
        val id = surat?.id

        if (id == null) {

            Toast.makeText(
                this,
                "ID surat tidak ditemukan.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!validateForm()) return

        val token = authSession.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Sesi login tidak ditemukan.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        showFormLoading(true)

        val filePart =
            createMultipartFile()

        BpskpRetrofitClient.api.updateSuratMasuk(
            authorization = "Bearer $token",
            id = id,

            nomorSurat =
            value(etNomorSurat)
                .toTextRequestBody(),

            tanggalSurat =
            value(etTanggalSurat)
                .toTextRequestBody(),

            tanggalDiterima =
            value(etTanggalDiterima)
                .toTextRequestBody(),

            asalSurat =
            value(etAsalSurat)
                .toTextRequestBody(),

            perihal =
            value(etPerihal)
                .toTextRequestBody(),

            isiRingkas =
            nullableValue(etIsiRingkas)
                ?.toTextRequestBody(),

            tanggalAcara =
            nullableValue(etTanggalAcara)
                ?.toTextRequestBody(),

            waktuMulai =
            nullableValue(etWaktuMulai)
                ?.toTextRequestBody(),

            waktuSelesai =
            nullableValue(etWaktuSelesai)
                ?.toTextRequestBody(),

            lokasi =
            nullableValue(etLokasi)
                ?.toTextRequestBody(),

            status =
            getSelectedStatus()
                .toTextRequestBody(),

            fileSurat = filePart

        ).enqueue(
            object : Callback<SuratMasukActionResponse> {

                override fun onResponse(
                    call: Call<SuratMasukActionResponse>,
                    response: Response<SuratMasukActionResponse>
                ) {

                    runOnUiThread {

                        showFormLoading(false)

                        if (response.isSuccessful) {

                            Toast.makeText(
                                this@SASuratMasukActivity,
                                response.body()?.message
                                    ?: "Surat berhasil diperbarui.",
                                Toast.LENGTH_SHORT
                            ).show()

                            showList()
                            loadSuratMasuk()

                        } else {

                            Toast.makeText(
                                this@SASuratMasukActivity,
                                "Gagal memperbarui surat.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<SuratMasukActionResponse>,
                    t: Throwable
                ) {

                    runOnUiThread {

                        showFormLoading(false)

                        Toast.makeText(
                            this@SASuratMasukActivity,
                            t.message
                                ?: "Koneksi gagal.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    // =========================================================
    // DELETE
    // =========================================================

    private fun deleteSuratMasuk(
        surat: SuratMasuk
    ) {

        val id = surat.id ?: return

        AlertDialog.Builder(this)
            .setTitle("Hapus Surat")
            .setMessage(
                "Apakah Anda yakin ingin menghapus surat ${surat.nomor_surat ?: "-"}?"
            )
            .setNegativeButton(
                "Batal",
                null
            )
            .setPositiveButton(
                "Hapus"
            ) { _, _ ->

                val token =
                    authSession.getToken()

                if (token.isNullOrBlank()) {

                    Toast.makeText(
                        this,
                        "Sesi login tidak ditemukan.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                showLoading(true)

                repository.deleteSuratMasuk(
                    authorization = "Bearer $token",
                    id = id
                ) { success, error ->

                    runOnUiThread {

                        showLoading(false)

                        if (error != null) {

                            Toast.makeText(
                                this,
                                error,
                                Toast.LENGTH_LONG
                            ).show()

                            return@runOnUiThread
                        }

                        if (success) {

                            Toast.makeText(
                                this,
                                "Surat berhasil dihapus.",
                                Toast.LENGTH_SHORT
                            ).show()

                            loadSuratMasuk()

                        } else {

                            Toast.makeText(
                                this,
                                "Gagal menghapus surat.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .show()
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private fun validateForm(): Boolean {

        if (value(etNomorSurat).isBlank()) {

            etNomorSurat?.error =
                "Nomor surat wajib diisi"

            etNomorSurat?.requestFocus()

            return false
        }

        if (value(etTanggalSurat).isBlank()) {

            Toast.makeText(
                this,
                "Tanggal surat wajib diisi.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        if (value(etTanggalDiterima).isBlank()) {

            Toast.makeText(
                this,
                "Tanggal diterima wajib diisi.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        if (value(etAsalSurat).isBlank()) {

            etAsalSurat?.error =
                "Asal surat wajib diisi"

            etAsalSurat?.requestFocus()

            return false
        }

        if (value(etPerihal).isBlank()) {

            etPerihal?.error =
                "Perihal wajib diisi"

            etPerihal?.requestFocus()

            return false
        }

        return true
    }

    // =========================================================
    // CREATE MULTIPART FILE
    // =========================================================

    private fun createMultipartFile():
            MultipartBody.Part? {

        val uri =
            selectedFileUri
                ?: return null

        return try {

            val file =
                copyUriToCache(uri)

            val mimeType =
                contentResolver.getType(uri)
                    ?: "application/octet-stream"

            val requestBody =
                file.asRequestBody(
                    mimeType.toMediaType()
                )

            MultipartBody.Part.createFormData(
                "file_surat",
                file.name,
                requestBody
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Gagal membaca file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            null
        }
    }

    private fun copyUriToCache(
        uri: Uri
    ): File {

        val fileName =
            getFileName(uri)

        val file =
            File(
                cacheDir,
                "${System.currentTimeMillis()}_$fileName"
            )

        contentResolver
            .openInputStream(uri)
            .use { input ->

                FileOutputStream(file)
                    .use { output ->

                        input?.copyTo(output)
                    }
            }

        return file
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var name = "surat"

        contentResolver
            .query(
                uri,
                null,
                null,
                null,
                null
            )
            ?.use { cursor ->

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (
                    index >= 0 &&
                    cursor.moveToFirst()
                ) {

                    name =
                        cursor.getString(index)
                }
            }

        return name
    }

    // =========================================================
    // FILE DIALOG
    // =========================================================

    private fun showFileDialog(
        surat: SuratMasuk
    ) {

        val fileUrl =
            surat.file_url

        if (fileUrl.isNullOrBlank()) {

            Toast.makeText(
                this,
                "File surat tidak tersedia.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val fileName =
            surat.file_surat
                ?.substringAfterLast("/")
                ?.ifBlank { "File Surat" }
                ?: "File Surat"

        val extension =
            fileName
                .substringAfterLast(
                    ".",
                    ""
                )
                .lowercase(Locale.US)

        when (extension) {

            "jpg",
            "jpeg",
            "png" -> {

                showImageFileDialog(
                    fileName,
                    fileUrl
                )
            }

            "pdf" -> {

                showPdfFileDialog(
                    fileName,
                    fileUrl
                )
            }

            else -> {

                showGenericFileDialog(
                    fileName,
                    fileUrl
                )
            }
        }
    }

    // =========================================================
    // IMAGE FILE DIALOG
    // =========================================================

    private fun showImageFileDialog(
        fileName: String,
        fileUrl: String
    ) {

        val imageView =
            ImageView(this).apply {

                adjustViewBounds = true

                scaleType =
                    ImageView.ScaleType.FIT_CENTER

                setBackgroundColor(
                    Color.parseColor("#F9FAFB")
                )

                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )
            }

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(4)
                )

                addView(
                    imageView,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(360)
                    )
                )
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(fileName)
                .setView(container)
                .setNegativeButton(
                    "Tutup",
                    null
                )
                .setPositiveButton(
                    "Download",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                downloadFile(
                    fileUrl,
                    fileName
                )
            }
        }

        dialog.show()

        loadImageIntoView(
            imageView,
            fileUrl,
            dialog
        )
    }

    // =========================================================
    // LOAD IMAGE
    // =========================================================

    private fun loadImageIntoView(
        imageView: ImageView,
        fileUrl: String,
        dialog: AlertDialog
    ) {

        val progressBar =
            ProgressBar(this).apply {

                isIndeterminate = true
            }

        val parent =
            imageView.parent as? ViewGroup

        parent?.addView(
            progressBar,
            0,
            LinearLayout.LayoutParams(
                dp(40),
                dp(40)
            ).apply {

                gravity = Gravity.CENTER
            }
        )

        fileExecutor.execute {

            var connection:
                    HttpURLConnection? = null

            try {

                val url =
                    URL(fileUrl)

                connection =
                    url.openConnection()
                            as HttpURLConnection

                val token =
                    authSession.getToken()

                if (!token.isNullOrBlank()) {

                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )
                }

                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"

                val responseCode =
                    connection.responseCode

                if (responseCode !in 200..299) {
                    throw Exception(
                        "HTTP $responseCode"
                    )
                }

                val bitmap =
                    connection
                        .inputStream
                        .use {
                            BitmapFactory.decodeStream(it)
                        }

                runOnUiThread {

                    parent?.removeView(
                        progressBar
                    )

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                            bitmap
                        )

                    } else {

                        imageView.setImageResource(
                            android.R.drawable.ic_dialog_alert
                        )

                        Toast.makeText(
                            this,
                            "Gagal membaca gambar.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    parent?.removeView(
                        progressBar
                    )

                    imageView.setImageResource(
                        android.R.drawable.ic_dialog_alert
                    )

                    Toast.makeText(
                        this,
                        "Gagal memuat gambar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                connection?.disconnect()
            }
        }
    }

    // =========================================================
    // PDF DIALOG
    // =========================================================

    private fun showPdfFileDialog(
        fileName: String,
        fileUrl: String
    ) {

        val message =
            TextView(this).apply {

                text =
                    "File PDF siap dibuka atau diunduh.\n\n$fileName"

                textSize = 14f

                setTextColor(
                    Color.parseColor("#374151")
                )

                setPadding(
                    dp(24),
                    dp(8),
                    dp(24),
                    dp(8)
                )
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("File Surat")
                .setView(message)
                .setNegativeButton(
                    "Tutup",
                    null
                )
                .setNeutralButton(
                    "Download",
                    null
                )
                .setPositiveButton(
                    "Buka PDF",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_NEUTRAL
            ).setOnClickListener {

                downloadFile(
                    fileUrl,
                    fileName
                )
            }

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                showFileLoadingDialog(
                    fileName,
                    fileUrl
                )
            }
        }

        dialog.show()
    }

    // =========================================================
    // GENERIC FILE DIALOG
    // =========================================================

    private fun showGenericFileDialog(
        fileName: String,
        fileUrl: String
    ) {

        val message =
            TextView(this).apply {

                text =
                    "File surat:\n\n$fileName"

                textSize = 14f

                setTextColor(
                    Color.parseColor("#374151")
                )

                setPadding(
                    dp(24),
                    dp(8),
                    dp(24),
                    dp(8)
                )
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("File Surat")
                .setView(message)
                .setNegativeButton(
                    "Tutup",
                    null
                )
                .setNeutralButton(
                    "Download",
                    null
                )
                .setPositiveButton(
                    "Buka",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_NEUTRAL
            ).setOnClickListener {

                downloadFile(
                    fileUrl,
                    fileName
                )
            }

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                showFileLoadingDialog(
                    fileName,
                    fileUrl
                )
            }
        }

        dialog.show()
    }

    // =========================================================
    // FILE LOADING DIALOG
    // =========================================================

    private fun showFileLoadingDialog(
        fileName: String,
        fileUrl: String
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(24),
                    dp(16),
                    dp(24),
                    dp(16)
                )
            }

        val progress =
            ProgressBar(this)

        val text =
            TextView(this).apply {

                this.text =
                    "Menyiapkan file..."

                textSize = 14f

                setTextColor(
                    Color.parseColor("#374151")
                )

                setPadding(
                    dp(16),
                    0,
                    0,
                    0
                )
            }

        container.addView(
            progress,
            LinearLayout.LayoutParams(
                dp(32),
                dp(32)
            )
        )

        container.addView(
            text,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(fileName)
                .setView(container)
                .setNegativeButton(
                    "Batal",
                    null
                )
                .create()

        dialog.show()

        downloadFileToCacheAndOpen(
            fileName,
            fileUrl,
            dialog
        )
    }

    // =========================================================
    // DOWNLOAD FILE TO CACHE + OPEN
    // =========================================================

    private fun downloadFileToCacheAndOpen(
        fileName: String,
        fileUrl: String,
        loadingDialog: AlertDialog
    ) {

        fileExecutor.execute {

            var connection:
                    HttpURLConnection? = null

            try {

                val safeFileName =
                    sanitizeFileName(fileName)

                val outputFile =
                    File(
                        cacheDir,
                        "surat_${System.currentTimeMillis()}_$safeFileName"
                    )

                val url =
                    URL(fileUrl)

                connection =
                    url.openConnection()
                            as HttpURLConnection

                val token =
                    authSession.getToken()

                if (!token.isNullOrBlank()) {

                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )
                }

                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"

                val responseCode =
                    connection.responseCode

                if (responseCode !in 200..299) {

                    throw Exception(
                        "Server mengembalikan HTTP $responseCode"
                    )
                }

                connection.inputStream.use { input ->

                    FileOutputStream(
                        outputFile
                    ).use { output ->

                        input.copyTo(output)
                    }
                }

                runOnUiThread {

                    loadingDialog.dismiss()

                    openDownloadedFile(
                        outputFile,
                        fileName
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    loadingDialog.dismiss()

                    Toast.makeText(
                        this,
                        "Gagal membuka file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                connection?.disconnect()
            }
        }
    }

    // =========================================================
    // OPEN DOWNLOADED FILE
    // =========================================================

    private fun openDownloadedFile(
        file: File,
        originalFileName: String
    ) {

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )

            val mimeType =
                getMimeType(
                    originalFileName
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        mimeType
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            try {

                startActivity(intent)

            } catch (_: Exception) {

                Toast.makeText(
                    this,
                    "Tidak ada aplikasi yang dapat membuka file ini.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Gagal membuka file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // DOWNLOAD TO DEVICE
    // =========================================================

    private fun downloadFile(
        fileUrl: String,
        fileName: String
    ) {

        try {

            val request =
                DownloadManager.Request(
                    Uri.parse(fileUrl)
                )

            request.setTitle(
                fileName
            )

            request.setDescription(
                "Mengunduh file surat masuk"
            )

            request.setNotificationVisibility(
                DownloadManager.Request
                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            request.setMimeType(
                getMimeType(fileName)
            )

            /*
             * Jika endpoint storage membutuhkan authentication,
             * Bearer token dikirim ke DownloadManager.
             *
             * Jika storage Laravel bersifat public,
             * header ini tidak masalah dan tetap dapat diabaikan server.
             */
            val token =
                authSession.getToken()

            if (!token.isNullOrBlank()) {

                request.addRequestHeader(
                    "Authorization",
                    "Bearer $token"
                )
            }

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                sanitizeFileName(fileName)
            )

            val downloadManager =
                getSystemService(
                    Context.DOWNLOAD_SERVICE
                ) as DownloadManager

            downloadManager.enqueue(request)

            Toast.makeText(
                this,
                "Download dimulai. File akan disimpan di folder Download.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Gagal memulai download: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // FILE UTILITIES
    // =========================================================

    private fun getMimeType(
        fileName: String
    ): String {

        val extension =
            fileName
                .substringAfterLast(
                    ".",
                    ""
                )
                .lowercase(Locale.US)

        return when (extension) {

            "pdf" ->
                "application/pdf"

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "txt" ->
                "text/plain"

            "doc" ->
                "application/msword"

            "docx" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

            "xls" ->
                "application/vnd.ms-excel"

            "xlsx" ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            else ->
                MimeTypeMap
                    .getSingleton()
                    .getMimeTypeFromExtension(
                        extension
                    )
                    ?: "application/octet-stream"
        }
    }

    private fun sanitizeFileName(
        fileName: String
    ): String {

        return fileName
            .replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            )
            .ifBlank {
                "surat"
            }
    }

    // =========================================================
    // VIEW TOGGLES
    // =========================================================

    private fun hideListViews() {

        binding.cardSearchSurat.visibility =
            View.GONE

        binding.statusFilterScroll.visibility =
            View.GONE

        binding.loadingAnimation.visibility =
            View.GONE

        binding.tvEmptySurat.visibility =
            View.GONE

        binding.rvSuratMasuk.visibility =
            View.GONE

        binding.btnTambahSurat.visibility =
            View.GONE
    }

    private fun showList() {

        formScrollView
            ?.animate()
            ?.alpha(0f)
            ?.translationY(
                dp(50).toFloat()
            )
            ?.setDuration(300)
            ?.withEndAction {

                currentFormMode = null
                currentSurat = null
                selectedFileUri = null

                removeFormView()

                binding.cardSearchSurat.visibility =
                    View.VISIBLE

                binding.statusFilterScroll.visibility =
                    View.VISIBLE

                binding.btnTambahSurat.visibility =
                    View.VISIBLE

                showEmptyState(
                    adapter.itemCount == 0
                )
            }
            ?.start()
            ?: run {

                removeFormView()

                binding.cardSearchSurat.visibility =
                    View.VISIBLE

                binding.statusFilterScroll.visibility =
                    View.VISIBLE

                binding.btnTambahSurat.visibility =
                    View.VISIBLE

                showEmptyState(
                    adapter.itemCount == 0
                )
            }
    }

    private fun removeFormView() {

        formScrollView?.let { view ->

            (
                    view.parent
                            as? ViewGroup
                    )?.removeView(view)
        }

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

        tvFileName = null
        btnPilihFile = null

        btnCancelForm = null
        btnSaveForm = null
    }

    // =========================================================
    // LOADING & EMPTY
    // =========================================================

    private fun showFormLoading(
        loading: Boolean
    ) {

        btnSaveForm?.isEnabled =
            !loading

        btnCancelForm?.isEnabled =
            !loading

        btnPilihFile?.isEnabled =
            !loading

        if (loading) {

            binding.loadingAnimation.visibility =
                View.VISIBLE

            binding.loadingAnimation.playAnimation()

        } else {

            binding.loadingAnimation.cancelAnimation()

            binding.loadingAnimation.visibility =
                View.GONE
        }
    }

    private fun showLoading(
        show: Boolean
    ) {

        if (show) {

            binding.rvSuratMasuk.visibility =
                View.GONE

            binding.tvEmptySurat.visibility =
                View.GONE

            binding.loadingAnimation.visibility =
                View.VISIBLE

            binding.loadingAnimation.playAnimation()

        } else {

            binding.loadingAnimation.cancelAnimation()

            binding.loadingAnimation.visibility =
                View.GONE

            binding.rvSuratMasuk.visibility =
                View.VISIBLE
        }
    }

    private fun showEmptyState(
        show: Boolean
    ) {

        if (show) {

            binding.tvEmptySurat.visibility =
                View.VISIBLE

            binding.rvSuratMasuk.visibility =
                View.GONE

        } else {

            binding.tvEmptySurat.visibility =
                View.GONE

            binding.rvSuratMasuk.visibility =
                View.VISIBLE
        }
    }

    // =========================================================
    // UTILS
    // =========================================================

    private fun value(
        editText: EditText?
    ): String {

        return editText
            ?.text
            ?.toString()
            ?.trim()
            ?: ""
    }

    private fun nullableValue(
        editText: EditText?
    ): String? {

        return value(editText)
            .ifBlank {
                null
            }
    }

    private fun String.toTextRequestBody():
            okhttp3.RequestBody {

        return toRequestBody(
            "text/plain".toMediaType()
        )
    }

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (
            !firstLoad &&
            currentFormMode == null
        ) {

            loadSuratMasuk()
        }

        firstLoad = false
    }

    // =========================================================
    // ADAPTER
    // =========================================================

    private inner class SuratMasukAdapter :
        RecyclerView.Adapter<
                SuratMasukAdapter.SuratViewHolder
                >() {

        private val items =
            mutableListOf<SuratMasuk>()

        inner class SuratViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            val tvNomorSurat: TextView =
                itemView.findViewById(
                    R.id.tvNomorSurat
                )

            val tvAsalSurat: TextView =
                itemView.findViewById(
                    R.id.tvAsalSurat
                )

            val tvStatusSurat: TextView =
                itemView.findViewById(
                    R.id.tvStatusSurat
                )

            val tvPerihalSurat: TextView =
                itemView.findViewById(
                    R.id.tvPerihalSurat
                )

            val tvTanggalDiterima: TextView =
                itemView.findViewById(
                    R.id.tvTanggalDiterima
                )

            val tvTanggalAcara: TextView =
                itemView.findViewById(
                    R.id.tvTanggalAcara
                )

            val btnDetailSurat: TextView =
                itemView.findViewById(
                    R.id.btnDetailSurat
                )

            val btnEditSurat: TextView =
                itemView.findViewById(
                    R.id.btnEditSurat
                )

            val btnHapusSurat: TextView =
                itemView.findViewById(
                    R.id.btnHapusSurat
                )
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SuratViewHolder {

            val view =
                LayoutInflater
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

            val item =
                items[position]

            holder.tvNomorSurat.text =
                item.nomor_surat ?: "-"

            holder.tvAsalSurat.text =
                item.asal_surat ?: "-"

            holder.tvPerihalSurat.text =
                item.perihal ?: "-"

            holder.tvStatusSurat.text =
                (
                        item.status
                            ?: "baru"
                        ).uppercase()

            holder.tvTanggalDiterima.text =
                "Diterima: ${item.tanggal_diterima ?: "-"}"

            holder.tvTanggalAcara.text =
                "Acara: ${item.tanggal_acara ?: "-"}"

            holder.btnDetailSurat.setOnClickListener {

                showDetailForm(item)
            }

            holder.btnEditSurat.setOnClickListener {

                showEditForm(item)
            }

            holder.btnHapusSurat.setOnClickListener {

                deleteSuratMasuk(item)
            }

            animateItem(
                holder.itemView,
                position
            )
        }

        override fun getItemCount(): Int =
            items.size

        fun updateData(
            newItems: List<SuratMasuk>
        ) {

            items.clear()

            items.addAll(
                newItems
            )

            notifyDataSetChanged()
        }

        private fun animateItem(
            view: View,
            position: Int
        ) {

            view.animate().cancel()

            view.alpha = 0f

            view.translationY =
                dp(40).toFloat()

            view.scaleX = 0.95f
            view.scaleY = 0.95f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400L)
                .setInterpolator(
                    OvershootInterpolator()
                )
                .setStartDelay(
                    position
                        .coerceAtMost(10)
                        .times(50L)
                )
                .start()
        }
    }
}