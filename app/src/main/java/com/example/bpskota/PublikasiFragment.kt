
package com.example.bpskota

import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.bpskota.bps.model.Publikasi
import com.example.bpskota.bps.repository.BpsRepository

class PublikasiFragment : Fragment(), RefreshableFragment {

    companion object {
        private const val PAGE_INDEX = 2
        private const val MAX_PARALLEL_REQUESTS = 3
    }

    private val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
    private val DOMAIN = "3574"

    // =========================================================
    // DATA
    // =========================================================

    private val semuaPublikasi = mutableListOf<Publikasi>()

    private var tahunTerpilih: String? = null
    private var kataKunci = ""

    private var sedangMemuatSemua = false
    private var sudahMemuat = false
    private var sedangRefresh = false

    private val repository = BpsRepository()

    // =========================================================
    // VIEW
    // =========================================================

    private lateinit var publikasiContainer: RecyclerView
    private lateinit var tvJumlahPublikasi: TextView
    private lateinit var etSearchPublikasi: EditText
    private lateinit var btnFilterPublikasi: View
    private lateinit var publikasiLoading: LottieAnimationView

    private lateinit var publikasiAdapter: PublikasiAdapter

    // =========================================================
    // CREATE VIEW
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_publikasi,
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

        publikasiContainer =
            view.findViewById(R.id.publikasiContainer)

        tvJumlahPublikasi =
            view.findViewById(R.id.tvJumlahPublikasi)

        etSearchPublikasi =
            view.findViewById(R.id.etSearchPublikasi)

        btnFilterPublikasi =
            view.findViewById(R.id.btnFilterPublikasi)

        publikasiLoading =
            view.findViewById(R.id.PublikasiLoading)

        setupRecyclerView()
        setupSwipeRefreshState()
        setupSearch()
        setupFilter()

        when {
            sedangMemuatSemua && semuaPublikasi.isEmpty() -> {
                mulaiLoading()
            }

            sudahMemuat -> {
                selesaiLoading()
                tampilkanHasil()
            }

            semuaPublikasi.isNotEmpty() -> {
                selesaiLoading()
                tampilkanHasil()
            }

            sedangMemuatSemua -> {
                mulaiLoading()
            }
        }
    }

    // =========================================================
    // SETUP RECYCLERVIEW
    // =========================================================

    private fun setupRecyclerView() {

        publikasiAdapter = PublikasiAdapter { publikasi ->
            tampilkanPopupPublikasi(publikasi)
        }

        publikasiContainer.apply {

            layoutManager = LinearLayoutManager(requireContext())

            adapter = publikasiAdapter

            setHasFixedSize(false)

            itemAnimator = null

            overScrollMode = View.OVER_SCROLL_NEVER

            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {

                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int
                    ) {
                        super.onScrolled(recyclerView, dx, dy)
                        updateRefreshState()
                    }
                }
            )
        }
    }

    // =========================================================
    // LOAD KETIKA MENU PUBLIKASI AKTIF
    // =========================================================

    override fun onResume() {
        super.onResume()

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        if (sudahMemuat || sedangMemuatSemua) return

        if (semuaPublikasi.isEmpty()) {
            mulaiLoading()
        }

        loadSemuaPublikasi()
    }

    // =========================================================
    // SETUP SWIPE REFRESH
    // =========================================================

    private fun setupSwipeRefreshState() {

        publikasiContainer.post {
            updateRefreshState()
        }
    }

    // =========================================================
    // UPDATE STATUS SWIPE REFRESH
    // =========================================================

    override fun updateRefreshState() {

        if (!isAdded || !::publikasiContainer.isInitialized) return

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        val isAtTop =
            !publikasiContainer.canScrollVertically(-1)

        homeActivity.setSwipeRefreshEnabled(isAtTop)
    }

    // =========================================================
    // REFRESH DATA
    // =========================================================

    override fun refreshData() {

        if (!isAdded) return

        val homeActivity = activity as? HomeActivity ?: return

        if (homeActivity.currentPage != PAGE_INDEX) return

        if (publikasiContainer.canScrollVertically(-1)) return

        if (sedangMemuatSemua) {
            homeActivity.finishSwipeRefresh()
            return
        }

        sedangRefresh = true

        loadSemuaPublikasi()
    }

    // =========================================================
    // MULAI LOADING
    // =========================================================

    private fun mulaiLoading() {

        if (!isAdded || view == null) return

        publikasiLoading.visibility = View.VISIBLE

        publikasiLoading.setAnimation("Loading_Animation.json")

        publikasiLoading.repeatCount = LottieDrawable.INFINITE

        publikasiLoading.playAnimation()

        publikasiContainer.visibility = View.GONE

        tvJumlahPublikasi.text = "Memuat..."
    }

    // =========================================================
    // SELESAI LOADING
    // =========================================================

    private fun selesaiLoading() {

        if (!isAdded || view == null) return

        publikasiLoading.cancelAnimation()

        publikasiLoading.visibility = View.GONE

        publikasiContainer.visibility = View.VISIBLE
    }

    // =========================================================
    // SEARCH LOKAL
    // =========================================================

    private fun setupSearch() {

        etSearchPublikasi.addTextChangedListener(
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

                    kataKunci = s?.toString()?.trim() ?: ""

                    tampilkanHasil()
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    // =========================================================
    // FILTER
    // =========================================================

    private fun setupFilter() {

        btnFilterPublikasi.setOnClickListener {
            tampilkanDialogTahun()
        }
    }

    // =========================================================
    // LOAD SEMUA PUBLIKASI
    // =========================================================

    private fun loadSemuaPublikasi() {

        if (sedangMemuatSemua) return

        sedangMemuatSemua = true

        // Saat pertama membuka halaman, data ditampilkan
        // secara bertahap.
        //
        // Saat refresh, data lama tetap ditampilkan
        // sampai data baru selesai dimuat.

        val tampilkanBertahap = semuaPublikasi.isEmpty()

        val dataPerHalaman =
            mutableMapOf<Int, List<Publikasi>>()

        repository.getPublikasi(
            page = 1,
            domain = DOMAIN,
            apiKey = API_KEY
        ) { response ->

            if (!isAdded) {
                sedangMemuatSemua = false
                return@getPublikasi
            }

            if (response == null) {
                gagalLoadPublikasi()
                return@getPublikasi
            }

            val pagination = response.getPagination()

            val dataHalamanPertama = response.getPublikasi()

            dataPerHalaman[1] = dataHalamanPertama

            val totalPage =
                (pagination?.pages ?: 1).coerceAtLeast(1)

            if (tampilkanBertahap) {
                perbaruiDataPublikasi(dataPerHalaman)
            }

            if (totalPage <= 1) {

                selesaiLoadSemuaPublikasi(dataPerHalaman)

            } else {

                loadHalamanBerikutnya(
                    halaman = 2,
                    totalPage = totalPage,
                    dataPerHalaman = dataPerHalaman,
                    tampilkanBertahap = tampilkanBertahap
                )
            }
        }
    }

    // =========================================================
    // LOAD HALAMAN BERIKUTNYA SECARA PARALEL
    // =========================================================

    private fun loadHalamanBerikutnya(
        halaman: Int,
        totalPage: Int,
        dataPerHalaman: MutableMap<Int, List<Publikasi>>,
        tampilkanBertahap: Boolean
    ) {

        if (halaman > totalPage) {
            selesaiLoadSemuaPublikasi(dataPerHalaman)
            return
        }

        val halamanTerakhir = minOf(
            halaman + MAX_PARALLEL_REQUESTS - 1,
            totalPage
        )

        val jumlahRequest = halamanTerakhir - halaman + 1

        var jumlahSelesai = 0

        var adaKegagalan = false

        for (page in halaman..halamanTerakhir) {

            repository.getPublikasi(
                page = page,
                domain = DOMAIN,
                apiKey = API_KEY
            ) { response ->

                if (!isAdded) {
                    sedangMemuatSemua = false
                    return@getPublikasi
                }

                if (response == null) {
                    adaKegagalan = true
                } else {
                    dataPerHalaman[page] = response.getPublikasi()
                }

                jumlahSelesai++

                if (jumlahSelesai == jumlahRequest) {

                    if (adaKegagalan) {
                        gagalLoadPublikasi()
                        return@getPublikasi
                    }

                    if (tampilkanBertahap) {
                        perbaruiDataPublikasi(dataPerHalaman)
                    }

                    loadHalamanBerikutnya(
                        halaman = halamanTerakhir + 1,
                        totalPage = totalPage,
                        dataPerHalaman = dataPerHalaman,
                        tampilkanBertahap = tampilkanBertahap
                    )
                }
            }
        }
    }

    // =========================================================
    // IDENTITAS PUBLIKASI
    // =========================================================

    private fun identitasPublikasi(
        publikasi: Publikasi
    ): String {

        return publikasi.pub_id
            ?.takeIf { it.isNotBlank() }
            ?: "${publikasi.title}_${publikasi.sch_date}_${publikasi.rl_date}"
    }

    // =========================================================
    // GABUNGKAN DATA BERDASARKAN NOMOR HALAMAN
    // =========================================================

    private fun perbaruiDataPublikasi(
        dataPerHalaman: Map<Int, List<Publikasi>>
    ) {
        if (!isAdded) return

        // Simpan data sementara, tetapi jangan tampilkan dahulu.
        // Loading tetap berjalan sampai seluruh halaman selesai.
    }

    // =========================================================
    // SEMUA HALAMAN SELESAI
    // =========================================================

    private fun selesaiLoadSemuaPublikasi(
        dataPerHalaman: Map<Int, List<Publikasi>>
    ) {

        sedangMemuatSemua = false

        if (!isAdded) return

        val dataLengkap = dataPerHalaman
            .toSortedMap()
            .values
            .flatten()
            .distinctBy { identitasPublikasi(it) }

        semuaPublikasi.clear()
        semuaPublikasi.addAll(dataLengkap)

        sudahMemuat = true

        if (view != null) {

            // Loading baru dihilangkan setelah semua halaman selesai.
            selesaiLoading()

            // Tampilkan seluruh data.
            tampilkanHasil()

            publikasiContainer.post {
                updateRefreshState()
            }
        }

        if (sedangRefresh) {
            sedangRefresh = false
            (activity as? HomeActivity)?.finishSwipeRefresh()
        }
    }

    // =========================================================
    // GAGAL LOAD PUBLIKASI
    // =========================================================

    private fun gagalLoadPublikasi() {

        sedangMemuatSemua = false

        if (!isAdded) return

        if (view != null) {

            publikasiLoading.cancelAnimation()
            publikasiLoading.visibility = View.GONE

            publikasiContainer.visibility = View.VISIBLE

            if (semuaPublikasi.isEmpty()) {
                publikasiAdapter.submitList(emptyList())
                tvJumlahPublikasi.text = "Gagal memuat"
            } else {
                // Jika refresh gagal, data lama tetap ditampilkan.
                tampilkanHasil()
            }

            publikasiContainer.post {
                updateRefreshState()
            }
        }

        if (sedangRefresh) {
            sedangRefresh = false
            (activity as? HomeActivity)?.finishSwipeRefresh()
        }

        Toast.makeText(
            requireContext(),
            "Gagal memuat data publikasi",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun tampilkanHasil() {

        if (!isAdded || view == null) return

        val hasil = semuaPublikasi.filter { publikasi ->

            val sesuaiTahun =
                tahunTerpilih == null ||
                        ambilTahun(publikasi) == tahunTerpilih

            val sesuaiPencarian =
                kataKunci.isEmpty() ||
                        publikasi.title?.contains(
                            kataKunci,
                            ignoreCase = true
                        ) == true

            sesuaiTahun && sesuaiPencarian
        }

        publikasiAdapter.submitList(hasil)

        tvJumlahPublikasi.text =
            if (hasil.isEmpty() && semuaPublikasi.isNotEmpty()) {
                "0 publikasi • tidak ditemukan"
            } else {
                "${hasil.size} publikasi"
            }

        publikasiContainer.visibility = View.VISIBLE
    }

    // =========================================================
    // AMBIL TAHUN PUBLIKASI
    // =========================================================

    private fun ambilTahun(
        publikasi: Publikasi
    ): String? {

        val tanggal = publikasi.rl_date ?: publikasi.sch_date

        return tanggal
            ?.takeIf { it.length >= 4 }
            ?.substring(0, 4)
    }

    // =========================================================
    // DIALOG FILTER TAHUN
    // =========================================================

    private fun tampilkanDialogTahun() {

        val tahun = semuaPublikasi
            .mapNotNull { ambilTahun(it) }
            .distinct()
            .sortedDescending()

        if (tahun.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Tahun publikasi belum tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pilihan = mutableListOf("Semua Tahun")

        pilihan.addAll(tahun)

        val posisiTerpilih = if (tahunTerpilih == null) {
            0
        } else {
            pilihan.indexOf(tahunTerpilih)
                .takeIf { it >= 0 } ?: 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Tahun")
            .setSingleChoiceItems(
                pilihan.toTypedArray(),
                posisiTerpilih
            ) { dialog, which ->

                tahunTerpilih =
                    if (which == 0) null else pilihan[which]

                tampilkanHasil()

                dialog.dismiss()
            }
            .show()
    }

    // =========================================================
    // DETAIL PUBLIKASI
    // =========================================================

    private fun tampilkanPopupPublikasi(
        publikasi: Publikasi
    ) {

        val dialog = Dialog(requireContext())

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_detail_home,
            null,
            false
        )

        val image = dialogView.findViewById<ImageView>(
            R.id.imgDetailHome
        )

        val title = dialogView.findViewById<TextView>(
            R.id.tvDetailHomeTitle
        )

        val date = dialogView.findViewById<TextView>(
            R.id.tvDetailHomeDate
        )

        val description = dialogView.findViewById<TextView>(
            R.id.tvDetailHomeDescription
        )

        val containerDownload = dialogView.findViewById<LinearLayout>(
            R.id.containerDownload
        )

        title.text = publikasi.title ?: "Publikasi"

        date.text = ambilTahun(publikasi) ?: "-"

        description.text = "Memuat deskripsi..."

        Glide.with(this)
            .load(publikasi.cover)
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_bpslogo)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .error(R.drawable.ic_bpslogo)
            .into(image)

        // =====================================================
        // TOMBOL DOWNLOAD PDF
        // =====================================================

        val tombolDownload = TextView(requireContext()).apply {

            text = "Download PDF"

            textSize = 14f

            setTextColor(Color.WHITE)

            gravity = Gravity.CENTER

            typeface = Typeface.DEFAULT_BOLD

            setPadding(20, 14, 20, 14)

            setBackgroundColor(Color.parseColor("#F97316"))

            isClickable = true

            isFocusable = true

            setOnClickListener {

                val pdfUrl = publikasi.pdf

                if (pdfUrl.isNullOrBlank()) {

                    Toast.makeText(
                        requireContext(),
                        "File PDF tidak tersedia",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val namaPublikasi = publikasi.title
                    ?.trim()
                    ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    ?.replace(Regex("\\s+"), " ")
                    ?.take(150)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Publikasi"

                AlertDialog.Builder(requireContext())
                    .setTitle("Download Publikasi")
                    .setMessage(
                        "Yakin ingin mendownload publikasi:\n\n$namaPublikasi?"
                    )
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Download") { _, _ ->

                        try {

                            val request = DownloadManager.Request(
                                Uri.parse(pdfUrl)
                            )

                            request.setTitle(namaPublikasi)

                            request.setDescription(
                                "Mengunduh publikasi PDF..."
                            )

                            request.setNotificationVisibility(
                                DownloadManager.Request
                                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            )

                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "$namaPublikasi.pdf"
                            )

                            request.setMimeType("application/pdf")

                            request.setAllowedOverMetered(true)

                            request.setAllowedOverRoaming(true)

                            val downloadManager = requireContext()
                                .getSystemService(
                                    Context.DOWNLOAD_SERVICE
                                ) as DownloadManager

                            downloadManager.enqueue(request)

                            Toast.makeText(
                                requireContext(),
                                "Download dimulai",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (e: Exception) {

                            Toast.makeText(
                                requireContext(),
                                "Gagal memulai download",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
            }
        }

        containerDownload.addView(
            tombolDownload,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        dialog.setContentView(dialogView)

        dialog.show()

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // =====================================================
        // AMBIL DETAIL PUBLIKASI
        // =====================================================

        val pubId = publikasi.pub_id

        if (pubId.isNullOrBlank()) {

            description.text = "Deskripsi tidak tersedia"

            return
        }

        repository.getPublikasiDetail(
            id = pubId,
            domain = DOMAIN,
            apiKey = API_KEY
        ) { response ->

            if (!isAdded) return@getPublikasiDetail

            if (!dialog.isShowing) return@getPublikasiDetail

            if (response == null) {

                description.text = "Gagal memuat deskripsi"

                return@getPublikasiDetail
            }

            val abstractText = response.data?.abstract

            description.text = abstractText
                ?.replace(
                    Regex("<br\\s*/?>", RegexOption.IGNORE_CASE),
                    "\n"
                )
                ?.replace(Regex("<[^>]*>"), "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Deskripsi tidak tersedia"
        }
    }

    // =========================================================
    // RECYCLERVIEW ADAPTER
    // =========================================================

    private class PublikasiAdapter(
        private val onItemClick: (Publikasi) -> Unit
    ) : ListAdapter<Publikasi, PublikasiAdapter.PublikasiViewHolder>(
        DIFF_CALLBACK
    ) {

        companion object {

            private val DIFF_CALLBACK =
                object : DiffUtil.ItemCallback<Publikasi>() {

                    override fun areItemsTheSame(
                        oldItem: Publikasi,
                        newItem: Publikasi
                    ): Boolean {

                        val oldId = oldItem.pub_id
                        val newId = newItem.pub_id

                        return if (
                            !oldId.isNullOrBlank() &&
                            !newId.isNullOrBlank()
                        ) {
                            oldId == newId
                        } else {
                            oldItem == newItem
                        }
                    }

                    override fun areContentsTheSame(
                        oldItem: Publikasi,
                        newItem: Publikasi
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
        }

        class PublikasiViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            val image: ImageView =
                itemView.findViewById(R.id.imgStatistik)

            val imageLoading: ProgressBar =
                itemView.findViewById(R.id.imageLoading)

            val title: TextView =
                itemView.findViewById(R.id.tvStatistikTitle)

            val description: TextView =
                itemView.findViewById(R.id.tvStatistikDescription)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PublikasiViewHolder {

            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_home_panjang,
                parent,
                false
            )

            return PublikasiViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: PublikasiViewHolder,
            position: Int
        ) {

            val publikasi = getItem(position)

            holder.title.text =
                publikasi.title ?: "Publikasi"

            holder.description.text = ""

            holder.image.setImageResource(
                R.drawable.ic_bpslogo
            )

            holder.itemView.setOnClickListener {
                onItemClick(publikasi)
            }

            val imageUrl = publikasi.cover

            if (imageUrl.isNullOrBlank()) {

                holder.imageLoading.visibility = View.GONE

                Glide.with(holder.image).clear(holder.image)

                return
            }

            holder.imageLoading.visibility = View.VISIBLE

            Glide.with(holder.image)
                .load(imageUrl)
                .thumbnail(0.25f)
                .placeholder(R.drawable.ic_bpslogo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .error(R.drawable.ic_bpslogo)
                .listener(
                    object : RequestListener<android.graphics.drawable.Drawable> {

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {

                            holder.imageLoading.visibility = View.GONE

                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: Target<android.graphics.drawable.Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {

                            holder.imageLoading.visibility = View.GONE

                            return false
                        }
                    }
                )
                .into(holder.image)
        }

        override fun onViewRecycled(
            holder: PublikasiViewHolder
        ) {
            super.onViewRecycled(holder)

            Glide.with(holder.image).clear(holder.image)

            holder.imageLoading.visibility = View.GONE

            holder.image.setImageResource(
                R.drawable.ic_bpslogo
            )
        }
    }

    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {

        if (sedangRefresh) {

            sedangRefresh = false

            (activity as? HomeActivity)?.finishSwipeRefresh()
        }

        if (::publikasiLoading.isInitialized) {
            publikasiLoading.cancelAnimation()
        }

        if (::publikasiContainer.isInitialized) {
            publikasiContainer.adapter = null
        }

        super.onDestroyView()
    }
}
