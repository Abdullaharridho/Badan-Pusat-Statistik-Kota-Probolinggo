package com.example.bpskota.uiketuatim

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bpskota.uioperator.OperatorProfilFragment
import com.example.bpskota.uimaps.PetaKodeRumahFragment

class KetuaTimPagerAdapter(
    activity: AppCompatActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> KetuaTimSuratMasukFragment()
            1 -> KetuaTimTimFragment()
            2 -> KetuaTimBerandaFragment()
            3 -> PetaKodeRumahFragment()
            4 -> OperatorProfilFragment()
            else -> KetuaTimBerandaFragment()
        }
    }
}