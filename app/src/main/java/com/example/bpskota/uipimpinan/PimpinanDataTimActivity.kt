
package com.example.bpskota.uipimpinan

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.Tim
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.bpskp.repository.BpskpAuthSession

class PimpinanDataTimActivity : AppCompatActivity() {

    private lateinit var repository: BpskpRepository
    private lateinit var authSession: BpskpAuthSession

    private lateinit var tvTotalTim: TextView
    private lateinit var tvTotalAnggota: TextView
    private lateinit var etCariTim: EditText
    private lateinit var btnBersihkanCariTim: View
    private lateinit var tvSubtitleDaftarTim: TextView
    private lateinit var tvJumlahTim: TextView
    private lateinit var rvDataTim: RecyclerView

    private lateinit var layoutLoadingTim: View
    private lateinit var layoutTimKosong: View
    private lateinit var tvPesanTimKosong: TextView
    private lateinit var layoutErrorTim: View
    private lateinit var tvErrorTim: TextView
    private lateinit var btnCobaLagiTim: View

    private lateinit var adapter: TimAdapter

    private var semuaTim: List<Tim> = emptyList()
    private var pencarian = ""

    private val orange = Color.rgb(249, 115, 22)
    private val teksUtama = Color.rgb(38, 38, 38)
    private val teksSekunder = Color.rgb(117, 117, 117)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pimpinan_data_tim)

        repository = BpskpRepository()
        authSession = BpskpAuthSession(this)

        inisialisasiView()
        aturRecyclerView()
        aturPencarian()
        aturTombol()

        muatDataTim()
    }

    private fun inisialisasiView() {
        tvTotalTim = findViewById(R.id.tvTotalTim)
        tvTotalAnggota = findViewById(R.id.tvTotalAnggota)
        etCariTim = findViewById(R.id.etCariTim)
        btnBersihkanCariTim = findViewById(R.id.btnBersihkanCariTim)
        tvSubtitleDaftarTim = findViewById(R.id.tvSubtitleDaftarTim)
        tvJumlahTim = findViewById(R.id.tvJumlahTim)
        rvDataTim = findViewById(R.id.rvDataTim)

        layoutLoadingTim = findViewById(R.id.layoutLoadingTim)
        layoutTimKosong = findViewById(R.id.layoutTimKosong)
        tvPesanTimKosong = findViewById(R.id.tvPesanTimKosong)
        layoutErrorTim = findViewById(R.id.layoutErrorTim)
        tvErrorTim = findViewById(R.id.tvErrorTim)
        btnCobaLagiTim = findViewById(R.id.btnCobaLagiTim)
    }

    private fun aturRecyclerView() {
        adapter = TimAdapter()

        rvDataTim.layoutManager = LinearLayoutManager(this)
        rvDataTim.adapter = adapter
        rvDataTim.setHasFixedSize(false)
    }

    private fun aturPencarian() {
        etCariTim.addTextChangedListener(object : TextWatcher {

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

                btnBersihkanCariTim.visibility =
                    if (pencarian.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                tampilkanHasilFilter()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun aturTombol() {
        btnBersihkanCariTim.setOnClickListener {
            etCariTim.setText("")
        }

        btnCobaLagiTim.setOnClickListener {
            muatDataTim()
        }
    }

    // =========================================================
    // MEMUAT DATA TIM DARI API
    // =========================================================

    private fun muatDataTim() {
        tampilkanLoading()

        repository.getTim(
            session = authSession,
            search = null,
            status = null
        ) { data, error ->

            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (error != null) {
                    tampilkanError(error)
                    return@runOnUiThread
                }

                semuaTim = data.orEmpty()

                tvTotalTim.text = semuaTim.size.toString()

                // Aman apabila properti anggota bernilai null.
                val totalAnggota = semuaTim.sumOf { tim ->
                    tim.anggota.orEmpty().size
                }

                tvTotalAnggota.text = totalAnggota.toString()

                tvSubtitleDaftarTim.text =
                    "Daftar tim dan anggota yang terdaftar"

                tampilkanHasilFilter()
            }
        }
    }

    // =========================================================
    // PENCARIAN DAN FILTER
    // =========================================================

    private fun tampilkanHasilFilter() {
        val kataKunci = pencarian.trim()

        val hasil = if (kataKunci.isBlank()) {
            semuaTim
        } else {
            semuaTim.filter { tim ->

                val cocokNamaTim =
                    tim.namaTim.contains(
                        kataKunci,
                        ignoreCase = true
                    )

                val cocokKetua =
                    tim.namaKetua.orEmpty().contains(
                        kataKunci,
                        ignoreCase = true
                    )

                val cocokKeterangan =
                    tim.keterangan.orEmpty().contains(
                        kataKunci,
                        ignoreCase = true
                    )

                val cocokAnggota = tim.anggota.orEmpty().any { anggota ->
                    anggota.nama.contains(
                        kataKunci,
                        ignoreCase = true
                    ) || anggota.role.contains(
                        kataKunci,
                        ignoreCase = true
                    )
                }

                cocokNamaTim ||
                        cocokKetua ||
                        cocokKeterangan ||
                        cocokAnggota
            }
        }

        adapter.submitList(hasil)

        tvJumlahTim.text = "${hasil.size} tim"

        layoutLoadingTim.visibility = View.GONE
        layoutErrorTim.visibility = View.GONE

        if (hasil.isEmpty()) {
            rvDataTim.visibility = View.GONE
            layoutTimKosong.visibility = View.VISIBLE

            tvPesanTimKosong.text = if (kataKunci.isBlank()) {
                "Belum ada data tim yang tersedia."
            } else {
                "Tim atau anggota dengan kata kunci \"$kataKunci\" tidak ditemukan."
            }
        } else {
            layoutTimKosong.visibility = View.GONE
            rvDataTim.visibility = View.VISIBLE
        }
    }

    private fun tampilkanLoading() {
        layoutLoadingTim.visibility = View.VISIBLE
        rvDataTim.visibility = View.GONE
        layoutTimKosong.visibility = View.GONE
        layoutErrorTim.visibility = View.GONE
    }

    private fun tampilkanError(pesan: String) {
        layoutLoadingTim.visibility = View.GONE
        rvDataTim.visibility = View.GONE
        layoutTimKosong.visibility = View.GONE
        layoutErrorTim.visibility = View.VISIBLE

        tvErrorTim.text = pesan
    }

    // =========================================================
    // ADAPTER KARTU TIM
    // Klik kartu langsung membuka popup detail.
    // =========================================================

    private inner class TimAdapter :
        RecyclerView.Adapter<TimAdapter.TimViewHolder>() {

        private var daftarTim: List<Tim> = emptyList()

        fun submitList(data: List<Tim>) {
            daftarTim = data
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TimViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)

            return TimViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: TimViewHolder,
            position: Int
        ) {
            holder.bind(daftarTim[position])
        }

        override fun getItemCount(): Int = daftarTim.size

        inner class TimViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {

            private val tvNama: TextView =
                itemView.findViewById(R.id.tvUserName)

            private val tvUsername: TextView =
                itemView.findViewById(R.id.tvUserUsername)

            private val tvRole: TextView =
                itemView.findViewById(R.id.tvUserRole)

            private val tvTanggal: TextView =
                itemView.findViewById(R.id.tvKegiatanTanggal)

            private val tvJam: TextView =
                itemView.findViewById(R.id.tvKegiatanJam)

            private val tvLokasi: TextView =
                itemView.findViewById(R.id.tvKegiatanLokasi)

            private val btnEdit: View =
                itemView.findViewById(R.id.btnEditUser)

            private val btnHapus: View =
                itemView.findViewById(R.id.btnDeleteUser)

            init {
                // Halaman pimpinan hanya untuk melihat data.
                btnEdit.visibility = View.GONE
                btnHapus.visibility = View.GONE

                itemView.isClickable = true
                itemView.isFocusable = true

                // Tidak membutuhkan tombol detail.
                itemView.setOnClickListener {
                    val posisi = adapterPosition

                    if (posisi != RecyclerView.NO_POSITION) {
                        tampilkanDetailTim(daftarTim[posisi])
                    }
                }
            }

            fun bind(tim: Tim) {
                val anggota = tim.anggota.orEmpty()

                tvNama.text = tim.namaTim

                tvUsername.text = "Ketua: ${
                    tim.namaKetua
                        ?.takeIf { it.isNotBlank() }
                        ?: "Belum tersedia"
                }"

                tvRole.text = when (tim.status.lowercase()) {
                    "aktif" -> "TIM AKTIF"
                    "nonaktif" -> "TIM NONAKTIF"
                    else -> tim.status.uppercase()
                }

                tvTanggal.text = "Anggota: ${anggota.size} orang"

                tvJam.text = tim.keterangan
                    ?.takeIf { it.isNotBlank() }
                    ?: "Tidak ada keterangan"

                tvLokasi.text = anggota
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { it.nama }
                    ?: "Belum ada anggota"
            }
        }
    }

    // =========================================================
    // POPUP DETAIL TIM
    // =========================================================

    private fun tampilkanDetailTim(tim: Tim) {
        val dialog = Dialog(this)

        val anggotaTim = tim.anggota.orEmpty()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        // Header popup
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))

            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.rgb(255, 239, 220),
                    Color.rgb(255, 248, 240)
                )
            ).apply {
                cornerRadii = floatArrayOf(
                    dp(22).toFloat(), dp(22).toFloat(),
                    dp(22).toFloat(), dp(22).toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
        }

        header.addView(TextView(this).apply {
            text = "DETAIL TIM"
            textSize = 11f
            setTextColor(orange)
            setTypeface(null, Typeface.BOLD)
        })

        header.addView(TextView(this).apply {
            text = tim.namaTim
            textSize = 21f
            setTextColor(teksUtama)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(4))
        })

        header.addView(TextView(this).apply {
            text = "Informasi ketua dan anggota tim"
            textSize = 13f
            setTextColor(teksSekunder)
        })

        container.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )

        fun tambahLabel(teks: String) {
            container.addView(TextView(this).apply {
                text = teks.uppercase()
                textSize = 11f
                setTextColor(teksSekunder)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(10), 0, dp(6))
            })
        }

        fun tambahIsi(teks: String) {
            container.addView(TextView(this).apply {
                text = teks
                textSize = 14f
                setTextColor(teksUtama)
                setPadding(dp(14), dp(12), dp(14), dp(12))

                background = GradientDrawable().apply {
                    setColor(Color.rgb(250, 250, 250))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.rgb(235, 235, 235))
                }
            })
        }

        tambahLabel("Ketua Tim")
        tambahIsi(
            tim.namaKetua
                ?.takeIf { it.isNotBlank() }
                ?: "Belum tersedia"
        )

        tambahLabel("Status Tim")
        tambahIsi(
            tim.status
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
        )

        tambahLabel("Keterangan")
        tambahIsi(
            tim.keterangan
                ?.takeIf { it.isNotBlank() }
                ?: "Tidak ada keterangan"
        )

        tambahLabel("Daftar Anggota (${anggotaTim.size})")

        val daftarAnggota = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(4))

            background = GradientDrawable().apply {
                setColor(Color.rgb(250, 250, 250))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(235, 235, 235))
            }
        }

        if (anggotaTim.isEmpty()) {
            daftarAnggota.addView(TextView(this).apply {
                text = "Belum ada anggota yang terdaftar."
                textSize = 13f
                setTextColor(teksSekunder)
                setPadding(0, dp(12), 0, dp(12))
            })
        } else {
            anggotaTim.forEachIndexed { index, anggota ->

                val itemAnggota = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(12), 0, dp(12))
                }

                itemAnggota.addView(TextView(this).apply {
                    text = anggota.nama
                    textSize = 14f
                    setTextColor(teksUtama)
                    setTypeface(null, Typeface.BOLD)
                })

                itemAnggota.addView(TextView(this).apply {
                    text = formatRole(anggota.role)
                    textSize = 12f
                    setTextColor(teksSekunder)
                    setPadding(0, dp(3), 0, 0)
                })

                daftarAnggota.addView(itemAnggota)

                if (index < anggotaTim.lastIndex) {
                    daftarAnggota.addView(
                        View(this).apply {
                            setBackgroundColor(
                                Color.rgb(235, 235, 235)
                            )
                        },
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(1)
                        )
                    )
                }
            }
        }

        container.addView(daftarAnggota)

        // Tombol tutup popup.
        val btnTutup = TextView(this).apply {
            text = "TUTUP"
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(16), dp(13), dp(16), dp(13))

            background = GradientDrawable().apply {
                setColor(orange)
                cornerRadius = dp(12).toFloat()
            }

            setOnClickListener {
                dialog.dismiss()
            }
        }

        container.addView(
            btnTutup,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        )

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(container)
        }

        dialog.setContentView(scrollView)
        dialog.setCancelable(true)

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            (resources.displayMetrics.heightPixels * 0.80).toInt()
        )
    }

    private fun formatRole(role: String): String {
        return role
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { kata ->
                kata.replaceFirstChar { it.uppercase() }
            }
    }

    private fun dp(nilai: Int): Int {
        return (nilai * resources.displayMetrics.density).toInt()
    }
}
