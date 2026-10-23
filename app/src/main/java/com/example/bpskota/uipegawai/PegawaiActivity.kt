
package com.example.bpskota.uipegawai

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.BottomNavCurveView
import com.example.bpskota.HomeActivity
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityPegawaiBinding

class PegawaiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPegawaiBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var bottomNavCurve: BottomNavCurveView

    private var lastSelectedPosition = -1

    private val activeColor = Color.parseColor("#F97316")
    private val inactiveColor = Color.parseColor("#9CA3AF")

    companion object {
        private const val PAGE_PENUGASAN = 0
        private const val PAGE_KEGIATAN = 1
        private const val PAGE_BERANDA = 2
        private const val PAGE_MAPS = 3
        private const val PAGE_PROFIL = 4
    }

    private val navigationItems: List<ViewGroup>
        get() = listOf(
            binding.navPegawaiPenugasan,
            binding.navPegawaiKegiatan,
            binding.navPegawaiHome,
            binding.navPegawaiPeta,
            binding.navPegawaiProfil
        )

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavigation(position, animate = true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPegawaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        // Kurva indikator yang sama dengan OperatorActivity.
        bottomNavCurve = findViewById(
            com.example.bpskota.R.id.bottomNavCurvePegawai
        )

        setupViewPager()
        setupBottomNavigation()
        setupAccountMenu()

        // Beranda menjadi halaman awal.
        binding.pegawaiViewPager.setCurrentItem(
            PAGE_BERANDA,
            false
        )

        binding.root.post {
            if (!isFinishing && !isDestroyed) {
                updateBottomNavigation(
                    PAGE_BERANDA,
                    animate = false
                )
            }
        }
    }

    // =========================================================
    // VIEW PAGER
    // =========================================================

    private fun setupViewPager() {
        binding.pegawaiViewPager.apply {
            adapter = PegawaiPagerAdapter(this@PegawaiActivity)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            isUserInputEnabled = true
            offscreenPageLimit = 1

            registerOnPageChangeCallback(pageChangeCallback)
        }
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        binding.navPegawaiPenugasan.setOnClickListener {
            navigateToPage(PAGE_PENUGASAN)
        }

        binding.navPegawaiKegiatan.setOnClickListener {
            navigateToPage(PAGE_KEGIATAN)
        }

        binding.navPegawaiHome.setOnClickListener {
            navigateToPage(PAGE_BERANDA)
        }

        binding.navPegawaiPeta.setOnClickListener {
            navigateToPage(PAGE_MAPS)
        }

        binding.navPegawaiProfil.setOnClickListener {
            navigateToPage(PAGE_PROFIL)
        }
    }

    private fun navigateToPage(position: Int) {
        if (position !in PAGE_PENUGASAN..PAGE_PROFIL) return

        if (binding.pegawaiViewPager.currentItem != position) {
            binding.pegawaiViewPager.setCurrentItem(position, true)
        } else {
            updateBottomNavigation(position, animate = true)
        }
    }

    private fun getNavigationPosition(pagePosition: Int): Int {
        return when (pagePosition) {
            PAGE_PENUGASAN -> 0
            PAGE_KEGIATAN -> 1
            PAGE_BERANDA -> 2
            PAGE_MAPS -> 3
            PAGE_PROFIL -> 4
            else -> -1
        }
    }

    private fun updateBottomNavigation(
        position: Int,
        animate: Boolean
    ) {
        if (position !in PAGE_PENUGASAN..PAGE_PROFIL) return

        val navigationPosition = getNavigationPosition(position)
        if (navigationPosition == -1) return

        val items = navigationItems
        val activeView = items.getOrNull(navigationPosition) ?: return

        items.forEachIndexed { index, item ->
            updateNavigationItem(
                item,
                selected = index == navigationPosition
            )
        }

        val shouldAnimate =
            animate &&
                    lastSelectedPosition != -1 &&
                    lastSelectedPosition != navigationPosition

        // Pindahkan kurva ke item navigasi aktif.
        positionBottomNavCurve(activeView, shouldAnimate)

        if (shouldAnimate) {
            animateActiveNavigation(activeView, items)
        } else {
            items.forEach { item ->
                resetNavigationItem(item)
            }
        }

        lastSelectedPosition = navigationPosition
    }

    private fun updateNavigationItem(
        item: ViewGroup,
        selected: Boolean
    ) {
        val icon = item.getChildAt(0) as? ImageView
        val label = item.getChildAt(1) as? TextView

        val color = if (selected) activeColor else inactiveColor

        icon?.setColorFilter(color)
        label?.setTextColor(color)

        label?.setTypeface(
            null,
            if (selected) Typeface.BOLD else Typeface.NORMAL
        )

        item.isSelected = selected
    }

    private fun positionBottomNavCurve(
        target: View,
        animate: Boolean
    ) {
        target.post {
            if (isFinishing || isDestroyed) return@post

            val targetLocation = IntArray(2)
            val curveLocation = IntArray(2)

            target.getLocationOnScreen(targetLocation)
            bottomNavCurve.getLocationOnScreen(curveLocation)

            val centerX =
                targetLocation[0] - curveLocation[0] +
                        target.width / 2f

            if (animate) {
                bottomNavCurve.animateCurveTo(centerX)
            } else {
                bottomNavCurve.setActiveCenterX(centerX)
            }
        }
    }

    private fun animateActiveNavigation(
        target: View,
        items: List<ViewGroup>
    ) {
        items.forEach { item ->
            item.animate().cancel()

            item.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .alpha(1f)
                .setDuration(150)
                .start()
        }

        target.animate().cancel()

        target.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .translationY(-3f * resources.displayMetrics.density)
            .setDuration(180)
            .start()
    }

    private fun resetNavigationItem(item: View) {
        item.animate().cancel()
        item.scaleX = 1f
        item.scaleY = 1f
        item.translationX = 0f
        item.translationY = 0f
        item.alpha = 1f
    }

    // =========================================================
    // MENU AKUN
    // =========================================================

    private fun setupAccountMenu() {
        binding.btnPegawaiMenu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this@PegawaiActivity, view)

            popupMenu.menu.add(
                Menu.NONE,
                1,
                Menu.NONE,
                "Keluar"
            )

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        confirmLogout()
                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()
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
                if (isFinishing || isDestroyed) return@runOnUiThread

                authSession.clearSession()

                val intent = Intent(
                    this@PegawaiActivity,
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

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.pegawaiViewPager.unregisterOnPageChangeCallback(
                pageChangeCallback
            )

            binding.pegawaiViewPager.adapter = null
        }

        super.onDestroy()
    }
}
