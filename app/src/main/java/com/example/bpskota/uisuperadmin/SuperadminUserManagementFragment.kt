
package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.JabatanData
import com.example.bpskota.bpskp.model.UserManagementData
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.DialogUserFormBinding
import com.example.bpskota.databinding.FragmentUserManagementBinding

class SuperadminUserManagementFragment : Fragment() {

    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    private var users: List<UserManagementData> = emptyList()

    private var daftarJabatan: List<JabatanData> = emptyList()
    private var jabatanLoaded = false
    private var jabatanLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentUserManagementBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        authSession = BpskpAuthSession(requireContext())
        repository = BpskpRepository()

        setupAddButton()
        loadUsers()
        loadJabatan()
    }

    // =========================================================
    // TAMBAH PENGGUNA
    // =========================================================

    private fun setupAddButton() {

        binding.btnAddUser.setOnClickListener {
            showUserForm(null)
        }
    }

    // =========================================================
    // GET ALL USERS
    // =========================================================

    private fun loadUsers() {

        repository.getUsers(authSession) { response, error ->

            if (!isAdded || _binding == null) {
                return@getUsers
            }

            if (response != null) {

                users = response.data ?: emptyList()
                displayUsers()

            } else {

                Toast.makeText(
                    requireContext(),
                    error ?: "Gagal mengambil data pengguna.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // GET JABATAN
    // =========================================================

    private fun loadJabatan() {

        if (jabatanLoaded || jabatanLoading) {
            return
        }

        jabatanLoading = true

        repository.getJabatan(authSession) { response, error ->

            if (!isAdded || _binding == null) {
                return@getJabatan
            }

            jabatanLoading = false

            if (response != null) {

                daftarJabatan = response.data ?: emptyList()
                jabatanLoaded = true

            } else {

                Toast.makeText(
                    requireContext(),
                    error ?: "Gagal mengambil data jabatan.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // ATUR VISIBILITY VIEW BERDASARKAN DATA
    // =========================================================

    private fun aturVisibility(
        view: View,
        nilai: String?
    ) {

        view.visibility = if (
            !nilai.isNullOrBlank() && nilai != "-"
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    // =========================================================
    // TAMPILKAN USER
    // =========================================================

    private fun displayUsers() {

        binding.userManagementContainer.removeAllViews()

        if (users.isEmpty()) {
            showEmptyState()
            return
        }

        val inflater = LayoutInflater.from(requireContext())

        users.forEach { user ->

            val card = inflater.inflate(
                R.layout.item_user,
                binding.userManagementContainer,
                false
            )

            val tvUserName =
                card.findViewById<TextView>(R.id.tvUserName)

            val tvUserUsername =
                card.findViewById<TextView>(R.id.tvUserUsername)

            val tvUserRole =
                card.findViewById<TextView>(R.id.tvUserRole)

            val tvKegiatanTanggal =
                card.findViewById<TextView>(R.id.tvKegiatanTanggal)

            val tvKegiatanJam =
                card.findViewById<TextView>(R.id.tvKegiatanJam)

            val tvKegiatanLokasi =
                card.findViewById<TextView>(R.id.tvKegiatanLokasi)

            val btnEditUser =
                card.findViewById<TextView>(R.id.btnEditUser)

            val btnDeleteUser =
                card.findViewById<TextView>(R.id.btnDeleteUser)

            // -------------------------------------------------
            // NAMA
            // -------------------------------------------------

            tvUserName.text = user.name ?: ""

            aturVisibility(
                tvUserName,
                user.name
            )

            // -------------------------------------------------
            // USERNAME
            // -------------------------------------------------

            tvUserUsername.text = user.username
                ?.takeIf { it.isNotBlank() }
                ?.let { "@$it" } ?: ""

            aturVisibility(
                tvUserUsername,
                user.username
            )

            // -------------------------------------------------
            // ROLE
            // -------------------------------------------------

            val roleText = formatRole(user.role)

            tvUserRole.text = roleText

            aturVisibility(
                tvUserRole,
                user.role
            )

            tvUserRole.setTextColor(
                when (user.role?.lowercase()) {
                    "super_admin" -> Color.parseColor("#F97316")
                    "pimpinan" -> Color.parseColor("#2563EB")
                    "pegawai" -> Color.parseColor("#16A34A")
                    "ketua_tim" -> Color.parseColor("#7C3AED")
                    "operator" -> Color.parseColor("#0891B2")
                    else -> Color.parseColor("#6B7280")
                }
            )

            // -------------------------------------------------
            // INFORMASI KEGIATAN TIDAK DIGUNAKAN DI CARD USER
            // -------------------------------------------------

            tvKegiatanTanggal.visibility = View.GONE
            tvKegiatanJam.visibility = View.GONE
            tvKegiatanLokasi.visibility = View.GONE

            // -------------------------------------------------
            // EDIT
            // -------------------------------------------------

            btnEditUser.visibility = View.VISIBLE

            btnEditUser.setOnClickListener {
                showUserForm(user)
            }

            // -------------------------------------------------
            // DELETE
            // -------------------------------------------------

            btnDeleteUser.visibility = View.VISIBLE

            btnDeleteUser.setOnClickListener {
                confirmDeleteUser(user)
            }

            binding.userManagementContainer.addView(card)
        }
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private fun showEmptyState() {

        val textView = TextView(requireContext()).apply {

            text = "Belum ada pengguna."
            textSize = 13f

            setTextColor(
                Color.parseColor("#6B7280")
            )

            gravity = Gravity.CENTER

            setPadding(
                0,
                40,
                0,
                40
            )
        }

        binding.userManagementContainer.addView(textView)
    }

    // =========================================================
    // FORM TAMBAH / EDIT
    // =========================================================

    private fun showUserForm(
        user: UserManagementData?
    ) {

        if (!jabatanLoaded) {

            loadJabatan()

            if (jabatanLoading) {
                return
            }

            Toast.makeText(
                requireContext(),
                "Data jabatan belum tersedia. Silakan coba kembali.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val dialogBinding = DialogUserFormBinding.inflate(
            LayoutInflater.from(requireContext())
        )

        val isEdit = user != null

        // -----------------------------------------------------
        // PASTIKAN SEMUA TEKS INPUT HITAM
        // -----------------------------------------------------

        val editTexts = listOf(
            dialogBinding.etUserName,
            dialogBinding.etUserUsername,
            dialogBinding.etUserPassword,
            dialogBinding.etUserNip,
            dialogBinding.etUserNoHp
        )

        editTexts.forEach { editText ->

            editText.setTextColor(Color.BLACK)
            editText.setHintTextColor(Color.BLACK)
        }

        dialogBinding.tvUserPasswordHint.setTextColor(Color.BLACK)

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        dialogBinding.tvUserFormTitle.text =
            if (isEdit) {
                "Edit Pengguna"
            } else {
                "Tambah Pengguna"
            }

        // -----------------------------------------------------
        // DATA SAAT EDIT
        // -----------------------------------------------------

        if (isEdit) {

            dialogBinding.etUserName.setText(user?.name ?: "")
            dialogBinding.etUserUsername.setText(user?.username ?: "")
            dialogBinding.etUserPassword.setText("")
            dialogBinding.etUserNip.setText(user?.nip ?: "")
            dialogBinding.etUserNoHp.setText(user?.no_hp ?: "")

            dialogBinding.tvUserPasswordHint.visibility = View.VISIBLE

        } else {

            dialogBinding.tvUserPasswordHint.visibility = View.GONE
        }

        // -----------------------------------------------------
        // ROLE
        // -----------------------------------------------------

        val availableRoles = getAvailableRoles(user)

        setupSpinner(
            dialogBinding.spinnerUserRole,
            availableRoles.map { formatRole(it) }
        )

        val currentRoleIndex = availableRoles.indexOf(
            user?.role?.lowercase()
        )

        if (currentRoleIndex >= 0) {
            dialogBinding.spinnerUserRole.setSelection(currentRoleIndex)
        }

        // -----------------------------------------------------
        // JENIS KELAMIN
        // -----------------------------------------------------

        val jenisKelaminOptions = listOf(
            "Pilih jenis kelamin",
            "Laki-laki",
            "Perempuan"
        )

        setupSpinner(
            dialogBinding.spinnerUserJenisKelamin,
            jenisKelaminOptions
        )

        val jenisKelaminIndex = when (user?.jenis_kelamin?.lowercase()) {
            "laki-laki" -> 1
            "perempuan" -> 2
            else -> 0
        }

        dialogBinding.spinnerUserJenisKelamin.setSelection(
            jenisKelaminIndex
        )

        // -----------------------------------------------------
        // JABATAN
        // -----------------------------------------------------

        val jabatanOptions = daftarJabatan.toMutableList()

        if (
            user?.id_jabatan != null &&
            jabatanOptions.none { it.id == user.id_jabatan }
        ) {

            jabatanOptions.add(
                JabatanData(
                    id = user.id_jabatan,
                    nama_jabatan = user.nama_jabatan
                )
            )
        }

        val jabatanNames = mutableListOf("Pilih jabatan")

        jabatanNames.addAll(
            jabatanOptions.map {
                it.nama_jabatan ?: "-"
            }
        )

        setupSpinner(
            dialogBinding.spinnerUserJabatan,
            jabatanNames
        )

        val jabatanIndex = jabatanOptions.indexOfFirst {
            it.id == user?.id_jabatan
        }

        dialogBinding.spinnerUserJabatan.setSelection(
            if (jabatanIndex >= 0) jabatanIndex + 1 else 0
        )

        // -----------------------------------------------------
        // GOLONGAN
        // -----------------------------------------------------

        val golonganOptions = listOf(
            "Pilih golongan",
            "I/a",
            "I/b",
            "I/c",
            "I/d",
            "II/a",
            "II/b",
            "II/c",
            "II/d",
            "III/a",
            "III/b",
            "III/c",
            "III/d",
            "IV/a",
            "IV/b",
            "IV/c",
            "IV/d",
            "IV/e"
        )

        setupSpinner(
            dialogBinding.spinnerUserGolongan,
            golonganOptions
        )

        val golonganIndex = golonganOptions.indexOf(user?.golongan)

        dialogBinding.spinnerUserGolongan.setSelection(
            if (golonganIndex >= 0) golonganIndex else 0
        )

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        val statusOptions = listOf(
            "Aktif",
            "Nonaktif"
        )

        setupSpinner(
            dialogBinding.spinnerUserStatus,
            statusOptions
        )

        val statusIndex = when (user?.status?.lowercase()) {
            "nonaktif" -> 1
            else -> 0
        }

        dialogBinding.spinnerUserStatus.setSelection(statusIndex)

        // -----------------------------------------------------
        // BUAT DIALOG
        // -----------------------------------------------------

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // -----------------------------------------------------
        // BATAL
        // -----------------------------------------------------

        dialogBinding.btnCancelUser.setOnClickListener {
            dialog.dismiss()
        }

        // -----------------------------------------------------
        // SIMPAN
        // -----------------------------------------------------

        dialogBinding.btnSaveUser.setOnClickListener {

            saveUser(
                dialogBinding = dialogBinding,
                user = user,
                dialog = dialog,
                jabatanOptions = jabatanOptions,
                availableRoles = availableRoles
            )
        }

        dialog.show()
    }

    // =========================================================
    // SPINNER ADAPTER
    // BACKGROUND PUTIH + BORDER ORANGE
    // =========================================================

    private fun setupSpinner(
        spinner: Spinner,
        items: List<String>
    ) {

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = super.getView(
                    position,
                    convertView,
                    parent
                )

                val textView = view.findViewById<TextView>(
                    android.R.id.text1
                )

                textView.setTextColor(Color.BLACK)
                textView.textSize = 14f

                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = super.getDropDownView(
                    position,
                    convertView,
                    parent
                )

                val textView = view.findViewById<TextView>(
                    android.R.id.text1
                )

                textView.setTextColor(Color.BLACK)
                textView.textSize = 14f
                textView.setBackgroundColor(Color.WHITE)

                return view
            }
        }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter

        val popupBackground = GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            setColor(Color.WHITE)

            setStroke(
                2,
                Color.parseColor("#F97316")
            )

            cornerRadius = 12f * resources.displayMetrics.density
        }

        spinner.setPopupBackgroundDrawable(popupBackground)
    }

    // =========================================================
    // ROLE YANG TERSEDIA
    // =========================================================

    private fun getAvailableRoles(
        editingUser: UserManagementData?
    ): List<String> {

        val roles = mutableListOf(
            "pegawai",
            "ketua_tim",
            "operator"
        )

        val currentRole = editingUser?.role?.lowercase()

        if (
            !currentRole.isNullOrBlank() &&
            !roles.contains(currentRole)
        ) {
            roles.add(currentRole)
        }

        return roles
    }

    // =========================================================
    // SIMPAN USER
    // =========================================================

    private fun saveUser(
        dialogBinding: DialogUserFormBinding,
        user: UserManagementData?,
        dialog: AlertDialog,
        jabatanOptions: List<JabatanData>,
        availableRoles: List<String>
    ) {

        val name = dialogBinding.etUserName.text
            .toString()
            .trim()

        val username = dialogBinding.etUserUsername.text
            .toString()
            .trim()

        val password = dialogBinding.etUserPassword.text
            .toString()

        val nip = dialogBinding.etUserNip.text
            .toString()
            .trim()
            .ifBlank { null }

        val noHp = dialogBinding.etUserNoHp.text
            .toString()
            .trim()
            .ifBlank { null }

        // -----------------------------------------------------
        // ROLE
        // -----------------------------------------------------

        val role = availableRoles.getOrNull(
            dialogBinding.spinnerUserRole.selectedItemPosition
        ) ?: "pegawai"

        // -----------------------------------------------------
        // JENIS KELAMIN
        // -----------------------------------------------------

        val jenisKelamin = when (
            dialogBinding.spinnerUserJenisKelamin.selectedItemPosition
        ) {
            1 -> "laki-laki"
            2 -> "perempuan"
            else -> null
        }

        // -----------------------------------------------------
        // JABATAN
        // -----------------------------------------------------

        val selectedJabatanIndex =
            dialogBinding.spinnerUserJabatan.selectedItemPosition - 1

        val idJabatan = jabatanOptions
            .getOrNull(selectedJabatanIndex)
            ?.id

        // -----------------------------------------------------
        // GOLONGAN
        // -----------------------------------------------------

        val golongan = dialogBinding.spinnerUserGolongan
            .selectedItem
            ?.toString()
            ?.takeIf {
                it != "Pilih golongan"
            }

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        val status = when (
            dialogBinding.spinnerUserStatus.selectedItemPosition
        ) {
            1 -> "nonaktif"
            else -> "aktif"
        }

        // -----------------------------------------------------
        // VALIDASI NAMA
        // -----------------------------------------------------

        if (name.isBlank()) {

            dialogBinding.etUserName.error =
                "Nama wajib diisi."

            dialogBinding.etUserName.requestFocus()

            return
        }

        // -----------------------------------------------------
        // VALIDASI USERNAME
        // -----------------------------------------------------

        if (username.isBlank()) {

            dialogBinding.etUserUsername.error =
                "Username wajib diisi."

            dialogBinding.etUserUsername.requestFocus()

            return
        }

        // -----------------------------------------------------
        // VALIDASI PASSWORD
        // -----------------------------------------------------

        if (user == null && password.isBlank()) {

            dialogBinding.etUserPassword.error =
                "Password wajib diisi."

            dialogBinding.etUserPassword.requestFocus()

            return
        }

        // -----------------------------------------------------
        // VALIDASI ROLE EXCLUSIVE
        // -----------------------------------------------------

        if (
            role.equals("pimpinan", ignoreCase = true) ||
            role.equals("super_admin", ignoreCase = true)
        ) {

            val duplicateRole = users.any { existingUser ->

                existingUser.id != user?.id &&
                        existingUser.role?.equals(
                            role,
                            ignoreCase = true
                        ) == true
            }

            if (duplicateRole) {

                Toast.makeText(
                    requireContext(),
                    "${formatRole(role)} hanya boleh memiliki satu akun.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }
        }

        // -----------------------------------------------------
        // TAMBAH USER
        // -----------------------------------------------------

        if (user == null) {

            repository.createUser(
                session = authSession,
                name = name,
                username = username,
                password = password,
                role = role,
                jenisKelamin = jenisKelamin,
                idJabatan = idJabatan,
                nip = nip,
                noHp = noHp,
                golongan = golongan,
                status = status
            ) { response, error ->

                if (!isAdded || _binding == null) {
                    return@createUser
                }

                if (response != null) {

                    dialog.dismiss()

                    Toast.makeText(
                        requireContext(),
                        response.message
                            ?: "Pengguna berhasil ditambahkan.",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadUsers()

                } else {

                    Toast.makeText(
                        requireContext(),
                        error ?: "Gagal menambahkan pengguna.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } else {

            // -------------------------------------------------
            // EDIT USER
            // -------------------------------------------------

            val userId = user.id

            if (userId == null) {

                Toast.makeText(
                    requireContext(),
                    "ID pengguna tidak valid.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            repository.updateUser(
                session = authSession,
                id = userId,
                name = name,
                username = username,
                password = password.ifBlank { null },
                role = role,
                jenisKelamin = jenisKelamin,
                idJabatan = idJabatan,
                nip = nip,
                noHp = noHp,
                golongan = golongan,
                status = status
            ) { response, error ->

                if (!isAdded || _binding == null) {
                    return@updateUser
                }

                if (response != null) {

                    dialog.dismiss()

                    Toast.makeText(
                        requireContext(),
                        response.message
                            ?: "Pengguna berhasil diperbarui.",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadUsers()

                } else {

                    Toast.makeText(
                        requireContext(),
                        error ?: "Gagal memperbarui pengguna.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // =========================================================
    // KONFIRMASI DELETE
    // =========================================================

    private fun confirmDeleteUser(
        user: UserManagementData
    ) {

        val userId = user.id

        if (userId == null) {

            Toast.makeText(
                requireContext(),
                "ID pengguna tidak valid.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val currentUserId = authSession.getUserId()

        if (
            currentUserId != null &&
            currentUserId == userId
        ) {

            Toast.makeText(
                requireContext(),
                "Akun yang sedang digunakan tidak dapat dihapus.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val userName = user.name
            ?.takeIf { it.isNotBlank() }
            ?: user.username
            ?: "pengguna ini"

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pengguna")
            .setMessage(
                "Apakah kamu yakin ingin menghapus akun \"$userName\"?"
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->

                deleteUser(
                    userId = userId,
                    userName = userName
                )
            }
            .show()
    }

    // =========================================================
    // DELETE USER
    // =========================================================

    private fun deleteUser(
        userId: Int,
        userName: String
    ) {

        repository.deleteUser(
            session = authSession,
            id = userId
        ) { response, error ->

            if (!isAdded || _binding == null) {
                return@deleteUser
            }

            if (response != null) {

                Toast.makeText(
                    requireContext(),
                    response.message
                        ?: "Pengguna \"$userName\" berhasil dihapus.",
                    Toast.LENGTH_SHORT
                ).show()

                loadUsers()

            } else {

                Toast.makeText(
                    requireContext(),
                    error ?: "Gagal menghapus pengguna.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // FORMAT ROLE
    // =========================================================

    private fun formatRole(role: String?): String {

        return when (role?.lowercase()) {
            "super_admin" -> "SUPER ADMIN"
            "pimpinan" -> "PIMPINAN"
            "ketua_tim" -> "KETUA TIM"
            "pegawai" -> "PEGAWAI"
            "operator" -> "OPERATOR"
            "mitra" -> "MITRA"
            else -> role?.uppercase() ?: "-"
        }
    }

    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
