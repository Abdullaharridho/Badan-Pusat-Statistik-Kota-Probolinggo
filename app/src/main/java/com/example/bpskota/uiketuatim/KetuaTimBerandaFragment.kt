
package com.example.bpskota.uiketuatim

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentKetuaTimBerandaBinding
import com.example.bpskota.bpskp.model.Tim

class KetuaTimBerandaFragment : Fragment() {

    private var _binding: FragmentKetuaTimBerandaBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession
    private lateinit var repository: BpskpRepository

    private var idKetuaTim: Int? = null
    private var timKetua: Tim? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKetuaTimBerandaBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authSession = BpskpAuthSession(requireContext())
        repository = BpskpRepository()

        tampilkanLoadingAwal()
        ambilDataKetuaTim()
        ambilStatistikSuratMasuk()

        binding.btnLihatSemuaAnggota.setOnClickListener {
            pindahKeHalamanTim()
        }

        binding.btnLihatSemuaAnggotaHeader.setOnClickListener {
            pindahKeHalamanTim()
        }
    }

    private fun tampilkanLoadingAwal() {
        binding.tvNamaKetuaTim.text = "Memuat profil..."
        binding.tvNamaTimKetua.text = "Memuat informasi tim..."
        binding.tvJumlahAnggotaTim.text = "..."
        binding.tvJumlahSuratMasuk.text = "—"
        binding.tvJumlahPerluTindakLanjut.text = "—"
    }


    private fun ambilDataKetuaTim() {
        repository.getUser(authSession) { response, error ->
            if (!isAdded || _binding == null) return@getUser

            val user = response?.user

            if (user == null) {
                tampilkanError(
                    error ?: "Gagal mengambil profil pengguna."
                )
                return@getUser
            }

            binding.tvNamaKetuaTim.text =
                user.name.ifBlank { "Nama tidak tersedia" }

            idKetuaTim = user.id

            ambilTimMilikKetua(user.id)
        }
    }
    private fun ambilStatistikSuratMasuk() {
        val token = authSession.getToken()

        if (token.isNullOrBlank()) {
            binding.tvJumlahSuratMasuk.text = "—"
            binding.tvJumlahPerluTindakLanjut.text = "—"
            tampilkanError("Sesi login tidak ditemukan.")
            return
        }

        val authorization = "Bearer $token"

        repository.getSuratMasuk(authorization) { daftarSurat, error ->
            if (!isAdded || _binding == null) return@getSuratMasuk

            if (daftarSurat == null) {
                binding.tvJumlahSuratMasuk.text = "—"
                binding.tvJumlahPerluTindakLanjut.text = "—"

                tampilkanError(
                    error ?: "Gagal mengambil statistik surat masuk."
                )
                return@getSuratMasuk
            }

            // Total surat berdasarkan data yang dikembalikan API.
            binding.tvJumlahSuratMasuk.text = daftarSurat.size.toString()

            // Sementara belum menghitung status tindak lanjut karena
            // nilai status yang digunakan backend belum dikonfirmasi.
            binding.tvJumlahPerluTindakLanjut.text = "—"
        }
    }


    private fun ambilTimMilikKetua(userId: Int) {
        repository.getTim(authSession) { daftarTim, error ->
            if (!isAdded || _binding == null) return@getTim

            if (daftarTim == null) {
                tampilkanError(
                    error ?: "Gagal mengambil daftar tim."
                )
                return@getTim
            }

            // Cari tim berdasarkan ID ketua yang sedang login.
            val tim = daftarTim.firstOrNull {
                it.ketuaId == userId
            }

            if (tim == null) {
                timKetua = null

                binding.tvNamaTimKetua.text = "Belum memiliki tim"
                binding.tvDeskripsiTimKetua.text =
                    "Belum ada tim yang terhubung dengan akun Anda."
                binding.tvJumlahAnggotaTim.text = "0 orang"

                tampilkanAnggotaKosong(
                    "Belum ada anggota tim",
                    "Anggota akan muncul setelah tim ditetapkan."
                )
                return@getTim
            }

            timKetua = tim
            binding.tvNamaTimKetua.text = tim.namaTim
            binding.tvDeskripsiTimKetua.text =
                tim.keterangan?.takeIf { it.isNotBlank() }
                    ?: "Kelola informasi anggota dan pantau pekerjaan tim Anda."

            // Ambil data lengkap tim beserta anggotanya.
            repository.getDetailTim(authSession, tim.id) { detail, detailError ->
                if (!isAdded || _binding == null) return@getDetailTim

                if (detail == null) {
                    tampilkanAnggotaKosong(
                        "Anggota belum dapat dimuat",
                        detailError ?: "Silakan coba kembali."
                    )
                    return@getDetailTim
                }

                timKetua = detail

                val anggota = detail.anggota

                binding.tvNamaTimKetua.text = detail.namaTim
                binding.tvDeskripsiTimKetua.text =
                    detail.keterangan?.takeIf { it.isNotBlank() }
                        ?: "Kelola informasi anggota dan pantau pekerjaan tim Anda."

                binding.tvJumlahAnggotaTim.text = "${anggota.size} orang"

                tampilkanDaftarAnggota(
                    anggota.map { it.nama to it.role }
                )
            }
        }
    }

    private fun tampilkanDaftarAnggota(
        anggota: List<Pair<String, String>>
    ) {
        val container = binding.containerAnggotaTim
        container.removeAllViews()

        if (anggota.isEmpty()) {
            tampilkanAnggotaKosong(
                "Belum ada anggota tim",
                "Tambahkan anggota untuk menampilkan daftar di sini."
            )
            return
        }

        anggota.forEachIndexed { index, data ->
            val nama = data.first
            val role = data.second

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(12), dp(4), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val avatar = TextView(requireContext()).apply {
                text = inisial(nama)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#F97316"))
                gravity = android.view.Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FFF0E5"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    dp(42),
                    dp(42)
                )
            }

            val detail = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dp(12)
                }
            }

            val namaView = TextView(requireContext()).apply {
                text = nama
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#111827"))
            }

            val roleView = TextView(requireContext()).apply {
                text = role.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                textSize = 11f
                setTextColor(Color.parseColor("#6B7280"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(3)
                }
            }

            detail.addView(namaView)
            detail.addView(roleView)

            row.addView(avatar)
            row.addView(detail)
            container.addView(row)

            if (index < anggota.lastIndex) {
                val divider = View(requireContext()).apply {
                    setBackgroundColor(Color.parseColor("#F0EBE6"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                }
                container.addView(divider)
            }
        }
    }

    private fun tampilkanAnggotaKosong(
        judul: String,
        deskripsi: String
    ) {
        val container = binding.containerAnggotaTim
        container.removeAllViews()

        val wrapper = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(12), dp(24), dp(12), dp(24))
        }

        val titleView = TextView(requireContext()).apply {
            text = judul
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            gravity = android.view.Gravity.CENTER
        }

        val descriptionView = TextView(requireContext()).apply {
            text = deskripsi
            textSize = 11f
            setTextColor(Color.parseColor("#9CA3AF"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(5)
            }
        }

        wrapper.addView(titleView)
        wrapper.addView(descriptionView)
        container.addView(wrapper)
    }

    private fun tampilkanError(pesan: String) {
        if (!isAdded || _binding == null) return

        Toast.makeText(
            requireContext(),
            pesan,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun inisial(nama: String): String {
        return nama.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun pindahKeHalamanTim() {
        // Indeks 2 adalah halaman Tim pada KetuaTimPagerAdapter.
        (activity as? KetuaTimActivity)
            ?.findViewById<androidx.viewpager2.widget.ViewPager2>(
                com.example.bpskota.R.id.ketuaTimViewPager
            )
            ?.currentItem = 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}