package com.example.bpskota

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

import com.example.bpskota.bps.model.NewsItem
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import com.google.gson.JsonArray

class BeritaFragment : Fragment(), RefreshableFragment {

    companion object {
        private const val PAGE_INDEX = 0
    }

    private lateinit var repository: BpsRepository

    private val API_KEY = "008edaaae5d450b1913b31a2cef618c3"

    // Cache data berita selama instance Fragment masih hidup.
    private val semuaBerita = mutableListOf<NewsItem>()

    // Penampung sementara agar data lama tidak hilang ketika refresh gagal.
    private val beritaSementara = mutableListOf<NewsItem>()

    private var tahunTerpilih: String? = null
    private var keywordPencarian = ""

    private var totalPage = 1
    private var jumlahPageSelesai = 0

    private var sudahMemuat = false
    private var sedangLoad = false
    private var sedangRefresh = false
    private var adaPageGagal = false

    private lateinit var beritaContainer: LinearLayout
    private lateinit var tvJumlahBerita: TextView
    private lateinit var etSearchBerita: EditText
    private lateinit var btnFilterBerita: View
    private lateinit var loadingView: LottieAnimationView

    private var beritaScrollView: ScrollView? = null

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        repository = BpsRepository()

        beritaContainer = view.findViewById(R.id.beritaContainer)
        tvJumlahBerita = view.findViewById(R.id.tvJumlahBerita)
        etSearchBerita = view.findViewById(R.id.etSearchBerita)
        btnFilterBerita = view.findViewById(R.id.btnFilterBerita)

        setupLoading()
        setupSearch()
        setupFilter()
        setupSwipeRefreshState(view)

        if (sudahMemuat) {
            tampilkanBerita()
        }
    }

    override fun onResume() {
        super.onResume()

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        if (!sudahMemuat && !sedangLoad) {
            loadSemuaBerita()
        } else if (sudahMemuat) {
            tampilkanBerita()
        }
    }

    private fun setupSwipeRefreshState(view: View) {
        beritaScrollView = findScrollView(view)

        val scrollView = beritaScrollView ?: return

        val swipeRefreshHome = requireActivity().findViewById<SwipeRefreshLayout>(
            R.id.swipeRefreshHome
        )

        fun updateState() {
            if (!isAdded) return

            val homeActivity = activity as? HomeActivity ?: return

            if (homeActivity.currentPage != PAGE_INDEX) return

            swipeRefreshHome.isEnabled =
                !scrollView.canScrollVertically(-1)
        }

        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            updateState()
        }

        scrollView.post {
            updateState()
        }
    }

    override fun updateRefreshState() {
        if (!isAdded) return

        val currentView = view ?: return

        val scrollView = beritaScrollView
            ?: findScrollView(currentView)
            ?: return

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        val swipeRefreshHome = requireActivity().findViewById<SwipeRefreshLayout>(
            R.id.swipeRefreshHome
        )

        swipeRefreshHome.isEnabled =
            !scrollView.canScrollVertically(-1)
    }

    private fun findScrollView(view: View): ScrollView? {
        if (view is ScrollView) {
            return view
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)

                val result = findScrollView(child)

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    override fun refreshData() {
        if (!isAdded || sedangLoad) {
            selesaiSwipeRefresh()
            return
        }

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        val currentView = view ?: return

        val scrollView = beritaScrollView
            ?: findScrollView(currentView)

        if (scrollView != null && scrollView.canScrollVertically(-1)) {
            return
        }

        sedangRefresh = true
        loadSemuaBerita(refresh = true)
    }

    private fun setupLoading() {
        loadingView = LottieAnimationView(requireContext())

        loadingView.layoutParams = LinearLayout.LayoutParams(
            dpToPx(100),
            dpToPx(100)
        ).apply {
            gravity = Gravity.CENTER
            topMargin = dpToPx(40)
            bottomMargin = dpToPx(40)
        }

        loadingView.setAnimation("Loading_Animation.json")
        loadingView.repeatCount = -1
        loadingView.visibility = View.GONE

        beritaContainer.addView(loadingView)
    }

    private fun tampilkanLoading() {
        if (!::loadingView.isInitialized) return

        loadingView.visibility = View.VISIBLE
        loadingView.playAnimation()

        tvJumlahBerita.text = "Memuat berita..."

        beritaContainer.childrenExceptLoading()
    }

    private fun sembunyikanLoading() {
        if (!::loadingView.isInitialized) return

        loadingView.cancelAnimation()
        loadingView.visibility = View.GONE
    }

    private fun loadSemuaBerita(refresh: Boolean = false) {
        if (sedangLoad) return

        if (!isAdded) return

        sedangLoad = true

        beritaSementara.clear()

        totalPage = 1
        jumlahPageSelesai = 0
        adaPageGagal = false

        if (!refresh && !sudahMemuat) {
            tahunTerpilih = null
            keywordPencarian = ""

            if (::etSearchBerita.isInitialized) {
                etSearchBerita.setText("")
            }
        }

        if (!sudahMemuat && view != null) {
            tampilkanLoading()
        }

        loadPagePertama()
    }

    private fun loadPagePertama() {
        if (!isAdded) return

        repository.getNews(
            domain = "3574",
            page = 1,
            perPage = 10,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) return@getNews

            activity?.runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (error != null || response == null) {
                    gagalMemuatBerita("Gagal memuat berita")
                    return@runOnUiThread
                }

                try {
                    val dataArray = response.getAsJsonArray("data")

                    if (dataArray == null || dataArray.size() < 2) {
                        gagalMemuatBerita("Format data berita tidak valid")
                        return@runOnUiThread
                    }

                    val newsArray = dataArray[1].asJsonArray

                    prosesBeritaPage(newsArray)

                    jumlahPageSelesai = 1
                    totalPage = ambilTotalPage(dataArray)

                    if (totalPage <= 1) {
                        selesaiMemuatSemuaBerita()
                        return@runOnUiThread
                    }

                    for (page in 2..totalPage) {
                        loadPageParalel(page)
                    }

                } catch (e: Exception) {
                    gagalMemuatBerita("Gagal memproses berita")
                }
            }
        }
    }

    private fun loadPageParalel(page: Int) {
        if (!isAdded) return

        repository.getNews(
            domain = "3574",
            page = page,
            perPage = 10,
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) return@getNews

            activity?.runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (error != null || response == null) {
                    adaPageGagal = true

                    jumlahPageSelesai++
                    cekSelesaiSemuaPage()

                    return@runOnUiThread
                }

                try {
                    val dataArray = response.getAsJsonArray("data")

                    if (dataArray == null || dataArray.size() < 2) {
                        adaPageGagal = true

                        jumlahPageSelesai++
                        cekSelesaiSemuaPage()

                        return@runOnUiThread
                    }

                    val newsArray = dataArray[1].asJsonArray

                    prosesBeritaPage(newsArray)

                    jumlahPageSelesai++
                    cekSelesaiSemuaPage()

                } catch (e: Exception) {
                    adaPageGagal = true

                    jumlahPageSelesai++
                    cekSelesaiSemuaPage()
                }
            }
        }
    }

    private fun prosesBeritaPage(newsArray: JsonArray) {
        newsArray.forEach { item ->
            try {
                val news = Gson().fromJson(
                    item,
                    NewsItem::class.java
                )

                beritaSementara.add(news)

            } catch (e: Exception) {
                // Abaikan item yang gagal diparsing
            }
        }
    }

    private fun ambilTotalPage(dataArray: JsonArray): Int {
        try {
            val metadata = dataArray[0]

            if (metadata.isJsonObject) {
                val obj = metadata.asJsonObject

                val kemungkinanField = listOf(
                    "total_page",
                    "total_pages",
                    "pages",
                    "page_total"
                )

                for (field in kemungkinanField) {
                    if (obj.has(field) && !obj.get(field).isJsonNull) {
                        val total = obj.get(field).asInt

                        if (total > 0) {
                            return total
                        }
                    }
                }
            }

        } catch (e: Exception) {
            // Gunakan nilai default
        }

        return 1
    }

    private fun cekSelesaiSemuaPage() {
        if (jumlahPageSelesai >= totalPage) {
            if (adaPageGagal) {
                gagalMemuatBerita("Sebagian berita gagal dimuat")
            } else {
                selesaiMemuatSemuaBerita()
            }
        }
    }

    private fun selesaiMemuatSemuaBerita() {
        if (!isAdded) return

        semuaBerita.clear()

        semuaBerita.addAll(
            beritaSementara.distinctBy { it.news_id }
        )

        beritaSementara.clear()

        sudahMemuat = true
        sedangLoad = false
        sedangRefresh = false

        if (view != null) {
            sembunyikanLoading()
            tampilkanBerita()
        }

        selesaiSwipeRefresh()
    }

    private fun gagalMemuatBerita(pesan: String) {
        sedangLoad = false
        sedangRefresh = false

        beritaSementara.clear()

        if (view != null) {
            sembunyikanLoading()

            if (sudahMemuat) {
                tampilkanBerita()
            } else {
                tvJumlahBerita.text = pesan
            }
        }

        selesaiSwipeRefresh()

        if (isAdded) {
            android.widget.Toast.makeText(
                requireContext(),
                pesan,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun selesaiSwipeRefresh() {
        if (!isAdded) return

        val homeActivity = activity as? HomeActivity ?: return

        homeActivity.finishSwipeRefresh()

        updateRefreshState()

        sedangRefresh = false
    }

    private fun setupSearch() {
        etSearchBerita.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                keywordPencarian =
                    s?.toString()?.trim()?.lowercase() ?: ""

                if (!sedangLoad && sudahMemuat) {
                    tampilkanBerita()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilter() {
        btnFilterBerita.setOnClickListener {
            if (sedangLoad || !sudahMemuat) {
                return@setOnClickListener
            }

            tampilkanDialogFilterTahun()
        }
    }

    private fun tampilkanDialogFilterTahun() {
        val tahunList = semuaBerita
            .mapNotNull { ambilTahun(it.rl_date) }
            .distinct()
            .sortedDescending()
            .toMutableList()

        tahunList.add(0, "Semua Tahun")

        val pilihan = if (tahunTerpilih == null) {
            0
        } else {
            tahunList.indexOf(tahunTerpilih).takeIf { it >= 0 } ?: 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Tahun")
            .setSingleChoiceItems(
                tahunList.toTypedArray(),
                pilihan
            ) { dialog, which ->

                val tahun = tahunList[which]

                tahunTerpilih =
                    if (tahun == "Semua Tahun") null else tahun

                tampilkanBerita()

                dialog.dismiss()
            }
            .show()
    }

    private fun ambilTahun(tanggal: String?): String? {
        if (tanggal.isNullOrBlank()) return null

        val regex = Regex("\\b(19|20)\\d{2}\\b")

        return regex.find(tanggal)?.value
    }

    private fun ambilBeritaTerfilter(): List<NewsItem> {
        return semuaBerita
            .filter { item ->

                val cocokTahun =
                    tahunTerpilih == null ||
                            ambilTahun(item.rl_date) == tahunTerpilih

                val judul = item.title?.lowercase() ?: ""

                val isi = Html.fromHtml(
                    item.news ?: "",
                    Html.FROM_HTML_MODE_LEGACY
                ).toString().lowercase()

                val cocokPencarian =
                    keywordPencarian.isBlank() ||
                            judul.contains(keywordPencarian) ||
                            isi.contains(keywordPencarian)

                cocokTahun && cocokPencarian
            }
            .sortedByDescending { it.rl_date ?: "" }
    }

    private fun tampilkanBerita() {
        if (!isAdded || sedangLoad || !::beritaContainer.isInitialized) {
            return
        }

        val currentView = view ?: return

        currentView.post {
            if (!isAdded || view == null || sedangLoad) {
                return@post
            }

            beritaContainer.removeAllViews()

            beritaContainer.addView(loadingView)
            loadingView.visibility = View.GONE

            val berita = ambilBeritaTerfilter()

            tvJumlahBerita.text = "${berita.size} berita"

            var row: LinearLayout? = null

            berita.forEachIndexed { index, item ->

                if (index % 2 == 0) {
                    row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL

                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            if (index > 0) {
                                topMargin = dpToPx(14)
                            }
                        }
                    }

                    beritaContainer.addView(row)
                }

                val card = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_home,
                    row,
                    false
                )

                card.layoutParams = LinearLayout.LayoutParams(
                    0,
                    dpToPx(210)
                ).apply {
                    weight = 1f

                    if (index % 2 == 0) {
                        marginEnd = dpToPx(6)
                    } else {
                        marginStart = dpToPx(6)
                    }
                }

                val image = card.findViewById<ImageView>(R.id.imgHome)

                val progressImage = card.findViewById<android.widget.ProgressBar>(
                    R.id.progressImage
                )

                val title = card.findViewById<TextView>(R.id.tvHomeTitle)

                val description = card.findViewById<TextView>(
                    R.id.tvHomeDescription
                )

                title.text = item.title ?: "Berita"

                description.text = Html.fromHtml(
                    item.news ?: "",
                    Html.FROM_HTML_MODE_LEGACY
                ).toString().trim()

                progressImage.visibility = View.VISIBLE
                image.visibility = View.INVISIBLE

                Glide.with(this@BeritaFragment)
                    .load(item.picture)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .placeholder(R.drawable.ic_bpslogo)
                    .error(R.drawable.ic_bpslogo)
                    .into(object : com.bumptech.glide.request.target.CustomTarget<Drawable>() {

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                        ) {
                            image.setImageDrawable(resource)
                            image.visibility = View.VISIBLE
                            progressImage.visibility = View.GONE
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            image.setImageDrawable(placeholder)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            image.setImageDrawable(errorDrawable)
                            image.visibility = View.VISIBLE
                            progressImage.visibility = View.GONE
                        }
                    })
                card.setOnClickListener {
                    tampilkanDetailBerita(item)
                }

                row?.addView(card)
            }

            if (berita.size % 2 != 0 && berita.isNotEmpty()) {
                val emptyView = View(requireContext())

                emptyView.layoutParams = LinearLayout.LayoutParams(
                    0,
                    dpToPx(210)
                ).apply {
                    weight = 1f
                    marginStart = dpToPx(6)
                }

                row?.addView(emptyView)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun LinearLayout.childrenExceptLoading() {
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)

            if (child !== loadingView) {
                removeViewAt(i)
            }
        }
    }

    private fun tampilkanDetailBerita(item: NewsItem) {
        if (!isAdded) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_detail_home,
            null
        )

        val image = dialogView.findViewById<ImageView>(R.id.imgDetailHome)

        val title = dialogView.findViewById<TextView>(
            R.id.tvDetailHomeTitle
        )

        val date = dialogView.findViewById<TextView>(
            R.id.tvDetailHomeDate
        )

        val description = dialogView.findViewById<TextView>(
            R.id.tvDetailHomeDescription
        )

        title.text = item.title ?: "Berita"
        date.text = item.rl_date ?: "-"
        description.text = "Memuat berita..."

        Glide.with(requireContext())
            .load(item.picture)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_bpslogo)
            .error(R.drawable.ic_bpslogo)
            .into(image)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        repository.getNewsDetail(
            domain = "3574",
            newsId = item.news_id.toString(),
            apiKey = API_KEY
        ) { response, error ->

            if (!isAdded) return@getNewsDetail

            activity?.runOnUiThread {

                if (!isAdded || !dialog.isShowing) {
                    return@runOnUiThread
                }

                if (error != null || response == null) {
                    description.text = "Gagal memuat isi berita."
                    return@runOnUiThread
                }

                try {
                    val data = response.getAsJsonObject("data")

                    if (data == null) {
                        description.text = "Isi berita tidak tersedia."
                        return@runOnUiThread
                    }

                    val newsLengkap = data.get("news")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString ?: ""

                    val tanggal = data.get("rl_date")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString

                    val judul = data.get("title")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString

                    title.text = judul ?: item.title ?: "Berita"
                    date.text = tanggal ?: item.rl_date ?: "-"

                    var newsDiformat = newsLengkap
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")

                    newsDiformat = newsDiformat
                        .replace("<br></br>", "<br>")
                        .replace("<br/>", "<br>")
                        .replace("<br />", "<br>")
                        .replace("<br>", "<br><br>")
                        .replace("<br><br><br><br>", "<br><br>")
                        .replace("<br><br><br>", "<br><br>")

                    description.text = Html.fromHtml(
                        newsDiformat,
                        Html.FROM_HTML_MODE_LEGACY
                    )

                } catch (e: Exception) {
                    description.text = "Gagal memproses isi berita."
                }
            }
        }
    }

    override fun onDestroyView() {
        if (::loadingView.isInitialized) {
            loadingView.cancelAnimation()
        }

        beritaScrollView = null
        sedangRefresh = false

        super.onDestroyView()
    }
}