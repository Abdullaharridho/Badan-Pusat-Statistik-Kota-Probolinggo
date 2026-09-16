package com.example.bpskota

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var homeHeader: View
    private lateinit var bottomHomeActivity: View
    private lateinit var swipeRefreshHome: SwipeRefreshLayout
    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var informasiMenu: LinearLayout
    private lateinit var informasiOverlay: View
    private lateinit var bottomNavCurve: BottomNavCurveView
    private lateinit var searchInput: EditText
    private lateinit var searchBack: ImageView
    private lateinit var headerNormalContent: View

    private var informasiMenuVisible = false
    private lateinit var activityTracker: ActivityTracker

    val currentPage: Int
        get() = viewPager.currentItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        // =====================================================
        // TRACKING APP OPEN
        // =====================================================



        viewPager = findViewById(R.id.viewPager)
        homeHeader = findViewById(R.id.homeHeader)
        bottomHomeActivity = findViewById(R.id.bottomHomeActivity)
        swipeRefreshHome = findViewById(R.id.swipeRefreshHome)
        informasiMenu = findViewById(R.id.informasiMenu)
        informasiOverlay = findViewById(R.id.informasiOverlay)
        searchInput = findViewById(R.id.searchInput)
        searchBack = findViewById(R.id.searchBack)
        headerNormalContent = findViewById(R.id.headerNormalContent)

        setupBottomNavCurve()
        setupViewPager()

        val startPage = intent.getIntExtra("START_PAGE", 4)
        viewPager.setCurrentItem(startPage, false)

        setupBottomNavigation()
        setupInformasiMenu()
        setupSwipeRefresh()
        setupSearch()

        updateBottomNavigation(4)
        startHomeAnimation()
    }

    // =========================================================
    // BOTTOM NAV CURVE
    // =========================================================

    private fun setupBottomNavCurve() {

        val bottomContainer =
            bottomHomeActivity as? ViewGroup ?: return

        val frameLayout =
            bottomContainer.getChildAt(0) as? ViewGroup ?: return

        for (index in 0 until frameLayout.childCount) {

            val child = frameLayout.getChildAt(index)

            if (child is BottomNavCurveView) {
                bottomNavCurve = child
                break
            }
        }
    }

    // =========================================================
    // VIEW PAGER
    // =========================================================

    private fun setupViewPager() {

        val fragments = listOf(
            BeritaFragment(),
            InfografikFragment(),
            PublikasiFragment(),
            DataFragment(),
            HomeFragment(),
            MoreFragment(),
            LoginFragment()
        )

        pagerAdapter = MainPagerAdapter(
            this,
            fragments
        )

        viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 1
            isUserInputEnabled = true
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {

                    hideInformasiMenu()
                    updateBottomNavigation(position)

                    val screenName = when (position) {
                        0 -> "Berita"
                        1 -> "Infografik"
                        2 -> "Publikasi"
                        3 -> "Data"
                        4 -> "Home"
                        5 -> "More"
                        6 -> "Login"
                        else -> null
                    }

                    if (screenName != null) {
                        activityTracker.trackScreen(screenName)
                    }




                    swipeRefreshHome.isRefreshing = false

                    viewPager.post {

                        val fragment =
                            pagerAdapter.getFragment(position)

                        if (fragment is RefreshableFragment) {

                            /*
                             * SwipeRefresh tetap diaktifkan untuk
                             * fragment yang mendukung refresh.
                             *
                             * Apakah boleh refresh atau tidak ketika
                             * user menarik layar ditentukan oleh
                             * setOnChildScrollUpCallback().
                             */
                            swipeRefreshHome.isEnabled = true

                            fragment.updateRefreshState()

                        } else {

                            swipeRefreshHome.isEnabled = false
                        }
                    }
                }
            }
        )
    }

    // =========================================================
    // SWIPE REFRESH
    // =========================================================

    private fun setupSwipeRefresh() {

        /*
         * Bagian terpenting:
         *
         * SwipeRefreshLayout berada di HomeActivity,
         * sedangkan ScrollView / RecyclerView berada
         * di dalam masing-masing Fragment.
         *
         * Callback ini membuat SwipeRefreshLayout bertanya:
         *
         * "Apakah halaman yang sedang aktif masih bisa
         *  scroll ke atas?"
         *
         * Jika TRUE:
         *      jangan izinkan pull-to-refresh.
         *
         * Jika FALSE:
         *      halaman sudah paling atas,
         *      pull-to-refresh boleh dilakukan.
         */
        swipeRefreshHome.setOnChildScrollUpCallback { _, _ ->

            val position = viewPager.currentItem

            val fragment =
                pagerAdapter.getFragment(position)

            /*
             * Fragment yang tidak mendukung refresh
             * tidak perlu diproses.
             */
            if (fragment !is RefreshableFragment) {
                false
            } else {

                val fragmentView = fragment.view

                if (fragmentView == null) {

                    false

                } else {

                    /*
                     * Cari ScrollView / NestedScrollView /
                     * RecyclerView yang benar-benar melakukan
                     * scroll di dalam fragment.
                     */
                    val scrollableView =
                        findScrollableView(fragmentView)

                    if (scrollableView != null) {

                        /*
                         * TRUE  = masih bisa scroll ke atas
                         * FALSE = sudah berada paling atas
                         */
                        scrollableView.canScrollVertically(-1)

                    } else {

                        /*
                         * Fallback jika fragment tidak memiliki
                         * container scroll yang dikenali.
                         */
                        fragmentView.canScrollVertically(-1)
                    }
                }
            }
        }

        swipeRefreshHome.setOnRefreshListener {

            refreshCurrentFragment()
        }

        /*
         * Tentukan apakah halaman awal mendukung refresh.
         */
        val fragment =
            pagerAdapter.getFragment(viewPager.currentItem)

        swipeRefreshHome.isEnabled =
            fragment is RefreshableFragment
    }

    // =========================================================
    // FIND SCROLLABLE VIEW
    // =========================================================

    /**
     * Mencari View yang benar-benar menangani scroll vertikal
     * di dalam Fragment.
     *
     * Tidak membutuhkan ID baru.
     *
     * Saat ini mendukung:
     * - ScrollView
     * - NestedScrollView
     * - RecyclerView
     * - ListView
     */
    private fun findScrollableView(view: View): View? {

        when (view) {

            is ScrollView,
            is NestedScrollView,
            is RecyclerView,
            is ListView -> {
                return view
            }
        }

        if (view is ViewGroup) {

            for (index in 0 until view.childCount) {

                val child =
                    view.getChildAt(index)

                val result =
                    findScrollableView(child)

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    // =========================================================
    // REFRESH CURRENT FRAGMENT
    // =========================================================

    private fun refreshCurrentFragment() {

        val position =
            viewPager.currentItem

        val fragment =
            pagerAdapter.getFragment(position)

        /*
         * Pastikan fragment memang mendukung refresh.
         */
        if (fragment !is RefreshableFragment) {

            swipeRefreshHome.isRefreshing = false

            return
        }

        val fragmentView =
            fragment.view

        if (fragmentView == null) {

            swipeRefreshHome.isRefreshing = false

            return
        }

        /*
         * Cari scroll container sebenarnya.
         */
        val scrollableView =
            findScrollableView(fragmentView)

        /*
         * Cek posisi halaman.
         */
        val isNotAtTop = if (scrollableView != null) {

            scrollableView.canScrollVertically(-1)

        } else {

            fragmentView.canScrollVertically(-1)
        }

        /*
         * Jika masih bisa scroll ke atas,
         * berarti user belum berada di posisi paling atas.
         *
         * Jangan lakukan refresh.
         */
        if (isNotAtTop) {

            swipeRefreshHome.isRefreshing = false

            return
        }

        /*
         * Sudah benar-benar di posisi paling atas.
         * Sekarang refresh diperbolehkan.
         */
        fragment.refreshData()
    }

    // =========================================================
    // PUBLIC REFRESH CONTROL
    // =========================================================

    fun setSwipeRefreshEnabled(enabled: Boolean) {

        if (::swipeRefreshHome.isInitialized) {

            swipeRefreshHome.isEnabled = enabled
        }
    }

    fun finishSwipeRefresh() {

        if (::swipeRefreshHome.isInitialized) {

            swipeRefreshHome.isRefreshing = false
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private fun setupSearch() {

        findViewById<View>(R.id.search).setOnClickListener {

            if (searchInput.visibility == View.VISIBLE) {

                executeSearch()

            } else {

                showSearchMode()
            }
        }

        searchBack.setOnClickListener {

            hideSearchMode()
        }

        searchInput.setOnEditorActionListener { _, actionId, event ->

            if (
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                (
                        event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                                event.action == KeyEvent.ACTION_DOWN
                        )
            ) {

                executeSearch()

                true

            } else {

                false
            }
        }
    }

    private fun showSearchMode() {

        headerNormalContent.visibility =
            View.GONE

        searchBack.visibility =
            View.VISIBLE

        searchInput.visibility =
            View.VISIBLE

        searchInput.requestFocus()

        searchInput.post {

            val inputMethodManager =
                getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

            inputMethodManager.showSoftInput(
                searchInput,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
    }

    private fun hideSearchMode() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            searchInput.windowToken,
            0
        )

        searchInput.clearFocus()

        searchInput.text.clear()

        searchInput.visibility =
            View.GONE

        searchBack.visibility =
            View.GONE

        headerNormalContent.visibility =
            View.VISIBLE
    }

    private fun executeSearch() {

        val keyword =
            searchInput.text.toString().trim()

        if (keyword.isEmpty()) {

            searchInput.error =
                "Masukkan kata pencarian"

            searchInput.requestFocus()

            return
        }

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            searchInput.windowToken,
            0
        )

        val intent =
            Intent(
                this,
                SearchActivity::class.java
            )

        intent.putExtra(
            "keyword",
            keyword
        )

        startActivity(intent)
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        findViewById<View>(
            R.id.navInfografik
        ).setOnClickListener {

            val target =
                findViewById<View>(
                    R.id.navInfografik
                )

            animateActiveNavigation(target)

            positionBottomNavCurve(
                target,
                true
            )

            showInformasiPanel()
        }

        findViewById<View>(
            R.id.navData
        ).setOnClickListener {

            goToPage(3)
        }

        findViewById<View>(
            R.id.navHome
        ).setOnClickListener {

            goToPage(4)
        }

        findViewById<View>(
            R.id.navMore
        ).setOnClickListener {

            goToPage(5)
        }

        findViewById<View>(
            R.id.navAkun
        ).setOnClickListener {

            goToPage(6)
        }
    }

    private fun animateActiveNavigation(
        targetView: View
    ) {

        val navigationItems =
            listOf(
                R.id.navInfografik,
                R.id.navData,
                R.id.navHome,
                R.id.navMore,
                R.id.navAkun
            )

        navigationItems.forEach { id ->

            val item =
                findViewById<View>(id)

            item.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(
                    DecelerateInterpolator()
                )
                .start()
        }

        targetView.animate()
            .scaleX(1.10f)
            .scaleY(1.10f)
            .translationY(-12f)
            .setDuration(350)
            .setInterpolator(
                OvershootInterpolator(1.5f)
            )
            .start()
    }

    private fun positionBottomNavCurve(
        targetView: View,
        animate: Boolean
    ) {

        if (!::bottomNavCurve.isInitialized) {
            return
        }

        bottomNavCurve.post {

            val targetLocation =
                IntArray(2)

            val curveLocation =
                IntArray(2)

            targetView.getLocationOnScreen(
                targetLocation
            )

            bottomNavCurve.getLocationOnScreen(
                curveLocation
            )

            val targetCenterX =
                targetLocation[0] +
                        (targetView.width / 2f)

            val relativeX =
                targetCenterX -
                        curveLocation[0]

            if (animate) {

                bottomNavCurve.animateCurveTo(
                    relativeX
                )

            } else {

                bottomNavCurve.setActiveCenterX(
                    relativeX
                )
            }
        }
    }

    // =========================================================
    // INFORMASI MENU
    // =========================================================

    private fun setupInformasiMenu() {

        findViewById<View>(
            R.id.menuBerita
        ).setOnClickListener {

            hideInformasiMenu()

            goToPage(0)
        }

        findViewById<View>(
            R.id.menuInfografik
        ).setOnClickListener {

            hideInformasiMenu()

            goToPage(1)
        }

        findViewById<View>(
            R.id.menuPublikasi
        ).setOnClickListener {

            hideInformasiMenu()

            goToPage(2)
        }

        informasiMenu.visibility =
            View.GONE

        informasiOverlay.setOnClickListener {

            hideInformasiMenu()
        }
    }

    private fun showInformasiPanel() {

        if (informasiMenuVisible) {

            hideInformasiMenu()

            return
        }

        positionInformasiMenu()

        informasiMenuVisible = true

        informasiOverlay.apply {

            visibility =
                View.VISIBLE

            alpha = 0f

            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }

        informasiMenu.apply {

            visibility =
                View.VISIBLE

            alpha = 0f

            translationY =
                height * 0.2f

            scaleX = 0.8f
            scaleY = 0.8f

            animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(
                    OvershootInterpolator(1.2f)
                )
                .start()
        }
    }

    private fun hideInformasiMenu() {

        if (!informasiMenuVisible) {
            return
        }

        informasiMenuVisible = false

        informasiOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {

                informasiOverlay.visibility =
                    View.GONE

                informasiOverlay.alpha =
                    1f
            }
            .start()

        informasiMenu.animate()
            .alpha(0f)
            .translationY(
                informasiMenu.height * 0.1f
            )
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(200)
            .setInterpolator(
                DecelerateInterpolator(1.5f)
            )
            .withEndAction {

                informasiMenu.visibility =
                    View.GONE

                informasiMenu.alpha =
                    1f

                informasiMenu.translationY =
                    0f

                informasiMenu.translationX =
                    0f

                informasiMenu.scaleX =
                    1f

                informasiMenu.scaleY =
                    1f
            }
            .start()
    }

    private fun positionInformasiMenu() {

        val navInformasi =
            findViewById<View>(
                R.id.navInfografik
            )

        val root =
            informasiMenu.parent as View

        informasiMenu.translationX =
            0f

        informasiMenu.post {

            val navLocation =
                IntArray(2)

            val rootLocation =
                IntArray(2)

            navInformasi.getLocationOnScreen(
                navLocation
            )

            root.getLocationOnScreen(
                rootLocation
            )

            val navCenterX =
                navLocation[0] -
                        rootLocation[0] +
                        navInformasi.width / 2f

            val menuLeft =
                navCenterX -
                        informasiMenu.width / 2f

            informasiMenu.translationX =
                menuLeft -
                        informasiMenu.left
        }
    }

    // =========================================================
    // PAGE NAVIGATION
    // =========================================================

    fun goToPage(position: Int) {

        hideInformasiMenu()

        if (::viewPager.isInitialized) {

            viewPager.setCurrentItem(
                position,
                true
            )
        }
    }

    // =========================================================
    // BOTTOM NAV STATE
    // =========================================================

    private fun updateBottomNavigation(
        position: Int
    ) {

        val activeColor =
            Color.parseColor("#F97316")

        val inactiveColor =
            Color.parseColor("#9CA3AF")

        val informasiAktif =
            position in 0..2

        val dataAktif =
            position == 3

        val homeAktif =
            position == 4

        val moreAktif =
            position == 5

        val akunAktif =
            position == 6

        setNavigationState(
            findViewById(R.id.navInfografikIcon),
            findViewById(R.id.navInfografikText),
            informasiAktif,
            activeColor,
            inactiveColor
        )

        setNavigationState(
            findViewById(R.id.navDataIcon),
            findViewById(R.id.navDataText),
            dataAktif,
            activeColor,
            inactiveColor
        )

        setNavigationState(
            findViewById(R.id.navHomeIcon),
            findViewById(R.id.navHomeText),
            homeAktif,
            activeColor,
            inactiveColor
        )

        setNavigationState(
            findViewById(R.id.navMoreIcon),
            findViewById(R.id.navMoreText),
            moreAktif,
            activeColor,
            inactiveColor
        )

        setNavigationState(
            findViewById(R.id.navAkunIcon),
            findViewById(R.id.navAkunText),
            akunAktif,
            activeColor,
            inactiveColor
        )

        val activeView =
            when {

                informasiAktif ->
                    findViewById<View>(
                        R.id.navInfografik
                    )

                dataAktif ->
                    findViewById<View>(
                        R.id.navData
                    )

                homeAktif ->
                    findViewById<View>(
                        R.id.navHome
                    )

                moreAktif ->
                    findViewById<View>(
                        R.id.navMore
                    )

                akunAktif ->
                    findViewById<View>(
                        R.id.navAkun
                    )

                else ->
                    findViewById<View>(
                        R.id.navHome
                    )
            }

        animateActiveNavigation(
            activeView
        )

        positionBottomNavCurve(
            activeView,
            true
        )
    }

    private fun setNavigationState(
        icon: ImageView,
        text: TextView,
        active: Boolean,
        activeColor: Int,
        inactiveColor: Int
    ) {

        val color =
            if (active) {
                activeColor
            } else {
                inactiveColor
            }

        icon.setColorFilter(color)

        text.setTextColor(color)

        text.setTypeface(
            null,
            if (active) {
                Typeface.BOLD
            } else {
                Typeface.NORMAL
            }
        )
    }

    // =========================================================
    // HOME ANIMATION
    // =========================================================

    private fun startHomeAnimation() {

        homeHeader.alpha = 0f

        homeHeader.translationY =
            -180f

        homeHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(
                DecelerateInterpolator(1.5f)
            )
            .start()

        viewPager.alpha = 0f

        viewPager.translationY =
            60f

        viewPager.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(100)
            .setInterpolator(
                DecelerateInterpolator(1.5f)
            )
            .start()

        bottomHomeActivity.alpha =
            0f

        bottomHomeActivity.translationY =
            220f

        bottomHomeActivity.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(200)
            .setInterpolator(
                OvershootInterpolator(1.1f)
            )
            .withEndAction {

                if (::bottomNavCurve.isInitialized) {

                    val home =
                        findViewById<View>(
                            R.id.navHome
                        )

                    positionBottomNavCurve(
                        home,
                        false
                    )

                    animateActiveNavigation(
                        home
                    )
                }
            }
            .start()
    }
}