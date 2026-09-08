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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.bpskota.bps.model.NewsItem
import com.example.bpskota.bps.repository.BpsRepository
import com.google.gson.Gson
import com.google.gson.JsonArray

class BeritaFragment : Fragment() {

    private lateinit var repository: BpsRepository

    private val API_KEY =
        "008edaaae5d450b1913b31a2cef618c3"

    private val semuaBerita =
        mutableListOf<NewsItem>()

    private var tahunTerpilih: String? = null
    private var keywordPencarian = ""

    private var totalPage = 1
    private var jumlahPageSelesai = 0
    private var sedangLoad = false

    private lateinit var beritaContainer: LinearLayout
    private lateinit var tvJumlahBerita: TextView
    private lateinit var etSearchBerita: EditText
    private lateinit var btnFilterBerita: View

    private lateinit var loadingView: LottieAnimationView

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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        repository = BpsRepository()

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

        setupLoading()

        setupSearch()

        setupFilter()

        loadSemuaBerita()
    }

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
                gravity = Gravity.CENTER
                topMargin = dpToPx(40)
                bottomMargin = dpToPx(40)
            }

        loadingView.setAnimation(
            "Loading_Animation.json"
        )

        loadingView.repeatCount = -1

        loadingView.visibility =
            View.GONE

        beritaContainer.addView(
            loadingView
        )
    }

    private fun tampilkanLoading() {

        loadingView.visibility =
            View.VISIBLE

        loadingView.playAnimation()

        tvJumlahBerita.text =
            "Memuat berita..."

        beritaContainer.childrenExceptLoading()
    }

    private fun sembunyikanLoading() {

        loadingView.cancelAnimation()

        loadingView.visibility =
            View.GONE
    }

    private fun loadSemuaBerita() {

        semuaBerita.clear()

        tahunTerpilih = null
        keywordPencarian = ""

        totalPage = 1
        jumlahPageSelesai = 0
        sedangLoad = true

        etSearchBerita.setText("")

        tampilkanLoading()

        Log.d(
            "BERITA_FRAGMENT",
            "========================================"
        )

        Log.d(
            "BERITA_FRAGMENT",
            "BeritaFragment dibuka"
        )

        Log.d(
            "BERITA_FRAGMENT",
            "Mulai load berita"
        )

        loadPagePertama()
    }

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

                if (error != null) {

                    sedangLoad = false

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Gagal mengambil page 1",
                        error
                    )

                    sembunyikanLoading()

                    tvJumlahBerita.text =
                        "Gagal memuat berita"

                    return@runOnUiThread
                }

                if (response == null) {

                    sedangLoad = false

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Response page 1 null"
                    )

                    sembunyikanLoading()

                    tvJumlahBerita.text =
                        "Gagal memuat berita"

                    return@runOnUiThread
                }

                try {

                    val dataArray =
                        response.getAsJsonArray(
                            "data"
                        )

                    if (
                        dataArray == null ||
                        dataArray.size() < 2
                    ) {

                        sedangLoad = false

                        Log.e(
                            "BERITA_FRAGMENT",
                            "Data page 1 tidak tersedia"
                        )

                        sembunyikanLoading()

                        tvJumlahBerita.text =
                            "Tidak ada berita"

                        return@runOnUiThread
                    }

                    val newsArray =
                        dataArray[1].asJsonArray

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

                    if (totalPage <= 1) {

                        sedangLoad = false

                        Log.d(
                            "BERITA_FRAGMENT",
                            "Hanya terdapat 1 page"
                        )

                        selesaiMemuatSemuaBerita()

                        return@runOnUiThread
                    }

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

                    sedangLoad = false

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Error memproses page 1",
                        e
                    )

                    sembunyikanLoading()

                    tvJumlahBerita.text =
                        "Gagal memproses berita"
                }
            }
        }
    }

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

                if (response == null) {

                    Log.e(
                        "BERITA_FRAGMENT",
                        "Response page $page null"
                    )

                    jumlahPageSelesai++

                    cekSelesaiSemuaPage()

                    return@runOnUiThread
                }

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
                        dataArray[1].asJsonArray

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

    private fun ambilTotalPage(
        dataArray: JsonArray
    ): Int {

        try {

            val metadata =
                dataArray[0]

            if (metadata.isJsonObject) {

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

                        if (total > 0) {
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

    private fun cekSelesaiSemuaPage() {

        if (
            jumlahPageSelesai >= totalPage
        ) {

            sedangLoad = false

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

    private fun selesaiMemuatSemuaBerita() {

        if (!isAdded) {
            return
        }

        sedangLoad = false

        sembunyikanLoading()

        tampilkanBerita()

        Log.d(
            "BERITA_FRAGMENT",
            "Berita siap ditampilkan"
        )
    }

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

                    if (!sedangLoad) {
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

    private fun setupFilter() {

        btnFilterBerita.setOnClickListener {

            if (sedangLoad) {
                return@setOnClickListener
            }

            tampilkanDialogFilterTahun()
        }
    }

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
            if (tahunTerpilih == null) {

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

    private fun ambilTahun(
        tanggal: String?
    ): String? {

        if (tanggal.isNullOrBlank()) {
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

    private fun tampilkanBerita() {

        if (!isAdded) {
            return
        }

        if (sedangLoad) {
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

            berita.forEachIndexed { index, item ->

                if (index % 2 == 0) {

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

                            if (index > 0) {
                                topMargin =
                                    dpToPx(14)
                            }
                        }

                    beritaContainer.addView(
                        row
                    )
                }

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

                        weight = 1f

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

                val image =
                    card.findViewById<ImageView>(
                        R.id.imgHome
                    )

                val title =
                    card.findViewById<TextView>(
                        R.id.tvHomeTitle
                    )

                val description =
                    card.findViewById<TextView>(
                        R.id.tvHomeDescription
                    )

                title.text =
                    item.title
                        ?: "Berita"

                description.text =
                    Html.fromHtml(
                        item.news ?: "",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                        .toString()
                        .trim()

                Glide.with(this@BeritaFragment)
                    .load(item.picture)
                    .diskCacheStrategy(
                        com.bumptech.glide.load.engine.DiskCacheStrategy.ALL
                    )
                    .thumbnail(0.25f)
                    .placeholder(R.drawable.ic_bpslogo)
                    .error(R.drawable.ic_bpslogo)
                    .into(image)
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

                        weight = 1f

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

    private fun LinearLayout.childrenExceptLoading() {

        for (
        i in childCount - 1 downTo 0
        ) {

            val child =
                getChildAt(i)

            if (child !== loadingView) {
                removeViewAt(i)
            }
        }
    }

    override fun onDestroyView() {

        Log.d(
            "BERITA_FRAGMENT",
            "BeritaFragment ditutup"
        )

        if (::loadingView.isInitialized) {
            loadingView.cancelAnimation()
        }

        semuaBerita.clear()

        sedangLoad = false
        totalPage = 1
        jumlahPageSelesai = 0

        super.onDestroyView()
    }
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

        title.text =
            item.title ?: "Berita"

        date.text =
            item.rl_date ?: "-"

        description.text =
            "Memuat berita..."

        Glide.with(
            requireContext()
        )
            .load(item.picture)
            .diskCacheStrategy(
                com.bumptech.glide.load.engine.DiskCacheStrategy.ALL
            )
            .thumbnail(0.25f)
            .placeholder(
                R.drawable.ic_bpslogo
            )
            .error(
                R.drawable.ic_bpslogo
            )
            .into(image)

        val dialog =
            AlertDialog.Builder(
                requireContext()
            )
                .setView(dialogView)
                .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

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

                if (dialog.isShowing.not()) {
                    return@runOnUiThread
                }

                if (error != null) {

                    Log.e(
                        "DETAIL_BERITA",
                        "Gagal mengambil detail",
                        error
                    )

                    description.text =
                        "Gagal memuat isi berita."

                    return@runOnUiThread
                }

                if (response == null) {

                    description.text =
                        "Isi berita tidak tersedia."

                    return@runOnUiThread
                }

                try {

                    val data =
                        response.getAsJsonObject(
                            "data"
                        )

                    if (data == null) {

                        description.text =
                            "Isi berita tidak tersedia."

                        return@runOnUiThread
                    }

                    val newsLengkap =
                        data.get("news")
                            ?.takeIf {
                                !it.isJsonNull
                            }
                            ?.asString
                            ?: ""

                    val tanggal =
                        data.get("rl_date")
                            ?.takeIf {
                                !it.isJsonNull
                            }
                            ?.asString

                    val judul =
                        data.get("title")
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

                    title.text =
                        judul ?: item.title ?: "Berita"

                    date.text =
                        tanggal ?: item.rl_date ?: "-"

                    var newsDiformat = newsLengkap
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")

// 2. Seragamkan semua variasi tag baris baru menjadi format standar (<br>)
                    newsDiformat = newsDiformat
                        .replace("<br></br>", "<br>")
                        .replace("<br/>", "<br>")
                        .replace("<br />", "<br>")

// 3. Jadikan paragraf dengan menggandakan <br>
                    newsDiformat = newsDiformat.replace("<br>", "<br><br>")

// 4. (Opsional) Rapikan agar spasi tidak terlalu lebar jika awalnya sudah ada banyak break berderet
                    newsDiformat = newsDiformat
                        .replace("<br><br><br><br>", "<br><br>")
                        .replace("<br><br><br>", "<br><br>")

// 5. Masukkan ke dalam TextView
                    description.text = Html.fromHtml(
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
}