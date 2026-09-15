package com.example.bpskota.uisuperadmin

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.HomeActivity
import com.example.bpskota.databinding.ActivitySuperAdminBinding
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository

class SuperAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperAdminBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuperAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        setupViewPager()
        setupBottomNavigation()
        setupAccountMenu()
    }

    private fun setupViewPager() {

        binding.superAdminViewPager.adapter =
            SuperAdminPagerAdapter(this)

        binding.superAdminViewPager.isUserInputEnabled = true

        binding.superAdminViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    updateBottomNavigation(position)
                }
            }
        )
    }

    private fun setupBottomNavigation() {

        binding.navSuperHome.setOnClickListener {
            binding.superAdminViewPager.currentItem = 0
        }

        binding.navSuperUsers.setOnClickListener {
            binding.superAdminViewPager.currentItem = 1
        }
    }

    private fun setupAccountMenu() {

        binding.btnSuperAdminMenu.setOnClickListener { view ->

            val popupMenu = PopupMenu(
                this,
                view
            )

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
                        startActivity(Intent(this@SuperAdminActivity,SuperadminProfileActivity::class.java))

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
            .setMessage(
                "Apakah Anda yakin ingin keluar dari akun ini?"
            )
            .setNegativeButton(
                "Batal",
                null
            )
            .setPositiveButton(
                "Keluar"
            ) { _, _ ->

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

    private fun updateBottomNavigation(position: Int) {

        val activeColor =
            Color.parseColor("#F97316")

        val inactiveColor =
            Color.parseColor("#9CA3AF")

        val homeIcon =
            binding.navSuperHome
                .getChildAt(0) as? ImageView

        val homeText =
            binding.navSuperHome
                .getChildAt(1) as? TextView

        val usersIcon =
            binding.navSuperUsers
                .getChildAt(0) as? ImageView

        val usersText =
            binding.navSuperUsers
                .getChildAt(1) as? TextView

        homeIcon?.setColorFilter(
            if (position == 0)
                activeColor
            else
                inactiveColor
        )

        homeText?.setTextColor(
            if (position == 0)
                activeColor
            else
                inactiveColor
        )

        homeText?.setTypeface(
            null,
            if (position == 0)
                Typeface.BOLD
            else
                Typeface.NORMAL
        )

        usersIcon?.setColorFilter(
            if (position == 1)
                activeColor
            else
                inactiveColor
        )

        usersText?.setTextColor(
            if (position == 1)
                activeColor
            else
                inactiveColor
        )

        usersText?.setTypeface(
            null,
            if (position == 1)
                Typeface.BOLD
            else
                Typeface.NORMAL
        )
    }

    private class SuperAdminPagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int) =
            when (position) {

                0 -> SuperAdminHomeFragment()

                1 -> SuperadminUserManagementFragment()

                else -> SuperAdminHomeFragment()
            }
    }
}