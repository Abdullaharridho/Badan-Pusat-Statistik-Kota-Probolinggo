package com.example.bpskota.uipimpinan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bpskota.R
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.databinding.FragmentPimpinanLainnyaBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PimpinanLainnyaFragment :
    Fragment(R.layout.fragment_pimpinan_lainnya) {

    private var _binding: FragmentPimpinanLainnyaBinding? = null
    private val binding get() = _binding!!

    private lateinit var authSession: BpskpAuthSession

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPimpinanLainnyaBinding.bind(view)
        authSession = BpskpAuthSession(requireContext())

        setupNavigasi()
        muatRingkasan()
    }

    private fun setupNavigasi() {
        binding.cardMenuSuratPimpinan.setOnClickListener {
            bukaActivity(PimpinanSuratMasukActivity::class.java)
        }

        binding.cardMenuDataTimPimpinan.setOnClickListener {
            bukaActivity(PimpinanDataTimActivity::class.java)
        }
    }

    private fun bukaActivity(activityClass: Class<*>) {
        startActivity(Intent(requireContext(), activityClass))
    }

    private fun muatRingkasan() {
        val token = authSession.getToken().orEmpty()

        if (token.isBlank()) {
            tampilkanRingkasan("-", "-")
            return
        }

        val authorization =
            if (token.startsWith("Bearer ")) token
            else "Bearer $token"

        binding.tvRingkasanSuratPimpinan.text = "..."
        binding.tvRingkasanTimPimpinan.text = "..."

        muatTotalSurat(authorization)
        muatTotalTim(authorization)
    }

    private fun muatTotalSurat(authorization: String) {
        BpskpRetrofitClient.api.getSuratMasuk(
            authorization = authorization,
            search = null,
            status = null
        ).enqueue(object : Callback<com.example.bpskota.bpskp.model.SuratMasukListResponse> {

            override fun onResponse(
                call: Call<com.example.bpskota.bpskp.model.SuratMasukListResponse>,
                response: Response<com.example.bpskota.bpskp.model.SuratMasukListResponse>
            ) {
                if (_binding == null) return

                if (response.isSuccessful) {
                    val total = response.body()?.data?.size ?: 0
                    binding.tvRingkasanSuratPimpinan.text = total.toString()
                } else {
                    binding.tvRingkasanSuratPimpinan.text = "-"
                }
            }

            override fun onFailure(
                call: Call<com.example.bpskota.bpskp.model.SuratMasukListResponse>,
                t: Throwable
            ) {
                if (_binding == null) return

                binding.tvRingkasanSuratPimpinan.text = "-"
            }
        })
    }

    private fun muatTotalTim(authorization: String) {
        BpskpRetrofitClient.api.getTim(
            authorization = authorization,
            search = null,
            status = null
        ).enqueue(object : Callback<com.example.bpskota.bpskp.model.TimListResponse> {

            override fun onResponse(
                call: Call<com.example.bpskota.bpskp.model.TimListResponse>,
                response: Response<com.example.bpskota.bpskp.model.TimListResponse>
            ) {
                if (_binding == null) return

                if (response.isSuccessful) {
                    val total = response.body()?.data?.size ?: 0
                    binding.tvRingkasanTimPimpinan.text = total.toString()
                } else {
                    binding.tvRingkasanTimPimpinan.text = "-"
                }
            }

            override fun onFailure(
                call: Call<com.example.bpskota.bpskp.model.TimListResponse>,
                t: Throwable
            ) {
                if (_binding == null) return

                binding.tvRingkasanTimPimpinan.text = "-"
            }
        })
    }

    private fun tampilkanRingkasan(surat: String, tim: String) {
        binding.tvRingkasanSuratPimpinan.text = surat
        binding.tvRingkasanTimPimpinan.text = tim
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}