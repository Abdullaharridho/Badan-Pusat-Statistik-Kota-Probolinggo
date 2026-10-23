
package com.example.bpskota.uipimpinan

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.bpskota.R

class PimpinanBerandaFragment :
    Fragment(R.layout.fragment_pimpinan_beranda) {

    private var viewPager: ViewPager2? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = requireActivity().findViewById(
            R.id.pimpinanViewPager
        )

        setupGreeting(view)
        setupQuickNavigation(view)
    }

    private fun setupGreeting(view: View) {
        view.findViewById<TextView>(
            R.id.tvPimpinanBerandaGreeting
        ).text = "Selamat Datang, Pimpinan"

        view.findViewById<TextView>(
            R.id.tvPimpinanBerandaDescription
        ).text =
            "Pantau penugasan, kegiatan, dan informasi wilayah " +
                    "BPS Kota Probolinggo melalui satu halaman."
    }

    private fun setupQuickNavigation(view: View) {

        view.findViewById<View>(
            R.id.btnBerandaLihatKegiatan
        ).setOnClickListener {
            openPage(0)
        }

        view.findViewById<View>(
            R.id.btnBerandaLihatPeta
        ).setOnClickListener {
            openPage(1)
        }
    }

    private fun openPage(position: Int) {
        viewPager?.setCurrentItem(position, true)
    }

    override fun onDestroyView() {
        viewPager = null
        super.onDestroyView()
    }
}
