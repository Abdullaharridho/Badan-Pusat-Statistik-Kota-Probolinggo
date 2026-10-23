
package com.example.bpskota.uipegawai

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bpskota.uimaps.PetaKodeRumahFragment
import com.example.bpskota.uioperator.OperatorProfilFragment

class PegawaiPagerAdapter(
    activity: AppCompatActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PegawaiPenugasanFragment()
            1 -> PegawaiKegiatanFragment()
            2 -> PegawaiBerandaFragment()
            3 -> PetaKodeRumahFragment()
            4 -> OperatorProfilFragment()

            else -> throw IllegalArgumentException(
                "Posisi halaman tidak valid: $position"
            )
        }
    }
}
