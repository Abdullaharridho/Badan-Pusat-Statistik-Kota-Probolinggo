
package com.example.bpskota.uipimpinan

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.KegiatanHarian
import com.example.bpskota.bpskp.model.PenugasanResponse
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PimpinanKegiatanFragment : Fragment() {

    companion object {
        private const val FILTER_SEMUA = "Semua"
        private const val FILTER_MANDIRI = "Mandiri"
        private const val FILTER_PENUGASAN = "Penugasan"

        private const val WARNA_ORANGE = "#F97316"
        private const val WARNA_TEKS = "#292524"
        private const val WARNA_MUTED = "#78716C"
        private const val WARNA_BACKGROUND = "#F6F2EE"
    }

    private lateinit var session: BpskpAuthSession

    private lateinit var tvJumlahKegiatanMandiri: TextView
    private lateinit var tvJumlahAktivitasPenugasan: TextView
    private lateinit var etCariKegiatan: EditText

    private lateinit var btnFilterSemua: TextView
    private lateinit var btnFilterMandiri: TextView
    private lateinit var btnFilterPenugasan: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var layoutKosong: View
    private lateinit var tvError: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: KegiatanAdapter

    private val semuaKegiatan = mutableListOf<ItemKegiatan>()

    private var filterAktif = FILTER_SEMUA
    private var pencarian = ""

    private var jumlahRequestSelesai = 0
    private var jumlahRequestGagal = 0
    private var pesanErrorTerakhir = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_pimpinan_kegiatan,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = BpskpAuthSession(requireContext())

        inisialisasiView(view)
        aturRecyclerView()
        aturFilter()
        aturPencarian()

        muatDataKegiatan()
    }

    private fun inisialisasiView(view: View) {
        tvJumlahKegiatanMandiri =
            view.findViewById(R.id.tvJumlahKegiatanMandiri)

        tvJumlahAktivitasPenugasan =
            view.findViewById(R.id.tvJumlahAktivitasPenugasan)

        etCariKegiatan =
            view.findViewById(R.id.etCariKegiatanPimpinan)

        btnFilterSemua =
            view.findViewById(R.id.btnFilterSemuaKegiatan)

        btnFilterMandiri =
            view.findViewById(R.id.btnFilterMandiri)

        btnFilterPenugasan =
            view.findViewById(R.id.btnFilterPenugasan)

        progressBar =
            view.findViewById(R.id.progressKegiatanPimpinan)

        layoutKosong =
            view.findViewById(R.id.layoutKosongKegiatanPimpinan)

        tvError =
            view.findViewById(R.id.tvErrorKegiatanPimpinan)

        recyclerView =
            view.findViewById(R.id.rvKegiatanPimpinan)
    }

    private fun aturRecyclerView() {
        adapter = KegiatanAdapter(
            data = mutableListOf(),
            onItemClick = { item ->
                tampilkanDetail(item)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(false)
    }

    private fun aturFilter() {
        btnFilterSemua.setOnClickListener {
            ubahFilter(FILTER_SEMUA)
        }

        btnFilterMandiri.setOnClickListener {
            ubahFilter(FILTER_MANDIRI)
        }

        btnFilterPenugasan.setOnClickListener {
            ubahFilter(FILTER_PENUGASAN)
        }

        perbaruiTampilanFilter()
    }

    private fun ubahFilter(filter: String) {
        filterAktif = filter
        perbaruiTampilanFilter()
        terapkanFilterDanPencarian()
    }

    private fun perbaruiTampilanFilter() {
        aturTampilanTombolFilter(
            btnFilterSemua,
            filterAktif == FILTER_SEMUA
        )

        aturTampilanTombolFilter(
            btnFilterMandiri,
            filterAktif == FILTER_MANDIRI
        )

        aturTampilanTombolFilter(
            btnFilterPenugasan,
            filterAktif == FILTER_PENUGASAN
        )
    }

    private fun aturTampilanTombolFilter(
        tombol: TextView,
        aktif: Boolean
    ) {
        tombol.setBackgroundColor(
            Color.parseColor(
                if (aktif) WARNA_ORANGE else "#FFFFFF"
            )
        )

        tombol.setTextColor(
            Color.parseColor(
                if (aktif) "#FFFFFF" else "#57534E"
            )
        )

        tombol.setTypeface(
            null,
            if (aktif) Typeface.BOLD else Typeface.NORMAL
        )

        tombol.isSelected = aktif
    }

    private fun aturPencarian() {
        etCariKegiatan.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                pencarian = s?.toString()?.trim().orEmpty()
                terapkanFilterDanPencarian()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun muatDataKegiatan() {
        val token = session.getToken()

        if (token.isNullOrBlank()) {
            tampilkanError("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        val authorization = if (token.startsWith("Bearer ")) {
            token
        } else {
            "Bearer $token"
        }

        semuaKegiatan.clear()
        adapter.updateData(emptyList())

        tvError.visibility = View.GONE
        layoutKosong.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        tvJumlahKegiatanMandiri.text = "0"
        tvJumlahAktivitasPenugasan.text = "0"

        jumlahRequestSelesai = 0
        jumlahRequestGagal = 0
        pesanErrorTerakhir = ""

        muatKegiatanMandiri(authorization)
        muatPenugasan(authorization)
    }

    private fun muatKegiatanMandiri(authorization: String) {
        BpskpRetrofitClient.api
            .getKegiatanHarian(authorization)
            .enqueue(object : Callback<com.example.bpskota.bpskp.model.KegiatanHarianListResponse> {

                override fun onResponse(
                    call: Call<com.example.bpskota.bpskp.model.KegiatanHarianListResponse>,
                    response: Response<com.example.bpskota.bpskp.model.KegiatanHarianListResponse>
                ) {
                    if (!isAdded) return

                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body?.success == true) {
                            val data = body.data.orEmpty()

                            semuaKegiatan.addAll(
                                data.map { kegiatan ->
                                    kegiatan.toItemKegiatan()
                                }
                            )
                        } else {
                            jumlahRequestGagal++
                            pesanErrorTerakhir =
                                body?.message
                                    ?: "Data kegiatan mandiri tidak dapat dibaca."
                        }
                    } else {
                        jumlahRequestGagal++
                        pesanErrorTerakhir =
                            "Gagal memuat kegiatan mandiri (${response.code()})."
                    }

                    selesaikanRequest()
                }

                override fun onFailure(
                    call: Call<com.example.bpskota.bpskp.model.KegiatanHarianListResponse>,
                    t: Throwable
                ) {
                    if (!isAdded) return

                    jumlahRequestGagal++
                    pesanErrorTerakhir =
                        t.message ?: "Koneksi kegiatan mandiri gagal."

                    selesaikanRequest()
                }
            })
    }

    private fun muatPenugasan(authorization: String) {
        BpskpRetrofitClient.api
            .getPenugasan(authorization)
            .enqueue(object : Callback<com.example.bpskota.bpskp.model.ApiResponse<List<PenugasanResponse>>> {

                override fun onResponse(
                    call: Call<com.example.bpskota.bpskp.model.ApiResponse<List<PenugasanResponse>>>,
                    response: Response<com.example.bpskota.bpskp.model.ApiResponse<List<PenugasanResponse>>>
                ) {
                    if (!isAdded) return

                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body?.success == true || body?.status == "success") {
                            val data = body.data.orEmpty()

                            semuaKegiatan.addAll(
                                data.map { penugasan ->
                                    penugasan.toItemKegiatan()
                                }
                            )
                        } else {
                            jumlahRequestGagal++
                            pesanErrorTerakhir =
                                body?.message
                                    ?: "Data penugasan tidak dapat dibaca."
                        }
                    } else {
                        jumlahRequestGagal++
                        pesanErrorTerakhir =
                            "Gagal memuat penugasan (${response.code()})."
                    }

                    selesaikanRequest()
                }

                override fun onFailure(
                    call: Call<com.example.bpskota.bpskp.model.ApiResponse<List<PenugasanResponse>>>,
                    t: Throwable
                ) {
                    if (!isAdded) return

                    jumlahRequestGagal++
                    pesanErrorTerakhir =
                        t.message ?: "Koneksi penugasan gagal."

                    selesaikanRequest()
                }
            })
    }

    private fun selesaikanRequest() {
        jumlahRequestSelesai++

        if (jumlahRequestSelesai < 2) return

        progressBar.visibility = View.GONE

        val dataMandiri = semuaKegiatan.count {
            it.kategori == FILTER_MANDIRI
        }

        val dataPenugasan = semuaKegiatan.count {
            it.kategori == FILTER_PENUGASAN
        }

        tvJumlahKegiatanMandiri.text = dataMandiri.toString()
        tvJumlahAktivitasPenugasan.text = dataPenugasan.toString()

        if (semuaKegiatan.isEmpty() && jumlahRequestGagal == 2) {
            tampilkanError(
                pesanErrorTerakhir.ifBlank {
                    "Data kegiatan belum dapat dimuat."
                }
            )
            return
        }

        tvError.visibility = View.GONE
        terapkanFilterDanPencarian()

        if (jumlahRequestGagal > 0 && semuaKegiatan.isNotEmpty()) {
            Toast.makeText(
                requireContext(),
                "Sebagian data gagal dimuat. Periksa kembali koneksi atau akses API.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun tampilkanError(pesan: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        layoutKosong.visibility = View.GONE
        tvError.text = pesan
        tvError.visibility = View.VISIBLE
    }

    private fun terapkanFilterDanPencarian() {
        val hasil = semuaKegiatan.filter { item ->
            val cocokFilter = when (filterAktif) {
                FILTER_MANDIRI -> item.kategori == FILTER_MANDIRI
                FILTER_PENUGASAN -> item.kategori == FILTER_PENUGASAN
                else -> true
            }

            val teksCari = listOf(
                item.namaPegawai,
                item.ringkasan,
                item.kategori,
                item.tanggal,
                item.jam,
                item.lokasi,
                item.nomorSurat,
                item.status
            ).joinToString(" ").lowercase()

            cocokFilter &&
                    (pencarian.isBlank() ||
                            teksCari.contains(pencarian.lowercase()))
        }

        adapter.updateData(hasil)

        val adaData = hasil.isNotEmpty()

        recyclerView.visibility =
            if (adaData) View.VISIBLE else View.GONE

        layoutKosong.visibility =
            if (!adaData && tvError.visibility != View.VISIBLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun tampilkanDetail(item: ItemKegiatan) {
        if (!isAdded) return

        val konten = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.parseColor("#FFF0E5"),
                    Color.parseColor("#FFE0C2")
                )
            ).apply {
                cornerRadius = dp(18).toFloat()
            }
        }

        header.addView(TextView(requireContext()).apply {
            text = item.kategori.uppercase()
            textSize = 10f
            setTextColor(Color.parseColor("#C2410C"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.08f
        })

        header.addView(TextView(requireContext()).apply {
            text = item.ringkasan
            textSize = 19f
            setTextColor(Color.parseColor(WARNA_TEKS))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(4))
        })

        header.addView(TextView(requireContext()).apply {
            text = "Informasi kegiatan dan penugasan pegawai"
            textSize = 12f
            setTextColor(Color.parseColor(WARNA_MUTED))
        })

        konten.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        )

        tambahDetail(konten, "PEGAWAI", item.namaPegawai)
        tambahDetail(konten, "KATEGORI", item.kategori)
        tambahDetail(konten, "TANGGAL", item.tanggal)
        tambahDetail(konten, "JAM KEGIATAN", item.jam)
        tambahDetail(konten, "LOKASI", item.lokasi)

        if (item.nomorSurat.isNotBlank()) {
            tambahDetail(konten, "NOMOR SURAT", item.nomorSurat)
        }

        if (item.pengirim.isNotBlank()) {
            tambahDetail(konten, "DITUGASKAN OLEH", item.pengirim)
        }

        if (item.penerima.isNotBlank()) {
            tambahDetail(konten, "PENERIMA PENUGASAN", item.penerima)
        }

        if (item.status.isNotBlank()) {
            tambahDetail(konten, "STATUS", item.status)
        }

        if (item.catatan.isNotBlank()) {
            tambahDetail(konten, "CATATAN", item.catatan)
        }

        val scrollView = android.widget.ScrollView(requireContext()).apply {
            isFillViewport = false
            addView(konten)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(scrollView)
            .setPositiveButton("Tutup", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor(WARNA_ORANGE))
                setTypeface(null, Typeface.BOLD)
            }
        }

        dialog.show()
    }

    private fun tambahDetail(
        parent: LinearLayout,
        label: String,
        nilai: String
    ) {
        val blok = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FAFAFA"))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#EEE7E1"))
            }
        }

        blok.addView(TextView(requireContext()).apply {
            text = label
            textSize = 10f
            letterSpacing = 0.07f
            setTextColor(Color.parseColor("#9A8172"))
            setTypeface(null, Typeface.BOLD)
        })

        blok.addView(TextView(requireContext()).apply {
            text = nilai.ifBlank { "Belum tersedia" }
            textSize = 14f
            setTextColor(Color.parseColor(WARNA_TEKS))
            setPadding(0, dp(5), 0, 0)
        })

        parent.addView(
            blok,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        )
    }

    private fun KegiatanHarian.toItemKegiatan(): ItemKegiatan {
        return ItemKegiatan(
            id = id.toLong(),
            kategori = FILTER_MANDIRI,
            namaPegawai = "Pegawai #$userId",
            ringkasan = namaKegiatan,
            tanggal = tanggal,
            jam = "${formatJam(jamMulai)} - ${formatJam(jamSelesai)}",
            lokasi = "Tidak dicantumkan",
            status = "",
            nomorSurat = "",
            pengirim = "",
            penerima = "",
            catatan = ""
        )
    }

    private fun formatJam(value: String): String {
        return value
            .takeIf { it.isNotBlank() }
            ?.take(5)
            ?: "--:--"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class ItemKegiatan(
        val id: Long,
        val kategori: String,
        val namaPegawai: String,
        val ringkasan: String,
        val tanggal: String,
        val jam: String,
        val lokasi: String,
        val status: String,
        val nomorSurat: String,
        val pengirim: String,
        val penerima: String,
        val catatan: String
    )

    private fun PenugasanResponse.toItemKegiatan(): ItemKegiatan {
        val nama = namaPenerima
            ?.takeIf { it.isNotBlank() }
            ?: ditugaskanKepada?.let { "Pegawai #$it" }
            ?: "Pegawai belum diketahui"

        val tanggalAcara = tanggalAcara
            ?.takeIf { it.isNotBlank() }
            ?: "Tanggal acara belum tersedia"

        val jamAcara = if (
            !waktuMulai.isNullOrBlank() &&
            !waktuSelesai.isNullOrBlank()
        ) {
            "${formatJam(waktuMulai)} - ${formatJam(waktuSelesai)}"
        } else {
            "Jadwal acara belum tersedia"
        }

        val lokasiAcara = lokasi
            ?.takeIf { it.isNotBlank() }
            ?: "Lokasi belum tersedia"

        return ItemKegiatan(
            id = id ?: 0L,
            kategori = FILTER_PENUGASAN,
            namaPegawai = nama,
            ringkasan = perihal
                ?.takeIf { it.isNotBlank() }
                ?: nomorSurat
                    ?.takeIf { it.isNotBlank() }
                ?: "Penugasan",
            tanggal = tanggalAcara,
            jam = jamAcara,
            lokasi = lokasiAcara,
            status = status.orEmpty(),
            nomorSurat = nomorSurat.orEmpty(),
            pengirim = namaPengirim.orEmpty(),
            penerima = namaPenerima.orEmpty(),
            catatan = catatanPenugasan.orEmpty()
        )
    }

    private inner class KegiatanAdapter(
        private val data: MutableList<ItemKegiatan>,
        private val onItemClick: (ItemKegiatan) -> Unit
    ) : RecyclerView.Adapter<KegiatanAdapter.KegiatanViewHolder>() {

        inner class KegiatanViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            val nama: TextView =
                itemView.findViewById(R.id.tvUserName)

            val ringkasan: TextView =
                itemView.findViewById(R.id.tvUserUsername)

            val kategori: TextView =
                itemView.findViewById(R.id.tvUserRole)

            val tanggal: TextView =
                itemView.findViewById(R.id.tvKegiatanTanggal)

            val jam: TextView =
                itemView.findViewById(R.id.tvKegiatanJam)

            val lokasi: TextView =
                itemView.findViewById(R.id.tvKegiatanLokasi)

            val tombolEdit: View? =
                itemView.findViewById(R.id.btnEditUser)

            val tombolHapus: View? =
                itemView.findViewById(R.id.btnDeleteUser)

            init {
                tombolEdit?.visibility = View.GONE
                tombolHapus?.visibility = View.GONE
                sembunyikanMenuTitikTiga(itemView)
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): KegiatanViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)

            return KegiatanViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: KegiatanViewHolder,
            position: Int
        ) {
            val item = data[position]

            holder.nama.text = item.namaPegawai
            holder.ringkasan.text = item.ringkasan
            holder.kategori.text = item.kategori
            holder.tanggal.text = item.tanggal
            holder.jam.text = item.jam
            holder.lokasi.text = item.lokasi

            holder.kategori.setTextColor(
                Color.parseColor(
                    if (item.kategori == FILTER_MANDIRI) {
                        "#C2410C"
                    } else {
                        "#1D4ED8"
                    }
                )
            )

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = data.size

        fun updateData(dataBaru: List<ItemKegiatan>) {
            data.clear()
            data.addAll(dataBaru)
            notifyDataSetChanged()
        }

        private fun sembunyikanMenuTitikTiga(view: View) {
            if (view is TextView && view.text.toString().trim() == "⋮") {
                view.visibility = View.GONE
                return
            }

            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    sembunyikanMenuTitikTiga(view.getChildAt(index))
                }
            }
        }
    }
}
