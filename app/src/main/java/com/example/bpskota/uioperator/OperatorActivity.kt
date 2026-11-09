package com.example.bpskota.uioperator

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.BottomNavCurveView
import com.example.bpskota.HomeActivity
import com.example.bpskota.R
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.ActivityOperatorBinding

class OperatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorBinding
    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository
    private lateinit var bottomNavCurve: BottomNavCurveView

    private var lastSelectedPosition = -1
    private var ruanganPopupWindow: PopupWindow? = null

    private val activeColor = Color.parseColor("#F97316")
    private val inactiveColor = Color.parseColor("#9CA3AF")

    companion object {
        private const val PAGE_RUANGAN = 0
        private const val PAGE_PEMESANAN = 1
        private const val PAGE_SURAT_MASUK = 2
        private const val PAGE_BERANDA = 3
        private const val PAGE_MAPS = 4
        private const val PAGE_PROFIL = 5
    }

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavigation(position, animate = true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOperatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSession = BpskpAuthSession(this)
        repository = BpskpRepository()

        bottomNavCurve = findViewById(R.id.bottomNavCurveOperator)

        setupViewPager()
        setupBottomNavigation()
        setupAccountMenu()

        // Beranda tetap menjadi halaman awal.
        binding.viewPagerOperator.setCurrentItem(PAGE_BERANDA, false)

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
        binding.viewPagerOperator.apply {
            adapter = OperatorPagerAdapter(this@OperatorActivity)
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

        // Klik Ruangan menampilkan submenu custom.
        binding.navOperatorRuangan.setOnClickListener { view ->
            showRuanganSubmenu(view)
        }

        binding.navOperatorSuratMasuk.setOnClickListener {
            navigateToPage(PAGE_SURAT_MASUK)
        }

        binding.navOperatorBeranda.setOnClickListener {
            navigateToPage(PAGE_BERANDA)
        }

        binding.navOperatorMaps.setOnClickListener {
            navigateToPage(PAGE_MAPS)
        }

        binding.navOperatorProfil.setOnClickListener {
            navigateToPage(PAGE_PROFIL)
        }
    }

    private fun navigateToPage(position: Int) {
        if (position !in PAGE_RUANGAN..PAGE_PROFIL) return

        if (binding.viewPagerOperator.currentItem != position) {
            binding.viewPagerOperator.setCurrentItem(position, true)
        } else {
            updateBottomNavigation(position, animate = true)
        }
    }

    /**
     * Mengubah posisi halaman menjadi posisi navigasi utama.
     *
     * Halaman Ruangan dan Pemesanan menggunakan tombol
     * bottom navigation yang sama.
     */
    private fun getNavigationPosition(pagePosition: Int): Int {
        return when (pagePosition) {
            PAGE_RUANGAN,
            PAGE_PEMESANAN -> 0

            PAGE_SURAT_MASUK -> 1
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
        if (position !in PAGE_RUANGAN..PAGE_PROFIL) return

        val navigationPosition = getNavigationPosition(position)
        if (navigationPosition == -1) return

        val items: List<ViewGroup> = listOf(
            binding.navOperatorRuangan,
            binding.navOperatorSuratMasuk,
            binding.navOperatorBeranda,
            binding.navOperatorMaps,
            binding.navOperatorProfil
        )

        val activeView = items.getOrNull(navigationPosition) ?: return

        items.forEachIndexed { index, item ->
            updateNavigationItem(
                item,
                selected = index == navigationPosition
            )
        }

        // Ruangan dan Pemesanan tetap berada pada tombol utama
        // yang sama, sehingga kurva tidak berpindah di antaranya.
        val shouldAnimate =
            animate &&
                    lastSelectedPosition != -1 &&
                    lastSelectedPosition != navigationPosition

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
    // SUBMENU RUANGAN
    // =========================================================

    private fun showRuanganSubmenu(anchor: View) {

        // Jika popup sudah tampil, tutup saat tombol ditekan lagi.
        if (ruanganPopupWindow?.isShowing == true) {
            closeRuanganSubmenu()
            return
        }

        val density = resources.displayMetrics.density

        fun dp(value: Int): Int = (value * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(22).toFloat()
            }

            elevation = dp(12).toFloat()
            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
            translationY = dp(8).toFloat()
        }

        // -----------------------------------------------------
        // KOMPONEN KARTU SUBMENU
        // -----------------------------------------------------

        fun createMenuItem(
            title: String,
            description: String,
            iconRes: Int,
            page: Int
        ): CardView {

            val card = CardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.parseColor("#FAFAFA"))
                useCompatPadding = false
                isClickable = true
                isFocusable = true

                foreground = resolveSelectableForeground()
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            // Kotak ikon oranye.
            val iconContainer = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFF0E5"))
                    cornerRadius = dp(13).toFloat()
                }
            }

            val icon = ImageView(this).apply {
                setImageResource(iconRes)
                setColorFilter(activeColor)
                contentDescription = title
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }

            iconContainer.addView(
                icon,
                FrameLayout.LayoutParams(
                    dp(46),
                    dp(46),
                    Gravity.CENTER
                )
            )

            // Judul dan deskripsi.
            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleView = TextView(this).apply {
                text = title
                setTextColor(Color.parseColor("#292524"))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }

            val descriptionView = TextView(this).apply {
                text = description
                setTextColor(Color.parseColor("#78716C"))
                textSize = 11f
                maxLines = 2
                setLineSpacing(dp(2).toFloat(), 1f)
            }

            textContainer.addView(titleView)

            textContainer.addView(
                descriptionView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(4)
                }
            )

            // Ikon panah di sebelah kanan.
            val arrow = TextView(this).apply {
                text = "›"
                setTextColor(Color.parseColor("#A8A29E"))
                textSize = 28f
                gravity = Gravity.CENTER
                contentDescription = "Buka $title"
            }

            row.addView(
                iconContainer,
                LinearLayout.LayoutParams(dp(46), dp(46))
            )

            row.addView(
                textContainer,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginStart = dp(12)
                    marginEnd = dp(8)
                }
            )

            row.addView(
                arrow,
                LinearLayout.LayoutParams(
                    dp(24),
                    dp(36)
                )
            )

            card.addView(
                row,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            card.setOnClickListener {
                closeRuanganSubmenu()
                navigateToPage(page)
            }

            return card
        }

        // -----------------------------------------------------
        // HEADER SUBMENU
        // -----------------------------------------------------

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(10))
        }

        val headerTitle = TextView(this).apply {
            text = "Menu Ruangan"
            setTextColor(Color.parseColor("#292524"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }

        val headerDescription = TextView(this).apply {
            text = "Pilih layanan yang ingin dibuka"
            setTextColor(Color.parseColor("#78716C"))
            textSize = 11f
            setPadding(0, dp(4), 0, 0)
        }

        header.addView(headerTitle)
        header.addView(headerDescription)

        container.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // -----------------------------------------------------
        // PILIHAN RUANGAN
        // -----------------------------------------------------

        container.addView(
            createMenuItem(
                title = "Ruangan",
                description = "Kelola data dan informasi ruangan",
                iconRes = android.R.drawable.ic_menu_view,
                page = PAGE_RUANGAN
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // -----------------------------------------------------
        // PILIHAN PEMESANAN
        // -----------------------------------------------------

        container.addView(
            createMenuItem(
                title = "Pemesanan",
                description = "Kelola jadwal pemesanan ruangan",
                iconRes = android.R.drawable.ic_menu_agenda,
                page = PAGE_PEMESANAN
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        // -----------------------------------------------------
        // POPUP WINDOW
        // -----------------------------------------------------

        val popup = PopupWindow(
            container,
            dp(280),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = dp(12).toFloat()

            setBackgroundDrawable(
                GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dp(22).toFloat()
                }
            )

            animationStyle = 0
        }

        ruanganPopupWindow = popup

        popup.setOnDismissListener {
            if (ruanganPopupWindow === popup) {
                ruanganPopupWindow = null
            }
        }

        // Ukur konten untuk menentukan posisi popup.
        container.measure(
            View.MeasureSpec.makeMeasureSpec(
                dp(280),
                View.MeasureSpec.AT_MOST
            ),
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )
        )

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        val screenHeight = resources.displayMetrics.heightPixels
        val popupHeight = container.measuredHeight
        val spaceAbove = location[1]
        val spaceBelow = screenHeight - (location[1] + anchor.height)

        val showAbove =
            spaceAbove >= popupHeight || spaceAbove > spaceBelow

        popup.showAsDropDown(
            anchor,
            dp(4),
            if (showAbove) {
                -(anchor.height + popupHeight + dp(8))
            } else {
                dp(4)
            },
            Gravity.START
        )

        // -----------------------------------------------------
        // ANIMASI MUNCUL
        // -----------------------------------------------------

        container.post {
            if (!popup.isShowing) return@post

            container.animate().cancel()

            container.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()

            // Kartu muncul bertahap.
            for (index in 1 until container.childCount) {
                val item = container.getChildAt(index)

                item.alpha = 0f
                item.translationY = dp(8).toFloat()

                item.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(index * 55L)
                    .setDuration(180)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun resolveSelectableForeground(): android.graphics.drawable.Drawable? {
        val typedValue = TypedValue()

        theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            typedValue,
            true
        )

        return if (typedValue.resourceId != 0) {
            AppCompatResources.getDrawable(
                this,
                typedValue.resourceId
            )
        } else {
            null
        }
    }

    private fun closeRuanganSubmenu() {
        val popup = ruanganPopupWindow ?: return

        if (!popup.isShowing) {
            ruanganPopupWindow = null
            return
        }

        ruanganPopupWindow = null

        val content = popup.contentView
        content.animate().cancel()

        content.animate()
            .alpha(0f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(130)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (popup.isShowing) {
                    popup.dismiss()
                }
            }
            .start()
    }

    // =========================================================
    // MENU AKUN
    // =========================================================

    private fun setupAccountMenu() {
        binding.btnOperatorAccount.setOnClickListener {
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
                    this@OperatorActivity,
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
        ruanganPopupWindow?.dismiss()
        ruanganPopupWindow = null

        if (::binding.isInitialized) {
            binding.viewPagerOperator.unregisterOnPageChangeCallback(
                pageChangeCallback
            )

            binding.viewPagerOperator.adapter = null
        }

        super.onDestroy()
    }
}