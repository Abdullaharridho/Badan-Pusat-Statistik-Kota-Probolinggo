
package com.example.bpskota.uioperator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.HomeActivity
import com.example.bpskota.bpskp.repository.BpsBiometricManager
import com.example.bpskota.bpskp.repository.BpsBiometricSecureStorage
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentOperatorProfilBinding
import java.util.UUID

class OperatorProfilFragment : Fragment() {

    private var _binding: FragmentOperatorProfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var biometricManager: BpsBiometricManager
    private lateinit var biometricSecureStorage: BpsBiometricSecureStorage

    private var biometricListenerTerpasang = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorProfilBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val appCompatActivity = requireActivity() as androidx.appcompat.app.AppCompatActivity

        authSession = BpskpAuthSession(context)
        repository = BpskpRepository()
        biometricManager = BpsBiometricManager(appCompatActivity)
        biometricSecureStorage = BpsBiometricSecureStorage(context)

        tampilkanDataProfile()
        tampilkanStatusBiometrik()
        setupClickListener()
    }

    private fun tampilkanDataProfile() {
        val nama = authSession.getName().orEmpty().ifBlank { "-" }
        val username = authSession.getUsername().orEmpty().ifBlank { "-" }
        val role = authSession.getRole().orEmpty().ifBlank { "-" }

        binding.tvProfileName.text = nama
        binding.tvProfileUsername.text = username
        binding.tvProfileRole.text = role

        // ID tambahan pada desain baru.
        binding.tvProfileNameDetail.text = nama
        binding.tvProfileUsernameDetail.text = username
    }

    private fun tampilkanStatusBiometrik() {
        binding.switchBiometric.setOnCheckedChangeListener(null)
        binding.switchBiometric.isChecked =
            biometricSecureStorage.hasCredential()

        pasangListenerBiometrik()
    }

    private fun setupClickListener() {
        binding.btnEditProfile.setOnClickListener {
            tampilkanDialogEditProfile()
        }

        binding.btnChangePassword.setOnClickListener {
            tampilkanDialogUbahPassword()
        }
    }

    private fun pasangListenerBiometrik() {
        biometricListenerTerpasang = true

        binding.switchBiometric.setOnCheckedChangeListener { buttonView, isChecked ->
            // Lepas listener ketika status switch diubah dari kode.
            binding.switchBiometric.setOnCheckedChangeListener(null)
            biometricListenerTerpasang = false

            if (!isAdded) return@setOnCheckedChangeListener

            if (!biometricManager.isBiometricAvailable()) {
                buttonView.isChecked = false

                tampilkanToast(
                    "Biometrik belum tersedia pada perangkat.",
                    Toast.LENGTH_LONG
                )

                pasangListenerBiometrik()
                return@setOnCheckedChangeListener
            }

            val token = authSession.getToken()

            if (token.isNullOrBlank()) {
                buttonView.isChecked = false

                tampilkanToast(
                    "Sesi login tidak ditemukan.",
                    Toast.LENGTH_LONG
                )

                pasangListenerBiometrik()
                return@setOnCheckedChangeListener
            }

            // Menonaktifkan biometrik cukup menghapus credential lokal.
            if (!isChecked) {
                biometricSecureStorage.clearCredential()
                buttonView.isChecked = false

                tampilkanToast(
                    "Login biometrik dinonaktifkan.",
                    Toast.LENGTH_SHORT
                )

                pasangListenerBiometrik()
                return@setOnCheckedChangeListener
            }

            // Buat credential baru, bukan token autentikasi.
            val credentialId = UUID.randomUUID().toString()

            biometricManager.authenticate(
                onSuccess = {
                    activity?.runOnUiThread {
                        if (!isAdded || _binding == null) return@runOnUiThread

                        repository.biometricRegister(
                            session = authSession,
                            credentialId = credentialId,
                            deviceName = android.os.Build.MODEL
                        ) { response, error, httpCode ->
                            activity?.runOnUiThread {
                                if (!isAdded || _binding == null) return@runOnUiThread

                                if (response != null && response.success) {
                                    // Simpan lokal hanya jika backend sukses.
                                    biometricSecureStorage.saveCredential(
                                        credentialId
                                    )

                                    val tersimpan =
                                        biometricSecureStorage.hasCredential()

                                    binding.switchBiometric.isChecked = tersimpan

                                    tampilkanToast(
                                        if (tersimpan) {
                                            "Login biometrik berhasil diaktifkan."
                                        } else {
                                            "Credential biometrik gagal disimpan."
                                        },
                                        Toast.LENGTH_LONG
                                    )
                                } else {
                                    biometricSecureStorage.clearCredential()
                                    binding.switchBiometric.isChecked = false

                                    val pesan = error ?: when (httpCode) {
                                        401 -> "Sesi login tidak valid atau sudah berakhir."
                                        422 -> "Data credential biometrik tidak valid."
                                        500 -> "Terjadi kesalahan pada server."
                                        else -> "Registrasi biometrik gagal."
                                    }

                                    tampilkanToast(pesan, Toast.LENGTH_LONG)
                                }

                                pasangListenerBiometrik()
                            }
                        }
                    }
                },
                onError = { message ->
                    activity?.runOnUiThread {
                        if (!isAdded || _binding == null) return@runOnUiThread

                        binding.switchBiometric.isChecked = false
                        tampilkanToast(message, Toast.LENGTH_SHORT)
                        pasangListenerBiometrik()
                    }
                },
                onFailed = {
                    activity?.runOnUiThread {
                        if (!isAdded || _binding == null) return@runOnUiThread

                        tampilkanToast(
                            "Sidik jari tidak cocok. Silakan coba lagi.",
                            Toast.LENGTH_SHORT
                        )
                    }
                }
            )
        }
    }

    private fun tampilkanDialogEditProfile() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 8, padding, 8)
        }

        val etName = EditText(context).apply {
            hint = "Nama"
            inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(authSession.getName().orEmpty())
            setSelection(text.length)
        }

        val etUsername = EditText(context).apply {
            hint = "Username"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(authSession.getUsername().orEmpty())
            setSelection(text.length)
        }

        container.addView(
            etName,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )

        container.addView(
            etUsername,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog = AlertDialog.Builder(context)
            .setTitle("Ubah Profil")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            button.setOnClickListener {
                val name = etName.text.toString().trim()
                val username = etUsername.text.toString().trim()

                if (name.isEmpty()) {
                    etName.error = "Nama wajib diisi"
                    etName.requestFocus()
                    return@setOnClickListener
                }

                if (username.isEmpty()) {
                    etUsername.error = "Username wajib diisi"
                    etUsername.requestFocus()
                    return@setOnClickListener
                }

                ubahProfile(
                    name = name,
                    username = username,
                    dialog = dialog
                )
            }
        }

        dialog.show()
    }

    private fun ubahProfile(
        name: String,
        username: String,
        dialog: AlertDialog
    ) {
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.isEnabled = false

        repository.updateProfile(
            session = authSession,
            name = name,
            username = username
        ) { response, error ->
            activity?.runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread

                if (response != null) {
                    val requiresLogin = response.requires_login == true

                    if (requiresLogin) {
                        biometricSecureStorage.clearCredential()
                        authSession.clearSession()
                        dialog.dismiss()

                        tampilkanToast(
                            response.message
                                ?: "Profil berhasil diperbarui. Silakan login kembali.",
                            Toast.LENGTH_LONG
                        )

                        kembaliKeLogin()
                    } else {
                        val user = response.user
                        val token = authSession.getToken()

                        if (!token.isNullOrBlank()) {
                            authSession.saveLogin(
                                token = token,
                                userId = user?.id ?: authSession.getUserId(),
                                name = user?.name ?: name,
                                username = user?.username ?: username,
                                role = user?.role
                                    ?: authSession.getRole()
                                    ?: ""
                            )
                        }

                        // Credential lama tidak boleh dipakai setelah perubahan profil.
                        if (biometricSecureStorage.hasCredential()) {
                            biometricSecureStorage.clearCredential()
                        }

                        tampilkanDataProfile()
                        tampilkanStatusBiometrik()
                        dialog.dismiss()

                        tampilkanToast(
                            response.message ?: "Profil berhasil diperbarui.",
                            Toast.LENGTH_SHORT
                        )
                    }
                } else {
                    button.isEnabled = true

                    tampilkanToast(
                        error ?: "Gagal memperbarui profil.",
                        Toast.LENGTH_LONG
                    )
                }
            }
        }
    }

    private fun tampilkanDialogUbahPassword() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 8, padding, 8)
        }

        val etCurrentPassword = EditText(context).apply {
            hint = "Password saat ini"
            inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etNewPassword = EditText(context).apply {
            hint = "Password baru"
            inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etConfirmPassword = EditText(context).apply {
            hint = "Konfirmasi password baru"
            inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        container.addView(
            etCurrentPassword,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )

        container.addView(
            etNewPassword,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )

        container.addView(
            etConfirmPassword,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog = AlertDialog.Builder(context)
            .setTitle("Ubah Password")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            button.setOnClickListener {
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (currentPassword.isBlank()) {
                    etCurrentPassword.error = "Password saat ini wajib diisi"
                    etCurrentPassword.requestFocus()
                    return@setOnClickListener
                }

                if (newPassword.isBlank()) {
                    etNewPassword.error = "Password baru wajib diisi"
                    etNewPassword.requestFocus()
                    return@setOnClickListener
                }

                if (newPassword.length < 8) {
                    etNewPassword.error = "Password minimal 8 karakter"
                    etNewPassword.requestFocus()
                    return@setOnClickListener
                }

                if (confirmPassword.isBlank()) {
                    etConfirmPassword.error = "Konfirmasi password wajib diisi"
                    etConfirmPassword.requestFocus()
                    return@setOnClickListener
                }

                if (newPassword != confirmPassword) {
                    etConfirmPassword.error =
                        "Konfirmasi password tidak sama"
                    etConfirmPassword.requestFocus()
                    return@setOnClickListener
                }

                ubahPassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    dialog = dialog
                )
            }
        }

        dialog.show()
    }

    private fun ubahPassword(
        currentPassword: String,
        newPassword: String,
        dialog: AlertDialog
    ) {
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.isEnabled = false

        repository.updatePassword(
            session = authSession,
            currentPassword = currentPassword,
            newPassword = newPassword
        ) { response, error ->
            activity?.runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread

                if (response != null) {
                    biometricSecureStorage.clearCredential()
                    authSession.clearSession()
                    dialog.dismiss()

                    tampilkanToast(
                        response.message
                            ?: "Password berhasil diubah. Silakan login kembali.",
                        Toast.LENGTH_LONG
                    )

                    kembaliKeLogin()
                } else {
                    button.isEnabled = true

                    tampilkanToast(
                        error ?: "Gagal mengubah password.",
                        Toast.LENGTH_LONG
                    )
                }
            }
        }
    }

    private fun kembaliKeLogin() {
        val context = requireContext()

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        activity?.finish()
    }

    private fun tampilkanToast(
        pesan: String,
        durasi: Int
    ) {
        context?.let {
            Toast.makeText(it, pesan, durasi).show()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        _binding?.switchBiometric?.setOnCheckedChangeListener(null)
        biometricListenerTerpasang = false
        _binding = null
        super.onDestroyView()
    }
}
