package com.example.bpskota.uioperator

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bpskota.uimaps.PetaKodeRumahFragment

class OperatorPagerAdapter(
    activity: AppCompatActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OperatorRuanganFragment()
            1 -> OperatorPemesananFragment()
            2 -> OperatorSuratMasukFragment()
            3 -> OperatorBerandaFragment()
            4 -> PetaKodeRumahFragment()
            5 -> OperatorProfilFragment()
            else -> OperatorBerandaFragment()
        }
    }
}