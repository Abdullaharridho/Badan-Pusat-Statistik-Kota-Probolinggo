
package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.Tim
import com.example.bpskota.bpskp.model.UserManagementData
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityManagementTimBinding

class ManagementTimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagementTimBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var timAdapter: TimAdapter

    private val daftarTim = mutableListOf<Tim>()
    private val daftarTimTampil = mutableListOf<Tim>()

    private var sedangMemuat = false
    private var dialogSedangDiproses = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManagementTimBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        setupRecyclerView()
        setupActions()
        setupSearch()

        loadTim()
    }

    // =========================================================
    // INISIALISASI HALAMAN
    // =========================================================

    private fun setupRecyclerView() {
        timAdapter = TimAdapter(
            items = daftarTimTampil,
            onEdit = { tim ->
                tampilkanFormTim(tim)
            },
            onDelete = { tim ->
                konfirmasiHapusTim(tim)
            }
        )

        binding.rvManagementTim.apply {
            layoutManager = LinearLayoutManager(this@ManagementTimActivity)
            adapter = timAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupActions() {
        binding.btnBackManagementTim.setOnClickListener {
            finish()
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }

        binding.btnTambahTim.setOnClickListener {
            tampilkanFormTim()
        }
    }

    private fun setupSearch() {
        binding.etSearchTim.addTextChangedListener(object : TextWatcher {

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
                filterTim(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    // =========================================================
    // MEMUAT DAFTAR TIM
    // =========================================================

    private fun loadTim() {
        if (sedangMemuat) return

        if (authSession.getToken().isNullOrBlank()) {
            tampilkanError(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        sedangMemuat = true
        tampilkanLoading(true)

        repository.getTim(session = authSession) { data, error ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                sedangMemuat = false
                tampilkanLoading(false)

                if (error != null) {
                    tampilkanError(error)
                    tampilkanEmpty(
                        true,
                        "Gagal memuat daftar tim. Periksa koneksi lalu coba lagi."
                    )
                    return@runOnUiThread
                }

                daftarTim.clear()
                daftarTim.addAll(data.orEmpty())

                perbaruiRingkasan()
                filterTim(binding.etSearchTim.text?.toString().orEmpty())
            }
        }
    }

    private fun filterTim(keyword: String) {
        val query = keyword.trim()

        daftarTimTampil.clear()
        daftarTimTampil.addAll(
            daftarTim.filter { tim ->
                tim.namaTim.contains(query, ignoreCase = true) ||
                        tim.namaKetua.orEmpty()
                            .contains(query, ignoreCase = true)
            }
        )

        timAdapter.notifyDataSetChanged()

        val kosong = daftarTimTampil.isEmpty()

        val pesan = when {
            daftarTim.isEmpty() ->
                "Belum ada tim. Tekan Tambah Tim Baru untuk membuat tim."

            else ->
                "Tim dengan kata kunci \"$query\" tidak ditemukan."
        }

        tampilkanEmpty(kosong, pesan)
    }

    private fun perbaruiRingkasan() {
        binding.tvTotalTim.text = daftarTim.size.toString()

        binding.tvTimAktif.text = daftarTim.count {
            it.status.equals("aktif", ignoreCase = true)
        }.toString()
    }

    private fun tampilkanLoading(tampil: Boolean) {
        binding.loadingAnimation.apply {
            if (tampil) {
                visibility = View.VISIBLE
                playAnimation()
            } else {
                cancelAnimation()
                visibility = View.GONE
            }
        }

        binding.rvManagementTim.visibility =
            if (tampil) View.GONE else View.VISIBLE

        binding.layoutEmptyTim.visibility = View.GONE
    }

    private fun tampilkanEmpty(
        tampil: Boolean,
        pesan: String
    ) {
        if (sedangMemuat) return

        binding.layoutEmptyTim.visibility =
            if (tampil) View.VISIBLE else View.GONE

        binding.tvEmptyTim.text = pesan

        binding.rvManagementTim.visibility =
            if (tampil) View.GONE else View.VISIBLE
    }

    private fun tampilkanError(pesan: String) {
        Toast.makeText(
            this,
            pesan,
            Toast.LENGTH_LONG
        ).show()
    }

    // =========================================================
    // FORM TAMBAH DAN EDIT TIM
    // =========================================================

    private fun tampilkanFormTim(tim: Tim? = null) {
        if (dialogSedangDiproses) return

        val sedangEdit = tim != null
        val padding = dp(20)
        val jarak = dp(12)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, dp(8), padding, dp(8))
        }

        val inputNama = buatInput(
            hint = "Contoh: Tim Statistik Sosial",
            nilai = tim?.namaTim.orEmpty()
        )

        // Dropdown ketua tim dari API pengguna.
        val spinnerKetua = Spinner(this)

        val penggunaKetua = mutableListOf<UserManagementData>()

        val adapterKetua = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf("Memuat daftar pengguna...")
        )

        adapterKetua.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerKetua.adapter = adapterKetua

        val inputKeterangan = buatInput(
            hint = "Keterangan tim (opsional)",
            nilai = tim?.keterangan.orEmpty(),
            multiline = true
        )

        val spinnerStatus = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ManagementTimActivity,
                android.R.layout.simple_spinner_item,
                listOf("aktif", "nonaktif")
            ).also { adapter ->
                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            setSelection(
                if (tim?.status.equals("nonaktif", ignoreCase = true)) 1
                else 0
            )
        }

        container.addView(labelForm("Nama Tim"))
        container.addView(
            inputNama,
            marginParams(bottom = jarak)
        )

        container.addView(labelForm("Ketua Tim"))
        container.addView(
            spinnerKetua,
            marginParams(bottom = jarak).apply {
                height = dp(52)
            }
        )

        container.addView(labelForm("Keterangan"))
        container.addView(
            inputKeterangan,
            marginParams(bottom = jarak)
        )

        container.addView(labelForm("Status"))
        container.addView(
            spinnerStatus,
            marginParams(bottom = dp(4)).apply {
                height = dp(52)
            }
        )

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(container)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (sedangEdit) "Edit Tim" else "Buat Tim Baru"
            )
            .setView(scrollView)
            .setNegativeButton("Batal", null)
            .setPositiveButton(
                if (sedangEdit) "Simpan Perubahan" else "Buat Tim",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(Color.rgb(249, 115, 22))

                // Cegah submit sebelum daftar ketua berhasil dimuat.
                isEnabled = false
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                Color.rgb(120, 113, 108)
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val nama = inputNama.text.toString().trim()

                    val posisiKetua = spinnerKetua.selectedItemPosition
                    val ketuaTerpilih =
                        penggunaKetua.getOrNull(posisiKetua)

                    val idKetua = ketuaTerpilih?.id

                    val keterangan = inputKeterangan.text
                        .toString()
                        .trim()
                        .ifBlank { null }

                    val status =
                        spinnerStatus.selectedItem?.toString() ?: "aktif"

                    when {
                        nama.isBlank() -> {
                            inputNama.error = "Nama tim wajib diisi"
                            inputNama.requestFocus()
                        }

                        idKetua == null || idKetua <= 0 -> {
                            Toast.makeText(
                                this@ManagementTimActivity,
                                "Pilih ketua tim terlebih dahulu.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            dialog.dismiss()

                            if (sedangEdit && tim != null) {
                                simpanPerubahanTim(
                                    tim = tim,
                                    nama = nama,
                                    ketuaId = idKetua,
                                    keterangan = keterangan,
                                    status = status
                                )
                            } else {
                                buatTim(
                                    nama = nama,
                                    ketuaId = idKetua,
                                    keterangan = keterangan,
                                    status = status
                                )
                            }
                        }
                    }
                }
        }

        dialog.show()

        // Muat daftar pengguna setelah dialog ditampilkan.
        loadPenggunaKetua(
            spinner = spinnerKetua,
            adapter = adapterKetua,
            daftarPengguna = penggunaKetua,
            ketuaIdTerpilih = tim?.ketuaId,
            dialog = dialog
        )
    }

    // =========================================================
    // MEMUAT PENGGUNA UNTUK DROPDOWN KETUA
    // =========================================================

    private fun loadPenggunaKetua(
        spinner: Spinner,
        adapter: ArrayAdapter<String>,
        daftarPengguna: MutableList<UserManagementData>,
        ketuaIdTerpilih: Int?,
        dialog: AlertDialog
    ) {
        repository.getUsers(session = authSession) { response, error ->
            runOnUiThread {
                if (
                    isFinishing ||
                    isDestroyed ||
                    !dialog.isShowing
                ) {
                    return@runOnUiThread
                }

                if (error != null || response?.success != true) {
                    adapter.clear()
                    adapter.add("Gagal memuat pengguna")
                    adapter.notifyDataSetChanged()

                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = false

                    tampilkanError(
                        error
                            ?: response?.message
                            ?: "Daftar pengguna tidak dapat dimuat."
                    )
                    return@runOnUiThread
                }

                val penggunaValid = response.data.orEmpty().filter {
                        pengguna ->
                    val role = pengguna.role.orEmpty()

                    pengguna.id != null &&
                            !role.equals(
                                "super_admin",
                                ignoreCase = true
                            ) &&
                            !role.equals(
                                "operator",
                                ignoreCase = true
                            )
                }

                daftarPengguna.clear()
                daftarPengguna.addAll(penggunaValid)

                adapter.clear()

                if (penggunaValid.isEmpty()) {
                    adapter.add("Tidak ada pengguna yang bisa dipilih")
                } else {
                    adapter.addAll(
                        penggunaValid.map { pengguna ->
                            val nama = pengguna.name
                                ?.takeIf { it.isNotBlank() }
                                ?: "Tanpa nama"

                            val username = pengguna.username
                                ?.takeIf { it.isNotBlank() }
                                ?: "-"

                            val role = pengguna.role
                                ?.takeIf { it.isNotBlank() }
                                ?: "user"

                            "$nama (@$username) — $role"
                        }
                    )
                }

                adapter.notifyDataSetChanged()

                val posisiKetua = penggunaValid.indexOfFirst {
                    it.id == ketuaIdTerpilih
                }

                if (posisiKetua >= 0) {
                    spinner.setSelection(posisiKetua)
                }

                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                ).isEnabled = penggunaValid.any {
                    it.id != null && it.id > 0
                }
            }
        }
    }

    // =========================================================
    // KOMPONEN FORM
    // =========================================================

    private fun buatInput(
        hint: String,
        nilai: String,
        numeric: Boolean = false,
        multiline: Boolean = false
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(nilai)

            textSize = 14f
            setTextColor(Color.rgb(41, 37, 36))
            setHintTextColor(Color.rgb(168, 162, 158))
            setPadding(dp(12), dp(10), dp(12), dp(10))

            backgroundTintList = ColorStateList.valueOf(
                Color.rgb(249, 115, 22)
            )

            when {
                numeric -> {
                    inputType =
                        android.text.InputType.TYPE_CLASS_NUMBER
                }

                multiline -> {
                    inputType =
                        android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

                    minLines = 2
                    maxLines = 4
                    gravity = Gravity.TOP or Gravity.START
                }

                else -> {
                    inputType =
                        android.text.InputType.TYPE_CLASS_TEXT
                }
            }

            if (!multiline) {
                maxLines = 1
            }
        }
    }

    private fun labelForm(teks: String): TextView {
        return TextView(this).apply {
            text = teks
            textSize = 13f
            setTextColor(Color.rgb(68, 64, 60))
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun marginParams(
        bottom: Int
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = bottom
        }
    }

    // =========================================================
    // MEMBUAT TIM
    // =========================================================

    private fun buatTim(
        nama: String,
        ketuaId: Int,
        keterangan: String?,
        status: String
    ) {
        if (dialogSedangDiproses) return
        dialogSedangDiproses = true

        repository.buatTim(
            session = authSession,
            namaTim = nama,
            ketuaId = ketuaId,
            keterangan = keterangan,
            status = status
        ) { response, error ->
            runOnUiThread {
                dialogSedangDiproses = false

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (error != null || response?.success != true) {
                    tampilkanError(
                        error
                            ?: response?.message
                            ?: "Tim gagal dibuat."
                    )
                    return@runOnUiThread
                }

                Toast.makeText(
                    this,
                    response.message ?: "Tim berhasil dibuat.",
                    Toast.LENGTH_SHORT
                ).show()

                loadTim()
            }
        }
    }

    // =========================================================
    // MEMPERBARUI TIM
    // =========================================================

    private fun simpanPerubahanTim(
        tim: Tim,
        nama: String,
        ketuaId: Int,
        keterangan: String?,
        status: String
    ) {
        if (dialogSedangDiproses) return
        dialogSedangDiproses = true

        repository.updateTim(
            session = authSession,
            id = tim.id,
            namaTim = nama,
            ketuaId = ketuaId,
            keterangan = keterangan,
            status = status
        ) { response, error ->
            runOnUiThread {
                dialogSedangDiproses = false

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (error != null || response?.success != true) {
                    tampilkanError(
                        error
                            ?: response?.message
                            ?: "Perubahan tim gagal disimpan."
                    )
                    return@runOnUiThread
                }

                Toast.makeText(
                    this,
                    response.message ?: "Tim berhasil diperbarui.",
                    Toast.LENGTH_SHORT
                ).show()

                loadTim()
            }
        }
    }

    // =========================================================
    // MENGHAPUS TIM
    // =========================================================

    private fun konfirmasiHapusTim(tim: Tim) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Tim")
            .setMessage(
                "Apakah kamu yakin ingin menghapus tim \"${tim.namaTim}\"?"
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).apply {
                setTextColor(Color.rgb(220, 38, 38))

                setOnClickListener {
                    dialog.dismiss()
                    hapusTim(tim)
                }
            }

            dialog.getButton(
                AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(Color.rgb(120, 113, 108))
        }

        dialog.show()
    }

    private fun hapusTim(tim: Tim) {
        if (dialogSedangDiproses) return
        dialogSedangDiproses = true

        repository.hapusTim(
            session = authSession,
            id = tim.id
        ) { berhasil, error ->
            runOnUiThread {
                dialogSedangDiproses = false

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (!berhasil) {
                    tampilkanError(error ?: "Tim gagal dihapus.")
                    return@runOnUiThread
                }

                Toast.makeText(
                    this,
                    "Tim berhasil dihapus.",
                    Toast.LENGTH_SHORT
                ).show()

                loadTim()
            }
        }
    }

    // =========================================================
    // UTILITAS
    // =========================================================

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    // =========================================================
    // ADAPTER DAFTAR TIM
    // Menggunakan item_user.xml yang sudah ada
    // =========================================================

    private inner class TimAdapter(
        private val items: List<Tim>,
        private val onEdit: (Tim) -> Unit,
        private val onDelete: (Tim) -> Unit
    ) : RecyclerView.Adapter<TimAdapter.TimViewHolder>() {

        inner class TimViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            val nama: TextView =
                view.findViewById(R.id.tvUserName)

            val ketua: TextView =
                view.findViewById(R.id.tvUserUsername)

            val status: TextView =
                view.findViewById(R.id.tvUserRole)

            val edit: TextView =
                view.findViewById(R.id.btnEditUser)

            val hapus: TextView =
                view.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TimViewHolder {
            val view = layoutInflater.inflate(
                R.layout.item_user,
                parent,
                false
            )

            return TimViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: TimViewHolder,
            position: Int
        ) {
            val tim = items[position]

            holder.nama.text = tim.namaTim

            holder.ketua.text =
                "Ketua: ${tim.namaKetua ?: "Belum tersedia"}"

            val aktif = tim.status.equals(
                "aktif",
                ignoreCase = true
            )

            holder.status.text =
                if (aktif) "AKTIF" else "NONAKTIF"

            holder.status.setTextColor(
                if (aktif) {
                    Color.rgb(22, 163, 74)
                } else {
                    Color.rgb(220, 38, 38)
                }
            )

            holder.edit.setOnClickListener {
                onEdit(tim)
            }

            holder.hapus.setOnClickListener {
                onDelete(tim)
            }

            holder.itemView.animate().cancel()

            holder.itemView.alpha = 0f
            holder.itemView.translationY = dp(12).toFloat()
            holder.itemView.scaleX = 0.98f
            holder.itemView.scaleY = 0.98f

            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260)
                .setInterpolator(
                    OvershootInterpolator(0.8f)
                )
                .start()
        }

        override fun getItemCount(): Int = items.size
    }
}