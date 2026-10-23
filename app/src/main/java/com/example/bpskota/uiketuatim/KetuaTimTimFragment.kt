package com.example.bpskota.uiketuatim

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.AnggotaTim
import com.example.bpskota.bpskp.model.PegawaiTersedia
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository

class KetuaTimTimFragment : Fragment() {

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var adapter: AnggotaAdapter

    private lateinit var tvJumlahAnggota: TextView
    private lateinit var btnTambahAnggota: TextView
    private lateinit var progressAnggotaTim: ProgressBar
    private lateinit var rvAnggotaTim: RecyclerView
    private lateinit var layoutAnggotaKosong: View

    private var currentTimId: Int? = null
    private var anggotaList: List<AnggotaTim> = emptyList()

    private var isLoading = false
    private var isLoadingPegawai = false
    private var isSubmitting = false

    private val orange = Color.parseColor("#F97316")
    private val orangeDark = Color.parseColor("#EA580C")
    private val backgroundColor = Color.parseColor("#F6F2EE")
    private val textColor = Color.parseColor("#292524")
    private val secondaryTextColor = Color.parseColor("#78716C")
    private val borderColor = Color.parseColor("#E7E5E4")
    private val red = Color.parseColor("#DC2626")
    private val green = Color.parseColor("#15803D")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_ketua_tim_tim,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        authSession = BpskpAuthSession(requireContext())
        repository = BpskpRepository()

        initViews(view)
        setupRecyclerView()
        setupListeners()

        loadTimKetua()
    }

    override fun onDestroyView() {
        isLoading = false
        isLoadingPegawai = false
        isSubmitting = false

        currentTimId = null
        anggotaList = emptyList()

        super.onDestroyView()
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================

    private fun initViews(view: View) {
        tvJumlahAnggota =
            view.findViewById(R.id.tvJumlahAnggota)

        btnTambahAnggota =
            view.findViewById(R.id.btnTambahAnggota)

        progressAnggotaTim =
            view.findViewById(R.id.progressAnggotaTim)

        rvAnggotaTim =
            view.findViewById(R.id.rvAnggotaTim)

        layoutAnggotaKosong =
            view.findViewById(R.id.layoutAnggotaKosong)

        progressAnggotaTim.indeterminateTintList =
            ColorStateList.valueOf(orange)

        tvJumlahAnggota.text = "0"

        progressAnggotaTim.visibility = View.GONE
        layoutAnggotaKosong.visibility = View.GONE
        rvAnggotaTim.visibility = View.VISIBLE

        // Perbaiki tampilan tombol tanpa mengubah XML.
        btnTambahAnggota.setTextColor(Color.WHITE)
        btnTambahAnggota.background = roundedBackground(
            color = orange,
            radius = 14
        )
        btnTambahAnggota.setPadding(
            dp(18), dp(12), dp(18), dp(12)
        )
        btnTambahAnggota.textSize = 14f
        btnTambahAnggota.typeface =
            Typeface.create("sans-serif-medium", Typeface.NORMAL)

        // Warna dasar area daftar bila layout mendukungnya.
        view.setBackgroundColor(backgroundColor)
    }

    private fun setupRecyclerView() {
        adapter = AnggotaAdapter { anggota ->
            confirmHapusAnggota(anggota)
        }

        rvAnggotaTim.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@KetuaTimTimFragment.adapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }

    private fun setupListeners() {
        btnTambahAnggota.setOnClickListener {
            bukaPemilihanPegawai()
        }
    }

    // =========================================================
    // MEMUAT TIM KETUA
    // =========================================================

    private fun loadTimKetua() {
        if (!isAdded) return

        val userId = authSession.getUserId()

        if (userId <= 0) {
            currentTimId = null
            anggotaList = emptyList()

            adapter.updateData(emptyList())
            updateJumlahAnggota()
            showLoading(false)
            showEmpty(
                true,
                "Data pengguna login tidak ditemukan."
            )
            updateTambahButtonState()
            return
        }

        showLoading(true)

        repository.getTim(authSession) { timList, error ->
            if (!isAdded || view == null) {
                return@getTim
            }

            if (error != null) {
                showLoading(false)
                showEmpty(true, "Gagal memuat data tim.")
                showMessage(error)
                return@getTim
            }

            val timSaya = timList
                .orEmpty()
                .firstOrNull { it.ketuaId == userId }

            if (timSaya == null) {
                currentTimId = null
                anggotaList = emptyList()

                adapter.updateData(emptyList())
                updateJumlahAnggota()

                showLoading(false)
                showEmpty(true, "Anda belum memiliki tim.")
                updateTambahButtonState()

                return@getTim
            }

            currentTimId = timSaya.id
            updateTambahButtonState()

            loadDetailTim(timSaya.id)
        }
    }

    // =========================================================
    // MEMUAT DETAIL DAN DAFTAR ANGGOTA
    // =========================================================

    private fun loadDetailTim(timId: Int) {
        if (!isAdded) return

        showLoading(true)

        repository.getDetailTim(authSession, timId) { tim, error ->
            if (!isAdded || view == null) {
                return@getDetailTim
            }

            showLoading(false)

            if (error != null) {
                showEmpty(true, "Gagal memuat anggota tim.")
                showMessage(error)
                return@getDetailTim
            }

            currentTimId = timId
            anggotaList = tim?.anggota.orEmpty()

            adapter.updateData(anggotaList)
            updateJumlahAnggota()

            showEmpty(
                show = anggotaList.isEmpty(),
                message = "Belum ada anggota dalam tim ini."
            )
        }
    }

    // =========================================================
    // LOADING DAN EMPTY STATE
    // =========================================================

    private fun showLoading(show: Boolean) {
        isLoading = show

        progressAnggotaTim.visibility =
            if (show) View.VISIBLE else View.GONE

        if (show) {
            rvAnggotaTim.visibility = View.GONE
            layoutAnggotaKosong.visibility = View.GONE
        } else {
            rvAnggotaTim.visibility = View.VISIBLE
        }

        updateTambahButtonState()
    }

    private fun showEmpty(
        show: Boolean,
        message: String = "Belum ada anggota dalam tim ini."
    ) {
        layoutAnggotaKosong.visibility =
            if (show && !isLoading) View.VISIBLE else View.GONE

        rvAnggotaTim.visibility =
            if (show || isLoading) View.GONE else View.VISIBLE

        // Pertahankan struktur XML yang sudah ada.
        val container = layoutAnggotaKosong as? ViewGroup

        val textViews = mutableListOf<TextView>()

        fun collectTextViews(parent: ViewGroup) {
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)

                if (child is TextView) {
                    textViews.add(child)
                } else if (child is ViewGroup) {
                    collectTextViews(child)
                }
            }
        }

        if (container != null) {
            collectTextViews(container)
        }

        textViews.getOrNull(0)?.text = "Belum Ada Anggota"
        textViews.getOrNull(1)?.text = message
    }

    private fun updateJumlahAnggota() {
        tvJumlahAnggota.text = anggotaList.size.toString()
    }

    private fun updateTambahButtonState() {
        val enabled = currentTimId != null &&
                !isLoading &&
                !isLoadingPegawai &&
                !isSubmitting

        btnTambahAnggota.isEnabled = enabled
        btnTambahAnggota.alpha = if (enabled) 1f else 0.55f

        btnTambahAnggota.text = when {
            isSubmitting -> "Memproses..."
            isLoadingPegawai -> "Memuat Pegawai..."
            else -> "+  Tambahkan Pegawai"
        }
    }

    // =========================================================
    // POPUP PILIH PEGAWAI
    // =========================================================

    private fun bukaPemilihanPegawai() {
        if (
            !isAdded ||
            isLoading ||
            isLoadingPegawai ||
            isSubmitting
        ) {
            return
        }

        val timId = currentTimId

        if (timId == null) {
            showMessage("Data tim belum tersedia.")
            loadTimKetua()
            return
        }

        isLoadingPegawai = true
        updateTambahButtonState()

        repository.getPegawaiTersedia(authSession) { pegawai, error ->
            if (!isAdded || view == null) {
                return@getPegawaiTersedia
            }

            isLoadingPegawai = false
            updateTambahButtonState()

            if (error != null) {
                showMessage(error)
                return@getPegawaiTersedia
            }

            if (pegawai == null) {
                showMessage("Daftar pegawai tidak tersedia.")
                return@getPegawaiTersedia
            }

            showDialogPilihPegawai(
                timId = timId,
                pegawai = pegawai
            )
        }
    }

    private fun showDialogPilihPegawai(
        timId: Int,
        pegawai: List<PegawaiTersedia>
    ) {
        if (!isAdded) return

        if (pegawai.isEmpty()) {
            showStyledMessage(
                title = "Pegawai Tidak Tersedia",
                message = "Tidak ada pegawai yang tersedia untuk dipilih.",
                positiveText = "Mengerti"
            )
            return
        }

        val selectedIds = mutableSetOf<Int>()
        val checkboxMap = linkedMapOf<Int, CheckBox>()

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val description = TextView(requireContext()).apply {
            text = "Pilih satu atau beberapa pegawai untuk bergabung dengan tim."
            textSize = 13f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, dp(12))
        }

        val searchInput = EditText(requireContext()).apply {
            hint = "Cari nama pegawai..."
            textSize = 14f
            setSingleLine(true)
            setTextColor(textColor)
            setHintTextColor(secondaryTextColor)
            background = roundedBackground(
                color = Color.WHITE,
                radius = 12,
                strokeColor = borderColor
            )
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val selectionSummary = TextView(requireContext()).apply {
            text = "0 pegawai dipilih"
            textSize = 12f
            setTextColor(orangeDark)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(8))
        }

        val listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(requireContext()).apply {
            isFillViewport = true
            addView(
                listContainer,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        content.addView(
            description,
            matchWrap()
        )
        content.addView(
            searchInput,
            matchWrap()
        )
        content.addView(
            selectionSummary,
            matchWrap()
        )
        content.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(300)
            )
        )

        fun updateSummary() {
            selectionSummary.text =
                "${selectedIds.size} pegawai dipilih"
        }

        fun renderPegawai(query: String = "") {
            listContainer.removeAllViews()
            checkboxMap.clear()

            val filtered = pegawai.filter {
                it.nama.contains(query.trim(), ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                val emptyText = TextView(requireContext()).apply {
                    text = "Pegawai tidak ditemukan."
                    textSize = 13f
                    setTextColor(secondaryTextColor)
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(28), dp(12), dp(28))
                }

                listContainer.addView(emptyText, matchWrap())
                return
            }

            filtered.forEach { item ->
                val checkbox = CheckBox(requireContext()).apply {
                    text = item.nama
                    textSize = 14f
                    setTextColor(textColor)
                    buttonTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf()
                        ),
                        intArrayOf(orange, secondaryTextColor)
                    )
                    isChecked = selectedIds.contains(item.id)
                    setPadding(dp(4), dp(8), dp(4), dp(8))

                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            selectedIds.add(item.id)
                        } else {
                            selectedIds.remove(item.id)
                        }

                        updateSummary()
                    }
                }

                checkboxMap[item.id] = checkbox

                listContainer.addView(
                    checkbox,
                    matchWrap()
                )

                val divider = View(requireContext()).apply {
                    setBackgroundColor(borderColor)
                }

                listContainer.addView(
                    divider,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                )
            }
        }

        renderPegawai()

        searchInput.addTextChangedListener(
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
                    renderPegawai(s?.toString().orEmpty())
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) = Unit
            }
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambah Anggota Tim")
            .setView(content)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Lanjutkan", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(
                roundedBackground(Color.WHITE, 20)
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(orangeDark)
                typeface = Typeface.DEFAULT_BOLD

                setOnClickListener {
                    val pegawaiTerpilih = pegawai.filter {
                        selectedIds.contains(it.id)
                    }

                    if (pegawaiTerpilih.isEmpty()) {
                        showMessage("Pilih minimal satu pegawai.")
                        return@setOnClickListener
                    }

                    dialog.dismiss()

                    konfirmasiTambahBanyakAnggota(
                        timId = timId,
                        pegawaiTerpilih = pegawaiTerpilih
                    )
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(secondaryTextColor)
            }
        }

        dialog.show()
    }

    // =========================================================
    // KONFIRMASI TAMBAH BANYAK ANGGOTA
    // =========================================================

    private fun konfirmasiTambahBanyakAnggota(
        timId: Int,
        pegawaiTerpilih: List<PegawaiTersedia>
    ) {
        if (!isAdded || isSubmitting) return

        val daftarNama = pegawaiTerpilih.joinToString("\n") {
            "• ${it.nama}"
        }

        showStyledMessage(
            title = "Konfirmasi Penambahan",
            message = "Tambahkan ${pegawaiTerpilih.size} pegawai berikut ke dalam tim?\n\n$daftarNama",
            positiveText = "Tambah",
            negativeText = "Batal",
            onPositive = {
                if (!isAdded || isSubmitting) return@showStyledMessage

                isSubmitting = true
                showLoading(true)
                updateTambahButtonState()

                tambahAnggotaBerikutnya(
                    timId = timId,
                    pegawaiTerpilih = pegawaiTerpilih,
                    index = 0,
                    berhasil = 0,
                    gagal = mutableListOf()
                )
            }
        )
    }

    private fun tambahAnggotaBerikutnya(
        timId: Int,
        pegawaiTerpilih: List<PegawaiTersedia>,
        index: Int,
        berhasil: Int,
        gagal: MutableList<String>
    ) {
        if (!isAdded || view == null) return

        if (index >= pegawaiTerpilih.size) {
            isSubmitting = false
            showLoading(false)
            updateTambahButtonState()

            val pesan = buildString {
                append("Berhasil ditambahkan: $berhasil pegawai.")

                if (gagal.isNotEmpty()) {
                    append("\n\nGagal: ${gagal.size} pegawai.")
                    append("\n")
                    append(gagal.joinToString("\n"))
                }
            }

            showStyledMessage(
                title = "Hasil Penambahan",
                message = pesan,
                positiveText = "Selesai",
                onPositive = {
                    loadDetailTim(timId)
                }
            )

            loadDetailTim(timId)
            return
        }

        val pegawai = pegawaiTerpilih[index]

        repository.tambahAnggotaTim(
            authSession,
            timId,
            pegawai.id
        ) { response, error ->
            if (!isAdded || view == null) {
                return@tambahAnggotaTim
            }

            val pesanError = error ?: if (response == null) {
                "Respons API kosong."
            } else {
                null
            }

            if (pesanError == null) {
                tambahAnggotaBerikutnya(
                    timId = timId,
                    pegawaiTerpilih = pegawaiTerpilih,
                    index = index + 1,
                    berhasil = berhasil + 1,
                    gagal = gagal
                )
            } else {
                gagal.add("${pegawai.nama}: $pesanError")

                tambahAnggotaBerikutnya(
                    timId = timId,
                    pegawaiTerpilih = pegawaiTerpilih,
                    index = index + 1,
                    berhasil = berhasil,
                    gagal = gagal
                )
            }
        }
    }

    // =========================================================
    // KONFIRMASI HAPUS ANGGOTA
    // =========================================================

    private fun confirmHapusAnggota(anggota: AnggotaTim) {
        if (!isAdded || isSubmitting || isLoading) return

        val timId = currentTimId

        if (timId == null) {
            showMessage("Data tim belum tersedia.")
            return
        }

        showStyledMessage(
            title = "Hapus Anggota",
            message = "Apakah Anda yakin ingin menghapus ${anggota.nama} dari tim?",
            positiveText = "Hapus",
            negativeText = "Batal",
            destructive = true,
            onPositive = {
                if (!isAdded || isSubmitting) return@showStyledMessage

                isSubmitting = true
                showLoading(true)
                updateTambahButtonState()

                repository.hapusAnggotaTim(
                    authSession,
                    timId,
                    anggota.pegawaiId
                ) { success, error ->
                    if (!isAdded || view == null) {
                        return@hapusAnggotaTim
                    }

                    isSubmitting = false
                    showLoading(false)
                    updateTambahButtonState()

                    if (error != null) {
                        showMessage(error)
                        return@hapusAnggotaTim
                    }

                    if (success) {
                        showMessage("Anggota berhasil dihapus.")
                        loadDetailTim(timId)
                    } else {
                        showMessage("Anggota gagal dihapus.")
                    }
                }
            }
        )
    }

    // =========================================================
    // DIALOG BERGAYA OPERATOR
    // =========================================================

    private fun showStyledMessage(
        title: String,
        message: String,
        positiveText: String = "OK",
        negativeText: String? = null,
        destructive: Boolean = false,
        onPositive: (() -> Unit)? = null
    ) {
        if (!isAdded) return

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText, null)
            .apply {
                if (negativeText != null) {
                    setNegativeButton(negativeText, null)
                }
            }
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(
                roundedBackground(Color.WHITE, 20)
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(if (destructive) red else orangeDark)
                typeface = Typeface.DEFAULT_BOLD

                setOnClickListener {
                    dialog.dismiss()
                    onPositive?.invoke()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(secondaryTextColor)
            }
        }

        dialog.show()
    }

    // =========================================================
    // ADAPTER ANGGOTA TIM
    // =========================================================

    private inner class AnggotaAdapter(
        private val onDelete: (AnggotaTim) -> Unit
    ) : RecyclerView.Adapter<AnggotaAdapter.ViewHolder>() {

        private val items = mutableListOf<AnggotaTim>()

        inner class ViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            val nama: TextView =
                view.findViewById(R.id.tvUserName)

            val username: TextView =
                view.findViewById(R.id.tvUserUsername)

            val role: TextView =
                view.findViewById(R.id.tvUserRole)

            val edit: TextView =
                view.findViewById(R.id.btnEditUser)

            val delete: TextView =
                view.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val itemView = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_user, parent, false)

            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            val anggota = items[position]

            holder.nama.text = anggota.nama
            holder.username.text = "ID Pegawai: ${anggota.pegawaiId}"
            holder.role.text = anggota.role.uppercase()

            holder.nama.setTextColor(textColor)
            holder.username.setTextColor(secondaryTextColor)
            holder.role.setTextColor(orangeDark)

            // Tidak ada fitur edit anggota pada layar ini.
            holder.edit.visibility = View.GONE

            // Tombol hapus tetap tersedia.
            holder.delete.visibility = View.VISIBLE
            holder.delete.text = "Hapus"
            holder.delete.setTextColor(red)
            holder.delete.typeface =
                Typeface.create("sans-serif-medium", Typeface.NORMAL)

            holder.delete.setOnClickListener {
                val index = holder.adapterPosition

                if (index != RecyclerView.NO_POSITION) {
                    onDelete(items[index])
                }
            }

            // Sembunyikan komponen kegiatan yang tidak digunakan.
            holder.itemView.findViewById<View>(
                R.id.tvKegiatanTanggal
            )?.visibility = View.GONE

            holder.itemView.findViewById<View>(
                R.id.tvKegiatanJam
            )?.visibility = View.GONE

            holder.itemView.findViewById<View>(
                R.id.tvKegiatanLokasi
            )?.visibility = View.GONE

            // Sembunyikan simbol titik tiga yang tidak memiliki ID.
            // Struktur item_user.xml: CardView -> LinearLayout konten
            // -> LinearLayout header -> anak kedua adalah simbol titik tiga.
            val content = (holder.itemView as? ViewGroup)
                ?.getChildAt(0) as? ViewGroup

            val header = content?.getChildAt(0) as? ViewGroup

            if (header != null && header.childCount > 1) {
                val possibleMenu = header.getChildAt(1)

                if (possibleMenu is TextView &&
                    possibleMenu.text.toString().trim() == "⋮"
                ) {
                    possibleMenu.visibility = View.GONE
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateData(data: List<AnggotaTim>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }

    // =========================================================
    // UTILITAS DESAIN
    // =========================================================

    private fun roundedBackground(
        color: Int,
        radius: Int,
        strokeColor: Int? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (strokeColor != null) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showMessage(message: String) {
        if (!isAdded) return

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}