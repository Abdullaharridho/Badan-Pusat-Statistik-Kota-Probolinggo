package com.example.bpskota

import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import androidx.fragment.app.Fragment
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

class PublikasiFragment : Fragment() {

    companion object {
        private const val TAG = "PublikasiFragment"
    }

    private val API_KEY = "008edaaae5d450b1913b31a2cef618c3"
    private val DOMAIN = "3574"

    private val semuaPublikasi = mutableListOf<Publikasi>()

    private var tahunTerpilih: String? = null
    private var kataKunci: String = ""

    private lateinit var publikasiContainer: LinearLayout
    private lateinit var tvJumlahPublikasi: TextView
    private lateinit var etSearchPublikasi: EditText
    private lateinit var btnFilterPublikasi: View
    private lateinit var publikasiLoading: LottieAnimationView
    private lateinit var cardInfoPublikasi: View

    private val repository = BpsRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")

        return inflater.inflate(
            R.layout.fragment_publikasi,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated")

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
        cardInfoPublikasi =
            view.findViewById(R.id.cardInfoPublikasi)

        mulaiLoading()

        setupSearch()
        setupFilter()

        loadSemuaPublikasi()
    }

    private fun mulaiLoading() {

        Log.d(TAG, "Memulai loading publikasi")

        publikasiLoading.visibility = View.VISIBLE
        cardInfoPublikasi.visibility = View.GONE

        publikasiLoading.setAnimation(
            "Loading_Animation.json"
        )

        publikasiLoading.repeatCount =
            LottieDrawable.INFINITE

        publikasiLoading.playAnimation()

        publikasiContainer.visibility =
            View.GONE

        tvJumlahPublikasi.text =
            "Memuat..."
    }

    private fun selesaiLoading() {

        Log.d(
            TAG,
            "Loading publikasi selesai. Total data = ${semuaPublikasi.size}"
        )

        publikasiLoading.cancelAnimation()
        publikasiLoading.visibility =
            View.GONE
        cardInfoPublikasi.visibility =
            View.VISIBLE

        publikasiContainer.visibility =
            View.VISIBLE
    }

    private fun setupSearch() {

        etSearchPublikasi.addTextChangedListener(
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

                    kataKunci =
                        s?.toString()?.trim() ?: ""

                    Log.d(
                        TAG,
                        "Pencarian berubah: '$kataKunci'"
                    )

                    tampilkanHasil()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupFilter() {

        btnFilterPublikasi.setOnClickListener {

            Log.d(
                TAG,
                "Tombol filter tahun diklik"
            )

            tampilkanDialogTahun()
        }
    }

    private fun loadSemuaPublikasi() {

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "Mulai mengambil semua publikasi"
        )

        Log.d(
            TAG,
            "DOMAIN = $DOMAIN"
        )

        semuaPublikasi.clear()

        Log.d(
            TAG,
            "Meminta halaman 1..."
        )

        repository.getPublikasi(
            page = 1,
            domain = DOMAIN,
            apiKey = API_KEY
        ) { response ->

            if (!isAdded) {
                Log.d(
                    TAG,
                    "Fragment sudah tidak terpasang"
                )
                return@getPublikasi
            }

            if (response == null) {

                Log.e(
                    TAG,
                    "Response halaman 1 = NULL"
                )

                gagalLoadPublikasi()
                return@getPublikasi
            }

            Log.d(
                TAG,
                "Response halaman 1 diterima"
            )

            Log.d(
                TAG,
                "Status = ${response.status}"
            )

            Log.d(
                TAG,
                "Data availability = ${response.dataAvailability}"
            )

            val pagination =
                response.getPagination()

            Log.d(
                TAG,
                "Pagination = $pagination"
            )

            val publikasiHalamanPertama =
                response.getPublikasi()

            Log.d(
                TAG,
                "Jumlah publikasi halaman 1 = ${publikasiHalamanPertama.size}"
            )

            publikasiHalamanPertama.forEachIndexed { index, publikasi ->

                Log.d(
                    TAG,
                    "PAGE 1 [$index] " +
                            "pub_id=${publikasi.pub_id}, " +
                            "title=${publikasi.title}, " +
                            "rl_date=${publikasi.rl_date}, " +
                            "cover=${publikasi.cover}"
                )
            }

            semuaPublikasi.addAll(
                publikasiHalamanPertama
            )

            val totalPage =
                pagination?.pages ?: 1

            Log.d(
                TAG,
                "Total halaman = $totalPage"
            )

            Log.d(
                TAG,
                "Total data sementara = ${semuaPublikasi.size}"
            )

            if (totalPage <= 1) {

                Log.d(
                    TAG,
                    "Hanya ada 1 halaman"
                )

                selesaiLoadSemuaPublikasi()

            } else {

                Log.d(
                    TAG,
                    "Melanjutkan ke halaman 2 sampai $totalPage"
                )

                loadHalamanBerikutnya(
                    halaman = 2,
                    totalPage = totalPage
                )
            }
        }
    }

    private fun loadHalamanBerikutnya(
        halaman: Int,
        totalPage: Int
    ) {

        if (halaman > totalPage) {

            Log.d(
                TAG,
                "Semua halaman sudah selesai dimuat"
            )

            selesaiLoadSemuaPublikasi()
            return
        }

        Log.d(
            TAG,
            "Meminta halaman $halaman dari $totalPage..."
        )

        repository.getPublikasi(
            page = halaman,
            domain = DOMAIN,
            apiKey = API_KEY
        ) { response ->

            if (!isAdded) {

                Log.d(
                    TAG,
                    "Fragment sudah tidak terpasang saat halaman $halaman"
                )

                return@getPublikasi
            }

            if (response == null) {

                Log.e(
                    TAG,
                    "Response halaman $halaman = NULL"
                )

            } else {

                Log.d(
                    TAG,
                    "Response halaman $halaman diterima"
                )

                Log.d(
                    TAG,
                    "Status halaman $halaman = ${response.status}"
                )

                val publikasi =
                    response.getPublikasi()

                Log.d(
                    TAG,
                    "Jumlah publikasi halaman $halaman = ${publikasi.size}"
                )

                publikasi.forEachIndexed { index, item ->

                    Log.d(
                        TAG,
                        "PAGE $halaman [$index] " +
                                "pub_id=${item.pub_id}, " +
                                "title=${item.title}, " +
                                "rl_date=${item.rl_date}"
                    )
                }

                semuaPublikasi.addAll(
                    publikasi
                )

                Log.d(
                    TAG,
                    "Total data setelah halaman $halaman = ${semuaPublikasi.size}"
                )
            }

            loadHalamanBerikutnya(
                halaman = halaman + 1,
                totalPage = totalPage
            )
        }
    }

    private fun selesaiLoadSemuaPublikasi() {

        if (!isAdded) return

        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "Selesai mengambil semua halaman"
        )

        Log.d(
            TAG,
            "Total data sebelum distinct = ${semuaPublikasi.size}"
        )

        val publikasiUnik =
            semuaPublikasi.distinctBy {
                it.pub_id
            }

        Log.d(
            TAG,
            "Total data setelah distinct = ${publikasiUnik.size}"
        )

        semuaPublikasi.clear()
        semuaPublikasi.addAll(
            publikasiUnik
        )

        semuaPublikasi.forEachIndexed { index, publikasi ->

            Log.d(
                TAG,
                "DATA FINAL [$index] " +
                        "pub_id=${publikasi.pub_id}, " +
                        "title=${publikasi.title}, " +
                        "tahun=${ambilTahun(publikasi)}, " +
                        "cover=${publikasi.cover}"
            )
        }

        selesaiLoading()

        tampilkanHasil()

        Log.d(
            TAG,
            "========================================"
        )
    }

    private fun gagalLoadPublikasi() {

        if (!isAdded) return

        Log.e(
            TAG,
            "Gagal memuat data publikasi"
        )

        publikasiLoading.cancelAnimation()
        publikasiLoading.visibility =
            View.GONE

        publikasiContainer.visibility =
            View.VISIBLE

        publikasiContainer.removeAllViews()

        tvJumlahPublikasi.text =
            "Gagal memuat"

        Toast.makeText(
            requireContext(),
            "Gagal memuat data publikasi",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun tampilkanHasil() {

        val hasil =
            semuaPublikasi
                .filter { publikasi ->

                    val sesuaiTahun =
                        tahunTerpilih == null ||
                                ambilTahun(publikasi) ==
                                tahunTerpilih

                    val sesuaiPencarian =
                        kataKunci.isEmpty() ||
                                publikasi.title
                                    ?.contains(
                                        kataKunci,
                                        ignoreCase = true
                                    ) == true

                    sesuaiTahun &&
                            sesuaiPencarian
                }
                .distinctBy {
                    it.pub_id
                }

        Log.d(
            TAG,
            "Filter hasil: " +
                    "tahun=$tahunTerpilih, " +
                    "keyword='$kataKunci', " +
                    "hasil=${hasil.size}"
        )

        publikasiContainer.removeAllViews()

        tvJumlahPublikasi.text =
            "${hasil.size} publikasi"

        hasil.forEach { publikasi ->

            tambahCardPublikasi(
                publikasiContainer,
                publikasi
            )
        }
    }

    private fun ambilTahun(
        publikasi: Publikasi
    ): String? {

        val tanggal =
            publikasi.rl_date
                ?: publikasi.sch_date

        return tanggal
            ?.takeIf {
                it.length >= 4
            }
            ?.substring(0, 4)
    }

    private fun tampilkanDialogTahun() {

        val tahun =
            semuaPublikasi
                .mapNotNull {
                    ambilTahun(it)
                }
                .distinct()
                .sortedDescending()

        Log.d(
            TAG,
            "Daftar tahun tersedia = $tahun"
        )

        if (tahun.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Tahun publikasi belum tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pilihan =
            mutableListOf("Semua Tahun")

        pilihan.addAll(tahun)

        val posisiTerpilih =
            if (tahunTerpilih == null) {

                0

            } else {

                pilihan.indexOf(
                    tahunTerpilih
                ).takeIf {
                    it >= 0
                } ?: 0
            }

        androidx.appcompat.app.AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Filter Tahun")
            .setSingleChoiceItems(
                pilihan.toTypedArray(),
                posisiTerpilih
            ) { dialog, which ->

                tahunTerpilih =
                    if (which == 0) {
                        null
                    } else {
                        pilihan[which]
                    }

                Log.d(
                    TAG,
                    "Tahun dipilih = $tahunTerpilih"
                )

                tampilkanHasil()

                dialog.dismiss()
            }
            .show()
    }

    private fun tambahCardPublikasi(
        container: LinearLayout,
        publikasi: Publikasi
    ) {

        Log.d(
            TAG,
            "Membuat card publikasi: " +
                    "pub_id=${publikasi.pub_id}, " +
                    "title=${publikasi.title}"
        )

        val card =
            LayoutInflater.from(
                requireContext()
            ).inflate(
                R.layout.item_home_panjang,
                container,
                false
            )

        val image =
            card.findViewById<ImageView>(
                R.id.imgStatistik
            )

        val imageLoading =
            card.findViewById<ProgressBar>(
                R.id.imageLoading
            )

        val title =
            card.findViewById<TextView>(
                R.id.tvStatistikTitle
            )

        val description =
            card.findViewById<TextView>(
                R.id.tvStatistikDescription
            )

        title.text =
            publikasi.title
                ?: "Publikasi"

        description.text =""

        imageLoading.visibility =
            View.VISIBLE

        Log.d(
            TAG,
            "Load cover: pub_id=${publikasi.pub_id}, url=${publikasi.cover}"
        )

        Glide.with(this)
            .load(publikasi.cover)
            .placeholder(
                R.drawable.ic_bpslogo
            )
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(
                R.drawable.ic_bpslogo
            )
            .listener(
                object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {

                        Log.e(
                            TAG,
                            "Gagal load cover: " +
                                    "pub_id=${publikasi.pub_id}, " +
                                    "url=${publikasi.cover}",
                            e
                        )

                        imageLoading.visibility =
                            View.GONE

                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {

                        Log.d(
                            TAG,
                            "Cover berhasil dimuat: " +
                                    "pub_id=${publikasi.pub_id}"
                        )

                        imageLoading.visibility =
                            View.GONE

                        return false
                    }
                }
            )
            .into(image)

        card.setOnClickListener {

            Log.d(
                TAG,
                "Card publikasi diklik: " +
                        "pub_id=${publikasi.pub_id}"
            )

            tampilkanPopupPublikasi(
                publikasi
            )
        }

        container.addView(card)
    }
    private fun tampilkanPopupPublikasi(
        publikasi: Publikasi
    ) {

        Log.d(
            TAG,
            "Membuka popup publikasi: pub_id=${publikasi.pub_id}"
        )

        val dialog = Dialog(requireContext())

        dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        val view = LayoutInflater.from(
            requireContext()
        ).inflate(
            R.layout.dialog_detail_home,
            null,
            false
        )

        // =========================
        // FIND VIEW
        // =========================

        val image =
            view.findViewById<ImageView>(
                R.id.imgDetailHome
            )

        val title =
            view.findViewById<TextView>(
                R.id.tvDetailHomeTitle
            )

        val date =
            view.findViewById<TextView>(
                R.id.tvDetailHomeDate
            )

        val description =
            view.findViewById<TextView>(
                R.id.tvDetailHomeDescription
            )

        // LinearLayout khusus tombol download
        val containerDownload =
            view.findViewById<LinearLayout>(
                R.id.containerDownload
            )


        // =========================
        // SET DATA AWAL
        // =========================

        title.text =
            publikasi.title
                ?: "Publikasi"

        date.text =
            ambilTahun(publikasi)
                ?: "-"

        description.text =
            "Memuat deskripsi..."


        // =========================
        // LOAD COVER
        // =========================

        Glide.with(this)
            .load(publikasi.cover)
            .placeholder(
                R.drawable.ic_bpslogo
            )
            .error(
                R.drawable.ic_bpslogo
            )
            .into(image)


        // =========================
        // TOMBOL DOWNLOAD
        // =========================

        val tombolDownload =
            TextView(requireContext()).apply {

                text = "Download PDF"

                textSize = 14f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.DEFAULT_BOLD

                setPadding(
                    20,
                    14,
                    20,
                    14
                )

                setBackgroundColor(
                    Color.parseColor("#F97316")
                )

                isClickable = true
                isFocusable = true

                setOnClickListener {

                    val pdfUrl =
                        publikasi.pdf

                    // =========================
                    // CEK PDF
                    // =========================

                    if (pdfUrl.isNullOrBlank()) {

                        Toast.makeText(
                            requireContext(),
                            "File PDF tidak tersedia",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }


                    // =========================
                    // NAMA FILE
                    // =========================

                    val namaPublikasi =
                        publikasi.title
                            ?.trim()
                            ?.replace(
                                Regex("[\\\\/:*?\"<>|]"),
                                "_"
                            )
                            ?.replace(
                                Regex("\\s+"),
                                " "
                            )
                            ?.take(150)
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Publikasi"


                    // =========================
                    // KONFIRMASI DOWNLOAD
                    // =========================

                    androidx.appcompat.app.AlertDialog.Builder(
                        requireContext()
                    )
                        .setTitle(
                            "Download Publikasi"
                        )
                        .setMessage(
                            "Yakin ingin mendownload publikasi:\n\n$namaPublikasi?"
                        )
                        .setNegativeButton(
                            "Batal",
                            null
                        )
                        .setPositiveButton(
                            "Download"
                        ) { _, _ ->

                            try {

                                // =========================
                                // DOWNLOAD MANAGER
                                // =========================

                                val request =
                                    DownloadManager.Request(
                                        Uri.parse(pdfUrl)
                                    )

                                request.setTitle(
                                    namaPublikasi
                                )

                                request.setDescription(
                                    "Mengunduh publikasi PDF..."
                                )

                                request.setNotificationVisibility(
                                    DownloadManager
                                        .Request
                                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                )

                                request.setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    "$namaPublikasi.pdf"
                                )

                                request.setMimeType(
                                    "application/pdf"
                                )

                                request.setAllowedOverMetered(
                                    true
                                )

                                request.setAllowedOverRoaming(
                                    true
                                )


                                // =========================
                                // DOWNLOAD MANAGER SERVICE
                                // =========================

                                val downloadManager =
                                    requireContext()
                                        .getSystemService(
                                            Context.DOWNLOAD_SERVICE
                                        ) as DownloadManager


                                downloadManager.enqueue(
                                    request
                                )


                                Toast.makeText(
                                    requireContext(),
                                    "Download dimulai",
                                    Toast.LENGTH_SHORT
                                ).show()


                                Log.d(
                                    TAG,
                                    "Download PDF dimulai: $pdfUrl"
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Gagal memulai download PDF",
                                    e
                                )

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


        // =========================
        // MASUKKAN BUTTON KE CONTAINER
        // =========================

        containerDownload.addView(
            tombolDownload,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )


        // =========================
        // TAMPILKAN DIALOG
        // =========================

        dialog.setContentView(
            view
        )

        dialog.show()


        // =========================
        // STYLE DIALOG
        // =========================

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        // =========================
        // AMBIL DETAIL PUBLIKASI
        // =========================

        Log.d(
            TAG,
            "Meminta detail publikasi: pub_id=${publikasi.pub_id}"
        )

        val pubId =
            publikasi.pub_id


        // =========================
        // CEK PUB ID
        // =========================

        if (pubId.isNullOrBlank()) {

            Log.e(
                TAG,
                "pub_id kosong, tidak bisa mengambil detail"
            )

            description.text =
                "Deskripsi tidak tersedia"

            return
        }


        // =========================
        // REQUEST DETAIL
        // =========================

        repository.getPublikasiDetail(
            id = pubId,
            domain = DOMAIN,
            apiKey = API_KEY
        ) { response ->

            if (!isAdded) {
                return@getPublikasiDetail
            }


            // =========================
            // RESPONSE NULL
            // =========================

            if (response == null) {

                Log.e(
                    TAG,
                    "Response detail publikasi = NULL"
                )

                description.text =
                    "Gagal memuat deskripsi"

                return@getPublikasiDetail
            }


            Log.d(
                TAG,
                "Response detail diterima: pub_id=$pubId"
            )

            Log.d(
                TAG,
                "Status detail = ${response.status}"
            )


            // =========================
            // AMBIL ABSTRACT
            // =========================

            val abstractText =
                response.data?.abstract


            Log.d(
                TAG,
                "Abstract detail = $abstractText"
            )


            // =========================
            // BERSIHKAN HTML
            // =========================

            description.text =
                abstractText
                    ?.replace(
                        Regex(
                            "<br\\s*/?>",
                            RegexOption.IGNORE_CASE
                        ),
                        "\n"
                    )
                    ?.replace(
                        Regex(
                            "<[^>]*>"
                        ),
                        ""
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: "Deskripsi tidak tersedia"
        }
    }

    override fun onDestroyView() {

        Log.d(
            TAG,
            "onDestroyView"
        )

        publikasiLoading.cancelAnimation()


        super.onDestroyView()
    }
}