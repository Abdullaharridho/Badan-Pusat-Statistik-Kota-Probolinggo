package com.example.bpskota

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import com.example.bpskota.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val decelerateInterpolator = DecelerateInterpolator(1.5f)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAnimations()
        setupButtonInteraction()
    }

    // ============================================================
    // ANIMATIONS
    // ============================================================

    private fun setupAnimations() {

        /*
         * Urutan animasi dibuat berdasarkan visual hierarchy:
         *
         * 1. Logo
         * 2. Judul
         * 3. Subtitle
         * 4. Username
         * 5. Password
         * 6. Lupa password
         * 7. Button
         */

        val animatedViews = listOf(
            binding.ivLogo,
            binding.tvWelcome,
            binding.tvSubtitle,
            binding.tilEmail,
            binding.tilPassword,
            binding.tvLupaPassword,
            binding.btnLogin
        )

        // --------------------------------------------------------
        // Initial state
        // --------------------------------------------------------

        animatedViews.forEach { view ->

            view.alpha = 0f
            view.translationY = 28f

            // Hindari scale pada TextInput agar layout tidak terlihat
            // seperti berubah ukuran ketika pertama kali muncul.
        }

        binding.ivLogo.apply {
            scaleX = 0.82f
            scaleY = 0.82f
        }

        binding.btnLogin.apply {
            scaleX = 0.96f
            scaleY = 0.96f
        }

        // --------------------------------------------------------
        // Logo
        // --------------------------------------------------------

        binding.ivLogo.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(650)
            .setStartDelay(80)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Welcome title
        // --------------------------------------------------------

        binding.tvWelcome.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550)
            .setStartDelay(180)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Subtitle
        // --------------------------------------------------------

        binding.tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550)
            .setStartDelay(260)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Username
        // --------------------------------------------------------

        binding.tilEmail.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(340)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Password
        // --------------------------------------------------------

        binding.tilPassword.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(420)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Forgot password
        // --------------------------------------------------------

        binding.tvLupaPassword.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .setStartDelay(500)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Login button
        // --------------------------------------------------------

        binding.btnLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(550)
            .setStartDelay(580)
            .setInterpolator(decelerateInterpolator)
            .start()

        // --------------------------------------------------------
        // Background floating effect
        // --------------------------------------------------------

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

    // ============================================================
    // BACKGROUND FLOATING
    // ============================================================

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

            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            /*
             * LinearInterpolator membuat pergerakan background
             * sangat halus dan tidak terasa berhenti di ujung.
             */
            interpolator = LinearInterpolator()

            start()
        }
    }

    // ============================================================
    // BUTTON INTERACTION
    // ============================================================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtonInteraction() {

        binding.btnLogin.setOnTouchListener { view, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    view.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }

                MotionEvent.ACTION_UP -> {

                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220)
                        .setInterpolator(DecelerateInterpolator(1.8f))
                        .start()
                }

                MotionEvent.ACTION_CANCEL -> {

                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }

            /*
             * false tetap dipertahankan supaya performa klik
             * Android tetap bisa menggunakan OnClickListener.
             */
            false
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}