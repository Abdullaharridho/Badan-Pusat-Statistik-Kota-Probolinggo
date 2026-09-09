package com.example.bpskota

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    private lateinit var homeHeader: View
    private lateinit var bottomHomeActivity: View

    private lateinit var swipeRefreshHome: SwipeRefreshLayout

    private lateinit var pagerAdapter: MainPagerAdapter

    private lateinit var informasiMenu: LinearLayout
    private lateinit var informasiOverlay: View

    // =====================================================
    // SEARCH
    // =====================================================

    private lateinit var searchInput: EditText
    private lateinit var searchBack: ImageView
    private lateinit var headerNormalContent: View

    private var informasiMenuVisible = false


    /*
     * Halaman ViewPager yang sedang aktif.
     *
     * 0 = Home
     * 1 = Infografik
     * 2 = Berita
     * 3 = Publikasi
     * 4 = Data
     * 5 = More
     */
    val currentPage: Int
        get() = viewPager.currentItem


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)


        // =====================================================
        // INIT VIEW
        // =====================================================

        viewPager =
            findViewById(R.id.viewPager)

        homeHeader =
            findViewById(R.id.homeHeader)

        bottomHomeActivity =
            findViewById(R.id.bottomHomeActivity)

        swipeRefreshHome =
            findViewById(R.id.swipeRefreshHome)

        informasiMenu =
            findViewById(R.id.informasiMenu)

        informasiOverlay =
            findViewById(R.id.informasiOverlay)


        // =====================================================
        // INIT SEARCH VIEW
        // =====================================================

        searchInput =
            findViewById(R.id.searchInput)

        searchBack =
            findViewById(R.id.searchBack)

        headerNormalContent =
            findViewById(R.id.headerNormalContent)


        // =====================================================
        // SETUP
        // =====================================================

        setupViewPager()

        setupBottomNavigation()

        setupInformasiMenu()

        setupSwipeRefresh()

        setupSearch()

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
            MoreFragment()           // 5
        )


        pagerAdapter =
            MainPagerAdapter(
                this,
                fragments
            )


        viewPager.adapter =
            pagerAdapter


        viewPager.offscreenPageLimit =
            1


        viewPager.isUserInputEnabled =
            true


        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(
                    position: Int
                ) {

                    super.onPageSelected(
                        position
                    )


                    hideInformasiMenu()


                    updateBottomNavigation(
                        position
                    )


                    /*
                     * Matikan spinner ketika
                     * berpindah halaman.
                     */
                    swipeRefreshHome.isRefreshing =
                        false


                    /*
                     * Jangan langsung menentukan
                     * enabled / disabled di sini.
                     *
                     * Fragment yang sedang aktif akan
                     * menentukan berdasarkan posisi
                     * scroll-nya.
                     */
                    viewPager.post {

                        val fragment =
                            pagerAdapter.getFragment(
                                position
                            )


                        if (fragment is RefreshableFragment) {

                            // Aktifkan dulu agar gesture pull-to-refresh tersedia.
                            swipeRefreshHome.isEnabled = true

                            // Setelah fragment siap, fragment menentukan
                            // apakah posisi scroll memang sudah di paling atas.
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
    // SETUP PULL TO REFRESH
    // =========================================================

    private fun setupSwipeRefresh() {

        /*
         * Default awal aktif.
         *
         * HomeFragment nantinya akan menentukan
         * apakah benar-benar berada di posisi paling atas.
         */
        swipeRefreshHome.isEnabled =
            true


        swipeRefreshHome.setOnRefreshListener {

            refreshCurrentFragment()
        }
    }


    // =========================================================
    // REFRESH FRAGMENT AKTIF
    // =========================================================

    private fun refreshCurrentFragment() {

        val position =
            viewPager.currentItem


        val fragment =
            pagerAdapter.getFragment(
                position
            )


        if (
            fragment is RefreshableFragment
        ) {

            fragment.refreshData()

        } else {

            /*
             * Jika fragment tidak mendukung refresh,
             * hentikan spinner.
             */
            swipeRefreshHome.isRefreshing =
                false
        }
    }


    // =========================================================
    // UPDATE STATUS SWIPE REFRESH
    // =========================================================

    fun setSwipeRefreshEnabled(
        enabled: Boolean
    ) {

        if (
            !::swipeRefreshHome.isInitialized
        ) {
            return
        }


        swipeRefreshHome.isEnabled =
            enabled
    }


    // =========================================================
    // SELESAI REFRESH
    // =========================================================

    fun finishSwipeRefresh() {

        if (
            !::swipeRefreshHome.isInitialized
        ) {
            return
        }


        swipeRefreshHome.isRefreshing =
            false
    }


    // =========================================================
    // SETUP SEARCH
    // =========================================================

    private fun setupSearch() {

        /*
         * Tombol search pada header.
         *
         * Ketika diklik, header normal disembunyikan
         * dan search input ditampilkan.
         */
        findViewById<View>(
            R.id.search
        ).setOnClickListener {

            /*
             * Jika search input sedang terlihat,
             * berarti tombol ini digunakan untuk
             * menjalankan pencarian.
             */
            if (searchInput.visibility == View.VISIBLE) {

                executeSearch()

            } else {

                showSearchMode()
            }
        }


        /*
         * Tombol kembali pada mode pencarian.
         */
        searchBack.setOnClickListener {

            hideSearchMode()
        }


        /*
         * Jalankan pencarian ketika user menekan
         * tombol Search pada keyboard.
         */
        searchInput.setOnEditorActionListener {
                _,
                actionId,
                event ->

            if (
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                (
                        event != null &&
                                event.keyCode == KeyEvent.KEYCODE_ENTER &&
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


    // =========================================================
    // TAMPILKAN MODE SEARCH
    // =========================================================

    private fun showSearchMode() {

        /*
         * Sembunyikan header normal:
         *
         * Logo
         * Judul
         * Subtitle
         */
        headerNormalContent.visibility =
            View.GONE


        /*
         * Tampilkan tombol kembali.
         */
        searchBack.visibility =
            View.VISIBLE


        /*
         * Tampilkan EditText search.
         */
        searchInput.visibility =
            View.VISIBLE


        /*
         * Fokus ke EditText.
         */
        searchInput.requestFocus()


        /*
         * Tampilkan keyboard setelah layout
         * selesai diproses Android.
         */
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


    // =========================================================
    // SEMBUNYIKAN MODE SEARCH
    // =========================================================

    private fun hideSearchMode() {

        /*
         * Sembunyikan keyboard.
         */
        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager


        inputMethodManager.hideSoftInputFromWindow(
            searchInput.windowToken,
            0
        )


        /*
         * Hilangkan fokus.
         */
        searchInput.clearFocus()


        /*
         * Bersihkan input.
         */
        searchInput.text.clear()


        /*
         * Sembunyikan search input.
         */
        searchInput.visibility =
            View.GONE


        /*
         * Sembunyikan tombol kembali.
         */
        searchBack.visibility =
            View.GONE


        /*
         * Tampilkan kembali header normal.
         */
        headerNormalContent.visibility =
            View.VISIBLE
    }


    // =========================================================
    // EKSEKUSI SEARCH
    // =========================================================

    private fun executeSearch() {

        val keyword =
            searchInput.text
                .toString()
                .trim()


        /*
         * Jangan lanjut jika keyword kosong.
         */
        if (keyword.isEmpty()) {

            searchInput.error =
                "Masukkan kata pencarian"

            searchInput.requestFocus()

            return
        }


        /*
         * Sembunyikan keyboard.
         */
        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager


        inputMethodManager.hideSoftInputFromWindow(
            searchInput.windowToken,
            0
        )


        /*
         * Buka halaman SearchActivity.
         */
        val intent =
            Intent(
                this,
                SearchActivity::class.java
            )


        /*
         * Kirim keyword ke SearchActivity.
         */
        intent.putExtra(
            "keyword",
            keyword
        )


        startActivity(intent)
    }


    // =========================================================
    // SETUP BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        // INFORMASI
        findViewById<View>(
            R.id.navInfografik
        ).setOnClickListener {

            showInformasiPanel()
        }


        // DATA
        findViewById<View>(
            R.id.navData
        ).setOnClickListener {

            goToPage(4)
        }


        // BERANDA
        findViewById<View>(
            R.id.navHome
        ).setOnClickListener {

            goToPage(0)
        }


        // LAINNYA
        findViewById<View>(
            R.id.navMore
        ).setOnClickListener {

            goToPage(5)
        }


        // AKUN
        findViewById<View>(
            R.id.navAkun
        ).setOnClickListener {

            // Dikosongkan sementara.
        }
    }


    // =========================================================
    // SETUP MENU INFORMASI
    // =========================================================

    private fun setupInformasiMenu() {

        // BERITA
        findViewById<View>(
            R.id.menuBerita
        ).setOnClickListener {

            hideInformasiMenu()

            goToPage(2)
        }


        // INFOGRAFIK
        findViewById<View>(
            R.id.menuInfografik
        ).setOnClickListener {

            hideInformasiMenu()

            goToPage(1)
        }


        // PUBLIKASI
        findViewById<View>(
            R.id.menuPublikasi
        ).setOnClickListener {

            hideInformasiMenu()

            goToPage(3)
        }


        informasiMenu.visibility =
            View.GONE


        informasiOverlay.setOnClickListener {

            hideInformasiMenu()
        }
    }


    // =========================================================
    // TAMPILKAN MENU INFORMASI
    // =========================================================

    private fun showInformasiPanel() {

        if (informasiMenuVisible) {

            hideInformasiMenu()

            return
        }


        positionInformasiMenu()


        informasiOverlay.visibility =
            View.VISIBLE


        informasiOverlay.alpha =
            0f


        informasiOverlay.animate()
            .alpha(1f)
            .setDuration(180)
            .start()


        informasiMenu.visibility =
            View.VISIBLE


        informasiMenu.alpha =
            0f


        informasiMenu.translationY =
            informasiMenu.height * 0.35f


        informasiMenuVisible =
            true


        informasiMenu.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(
                DecelerateInterpolator(
                    1.8f
                )
            )
            .start()
    }


    // =========================================================
    // SEMBUNYIKAN MENU INFORMASI
    // =========================================================

    private fun hideInformasiMenu() {

        if (!informasiMenuVisible) {
            return
        }


        informasiMenuVisible =
            false


        informasiOverlay.animate()
            .alpha(0f)
            .setDuration(180)
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
                informasiMenu.height * 0.35f
            )
            .setDuration(180)
            .setInterpolator(
                DecelerateInterpolator(
                    1.8f
                )
            )
            .withEndAction {

                informasiMenu.visibility =
                    View.GONE

                informasiMenu.alpha =
                    1f

                informasiMenu.translationY =
                    0f

                /*
                 * Reset posisi horizontal.
                 *
                 * Ini mencegah menu Informasi bergeser
                 * setiap kali dibuka kembali.
                 */
                informasiMenu.translationX =
                    0f
            }
            .start()
    }


    // =========================================================
    // POSISI MENU INFORMASI
    // =========================================================

    private fun positionInformasiMenu() {

        val navInformasi =
            findViewById<View>(
                R.id.navInfografik
            )


        val root =
            informasiMenu.parent as View


        /*
         * Reset posisi translasi sebelumnya.
         */
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
    // PINDAH HALAMAN
    // =========================================================

    fun goToPage(
        position: Int
    ) {

        hideInformasiMenu()


        if (
            ::viewPager.isInitialized
        ) {

            viewPager.setCurrentItem(
                position,
                true
            )
        }
    }


    // =========================================================
    // UPDATE BOTTOM NAVIGATION
    // =========================================================

    private fun updateBottomNavigation(
        position: Int
    ) {

        val activeColor =
            Color.parseColor(
                "#F97316"
            )


        val inactiveColor =
            Color.parseColor(
                "#9CA3AF"
            )


        /*
         * Informasi aktif ketika berada pada:
         *
         * 1 = Infografik
         * 2 = Berita
         * 3 = Publikasi
         */
        val informasiAktif =
            position == 1 ||
                    position == 2 ||
                    position == 3


        val dataAktif =
            position == 4


        val homeAktif =
            position == 0


        val moreAktif =
            position == 5


        // =====================================================
        // INFORMASI
        // =====================================================

        setNavigationState(

            findViewById(
                R.id.navInfografikIcon
            ),

            findViewById(
                R.id.navInfografikText
            ),

            informasiAktif,

            activeColor,

            inactiveColor
        )


        // =====================================================
        // DATA
        // =====================================================

        setNavigationState(

            findViewById(
                R.id.navDataIcon
            ),

            findViewById(
                R.id.navDataText
            ),

            dataAktif,

            activeColor,

            inactiveColor
        )


        // =====================================================
        // BERANDA
        // =====================================================

        setNavigationState(

            findViewById(
                R.id.navHomeIcon
            ),

            findViewById(
                R.id.navHomeText
            ),

            homeAktif,

            activeColor,

            inactiveColor
        )


        // =====================================================
        // LAINNYA
        // =====================================================

        setNavigationState(

            findViewById(
                R.id.navMoreIcon
            ),

            findViewById(
                R.id.navMoreText
            ),

            moreAktif,

            activeColor,

            inactiveColor
        )


        // =====================================================
        // AKUN
        // =====================================================

        setNavigationState(

            findViewById(
                R.id.navAkunIcon
            ),

            findViewById(
                R.id.navAkunText
            ),

            false,

            activeColor,

            inactiveColor
        )
    }


    // =========================================================
    // SET NAVIGATION STATE
    // =========================================================

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


        icon.setColorFilter(
            color
        )


        text.setTextColor(
            color
        )


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
    // ANIMASI AWAL HOME
    // =========================================================

    private fun startHomeAnimation() {

        // =====================================================
        // HEADER
        // =====================================================

        homeHeader.alpha =
            0f


        homeHeader.translationY =
            -180f


        homeHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(850)
            .setInterpolator(
                DecelerateInterpolator(
                    1.8f
                )
            )
            .start()


        // =====================================================
        // CONTENT / VIEWPAGER
        // =====================================================

        viewPager.alpha =
            0f


        viewPager.translationY =
            60f


        viewPager.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(150)
            .setInterpolator(
                DecelerateInterpolator(
                    1.8f
                )
            )
            .start()


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomHomeActivity.alpha =
            0f


        bottomHomeActivity.translationY =
            220f


        bottomHomeActivity.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(900)
            .setStartDelay(250)
            .setInterpolator(
                DecelerateInterpolator(
                    1.8f
                )
            )
            .start()
    }
}