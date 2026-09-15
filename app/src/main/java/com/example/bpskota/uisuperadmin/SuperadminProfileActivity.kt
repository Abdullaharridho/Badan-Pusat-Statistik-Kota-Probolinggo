package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.HomeActivity
import com.example.bpskota.bpskp.repository.BpsBiometricManager
import com.example.bpskota.bpskp.repository.BpsBiometricSecureStorage
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivitySuperadminProfileBinding
import java.util.UUID

class SuperadminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperadminProfileBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var biometricManager: BpsBiometricManager
    private lateinit var biometricSecureStorage: BpsBiometricSecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivitySuperadminProfileBinding.inflate(layoutInflater)

        setContentView(binding.root)

        authSession =
            BpskpAuthSession(this)

        repository =
            BpskpRepository()

        biometricManager =
            BpsBiometricManager(this)

        biometricSecureStorage =
            BpsBiometricSecureStorage(this)

        tampilkanDataProfile()
        tampilkanStatusBiometrik()
        setupClickListener()
    }

    // Menampilkan data user yang sedang login
    private fun tampilkanDataProfile() {

        binding.tvProfileName.text =
            authSession.getName() ?: "-"

        binding.tvProfileUsername.text =
            authSession.getUsername() ?: "-"

        binding.tvProfileRole.text =
            authSession.getRole() ?: "-"
    }

    // Menampilkan status biometric yang tersimpan
    private fun tampilkanStatusBiometrik() {

        binding.switchBiometric.setOnCheckedChangeListener(null)

        binding.switchBiometric.isChecked =
            biometricSecureStorage.hasCredential()

        pasangListenerBiometrik()
    }

    // Menyiapkan aksi tombol
    private fun setupClickListener() {

        // Kembali
        binding.btnBackProfile.setOnClickListener {
            finish()
        }

        // Ubah nama dan username
        binding.btnEditProfile.setOnClickListener {
            tampilkanDialogEditProfile()
        }

        // Ubah password
        binding.btnChangePassword.setOnClickListener {
            tampilkanDialogUbahPassword()
        }
    }

    /**
     * Listener khusus switch biometric.
     *
     * Switch ON:
     * 1. Buat credential_id baru.
     * 2. Verifikasi biometric.
     * 3. Daftarkan credential_id ke backend.
     * 4. Simpan credential_id secara terenkripsi.
     *
     * Switch OFF:
     * 1. Hapus credential biometric lokal.
     */
    private fun pasangListenerBiometrik() {

        binding.switchBiometric.setOnCheckedChangeListener { buttonView, isChecked ->

            // Lepas listener sementara.
            //
            // Tujuannya agar perubahan isChecked secara programmatic
            // tidak memanggil listener kembali.
            binding.switchBiometric.setOnCheckedChangeListener(null)

            if (!biometricManager.isBiometricAvailable()) {

                buttonView.isChecked = false

                Toast.makeText(
                    this,
                    "Biometrik belum tersedia pada perangkat.",
                    Toast.LENGTH_LONG
                ).show()

                pasangListenerBiometrik()

                return@setOnCheckedChangeListener
            }

            val token =
                authSession.getToken()

            if (token.isNullOrBlank()) {

                buttonView.isChecked = false

                Toast.makeText(
                    this,
                    "Session login tidak ditemukan.",
                    Toast.LENGTH_LONG
                ).show()

                pasangListenerBiometrik()

                return@setOnCheckedChangeListener
            }

            /*
             * SWITCH OFF
             *
             * Tidak perlu autentikasi biometric lagi.
             * Credential lokal cukup dihapus.
             */
            if (!isChecked) {

                biometricSecureStorage.clearCredential()

                buttonView.isChecked = false

                Toast.makeText(
                    this,
                    "Login biometrik dinonaktifkan.",
                    Toast.LENGTH_SHORT
                ).show()

                pasangListenerBiometrik()

                return@setOnCheckedChangeListener
            }

            /*
             * SWITCH ON
             *
             * Buat credential_id baru.
             *
             * Credential ini BUKAN token Sanctum.
             */
            val credentialId =
                UUID.randomUUID().toString()

            biometricManager.authenticate(

                onSuccess = {

                    runOnUiThread {

                        /*
                         * Biometric berhasil diverifikasi.
                         *
                         * Sekarang credential_id didaftarkan
                         * ke backend menggunakan session login
                         * yang sedang aktif.
                         */
                        repository.biometricRegister(
                            session = authSession,
                            credentialId = credentialId,
                            deviceName = android.os.Build.MODEL
                        ) { response, error, httpCode ->

                            runOnUiThread {

                                if (
                                    response != null &&
                                    response.success
                                ) {

                                    /*
                                     * Backend berhasil mendaftarkan
                                     * credential_id.
                                     *
                                     * Baru setelah backend sukses,
                                     * credential disimpan lokal.
                                     */
                                    biometricSecureStorage.saveCredential(
                                        credentialId
                                    )

                                    val credentialTersimpan =
                                        biometricSecureStorage.hasCredential()

                                    buttonView.isChecked =
                                        credentialTersimpan

                                    if (credentialTersimpan) {

                                        Toast.makeText(
                                            this,
                                            "Login biometrik berhasil diaktifkan.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } else {

                                        /*
                                         * Secara teori saveCredential()
                                         * sudah berhasil, tetapi jika
                                         * credential tidak ditemukan
                                         * setelah penyimpanan, jangan
                                         * menganggap biometric aktif.
                                         */
                                        buttonView.isChecked = false

                                        Toast.makeText(
                                            this,
                                            "Credential biometrik gagal disimpan.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    pasangListenerBiometrik()

                                } else {

                                    /*
                                     * Registrasi backend gagal.
                                     *
                                     * Jangan menyimpan credential_id
                                     * secara lokal.
                                     */
                                    biometricSecureStorage.clearCredential()

                                    buttonView.isChecked = false

                                    val pesan =
                                        error
                                            ?: when (httpCode) {
                                                401 ->
                                                    "Sesi login tidak valid atau sudah berakhir."

                                                422 ->
                                                    "Data credential biometrik tidak valid."

                                                500 ->
                                                    "Terjadi kesalahan pada server."

                                                else ->
                                                    "Registrasi biometrik gagal."
                                            }

                                    Toast.makeText(
                                        this,
                                        pesan,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    pasangListenerBiometrik()
                                }
                            }
                        }
                    }
                },

                onError = { message ->

                    runOnUiThread {

                        /*
                         * BiometricPrompt mengalami error
                         * atau dibatalkan pengguna.
                         */
                        buttonView.isChecked = false

                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        pasangListenerBiometrik()
                    }
                },

                onFailed = {

                    /*
                     * Jangan panggil authenticate() lagi.
                     *
                     * BiometricPrompt masih aktif dan pengguna
                     * masih bisa mencoba sidik jari kembali.
                     */
                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "Sidik jari tidak cocok. Silakan coba lagi.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }

    // Dialog ubah nama dan username
    private fun tampilkanDialogEditProfile() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    48,
                    8,
                    48,
                    8
                )
            }

        val etName =
            EditText(this).apply {

                hint = "Nama"

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_WORDS

                setText(
                    authSession.getName() ?: ""
                )

                setSelection(text.length)
            }

        val etUsername =
            EditText(this).apply {

                hint = "Username"

                inputType =
                    InputType.TYPE_CLASS_TEXT

                setText(
                    authSession.getUsername() ?: ""
                )

                setSelection(text.length)
            }

        container.addView(
            etName,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        )

        container.addView(
            etUsername,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Ubah Profil")
                .setView(container)
                .setNegativeButton(
                    "Batal",
                    null
                )
                .setPositiveButton(
                    "Simpan",
                    null
                )
                .create()

        dialog.setOnShowListener {

            val button =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            button.setOnClickListener {

                val name =
                    etName.text
                        .toString()
                        .trim()

                val username =
                    etUsername.text
                        .toString()
                        .trim()

                if (name.isEmpty()) {

                    etName.error =
                        "Nama wajib diisi"

                    etName.requestFocus()

                    return@setOnClickListener
                }

                if (username.isEmpty()) {

                    etUsername.error =
                        "Username wajib diisi"

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

    // Mengirim perubahan nama dan username
    private fun ubahProfile(
        name: String,
        username: String,
        dialog: AlertDialog
    ) {

        val button =
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

        button.isEnabled = false

        repository.updateProfile(
            session = authSession,
            name = name,
            username = username
        ) { response, error ->

            runOnUiThread {

                if (response != null) {

                    val requiresLogin =
                        response.requires_login == true

                    if (requiresLogin) {

                        /*
                         * Username berubah.
                         *
                         * Credential biometric lama
                         * tidak boleh dipertahankan.
                         */
                        biometricSecureStorage.clearCredential()

                        authSession.clearSession()

                        dialog.dismiss()

                        Toast.makeText(
                            this,
                            response.message
                                ?: "Profil berhasil diperbarui. Silakan login kembali.",
                            Toast.LENGTH_LONG
                        ).show()

                        kembaliKeLogin()

                    } else {

                        val user =
                            response.user

                        val token =
                            authSession.getToken()

                        if (!token.isNullOrBlank()) {

                            authSession.saveLogin(
                                token = token,
                                userId =
                                user?.id
                                    ?: authSession.getUserId(),
                                name =
                                user?.name
                                    ?: name,
                                username =
                                user?.username
                                    ?: username,
                                role =
                                user?.role
                                    ?: authSession.getRole()
                                    ?: ""
                            )

                            /*
                             * Jika biometric sebelumnya aktif,
                             * credential lama dihapus.
                             *
                             * Credential baru harus didaftarkan
                             * kembali melalui autentikasi biometric.
                             */
                            if (biometricSecureStorage.hasCredential()) {

                                biometricSecureStorage.clearCredential()
                            }
                        }

                        tampilkanDataProfile()

                        dialog.dismiss()

                        Toast.makeText(
                            this,
                            response.message
                                ?: "Profil berhasil diperbarui.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    button.isEnabled = true

                    Toast.makeText(
                        this,
                        error
                            ?: "Gagal memperbarui profil.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Dialog ubah password
    private fun tampilkanDialogUbahPassword() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    48,
                    8,
                    48,
                    8
                )
            }

        val etCurrentPassword =
            EditText(this).apply {

                hint = "Password saat ini"

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        val etNewPassword =
            EditText(this).apply {

                hint = "Password baru"

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        val etConfirmPassword =
            EditText(this).apply {

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
                bottomMargin = 12
            }
        )

        container.addView(
            etNewPassword,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        )

        container.addView(
            etConfirmPassword,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Ubah Password")
                .setView(container)
                .setNegativeButton(
                    "Batal",
                    null
                )
                .setPositiveButton(
                    "Simpan",
                    null
                )
                .create()

        dialog.setOnShowListener {

            val button =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            button.setOnClickListener {

                val currentPassword =
                    etCurrentPassword.text.toString()

                val newPassword =
                    etNewPassword.text.toString()

                val confirmPassword =
                    etConfirmPassword.text.toString()

                if (currentPassword.isEmpty()) {

                    etCurrentPassword.error =
                        "Password saat ini wajib diisi"

                    etCurrentPassword.requestFocus()

                    return@setOnClickListener
                }

                if (newPassword.isEmpty()) {

                    etNewPassword.error =
                        "Password baru wajib diisi"

                    etNewPassword.requestFocus()

                    return@setOnClickListener
                }

                if (newPassword.length < 8) {

                    etNewPassword.error =
                        "Password minimal 8 karakter"

                    etNewPassword.requestFocus()

                    return@setOnClickListener
                }

                if (confirmPassword.isEmpty()) {

                    etConfirmPassword.error =
                        "Konfirmasi password wajib diisi"

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

    // Mengirim perubahan password
    private fun ubahPassword(
        currentPassword: String,
        newPassword: String,
        dialog: AlertDialog
    ) {

        val button =
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

        button.isEnabled = false

        repository.updatePassword(
            session = authSession,
            currentPassword = currentPassword,
            newPassword = newPassword
        ) { response, error ->

            runOnUiThread {

                if (response != null) {

                    /*
                     * Password berubah.
                     *
                     * Credential biometric lama harus
                     * dihapus karena credential tersebut
                     * tidak boleh terus dipakai.
                     */
                    biometricSecureStorage.clearCredential()

                    authSession.clearSession()

                    dialog.dismiss()

                    Toast.makeText(
                        this,
                        response.message
                            ?: "Password berhasil diubah. Silakan login kembali.",
                        Toast.LENGTH_LONG
                    ).show()

                    kembaliKeLogin()

                } else {

                    button.isEnabled = true

                    Toast.makeText(
                        this,
                        error
                            ?: "Gagal mengubah password.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Kembali ke halaman awal
    private fun kembaliKeLogin() {

        val intent =
            Intent(
                this,
                HomeActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}