package com.example.bpskota

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var textBps: TextView
    private lateinit var textKota: TextView
    private lateinit var progressBar: ProgressBar

    private var dialogKoneksi: AlertDialog? = null
    private var sedangMemeriksa = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadingAnimation = findViewById(R.id.loadingAnimation)
        textBps = findViewById(R.id.textBps)
        textKota = findViewById(R.id.textKota)
        progressBar = findViewById(R.id.progressBar)

        loadingAnimation.setAnimation("Loading_Animation.json")
        loadingAnimation.repeatCount = -1
        loadingAnimation.playAnimation()

        textBps.alpha = 0f
        textKota.alpha = 0f
        progressBar.alpha = 0f

        animateTexts()
        startLoading()
    }

    private fun animateTexts() {
        val bpsAnimation = ObjectAnimator.ofFloat(
            textBps, View.ALPHA, 0f, 1f
        )
        val bpsTranslation = ObjectAnimator.ofFloat(
            textBps, View.TRANSLATION_Y, 25f, 0f
        )
        val kotaAnimation = ObjectAnimator.ofFloat(
            textKota, View.ALPHA, 0f, 1f
        )
        val kotaTranslation = ObjectAnimator.ofFloat(
            textKota, View.TRANSLATION_Y, 25f, 0f
        )
        val progressAnimation = ObjectAnimator.ofFloat(
            progressBar, View.ALPHA, 0f, 1f
        )

        AnimatorSet().apply {
            playTogether(bpsAnimation, bpsTranslation)
            duration = 700
            interpolator = DecelerateInterpolator()
            start()
        }

        textKota.postDelayed({
            AnimatorSet().apply {
                playTogether(kotaAnimation, kotaTranslation)
                duration = 700
                interpolator = DecelerateInterpolator()
                start()
            }
        }, 300)

        progressBar.postDelayed({
            progressAnimation.duration = 500
            progressAnimation.start()
        }, 800)
    }

    private fun startLoading() {
        lifecycleScope.launch {
            delay(5000)
            periksaKoneksi()
        }
    }

    private fun periksaKoneksi() {
        if (sedangMemeriksa || isFinishing || isDestroyed) return
        sedangMemeriksa = true

        lifecycleScope.launch {
            val internetTersedia = withContext(Dispatchers.IO) {
                isInternetAvailable()
            }
            sedangMemeriksa = false

            if (isFinishing || isDestroyed) return@launch

            if (internetTersedia) {
                openHomeActivity()
            } else {
                tampilkanDialogKoneksi()
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        val hasInternet = capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
        val isValidated = capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )

        if (!hasInternet || !isValidated) return false

        var connection: HttpURLConnection? = null
        return try {
            connection = URL("https://www.google.com/generate_204")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.responseCode == 204
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun tampilkanDialogKoneksi() {
        if (isFinishing || isDestroyed) return
        if (dialogKoneksi?.isShowing == true) return

        // 1. Inflate Layout Kustom
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null)

        // 2. Inisialisasi View dari Layout Kustom
        val btnMuatUlang = dialogView.findViewById<MaterialButton>(R.id.btnMuatUlang)
        val btnKeluar = dialogView.findViewById<MaterialButton>(R.id.btnKeluar)

        // 3. Buat AlertDialog dengan View Kustom
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // SANGAT PENTING: Jadikan background window transparan agar bg_rounded_dialog terlihat
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogKoneksi = dialog

        // 4. Set Action untuk Tombol
        btnMuatUlang.setOnClickListener {
            if (sedangMemeriksa) return@setOnClickListener
            dialog.dismiss()
            periksaKoneksi()
        }

        btnKeluar.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        // 5. Terapkan Animasi Programmatic saat Muncul
        dialog.setOnShowListener {
            val windowView = dialog.window?.decorView
            windowView?.let { view ->
                view.alpha = 0f
                view.scaleX = 0.80f
                view.scaleY = 0.80f

                view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        dialog.show()
    }

    private fun openHomeActivity() {
        if (isFinishing || isDestroyed) return
        loadingAnimation.cancelAnimation()
        val intent = Intent(this@MainActivity, HomeActivity::class.java)
        intent.putExtra("START_PAGE", 4)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        dialogKoneksi?.dismiss()
        loadingAnimation.cancelAnimation()
        super.onDestroy()
    }
}