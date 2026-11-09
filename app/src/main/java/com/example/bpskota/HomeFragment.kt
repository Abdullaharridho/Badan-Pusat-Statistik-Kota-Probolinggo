package com.example.bpskota

import android.animation.ObjectAnimator
import android.net.Uri
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.bpskota.bps.model.*
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeFragment : Fragment(), RefreshableFragment {

    // ============================================================
    // ANIMATION STATE
    // ============================================================

    private var statistikAnimated = false
    private var infografikAnimated = false
    private var beritaAnimated = false
    private var publikasiAnimated = false
    private var statistikScrollAnimator: ObjectAnimator? = null

    private lateinit var repository: BpsRepository

    // ============================================================
    // STATISTIK CAROUSEL
    // ============================================================

    private lateinit var statistikScrollView: HorizontalScrollView
    private lateinit var statistikIndicator: LinearLayout

    private val statistikHandler =
        Handler(Looper.getMainLooper())

    private var statistikCurrentPosition = 0
    private var statistikCardCount = 0

    private val statistikCardWidthDp = 158

    private val statistikAutoScrollRunnable =
        object : Runnable {

            override fun run() {

                if (
                    !isAdded ||
                    statistikCardCount <= 1
                ) {
                    return
                }

                val nextPosition =
                    if (
                        statistikCurrentPosition >=
                        statistikCardCount - 1
                    ) {
                        0
                    } else {
                        statistikCurrentPosition + 1
                    }

                scrollToStatistik(
                    nextPosition,
                    true
                )

                statistikHandler.postDelayed(
                    this,
                    5000L
                )
            }
        }

    // ============================================================
    // API CONFIGURATION
    // ============================================================

    private val API_KEY =
        "008edaaae5d450b1913b31a2cef618c3"

    private val PDRB_TABLE_ID = "MTQ2IzI="
    private val PDRB_VARIABLE_ID = "146"
    private val PDRB_VERVAR_ID = 18
    private val PDRB_SUBJECT_ID = 531

    private val IKG_VARIABLE_ID = 134

    private val daftarHargaVariable = listOf(
        120,
        30,
        2,
        116
    )

    data class StatistikTerkiniItem(
        val variable: Int
    )

    private val daftarStatistikTerkini = listOf(
        StatistikTerkiniItem(variable = 88),
        StatistikTerkiniItem(variable = 92)
    )

    private val TAHUN_MULAI = 2026
    private val TAHUN_MINIMUM = 2020

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        repository = BpsRepository()

        setupNavigationListeners(view)
        activateMarquee(view)
        setupSeeAllAction(view)
        setupScrollAnimation(view)
        setupHorizontalScroll(view)
        setupStatistikCarousel(view)
        setupSwipeRefreshState(view)

        loadStatistikTerkini(view)
        loadInfographics(view)
        loadBerita(view)
        loadPublikasi(view)
    }

    // =========================================================
    // SETUP SWIPE REFRESH
    // =========================================================

    private fun setupSwipeRefreshState(
        view: View
    ) {

        val homeScrollView =
            view.findViewById<ScrollView>(
                R.id.homeScrollView
            )

        val swipeRefreshHome =
            requireActivity().findViewById<SwipeRefreshLayout>(
                R.id.swipeRefreshHome
            )

        fun updateState() {

            if (!isAdded) {
                return
            }

            val homeActivity =
                activity as? HomeActivity

            if (homeActivity == null) {
                return
            }

            if (
                homeActivity.currentPage != 0
            ) {
                return
            }

            val isAtTop =
                !homeScrollView.canScrollVertically(-1)

            swipeRefreshHome.isEnabled =
                isAtTop
        }

        homeScrollView.setOnScrollChangeListener {
                _, _, _, _, _ ->
            updateState()
        }

        homeScrollView.post {
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
            view ?: return

        val homeScrollView =
            currentView.findViewById<ScrollView>(
                R.id.homeScrollView
            )

        val swipeRefreshHome =
            requireActivity().findViewById<SwipeRefreshLayout>(
                R.id.swipeRefreshHome
            )

        val homeActivity =
            activity as? HomeActivity
                ?: return

        if (
            homeActivity.currentPage != 0
        ) {
            return
        }

        val isAtTop =
            !homeScrollView.canScrollVertically(-1)

        swipeRefreshHome.isEnabled =
            isAtTop
    }

    // ============================================================
    // REFRESH
    // ============================================================

    override fun refreshData() {

        if (!isAdded) {
            return
        }

        val currentView =
            view ?: return

        statistikHandler.removeCallbacks(
            statistikAutoScrollRunnable
        )

        statistikCurrentPosition = 0
        statistikCardCount = 0

        loadStatistikTerkini(
            currentView
        )

        loadInfographics(
            currentView
        )

        loadBerita(
            currentView
        )

        loadPublikasi(
            currentView
        )

        (activity as? HomeActivity)
            ?.finishSwipeRefresh()
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    private fun setupNavigationListeners(
        view: View
    ) {
        // Menu PPID membuka website melalui browser Chrome
        view.findViewById<View>(R.id.statistik10)
            ?.setOnClickListener {

                val url = "https://ppid.bps.go.id/?mfd=3574&_gl=1*1qtyvkd*_ga*MjEzNDg2MzM4LjE3OTM2MDczNTM.*_ga_XXTTVXWHDB*czE3OTM3NjA5ODUkbzIkZzEkdDE3OTM3NjM1NTUkajUkbDAkaDA."

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {
                    setPackage("com.android.chrome")
                }

                try {
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    // Jika Chrome tidak tersedia, gunakan browser default
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                    )
                }
            }

        val menuMap = mapOf(

            R.id.statistik1 to
                    KependudukanActivity::class.java,

            R.id.statistik2 to
                    TenagakerjaActivity::class.java,

            R.id.statistik3 to
                    GenderActivity::class.java,

            R.id.statistik4 to
                    EkonomiActivity::class.java,

            R.id.statistik5 to
                    TempattinggalActivity::class.java,

            R.id.statistik6 to
                    PertanianActivity::class.java,

            R.id.statistik7 to
                    PendapatanActivity::class.java,

            R.id.statistik9 to
                    MasterwilayahActivity::class.java
        )

        menuMap.forEach { (id, activityClass) ->

            view.findViewById<View>(id)
                .setOnClickListener {

                    startActivity(
                        Intent(
                            requireContext(),
                            activityClass
                        )
                    )
                }
        }
    }

    // ============================================================
    // MARQUEE
    // ============================================================

    private fun activateMarquee(
        view: View
    ) {

        val marqueeIds = listOf(
            R.id.tvStat1,
            R.id.tvStat2,
            R.id.tvStat3,
            R.id.tvStat4,
            R.id.tvStat5,
            R.id.tvStat6,
            R.id.tvStat7,
            R.id.tvStat8
        )

        marqueeIds.forEach { id ->

            view.findViewById<TextView>(id)
                .isSelected = true
        }
    }

    // ============================================================
    // SEE ALL
    // ============================================================

    private fun setupSeeAllAction(
        view: View
    ) {

        val homeActivity =
            activity as? HomeActivity

        view.findViewById<TextView>(
            R.id.infografikLihatSemua
        )?.setOnClickListener {

            homeActivity?.goToPage(1)
        }

        view.findViewById<TextView>(
            R.id.beritaLihatSemua
        )?.setOnClickListener {

            homeActivity?.goToPage(0)
        }

        view.findViewById<TextView>(
            R.id.publikasiLihatSemua
        )?.setOnClickListener {

            homeActivity?.goToPage(2)
        }

        view.findViewById<LinearLayout>(
            R.id.other
        )?.setOnClickListener {

            homeActivity?.goToPage(3)
        }
    }

    // ============================================================
    // HORIZONTAL SCROLL
    // ============================================================

    private fun setupHorizontalScroll(
        view: View
    ) {

        val scrollViews = listOf(

            view.findViewById<HorizontalScrollView>(
                R.id.beritaScrollView
            ),

            view.findViewById<HorizontalScrollView>(
                R.id.infografikScrollView
            )
        )

        scrollViews.forEach { scrollView ->

            scrollView.setOnTouchListener { v, event ->

                when (event.actionMasked) {

                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> {

                        v.parent
                            .requestDisallowInterceptTouchEvent(
                                true
                            )
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {

                        v.parent
                            .requestDisallowInterceptTouchEvent(
                                false
                            )
                    }
                }

                false
            }
        }
    }

    // ============================================================
    // STATISTIK CAROUSEL
    // ============================================================

    private fun setupStatistikCarousel(
        view: View
    ) {

        statistikScrollView =
            view.findViewById(
                R.id.statistikTerkiniScrollView
            )

        statistikIndicator =
            view.findViewById(
                R.id.statistikIndicator
            )

        statistikScrollView.setOnScrollChangeListener {
                _,
                scrollX,
                _,
                _,
                _ ->

            val posisi =
                (
                        scrollX.toFloat() /
                                dpToPx(
                                    statistikCardWidthDp
                                )
                        )
                    .roundToInt()

            statistikCurrentPosition =
                posisi.coerceIn(
                    0,
                    (statistikCardCount - 1)
                        .coerceAtLeast(0)
                )

            updateStatistikIndicator()

            animateStatistikCards(
                scrollX
            )
        }

        statistikScrollView.setOnTouchListener {
                _,
                event ->

            when (
                event.actionMasked
            ) {

                MotionEvent.ACTION_DOWN -> {

                    statistikHandler.removeCallbacks(
                        statistikAutoScrollRunnable
                    )
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    statistikHandler.removeCallbacks(
                        statistikAutoScrollRunnable
                    )

                    statistikHandler.postDelayed(
                        statistikAutoScrollRunnable,
                        5000L
                    )
                }
            }

            false
        }
    }

    // ============================================================
    // LOAD STATISTIK TERKINI (DENGAN PLACEHOLDER LOADING)
    // ============================================================

    private fun loadStatistikTerkini(
        view: View
    ) {

        val container =
            view.findViewById<LinearLayout>(
                R.id.statistikTerkiniContainer
            )

        container.removeAllViews()

        statistikCurrentPosition = 0
        statistikCardCount = 0

        setupStatistikIndicator()

        // 1. Placeholder & Load Tenaga Kerja
        daftarStatistikTerkini.forEachIndexed {
                index,
                item ->

            val placeholderCard = tampilkanCardLoading(
                container = container,
                judulPlaceholder = "Memuat Data...",
                kategoriPlaceholder = "Statistik",
                index = index
            )

            cariDataStatistikTerbaru(
                variable = item.variable,
                tahun = TAHUN_MULAI,
                index = index,
                cardToUpdate = placeholderCard
            )
        }

        // 2. Placeholder & Load PDRB
        val pdrbCard = tampilkanCardLoading(
            container = container,
            judulPlaceholder = "Laju Pertumbuhan PDRB",
            kategoriPlaceholder = "Neraca Ekonomi",
            index = 2
        )
        loadPdrbTerkini(container, pdrbCard)

        // 3. Placeholder & Load IKG
        val ikgCard = tampilkanCardLoading(
            container = container,
            judulPlaceholder = "Indeks Ketimpangan Gender (IKG)",
            kategoriPlaceholder = "Gender",
            index = 3
        )
        loadIkgTerkini(container, ikgCard)

        // 4. Placeholder & Load Harga-Harga
        daftarHargaVariable.forEachIndexed {
                index,
                variable ->

            val hargaCard = tampilkanCardLoading(
                container = container,
                judulPlaceholder = "Harga-Harga",
                kategoriPlaceholder = "Harga",
                index = index + 4
            )

            cariHargaTerkini(
                variable = variable,
                tahun = TAHUN_MULAI,
                index = index + 4,
                cardToUpdate = hargaCard
            )
        }
    }

    private fun tampilkanCardLoading(
        container: LinearLayout,
        judulPlaceholder: String,
        kategoriPlaceholder: String,
        index: Int
    ): View {
        val card =
            LayoutInflater.from(
                requireContext()
            ).inflate(
                R.layout.card_home,
                container,
                false
            )

        card.findViewById<TextView>(
            R.id.tvStatistikJudul
        ).text = judulPlaceholder

        card.findViewById<TextView>(
            R.id.tvStatistikKategori
        ).text = kategoriPlaceholder

        card.findViewById<TextView>(
            R.id.tvStatistikPersentase
        ).text = "Memuat..."

        container.addView(card)

        statistikCardCount =
            container.childCount

        updateStatistikCarouselLayout()

        animateCardIn(
            card,
            index
        )

        return card
    }

    private fun updateCardStatistikData(
        card: View,
        judul: String,
        kategori: String,
        nilai: Double,
        tahun: String,
        unit: String,
        bukaEkonomi: Boolean = false,
        bukaGender: Boolean = false,
        bisaDiklik: Boolean = true,
        bukaHarga: Boolean = false
    ) {
        card.findViewById<TextView>(
            R.id.tvStatistikJudul
        ).text = judul

        card.findViewById<TextView>(
            R.id.tvStatistikKategori
        ).text = "$kategori\n$tahun"

        // Bersihkan unit jika mengandung kalimat "tidak ada" atau kosong
        val cleanUnit = if (unit.contains("tidak ada", true) || unit.isBlank()) {
            ""
        } else {
            unit
        }

        card.findViewById<TextView>(
            R.id.tvStatistikPersentase
        ).text =
            when {
                cleanUnit.contains("%", true) ||
                        cleanUnit.equals("Persen", true) -> {
                    String.format(
                        Locale.US,
                        "%.2f%%",
                        nilai
                    )
                }

                judul.contains(
                    "Ketimpangan Gender",
                    ignoreCase = true
                ) -> {
                    String.format(
                        Locale.US,
                        "%.3f",
                        nilai
                    )
                }

                cleanUnit.isNotBlank() -> {
                    String.format(
                        Locale.US,
                        "%.2f %s",
                        nilai,
                        cleanUnit
                    )
                }

                else -> {
                    String.format(
                        Locale.US,
                        "%.2f",
                        nilai
                    )
                }
            }

        if (bisaDiklik) {
            card.setOnClickListener {
                if (bukaHarga) {
                    val intent = Intent(
                        requireContext(),
                        DataKategoriActivity::class.java
                    ).apply {
                        putExtra("NAMA_KATEGORI", kategori)
                    }
                    startActivity(intent)
                    return@setOnClickListener
                }

                val tujuan = when {
                    bukaEkonomi -> EkonomiActivity::class.java
                    bukaGender -> GenderActivity::class.java
                    else -> TenagakerjaActivity::class.java
                }

                startActivity(
                    Intent(
                        requireContext(),
                        tujuan
                    )
                )
            }
        }

    }

    private fun cariDataStatistikTerbaru(
        variable: Int,
        tahun: Int,
        index: Int,
        cardToUpdate: View
    ) {

        if (tahun < TAHUN_MINIMUM) {
            cardToUpdate.findViewById<TextView>(R.id.tvStatistikJudul).text = "Data Tidak Tersedia"
            cardToUpdate.findViewById<TextView>(R.id.tvStatistikPersentase).text = "-"
            return
        }

        val th = tahun - 1900

        repository.getTenagaKerja(
            "3574",
            variable,
            th,
            API_KEY
        ).enqueue(
            object :
                Callback<TenagaKerjaResponse> {

                override fun onResponse(
                    call: Call<TenagaKerjaResponse>,
                    response: Response<TenagaKerjaResponse>
                ) {

                    val body = response.body()

                    if (
                        !response.isSuccessful ||
                        body == null ||
                        body.status != "OK"
                    ) {

                        cariDataStatistikTerbaru(
                            variable,
                            tahun - 1,
                            index,
                            cardToUpdate
                        )

                        return
                    }

                    val variableData =
                        body.variable?.firstOrNull()

                    val subject =
                        body.subject?.firstOrNull()

                    val tahunData =
                        body.tahun?.firstOrNull()

                    val nilai =
                        body.dataContent
                            ?.values
                            ?.firstOrNull()

                    if (
                        variableData == null ||
                        nilai == null
                    ) {

                        cariDataStatistikTerbaru(
                            variable,
                            tahun - 1,
                            index,
                            cardToUpdate
                        )

                        return
                    }

                    val judul =
                        variableData.label
                            ?: "Statistik"

                    val kategori =
                        subject?.label
                            ?: variableData.subject
                            ?: "-"

                    val tahunLabel =
                        tahunData?.label
                            ?: tahun.toString()

                    val unit =
                        variableData.unit
                            ?: ""

                    if (!isAdded) return

                    requireActivity()
                        .runOnUiThread {

                            if (!isAdded) {
                                return@runOnUiThread
                            }

                            updateCardStatistikData(
                                card = cardToUpdate,
                                judul = judul,
                                kategori = kategori,
                                nilai = nilai,
                                tahun = tahunLabel,
                                unit = unit,
                                bisaDiklik = true
                            )
                        }
                }

                override fun onFailure(
                    call: Call<TenagaKerjaResponse>,
                    t: Throwable
                ) {

                    cariDataStatistikTerbaru(
                        variable,
                        tahun - 1,
                        index,
                        cardToUpdate
                    )
                }
            }
        )
    }

    // ============================================================
    // PDRB
    // ============================================================

    private fun loadPdrbTerkini(
        container: LinearLayout,
        cardToUpdate: View
    ) {

        repository.getEkonomiDetail(
            domain = "3574",
            id = PDRB_TABLE_ID,
            tahun = TAHUN_MULAI,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<EkonomiDetailResponse> {

                override fun onResponse(
                    call: Call<EkonomiDetailResponse>,
                    response: Response<EkonomiDetailResponse>
                ) {

                    if (!response.isSuccessful) return

                    val body = response.body()
                    if (body == null || body.status != "OK") return

                    prosesDetailPdrb(body, cardToUpdate)
                }

                override fun onFailure(
                    call: Call<EkonomiDetailResponse>,
                    t: Throwable
                ) {}
            }
        )
    }

    private fun prosesDetailPdrb(
        body: EkonomiDetailResponse,
        cardToUpdate: View
    ) {

        val variable =
            body.variables?.firstOrNull {
                it.valId?.toString() == PDRB_VARIABLE_ID ||
                        it.value?.toString() == PDRB_VARIABLE_ID
            } ?: return

        val vervar =
            body.vervar?.firstOrNull {
                (it.value ?: it.valId)?.toString() == PDRB_VERVAR_ID.toString()
            } ?: return

        val dataContent = body.dataContent ?: return

        val tahunData =
            body.tahun
                ?.sortedByDescending { it.label?.toIntOrNull() ?: 0 }
                ?.firstOrNull() ?: return

        val tahunVal = tahunData.value ?: tahunData.valId ?: return
        val tahunFormatted = tahunVal.toString().padStart(4, '0')

        val urutanTriwulan = listOf(34, 33, 32, 31)
        var triwulanTerpilih: EkonomiItem? = null
        var nilaiTerpilih: Double? = null

        for (kodeTriwulan in urutanTriwulan) {
            val triwulan = body.turtahun?.firstOrNull { it.valId == kodeTriwulan } ?: continue
            val key = "${PDRB_VERVAR_ID}${PDRB_VARIABLE_ID}${tahunFormatted}${kodeTriwulan}"
            val jsonValue = dataContent.get(key)

            if (jsonValue != null && !jsonValue.isJsonNull) {
                try {
                    nilaiTerpilih = jsonValue.asDouble
                    triwulanTerpilih = triwulan
                    break
                } catch (e: Exception) {}
            }
        }

        if (triwulanTerpilih == null || nilaiTerpilih == null) return

        val nilai = nilaiTerpilih
        val triwulan = triwulanTerpilih
        val tahunLabel = tahunData.label ?: tahunFormatted
        val triwulanLabel = triwulan.label ?: "Triwulan"
        val subject = body.subject?.firstOrNull()
        val kategori = subject?.label ?: "Neraca Ekonomi"

        if (!isAdded) return

        requireActivity().runOnUiThread {
            if (!isAdded) return@runOnUiThread

            updateCardStatistikData(
                card = cardToUpdate,
                judul = "Laju Pertumbuhan PDRB",
                kategori = kategori,
                nilai = nilai,
                tahun = "$tahunLabel $triwulanLabel",
                unit = "Persen",
                bukaEkonomi = true
            )
        }
    }

    // ============================================================
    // IKG
    // ============================================================

    private fun loadIkgTerkini(
        container: LinearLayout,
        cardToUpdate: View
    ) {
        cariIkgTerkini(TAHUN_MULAI, cardToUpdate)
    }

    private fun cariIkgTerkini(
        tahun: Int,
        cardToUpdate: View
    ) {

        if (tahun < TAHUN_MINIMUM) return

        val kodeTahun = tahun - 1900

        repository.getTenagaKerja(
            domain = "3574",
            variable = IKG_VARIABLE_ID,
            tahun = kodeTahun,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<TenagaKerjaResponse> {

                override fun onResponse(
                    call: Call<TenagaKerjaResponse>,
                    response: Response<TenagaKerjaResponse>
                ) {

                    if (!response.isSuccessful) {
                        cariIkgTerkini(tahun - 1, cardToUpdate)
                        return
                    }

                    val body = response.body()
                    if (body == null || body.status?.uppercase(Locale.ROOT) != "OK") {
                        cariIkgTerkini(tahun - 1, cardToUpdate)
                        return
                    }

                    val nilai = body.dataContent?.values?.firstOrNull()
                    if (nilai == null) {
                        cariIkgTerkini(tahun - 1, cardToUpdate)
                        return
                    }

                    val variableData = body.variable?.firstOrNull()
                    val subject = body.subject?.firstOrNull()
                    val tahunLabel = body.tahun?.firstOrNull()?.label ?: tahun.toString()
                    val kategori = subject?.label ?: variableData?.subject ?: "Gender"

                    if (!isAdded) return

                    requireActivity().runOnUiThread {
                        if (!isAdded) return@runOnUiThread

                        updateCardStatistikData(
                            card = cardToUpdate,
                            judul = "Indeks Ketimpangan Gender (IKG)",
                            kategori = kategori,
                            nilai = nilai,
                            tahun = tahunLabel,
                            unit = "",
                            bukaGender = true
                        )
                    }
                }

                override fun onFailure(
                    call: Call<TenagaKerjaResponse>,
                    t: Throwable
                ) {
                    cariIkgTerkini(tahun - 1, cardToUpdate)
                }
            }
        )
    }

    // ============================================================
    // HARGA
    // ============================================================

    private fun loadHargaTerkini(
        container: LinearLayout
    ) {}

    private fun cariHargaTerkini(
        variable: Int,
        tahun: Int,
        index: Int,
        cardToUpdate: View
    ) {

        if (tahun < TAHUN_MINIMUM) return

        val kodeTahun = tahun - 1900

        repository.getHarga(
            domain = "3574",
            variable = variable,
            tahun = kodeTahun,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<HargaResponse> {

                override fun onResponse(
                    call: Call<HargaResponse>,
                    response: Response<HargaResponse>
                ) {

                    if (!response.isSuccessful) {
                        cariHargaTerkini(variable, tahun - 1, index, cardToUpdate)
                        return
                    }

                    val body = response.body()
                    if (body == null || body.status?.uppercase(Locale.ROOT) != "OK") {
                        cariHargaTerkini(variable, tahun - 1, index, cardToUpdate)
                        return
                    }

                    val nilai = body.dataContent?.values?.firstOrNull()
                    if (nilai == null) {
                        cariHargaTerkini(variable, tahun - 1, index, cardToUpdate)
                        return
                    }

                    val variableData = body.variable?.firstOrNull()
                    val subject = body.subject?.firstOrNull()
                    val tahunLabel = body.tahun?.firstOrNull()?.label ?: tahun.toString()
                    val judul = variableData?.label ?: "Harga-Harga"
                    val kategori = subject?.label ?: variableData?.subject ?: "Harga-Harga"
                    val unit = variableData?.unit ?: ""

                    if (!isAdded) return

                    requireActivity().runOnUiThread {
                        if (!isAdded) return@runOnUiThread

                        updateCardStatistikData(
                            card = cardToUpdate,
                            judul = judul,
                            kategori = kategori,
                            nilai = nilai,
                            tahun = tahunLabel,
                            unit = unit,
                            bisaDiklik = true,
                            bukaHarga = true
                        )
                    }
                }

                override fun onFailure(
                    call: Call<HargaResponse>,
                    t: Throwable
                ) {
                    cariHargaTerkini(variable, tahun - 1, index, cardToUpdate)
                }
            }
        )
    }

    // ============================================================
    // STATISTIK CAROUSEL LAYOUT
    // ============================================================

    private fun updateStatistikCarouselLayout() {

        if (
            !::statistikScrollView.isInitialized ||
            !::statistikIndicator.isInitialized
        ) {
            return
        }

        val container =
            statistikScrollView.findViewById<LinearLayout>(
                R.id.statistikTerkiniContainer
            )
                ?: return

        val screenWidth =
            statistikScrollView.width

        if (screenWidth <= 0) {

            statistikScrollView.post {
                updateStatistikCarouselLayout()
            }

            return
        }

        val duaCardWidth =
            dpToPx(
                150 * 2 + 8
            )

        val padding =
            (
                    screenWidth -
                            duaCardWidth
                    ) / 2

        if (padding > 0) {

            container.setPadding(
                padding,
                0,
                padding,
                0
            )
        }

        setupStatistikIndicator()

        statistikScrollView.post {

            if (!isAdded) {
                return@post
            }

            val maxPosition =
                (
                        statistikCardCount - 1
                        )
                    .coerceAtLeast(0)

            statistikCurrentPosition =
                statistikCurrentPosition.coerceIn(
                    0,
                    maxPosition
                )

            val targetX =
                dpToPx(
                    statistikCardWidthDp
                ) *
                        statistikCurrentPosition

            statistikScrollView.scrollTo(
                targetX,
                0
            )

            updateStatistikIndicator()

            animateStatistikCards(
                targetX
            )
        }
    }

    // ============================================================
    // STATISTIK INDICATOR
    // ============================================================

    private fun setupStatistikIndicator() {

        if (
            !::statistikIndicator.isInitialized
        ) {
            return
        }

        statistikIndicator.removeAllViews()

        if (
            statistikCardCount <= 0
        ) {
            return
        }

        repeat(
            statistikCardCount
        ) { index ->

            val dot =
                View(requireContext())

            val active =
                index ==
                        statistikCurrentPosition

            val size =
                if (active) {
                    8
                } else {
                    6
                }

            val params =
                LinearLayout.LayoutParams(
                    dpToPx(size),
                    dpToPx(size)
                )

            params.marginStart =
                dpToPx(3)

            params.marginEnd =
                dpToPx(3)

            dot.layoutParams =
                params

            dot.background =
                createDotBackground(
                    active
                )

            statistikIndicator.addView(
                dot
            )
        }
    }

    private fun updateStatistikIndicator() {

        if (
            !::statistikIndicator.isInitialized
        ) {
            return
        }

        for (
        i in 0 until
                statistikIndicator.childCount
        ) {

            val dot =
                statistikIndicator.getChildAt(i)

            val active =
                i ==
                        statistikCurrentPosition

            val size =
                if (active) {
                    8
                } else {
                    6
                }

            val params =
                dot.layoutParams

            params.width =
                dpToPx(size)

            params.height =
                dpToPx(size)

            dot.layoutParams =
                params

            dot.background =
                createDotBackground(
                    active
                )
        }
    }

    private fun createDotBackground(
        active: Boolean
    ): android.graphics.drawable.GradientDrawable {

        val drawable =
            android.graphics.drawable.GradientDrawable()

        drawable.shape =
            android.graphics.drawable.GradientDrawable.OVAL

        drawable.setColor(
            Color.parseColor(
                if (active) {
                    "#F97316"
                } else {
                    "#D1D5DB"
                }
            )
        )

        return drawable
    }

    // ============================================================
    // SCROLL STATISTIK
    // ============================================================

    private fun scrollToStatistik(
        position: Int,
        animated: Boolean
    ) {

        if (
            statistikCardCount <= 0
        ) {
            return
        }

        val posisi =
            position.coerceIn(
                0,
                statistikCardCount - 1
            )

        statistikCurrentPosition =
            posisi

        val targetX =
            dpToPx(
                statistikCardWidthDp
            ) *
                    posisi

        if (animated) {

            val scrollView =
                view?.findViewById<HorizontalScrollView>(
                    R.id.statistikTerkiniScrollView
                )
                    ?: return

            statistikScrollAnimator?.cancel()

            statistikScrollAnimator =
                ObjectAnimator.ofInt(
                    scrollView,
                    "scrollX",
                    scrollView.scrollX,
                    targetX
                ).apply {

                    duration = 800L

                    interpolator =
                        DecelerateInterpolator(
                            1.5f
                        )

                    start()
                }

        } else {
            statistikScrollAnimator?.cancel()

            statistikScrollView.scrollTo(
                targetX,
                0
            )
        }

        updateStatistikIndicator()
    }

    // ============================================================
    // ANIMATE STATISTIK CARDS BASED ON POSITION
    // ============================================================

    private fun animateStatistikCards(
        scrollX: Int
    ) {

        if (
            !::statistikScrollView.isInitialized
        ) {
            return
        }

        val container =
            statistikScrollView.findViewById<LinearLayout>(
                R.id.statistikTerkiniContainer
            )
                ?: return

        val viewportCenter =
            statistikScrollView.width / 2f

        if (
            viewportCenter <= 0
        ) {
            return
        }

        for (
        i in 0 until
                container.childCount
        ) {

            val card =
                container.getChildAt(i)

            val cardCenter =
                card.left -
                        scrollX +
                        card.width / 2f

            val distance =
                abs(
                    cardCenter -
                            viewportCenter
                )

            val normalized =
                (
                        distance /
                                viewportCenter
                        )
                    .coerceIn(
                        0f,
                        1f
                    )

            val alpha =
                1f -
                        (
                                normalized *
                                        0.30f
                                )

            val scale =
                1f -
                        (
                                normalized *
                                        0.06f
                                )

            card.alpha =
                alpha.coerceIn(
                    0.70f,
                    1f
                )

            card.scaleX =
                scale

            card.scaleY =
                scale
        }
    }

    // ============================================================
    // DP TO PX
    // ============================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                )
            .roundToInt()
    }

    // ============================================================
    // INFOGRAFIK
    // ============================================================

    private fun loadInfographics(
        view: View
    ) {

        val container =
            view.findViewById<LinearLayout>(
                R.id.infografikContainer
            )

        repository.getInfographicsHome(
            API_KEY
        ).enqueue(
            object :
                Callback<InfographicResponse> {

                override fun onResponse(
                    call: Call<InfographicResponse>,
                    response: Response<InfographicResponse>
                ) {

                    val body = response.body()

                    if (
                        !response.isSuccessful ||
                        body == null ||
                        body.status != "OK" ||
                        body.data == null
                    ) {
                        return
                    }

                    val gson = Gson()
                    val data = mutableListOf<Infografik>()

                    body.data.forEach { element ->
                        if (element.isJsonArray) {
                            element.asJsonArray.forEach { item ->
                                try {
                                    if (item.isJsonObject) {
                                        data.add(
                                            gson.fromJson(
                                                item,
                                                Infografik::class.java
                                            )
                                        )
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }

                    val limaTerbaru =
                        data
                            .distinctBy { it.infId }
                            .sortedByDescending { it.date ?: "" }
                            .take(5)

                    if (!isAdded) return

                    requireActivity().runOnUiThread {
                        if (!isAdded) return@runOnUiThread

                        container.removeAllViews()

                        limaTerbaru.forEach {
                            tambahCardInfografik(container, it)
                        }

                        val scrollView = view.findViewById<ScrollView>(R.id.homeScrollView)
                        val infografik = view.findViewById<View>(R.id.sectionInfografik)
                        val berita = view.findViewById<View>(R.id.sectionBerita)
                        val publikasi = view.findViewById<View>(R.id.sectionPublikasi)

                        scrollView.post {
                            if (!isAdded) return@post
                            checkSectionAnimations(scrollView, infografik, berita, publikasi)
                        }
                    }
                }

                override fun onFailure(
                    call: Call<InfographicResponse>,
                    t: Throwable
                ) {}
            }
        )
    }

    private fun tambahCardInfografik(
        container: LinearLayout,
        infographic: Infografik
    ) {

        val card =
            LayoutInflater.from(
                requireContext()
            ).inflate(
                R.layout.item_home,
                container,
                false
            )
        card.setOnClickListener {
            (activity as? HomeActivity)?.goToPage(1)
        }

        card.findViewById<TextView>(
            R.id.tvHomeTitle
        ).text =
            infographic.title
                ?: "Infografis"

        card.findViewById<TextView>(
            R.id.tvHomeDescription
        ).text =
            Html.fromHtml(
                infographic.desc ?: "",
                Html.FROM_HTML_MODE_LEGACY
            )
                .toString()
                .trim()

        val progressImage =
            card.findViewById<ProgressBar>(
                R.id.progressImage
            )

        val image =
            card.findViewById<ImageView>(
                R.id.imgHome
            )

        progressImage.visibility =
            View.VISIBLE

        Glide.with(image)
            .load(infographic.img)
            .diskCacheStrategy(
                DiskCacheStrategy.ALL
            )
            .placeholder(
                R.drawable.ic_bpslogo
            )
            .error(
                R.drawable.ic_bpslogo
            )
            .into(
                object : CustomTarget<Drawable>() {

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {

                        image.setImageDrawable(
                            resource
                        )

                        progressImage.visibility =
                            View.GONE
                    }

                    override fun onLoadFailed(
                        errorDrawable: Drawable?
                    ) {

                        image.setImageResource(
                            R.drawable.ic_bpslogo
                        )

                        progressImage.visibility =
                            View.GONE
                    }

                    override fun onLoadCleared(
                        placeholder: Drawable?
                    ) {

                        image.setImageDrawable(
                            placeholder
                        )
                    }
                }
            )

        container.addView(
            card
        )
    }

    // ============================================================
    // BERITA
    // ============================================================

    private fun loadBerita(
        view: View
    ) {

        repository.getNews(
            "3574",
            1,
            10,
            API_KEY
        ) { response, error ->

            if (
                !isAdded ||
                error != null ||
                response == null
            ) {
                return@getNews
            }

            try {

                val dataArray =
                    response.getAsJsonArray(
                        "data"
                    )

                if (
                    dataArray.size() < 2
                ) {
                    return@getNews
                }

                val beritaTerbaru =
                    dataArray[1]
                        .asJsonArray
                        .mapNotNull {

                            try {

                                Gson().fromJson(
                                    it,
                                    NewsItem::class.java
                                )

                            } catch (
                                e: Exception
                            ) {

                                null
                            }
                        }
                        .take(5)

                requireActivity()
                    .runOnUiThread {

                        if (!isAdded) {
                            return@runOnUiThread
                        }

                        tampilkanBerita(
                            view,
                            beritaTerbaru
                        )

                        val scrollView =
                            view.findViewById<ScrollView>(
                                R.id.homeScrollView
                            )

                        val infografik =
                            view.findViewById<View>(
                                R.id.sectionInfografik
                            )

                        val beritaSection =
                            view.findViewById<View>(
                                R.id.sectionBerita
                            )

                        val publikasi =
                            view.findViewById<View>(
                                R.id.sectionPublikasi
                            )

                        scrollView.post {

                            if (!isAdded) {
                                return@post
                            }

                            checkSectionAnimations(
                                scrollView,
                                infografik,
                                beritaSection,
                                publikasi
                            )
                        }
                    }

            } catch (
                e: Exception
            ) {}
        }
    }

    private fun tampilkanBerita(
        view: View,
        berita: List<NewsItem>
    ) {

        val container =
            view.findViewById<LinearLayout>(
                R.id.beritaContainer
            )
                ?: return

        container.removeAllViews()


        berita.forEach { item ->

            val card =
                LayoutInflater.from(
                    requireContext()
                ).inflate(
                    R.layout.item_home,
                    container,
                    false
                )
            card.setOnClickListener{
                (activity as?HomeActivity)?.goToPage(0)
            }

            card.findViewById<TextView>(
                R.id.tvHomeTitle
            ).text =
                item.title
                    ?: "Berita"

            card.findViewById<TextView>(
                R.id.tvHomeDescription
            ).text =
                Html.fromHtml(
                    item.news ?: "",
                    Html.FROM_HTML_MODE_LEGACY
                )
                    .toString()
                    .trim()

            val progressImage =
                card.findViewById<ProgressBar>(
                    R.id.progressImage
                )

            val image =
                card.findViewById<ImageView>(
                    R.id.imgHome
                )

            progressImage.visibility =
                View.VISIBLE

            Glide.with(image)
                .load(item.picture)
                .diskCacheStrategy(
                    DiskCacheStrategy.ALL
                )
                .placeholder(
                    R.drawable.ic_bpslogo
                )
                .error(
                    R.drawable.ic_bpslogo
                )
                .into(
                    object : CustomTarget<Drawable>() {

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {

                            image.setImageDrawable(
                                resource
                            )

                            progressImage.visibility =
                                View.GONE
                        }

                        override fun onLoadFailed(
                            errorDrawable: Drawable?
                        ) {

                            image.setImageResource(
                                R.drawable.ic_bpslogo
                            )

                            progressImage.visibility =
                                View.GONE
                        }

                        override fun onLoadCleared(
                            placeholder: Drawable?
                        ) {

                            image.setImageDrawable(
                                placeholder
                            )
                        }
                    }
                )

            container.addView(
                card
            )
        }
    }

    // ============================================================
    // PUBLIKASI
    // ============================================================

    private fun loadPublikasi(
        view: View
    ) {

        val container =
            view.findViewById<LinearLayout>(
                R.id.publicationContainer
            )

        repository.getPublikasi(
            1,
            "3574",
            API_KEY
        ) { response ->

            if (
                !isAdded ||
                response == null
            ) {
                return@getPublikasi
            }

            try {

                val publikasi =
                    response
                        .getPublikasi()
                        .distinctBy {
                            it.pub_id
                        }
                        .take(5)

                requireActivity()
                    .runOnUiThread {

                        if (!isAdded) {
                            return@runOnUiThread
                        }

                        container.removeAllViews()

                        publikasi.forEach {

                            tambahCardPublikasi(
                                container,
                                it
                            )
                        }

                        val scrollView =
                            view.findViewById<ScrollView>(
                                R.id.homeScrollView
                            )

                        val infografik =
                            view.findViewById<View>(
                                R.id.sectionInfografik
                            )

                        val berita =
                            view.findViewById<View>(
                                R.id.sectionBerita
                            )

                        val publikasiSection =
                            view.findViewById<View>(
                                R.id.sectionPublikasi
                            )

                        scrollView.post {

                            if (!isAdded) {
                                return@post
                            }

                            checkSectionAnimations(
                                scrollView,
                                infografik,
                                berita,
                                publikasiSection
                            )
                        }
                    }

            } catch (
                e: Exception
            ) {}
        }
    }

    private fun tambahCardPublikasi(
        container: LinearLayout,
        publikasi: Publikasi
    ) {

        val card =
            LayoutInflater.from(
                requireContext()
            ).inflate(
                R.layout.item_home_panjang,
                container,
                false
            )

        card.findViewById<TextView>(
            R.id.tvStatistikTitle
        ).text =
            publikasi.title
                ?: "Publikasi"

        card.findViewById<TextView>(
            R.id.tvStatistikDescription
        ).text =
            Html.fromHtml(
                publikasi.abstract ?: "",
                Html.FROM_HTML_MODE_LEGACY
            )
                .toString()
                .trim()

        Glide.with(this)
            .load(
                publikasi.cover
            )
            .diskCacheStrategy(
                DiskCacheStrategy.ALL
            )
            .placeholder(
                R.drawable.ic_bpslogo
            )
            .error(
                R.drawable.ic_bpslogo
            )
            .into(
                card.findViewById(
                    R.id.imgStatistik
                )
            )

        card.setOnClickListener {

            (activity as? HomeActivity)
                ?.goToPage(2)
        }

        container.addView(
            card
        )
    }

    // ============================================================
    // MAIN SCROLL ANIMATION
    // ============================================================

    private fun setupScrollAnimation(
        view: View
    ) {

        val scrollView =
            view.findViewById<ScrollView>(
                R.id.homeScrollView
            )

        val statistik =
            view.findViewById<View>(
                R.id.sectionStatistik
            )

        val infografik =
            view.findViewById<View>(
                R.id.sectionInfografik
            )

        val berita =
            view.findViewById<View>(
                R.id.sectionBerita
            )

        val publikasi =
            view.findViewById<View>(
                R.id.sectionPublikasi
            )

        prepareSection(
            berita
        )

        prepareSection(
            publikasi
        )

        animateStatistik(
            statistik
        )

        if (!infografikAnimated) {

            infografikAnimated = true

            animateFromBottom(
                infografik,
                delay = 0L
            )
        }

        scrollView.post {

            if (!isAdded) {
                return@post
            }

            checkSectionAnimations(
                scrollView,
                infografik,
                berita,
                publikasi
            )
        }

        scrollView
            .viewTreeObserver
            .addOnScrollChangedListener {

                if (!isAdded) {
                    return@addOnScrollChangedListener
                }

                checkSectionAnimations(
                    scrollView,
                    infografik,
                    berita,
                    publikasi
                )
            }
    }

    // ============================================================
    // CHECK SECTION ANIMATION
    // ============================================================

    private fun checkSectionAnimations(
        scrollView: ScrollView,
        infografik: View,
        berita: View,
        publikasi: View
    ) {

        if (!isAdded) {
            return
        }

        if (
            !infografikAnimated &&
            isViewVisible(
                infografik,
                scrollView
            )
        ) {

            infografikAnimated = true

            animateFromBottom(
                infografik
            )
        }

        if (
            !beritaAnimated &&
            isViewVisible(
                berita,
                scrollView
            )
        ) {

            beritaAnimated = true

            animateFromBottom(
                berita
            )
        }

        if (
            !publikasiAnimated &&
            isViewVisible(
                publikasi,
                scrollView
            )
        ) {

            publikasiAnimated = true

            animateFromBottom(
                publikasi
            )
        }
    }

    // ============================================================
    // PREPARE SECTION
    // ============================================================

    private fun prepareSection(
        view: View
    ) {

        view.animate().cancel()

        view.alpha = 0f

        view.translationY = dpToPx(
            60
        ).toFloat()
    }

    // ============================================================
    // SECTION ANIMATION
    // ============================================================

    private fun animateFromBottom(
        view: View,
        delay: Long = 0L
    ) {

        view.animate().cancel()

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(650L)
            .setStartDelay(delay)
            .setInterpolator(
                DecelerateInterpolator(
                    1.5f
                )
            )
            .withLayer()
            .start()
    }

    // ============================================================
    // STATISTIK CARD ANIMATION
    // ============================================================

    private fun animateCardIn(
        card: View,
        index: Int
    ) {

        card.animate().cancel()

        card.alpha = 0f

        card.translationY =
            -dpToPx(
                45
            ).toFloat()

        card.scaleX = 0.96f
        card.scaleY = 0.96f

        card.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500L)
            .setStartDelay(
                index.coerceAtMost(6) * 70L
            )
            .setInterpolator(
                DecelerateInterpolator(
                    1.5f
                )
            )
            .withLayer()
            .start()
    }

    // ============================================================
    // STATISTIK SECTION ANIMATION
    // ============================================================

    private fun animateStatistik(
        section: View
    ) {

        if (statistikAnimated) {
            return
        }

        statistikAnimated = true

        val container =
            section.findViewById<ViewGroup>(
                R.id.statistikContainer
            )
                ?: return

        var index = 0

        for (
        i in 0 until container.childCount
        ) {

            val row =
                container.getChildAt(i)
                        as? ViewGroup
                    ?: continue

            for (
            j in 0 until row.childCount
            ) {

                animateCardIn(
                    row.getChildAt(j),
                    index++
                )
            }
        }
    }

    // ============================================================
    // CHECK VIEW VISIBILITY
    // ============================================================

    private fun isViewVisible(
        view: View,
        scrollView: ScrollView
    ): Boolean {

        if (
            view.height <= 0 ||
            scrollView.height <= 0
        ) {
            return false
        }

        val location =
            IntArray(2)

        view.getLocationOnScreen(
            location
        )

        val scrollLocation =
            IntArray(2)

        scrollView.getLocationOnScreen(
            scrollLocation
        )

        val scrollTop =
            scrollLocation[1]

        val scrollBottom =
            scrollTop +
                    scrollView.height

        val viewTop =
            location[1]

        val viewBottom =
            viewTop +
                    view.height

        return viewTop < scrollBottom &&
                viewBottom > scrollTop
    }

    // ============================================================
    // DESTROY VIEW
    // ============================================================

    override fun onDestroyView() {

        statistikScrollAnimator?.cancel()
        statistikScrollAnimator = null

        statistikHandler.removeCallbacks(
            statistikAutoScrollRunnable
        )

        statistikScrollView.setOnScrollChangeListener(null)
        statistikScrollView.setOnTouchListener(null)

        super.onDestroyView()

        statistikAnimated = false
        infografikAnimated = false
        beritaAnimated = false
        publikasiAnimated = false

        statistikCurrentPosition = 0
        statistikCardCount = 0
    }
}