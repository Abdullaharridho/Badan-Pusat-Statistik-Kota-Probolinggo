
package com.example.bpskota.uipimpinan

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bpskota.uimaps.PetaKodeRumahFragment
import com.example.bpskota.uioperator.OperatorProfilFragment

class PimpinanPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PimpinanKegiatanFragment()
            1 -> PetaKodeRumahFragment()
            2 -> PimpinanBerandaFragment()
            3 -> PimpinanLainnyaFragment()
            4 -> OperatorProfilFragment()

            else -> PimpinanBerandaFragment()
        }
    }
}
