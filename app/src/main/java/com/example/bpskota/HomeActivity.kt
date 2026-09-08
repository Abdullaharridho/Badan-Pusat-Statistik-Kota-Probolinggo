package com.example.bpskota

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    private lateinit var homeHeader: View
    private lateinit var bottomHomeActivity: View



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        viewPager = findViewById(R.id.viewPager)

        homeHeader = findViewById(R.id.homeHeader)

        bottomHomeActivity =
            findViewById(R.id.bottomHomeActivity)

        setupViewPager()

        setupBottomNavigation()

        updateBottomNavigation(0)

        startHomeAnimation()
    }


    // =========================================================
    // SETUP VIEWPAGER
    // =========================================================

    private fun setupViewPager() {

        val fragments = listOf(
            HomeFragment(),          // 0
            InfografikFragment(),    // 1
            BeritaFragment(),        // 2
            PublikasiFragment(),     // 3
            DataFragment(),          // 4
            MoreFragment()            // 5
        )

        val adapter = MainPagerAdapter(
            this,
            fragments
        )

        viewPager.adapter = adapter

        viewPager.offscreenPageLimit = 1

        viewPager.isUserInputEnabled = true

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    updateBottomNavigation(position)
                }
            }
        )
    }


    // =========================================================
    // SETUP BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        // BERANDA
        findViewById<View>(R.id.navHome)
            .setOnClickListener {

                goToPage(0)
            }


        // INFOGRAFIK
        findViewById<View>(R.id.navInfografik)
            .setOnClickListener {

                goToPage(1)
            }


        // BERITA
        findViewById<View>(R.id.navBerita)
            .setOnClickListener {

                goToPage(2)
            }


        // PUBLIKASI
        findViewById<View>(R.id.navPublikasi)
            .setOnClickListener {

                goToPage(3)
            }


        // DATA
        findViewById<View>(R.id.navData)
            .setOnClickListener {

                goToPage(4)
            }


        // LAINNYA
        findViewById<View>(R.id.navMore)
            .setOnClickListener {

                goToPage(5)
            }
    }


    // =========================================================
    // PINDAH HALAMAN
    // =========================================================

    fun goToPage(position: Int) {

        if (::viewPager.isInitialized) {

            viewPager.setCurrentItem(
                position,
                true
            )
        }
    }


    // =========================================================
    // UPDATE BOTTOM NAVIGATION
    // =========================================================

    private fun updateBottomNavigation(position: Int) {

        val activeColor =
            Color.parseColor("#F97316")

        val inactiveColor =
            Color.parseColor("#9CA3AF")


        // =====================================================
        // ICON
        // =====================================================

        val icons = listOf(

            findViewById<ImageView>(
                R.id.navHomeIcon
            ),

            findViewById<ImageView>(
                R.id.navInfografikIcon
            ),

            findViewById<ImageView>(
                R.id.navBeritaIcon
            ),

            findViewById<ImageView>(
                R.id.navPublikasiIcon
            ),

            findViewById<ImageView>(
                R.id.navDataIcon
            ),

            findViewById<ImageView>(
                R.id.navMoreIcon
            )
        )


        // =====================================================
        // TEXT
        // =====================================================

        val texts = listOf(

            findViewById<TextView>(
                R.id.navHomeText
            ),

            findViewById<TextView>(
                R.id.navInfografikText
            ),

            findViewById<TextView>(
                R.id.navBeritaText
            ),

            findViewById<TextView>(
                R.id.navPublikasiText
            ),

            findViewById<TextView>(
                R.id.navDataText
            ),

            findViewById<TextView>(
                R.id.navMoreText
            )
        )


        // =====================================================
        // UPDATE ICON
        // =====================================================

        icons.forEachIndexed { index, imageView ->

            imageView.setColorFilter(

                if (index == position) {
                    activeColor
                } else {
                    inactiveColor
                }
            )
        }


        // =====================================================
        // UPDATE TEXT
        // =====================================================

        texts.forEachIndexed { index, textView ->

            textView.setTextColor(

                if (index == position) {
                    activeColor
                } else {
                    inactiveColor
                }
            )

            textView.setTypeface(

                null,

                if (index == position) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )
        }
    }


    // =========================================================
    // ANIMASI AWAL HOME
    // =========================================================

    private fun startHomeAnimation() {

        // =========================================================
        // HEADER
        // =========================================================

        homeHeader.alpha = 0f
        homeHeader.translationY = -180f

        homeHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(850)
            .setInterpolator(
                DecelerateInterpolator(1.8f)
            )
            .start()


        // =========================================================
        // CONTENT / VIEWPAGER
        // =========================================================

        viewPager.alpha = 0f
        viewPager.translationY = 60f

        viewPager.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(150)
            .setInterpolator(
                DecelerateInterpolator(1.8f)
            )
            .start()


        // =========================================================
        // BOTTOM NAVIGATION
        // =========================================================

        bottomHomeActivity.alpha = 0f
        bottomHomeActivity.translationY = 220f

        bottomHomeActivity.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(900)
            .setStartDelay(250)
            .setInterpolator(
                DecelerateInterpolator(1.8f)
            )
            .start()
    }
}