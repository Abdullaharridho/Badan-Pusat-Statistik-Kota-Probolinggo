package com.example.bpskota.bps.ui.simdasi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bps.model.SimdasiTable

class SimdasiTableAdapter(
    private val onItemClick: (SimdasiTable) -> Unit
) : RecyclerView.Adapter<SimdasiTableAdapter.ViewHolder>() {

    private val items = mutableListOf<SimdasiTable>()

    fun submitList(data: List<SimdasiTable>) {

        items.clear()
        items.addAll(data)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_statistik,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvKode =
            itemView.findViewById<TextView>(
                R.id.tvKode
            )

        private val tvJudul =
            itemView.findViewById<TextView>(
                R.id.tvJudul
            )

        fun bind(table: SimdasiTable) {

            tvKode.text =
                table.kodeTabel ?: "-"

            tvJudul.text =
                table.judul ?: "Judul tidak tersedia"

            itemView.setOnClickListener {

                onItemClick(table)
            }
        }
    }
}