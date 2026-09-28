package com.example.bpskota.uisuperadmin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bpskota.uisuperadmin.ManagementTimActivity
import com.example.bpskota.databinding.FragmentSuperAdminMenuBinding

class SuperAdminMenuFragment : Fragment() {

    private var _binding: FragmentSuperAdminMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuperAdminMenuBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
    }

    private fun setupMenu() {

        // Menu Surat Masuk
        binding.cardSuratMasuk.setOnClickListener {
            val intent = Intent(
                requireContext(),
                SASuratMasukActivity::class.java
            )

            startActivity(intent)
        }

        // Menu Management Tim
        binding.cardManagementTim.setOnClickListener {
            val intent = Intent(
                requireContext(),
                ManagementTimActivity::class.java
            )

            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}