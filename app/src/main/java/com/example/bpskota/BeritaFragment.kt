package com.example.bpskota

import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.bpskota.bps.model.NewsItem
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import com.google.gson.JsonArray


class BeritaFragment : Fragment(), RefreshableFragment {

    // =========================================================
    // REPOSITORY
    // =========================================================

    private lateinit var repository: BpsRepository

    private val API_KEY =
        "008edaaae5d450b1913b31a2cef618c3"


    // =========================================================
    // DATA BERITA
    // =========================================================

    private val semuaBerita =
        mutableListOf<NewsItem>()

    private var tahunTerpilih: String? = null

    private var keywordPencarian =
        ""

    private var totalPage =
        1

    private var jumlahPageSelesai =
        0

    private var sedangLoad =
        false

    // Menandakan bahwa load sedang berasal
    // dari pull-to-refresh.
    private var sedangRefresh =
        false


    // =========================================================
    // VIEW
    // =========================================================

    private lateinit var beritaContainer: LinearLayout

    private lateinit var tvJumlahBerita: TextView

    private lateinit var etSearchBerita: EditText

    private lateinit var btnFilterBerita: View

    private lateinit var loadingView: LottieAnimationView


    // =========================================================
    // SCROLL VIEW
    // =========================================================

    /*
     * ScrollView utama BeritaFragment.
     *
     * Tidak menggunakan ID baru.
     * ScrollView dicari langsung dari hierarchy Fragment.
     */
    private var beritaScrollView: ScrollView? =
        null


    // =========================================================
    // CREATE VIEW
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_berita,
            container,
            false
        )
    }


    // =========================================================
    // VIEW CREATED
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )


        // =====================================================
        // INIT REPOSITORY
        // =====================================================

        repository =
            BpsRepository()


        // =====================================================
        // INIT VIEW
        // =====================================================

        beritaContainer =
            view.findViewById(
                R.id.beritaContainer
            )


        tvJumlahBerita =
            view.findViewById(
                R.id.tvJumlahBerita
            )


        etSearchBerita =
            view.findViewById(
                R.id.etSearchBerita
            )


        btnFilterBerita =
            view.findViewById(
                R.id.btnFilterBerita
            )


        // =====================================================
        // SETUP
        // =====================================================

        setupLoading()

        setupSearch()

        setupFilter()

        setupSwipeRefreshState(
            view
        )


        // =====================================================
        // LOAD DATA AWAL
        // =====================================================

        loadSemuaBerita()
    }


    // =========================================================
    // SETUP SWIPE REFRESH
    // =========================================================

    private fun setupSwipeRefreshState(
        view: View
    ) {

        /*
         * Cari ScrollView utama dari hierarchy.
         *
         * Tidak menambahkan ID baru ke XML.
         */
        beritaScrollView =
            findScrollView(view)


        val scrollView =
            beritaScrollView
                ?: return


        val swipeRefreshHome =
            requireActivity()
                .findViewById<SwipeRefreshLayout>(
                    R.id.swipeRefreshHome
                )


        // =====================================================
        // UPDATE STATUS REFRESH
        // =====================================================

        fun updateState() {

            if (!isAdded) {
                return
            }


            val homeActivity =
                activity as? HomeActivity
                    ?: return


            /*
             * BeritaFragment hanya boleh mengatur
             * SwipeRefreshLayout ketika halaman Berita
             * sedang aktif.
             *
             * Posisi Berita = 2
             */
            if (
                homeActivity.currentPage != 2
            ) {
                return
            }


            /*
             * canScrollVertically(-1):
             *
             * true  = masih bisa scroll ke atas
             * false = sudah mentok paling atas
             */
            val isAtTop =
                !scrollView.canScrollVertically(
                    -1
                )


            /*
             * Pull-to-refresh hanya aktif
             * ketika sudah mentok paling atas.
             */
            swipeRefreshHome.isEnabled =
                isAtTop
        }


        // =====================================================
        // LISTENER SCROLL
        // =====================================================

        scrollView.setOnScrollChangeListener {

                _,
                _,
                _,
                _,
                _ ->

            updateState()
        }


        // =====================================================
        // CEK POSISI AWAL
        // =====================================================

        scrollView.post {

            updateState()
        }
    }


    // =========================================================
    // UPDATE STATUS REFRESH
    // =========================================================

    override fun updateRefreshState() {

        if (!isAdded) {
            return
        }


        val currentView =
            view
                ?: return


        val scrollView =
            beritaScrollView
                ?: findScrollView(
                    currentView
                )
                ?: return


        val swipeRefreshHome =
            requireActivity()
                .findViewById<SwipeRefreshLayout>(
                    R.id.swipeRefreshHome
                )


        val homeActivity =
            activity as? HomeActivity
                ?: return


        /*
         * BeritaFragment hanya boleh mengatur
         * SwipeRefreshLayout ketika halaman Berita aktif.
         */
        if (
            homeActivity.currentPage != 2
        ) {
            return
        }


        /*
         * Refresh hanya aktif ketika ScrollView
         * sudah mentok paling atas.
         */
        val isAtTop =
            !scrollView.canScrollVertically(
                -1
            )


        swipeRefreshHome.isEnabled =
            isAtTop
    }


    // =========================================================
    // MENCARI SCROLLVIEW
    // =========================================================

    private fun findScrollView(
        view: View
    ): ScrollView? {

        /*
         * Jika root langsung merupakan ScrollView,
         * gunakan root tersebut.
         */
        if (
            view is ScrollView
        ) {
            return view
        }


        /*
         * Jika ViewGroup, cari secara recursive.
         */
        if (
            view is ViewGroup
        ) {

            for (
            i in 0 until view.childCount
            ) {

                val child =
                    view.getChildAt(i)


                val result =
                    findScrollView(
                        child
                    )


                if (
                    result != null
                ) {

                    return result
                }
            }
        }


        return null
    }


    // =========================================================
    // REFRESH DATA
    // =========================================================

    override fun refreshData() {

        if (!isAdded) {
            return
        }


        val currentView =
            view
                ?: return


        val scrollView =
            beritaScrollView
                ?: findScrollView(
                    currentView
                )


        /*
         * Pengaman tambahan.
         *
         * Walaupun SwipeRefreshLayout seharusnya
         * sudah disabled ketika tidak di atas,
         * kita tetap cek di sini.
         */
        if (
            scrollView != null &&
            scrollView.canScrollVertically(-1)
        ) {

            return
        }


        Log.d(
            "BERITA_FRAGMENT",
            "========================================"
        )

        Log.d(
            "BERITA_FRAGMENT",
            "PULL TO REFRESH BERITA"
        )

        Log.d(
            "BERITA_FRAGMENT",
            "Posisi sudah paling atas"
        )

        Log.d(
            "BERITA_FRAGMENT",
            "Memulai reload semua berita"
        )

        Log.d(
            "BERITA_FRAGMENT",
            "========================================"
        )


        sedangRefresh =
            true


        /*
         * Load ulang seluruh berita.
         */
        loadSemuaBerita()
    }


    // =========================================================
    // SETUP LOADING
    // =========================================================

    private fun setupLoading() {

        loadingView =
            LottieAnimationView(
                requireContext()
            )


        loadingView.layoutParams =
            LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(100)
            ).apply {

                gravity =
                    Gravity.CENTER

                topMargin =
                    dpToPx(40)

                bottomMargin =
                    dpToPx(40)
            }


        loadingView.setAnimation(
            "Loading_Animation.json"
        )


        loadingView.repeatCount =
            -1


        loadingView.visibility =
            View.GONE


        beritaContainer.addView(
            loadingView
        )
    }


    // =========================================================
    // TAMPILKAN LOADING
    // =========================================================

    private fun tampilkanLoading() {

        loadingView.visibility =
            View.VISIBLE


        loadingView.playAnimation()


        tvJumlahBerita.text =
            "Memuat berita..."


        beritaContainer.childrenExceptLoading()
    }


    // =========================================================
    // SEMBUNYIKAN LOADING
    // =========================================================

    private fun sembunyikanLoading() {

        loadingView.cancelAnimation()


        loadingView.visibility =
            View.GONE
    }


    // =========================================================
    // LOAD SEMUA BERITA
    // =========================================================

    private fun loadSemuaBerita() {

        semuaBerita.clear()


        tahunTerpilih =
            null


        keywordPencarian =
            ""


        totalPage =
            1


        jumlahPageSelesai =
            0


        sedangLoad =
            true


        /*
         * Hanya reset search ketika bukan
         * proses refresh?
         *
         * Untuk mempertahankan perilaku kode lama,
         * search tetap dikosongkan ketika reload.
         */
        etSearchBerita.setText("")


        tampilkanLoading()


        Log.d(
            "BERITA_FRAGMENT",
            "========================================"
        )


        Log.d(
            "BERITA_FRAGMENT",
            "BeritaFragment dibuka / direfresh"
        )


        Log.d(
            "BERITA_FRAGMENT",
            "Mulai load berita"
        )


        loadPagePertama()
    }


    // =========================================================
    // LOAD PAGE PERTAMA
    // =========================================================

    private fun loadPagePertama() {

        if (!isAdded) {
            return
        }


        Log.d(
            "BERITA_FRAGMENT",
            "Mengambil page 1"
        )


        repository.getNews(
            domain = "3574",
            page = 1,
            perPage = 10,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) {
                return@getNews
            }


            requireActivity().runOnUiThread {

                // =================================================
                // ERROR
                // =================================================

                if (error != null) {

                    sedangLoad =
                        false


                    Log.e(
                        "BERITA_FRAGMENT",
                        "Gagal mengambil page 1",
                        error
                    )


                    sembunyikanLoading()


                    tvJumlahBerita.text =
                        "Gagal memuat berita"


                    selesaiSwipeRefresh()


                    return@runOnUiThread
                }


                // =================================================
                // RESPONSE NULL
                // =================================================

                if (response == null) {

                    sedangLoad =
                        false


                    Log.e(
                        "BERITA_FRAGMENT",
                        "Response page 1 null"
                    )


                    sembunyikanLoading()


                    tvJumlahBerita.text =
                        "Gagal memuat berita"


                    selesaiSwipeRefresh()


                    return@runOnUiThread
                }


                // =================================================
                // PARSING
                // =================================================

                try {

                    val dataArray =
                        response.getAsJsonArray(
                            "data"
                        )


                    if (
                        dataArray == null ||
                        dataArray.size() < 2
                    ) {

                        sedangLoad =
                            false


                        Log.e(
                            "BERITA_FRAGMENT",
                            "Data page 1 tidak tersedia"
                        )


                        sembunyikanLoading()


                        tvJumlahBerita.text =
                            "Tidak ada berita"


                        selesaiSwipeRefresh()


                        return@runOnUiThread
                    }


                    val newsArray =
                        dataArray[1]
                            .asJsonArray


                    prosesBeritaPage(
                        newsArray
                    )


                    jumlahPageSelesai++


                    totalPage =
                        ambilTotalPage(
                            dataArray
                        )


                    Log.d(
                        "BERITA_FRAGMENT",
                        "Total page dari API: $totalPage"
                    )


                    Log.d(
                        "BERITA_FRAGMENT",
                        "Page 1 selesai"
                    )


                    // =================================================
                    // HANYA 1 PAGE
                    // =================================================

                    if (
                        totalPage <= 1
                    ) {

                        sedangLoad =
                            false


                        Log.d(
                            "BERITA_FRAGMENT",
                            "Hanya terdapat 1 page"
                        )


                        selesaiMemuatSemuaBerita()


                        return@runOnUiThread
                    }


                    // =================================================
                    // LOAD PAGE LAIN
                    // =================================================

                    /*
                     * Page 2 sampai page terakhir
                     * ditembak secara paralel.
                     */
                    for (
                    page in 2..totalPage
                    ) {

                        loadPageParalel(
                            page
                        )
                    }

                } catch (e: Exception) {

                    sedangLoad =
                        false


                    Log.e(
                        "BERITA_FRAGMENT",
                        "Error memproses page 1",
                        e
                    )


                    sembunyikanLoading()


                    tvJumlahBerita.text =
                        "Gagal memproses berita"


                    selesaiSwipeRefresh()
                }
            }
        }
    }


    // =========================================================
    // LOAD PAGE PARALEL
    // =========================================================

    private fun loadPageParalel(
        page: Int
    ) {

        if (!isAdded) {
            return
        }


        Log.d(
            "BERITA_FRAGMENT",
            "Request page $page secara paralel"
        )


        repository.getNews(
            domain = "3574",
            page = page,
            perPage = 10,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) {
                return@getNews
            }


            requireActivity().runOnUiThread {

                // =================================================
                // ERROR
                // =================================================

                if (error != null) {

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Gagal mengambil page $page",
                        error
                    )


                    jumlahPageSelesai++


                    cekSelesaiSemuaPage()


                    return@runOnUiThread
                }


                // =================================================
                // RESPONSE NULL
                // =================================================

                if (response == null) {

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Response page $page null"
                    )


                    jumlahPageSelesai++


                    cekSelesaiSemuaPage()


                    return@runOnUiThread
                }


                // =================================================
                // PARSING
                // =================================================

                try {

                    val dataArray =
                        response.getAsJsonArray(
                            "data"
                        )


                    if (
                        dataArray == null ||
                        dataArray.size() < 2
                    ) {

                        Log.d(
                            "BERITA_FRAGMENT",
                            "Data page $page kosong"
                        )


                        jumlahPageSelesai++


                        cekSelesaiSemuaPage()


                        return@runOnUiThread
                    }


                    val newsArray =
                        dataArray[1]
                            .asJsonArray


                    Log.d(
                        "BERITA_FRAGMENT",
                        "Page $page selesai - ${newsArray.size()} berita"
                    )


                    prosesBeritaPage(
                        newsArray
                    )


                    jumlahPageSelesai++


                    Log.d(
                        "BERITA_FRAGMENT",
                        "Progress page: $jumlahPageSelesai / $totalPage"
                    )


                    cekSelesaiSemuaPage()

                } catch (e: Exception) {

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Error memproses page $page",
                        e
                    )


                    jumlahPageSelesai++


                    cekSelesaiSemuaPage()
                }
            }
        }
    }


    // =========================================================
    // PROSES BERITA PAGE
    // =========================================================

    private fun prosesBeritaPage(
        newsArray: JsonArray
    ) {

        newsArray.forEach { item ->

            try {

                val news =
                    Gson().fromJson(
                        item,
                        NewsItem::class.java
                    )


                semuaBerita.add(
                    news
                )

            } catch (e: Exception) {

                Log.e(
                    "BERITA_FRAGMENT",
                    "Gagal parsing berita",
                    e
                )
            }
        }


        // =====================================================
        // HILANGKAN DATA DUPLIKAT
        // =====================================================

        val dataUnik =
            semuaBerita
                .distinctBy {
                    it.news_id
                }


        semuaBerita.clear()


        semuaBerita.addAll(
            dataUnik
        )


        Log.d(
            "BERITA_FRAGMENT",
            "Total berita sementara: ${semuaBerita.size}"
        )
    }


    // =========================================================
    // AMBIL TOTAL PAGE
    // =========================================================

    private fun ambilTotalPage(
        dataArray: JsonArray
    ): Int {

        try {

            val metadata =
                dataArray[0]


            if (
                metadata.isJsonObject
            ) {

                val obj =
                    metadata.asJsonObject


                val kemungkinanField =
                    listOf(
                        "total_page",
                        "total_pages",
                        "pages",
                        "page_total"
                    )


                for (
                field in kemungkinanField
                ) {

                    if (
                        obj.has(field) &&
                        !obj.get(field).isJsonNull
                    ) {

                        val total =
                            obj.get(field).asInt


                        if (
                            total > 0
                        ) {

                            return total
                        }
                    }
                }
            }

        } catch (e: Exception) {

            Log.e(
                "BERITA_FRAGMENT",
                "Gagal mengambil total page",
                e
            )
        }


        return 1
    }


    // =========================================================
    // CEK SEMUA PAGE
    // =========================================================

    private fun cekSelesaiSemuaPage() {

        if (
            jumlahPageSelesai >= totalPage
        ) {

            sedangLoad =
                false


            Log.d(
                "BERITA_FRAGMENT",
                "========================================"
            )


            Log.d(
                "BERITA_FRAGMENT",
                "SEMUA PAGE SELESAI"
            )


            Log.d(
                "BERITA_FRAGMENT",
                "Total page: $totalPage"
            )


            Log.d(
                "BERITA_FRAGMENT",
                "Total berita: ${semuaBerita.size}"
            )


            Log.d(
                "BERITA_FRAGMENT",
                "========================================"
            )


            selesaiMemuatSemuaBerita()
        }
    }


    // =========================================================
    // SELESAI LOAD SEMUA BERITA
    // =========================================================

    private fun selesaiMemuatSemuaBerita() {

        if (!isAdded) {
            return
        }


        sedangLoad =
            false


        sembunyikanLoading()


        tampilkanBerita()


        /*
         * Hentikan spinner SwipeRefreshLayout
         * setelah seluruh proses selesai.
         */
        selesaiSwipeRefresh()


        Log.d(
            "BERITA_FRAGMENT",
            "Berita siap ditampilkan"
        )
    }


    // =========================================================
    // SELESAI SWIPE REFRESH
    // =========================================================

    private fun selesaiSwipeRefresh() {

        if (!isAdded) {
            return
        }


        val homeActivity =
            activity as? HomeActivity
                ?: return


        homeActivity.finishSwipeRefresh()


        /*
         * Setelah refresh selesai,
         * hitung kembali apakah SwipeRefreshLayout
         * boleh aktif.
         */
        updateRefreshState()


        sedangRefresh =
            false
    }


    // =========================================================
    // SETUP SEARCH
    // =========================================================

    private fun setupSearch() {

        etSearchBerita.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    keywordPencarian =
                        s?.toString()
                            ?.trim()
                            ?.lowercase()
                            ?: ""


                    if (
                        !sedangLoad
                    ) {

                        tampilkanBerita()
                    }
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }


    // =========================================================
    // SETUP FILTER
    // =========================================================

    private fun setupFilter() {

        btnFilterBerita.setOnClickListener {

            if (
                sedangLoad
            ) {

                return@setOnClickListener
            }


            tampilkanDialogFilterTahun()
        }
    }


    // =========================================================
    // DIALOG FILTER TAHUN
    // =========================================================

    private fun tampilkanDialogFilterTahun() {

        val tahunList =
            semuaBerita
                .mapNotNull {

                    ambilTahun(
                        it.rl_date
                    )
                }
                .distinct()
                .sortedDescending()
                .toMutableList()


        tahunList.add(
            0,
            "Semua Tahun"
        )


        val pilihan =
            if (
                tahunTerpilih == null
            ) {

                0

            } else {

                tahunList
                    .indexOf(
                        tahunTerpilih
                    )
                    .takeIf {
                        it >= 0
                    }
                    ?: 0
            }


        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "Filter Tahun"
            )
            .setSingleChoiceItems(
                tahunList.toTypedArray(),
                pilihan
            ) { dialog, which ->

                val tahun =
                    tahunList[which]


                tahunTerpilih =
                    if (
                        tahun ==
                        "Semua Tahun"
                    ) {

                        null

                    } else {

                        tahun
                    }


                tampilkanBerita()


                dialog.dismiss()
            }
            .show()
    }


    // =========================================================
    // AMBIL TAHUN
    // =========================================================

    private fun ambilTahun(
        tanggal: String?
    ): String? {

        if (
            tanggal.isNullOrBlank()
        ) {

            return null
        }


        val regex =
            Regex(
                "\\b(19|20)\\d{2}\\b"
            )


        return regex
            .find(tanggal)
            ?.value
    }


    // =========================================================
    // FILTER BERITA
    // =========================================================

    private fun ambilBeritaTerfilter():
            List<NewsItem> {

        return semuaBerita
            .filter { item ->

                val cocokTahun =
                    tahunTerpilih == null ||
                            ambilTahun(
                                item.rl_date
                            ) ==
                            tahunTerpilih


                val judul =
                    item.title
                        ?.lowercase()
                        ?: ""


                val isi =
                    Html.fromHtml(
                        item.news ?: "",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                        .toString()
                        .lowercase()


                val cocokPencarian =
                    keywordPencarian.isBlank() ||
                            judul.contains(
                                keywordPencarian
                            ) ||
                            isi.contains(
                                keywordPencarian
                            )


                cocokTahun &&
                        cocokPencarian
            }
            .sortedByDescending {

                it.rl_date ?: ""
            }
    }


    // =========================================================
    // TAMPILKAN BERITA
    // =========================================================

    private fun tampilkanBerita() {

        if (!isAdded) {
            return
        }


        if (
            sedangLoad
        ) {

            return
        }


        requireActivity().runOnUiThread {

            if (!isAdded) {
                return@runOnUiThread
            }


            beritaContainer.removeAllViews()


            beritaContainer.addView(
                loadingView
            )


            loadingView.visibility =
                View.GONE


            val berita =
                ambilBeritaTerfilter()


            tvJumlahBerita.text =
                "${berita.size} berita"


            var row: LinearLayout? =
                null


            berita.forEachIndexed {
                    index,
                    item ->

                // =================================================
                // BARIS BARU
                // =================================================

                if (
                    index % 2 == 0
                ) {

                    row =
                        LinearLayout(
                            requireContext()
                        )


                    row?.orientation =
                        LinearLayout.HORIZONTAL


                    row?.layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {

                            if (
                                index > 0
                            ) {

                                topMargin =
                                    dpToPx(14)
                            }
                        }


                    beritaContainer.addView(
                        row
                    )
                }


                // =================================================
                // INFLATE CARD
                // =================================================

                val card =
                    LayoutInflater.from(
                        requireContext()
                    ).inflate(
                        R.layout.item_home,
                        row,
                        false
                    )


                val cardParams =
                    LinearLayout.LayoutParams(
                        0,
                        dpToPx(210)
                    ).apply {

                        weight =
                            1f


                        if (
                            index % 2 == 0
                        ) {

                            marginEnd =
                                dpToPx(6)

                        } else {

                            marginStart =
                                dpToPx(6)
                        }
                    }


                card.layoutParams =
                    cardParams


                // =================================================
                // VIEW CARD
                // =================================================

                val image =
                    card.findViewById<ImageView>(
                        R.id.imgHome
                    )


                val progressImage =
                    card.findViewById<android.widget.ProgressBar>(
                        R.id.progressImage
                    )


                val title =
                    card.findViewById<TextView>(
                        R.id.tvHomeTitle
                    )


                val description =
                    card.findViewById<TextView>(
                        R.id.tvHomeDescription
                    )


                // =================================================
                // TITLE
                // =================================================

                title.text =
                    item.title
                        ?: "Berita"


                // =================================================
                // DESCRIPTION
                // =================================================

                description.text =
                    Html.fromHtml(
                        item.news ?: "",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                        .toString()
                        .trim()


                // =================================================
                // IMAGE
                // =================================================

                progressImage.visibility =
                    View.VISIBLE


                Glide.with(
                    this@BeritaFragment
                )
                    .load(
                        item.picture
                    )
                    .diskCacheStrategy(
                        com.bumptech.glide.load.engine.DiskCacheStrategy.ALL
                    )
                    .placeholder(
                        R.drawable.ic_bpslogo
                    )
                    .error(
                        R.drawable.ic_bpslogo
                    )
                    .into(
                        image
                    )


                progressImage.visibility =
                    View.GONE


                // =================================================
                // CLICK DETAIL
                // =================================================

                card.setOnClickListener {

                    Log.d(
                        "BERITA_FRAGMENT",
                        "Klik berita: ${item.title}"
                    )


                    tampilkanDetailBerita(
                        item
                    )
                }


                row?.addView(
                    card
                )
            }


            // =====================================================
            // ITEM KOSONG JIKA JUMLAH GANJIL
            // =====================================================

            if (
                berita.size % 2 != 0 &&
                berita.isNotEmpty()
            ) {

                val emptyView =
                    View(
                        requireContext()
                    )


                emptyView.layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        dpToPx(210)
                    ).apply {

                        weight =
                            1f


                        marginStart =
                            dpToPx(6)
                    }


                row?.addView(
                    emptyView
                )
            }


            Log.d(
                "BERITA_FRAGMENT",
                "Menampilkan ${berita.size} berita"
            )
        }
    }


    // =========================================================
    // DP TO PX
    // =========================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }


    // =========================================================
    // HAPUS CHILD KECUALI LOADING
    // =========================================================

    private fun LinearLayout.childrenExceptLoading() {

        for (
        i in childCount - 1 downTo 0
        ) {

            val child =
                getChildAt(i)


            if (
                child !== loadingView
            ) {

                removeViewAt(i)
            }
        }
    }


    // =========================================================
    // DETAIL BERITA
    // =========================================================

    private fun tampilkanDetailBerita(
        item: NewsItem
    ) {

        if (!isAdded) {
            return
        }


        val dialogView =
            LayoutInflater.from(
                requireContext()
            ).inflate(
                R.layout.dialog_detail_home,
                null
            )


        val image =
            dialogView.findViewById<ImageView>(
                R.id.imgDetailHome
            )


        val title =
            dialogView.findViewById<TextView>(
                R.id.tvDetailHomeTitle
            )


        val date =
            dialogView.findViewById<TextView>(
                R.id.tvDetailHomeDate
            )


        val description =
            dialogView.findViewById<TextView>(
                R.id.tvDetailHomeDescription
            )


        // =====================================================
        // DATA AWAL DETAIL
        // =====================================================

        title.text =
            item.title
                ?: "Berita"


        date.text =
            item.rl_date
                ?: "-"


        description.text =
            "Memuat berita..."


        // =====================================================
        // IMAGE DETAIL
        // =====================================================

        Glide.with(
            requireContext()
        )
            .load(
                item.picture
            )
            .diskCacheStrategy(
                com.bumptech.glide.load.engine.DiskCacheStrategy.ALL
            )
            .thumbnail(
                0.25f
            )
            .placeholder(
                R.drawable.ic_bpslogo
            )
            .error(
                R.drawable.ic_bpslogo
            )
            .into(
                image
            )


        // =====================================================
        // DIALOG
        // =====================================================

        val dialog =
            AlertDialog.Builder(
                requireContext()
            )
                .setView(
                    dialogView
                )
                .create()


        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )


        dialog.show()


        dialog.window?.setLayout(
            (
                    resources
                        .displayMetrics
                        .widthPixels *
                            0.92
                    ).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        // =====================================================
        // REQUEST DETAIL
        // =====================================================

        Log.d(
            "DETAIL_BERITA",
            "Mengambil detail news_id = ${item.news_id}"
        )


        repository.getNewsDetail(
            domain = "3574",
            newsId = item.news_id.toString(),
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) {
                return@getNewsDetail
            }


            requireActivity().runOnUiThread {

                if (
                    dialog.isShowing.not()
                ) {

                    return@runOnUiThread
                }


                // =================================================
                // ERROR
                // =================================================

                if (
                    error != null
                ) {

                    Log.e(
                        "DETAIL_BERITA",
                        "Gagal mengambil detail",
                        error
                    )


                    description.text =
                        "Gagal memuat isi berita."


                    return@runOnUiThread
                }


                // =================================================
                // RESPONSE NULL
                // =================================================

                if (
                    response == null
                ) {

                    description.text =
                        "Isi berita tidak tersedia."


                    return@runOnUiThread
                }


                // =================================================
                // PARSING DETAIL
                // =================================================

                try {

                    val data =
                        response.getAsJsonObject(
                            "data"
                        )


                    if (
                        data == null
                    ) {

                        description.text =
                            "Isi berita tidak tersedia."


                        return@runOnUiThread
                    }


                    // =================================================
                    // NEWS
                    // =================================================

                    val newsLengkap =
                        data.get(
                            "news"
                        )
                            ?.takeIf {
                                !it.isJsonNull
                            }
                            ?.asString
                            ?: ""


                    // =================================================
                    // TANGGAL
                    // =================================================

                    val tanggal =
                        data.get(
                            "rl_date"
                        )
                            ?.takeIf {
                                !it.isJsonNull
                            }
                            ?.asString


                    // =================================================
                    // JUDUL
                    // =================================================

                    val judul =
                        data.get(
                            "title"
                        )
                            ?.takeIf {
                                !it.isJsonNull
                            }
                            ?.asString


                    Log.d(
                        "DETAIL_BERITA",
                        "NEWS LENGTH = ${newsLengkap.length}"
                    )


                    Log.d(
                        "DETAIL_BERITA",
                        "NEWS ASLI = $newsLengkap"
                    )


                    // =================================================
                    // UPDATE JUDUL DAN TANGGAL
                    // =================================================

                    title.text =
                        judul
                            ?: item.title
                                    ?: "Berita"


                    date.text =
                        tanggal
                            ?: item.rl_date
                                    ?: "-"


                    // =================================================
                    // FORMAT HTML
                    // =================================================

                    var newsDiformat =
                        newsLengkap
                            .replace(
                                "&lt;",
                                "<"
                            )
                            .replace(
                                "&gt;",
                                ">"
                            )


                    // =================================================
                    // SERAGAMKAN TAG BARIS BARU
                    // =================================================

                    newsDiformat =
                        newsDiformat
                            .replace(
                                "<br></br>",
                                "<br>"
                            )
                            .replace(
                                "<br/>",
                                "<br>"
                            )
                            .replace(
                                "<br />",
                                "<br>"
                            )


                    // =================================================
                    // JADIKAN PARAGRAF
                    // =================================================

                    newsDiformat =
                        newsDiformat.replace(
                            "<br>",
                            "<br><br>"
                        )


                    // =================================================
                    // RAPikan BREAK BERLEBIHAN
                    // =================================================

                    newsDiformat =
                        newsDiformat
                            .replace(
                                "<br><br><br><br>",
                                "<br><br>"
                            )
                            .replace(
                                "<br><br><br>",
                                "<br><br>"
                            )


                    // =================================================
                    // TAMPILKAN HTML
                    // =================================================

                    description.text =
                        Html.fromHtml(
                            newsDiformat,
                            Html.FROM_HTML_MODE_LEGACY
                        )

                } catch (e: Exception) {

                    Log.e(
                        "DETAIL_BERITA",
                        "Error parsing detail berita",
                        e
                    )


                    description.text =
                        "Gagal memproses isi berita."
                }
            }
        }
    }


    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {

        Log.d(
            "BERITA_FRAGMENT",
            "BeritaFragment ditutup"
        )


        if (
            ::loadingView.isInitialized
        ) {

            loadingView.cancelAnimation()
        }


        semuaBerita.clear()


        sedangLoad =
            false


        sedangRefresh =
            false


        totalPage =
            1


        jumlahPageSelesai =
            0


        beritaScrollView =
            null


        super.onDestroyView()
    }
}