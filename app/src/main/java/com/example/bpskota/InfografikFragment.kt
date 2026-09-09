package com.example.bpskota

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.bpskota.bps.model.Infografik
import com.example.bpskota.bps.repository.BpsRepository
import java.util.Locale

class InfografikFragment : Fragment(), RefreshableFragment {

    private lateinit var repository: BpsRepository

    private lateinit var container: LinearLayout
    private lateinit var search: EditText
    private lateinit var jumlah: TextView

    private var semuaInfografik =
        mutableListOf<Infografik>()

    private var tahunTerpilih: Int? = null

    /*
     * Scroll container yang digunakan
     * oleh fragment ini.
     */
    private var verticalScrollView: View? = null

    /*
     * Menandai apakah proses refresh sedang berjalan.
     */
    private var sedangRefresh = false

    private val API_KEY =
        "008edaaae5d450b1913b31a2cef618c3"


    // =========================================================
    // CREATE VIEW
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_infografik,
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

        repository =
            BpsRepository()


        container =
            view.findViewById(
                R.id.infografikContainer
            )


        search =
            view.findViewById(
                R.id.etSearchInfografis
            )


        jumlah =
            view.findViewById(
                R.id.tvJumlahInfografis
            )


        val btnFilter =
            view.findViewById<View>(
                R.id.btnFilter
            )


        // =====================================================
        // SETUP SCROLL
        // =====================================================

        verticalScrollView =
            findVerticalScrollContainer(
                view
            )

        setupSwipeRefreshState()


        // =====================================================
        // SEARCH
        // =====================================================

        search.addTextChangedListener(
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

                    tampilkanHasil()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )


        // =====================================================
        // FILTER
        // =====================================================

        btnFilter.setOnClickListener {

            tampilkanDialogTahun()
        }


        // =====================================================
        // LOAD DATA AWAL
        // =====================================================

        loadInfografik()
    }


    // =========================================================
    // CARI SCROLL CONTAINER
    // =========================================================

    private fun findVerticalScrollContainer(
        root: View
    ): View? {

        /*
         * ScrollView biasa.
         */
        if (root is ScrollView) {
            return root
        }


        /*
         * NestedScrollView.
         */
        if (root is NestedScrollView) {
            return root
        }


        /*
         * RecyclerView.
         */
        if (root is RecyclerView) {
            return root
        }


        /*
         * Cari secara rekursif ke seluruh
         * child layout.
         */
        if (root is ViewGroup) {

            for (i in 0 until root.childCount) {

                val child =
                    root.getChildAt(i)

                val result =
                    findVerticalScrollContainer(
                        child
                    )

                if (result != null) {
                    return result
                }
            }
        }


        return null
    }


    // =========================================================
    // SETUP SWIPE REFRESH
    // =========================================================

    private fun setupSwipeRefreshState() {

        val scrollView =
            verticalScrollView
                ?: return


        /*
         * Pantau perubahan posisi scroll.
         */
        scrollView.setOnScrollChangeListener {
                _,
                _,
                _,
                _,
                _ ->

            updateRefreshState()
        }


        /*
         * Cek posisi awal setelah layout selesai.
         */
        scrollView.post {

            updateRefreshState()
        }
    }


    // =========================================================
    // UPDATE STATUS SWIPE REFRESH
    // =========================================================

    override fun updateRefreshState() {

        if (!isAdded) {
            return
        }


        val homeActivity =
            activity as? HomeActivity
                ?: return


        /*
         * Hanya fragment Infografik
         * yang boleh mengatur SwipeRefreshLayout
         * ketika berada di halaman nomor 1.
         */
        if (homeActivity.currentPage != 1) {
            return
        }


        val scrollView =
            verticalScrollView
                ?: return


        /*
         * canScrollVertically(-1) bernilai false
         * jika posisi sudah paling atas.
         */
        val isAtTop =
            !scrollView.canScrollVertically(-1)


        homeActivity.setSwipeRefreshEnabled(
            isAtTop
        )
    }


    // =========================================================
    // REFRESH DATA
    // =========================================================

    override fun refreshData() {

        if (!isAdded) {
            return
        }


        val homeActivity =
            activity as? HomeActivity
                ?: return


        /*
         * Pastikan memang sedang berada
         * di halaman Infografik.
         */
        if (homeActivity.currentPage != 1) {
            return
        }


        val scrollView =
            verticalScrollView


        /*
         * Jangan refresh jika belum berada
         * di posisi paling atas.
         */
        if (
            scrollView != null &&
            scrollView.canScrollVertically(-1)
        ) {
            return
        }


        /*
         * Tandai proses refresh.
         */
        sedangRefresh = true


        /*
         * Ambil ulang data dari API.
         */
        loadInfografik()
    }


    // =========================================================
    // LOAD INFOGRAFIK
    // =========================================================

    private fun loadInfografik() {

        repository.getInfographics(

            API_KEY,

            callback = { data ->

                if (!isAdded) {
                    return@getInfographics
                }


                semuaInfografik.clear()


                semuaInfografik =
                    data
                        .distinctBy {
                            it.infId
                        }
                        .sortedByDescending {
                            it.date ?: ""
                        }
                        .dropLast(1)
                        .toMutableList()


                Log.d(
                    "HOME_INFOGRAFIK",
                    "Jumlah infografis diterima: ${data.size}"
                )


                Log.d(
                    "HOME_INFOGRAFIK",
                    "Jumlah setelah skip data terakhir: ${semuaInfografik.size}"
                )


                tampilkanHasil()


                /*
                 * Hentikan spinner setelah
                 * data berhasil dimuat.
                 */
                if (sedangRefresh) {

                    sedangRefresh = false

                    (activity as? HomeActivity)
                        ?.finishSwipeRefresh()
                }


                /*
                 * Update kembali status refresh
                 * berdasarkan posisi scroll terbaru.
                 */
                verticalScrollView?.post {

                    updateRefreshState()
                }
            },

            onError = { error ->

                Log.e(
                    "HOME_INFOGRAFIK",
                    "Gagal mengambil infografis",
                    error
                )


                /*
                 * Tetap hentikan spinner jika
                 * proses refresh gagal.
                 */
                if (sedangRefresh) {

                    sedangRefresh = false

                    (activity as? HomeActivity)
                        ?.finishSwipeRefresh()
                }


                verticalScrollView?.post {

                    updateRefreshState()
                }
            }
        )
    }


    // =========================================================
    // DETAIL INFOGRAFIK
    // =========================================================

    private fun tampilkanDetailInfografik(
        infographic: Infografik
    ) {

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
            infographic.title
                ?: "Infografis"


        val tanggal =
            infographic.date
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: ""


        if (tanggal.isEmpty()) {

            date.visibility =
                View.GONE

        } else {

            date.visibility =
                View.VISIBLE

            date.text =
                tanggal.take(10)
        }


        description.text =
            Html.fromHtml(
                infographic.desc ?: "",
                Html.FROM_HTML_MODE_LEGACY
            )
                .toString()
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()


        Glide.with(this)
            .load(infographic.img)
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
                .setPositiveButton(
                    "Tutup",
                    null
                )
                .create()


        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                android.graphics.Color.parseColor(
                    "#F97316"
                )
            )
        }


        dialog.show()
    }


    // =========================================================
    // TAMPILKAN HASIL
    // =========================================================

    private fun tampilkanHasil() {

        val keyword =
            search.text
                .toString()
                .trim()
                .lowercase(
                    Locale.getDefault()
                )


        val hasil =
            semuaInfografik
                .filter { infographic ->

                    // =========================================
                    // EXCLUDE INFOGRAFIS ANOMALI
                    // =========================================

                    val judul =
                        infographic.title
                            ?.lowercase(
                                Locale.getDefault()
                            )
                            ?: ""


                    if (judul.contains("anomali")) {
                        return@filter false
                    }


                    // =========================================
                    // FILTER TAHUN
                    // =========================================

                    val cocokTahun =
                        tahunTerpilih == null ||
                                infographic.date
                                    ?.take(4)
                                    ?.toIntOrNull() ==
                                tahunTerpilih


                    if (!cocokTahun) {
                        return@filter false
                    }


                    // =========================================
                    // SEARCH
                    // JUDUL + DESKRIPSI
                    // =========================================

                    if (keyword.isEmpty()) {
                        return@filter true
                    }


                    val deskripsi =
                        bersihkanHtml(
                            infographic.desc ?: ""
                        )
                            .lowercase(
                                Locale.getDefault()
                            )


                    judul.contains(keyword) ||
                            deskripsi.contains(keyword)
                }
                .sortedByDescending {
                    it.date ?: ""
                }


        // =========================================
        // TAMPILKAN
        // =========================================

        container.removeAllViews()


        hasil.chunked(2).forEach { baris ->

            val row =
                LinearLayout(
                    requireContext()
                ).apply {

                    orientation =
                        LinearLayout.HORIZONTAL


                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {

                            bottomMargin =
                                (
                                        12 *
                                                resources
                                                    .displayMetrics
                                                    .density
                                        ).toInt()
                        }
                }


            baris.forEachIndexed {
                    index,
                    infographic ->

                val card =
                    LayoutInflater.from(
                        requireContext()
                    ).inflate(
                        R.layout.item_home,
                        row,
                        false
                    )


                val image =
                    card.findViewById<ImageView>(
                        R.id.imgHome
                    )


                val progressImage =
                    card.findViewById<ProgressBar>(
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


                title.text =
                    infographic.title
                        ?: "Infografis"


                description.text =
                    bersihkanHtml(
                        infographic.desc ?: ""
                    )


                progressImage.visibility =
                    View.VISIBLE


                Glide.with(this)
                    .load(infographic.img)
                    .placeholder(
                        R.drawable.ic_bpslogo
                    )
                    .error(
                        R.drawable.ic_bpslogo
                    )
                    .into(
                        object :
                            CustomTarget<Drawable>() {

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


                card.setOnClickListener {

                    tampilkanDetailInfografik(
                        infographic
                    )
                }


                val params =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {

                        if (index == 0) {

                            rightMargin =
                                (
                                        6 *
                                                resources
                                                    .displayMetrics
                                                    .density
                                        ).toInt()

                        } else {

                            leftMargin =
                                (
                                        6 *
                                                resources
                                                    .displayMetrics
                                                    .density
                                        ).toInt()
                        }
                    }


                card.layoutParams =
                    params


                row.addView(card)
            }


            // =============================================
            // PENYEIMBANG CARD GANJIL
            // =============================================

            if (baris.size == 1) {

                val emptySpace =
                    View(requireContext())


                emptySpace.layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        0,
                        1f
                    ).apply {

                        leftMargin =
                            (
                                    6 *
                                            resources
                                                .displayMetrics
                                                .density
                                    ).toInt()
                    }


                row.addView(
                    emptySpace
                )
            }


            container.addView(row)
        }


        jumlah.text =
            "${hasil.size} infografis"
    }


    // =========================================================
    // FILTER TAHUN
    // =========================================================

    private fun tampilkanDialogTahun() {

        val tahun =
            semuaInfografik
                .mapNotNull {

                    it.date
                        ?.take(4)
                        ?.toIntOrNull()
                }
                .distinct()
                .sortedDescending()


        val pilihan =
            mutableListOf<String>()


        pilihan.add(
            "Semua Tahun"
        )


        tahun.forEach {

            pilihan.add(
                it.toString()
            )
        }


        val checkedItem =
            when (tahunTerpilih) {

                null -> 0

                else ->
                    pilihan.indexOf(
                        tahunTerpilih.toString()
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
                pilihan.toTypedArray(),
                checkedItem
            ) { dialog, which ->

                tahunTerpilih =
                    if (which == 0) {

                        null

                    } else {

                        pilihan[which]
                            .toIntOrNull()
                    }


                tampilkanHasil()


                dialog.dismiss()
            }
            .show()
    }


    // =========================================================
    // BERSIHKAN HTML
    // =========================================================

    private fun bersihkanHtml(
        text: String
    ): String {

        return Html.fromHtml(
            text,
            Html.FROM_HTML_MODE_LEGACY
        )
            .toString()
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }


    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {

        /*
         * Hentikan spinner jika fragment
         * dihancurkan ketika sedang refresh.
         */
        if (sedangRefresh) {

            sedangRefresh = false

            (activity as? HomeActivity)
                ?.finishSwipeRefresh()
        }


        verticalScrollView = null


        super.onDestroyView()
    }
}