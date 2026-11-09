package com.example.bpskota

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.Html
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
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.bpskota.bps.model.Infografik
import com.example.bpskota.bps.repository.BpsRepository
import java.util.Locale

class InfografikFragment : Fragment(), RefreshableFragment {

    companion object {
        private const val PAGE_INDEX = 1
    }

    private lateinit var repository: BpsRepository

    private lateinit var container: LinearLayout
    private lateinit var search: EditText
    private lateinit var jumlah: TextView

    private var layoutLoadingInfografik: View? = null
    private var lottieLoadingInfografik: LottieAnimationView? = null

    private var semuaInfografik = mutableListOf<Infografik>()
    private var tahunTerpilih: Int? = null

    private var verticalScrollView: View? = null

    // Status loading data
    private var sudahMemuat = false
    private var sedangMemuat = false
    private var sedangRefresh = false

    private val API_KEY = "008edaaae5d450b1913b31a2cef618c3"

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
        super.onViewCreated(view, savedInstanceState)

        repository = BpsRepository()

        container = view.findViewById(R.id.infografikContainer)
        search = view.findViewById(R.id.etSearchInfografis)
        jumlah = view.findViewById(R.id.tvJumlahInfografis)

        layoutLoadingInfografik = view.findViewById(R.id.layoutLoadingInfografik)
        lottieLoadingInfografik = view.findViewById(R.id.lottieLoadingInfografik)

        val btnFilter = view.findViewById<View>(R.id.btnFilter)

        // =====================================================
        // SETUP SCROLL
        // =====================================================

        verticalScrollView = findVerticalScrollContainer(view)

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
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    tampilkanHasil()
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )

        // =====================================================
        // FILTER
        // =====================================================

        btnFilter.setOnClickListener {
            tampilkanDialogTahun()
        }

        // =====================================================
        // TAMPILKAN DATA TERSIMPAN
        // =====================================================

        if (semuaInfografik.isNotEmpty()) {
            tampilkanHasil()
        }
    }

    // =========================================================
    // LOADING LOTTIE
    // =========================================================

    private fun tampilkanLoading() {
        val layout = layoutLoadingInfografik ?: return
        val lottie = lottieLoadingInfografik ?: return

        layout.visibility = View.VISIBLE
        lottie.playAnimation()
    }

    private fun sembunyikanLoading() {
        val layout = layoutLoadingInfografik ?: return
        val lottie = lottieLoadingInfografik ?: return

        lottie.cancelAnimation()
        layout.visibility = View.GONE
    }

    // =========================================================
    // LOAD KETIKA FRAGMENT AKTIF
    // =========================================================

    override fun onResume() {
        super.onResume()

        val homeActivity = activity as? HomeActivity ?: return

        // Hanya load ketika menu Infografik aktif.
        if (homeActivity.currentPage != PAGE_INDEX) {
            return
        }

        // Jangan request ulang jika sudah berhasil dimuat.
        if (sudahMemuat || sedangMemuat) {
            return
        }

        loadInfografik()
    }

    // =========================================================
    // CARI SCROLL CONTAINER
    // =========================================================

    private fun findVerticalScrollContainer(
        root: View
    ): View? {
        if (root is ScrollView) {
            return root
        }

        if (root is NestedScrollView) {
            return root
        }

        if (root is RecyclerView) {
            return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val result = findVerticalScrollContainer(child)
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
        val scrollView = verticalScrollView ?: return

        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            updateRefreshState()
        }

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

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) {
            return
        }

        val scrollView = verticalScrollView ?: return
        val isAtTop = !scrollView.canScrollVertically(-1)

        homeActivity.setSwipeRefreshEnabled(isAtTop)
    }

    // =========================================================
    // REFRESH DATA
    // =========================================================

    override fun refreshData() {
        if (!isAdded) {
            return
        }

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) {
            return
        }

        val scrollView = verticalScrollView

        if (
            scrollView != null &&
            scrollView.canScrollVertically(-1)
        ) {
            return
        }

        if (sedangMemuat) {
            return
        }

        sedangRefresh = true
        loadInfografik()
    }

    // =========================================================
    // LOAD INFOGRAFIK
    // =========================================================

    private fun loadInfografik() {
        if (sedangMemuat) {
            return
        }

        sedangMemuat = true

        if (!sudahMemuat && !sedangRefresh) {
            tampilkanLoading()
        }

        repository.getInfographics(
            API_KEY,
            callback = { data ->
                sedangMemuat = false

                if (!isAdded || view == null) {
                    return@getInfographics
                }

                semuaInfografik = data
                    .distinctBy { it.infId }
                    .sortedByDescending { it.date ?: "" }
                    .dropLast(1)
                    .toMutableList()

                sudahMemuat = true

                sembunyikanLoading()
                tampilkanHasil()

                if (sedangRefresh) {
                    sedangRefresh = false
                    (activity as? HomeActivity)?.finishSwipeRefresh()
                }

                verticalScrollView?.post {
                    updateRefreshState()
                }
            },
            onError = { error ->
                sedangMemuat = false

                Log.e(
                    "HOME_INFOGRAFIK",
                    "Gagal mengambil infografis",
                    error
                )

                sudahMemuat = false

                if (isAdded && view != null) {
                    sembunyikanLoading()
                }

                if (sedangRefresh) {
                    sedangRefresh = false
                    (activity as? HomeActivity)?.finishSwipeRefresh()
                }

                verticalScrollView?.post {
                    updateRefreshState()
                }
            }
        )
    }

    // =========================================================
    // LOAD GAMBAR DENGAN CACHE GLIDE + THUMBNAIL
    // =========================================================

    private fun muatGambar(
        image: ImageView,
        imageUrl: String?,
        progressImage: ProgressBar? = null
    ) {
        progressImage?.visibility = View.VISIBLE

        if (imageUrl.isNullOrBlank()) {
            image.setImageResource(R.drawable.ic_bpslogo)
            progressImage?.visibility = View.GONE
            return
        }

        val requestManager = Glide.with(this)

        requestManager
            .load(imageUrl)
            .thumbnail(
                requestManager
                    .load(imageUrl)
                    .override(180, 135)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
            )
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .placeholder(R.drawable.ic_bpslogo)
            .error(R.drawable.ic_bpslogo)
            .listener(
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressImage?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressImage?.visibility = View.GONE
                        return false
                    }
                }
            )
            .into(image)
    }

    // =========================================================
    // DETAIL INFOGRAFIK
    // =========================================================

    private fun tampilkanDetailInfografik(
        infographic: Infografik
    ) {
        val dialogView = LayoutInflater.from(
            requireContext()
        ).inflate(
            R.layout.dialog_detail_home,
            null
        )

        val image = dialogView.findViewById<ImageView>(R.id.imgDetailHome)
        val title = dialogView.findViewById<TextView>(R.id.tvDetailHomeTitle)
        val date = dialogView.findViewById<TextView>(R.id.tvDetailHomeDate)
        val description = dialogView.findViewById<TextView>(R.id.tvDetailHomeDescription)

        title.text = infographic.title ?: "Infografis"

        val tanggal = infographic.date?.takeIf { it.isNotBlank() } ?: ""

        if (tanggal.isEmpty()) {
            date.visibility = View.GONE
        } else {
            date.visibility = View.VISIBLE
            date.text = tanggal.take(10)
        }

        description.text = Html.fromHtml(
            infographic.desc ?: "",
            Html.FROM_HTML_MODE_LEGACY
        )
            .toString()
            .replace(Regex("\\s+"), " ")
            .trim()

        muatGambar(
            image = image,
            imageUrl = infographic.img
        )

        val dialog = AlertDialog.Builder(
            requireContext()
        )
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                android.graphics.Color.parseColor("#F97316")
            )
        }

        dialog.show()
    }

    // =========================================================
    // TAMPILKAN HASIL
    // =========================================================

    private fun tampilkanHasil() {
        if (!isAdded || !::container.isInitialized) {
            return
        }

        val keyword = search.text
            .toString()
            .trim()
            .lowercase(Locale.getDefault())

        val hasil = semuaInfografik
            .filter { infographic ->
                val judul = infographic.title
                    ?.lowercase(Locale.getDefault()) ?: ""

                if (judul.contains("anomali")) {
                    return@filter false
                }

                val cocokTahun = tahunTerpilih == null ||
                        infographic.date
                            ?.take(4)
                            ?.toIntOrNull() == tahunTerpilih

                if (!cocokTahun) {
                    return@filter false
                }

                if (keyword.isEmpty()) {
                    return@filter true
                }

                val deskripsi = bersihkanHtml(
                    infographic.desc ?: ""
                ).lowercase(Locale.getDefault())

                judul.contains(keyword) ||
                        deskripsi.contains(keyword)
            }
            .sortedByDescending {
                it.date ?: ""
            }

        container.removeAllViews()

        hasil.chunked(2).forEach { baris ->
            val row = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.HORIZONTAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (12 * resources.displayMetrics.density).toInt()
                }
            }

            baris.forEachIndexed { index, infographic ->
                val card = LayoutInflater.from(
                    requireContext()
                ).inflate(
                    R.layout.item_home,
                    row,
                    false
                )

                val image = card.findViewById<ImageView>(R.id.imgHome)
                val progressImage = card.findViewById<ProgressBar>(R.id.progressImage)
                val title = card.findViewById<TextView>(R.id.tvHomeTitle)
                val description = card.findViewById<TextView>(R.id.tvHomeDescription)

                title.text = infographic.title ?: "Infografis"

                description.text = bersihkanHtml(
                    infographic.desc ?: ""
                )

                muatGambar(
                    image = image,
                    imageUrl = infographic.img,
                    progressImage = progressImage
                )

                card.setOnClickListener {
                    tampilkanDetailInfografik(
                        infographic
                    )
                }

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index == 0) {
                        rightMargin = (6 * resources.displayMetrics.density).toInt()
                    } else {
                        leftMargin = (6 * resources.displayMetrics.density).toInt()
                    }
                }

                card.layoutParams = params
                row.addView(card)
            }

            if (baris.size == 1) {
                val emptySpace = View(requireContext())

                emptySpace.layoutParams = LinearLayout.LayoutParams(
                    0,
                    0,
                    1f
                ).apply {
                    leftMargin = (6 * resources.displayMetrics.density).toInt()
                }

                row.addView(emptySpace)
            }

            container.addView(row)
        }

        jumlah.text = "${hasil.size} infografis"
    }

    // =========================================================
    // FILTER TAHUN
    // =========================================================

    private fun tampilkanDialogTahun() {
        val tahun = semuaInfografik
            .mapNotNull {
                it.date?.take(4)?.toIntOrNull()
            }
            .distinct()
            .sortedDescending()

        val pilihan = mutableListOf<String>()
        pilihan.add("Semua Tahun")

        tahun.forEach {
            pilihan.add(it.toString())
        }

        val checkedItem = when (tahunTerpilih) {
            null -> 0
            else -> pilihan.indexOf(
                tahunTerpilih.toString()
            ).takeIf {
                it >= 0
            } ?: 0
        }

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Filter Tahun")
            .setSingleChoiceItems(
                pilihan.toTypedArray(),
                checkedItem
            ) { dialog, which ->
                tahunTerpilih = if (which == 0) {
                    null
                } else {
                    pilihan[which].toIntOrNull()
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
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {
        if (sedangRefresh) {
            sedangRefresh = false
            (activity as? HomeActivity)?.finishSwipeRefresh()
        }

        lottieLoadingInfografik?.cancelAnimation()
        lottieLoadingInfografik = null
        layoutLoadingInfografik = null

        verticalScrollView = null

        super.onDestroyView()
    }
}