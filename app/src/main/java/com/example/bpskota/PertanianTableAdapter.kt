package com.example.bpskota

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.bpskota.bps.model.SimdasiTable

class PertanianTableAdapter(
    private val container: LinearLayout,
    private val kategori: String,
    private val onItemClick: (SimdasiTable, Int) -> Unit
) {

    data class Item(
        val tahun: Int,
        val table: SimdasiTable
    )

    fun tampilkan(
        data: List<Item>
    ) {

        container.removeAllViews()

        val inflater =
            LayoutInflater.from(container.context)

        for (item in data) {

            val table = item.table

            val card =
                inflater.inflate(
                    R.layout.item_statistik,
                    container,
                    false
                )

            val tvKode =
                card.findViewById<TextView>(
                    R.id.tvKode
                )

            val tvTahun =
                card.findViewById<TextView>(
                    R.id.tvTahun
                )

            val tvKategori =
                card.findViewById<TextView>(
                    R.id.tvKategori
                )

            val tvJudul =
                card.findViewById<TextView>(
                    R.id.tvJudul
                )


            // KODE TABEL



            // TAHUN
            tvTahun.text =
                item.tahun.toString()


            // KATEGORI
            tvKategori.text =
                kategori


            // JUDUL
            tvJudul.text =
                table.judul
                    ?: "Judul tidak tersedia"


            // CLICK CARD
            card.setOnClickListener {

                onItemClick(
                    table,
                    item.tahun
                )
            }


            container.addView(card)
        }
    }
}