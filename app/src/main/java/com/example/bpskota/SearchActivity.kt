package com.example.bpskota

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SearchActivity : AppCompatActivity() {

    private lateinit var btnBackSearch: ImageView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageView

    private lateinit var tvSearchTitle: TextView
    private lateinit var progressSearch: ProgressBar
    private lateinit var searchResultContainer: LinearLayout
    private lateinit var tvSearchEmpty: TextView


    // =========================================================
    // MODEL SEARCH
    // =========================================================

    private data class SearchFeature(
        val title: String,
        val description: String,
        val category: String,
        val keywords: List<String>,
        val action: () -> Unit
    )


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)


        // =====================================================
        // FIND VIEW
        // =====================================================

        btnBackSearch = findViewById(
            R.id.btnBackSearch
        )

        etSearch = findViewById(
            R.id.etSearch
        )

        btnSearch = findViewById(
            R.id.btnSearch
        )

        tvSearchTitle = findViewById(
            R.id.tvSearchTitle
        )

        progressSearch = findViewById(
            R.id.progressSearch
        )

        searchResultContainer = findViewById(
            R.id.searchResultContainer
        )

        tvSearchEmpty = findViewById(
            R.id.tvSearchEmpty
        )


        // =====================================================
        // AMBIL KEYWORD DARI HOME
        // =====================================================

        val keyword = intent
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


        // =====================================================
        // BUTTON BACK
        // =====================================================

        btnBackSearch.setOnClickListener {

            finish()
        }


        // =====================================================
        // BUTTON SEARCH
        // =====================================================

        btnSearch.setOnClickListener {

            executeSearch()
        }


        // =====================================================
        // KEYBOARD SEARCH / ENTER
        // =====================================================

        etSearch.setOnEditorActionListener {
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
    // EXECUTE SEARCH
    // =========================================================

    private fun executeSearch() {

        val keyword = etSearch
            .text
            .toString()
            .trim()


        // =====================================================
        // VALIDASI
        // =====================================================

        if (keyword.isEmpty()) {

            etSearch.error =
                "Masukkan kata pencarian"

            etSearch.requestFocus()

            return
        }


        // =====================================================
        // HIDE KEYBOARD
        // =====================================================

        hideKeyboard()


        // =====================================================
        // JUDUL
        // =====================================================

        tvSearchTitle.text =
            "Hasil pencarian untuk \"$keyword\""


        // =====================================================
        // RESET HASIL
        // =====================================================

        searchResultContainer.removeAllViews()

        searchResultContainer.addView(
            tvSearchEmpty
        )

        tvSearchEmpty.visibility =
            View.GONE


        // =====================================================
        // LOADING
        // =====================================================

        progressSearch.visibility =
            View.VISIBLE


        // =====================================================
        // PENCARIAN LOKAL
        //
        // Sementara belum menggunakan API.
        // =====================================================

        progressSearch.postDelayed({

            progressSearch.visibility =
                View.GONE

            tampilkanHasilPencarian(
                keyword
            )

        }, 300)
    }


    // =========================================================
    // TAMPILKAN HASIL
    // =========================================================

    private fun tampilkanHasilPencarian(
        keyword: String
    ) {

        val keywordNormal = keyword
            .lowercase()
            .trim()


        // =====================================================
        // FILTER
        // =====================================================

        val hasil = getSearchFeatures()
            .filter { feature ->

                feature.keywords.any { kata ->

                    val kataNormal = kata
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


        // =====================================================
        // RESET CONTAINER
        // =====================================================

        searchResultContainer.removeAllViews()


        // =====================================================
        // TIDAK ADA HASIL
        // =====================================================

        if (hasil.isEmpty()) {

            searchResultContainer.addView(
                tvSearchEmpty
            )

            tvSearchEmpty.visibility =
                View.VISIBLE

            tvSearchEmpty.text =
                "Belum ada hasil pencarian untuk \"$keyword\""

            return
        }


        // =====================================================
        // ADA HASIL
        // =====================================================

        tvSearchEmpty.visibility =
            View.GONE


        hasil.forEach { feature ->

            val card = buatSearchResultCard(
                feature
            )

            searchResultContainer.addView(
                card
            )
        }
    }


    // =========================================================
    // DAFTAR FITUR SEARCH
    // =========================================================

    private fun getSearchFeatures():
            List<SearchFeature> {

        return listOf(

            // =================================================
            // DATA
            // =================================================

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


            // =================================================
            // INFORMASI
            // =================================================

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

                    bukaMenuPager(
                        2
                    )
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

                    bukaMenuPager(
                        1
                    )
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

                    bukaMenuPager(
                        3
                    )
                }
            ),


            // =================================================
            // LAINNYA
            // =================================================

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

                    bukaMenuPager(
                        4
                    )
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

                    bukaMenuPager(
                        5
                    )
                }
            )
        )
    }


    // =========================================================
    // CARD SEARCH
    //
    // DESAIN MENGIKUTI card_statistik
    //
    // card_statistik.xml TIDAK DIUBAH.
    // =========================================================

    private fun buatSearchResultCard(
        feature: SearchFeature
    ): View {

        // =====================================================
        // CARD VIEW
        // =====================================================

        val card =
            CardView(this)


        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        cardParams.bottomMargin =
            14.dp()


        card.layoutParams =
            cardParams


        card.setCardBackgroundColor(
            Color.WHITE
        )


        card.radius =
            18.dp().toFloat()


        card.cardElevation =
            2.dp().toFloat()


        // =====================================================
        // ROOT
        // =====================================================

        val root =
            LinearLayout(this)


        root.orientation =
            LinearLayout.VERTICAL


        root.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        // =====================================================
        // GARIS ORANGE
        // =====================================================

        val garisOrange =
            View(this)


        garisOrange.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                5.dp()
            )


        garisOrange.setBackgroundColor(
            Color.parseColor(
                "#F97316"
            )
        )


        root.addView(
            garisOrange
        )


        // =====================================================
        // CONTENT
        // =====================================================

        val content =
            LinearLayout(this)


        content.orientation =
            LinearLayout.VERTICAL


        content.setPadding(
            16.dp(),
            16.dp(),
            16.dp(),
            16.dp()
        )


        content.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        // =====================================================
        // TITLE ROW
        // =====================================================

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


        // =====================================================
        // ICON SEARCH
        // =====================================================

        val icon =
            ImageView(this)


        icon.layoutParams =
            LinearLayout.LayoutParams(
                22.dp(),
                22.dp()
            ).apply {

                rightMargin =
                    10.dp()
            }


        icon.setImageResource(
            R.drawable.ic_search
        )


        icon.scaleType =
            ImageView.ScaleType.CENTER_INSIDE


        icon.contentDescription =
            "Pencarian"


        titleRow.addView(
            icon
        )


        // =====================================================
        // TITLE
        // =====================================================

        val title =
            TextView(this)


        title.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )


        title.text =
            feature.title


        title.setTextColor(
            Color.parseColor(
                "#111827"
            )
        )


        title.textSize =
            18f


        title.setTypeface(
            null,
            Typeface.BOLD
        )


        title.maxLines =
            2


        title.ellipsize =
            TextUtils.TruncateAt.END


        titleRow.addView(
            title
        )


        content.addView(
            titleRow
        )


        // =====================================================
        // DIVIDER
        // =====================================================

        val divider =
            View(this)


        divider.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dp()
            ).apply {

                topMargin =
                    12.dp()

                bottomMargin =
                    12.dp()
            }


        divider.setBackgroundColor(
            Color.parseColor(
                "#E5E7EB"
            )
        )


        content.addView(
            divider
        )


        // =====================================================
        // CATEGORY
        // =====================================================

        val category =
            TextView(this)


        category.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        category.text =
            feature.category.uppercase()


        category.setTextColor(
            Color.parseColor(
                "#F97316"
            )
        )


        category.textSize =
            11f


        category.setTypeface(
            null,
            Typeface.BOLD
        )


        content.addView(
            category
        )


        // =====================================================
        // DESCRIPTION
        // =====================================================

        val description =
            TextView(this)


        description.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    5.dp()
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


        content.addView(
            description
        )


        // =====================================================
        // CONTENT → ROOT
        // =====================================================

        root.addView(
            content
        )


        // =====================================================
        // ROOT → CARD
        // =====================================================

        card.addView(
            root
        )


        // =====================================================
        // CLICK
        // =====================================================

        card.setOnClickListener {

            feature.action()
        }


        return card
    }


    // =========================================================
    // BUKA MENU PAGER
    //
    // 0 = HomeFragment
    // 1 = InfografikFragment
    // 2 = BeritaFragment
    // 3 = PublikasiFragment
    // 4 = DataFragment
    // 5 = MoreFragment
    // =========================================================

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


        startActivity(
            intent
        )


        finish()
    }


    // =========================================================
    // SHOW KEYBOARD
    // =========================================================

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


    // =========================================================
    // HIDE KEYBOARD
    // =========================================================

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


    // =========================================================
    // DP HELPER
    // =========================================================

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }


    // =========================================================
    // BACK
    // =========================================================

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        finish()
    }
}