package com.example.bpskota.uipegawai

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentPegawaiBerandaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PegawaiBerandaFragment : Fragment() {
    private var _binding: FragmentPegawaiBerandaBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPegawaiBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = BpskpAuthSession(requireContext())
        repository = BpskpRepository()

        setupDashboardInfo()
        loadStatistik()
        playEntranceAnimation()
    }

    private fun setupDashboardInfo() {
        val namaUser = session.getName()?.takeIf { it.isNotBlank() } ?: "Pegawai BPS"
        binding.tvNamaPegawaiBeranda.text = namaUser
        binding.tvJabatanPegawaiBeranda.text = "Portal Pegawai BPSKp"

        binding.tvJumlahPenugasanPegawai.text = "-"
        binding.tvJumlahAgendaPegawai.text = "-"
        binding.tvJumlahTugasSelesaiPegawai.text = "-"
    }

    private fun loadStatistik() {
        if (session.getToken().isNullOrBlank()) return

        repository.getPenugasan(session = session, search = null, status = null) { response, error ->
            if (!isAdded || _binding == null) return@getPenugasan

            if (response?.data != null) {
                val daftarPenugasan = response.data
                val totalPenugasan = daftarPenugasan.size
                val totalSelesai = daftarPenugasan.count { it.status.equals("selesai", ignoreCase = true) }

                binding.tvJumlahPenugasanPegawai.text = totalPenugasan.toString()
                binding.tvJumlahTugasSelesaiPegawai.text = totalSelesai.toString()
            } else if (error != null) {
                binding.tvJumlahPenugasanPegawai.text = "0"
                binding.tvJumlahTugasSelesaiPegawai.text = "0"
            }
        }

        val tanggalHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        repository.getKegiatanHarian(session = session, tanggal = tanggalHariIni) { response, error ->
            if (!isAdded || _binding == null) return@getKegiatanHarian

            if (response != null) {
                binding.tvJumlahAgendaPegawai.text = response.size.toString()
            } else {
                binding.tvJumlahAgendaPegawai.text = "0"
            }
        }
    }

    private fun playEntranceAnimation() {
        val currentBinding = _binding ?: return
        val views = listOf(currentBinding.cardSambutanPegawai, currentBinding.cardInformasiPegawai)

        views.forEachIndexed { index, target ->
            target.alpha = 0f
            target.translationY = 40f

            val fadeIn = ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f)
            val slideUp = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 40f, 0f)

            AnimatorSet().apply {
                playTogether(fadeIn, slideUp)
                duration = 400L
                startDelay = index * 120L
                interpolator = DecelerateInterpolator(1.2f)
                start()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}