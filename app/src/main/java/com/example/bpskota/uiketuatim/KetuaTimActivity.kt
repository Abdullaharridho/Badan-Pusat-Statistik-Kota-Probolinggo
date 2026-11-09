package com.example.bpskota.uiketuatim

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.BottomNavCurveView
import com.example.bpskota.HomeActivity
import com.example.bpskota.R
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityKetuaTimBinding

class KetuaTimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKetuaTimBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var bottomNavCurve: BottomNavCurveView

    companion object {
        private const val PAGE_SURAT = 0
        private const val PAGE_TIM = 1
        private const val PAGE_BERANDA = 2
        private const val PAGE_MAPS = 3
        private const val PAGE_PROFILE = 4
        private const val TOTAL_PAGES = 5
    }

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateBottomNavigation(position)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityKetuaTimBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        // Menghubungkan custom view kurva dari XML.
        bottomNavCurve = findViewById(R.id.bottomNavCurveKetuaTim)

        setupViewPager()
        setupBottomNavigation()
        setupAccountMenu()

        // Beranda menjadi halaman awal.
        binding.ketuaTimViewPager.setCurrentItem(
            PAGE_BERANDA,
            false
        )

        // Atur posisi awal kurva setelah ukuran view tersedia.
        inisialisasiPosisiKurva()
    }

    private fun setupViewPager() {
        binding.ketuaTimViewPager.adapter =
            KetuaTimPagerAdapter(this)

        binding.ketuaTimViewPager.isUserInputEnabled = true

        binding.ketuaTimViewPager.registerOnPageChangeCallback(
            pageChangeCallback
        )
    }

    private fun setupBottomNavigation() {
        binding.navKetuaSurat.setOnClickListener {
            bukaHalaman(PAGE_SURAT)
        }

        binding.navKetuaTim.setOnClickListener {
            bukaHalaman(PAGE_TIM)
        }

        binding.navKetuaHome.setOnClickListener {
            bukaHalaman(PAGE_BERANDA)
        }

        binding.navKetuaMaps.setOnClickListener {
            bukaHalaman(PAGE_MAPS)
        }

        binding.navKetuaProfile.setOnClickListener {
            bukaHalaman(PAGE_PROFILE)
        }
    }

    private fun bukaHalaman(position: Int) {
        if (position !in 0 until TOTAL_PAGES) return

        binding.ketuaTimViewPager.setCurrentItem(
            position,
            true
        )
    }

    private fun inisialisasiPosisiKurva() {
        binding.bottomKetuaTimActivity.post {
            if (isFinishing || isDestroyed) return@post

            val targetView = binding.navKetuaHome

            val centerX =
                targetView.x + targetView.width / 2f

            bottomNavCurve.setActiveCenterX(centerX)

            updateBottomNavigation(PAGE_BERANDA)
        }
    }

    /**
     * Klik ikon langsung menampilkan konfirmasi logout.
     * Tidak menggunakan PopupMenu.
     */
    private fun setupAccountMenu() {
        binding.btnKetuaTimMenu.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        repository.logout(authSession) { _, _ ->
            runOnUiThread {
                authSession.clearSession()

                val intent = Intent(
                    this@KetuaTimActivity,
                    HomeActivity::class.java
                ).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateBottomNavigation(position: Int) {
        updateNavigationItem(
            binding.navKetuaSurat,
            position == PAGE_SURAT
        )

        updateNavigationItem(
            binding.navKetuaTim,
            position == PAGE_TIM
        )

        updateNavigationItem(
            binding.navKetuaHome,
            position == PAGE_BERANDA
        )

        updateNavigationItem(
            binding.navKetuaMaps,
            position == PAGE_MAPS
        )

        updateNavigationItem(
            binding.navKetuaProfile,
            position == PAGE_PROFILE
        )

        // Kurva mengikuti menu yang aktif.
        bottomNavCurve.post {
            if (isFinishing || isDestroyed) return@post

            val targetView = when (position) {
                PAGE_SURAT -> binding.navKetuaSurat
                PAGE_TIM -> binding.navKetuaTim
                PAGE_BERANDA -> binding.navKetuaHome
                PAGE_MAPS -> binding.navKetuaMaps
                PAGE_PROFILE -> binding.navKetuaProfile
                else -> return@post
            }

            val centerX =
                targetView.x + targetView.width / 2f

            bottomNavCurve.animateCurveTo(centerX)
        }
    }

    private fun updateNavigationItem(
        item: ViewGroup,
        selected: Boolean
    ) {
        val activeColor = Color.parseColor("#F97316")
        val inactiveColor = Color.parseColor("#9CA3AF")
        val color = if (selected) activeColor else inactiveColor

        val icon = item.getChildAt(0) as? ImageView
        val label = item.getChildAt(1) as? TextView

        icon?.setColorFilter(color)

        label?.setTextColor(color)
        label?.setTypeface(
            null,
            if (selected) Typeface.BOLD else Typeface.NORMAL
        )

        // Aksesibilitas: tandai item navigasi yang aktif.
        item.isSelected = selected
        item.contentDescription =
            if (selected) {
                "${label?.text ?: "Navigasi"}, halaman aktif"
            } else {
                label?.text?.toString()
            }
    }

    override fun onDestroy() {
        binding.ketuaTimViewPager.unregisterOnPageChangeCallback(
            pageChangeCallback
        )

        super.onDestroy()
    }
}