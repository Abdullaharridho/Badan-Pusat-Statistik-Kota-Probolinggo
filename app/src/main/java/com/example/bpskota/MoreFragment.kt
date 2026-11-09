package com.example.bpskota

import android.animation.LayoutTransition
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import java.util.Locale

class MoreFragment : Fragment() {

    private val bpskpRepository = BpskpRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners(view)
        setupUsageCard(view)
        loadActivityStatistics()
    }

    /**
     * Mendaftarkan semua event klik untuk menu-menu yang ada di halaman Lainnya (More).
     */
    private fun setupClickListeners(view: View) {
        // Menu Bantuan
        view.findViewById<CardView>(R.id.cardBantuan)?.setOnClickListener {
            val intent = Intent(requireContext(),PusatBantuanActivity::class.java)
            startActivity(intent)
        }

        // Menu Tentang Developer
        view.findViewById<LinearLayout>(R.id.cardDeveloper)?.setOnClickListener {
            showDeveloperDialog()
        }

        // Menu Tentang Kami
        view.findViewById<LinearLayout>(R.id.cardTentangKami)?.setOnClickListener {
            showTentangKamiDialog()
        }
    }

    /**
     * Mengatur logika buka-tutup (expand/collapse) pada kartu Statistik Penggunaan
     * beserta rotasi ikon panah secara mulus.
     */
    private fun setupUsageCard(view: View) {
        val cardUsage = view.findViewById<CardView>(R.id.cardUsage)
        val layoutUsageHeader = view.findViewById<LinearLayout>(R.id.layoutUsageHeader)
        val layoutUsageDetail = view.findViewById<LinearLayout>(R.id.layoutUsageDetail)
        val ivUsageArrow = view.findViewById<ImageView>(R.id.ivUsageArrow)

        // Kondisi awal ditutup
        layoutUsageDetail.visibility = View.GONE
        ivUsageArrow.rotation = 0f

        // Memberikan transisi halus saat tata letak berubah ukuran
        (layoutUsageDetail.parent as? ViewGroup)?.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        val toggleUsage = {
            if (layoutUsageDetail.visibility == View.GONE) {
                // Buka panel detail grid
                layoutUsageDetail.visibility = View.VISIBLE
                ivUsageArrow.animate().rotation(90f).setDuration(250).start()
            } else {
                // Tutup panel detail grid
                layoutUsageDetail.visibility = View.GONE
                ivUsageArrow.animate().rotation(0f).setDuration(250).start()
            }
        }

        cardUsage?.setOnClickListener { toggleUsage() }
        layoutUsageHeader?.setOnClickListener { toggleUsage() }
    }

    /**
     * Memanggil API melalui repository untuk mengambil data statistik penggunaan aplikasi.
     */
    private fun loadActivityStatistics() {
        if (!isAdded) return

        bpskpRepository.getActivityStatistics { response, error ->
            if (!isAdded) return@getActivityStatistics

            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread

                if (response != null) {
                    val users = response.summary?.users_accessing ?: 0
                    val totalAccess = response.summary?.total_access ?: 0
                    updateUsageStatistics(users, totalAccess)
                } else {
                    showUsageStatisticsError(error)
                }
            }
        }
    }

    /**
     * Memperbarui teks UI dashboard grid dengan data terbaru dari server.
     */
    private fun updateUsageStatistics(users: Int, totalAccess: Int) {
        val tvTotalPengguna = view?.findViewById<TextView>(R.id.tvTotalPengguna)
        val tvTotalAkses = view?.findViewById<TextView>(R.id.tvTotalAkses)

        tvTotalPengguna?.text = formatNumber(users)
        tvTotalAkses?.text = formatNumber(totalAccess)
    }

    /**
     * Menampilkan status error jika pengambilan data statistik gagal.
     */
    private fun showUsageStatisticsError(error: String?) {
        view?.findViewById<TextView>(R.id.tvTotalPengguna)?.text = "-"
        view?.findViewById<TextView>(R.id.tvTotalAkses)?.text = "-"

        if (!error.isNullOrBlank()) {
            Log.e("MoreFragment", "Gagal mengambil statistik: $error")
        }
    }

    /**
     * Memformat angka ribuan bergaya Indonesia (misal: 1.500.000).
     */
    private fun formatNumber(value: Int): String {
        return String.format(Locale("id", "ID"), "%,d", value).replace(",", ".")
    }

    // =========================================================
    // POPUP DIALOGS & INTENTS
    // =========================================================

    /**
     * Menampilkan dialog popup Tentang Kami beserta link ke sosial media.
     */
    private fun showTentangKamiDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tentang_kami, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<LinearLayout>(R.id.socialInstagram)?.setOnClickListener { openInstagram() }
        dialogView.findViewById<LinearLayout>(R.id.socialTiktok)?.setOnClickListener { openTikTok() }
        dialogView.findViewById<LinearLayout>(R.id.socialWhatsapp)?.setOnClickListener { openWhatsApp() }
        dialogView.findViewById<LinearLayout>(R.id.socialAlamat)?.setOnClickListener { openGoogleMaps() }
        dialogView.findViewById<LinearLayout>(R.id.socialEmail)?.setOnClickListener { openEmail() }

        dialogView.findViewById<TextView>(R.id.btnTutupTentangKami)?.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    /**
     * Menampilkan dialog popup informasi Tentang Developer.
     */
    private fun showDeveloperDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_developer, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.btnCloseDeveloper)?.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun openInstagram() {
        val username = "bpskotaprobolinggo"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=$username")))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/$username/")))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Tidak dapat membuka Instagram", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openTikTok() {
        val username = "bpskotaprobolinggo"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("snssdk1233://user/profile/$username")))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@$username")))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Tidak dapat membuka TikTok", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWhatsApp() {
        val nomorWhatsApp = "628xxxxxxxxxx"
        val pesan = Uri.encode("Halo BPS Kota Probolinggo")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$nomorWhatsApp?text=$pesan")))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGoogleMaps() {
        val alamat = Uri.encode("Jl. Bromo No. 32, Kota Probolinggo, Jawa Timur 67222")
        try {
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$alamat"))
            mapsIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapsIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$alamat")))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Tidak dapat membuka Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openEmail() {
        val email = "bps3574@bps.go.id"
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Pertanyaan - BPS Kota Probolinggo")
            }
            startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_SHORT).show()
        }
    }
}