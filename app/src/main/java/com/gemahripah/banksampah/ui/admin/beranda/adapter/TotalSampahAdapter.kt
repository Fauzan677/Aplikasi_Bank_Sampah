package com.gemahripah.banksampah.ui.admin.beranda.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.beranda.TotalSampahPerJenis
import java.text.NumberFormat
import java.util.Locale

class TotalSampahAdapter(
    private val list: List<TotalSampahPerJenis>
) : RecyclerView.Adapter<TotalSampahAdapter.ViewHolder>() {

    private val nf2 = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val jenis: TextView = itemView.findViewById(R.id.jenis)
        val kode: TextView = itemView.findViewById(R.id.kode)
        val nominal: TextView = itemView.findViewById(R.id.nominal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total_sampah, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.jenis.text = item.jenis_sampah
        holder.kode.text = item.sphKode

        val beratFormatted = nf2.format(item.total_berat)
        holder.nominal.text = "$beratFormatted ${item.sphSatuan ?: "Kg"}"
    }
}
