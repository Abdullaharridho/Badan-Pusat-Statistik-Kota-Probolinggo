package com.example.bpskota

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var textBps: TextView
    private lateinit var textKota: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadingAnimation = findViewById(R.id.loadingAnimation)
        textBps = findViewById(R.id.textBps)
        textKota = findViewById(R.id.textKota)
        progressBar = findViewById(R.id.progressBar)

        // ==========================================
        // LOAD ANIMASI LOTTIE
        // ==========================================

        loadingAnimation.setAnimation("Loading_Animation.json")
        loadingAnimation.repeatCount = -1
        loadingAnimation.playAnimation()

        // ==========================================
        // AWAL TEKS DAN PROGRESS
        // ==========================================

        textBps.alpha = 0f
        textKota.alpha = 0f
        progressBar.alpha = 0f

        // ==========================================
        // ANIMASI TEKS
        // ==========================================

        animateTexts()

        // ==========================================
        // MULAI LOADING
        // ==========================================

        startLoading()
    }

    private fun animateTexts() {

        // ------------------------------------------
        // BPS
        // ------------------------------------------

        val bpsAnimation = ObjectAnimator.ofFloat(
            textBps,
            View.ALPHA,
            0f,
            1f
        )

        val bpsTranslation = ObjectAnimator.ofFloat(
            textBps,
            View.TRANSLATION_Y,
            25f,
            0f
        )

        // ------------------------------------------
        // KOTA PROBOLINGGO
        // ------------------------------------------

        val kotaAnimation = ObjectAnimator.ofFloat(
            textKota,
            View.ALPHA,
            0f,
            1f
        )

        val kotaTranslation = ObjectAnimator.ofFloat(
            textKota,
            View.TRANSLATION_Y,
            25f,
            0f
        )

        // ------------------------------------------
        // PROGRESS BAR
        // ------------------------------------------

        val progressAnimation = ObjectAnimator.ofFloat(
            progressBar,
            View.ALPHA,
            0f,
            1f
        )

        // ==========================================
        // ANIMASI BPS
        // ==========================================

        AnimatorSet().apply {

            playTogether(
                bpsAnimation,
                bpsTranslation
            )

            duration = 700
            interpolator = DecelerateInterpolator()

            start()
        }

        // ==========================================
        // ANIMASI KOTA
        // ==========================================

        textKota.postDelayed({

            AnimatorSet().apply {

                playTogether(
                    kotaAnimation,
                    kotaTranslation
                )

                duration = 700
                interpolator = DecelerateInterpolator()

                start()
            }

        }, 300)

        // ==========================================
        // ANIMASI PROGRESS BAR
        // ==========================================

        progressBar.postDelayed({

            progressAnimation.duration = 500
            progressAnimation.start()

        }, 800)
    }

    private fun startLoading() {

        lifecycleScope.launch {

            // ==========================================
            // WAKTU MULAI
            // ==========================================

            val startTime = System.currentTimeMillis()

            // Minimal loading selama 5 detik
            val minimumLoadingTime = 5000L

            // ==========================================
            // TUNGGU SAMPAI 5 DETIK
            // ==========================================

            val elapsedTime =
                System.currentTimeMillis() - startTime

            val remainingTime =
                minimumLoadingTime - elapsedTime

            if (remainingTime > 0) {
                delay(remainingTime)
            }

            // ==========================================
            // CEK INTERNET
            // ==========================================

            while (true) {

                val internetAvailable = withContext(Dispatchers.IO) {
                    isInternetAvailable()
                }

                if (internetAvailable) {

                    // ==================================
                    // INTERNET TERSEDIA
                    // ==================================

                    loadingAnimation.cancelAnimation()

                    // Pindah ke HomeActivity
                    openHomeActivity()

                    break

                } else {

                    // ==================================
                    // INTERNET TIDAK TERSEDIA
                    // ==================================

                    // Cek lagi setelah 2 detik
                    delay(2000)
                }
            }
        }
    }

    private fun openHomeActivity() {

        val intent = Intent(
            this@MainActivity,
            HomeActivity::class.java
        )

        intent.putExtra("START_PAGE", 4)

        startActivity(intent)

        // Hilangkan MainActivity dari back stack
        finish()
    }

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
    }
}