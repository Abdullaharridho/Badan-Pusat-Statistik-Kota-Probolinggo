
package com.example.bpskota.uisuperadmin

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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.BottomNavCurveView
import com.example.bpskota.HomeActivity
import com.example.bpskota.R
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivitySuperAdminBinding

class SuperAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperAdminBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    private lateinit var bottomNavCurve: BottomNavCurveView

    private var lastSelectedPosition = -1

    private val activeColor = Color.parseColor("#F97316")
    private val inactiveColor = Color.parseColor("#9CA3AF")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuperAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        // Indikator navigasi harus mempunyai ID di XML.
        bottomNavCurve = findViewById(R.id.bottomNavCurveSuperAdmin)

        setupViewPager()
        setupBottomNavigation()
        setupAccountMenu()

        // Beranda adalah halaman awal Super Admin.
        binding.superAdminViewPager.setCurrentItem(0, false)

        // Tunggu layout selesai dihitung sebelum memosisikan indikator.
        binding.root.post {
            updateBottomNavigation(0, animate = false)
        }
    }

    private fun setupViewPager() {
        binding.superAdminViewPager.adapter = SuperAdminPagerAdapter(this)
        binding.superAdminViewPager.isUserInputEnabled = true
        binding.superAdminViewPager.offscreenPageLimit = 1

        binding.superAdminViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateBottomNavigation(position, animate = true)
                }
            }
        )
    }

    private fun setupBottomNavigation() {

        // Urutan indeks ViewPager:
        // 0 = Beranda
        // 1 = Akun
        // 2 = Menu

        binding.navSuperHome.setOnClickListener {
            navigateToPage(0)
        }

        binding.navSuperUsers.setOnClickListener {
            navigateToPage(1)
        }

        binding.navSuperMenu.setOnClickListener {
            navigateToPage(2)
        }
    }

    private fun navigateToPage(position: Int) {
        if (position !in 0..2) return

        if (binding.superAdminViewPager.currentItem != position) {
            binding.superAdminViewPager.setCurrentItem(position, true)
        } else {
            // Pastikan indikator tetap tepat jika menu aktif ditekan lagi.
            updateBottomNavigation(position, animate = true)
        }
    }

    private fun updateBottomNavigation(
        position: Int,
        animate: Boolean
    ) {
        if (position !in 0..2) return

        val activeView = when (position) {
            0 -> binding.navSuperHome
            1 -> binding.navSuperUsers
            2 -> binding.navSuperMenu
            else -> return
        }

        updateNavigationItem(binding.navSuperHome, position == 0)
        updateNavigationItem(binding.navSuperUsers, position == 1)
        updateNavigationItem(binding.navSuperMenu, position == 2)

        val shouldAnimate =
            animate && lastSelectedPosition != -1 &&
                    lastSelectedPosition != position

        positionBottomNavCurve(activeView, shouldAnimate)

        if (shouldAnimate) {
            animateActiveNavigation(activeView)
        } else {
            resetNavigationItem(binding.navSuperHome)
            resetNavigationItem(binding.navSuperUsers)
            resetNavigationItem(binding.navSuperMenu)
        }

        lastSelectedPosition = position
    }

    private fun updateNavigationItem(
        item: ViewGroup,
        selected: Boolean
    ) {
        val icon = item.getChildAt(0) as? ImageView
        val text = item.getChildAt(1) as? TextView

        val color = if (selected) activeColor else inactiveColor

        icon?.setColorFilter(color)
        text?.setTextColor(color)

        text?.setTypeface(
            null,
            if (selected) Typeface.BOLD else Typeface.NORMAL
        )
    }

    private fun positionBottomNavCurve(
        target: View,
        animate: Boolean
    ) {
        // Lokasi dihitung berdasarkan posisi layar agar tetap akurat
        // meskipun indikator dan item navigasi berada di parent berbeda.
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

    private fun animateActiveNavigation(target: View) {
        val items = listOf(
            binding.navSuperHome,
            binding.navSuperUsers,
            binding.navSuperMenu
        )

        items.forEach { item ->
            item.animate().cancel()
            item.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .alpha(1f)
                .setDuration(160)
                .start()
        }

        target.animate().cancel()
        target.animate()
            .scaleX(1.06f)
            .scaleY(1.06f)
            .translationY(-5f * resources.displayMetrics.density)
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

    private fun setupAccountMenu() {
        binding.btnSuperAdminMenu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)

            popupMenu.menu.add(
                Menu.NONE,
                1,
                Menu.NONE,
                "Ubah Profil"
            )

            popupMenu.menu.add(
                Menu.NONE,
                2,
                Menu.NONE,
                "Keluar"
            )

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        startActivity(
                            Intent(
                                this@SuperAdminActivity,
                                SuperadminProfileActivity::class.java
                            )
                        )
                        true
                    }

                    2 -> {
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
                authSession.clearSession()

                val intent = Intent(
                    this@SuperAdminActivity,
                    HomeActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                finish()
            }
        }
    }

    private class SuperAdminPagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int) =
            when (position) {
                0 -> SuperAdminHomeFragment()
                1 -> SuperadminUserManagementFragment()
                2 -> SuperAdminMenuFragment()
                else -> SuperAdminHomeFragment()
            }
    }
}
