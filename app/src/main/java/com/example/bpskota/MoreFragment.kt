package com.example.bpskota

import android.animation.LayoutTransition
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.repository.BpskpRepository

class MoreFragment : Fragment() {

    // =========================================================
    // REPOSITORY
    // =========================================================

    private val bpskpRepository = BpskpRepository()


    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(
            R.layout.fragment_more,
            container,
            false
        )
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)


        // =====================================================
        // CARD TENTANG KAMI
        // =====================================================

        val cardTentangKami =
            view.findViewById<CardView>(R.id.cardTentangKami)

        cardTentangKami?.setOnClickListener {

            showTentangKamiDialog()

        }


        // =====================================================
        // CARD PENGADUAN
        // =====================================================

        val cardPengaduan =
            view.findViewById<CardView>(R.id.cardPengaduan)

        cardPengaduan?.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Menu Pengaduan",
                Toast.LENGTH_SHORT
            ).show()

        }


        // =====================================================
        // CARD BANTUAN
        // =====================================================

        val cardBantuan =
            view.findViewById<CardView>(R.id.cardBantuan)

        cardBantuan?.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Menu Pusat Bantuan",
                Toast.LENGTH_SHORT
            ).show()

        }


        // =====================================================
        // CARD PENGGUNAAN APLIKASI
        // =====================================================

        setupUsageCard(view)


        // =====================================================
        // CARD DEVELOPER
        // =====================================================

        val cardDeveloper =
            view.findViewById<CardView>(R.id.cardDeveloper)

        cardDeveloper?.setOnClickListener {

            showDeveloperDialog()

        }


        // =====================================================
        // LOAD STATISTIK PENGGUNAAN
        // =====================================================

        loadActivityStatistics()

    }


    // =========================================================
    // SETUP CARD PENGGUNAAN APLIKASI
    // =========================================================

    private fun setupUsageCard(view: View) {

        val cardUsage =
            view.findViewById<CardView>(R.id.cardUsage)

        val layoutUsageHeader =
            view.findViewById<LinearLayout>(R.id.layoutUsageHeader)

        val layoutUsageDetail =
            view.findViewById<LinearLayout>(R.id.layoutUsageDetail)

        val ivUsageArrow =
            view.findViewById<ImageView>(R.id.ivUsageArrow)


        // =====================================================
        // KONDISI AWAL
        // =====================================================

        layoutUsageDetail.visibility = View.GONE

        ivUsageArrow.rotation = 0f


        // =====================================================
        // ANIMASI LAYOUT
        // =====================================================

        (layoutUsageDetail.parent as? ViewGroup)?.layoutTransition =
            LayoutTransition().apply {

                enableTransitionType(
                    LayoutTransition.CHANGING
                )

            }


        // =====================================================
        // CLICK CARD
        // =====================================================

        val toggleUsage = {

            if (layoutUsageDetail.visibility == View.GONE) {

                layoutUsageDetail.visibility = View.VISIBLE

                ivUsageArrow.animate()
                    .rotation(180f)
                    .setDuration(200)
                    .start()

            } else {

                layoutUsageDetail.visibility = View.GONE

                ivUsageArrow.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .start()

            }

        }


        cardUsage?.setOnClickListener {

            toggleUsage()

        }

        layoutUsageHeader?.setOnClickListener {

            toggleUsage()

        }

    }


    // =========================================================
    // LOAD STATISTIK AKTIVITAS
    // =========================================================

    private fun loadActivityStatistics() {

        if (!isAdded) {
            return
        }


        bpskpRepository.getActivityStatistics { response, error ->

            if (!isAdded) {
                return@getActivityStatistics
            }

            requireActivity().runOnUiThread {

                if (!isAdded) {
                    return@runOnUiThread
                }


                if (response != null) {

                    val users =
                        response.summary?.users_accessing ?: 0

                    val totalAccess =
                        response.summary?.total_access ?: 0


                    updateUsageStatistics(
                        users,
                        totalAccess
                    )

                } else {

                    showUsageStatisticsError(
                        error
                    )

                }

            }

        }

    }


    // =========================================================
    // UPDATE STATISTIK
    // =========================================================

    private fun updateUsageStatistics(
        users: Int,
        totalAccess: Int
    ) {

        val tvTotalPengguna =
            view?.findViewById<TextView>(
                R.id.tvTotalPengguna
            )

        val tvTotalAkses =
            view?.findViewById<TextView>(
                R.id.tvTotalAkses
            )


        tvTotalPengguna?.text =
            formatNumber(users)

        tvTotalAkses?.text =
            formatNumber(totalAccess)

    }


    // =========================================================
    // ERROR STATISTIK
    // =========================================================

    private fun showUsageStatisticsError(
        error: String?
    ) {

        val tvTotalPengguna =
            view?.findViewById<TextView>(
                R.id.tvTotalPengguna
            )

        val tvTotalAkses =
            view?.findViewById<TextView>(
                R.id.tvTotalAkses
            )


        tvTotalPengguna?.text = "-"

        tvTotalAkses?.text = "-"

        if (!error.isNullOrBlank()) {

            android.util.Log.e(
                "MoreFragment",
                "Gagal mengambil statistik: $error"
            )

        }

    }


    // =========================================================
    // FORMAT ANGKA
    // =========================================================

    private fun formatNumber(
        value: Int
    ): String {

        return String.format(
            java.util.Locale("id", "ID"),
            "%,d",
            value
        ).replace(",", ".")

    }


    // =========================================================
    // POPUP TENTANG KAMI
    // =========================================================

    private fun showTentangKamiDialog() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_tentang_kami,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()


        // =====================================================
        // INSTAGRAM
        // =====================================================

        val socialInstagram =
            dialogView.findViewById<LinearLayout>(
                R.id.socialInstagram
            )

        socialInstagram?.setOnClickListener {

            openInstagram()

        }


        // =====================================================
        // TIKTOK
        // =====================================================

        val socialTiktok =
            dialogView.findViewById<LinearLayout>(
                R.id.socialTiktok
            )

        socialTiktok?.setOnClickListener {

            openTikTok()

        }


        // =====================================================
        // WHATSAPP
        // =====================================================

        val socialWhatsapp =
            dialogView.findViewById<LinearLayout>(
                R.id.socialWhatsapp
            )

        socialWhatsapp?.setOnClickListener {

            openWhatsApp()

        }


        // =====================================================
        // ALAMAT
        // =====================================================

        val socialAlamat =
            dialogView.findViewById<LinearLayout>(
                R.id.socialAlamat
            )

        socialAlamat?.setOnClickListener {

            openGoogleMaps()

        }


        // =====================================================
        // EMAIL
        // =====================================================

        val socialEmail =
            dialogView.findViewById<LinearLayout>(
                R.id.socialEmail
            )

        socialEmail?.setOnClickListener {

            openEmail()

        }


        // =====================================================
        // TOMBOL TUTUP
        // =====================================================

        val btnTutup =
            dialogView.findViewById<TextView>(
                R.id.btnTutupTentangKami
            )

        btnTutup?.setOnClickListener {

            dialog.dismiss()

        }


        // =====================================================
        // BACKGROUND TRANSPARAN
        // =====================================================

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )


        // =====================================================
        // TAMPILKAN DIALOG
        // =====================================================

        dialog.show()

    }


    // =========================================================
    // INSTAGRAM
    // =========================================================

    private fun openInstagram() {

        val username = "bpskotaprobolinggo"

        try {

            val instagramIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "instagram://user?username=$username"
                )
            )

            startActivity(instagramIntent)

        } catch (e: ActivityNotFoundException) {

            try {

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.instagram.com/$username/"
                    )
                )

                startActivity(browserIntent)

            } catch (e2: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(),
                    "Tidak dapat membuka Instagram",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }


    // =========================================================
    // TIKTOK
    // =========================================================

    private fun openTikTok() {

        val username = "bpskotaprobolinggo"

        try {

            val tiktokIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "snssdk1233://user/profile/$username"
                )
            )

            startActivity(tiktokIntent)

        } catch (e: ActivityNotFoundException) {

            try {

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.tiktok.com/@$username"
                    )
                )

                startActivity(browserIntent)

            } catch (e2: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(),
                    "Tidak dapat membuka TikTok",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }


    // =========================================================
    // WHATSAPP
    // =========================================================

    private fun openWhatsApp() {

        val nomorWhatsApp = "628xxxxxxxxxx"

        val pesan = Uri.encode(
            "Halo BPS Kota Probolinggo"
        )

        try {

            val whatsappIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://wa.me/$nomorWhatsApp?text=$pesan"
                )
            )

            startActivity(whatsappIntent)

        } catch (e: ActivityNotFoundException) {

            Toast.makeText(
                requireContext(),
                "Tidak dapat membuka WhatsApp",
                Toast.LENGTH_SHORT
            ).show()

        }

    }


    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private fun openGoogleMaps() {

        val alamat = Uri.encode(
            "Jl. Bromo No. 32, Kota Probolinggo, Jawa Timur 67222"
        )

        try {

            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "geo:0,0?q=$alamat"
                )
            )

            mapsIntent.setPackage(
                "com.google.android.apps.maps"
            )

            startActivity(mapsIntent)

        } catch (e: ActivityNotFoundException) {

            try {

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.google.com/maps/search/?api=1&query=$alamat"
                    )
                )

                startActivity(browserIntent)

            } catch (e2: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(),
                    "Tidak dapat membuka Google Maps",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }


    // =========================================================
    // EMAIL
    // =========================================================

    private fun openEmail() {

        val email = "bps3574@bps.go.id"

        try {

            val emailIntent = Intent(
                Intent.ACTION_SENDTO
            )

            emailIntent.data = Uri.parse(
                "mailto:$email"
            )

            emailIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Pertanyaan - BPS Kota Probolinggo"
            )

            startActivity(emailIntent)

        } catch (e: ActivityNotFoundException) {

            Toast.makeText(
                requireContext(),
                "Tidak ada aplikasi email yang tersedia",
                Toast.LENGTH_SHORT
            ).show()

        }

    }


    // =========================================================
    // POPUP TENTANG DEVELOPER
    // =========================================================

    private fun showDeveloperDialog() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_developer,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()


        // =====================================================
        // TOMBOL TUTUP
        // =====================================================

        val btnTutup =
            dialogView.findViewById<TextView>(
                R.id.btnCloseDeveloper
            )

        btnTutup?.setOnClickListener {

            dialog.dismiss()

        }


        // =====================================================
        // BACKGROUND TRANSPARAN
        // =====================================================

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )


        // =====================================================
        // TAMPILKAN DIALOG
        // =====================================================

        dialog.show()

    }

}