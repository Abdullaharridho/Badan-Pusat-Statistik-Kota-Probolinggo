
package com.example.bpskota.uipimpinan

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.BottomNavCurveView
import com.example.bpskota.HomeActivity
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityPimpinanBinding

class PimpinanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPimpinanBinding
    private lateinit var bottomNavCurve: BottomNavCurveView

    private val repository by lazy {
        BpskpRepository()
    }

    private val authSession by lazy {
        BpskpAuthSession(this)
    }

    // Urutan harus sama dengan posisi fragment pada PimpinanPagerAdapter.
    private val navItems by lazy {
        listOf(
            binding.navPimpinanKegiatan, // 0
            binding.navPimpinanPeta,     // 1
            binding.navPimpinanBeranda,  // 2
            binding.navPimpinanLainnya,  // 3
            binding.navPimpinanProfil    // 4
        )
    }

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                updateBottomNavigation(position, true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPimpinanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavCurve = findViewById(
            com.example.bpskota.R.id.bottomNavCurvePimpinan
        )

        setupViewPager()
        setupBottomNavigation()
        setupAccountMenu()

        // Beranda tetap menjadi halaman awal di posisi tengah.
        binding.pimpinanViewPager.setCurrentItem(2, false)

        binding.navPimpinanBeranda.post {
            updateBottomNavigation(2, false)
        }
    }

    private fun setupViewPager() {
        binding.pimpinanViewPager.adapter =
            PimpinanPagerAdapter(this)

        binding.pimpinanViewPager.orientation =
            ViewPager2.ORIENTATION_HORIZONTAL

        binding.pimpinanViewPager.isUserInputEnabled = true
        binding.pimpinanViewPager.offscreenPageLimit = 1

        binding.pimpinanViewPager.registerOnPageChangeCallback(
            pageChangeCallback
        )
    }

    private fun setupBottomNavigation() {
        navItems.forEachIndexed { position, item ->
            item.setOnClickListener {
                if (binding.pimpinanViewPager.currentItem != position) {
                    binding.pimpinanViewPager.setCurrentItem(
                        position,
                        true
                    )
                } else {
                    updateBottomNavigation(position, true)
                }
            }
        }
    }

    private fun updateBottomNavigation(
        position: Int,
        animate: Boolean
    ) {
        if (position !in navItems.indices) return

        navItems.forEachIndexed { index, item ->
            val icon = item.getChildAt(0) as ImageView
            val label = item.getChildAt(1) as TextView
            val selected = index == position

            icon.setColorFilter(
                Color.parseColor(
                    if (selected) "#F97316" else "#78716C"
                )
            )

            label.setTextColor(
                Color.parseColor(
                    if (selected) "#F97316" else "#78716C"
                )
            )

            if (animate) {
                item.animate()
                    .scaleX(if (selected) 1.04f else 1f)
                    .scaleY(if (selected) 1.04f else 1f)
                    .alpha(if (selected) 1f else 0.82f)
                    .setDuration(180)
                    .start()
            } else {
                item.scaleX = if (selected) 1.04f else 1f
                item.scaleY = if (selected) 1.04f else 1f
                item.alpha = if (selected) 1f else 0.82f
            }
        }

        positionBottomNavCurve(position, animate)
    }

    private fun positionBottomNavCurve(
        position: Int,
        animate: Boolean
    ) {
        if (position !in navItems.indices) return

        val selectedItem = navItems[position]

        selectedItem.post {
            if (isFinishing || isDestroyed) return@post

            val targetLocation = IntArray(2)
            val curveLocation = IntArray(2)

            selectedItem.getLocationOnScreen(targetLocation)
            bottomNavCurve.getLocationOnScreen(curveLocation)

            val centerX =
                targetLocation[0] - curveLocation[0] +
                        selectedItem.width / 2f

            if (animate) {
                bottomNavCurve.animateCurveTo(centerX)
            } else {
                bottomNavCurve.setActiveCenterX(centerX)
            }
        }
    }

    private fun setupAccountMenu() {
        binding.btnPimpinanMenu.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Keluar dari akun?")
            .setMessage(
                "Apakah kamu yakin ingin keluar dari aplikasi?"
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                performLogout()
            }
            .show()
    }

    private fun performLogout() {
        repository.logout(authSession) { _, _ ->
            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                authSession.clearSession()

                val intent = Intent(
                    this@PimpinanActivity,
                    HomeActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        binding.pimpinanViewPager.unregisterOnPageChangeCallback(
            pageChangeCallback
        )
        binding.pimpinanViewPager.adapter = null

        super.onDestroy()
    }
}
