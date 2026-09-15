package com.example.bpskota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.bpskota.bps.model.SimdasiResponse
import com.example.bpskota.bps.model.SimdasiTable
import com.example.bpskota.bps.repository.BpsRepository
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PertanianActivity : AppCompatActivity() {

    private lateinit var cardContainer: LinearLayout
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView

    private lateinit var adapter: PertanianTableAdapter
    private lateinit var activityTracker: ActivityTracker

    private val repository =
        BpsRepository()

    private val semuaPertanian =
        mutableListOf<PertanianTableAdapter.Item>()

    private var tahunTerpilih =
        2025

    companion object {

        private const val WILAYAH =
            "3574000"

        private const val API_KEY =
            "008edaaae5d450b1913b31a2cef618c3"

        private const val BAB_PERTANIAN =
            "Pertanian, Kehutanan, Peternakan, dan Perikanan"

        private const val TAHUN_MULAI =
            2026

        private const val TAHUN_MINIMUM =
            2020
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_pertanian
        )

        initView()

        setupLoading()

        setupAdapter()

        activityTracker =
            ActivityTracker(
                this,
                BpskpRetrofitClient.api
            )

        activityTracker.trackScreen(
            screen = "Pertanian",
            metadata = mapOf(
                "tahun" to tahunTerpilih
            )
        )

        setupButton()

        tahunTerpilih = 2025

        loadTahun(
            tahunTerpilih
        )
    }

    private fun initView() {

        cardContainer =
            findViewById(
                R.id.cardContainer
            )

        loadingAnimation =
            findViewById(
                R.id.loadingAnimation
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

        loadingAnimation.setAnimation(
            "Loading_Animation.json"
        )

        loadingAnimation.repeatCount =
            LottieDrawable.INFINITE

        loadingAnimation.visibility =
            View.GONE
    }

    private fun showLoading() {

        loadingAnimation.visibility =
            View.VISIBLE

        loadingAnimation.playAnimation()
    }

    private fun hideLoading() {

        loadingAnimation.cancelAnimation()

        loadingAnimation.visibility =
            View.GONE
    }

    private fun setupAdapter() {

        adapter =
            PertanianTableAdapter(

                container = cardContainer,
                kategori =
                "Pertanian, Kehutanan, Peternakan, dan Perikanan",

                onItemClick = { table, tahun ->

                    onTableClicked(
                        table,
                        tahun
                    )
                }
            )
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
            (
                    TAHUN_MULAI downTo
                            TAHUN_MINIMUM
                    )
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
                android.R.layout.simple_spinner_dropdown_item,
                daftarTahun
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
                    daftarTahun[
                            posisi
                    ].toInt()

                if (
                    tahunBaru !=
                    tahunTerpilih
                ) {

                    tahunTerpilih =
                        tahunBaru

                    loadTahun(
                        tahunTerpilih
                    )
                }
            }

            .show()
    }

    private fun loadTahun(
        tahun: Int
    ) {

        showLoading()

        semuaPertanian.clear()

        adapter.tampilkan(
            semuaPertanian
        )

        repository
            .getSimdasiTables(

                tahun = tahun,

                wilayah = WILAYAH,

                apiKey = API_KEY

            )
            .enqueue(

                object :
                    Callback<SimdasiResponse> {

                    override fun onResponse(

                        call:
                        Call<SimdasiResponse>,

                        response:
                        Response<SimdasiResponse>

                    ) {

                        if (
                            !response.isSuccessful
                        ) {

                            hideLoading()

                            Toast.makeText(
                                this@PertanianActivity,
                                "Gagal mengambil data tahun $tahun",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        val body =
                            response.body()

                        if (
                            body == null
                        ) {

                            hideLoading()

                            Toast.makeText(
                                this@PertanianActivity,
                                "Response kosong",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        if (
                            body.status
                                ?.uppercase() != "OK"
                        ) {

                            hideLoading()

                            Toast.makeText(
                                this@PertanianActivity,
                                "Data tahun $tahun tidak tersedia",
                                Toast.LENGTH_SHORT
                            ).show()

                            return
                        }

                        val allTables =
                            body.data
                                ?.flatMap { page ->

                                    page.tables
                                        ?: emptyList()
                                }
                                ?: emptyList()

                        val pertanianTables =
                            allTables.filter { table ->

                                table.bab
                                    ?.trim()
                                    ?.equals(
                                        BAB_PERTANIAN,
                                        ignoreCase = true
                                    ) == true
                            }

                        val itemsTahunIni =
                            pertanianTables.map { table ->

                                PertanianTableAdapter.Item(

                                    tahun = tahun,

                                    table = table
                                )
                            }

                        semuaPertanian.addAll(
                            itemsTahunIni
                        )

                        adapter.tampilkan(
                            semuaPertanian
                        )

                        hideLoading()

                        if (
                            itemsTahunIni.isEmpty()
                        ) {

                            Toast.makeText(
                                this@PertanianActivity,
                                "Data Pertanian tahun $tahun tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<SimdasiResponse>,

                        t: Throwable

                    ) {

                        hideLoading()

                        Toast.makeText(
                            this@PertanianActivity,
                            "Gagal mengambil data: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun onTableClicked(

        table: SimdasiTable,

        tahun: Int

    ) {

        val idTabel =
            table.idTabel

        if (
            idTabel.isNullOrEmpty()
        ) {

            Toast.makeText(
                this,
                "ID tabel tidak tersedia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                this,
                StatistikDetailActivity::class.java
            )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_ID_TABEL,
            idTabel
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_TAHUN,
            tahun
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_JUDUL,
            table.judul
        )

        intent.putExtra(
            StatistikDetailActivity.EXTRA_KODE,
            table.kodeTabel
        )

        startActivity(
            intent
        )
    }

    override fun onDestroy() {

        if (
            ::loadingAnimation.isInitialized
        ) {

            loadingAnimation.cancelAnimation()

            loadingAnimation.visibility =
                View.GONE
        }

        super.onDestroy()
    }
}