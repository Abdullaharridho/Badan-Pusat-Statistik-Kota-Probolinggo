package com.example.bpskota.uisuperadmin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.databinding.FragmentSuperAdminHomeBinding

class SuperAdminHomeFragment : Fragment() {

    private var _binding: FragmentSuperAdminHomeBinding? = null
    private val binding get() = _binding!!

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

        val authSession = BpskpAuthSession(requireContext())

        val nama = authSession.getName()

        binding.tvSuperAdminName.text =
            if (!nama.isNullOrBlank()) {
                nama
            } else {
                "Super Admin"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}