
package com.example.bpskota.uioperator

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.bpskp.model.RuanganResponse
import com.example.bpskota.model.PemesananRuanganResponse
import com.example.bpskota.databinding.FragmentOperatorBerandaBinding

class OperatorBerandaFragment : Fragment() {

    private var _binding: FragmentOperatorBerandaBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { BpskpRepository() }
    private val authSession by lazy { BpskpAuthSession(requireContext()) }

    private val warnaUtama = Color.rgb(249, 115, 22)
    private val warnaTeks = Color.rgb(45, 45, 45)
    private val warnaSekunder = Color.rgb(110, 110, 110)

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorBerandaBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvNamaOperator.text = "Operator"
        binding.tvSapaanOperator.text = "Selamat datang kembali 👋"
        binding.tvDeskripsiDashboard.text =
            "Pantau data ruangan dan pemesanan dalam satu halaman."
        binding.tvStatusDashboard.text = "Dashboard Operator"

        tampilkanLoading()
        muatDataDashboard()
    }

    private fun muatDataDashboard() {
        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            tampilkanGagal("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        val authorization = "Bearer $token"

        // Ambil jumlah ruangan dari API.
        repository.getRuangan(authorization) { body, code, errorMsg ->
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread

                if (code in 200..299 && body?.success == true) {
                    val daftarRuangan = body.data.orEmpty()
                    binding.tvJumlahRuangan.text =
                        daftarRuangan.size.toString()
                } else {
                    binding.tvJumlahRuangan.text = "—"
                    tampilkanPesanError(
                        "Data ruangan belum dapat dimuat."
                    )
                }
            }
        }

        // Ambil daftar pemesanan dari API.
        repository.getPemesananRuangan(
            authorization = authorization,
            tanggal = null,
            ruanganId = null,
            status = null,
            search = null
        ) { body, code, errorMsg ->
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread

                if (code in 200..299 && body?.success == true) {
                    val daftarPemesanan = body.data.orEmpty()

                    binding.tvJumlahPemesanan.text =
                        daftarPemesanan.size.toString()

                    tampilkanPemesananTerbaru(daftarPemesanan)
                } else {
                    binding.tvJumlahPemesanan.text = "—"
                    tampilkanPemesananTerbaru(emptyList())
                    tampilkanPesanError(
                        "Data pemesanan belum dapat dimuat."
                    )
                }
            }
        }
    }

    private fun tampilkanLoading() {
        binding.tvJumlahRuangan.text = "..."
        binding.tvJumlahPemesanan.text = "..."
        binding.layoutPemesananTerbaru.removeAllViews()

        binding.layoutPemesananTerbaru.addView(
            buatTextView(
                teks = "Memuat data pemesanan...",
                ukuran = 14f,
                warna = warnaSekunder
            )
        )
    }

    private fun tampilkanPemesananTerbaru(
        daftar: List<com.example.bpskota.model.PemesananRuangan>
    ) {
        binding.layoutPemesananTerbaru.removeAllViews()

        if (daftar.isEmpty()) {
            binding.layoutPemesananTerbaru.addView(
                buatTextView(
                    teks = "Belum ada data pemesanan.",
                    ukuran = 14f,
                    warna = warnaSekunder
                )
            )
            return
        }

        val terbaru = daftar
            .sortedByDescending { it.created_at.orEmpty() }
            .take(5)

        terbaru.forEach { pemesanan ->
            val kartu = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(Color.WHITE)
                elevation = dp(1).toFloat()
            }

            val judul = pemesanan.judul_kegiatan
                ?.takeIf { it.isNotBlank() }
                ?: "Kegiatan tanpa judul"

            val namaRuangan = pemesanan.nama_ruangan
                ?.takeIf { it.isNotBlank() }
                ?: "Ruangan tidak tersedia"

            val tanggal = pemesanan.tanggal
                ?.takeIf { it.isNotBlank() }
                ?: "-"

            val waktu = listOfNotNull(
                pemesanan.waktu_mulai?.takeIf { it.isNotBlank() },
                pemesanan.waktu_selesai?.takeIf { it.isNotBlank() }
            ).joinToString(" - ").ifBlank { "-" }

            val status = pemesanan.status
                ?.takeIf { it.isNotBlank() }
                ?: "Belum ada status"

            kartu.addView(
                buatTextView(
                    teks = judul,
                    ukuran = 15f,
                    warna = warnaTeks,
                    tebal = true
                )
            )

            kartu.addView(
                buatTextView(
                    teks = namaRuangan,
                    ukuran = 13f,
                    warna = warnaUtama,
                    marginAtas = 5
                )
            )

            kartu.addView(
                buatTextView(
                    teks = "Tanggal: $tanggal",
                    ukuran = 12f,
                    warna = warnaSekunder,
                    marginAtas = 8
                )
            )

            kartu.addView(
                buatTextView(
                    teks = "Waktu: $waktu",
                    ukuran = 12f,
                    warna = warnaSekunder,
                    marginAtas = 3
                )
            )

            kartu.addView(
                buatTextView(
                    teks = "Status: $status",
                    ukuran = 12f,
                    warna = warnaTeks,
                    tebal = true,
                    marginAtas = 6
                )
            )

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }

            binding.layoutPemesananTerbaru.addView(kartu, params)
        }
    }

    private fun tampilkanGagal(pesan: String) {
        if (_binding == null) return

        binding.tvJumlahRuangan.text = "—"
        binding.tvJumlahPemesanan.text = "—"
        binding.layoutPemesananTerbaru.removeAllViews()

        binding.layoutPemesananTerbaru.addView(
            buatTextView(pesan, 14f, Color.RED)
        )
    }

    private fun tampilkanPesanError(pesan: String) {
        if (_binding == null) return

        binding.tvStatusDashboard.text = pesan
    }

    private fun buatTextView(
        teks: String,
        ukuran: Float,
        warna: Int,
        tebal: Boolean = false,
        marginAtas: Int = 0
    ): TextView {
        return TextView(requireContext()).apply {
            text = teks
            textSize = ukuran
            setTextColor(warna)
            gravity = Gravity.START

            if (tebal) {
                setTypeface(typeface, Typeface.BOLD)
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(marginAtas)
            }
        }
    }

    private fun dp(nilai: Int): Int {
        return (nilai * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
