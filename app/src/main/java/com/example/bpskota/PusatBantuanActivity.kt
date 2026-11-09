
package com.example.bpskota

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PusatBantuanActivity : AppCompatActivity() {

    private data class AccordionItem(
        val header: View,
        val description: TextView,
        val arrow: ImageView,
        var expanded: Boolean = false,
        var animator: ValueAnimator? = null
    )

    private val accordionItems = mutableListOf<AccordionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pusat_bantuan)

        // Tombol kembali
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Daftarkan seluruh card accordion
        setupAccordion(
            R.id.headerBeranda,
            R.id.descBeranda,
            R.id.arrowBeranda
        )

        setupAccordion(
            R.id.headerNavigasi,
            R.id.descNavigasi,
            R.id.arrowNavigasi
        )

        setupAccordion(
            R.id.headerInformasi,
            R.id.descInformasi,
            R.id.arrowInformasi
        )

        setupAccordion(
            R.id.headerBerita,
            R.id.descBerita,
            R.id.arrowBerita
        )

        setupAccordion(
            R.id.headerInfografis,
            R.id.descInfografis,
            R.id.arrowInfografis
        )

        setupAccordion(
            R.id.headerPublikasi,
            R.id.descPublikasi,
            R.id.arrowPublikasi
        )

        setupAccordion(
            R.id.headerMenuData,
            R.id.descMenuData,
            R.id.arrowMenuData
        )

        setupAccordion(
            R.id.headerKategoriData,
            R.id.descKategoriData,
            R.id.arrowKategoriData
        )

        setupAccordion(
            R.id.headerDetailData,
            R.id.descDetailData,
            R.id.arrowDetailData
        )

        setupAccordion(
            R.id.headerLainnya,
            R.id.descLainnya,
            R.id.arrowLainnya
        )

        setupAccordion(
            R.id.headerAkun,
            R.id.descAkun,
            R.id.arrowAkun
        )

        setupAccordion(
            R.id.headerKoneksi,
            R.id.descKoneksi,
            R.id.arrowKoneksi
        )
    }

    private fun setupAccordion(
        headerId: Int,
        descriptionId: Int,
        arrowId: Int
    ) {
        val header = findViewById<View>(headerId)
        val description = findViewById<TextView>(descriptionId)
        val arrow = findViewById<ImageView>(arrowId)

        // Pastikan semua deskripsi tertutup saat pertama dibuka
        description.visibility = View.GONE
        description.alpha = 0f

        val item = AccordionItem(
            header = header,
            description = description,
            arrow = arrow
        )

        accordionItems.add(item)

        // Klik bagian header card
        header.setOnClickListener {
            toggleAccordion(item)
        }

        // Klik ikon panah
        arrow.setOnClickListener {
            toggleAccordion(item)
        }
    }

    private fun toggleAccordion(item: AccordionItem) {
        // Hentikan animasi sebelumnya jika masih berjalan
        item.animator?.cancel()

        if (item.expanded) {
            collapseAccordion(item)
        } else {
            expandAccordion(item)
        }

        item.expanded = !item.expanded
    }

    private fun expandAccordion(item: AccordionItem) {
        val description = item.description
        val arrow = item.arrow

        description.visibility = View.VISIBLE
        description.alpha = 0f

        // Ukur tinggi asli deskripsi
        description.measure(
            View.MeasureSpec.makeMeasureSpec(
                description.width,
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )
        )

        val targetHeight = description.measuredHeight

        // Mulai dari tinggi 0
        val params = description.layoutParams
        params.height = 0
        description.layoutParams = params

        // Animasi tinggi deskripsi
        val animator = ValueAnimator.ofInt(0, targetHeight)

        animator.duration = 280
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int

            description.layoutParams = description.layoutParams.apply {
                this.height = height
            }
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Kembalikan tinggi ke ukuran otomatis
                description.layoutParams =
                    description.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }

                description.alpha = 1f
            }
        })

        animator.start()
        item.animator = animator

        // Animasi transparansi
        description.animate()
            .alpha(1f)
            .setDuration(280)
            .start()

        // Putar ikon panah
        arrow.animate()
            .rotation(180f)
            .setDuration(220)
            .start()
    }

    private fun collapseAccordion(item: AccordionItem) {
        val description = item.description
        val arrow = item.arrow

        val initialHeight = description.height

        // Animasi menutup deskripsi
        val animator = ValueAnimator.ofInt(initialHeight, 0)

        animator.duration = 250
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int

            description.layoutParams = description.layoutParams.apply {
                this.height = height
            }
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                description.visibility = View.GONE
                description.alpha = 0f

                description.layoutParams =
                    description.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
            }
        })

        animator.start()
        item.animator = animator

        // Animasi ikon panah kembali
        arrow.animate()
            .rotation(0f)
            .setDuration(220)
            .start()
    }

    override fun onDestroy() {
        // Bersihkan animasi ketika Activity ditutup
        accordionItems.forEach { item ->
            item.animator?.cancel()
            item.header.animate().cancel()
            item.description.animate().cancel()
            item.arrow.animate().cancel()
        }

        super.onDestroy()
    }
}
