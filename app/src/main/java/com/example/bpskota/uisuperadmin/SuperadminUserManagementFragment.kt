package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.R
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

            val btnEditUser =
                card.findViewById<TextView>(R.id.btnEditUser)

            val btnDeleteUser =
                card.findViewById<TextView>(R.id.btnDeleteUser)

            tvUserName.text =
                user.name?.takeIf { it.isNotBlank() } ?: "-"

            tvUserUsername.text =
                if (!user.username.isNullOrBlank()) {
                    "@${user.username}"
                } else {
                    "@-"
                }

            tvUserRole.text = formatRole(user.role)
            tvUserRole.setTextColor(
                when (user.role?.lowercase()) {
                    "super_admin" -> android.graphics.Color.parseColor("#F97316")
                    "pimpinan" -> android.graphics.Color.parseColor("#2563EB")
                    "user" -> android.graphics.Color.parseColor("#16A34A")
                    else -> android.graphics.Color.parseColor("#6B7280")
                }
            )

            // -------------------------------------------------
            // EDIT
            // -------------------------------------------------

            btnEditUser.setOnClickListener {

                showUserForm(user)
            }

            // -------------------------------------------------
            // DELETE
            // -------------------------------------------------

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
                android.graphics.Color.parseColor("#6B7280")
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

        val dialogBinding = DialogUserFormBinding.inflate(
            LayoutInflater.from(requireContext())
        )

        val isEdit = user != null

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

            dialogBinding.etUserName.setText(
                user?.name ?: ""
            )

            dialogBinding.etUserUsername.setText(
                user?.username ?: ""
            )

            dialogBinding.etUserPassword.setText("")

            dialogBinding.tvUserPasswordHint.visibility =
                View.VISIBLE

        } else {

            dialogBinding.tvUserPasswordHint.visibility =
                View.GONE
        }

        // -----------------------------------------------------
        // ROLE
        // -----------------------------------------------------

        val availableRoles = getAvailableRoles(user)

        val roleAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableRoles.map { formatRole(it) }
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent)

                val textView = view.findViewById<TextView>(
                    android.R.id.text1
                )

                textView.setTextColor(
                    android.graphics.Color.parseColor("#111827")
                )

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

                textView.setTextColor(
                    android.graphics.Color.parseColor("#111827")
                )

                textView.textSize = 14f

                return view
            }
        }

        roleAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        dialogBinding.spinnerUserRole.adapter = roleAdapter
        roleAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        dialogBinding.spinnerUserRole.adapter = roleAdapter

        // -----------------------------------------------------
        // PILIH ROLE SAAT EDIT
        // -----------------------------------------------------

        if (isEdit) {

            val currentRole =
                user?.role?.lowercase()

            val currentIndex =
                availableRoles.indexOf(currentRole)

            if (currentIndex >= 0) {
                dialogBinding.spinnerUserRole.setSelection(
                    currentIndex
                )
            }
        }

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
                dialog = dialog
            )
        }

        dialog.show()
    }

    // =========================================================
    // ROLE YANG TERSEDIA
    // =========================================================

    private fun getAvailableRoles(
        editingUser: UserManagementData?
    ): List<String> {

        val roles = mutableListOf<String>()

        // USER SELALU TERSEDIA
        roles.add("user")

        // Cek apakah sudah ada pimpinan
        val pimpinanExists = users.any { existingUser ->

            existingUser.role?.equals(
                "pimpinan",
                ignoreCase = true
            ) == true &&
                    existingUser.id != editingUser?.id
        }

        if (!pimpinanExists) {
            roles.add("pimpinan")
        }

        // Cek apakah sudah ada super admin
        val superAdminExists = users.any { existingUser ->

            existingUser.role?.equals(
                "super_admin",
                ignoreCase = true
            ) == true &&
                    existingUser.id != editingUser?.id
        }

        if (!superAdminExists) {
            roles.add("super_admin")
        }

        // -----------------------------------------------------
        // PENTING:
        // Role user yang sedang diedit harus tetap tersedia.
        // -----------------------------------------------------

        val currentRole =
            editingUser?.role?.lowercase()

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
        dialog: AlertDialog
    ) {

        val name =
            dialogBinding.etUserName.text
                .toString()
                .trim()

        val username =
            dialogBinding.etUserUsername.text
                .toString()
                .trim()

        val password =
            dialogBinding.etUserPassword.text
                .toString()

        val selectedRoleIndex =
            dialogBinding.spinnerUserRole.selectedItemPosition

        val availableRoles =
            getAvailableRoles(user)

        val role =
            availableRoles.getOrNull(selectedRoleIndex)
                ?: "user"

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
        // VALIDASI ROLE EXCLUSIVE DI SISI ANDROID
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
        // TAMBAH
        // -----------------------------------------------------

        if (user == null) {

            repository.createUser(
                session = authSession,
                name = name,
                username = username,
                password = password,
                role = role
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
                        error
                            ?: "Gagal menambahkan pengguna.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } else {

            // -------------------------------------------------
            // EDIT
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
                role = role
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
                        error
                            ?: "Gagal memperbarui pengguna.",
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

        // -----------------------------------------------------
        // CEGAH HAPUS AKUN SENDIRI
        // -----------------------------------------------------

        val currentUserId =
            authSession.getUserId()

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

        val userName =
            user.name?.takeIf { it.isNotBlank() }
                ?: user.username
                ?: "pengguna ini"

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pengguna")
            .setMessage(
                "Apakah kamu yakin ingin menghapus akun \"$userName\"?"
            )
            .setNegativeButton(
                "Batal",
                null
            )
            .setPositiveButton(
                "Hapus"
            ) { _, _ ->

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
                    error
                        ?: "Gagal menghapus pengguna.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // FORMAT ROLE
    // =========================================================

    private fun formatRole(
        role: String?
    ): String {

        return when (role?.lowercase()) {

            "super_admin" ->
                "SUPER ADMIN"

            "pimpinan" ->
                "PIMPINAN"

            "user" ->
                "USER"

            else ->
                role?.uppercase() ?: "-"
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