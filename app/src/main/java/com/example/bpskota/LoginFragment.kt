package com.example.bpskota

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.repository.BpsBiometricManager
import com.example.bpskota.bpskp.repository.BpsBiometricSecureStorage
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentLoginBinding
import com.example.bpskota.uisuperadmin.SuperAdminActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val decelerateInterpolator =
        DecelerateInterpolator(1.5f)

    private val repository =
        BpskpRepository()

    private lateinit var biometricManager: BpsBiometricManager

    private lateinit var biometricSecureStorage:
            BpsBiometricSecureStorage

    private var biometricAttempted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentLoginBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        biometricManager =
            BpsBiometricManager(
                requireActivity()
                        as androidx.appcompat.app.AppCompatActivity
            )

        biometricSecureStorage =
            BpsBiometricSecureStorage(
                requireContext()
            )

        setupAnimations()
        setupButtonInteraction()
        setupLogin()
    }

    override fun onResume() {
        super.onResume()

        val homeActivity =
            activity as? HomeActivity
                ?: return

        if (homeActivity.currentPage != 6) {
            return
        }

        binding.root.post {

            if (!isAdded || _binding == null) {
                return@post
            }

            val currentActivity =
                activity as? HomeActivity
                    ?: return@post

            if (currentActivity.currentPage != 6) {
                return@post
            }

            cekLoginBiometrik()
        }
    }

    override fun onPause() {
        super.onPause()

        val homeActivity =
            activity as? HomeActivity
                ?: return

        if (homeActivity.currentPage != 6) {
            biometricAttempted = false
        }
    }

    private fun setupLogin() {

        binding.btnLogin.setOnClickListener {
            loginDenganUsernamePassword()
        }
    }

    private fun loginDenganUsernamePassword() {

        val username =
            binding.etUsername.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val password =
            binding.etPassword.text
                ?.toString()
                .orEmpty()

        if (username.isEmpty()) {

            binding.tilUsername.error =
                "Username wajib diisi."

            binding.etUsername.requestFocus()

            return
        }

        binding.tilUsername.error = null

        if (password.isEmpty()) {

            binding.tilPassword.error =
                "Password wajib diisi."

            binding.etPassword.requestFocus()

            return
        }

        binding.tilPassword.error = null

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Memproses..."

        repository.login(
            username = username,
            password = password
        ) { response, error ->

            if (!isAdded || _binding == null) {
                return@login
            }

            requireActivity().runOnUiThread {

                if (!isAdded || _binding == null) {
                    return@runOnUiThread
                }

                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Masuk Sekarang"

                if (response != null) {

                    val user =
                        response.user

                    if (user == null) {

                        Toast.makeText(
                            requireContext(),
                            "Data pengguna tidak ditemukan.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    val token =
                        response.token

                    if (token.isNullOrBlank()) {

                        Toast.makeText(
                            requireContext(),
                            "Token login tidak ditemukan.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    simpanSessionDanMasuk(
                        token = token,
                        userId = user.id ?: -1,
                        name = user.name ?: "",
                        username = user.username ?: "",
                        role = user.role ?: ""
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        error ?: "Login gagal.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun cekLoginBiometrik() {

        val homeActivity =
            activity as? HomeActivity
                ?: return

        if (homeActivity.currentPage != 6) {
            return
        }

        if (biometricAttempted) {
            return
        }

        val hasCredential =
            biometricSecureStorage.hasCredential()

        if (!hasCredential) {
            return
        }

        biometricAttempted = true

        val biometricAvailable =
            biometricManager.isBiometricAvailable()

        if (!biometricAvailable) {

            Toast.makeText(
                requireContext(),
                "Login biometrik tidak tersedia pada perangkat.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        tampilkanBiometricPrompt()
    }

    private fun tampilkanBiometricPrompt() {

        biometricManager.authenticate(

            onSuccess = {

                if (!isAdded || _binding == null) {
                    return@authenticate
                }

                requireActivity().runOnUiThread {

                    if (!isAdded || _binding == null) {
                        return@runOnUiThread
                    }

                    prosesLoginBiometrik()
                }
            },

            onError = { message ->

                if (!isAdded || _binding == null) {
                    return@authenticate
                }

                requireActivity().runOnUiThread {

                    if (!isAdded || _binding == null) {
                        return@runOnUiThread
                    }

                    if (message.isNotBlank()) {

                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },

            onFailed = {

                if (!isAdded || _binding == null) {
                    return@authenticate
                }

                Toast.makeText(
                    requireContext(),
                    "Biometrik tidak cocok. Silakan coba lagi.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun prosesLoginBiometrik() {

        val credentialId =
            biometricSecureStorage.getCredential()

        if (credentialId.isNullOrBlank()) {

            Toast.makeText(
                requireContext(),
                "Credential biometrik tidak ditemukan. Silakan login dengan username dan password.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Memproses..."

        repository.biometricLogin(
            credentialId = credentialId
        ) { response, error, httpCode ->

            if (!isAdded || _binding == null) {
                return@biometricLogin
            }

            requireActivity().runOnUiThread {

                if (!isAdded || _binding == null) {
                    return@runOnUiThread
                }

                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Masuk Sekarang"

                if (response != null) {

                    val user =
                        response.user

                    if (user == null) {

                        Toast.makeText(
                            requireContext(),
                            "Data pengguna tidak ditemukan.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    val token =
                        response.token

                    if (token.isNullOrBlank()) {

                        Toast.makeText(
                            requireContext(),
                            "Token login tidak ditemukan.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    val userId =
                        user.id

                    val name =
                        user.name

                    val username =
                        user.username

                    val role =
                        user.role

                    if (
                        userId == null ||
                        role.isNullOrBlank()
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Data pengguna tidak lengkap. Silakan login kembali.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    val authSession =
                        BpskpAuthSession(
                            requireContext()
                        )

                    authSession.saveLogin(
                        token = token,
                        userId = userId,
                        name = name ?: "",
                        username = username ?: "",
                        role = role
                    )

                    Toast.makeText(
                        requireContext(),
                        "Login biometrik berhasil.",
                        Toast.LENGTH_SHORT
                    ).show()

                    masukBerdasarkanRole(
                        role = role
                    )

                } else {

                    if (httpCode == 401) {

                        biometricSecureStorage.clearCredential()

                        Toast.makeText(
                            requireContext(),
                            "Credential biometrik sudah tidak valid. Silakan login dengan username dan password.",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            requireContext(),
                            error
                                ?: "Login biometrik gagal. Silakan gunakan username dan password.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun simpanSessionDanMasuk(
        token: String,
        userId: Int,
        name: String,
        username: String,
        role: String
    ) {

        val authSession =
            BpskpAuthSession(
                requireContext()
            )

        authSession.saveLogin(
            token = token,
            userId = userId,
            name = name,
            username = username,
            role = role
        )

        masukBerdasarkanRole(
            role = role
        )
    }

    private fun masukBerdasarkanRole(
        role: String
    ) {

        when (role.lowercase()) {

            "super_admin" -> {

                startActivity(
                    Intent(
                        requireContext(),
                        SuperAdminActivity::class.java
                    )
                )

                requireActivity().finish()
            }

            "pimpinan" -> {

                Toast.makeText(
                    requireContext(),
                    "Login sebagai Pimpinan.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            "user" -> {

                Toast.makeText(
                    requireContext(),
                    "Login sebagai User.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {

                Toast.makeText(
                    requireContext(),
                    "Role pengguna tidak dikenali.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupAnimations() {

        val animatedViews = listOf(
            binding.ivLogo,
            binding.tilUsername,
            binding.tilPassword,
            binding.btnLogin
        )

        animatedViews.forEach { view ->
            view.alpha = 0f
            view.translationY = 28f
        }

        binding.ivLogo.apply {
            scaleX = 0.82f
            scaleY = 0.82f
        }

        binding.btnLogin.apply {
            scaleX = 0.96f
            scaleY = 0.96f
        }

        binding.ivLogo.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(650)
            .setStartDelay(80)
            .setInterpolator(
                decelerateInterpolator
            )
            .start()

        binding.tilUsername.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(340)
            .setInterpolator(
                decelerateInterpolator
            )
            .start()

        binding.tilPassword.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(420)
            .setInterpolator(
                decelerateInterpolator
            )
            .start()

        binding.btnLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(550)
            .setStartDelay(580)
            .setInterpolator(
                decelerateInterpolator
            )
            .start()

        animateFloatingBackground(
            binding.bgAccentTop,
            duration = 6500L,
            startTranslation = -8f,
            endTranslation = 8f
        )

        animateFloatingBackground(
            binding.bgAccentBottom,
            duration = 8000L,
            startTranslation = 8f,
            endTranslation = -8f
        )
    }

    private fun animateFloatingBackground(
        view: View,
        duration: Long,
        startTranslation: Float,
        endTranslation: Float
    ) {

        ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            startTranslation,
            endTranslation
        ).apply {

            this.duration = duration

            repeatCount =
                ValueAnimator.INFINITE

            repeatMode =
                ValueAnimator.REVERSE

            interpolator =
                LinearInterpolator()

            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtonInteraction() {

        binding.btnLogin.setOnTouchListener { view, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    view.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .setInterpolator(
                            DecelerateInterpolator()
                        )
                        .start()
                }

                MotionEvent.ACTION_UP -> {

                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220)
                        .setInterpolator(
                            DecelerateInterpolator(1.8f)
                        )
                        .start()
                }

                MotionEvent.ACTION_CANCEL -> {

                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(
                            DecelerateInterpolator()
                        )
                        .start()
                }
            }

            false
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}