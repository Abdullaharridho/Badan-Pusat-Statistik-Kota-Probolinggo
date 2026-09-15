package com.example.bpskota

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.bpskota.bps.model.TenagaKerjaResponse
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class TenagakerjaActivity : AppCompatActivity() {

    companion object {

        private const val DOMAIN = "3574"

        private const val VARIABLE = 88

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val TAHUN_AWAL = 2010

        private const val TAHUN_AKHIR = 2025

        private const val KODE_TAHUN_AWAL = 110

        private const val KODE_TAHUN_AKHIR = 125
    }

    private val repository =
        BpsRepository()

    private lateinit var activityTracker: ActivityTracker

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView

    private var tahunTerpilih = 2025

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_tenagakerja
        )

        initView()

        setupLoading()

        setupButton()

        activityTracker =
            ActivityTracker(
                this,
                BpskpRetrofitClient.api
            )

        activityTracker.trackScreen(
            screen = "Tenagakerja",
            metadata = mapOf(
                "tahun" to tahunTerpilih
            )
        )

        ambilDataTahun(
            tahun = tahunTerpilih,
            kodeTahun = getKodeTahun(
                tahunTerpilih
            )!!
        )
    }

    private fun initView() {

        cardContainer =
            findViewById(
                R.id.cardContainer
            )

        progressLoading =
            findViewById(
                R.id.progressLoading
            )

        btnBack =
            findViewById(
                R.id.btnBack
            )

        btnFilter =
            findViewById(
                R.id.btnFilter
            )
    }

    private fun setupLoading() {

        progressLoading.setAnimation(
            "Loading_Animation.json"
        )

        progressLoading.repeatCount =
            LottieDrawable.INFINITE

        progressLoading.visibility =
            View.GONE
    }

    private fun mulaiLoading() {

        progressLoading.visibility =
            View.VISIBLE

        progressLoading.playAnimation()
    }

    private fun selesaiLoading() {

        progressLoading.cancelAnimation()

        progressLoading.visibility =
            View.GONE
    }

    private fun setupButton() {

        btnBack.setOnClickListener {

            finish()
        }

        btnFilter.setOnClickListener {

            tampilkanFilterTahun()
        }
    }

    private fun tampilkanFilterTahun() {

        val daftarTahun =
            (TAHUN_AWAL..TAHUN_AKHIR)
                .reversed()
                .map {
                    it.toString()
                }
                .toTypedArray()

        val spinner =
            Spinner(this)

        spinner.setPadding(
            32,
            0,
            32,
            0
        )

        val spinnerAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                daftarTahun
            )

        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter =
            spinnerAdapter

        val posisiTerpilih =
            daftarTahun.indexOf(
                tahunTerpilih.toString()
            )

        if (
            posisiTerpilih >= 0
        ) {

            spinner.setSelection(
                posisiTerpilih
            )
        }

        AlertDialog.Builder(this)

            .setTitle(
                "Pilih Tahun"
            )

            .setView(
                spinner
            )

            .setNegativeButton(
                "Batal",
                null
            )

            .setPositiveButton(
                "Tampilkan"
            ) { _, _ ->

                val posisi =
                    spinner.selectedItemPosition

                if (
                    posisi < 0 ||
                    posisi >= daftarTahun.size
                ) {
                    return@setPositiveButton
                }

                val tahunBaru =
                    daftarTahun[posisi]
                        .toIntOrNull()

                if (
                    tahunBaru == null
                ) {
                    return@setPositiveButton
                }

                if (
                    tahunBaru ==
                    tahunTerpilih
                ) {
                    return@setPositiveButton
                }

                val kodeTahun =
                    getKodeTahun(
                        tahunBaru
                    )

                if (
                    kodeTahun == null
                ) {

                    Toast.makeText(
                        this,
                        "Kode tahun $tahunBaru tidak tersedia",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                tahunTerpilih =
                    tahunBaru

                ambilDataTahun(
                    tahun = tahunBaru,
                    kodeTahun = kodeTahun
                )
            }

            .show()
    }

    private fun getKodeTahun(
        tahun: Int
    ): Int? {

        if (
            tahun < TAHUN_AWAL ||
            tahun > TAHUN_AKHIR
        ) {
            return null
        }

        val kode =
            tahun - 1900

        if (
            kode < KODE_TAHUN_AWAL ||
            kode > KODE_TAHUN_AKHIR
        ) {
            return null
        }

        return kode
    }

    private fun ambilDataTahun(
        tahun: Int,
        kodeTahun: Int
    ) {

        mulaiLoading()

        cardContainer.removeAllViews()

        repository.getTenagaKerja(
            domain = DOMAIN,
            variable = VARIABLE,
            tahun = kodeTahun,
            apiKey = API_KEY
        ).enqueue(
            object :
                Callback<TenagaKerjaResponse> {

                override fun onResponse(
                    call: Call<TenagaKerjaResponse>,
                    response: Response<TenagaKerjaResponse>
                ) {

                    if (
                        !response.isSuccessful
                    ) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Gagal mengambil data tahun $tahun",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    val body =
                        response.body()

                    if (
                        body == null
                    ) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Response tahun $tahun kosong",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    if (
                        body.status
                            ?.uppercase(
                                Locale.ROOT
                            ) != "OK"
                    ) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Status API: ${body.status}",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    if (
                        body.dataAvailability
                            ?.lowercase(
                                Locale.ROOT
                            ) != "available"
                    ) {

                        selesaiLoading()

                        Toast.makeText(
                            this@TenagakerjaActivity,
                            "Data tahun $tahun tidak tersedia",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    tampilkanData(
                        body = body,
                        tahun = tahun,
                        kodeTahun = kodeTahun
                    )
                }

                override fun onFailure(
                    call: Call<TenagaKerjaResponse>,
                    t: Throwable
                ) {

                    selesaiLoading()

                    Toast.makeText(
                        this@TenagakerjaActivity,
                        "Gagal mengambil data tahun $tahun: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun tampilkanData(
        body: TenagaKerjaResponse,
        tahun: Int,
        kodeTahun: Int
    ) {

        val dataContent =
            body.dataContent
                ?: emptyMap()

        val prefix =
            "$DOMAIN$VARIABLE"

        var nilai: Double? =
            null

        val entryUtama =
            dataContent.entries
                .firstOrNull { entry ->

                    entry.key.startsWith(
                        prefix
                    ) &&
                            entry.key.contains(
                                kodeTahun.toString()
                            )
                }

        if (
            entryUtama != null
        ) {

            nilai =
                entryUtama.value
        }

        if (
            nilai == null
        ) {

            val entryFallback =
                dataContent.entries
                    .firstOrNull { entry ->

                        entry.key.endsWith(
                            kodeTahun.toString()
                        )
                    }

            if (
                entryFallback != null
            ) {

                nilai =
                    entryFallback.value
            }
        }

        if (
            nilai == null &&
            dataContent.size == 1
        ) {

            val entry =
                dataContent.entries.first()

            nilai =
                entry.value
        }

        if (
            nilai == null
        ) {

            selesaiLoading()

            Toast.makeText(
                this,
                "Nilai tahun $tahun tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val variable =
            body.variable
                ?.firstOrNull()

        val judul =
            variable?.label
                ?: "Tingkat Pengangguran Terbuka (TPT)"

        val satuan =
            variable?.unit
                ?: "Persen"

        val decimal =
            variable?.decimal
                ?: 2

        val nilaiFormat =
            String.format(
                Locale.US,
                "%.${decimal}f",
                nilai
            )

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_statistik,
                    cardContainer,
                    false
                )

        val tvTahun =
            card.findViewById<TextView>(
                R.id.tvTahun
            )

        val tvKategori =
            card.findViewById<TextView>(
                R.id.tvKategori
            )

        val tvJudul =
            card.findViewById<TextView>(
                R.id.tvJudul
            )

        val tvNilai =
            card.findViewById<TextView>(
                R.id.tvNilai
            )

        tvTahun.text =
            tahun.toString()

        tvKategori.text =
            "Tenaga Kerja"

        tvJudul.text =
            judul

        tvNilai.text =
            if (
                satuan.isNotBlank()
            ) {
                "$nilaiFormat $satuan"
            } else {
                nilaiFormat
            }

        card.setOnClickListener {

            Toast.makeText(
                this,
                "$nilaiFormat $satuan\nTahun $tahun",
                Toast.LENGTH_LONG
            ).show()
        }

        cardContainer.addView(
            card
        )

        selesaiLoading()
    }

    override fun onDestroy() {

        if (
            ::progressLoading.isInitialized
        ) {

            progressLoading.cancelAnimation()

            progressLoading.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}