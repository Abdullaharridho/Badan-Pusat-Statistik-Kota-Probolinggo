package com.example.bpskota

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bpskota.bps.wilayah.WilayahItem
import com.example.bpskota.bps.wilayah.WilayahResponse
import com.example.bpskota.bps.wilayah.WilayahRetrofitClient
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MasterwilayahActivity : AppCompatActivity() {

    companion object {
        // Kode Kemendagri Kota Probolinggo
        private const val KODE_KOTA = "35.74"

        private const val ORANGE = "#F97316"
        private const val BACKGROUND = "#FAFAFA"
        private const val TEXT_PRIMARY = "#292524"
        private const val TEXT_SECONDARY = "#78716C"
        private const val BORDER = "#E7E5E4"
    }

    private lateinit var tvTotalKecamatan: TextView
    private lateinit var listKecamatan: LinearLayout
    private lateinit var activityTracker: ActivityTracker

    // Menyimpan data kelurahan yang sudah berhasil diambil.
    // Jika kecamatan dibuka kembali, data tidak perlu diminta ulang.
    private val cacheKelurahan =
        mutableMapOf<String, List<WilayahItem>>()

    // Mencegah request kelurahan yang sama berjalan berulang.
    private val loadingCodes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_masterwilayah)

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        activityTracker.trackScreen(
            screen = "Masterwilayah"
        )

        findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        tvTotalKecamatan =
            findViewById(R.id.tvTotalKecamatan)

        listKecamatan =
            findViewById(R.id.listKecamatan)

        loadKecamatan()
    }

    /**
     * Mengambil kecamatan Kota Probolinggo
     * dari Wilayah.id.
     */
    private fun loadKecamatan() {
        tvTotalKecamatan.text = "Memuat kecamatan..."
        listKecamatan.removeAllViews()

        WilayahRetrofitClient.api
            .getKecamatan(KODE_KOTA)
            .enqueue(object : Callback<WilayahResponse> {

                override fun onResponse(
                    call: Call<WilayahResponse>,
                    response: Response<WilayahResponse>
                ) {
                    if (isFinishing || isDestroyed) return

                    if (!response.isSuccessful) {
                        tvTotalKecamatan.text =
                            "Gagal memuat kecamatan"

                        showError(
                            "Gagal mengambil kecamatan " +
                                    "(${response.code()})"
                        )
                        return
                    }

                    val kecamatanList =
                        response.body()?.data

                    if (kecamatanList == null) {
                        tvTotalKecamatan.text =
                            "Data kecamatan tidak tersedia"

                        showError(
                            "Respons API kecamatan kosong"
                        )
                        return
                    }

                    val dataValid = kecamatanList
                        .filter {
                            it.code.isNotBlank() &&
                                    it.name.isNotBlank()
                        }

                    if (dataValid.isEmpty()) {
                        tvTotalKecamatan.text =
                            "0 Kecamatan"

                        tampilkanPesan(
                            listKecamatan,
                            "Data kecamatan tidak ditemukan"
                        )
                        return
                    }

                    tampilkanKecamatan(dataValid)
                }

                override fun onFailure(
                    call: Call<WilayahResponse>,
                    t: Throwable
                ) {
                    if (isFinishing || isDestroyed) return

                    tvTotalKecamatan.text =
                        "Gagal memuat kecamatan"

                    showError(
                        "Koneksi Wilayah.id gagal: " +
                                "${t.message ?: "Kesalahan jaringan"}"
                    )
                }
            })
    }

    /**
     * Menampilkan seluruh kartu kecamatan.
     *
     * Setiap kecamatan memiliki wadah kelurahan
     * tepat di bawah kartu kecamatan tersebut.
     */
    private fun tampilkanKecamatan(
        kecamatanList: List<WilayahItem>
    ) {
        listKecamatan.removeAllViews()

        tvTotalKecamatan.text =
            "${kecamatanList.size} Kecamatan"

        kecamatanList.forEach { kecamatan ->

            val card = LayoutInflater.from(this)
                .inflate(
                    R.layout.item_statistik,
                    listKecamatan,
                    false
                )

            val tvNama = card.findViewById<TextView>(
                R.id.tvJudul
            )

            val tvKode = card.findViewById<TextView>(
                R.id.tvKode
            )

            tvNama.text = kecamatan.name
            tvKode.visibility = View.GONE

            // Wadah kelurahan untuk kecamatan ini.
            val containerKelurahan = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE

                setPadding(
                    dp(12),
                    dp(4),
                    dp(4),
                    dp(12)
                )
            }

            // Kartu kecamatan tetap memakai item_statistik.
            listKecamatan.addView(card)

            // Wadah kelurahan ditambahkan sesudah kartu.
            listKecamatan.addView(containerKelurahan)

            card.setOnClickListener {

                if (
                    containerKelurahan.visibility ==
                    View.VISIBLE
                ) {
                    containerKelurahan.visibility =
                        View.GONE
                    return@setOnClickListener
                }

                containerKelurahan.visibility =
                    View.VISIBLE

                val kodeKecamatan = kecamatan.code

                // Gunakan data yang sudah tersimpan jika ada.
                val dataTersimpan =
                    cacheKelurahan[kodeKecamatan]

                if (dataTersimpan != null) {
                    tampilkanKelurahan(
                        containerKelurahan,
                        dataTersimpan
                    )
                    return@setOnClickListener
                }

                // Jangan membuat request ganda.
                if (loadingCodes.contains(kodeKecamatan)) {
                    return@setOnClickListener
                }

                loadKelurahan(
                    kodeKecamatan,
                    containerKelurahan
                )
            }
        }
    }

    /**
     * Mengambil kelurahan berdasarkan kode kecamatan.
     *
     * Endpoint:
     * /api/villages/{kode_kecamatan}.json
     */
    private fun loadKelurahan(
        kodeKecamatan: String,
        container: LinearLayout
    ) {
        loadingCodes.add(kodeKecamatan)

        container.removeAllViews()

        tampilkanPesan(
            container,
            "Memuat kelurahan..."
        )

        WilayahRetrofitClient.api
            .getKelurahan(kodeKecamatan)
            .enqueue(object : Callback<WilayahResponse> {

                override fun onResponse(
                    call: Call<WilayahResponse>,
                    response: Response<WilayahResponse>
                ) {
                    loadingCodes.remove(kodeKecamatan)

                    if (isFinishing || isDestroyed) return

                    if (!response.isSuccessful) {
                        tampilkanPesan(
                            container,
                            "Gagal memuat kelurahan " +
                                    "(${response.code()}). " +
                                    "Tutup lalu buka kecamatan kembali untuk mencoba lagi."
                        )
                        return
                    }

                    val data = response.body()?.data

                    if (data == null) {
                        tampilkanPesan(
                            container,
                            "Data kelurahan tidak tersedia"
                        )
                        return
                    }

                    val kelurahanList = data
                        .filter {
                            it.code.isNotBlank() &&
                                    it.name.isNotBlank()
                        }

                    cacheKelurahan[kodeKecamatan] =
                        kelurahanList

                    tampilkanKelurahan(
                        container,
                        kelurahanList
                    )
                }

                override fun onFailure(
                    call: Call<WilayahResponse>,
                    t: Throwable
                ) {
                    loadingCodes.remove(kodeKecamatan)

                    if (isFinishing || isDestroyed) return

                    tampilkanPesan(
                        container,
                        "Koneksi gagal mengambil kelurahan. " +
                                "Tutup lalu buka kecamatan kembali untuk mencoba lagi."
                    )
                }
            })
    }

    /**
     * Menampilkan daftar kelurahan di bawah kecamatan.
     */
    private fun tampilkanKelurahan(
        container: LinearLayout,
        kelurahanList: List<WilayahItem>
    ) {
        container.removeAllViews()

        if (kelurahanList.isEmpty()) {
            tampilkanPesan(
                container,
                "Belum ada data kelurahan"
            )
            return
        }

        // Ringkasan jumlah kelurahan pada kecamatan.
        val tvJumlah = TextView(this).apply {
            text = "${kelurahanList.size} Kelurahan"
            textSize = 12f
            setTextColor(Color.parseColor(ORANGE))
            setTypeface(null, Typeface.BOLD)

            setPadding(
                dp(4),
                dp(8),
                dp(4),
                dp(10)
            )
        }

        container.addView(tvJumlah)

        kelurahanList.forEachIndexed { index, kelurahan ->

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                background = GradientDrawable().apply {
                    setColor(Color.parseColor(BACKGROUND))
                    cornerRadius = dp(12).toFloat()
                    setStroke(
                        dp(1),
                        Color.parseColor(BORDER)
                    )
                }

                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            }

            val tvNamaKelurahan = TextView(this).apply {
                text = kelurahan.name
                textSize = 14f
                setTextColor(Color.parseColor(TEXT_PRIMARY))
                setTypeface(null, Typeface.BOLD)
            }



            item.addView(tvNamaKelurahan)


            container.addView(item)
        }
    }

    /**
     * Pesan status untuk loading, error, atau data kosong.
     */
    private fun tampilkanPesan(
        container: LinearLayout,
        message: String
    ) {
        container.removeAllViews()

        val tvPesan = TextView(this).apply {
            text = message
            textSize = 12f
            setTextColor(Color.parseColor(TEXT_SECONDARY))

            setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
            )
        }

        container.addView(tvPesan)
    }

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density)
            .toInt()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Bersihkan penanda request ketika Activity ditutup.
        loadingCodes.clear()
    }
}