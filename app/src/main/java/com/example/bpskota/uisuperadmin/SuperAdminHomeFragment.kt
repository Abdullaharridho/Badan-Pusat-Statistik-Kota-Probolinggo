package com.example.bpskota.uisuperadmin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.model.AdminActivityStatisticsResponse
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentSuperAdminHomeBinding

class SuperAdminHomeFragment : Fragment() {

    companion object {
        private const val TAG = "SuperAdminHome"
    }

    private var _binding: FragmentSuperAdminHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSuperAdminHomeBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        authSession = BpskpAuthSession(requireContext())
        repository = BpskpRepository()

        tampilkanNamaSuperAdmin()
        loadActivityStatistics()
    }

    private fun tampilkanNamaSuperAdmin() {

        val nama = authSession.getName()

        binding.tvSuperAdminName.text =
            if (!nama.isNullOrBlank()) {
                nama
            } else {
                "Super Admin"
            }
    }

    private fun loadActivityStatistics() {

        Log.d(
            TAG,
            "Memuat statistik aktivitas Super Admin"
        )

        repository.getAdminActivityStatistics(
            authSession
        ) { response, error ->

            if (!isAdded || _binding == null) {
                return@getAdminActivityStatistics
            }

            requireActivity().runOnUiThread {

                if (!isAdded || _binding == null) {
                    return@runOnUiThread
                }

                if (response != null) {

                    tampilkanStatistik(response)

                } else {

                    Log.e(
                        TAG,
                        "Gagal mengambil statistik: $error"
                    )

                    Toast.makeText(
                        requireContext(),
                        error
                            ?: "Gagal mengambil statistik penggunaan.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun tampilkanStatistik(
        response: AdminActivityStatisticsResponse
    ) {

        val summary = response.summary

        // ==============================
        // TOTAL PENGGUNA
        // ==============================

        binding.tvTotalPengguna.text =
            formatNumber(
                summary?.users_accessing ?: 0
            )

        // ==============================
        // TOTAL AKSES
        // ==============================

        binding.tvTotalAkses.text =
            formatNumber(
                summary?.total_access ?: 0
            )

        // ==============================
        // TOTAL DURASI
        // ==============================

        binding.tvTotalDurasi.text =
            formatDuration(
                summary?.total_duration_seconds ?: 0L
            )

        // ==============================
        // MENU TERPOPULER
        // ==============================

        val menuTerpopuler =
            response.screens?.firstOrNull()

        if (menuTerpopuler != null) {

            val namaMenu =
                menuTerpopuler.screen
                    ?.takeIf { it.isNotBlank() }
                    ?: "-"

            binding.tvMenuTerpopuler.text =
                namaMenu

            binding.tvMenuTerpopulerTotal.text =
                "${formatNumber(menuTerpopuler.total)} akses"

        } else {

            binding.tvMenuTerpopuler.text =
                "-"

            binding.tvMenuTerpopulerTotal.text =
                "0 akses"
        }

        // ==============================
        // LOG
        // ==============================

        Log.d(
            TAG,
            "Statistik berhasil ditampilkan"
        )

        Log.d(
            TAG,
            "Total pengguna = ${summary?.users_accessing}"
        )

        Log.d(
            TAG,
            "Total akses = ${summary?.total_access}"
        )

        Log.d(
            TAG,
            "Total durasi = " +
                    "${summary?.total_duration_seconds} detik"
        )

        Log.d(
            TAG,
            "Menu terpopuler = " +
                    "${menuTerpopuler?.screen}"
        )

        Log.d(
            TAG,
            "Jumlah akses menu terpopuler = " +
                    "${menuTerpopuler?.total}"
        )
    }

    private fun formatNumber(
        number: Int
    ): String {

        return String
            .format("%,d", number)
            .replace(',', '.')
    }

    private fun formatDuration(
        totalSeconds: Long
    ): String {

        val hours =
            totalSeconds / 3600

        val minutes =
            (totalSeconds % 3600) / 60

        val seconds =
            totalSeconds % 60

        return when {

            hours > 0 -> {

                if (minutes > 0) {
                    "$hours jam $minutes menit"
                } else {
                    "$hours jam"
                }
            }

            minutes > 0 -> {

                if (seconds > 0) {
                    "$minutes menit $seconds detik"
                } else {
                    "$minutes menit"
                }
            }

            else -> {
                "$seconds detik"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}