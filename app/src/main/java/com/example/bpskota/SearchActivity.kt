package com.example.bpskota

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.bpskota.bps.model.AllSimdasiResponse
import com.example.bpskota.bps.model.AllStaticTableResponse
import com.example.bpskota.bps.model.AllVariableResponse
import com.example.bpskota.bps.repository.BpsAllDataRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import com.google.gson.JsonObject

class SearchActivity : AppCompatActivity() {

    private lateinit var btnBackSearch: ImageView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageView
    private lateinit var tvSearchTitle: TextView
    private lateinit var progressSearch: ProgressBar
    private lateinit var searchResultContainer: LinearLayout
    private lateinit var tvSearchEmpty: TextView

    private lateinit var activityTracker: ActivityTracker

    private val repository =
        BpsAllDataRepository()

    private data class SearchFeature(
        val title: String,
        val description: String,
        val category: String,
        val keywords: List<String>,
        val action: () -> Unit
    )

    private data class KategoriItem(
        val nama: String,
        val sumber: String
    )

    private val daftarKategori =
        mutableListOf<KategoriItem>()

    private val kategoriSudahAda =
        mutableSetOf<String>()

    private var simdasiSelesai = false
    private var staticSelesai = false
    private var variableSelesai = false

    companion object {
        private const val WILAYAH = "3574000"
        private const val DOMAIN = "3574"
        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_search
        )

        btnBackSearch =
            findViewById(R.id.btnBackSearch)

        etSearch =
            findViewById(R.id.etSearch)

        btnSearch =
            findViewById(R.id.btnSearch)

        tvSearchTitle =
            findViewById(R.id.tvSearchTitle)

        progressSearch =
            findViewById(R.id.progressSearch)

        searchResultContainer =
            findViewById(R.id.searchResultContainer)

        tvSearchEmpty =
            findViewById(R.id.tvSearchEmpty)

        activityTracker =
            ActivityTracker(
                this,
                BpskpRetrofitClient.api
            )

        activityTracker.trackScreen(
            screen = "Search"
        )

        btnBackSearch.setOnClickListener {
            animateClick(it) {
                finish()
            }
        }

        btnSearch.setOnClickListener {
            animateClick(it) {
                executeSearch()
            }
        }

        val keyword =
            intent
                .getStringExtra("keyword")
                ?.trim()
                ?: ""

        if (keyword.isNotEmpty()) {

            etSearch.setText(keyword)

            etSearch.setSelection(
                etSearch.text.length
            )

            executeSearch()

        } else {

            etSearch.requestFocus()

            etSearch.post {
                showKeyboard()
            }
        }

        etSearch.setOnEditorActionListener {
                _,
                actionId,
                event ->

            if (
                actionId ==
                EditorInfo.IME_ACTION_SEARCH ||
                (
                        event != null &&
                                event.keyCode ==
                                KeyEvent.KEYCODE_ENTER &&
                                event.action ==
                                KeyEvent.ACTION_DOWN
                        )
            ) {

                executeSearch()

                true

            } else {

                false
            }
        }
    }

    private fun executeSearch() {

        val keyword =
            etSearch
                .text
                .toString()
                .trim()

        if (keyword.isEmpty()) {

            etSearch.error =
                "Masukkan kata pencarian"

            etSearch.requestFocus()

            return
        }

        hideKeyboard()

        tvSearchTitle.text =
            "Hasil pencarian untuk \"$keyword\""

        tvSearchTitle.alpha = 0f

        tvSearchTitle.animate()
            .alpha(1f)
            .setDuration(220)
            .start()

        searchResultContainer.removeAllViews()

        tvSearchEmpty.visibility =
            View.GONE

        progressSearch.visibility =
            View.VISIBLE

        daftarKategori.clear()
        kategoriSudahAda.clear()

        simdasiSelesai = false
        staticSelesai = false
        variableSelesai = false

        loadApiSearch(keyword)
    }

    private fun loadApiSearch(
        keyword: String
    ) {

        searchResultContainer.removeAllViews()

        loadSimdasiPage(1)
        loadStaticTablePage(1)
        loadVariablePage(1)
    }

    private fun loadSimdasiPage(
        page: Int
    ) {

        repository.getAllSimdasi(
            wilayah = WILAYAH,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (
                    error != null ||
                    response == null
                ) {

                    simdasiSelesai = true
                    cekApiSelesai()

                    return@runOnUiThread
                }

                prosesSimdasi(response)

                val totalPages =
                    getSimdasiTotalPages(
                        response
                    )

                if (
                    page < totalPages
                ) {

                    loadSimdasiPage(
                        page + 1
                    )

                } else {

                    simdasiSelesai = true
                    cekApiSelesai()
                }
            }
        }
    }

    private fun prosesSimdasi(
        response: AllSimdasiResponse
    ) {

        try {

            val rootData =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return

            if (
                rootData.size() < 2
            ) {
                return
            }

            val wrapper =
                rootData[1]
                    .takeIf {
                        it.isJsonObject
                    }
                    ?.asJsonObject
                    ?: return

            val data =
                wrapper
                    .get("data")
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return

            for (element in data) {

                if (
                    !element.isJsonObject
                ) {
                    continue
                }

                val obj =
                    element.asJsonObject

                val nama =
                    getString(
                        obj,
                        "mms_subject"
                    )

                if (
                    nama.isBlank()
                ) {
                    continue
                }

                tambahKategori(
                    nama,
                    "SIMDASI"
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun getSimdasiTotalPages(
        response: AllSimdasiResponse
    ): Int {

        return try {

            val data =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return 1

            if (
                data.isEmpty()
            ) {
                return 1
            }

            data[0]
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.get("pages")
                ?.asInt
                ?: 1

        } catch (_: Exception) {

            1
        }
    }

    private fun loadStaticTablePage(
        page: Int
    ) {

        repository.getAllStaticTables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (
                    error != null ||
                    response == null
                ) {

                    staticSelesai = true
                    cekApiSelesai()

                    return@runOnUiThread
                }

                prosesStaticTable(response)

                val totalPages =
                    getStaticTotalPages(
                        response
                    )

                if (
                    page < totalPages
                ) {

                    loadStaticTablePage(
                        page + 1
                    )

                } else {

                    staticSelesai = true
                    cekApiSelesai()
                }
            }
        }
    }

    private fun prosesStaticTable(
        response: AllStaticTableResponse
    ) {

        try {

            val rootData =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return

            if (
                rootData.size() < 2
            ) {
                return
            }

            val data =
                rootData[1]
                    .takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return

            for (element in data) {

                if (
                    !element.isJsonObject
                ) {
                    continue
                }

                val obj =
                    element.asJsonObject

                val nama =
                    getString(
                        obj,
                        "subj"
                    )

                if (
                    nama.isBlank()
                ) {
                    continue
                }

                tambahKategori(
                    nama,
                    "STATIC"
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun getStaticTotalPages(
        response: AllStaticTableResponse
    ): Int {

        return try {

            val data =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return 1

            if (
                data.isEmpty()
            ) {
                return 1
            }

            data[0]
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.get("pages")
                ?.asInt
                ?: 1

        } catch (_: Exception) {

            1
        }
    }

    private fun loadVariablePage(
        page: Int
    ) {

        repository.getAllVariables(
            domain = DOMAIN,
            page = page,
            apiKey = API_KEY
        ) { response, error ->

            runOnUiThread {

                if (
                    error != null ||
                    response == null
                ) {

                    variableSelesai = true
                    cekApiSelesai()

                    return@runOnUiThread
                }

                prosesVariable(response)

                val totalPages =
                    getVariableTotalPages(
                        response
                    )

                if (
                    page < totalPages
                ) {

                    loadVariablePage(
                        page + 1
                    )

                } else {

                    variableSelesai = true
                    cekApiSelesai()
                }
            }
        }
    }

    private fun prosesVariable(
        response: AllVariableResponse
    ) {

        try {

            val rootData =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return

            if (
                rootData.size() < 2
            ) {
                return
            }

            val data =
                rootData[1]
                    .takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return

            for (element in data) {

                if (
                    !element.isJsonObject
                ) {
                    continue
                }

                val obj =
                    element.asJsonObject

                val nama =
                    getString(
                        obj,
                        "subcsa_name"
                    )

                if (
                    nama.isBlank()
                ) {
                    continue
                }

                tambahKategori(
                    nama,
                    "VARIABLE"
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun getVariableTotalPages(
        response: AllVariableResponse
    ): Int {

        return try {

            val data =
                response.data
                    ?.takeIf {
                        it.isJsonArray
                    }
                    ?.asJsonArray
                    ?: return 1

            if (
                data.isEmpty()
            ) {
                return 1
            }

            data[0]
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.get("pages")
                ?.asInt
                ?: 1

        } catch (_: Exception) {

            1
        }
    }

    private fun tambahKategori(
        nama: String,
        sumber: String
    ) {

        val namaBersih =
            nama
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )

        if (
            namaBersih.isBlank()
        ) {
            return
        }

        val key =
            "$sumber|${namaBersih.lowercase()}"

        if (
            !kategoriSudahAda.add(key)
        ) {
            return
        }

        daftarKategori.add(
            KategoriItem(
                namaBersih,
                sumber
            )
        )
    }

    private fun cekApiSelesai() {

        if (
            !semuaApiSelesai()
        ) {
            return
        }

        val keyword =
            etSearch
                .text
                .toString()
                .trim()

        progressSearch.visibility =
            View.GONE

        tampilkanSemuaHasil(
            keyword
        )
    }

    private fun semuaApiSelesai(): Boolean {

        return simdasiSelesai &&
                staticSelesai &&
                variableSelesai
    }

    private fun tampilkanSemuaHasil(
        keyword: String
    ) {

        val hasilFitur =
            cariFitur(keyword)

        val hasilApi =
            cariKategoriApi(keyword)

        searchResultContainer.removeAllViews()

        tvSearchEmpty.visibility =
            View.GONE

        var jumlahHasil = 0

        if (
            hasilFitur.isNotEmpty()
        ) {

            tambahHeader(
                "Fitur"
            )

            hasilFitur.forEachIndexed {
                    index,
                    feature ->

                val card =
                    buatSearchResultCard(
                        feature
                    )

                searchResultContainer.addView(
                    card
                )

                animasikanHasil(
                    card,
                    index
                )

                jumlahHasil++
            }
        }

        if (
            hasilApi.isNotEmpty()
        ) {

            tambahHeader(
                "Kategori Data BPS"
            )

            hasilApi.forEachIndexed {
                    index,
                    kategori ->

                val card =
                    buatKategoriApiCard(
                        kategori
                    )

                searchResultContainer.addView(
                    card
                )

                animasikanHasil(
                    card,
                    index
                )

                jumlahHasil++
            }
        }

        if (
            jumlahHasil == 0
        ) {

            searchResultContainer.addView(
                tvSearchEmpty
            )

            tvSearchEmpty.visibility =
                View.VISIBLE

            tvSearchEmpty.text =
                "Belum ada hasil pencarian untuk \"$keyword\""

            tvSearchEmpty.alpha = 0f

            tvSearchEmpty.translationY =
                10.dp().toFloat()

            tvSearchEmpty.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .start()

        } else {

            tvSearchEmpty.visibility =
                View.GONE
        }
    }

    private fun cariFitur(
        keyword: String
    ): List<SearchFeature> {

        val keywordNormal =
            keyword
                .lowercase()
                .trim()

        return getSearchFeatures()
            .filter { feature ->

                feature.keywords.any { kata ->

                    val kataNormal =
                        kata
                            .lowercase()
                            .trim()

                    kataNormal.contains(
                        keywordNormal
                    ) ||
                            keywordNormal.contains(
                                kataNormal
                            )
                }
            }
    }

    private fun cariKategoriApi(
        keyword: String
    ): List<KategoriItem> {

        val query =
            keyword
                .lowercase()
                .trim()

        return daftarKategori
            .filter { kategori ->

                kategori.nama
                    .lowercase()
                    .contains(query)
            }
            .sortedBy {
                it.nama
            }
    }

    private fun tambahHeader(
        text: String
    ) {

        val header =
            TextView(this)

        header.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    12.dp()

                bottomMargin =
                    10.dp()
            }

        header.text =
            text

        header.textSize =
            16f

        header.setTextColor(
            Color.parseColor(
                "#111827"
            )
        )

        header.setTypeface(
            null,
            Typeface.BOLD
        )

        header.includeFontPadding =
            false

        header.alpha = 0f

        header.translationY =
            8.dp().toFloat()

        searchResultContainer.addView(
            header
        )

        header.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .start()
    }

    private fun buatKategoriApiCard(
        kategori: KategoriItem
    ): View {

        val card =
            CardView(this)

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    12.dp()
            }

        card.setCardBackgroundColor(
            Color.WHITE
        )

        card.radius =
            18.dp().toFloat()

        card.cardElevation =
            1.5f.dp().toFloat()

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            16.dp(),
            15.dp(),
            16.dp(),
            15.dp()
        )

        val titleRow =
            LinearLayout(this)

        titleRow.orientation =
            LinearLayout.HORIZONTAL

        titleRow.gravity =
            Gravity.CENTER_VERTICAL

        titleRow.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        val iconContainer =
            LinearLayout(this)

        iconContainer.orientation =
            LinearLayout.VERTICAL

        iconContainer.gravity =
            Gravity.CENTER

        iconContainer.layoutParams =
            LinearLayout.LayoutParams(
                38.dp(),
                38.dp()
            )

        iconContainer.setBackgroundColor(
            Color.parseColor(
                "#FFF4EC"
            )
        )

        val icon =
            ImageView(this)

        icon.layoutParams =
            LinearLayout.LayoutParams(
                21.dp(),
                21.dp()
            )

        icon.setImageResource(
            R.drawable.ic_search
        )

        icon.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        icon.setColorFilter(
            Color.parseColor(
                "#F97316"
            )
        )

        icon.contentDescription =
            "Kategori data"

        iconContainer.addView(
            icon
        )

        titleRow.addView(
            iconContainer
        )

        val title =
            TextView(this)

        title.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {

                leftMargin =
                    12.dp()
            }

        title.text =
            kategori.nama

        title.textSize =
            15.5f

        title.setTextColor(
            Color.parseColor(
                "#111827"
            )
        )

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        title.maxLines =
            3

        title.ellipsize =
            TextUtils.TruncateAt.END

        title.includeFontPadding =
            false

        titleRow.addView(
            title
        )

        root.addView(
            titleRow
        )

        val sumberContainer =
            LinearLayout(this)

        sumberContainer.orientation =
            LinearLayout.HORIZONTAL

        sumberContainer.gravity =
            Gravity.CENTER_VERTICAL

        sumberContainer.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    11.dp()
            }

        val sumber =
            TextView(this)

        sumber.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                28.dp()
            )

        sumber.gravity =
            Gravity.CENTER

        sumber.setPadding(
            10.dp(),
            0,
            10.dp(),
            0
        )

        sumber.text =
            kategori.sumber

        sumber.textSize =
            10f

        sumber.setTextColor(
            Color.parseColor(
                "#F97316"
            )
        )

        sumber.setTypeface(
            null,
            Typeface.BOLD
        )

        sumber.setBackgroundColor(
            Color.parseColor(
                "#FFF4EC"
            )
        )

        sumberContainer.addView(
            sumber
        )

        root.addView(
            sumberContainer
        )

        card.addView(
            root
        )

        card.setOnClickListener {

            animateClick(it) {

                val intent =
                    Intent(
                        this,
                        DataKategoriActivity::class.java
                    )

                intent.putExtra(
                    "NAMA_KATEGORI",
                    kategori.nama
                )

                startActivity(
                    intent
                )
            }
        }

        return card
    }

    private fun buatSearchResultCard(
        feature: SearchFeature
    ): View {

        val card =
            CardView(this)

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.bottomMargin =
            12.dp()

        card.layoutParams =
            cardParams

        card.setCardBackgroundColor(
            Color.WHITE
        )

        card.radius =
            18.dp().toFloat()

        card.cardElevation =
            1.5f.dp().toFloat()

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        val garisOrange =
            View(this)

        garisOrange.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4.dp()
            )

        garisOrange.setBackgroundColor(
            Color.parseColor(
                "#F97316"
            )
        )

        root.addView(
            garisOrange
        )

        val content =
            LinearLayout(this)

        content.orientation =
            LinearLayout.VERTICAL

        content.setPadding(
            16.dp(),
            15.dp(),
            16.dp(),
            15.dp()
        )

        val titleRow =
            LinearLayout(this)

        titleRow.orientation =
            LinearLayout.HORIZONTAL

        titleRow.gravity =
            Gravity.CENTER_VERTICAL

        titleRow.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        val iconContainer =
            LinearLayout(this)

        iconContainer.orientation =
            LinearLayout.VERTICAL

        iconContainer.gravity =
            Gravity.CENTER

        iconContainer.layoutParams =
            LinearLayout.LayoutParams(
                38.dp(),
                38.dp()
            )

        iconContainer.setBackgroundColor(
            Color.parseColor(
                "#FFF4EC"
            )
        )

        val icon =
            ImageView(this)

        icon.layoutParams =
            LinearLayout.LayoutParams(
                21.dp(),
                21.dp()
            )

        icon.setImageResource(
            R.drawable.ic_search
        )

        icon.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        icon.setColorFilter(
            Color.parseColor(
                "#F97316"
            )
        )

        icon.contentDescription =
            "Fitur"

        iconContainer.addView(
            icon
        )

        titleRow.addView(
            iconContainer
        )

        val title =
            TextView(this)

        title.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {

                leftMargin =
                    12.dp()
            }

        title.text =
            feature.title

        title.setTextColor(
            Color.parseColor(
                "#111827"
            )
        )

        title.textSize =
            17f

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        title.maxLines =
            2

        title.ellipsize =
            TextUtils.TruncateAt.END

        title.includeFontPadding =
            false

        titleRow.addView(
            title
        )

        content.addView(
            titleRow
        )

        val divider =
            View(this)

        divider.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dp()
            ).apply {

                topMargin =
                    13.dp()

                bottomMargin =
                    11.dp()
            }

        divider.setBackgroundColor(
            Color.parseColor(
                "#E5E7EB"
            )
        )

        content.addView(
            divider
        )

        val categoryContainer =
            LinearLayout(this)

        categoryContainer.orientation =
            LinearLayout.HORIZONTAL

        categoryContainer.gravity =
            Gravity.CENTER_VERTICAL

        categoryContainer.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        val category =
            TextView(this)

        category.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                27.dp()
            )

        category.gravity =
            Gravity.CENTER

        category.setPadding(
            10.dp(),
            0,
            10.dp(),
            0
        )

        category.text =
            feature.category.uppercase()

        category.setTextColor(
            Color.parseColor(
                "#F97316"
            )
        )

        category.textSize =
            10f

        category.setTypeface(
            null,
            Typeface.BOLD
        )

        category.setBackgroundColor(
            Color.parseColor(
                "#FFF4EC"
            )
        )

        categoryContainer.addView(
            category
        )

        content.addView(
            categoryContainer
        )

        val description =
            TextView(this)

        description.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    7.dp()
            }

        description.text =
            feature.description

        description.setTextColor(
            Color.parseColor(
                "#6B7280"
            )
        )

        description.textSize =
            13f

        description.includeFontPadding =
            false

        content.addView(
            description
        )

        root.addView(
            content
        )

        card.addView(
            root
        )

        card.setOnClickListener {

            animateClick(it) {
                feature.action()
            }
        }

        return card
    }

    private fun animasikanHasil(
        view: View,
        position: Int
    ) {

        view.alpha = 0f

        view.translationY =
            18.dp().toFloat()

        val delay =
            (position * 35L)
                .coerceAtMost(280L)

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(280)
            .start()
    }

    private fun animateClick(
        view: View,
        action: () -> Unit
    ) {

        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(70)
            .withEndAction {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(90)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }

    private fun getSearchFeatures():
            List<SearchFeature> {

        return listOf(

            SearchFeature(
                title = "Gender",
                description =
                "Data statistik gender",
                category = "Data",
                keywords = listOf(
                    "gender",
                    "jenis kelamin",
                    "laki laki",
                    "laki-laki",
                    "perempuan"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            GenderActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Pertanian",
                description =
                "Data statistik pertanian",
                category = "Data",
                keywords = listOf(
                    "pertanian",
                    "petani",
                    "tanaman",
                    "perkebunan",
                    "perikanan",
                    "peternakan"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            PertanianActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Ekonomi",
                description =
                "Data statistik ekonomi",
                category = "Data",
                keywords = listOf(
                    "ekonomi",
                    "perekonomian",
                    "perdagangan",
                    "usaha",
                    "bisnis"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            EkonomiActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Kependudukan",
                description =
                "Data statistik kependudukan",
                category = "Data",
                keywords = listOf(
                    "kependudukan",
                    "penduduk",
                    "populasi",
                    "demografi"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            KependudukanActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Tenaga Kerja",
                description =
                "Data statistik tenaga kerja",
                category = "Data",
                keywords = listOf(
                    "tenaga kerja",
                    "tenagakerja",
                    "pekerjaan",
                    "pekerja",
                    "ketenagakerjaan",
                    "buruh"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            TenagakerjaActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Konsumsi dan Pendapatan",
                description =
                "Data konsumsi dan pendapatan",
                category = "Data",
                keywords = listOf(
                    "konsumsi",
                    "pendapatan",
                    "pengeluaran",
                    "penghasilan"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            PendapatanActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Kondisi Tempat Tinggal",
                description =
                "Data kondisi tempat tinggal",
                category = "Data",
                keywords = listOf(
                    "tempat tinggal",
                    "rumah",
                    "perumahan",
                    "hunian",
                    "kondisi tempat tinggal"
                ),
                action = {
                    startActivity(
                        Intent(
                            this,
                            TempattinggalActivity::class.java
                        )
                    )
                }
            ),

            SearchFeature(
                title = "Berita",
                description =
                "Berita terbaru BPS",
                category = "Informasi",
                keywords = listOf(
                    "berita",
                    "kabar",
                    "informasi berita"
                ),
                action = {
                    bukaMenuPager(2)
                }
            ),

            SearchFeature(
                title = "Infografis",
                description =
                "Kumpulan infografis BPS",
                category = "Informasi",
                keywords = listOf(
                    "infografis",
                    "infografik",
                    "grafis",
                    "gambar statistik"
                ),
                action = {
                    bukaMenuPager(1)
                }
            ),

            SearchFeature(
                title = "Publikasi",
                description =
                "Publikasi statistik BPS",
                category = "Informasi",
                keywords = listOf(
                    "publikasi",
                    "buku",
                    "dokumen",
                    "laporan"
                ),
                action = {
                    bukaMenuPager(3)
                }
            ),

            SearchFeature(
                title = "Data",
                description =
                "Jelajahi data statistik",
                category = "Lainnya",
                keywords = listOf(
                    "data",
                    "statistik"
                ),
                action = {
                    bukaMenuPager(4)
                }
            ),

            SearchFeature(
                title = "Lainnya",
                description =
                "Informasi dan fitur lainnya",
                category = "Lainnya",
                keywords = listOf(
                    "lainnya",
                    "menu lainnya"
                ),
                action = {
                    bukaMenuPager(5)
                }
            )
        )
    }

    private fun bukaMenuPager(
        position: Int
    ) {

        val intent =
            Intent(
                this,
                HomeActivity::class.java
            )

        intent.putExtra(
            "page",
            position
        )

        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )

        startActivity(intent)

        finish()
    }

    private fun getString(
        obj: JsonObject,
        key: String
    ): String {

        return try {

            obj.get(key)
                ?.takeIf {
                    !it.isJsonNull
                }
                ?.asString
                ?.trim()
                ?: ""

        } catch (_: Exception) {

            ""
        }
    }

    private fun showKeyboard() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.showSoftInput(
            etSearch,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    private fun hideKeyboard() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            etSearch.windowToken,
            0
        )

        etSearch.clearFocus()
    }

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }

    private fun Float.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        finish()
    }
}