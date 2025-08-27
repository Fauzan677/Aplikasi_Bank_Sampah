package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.databinding.ItemTotalSampahBinding
import android.view.LayoutInflater
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import java.text.NumberFormat
import java.util.Locale

class SampahAdapter(
    private val list: List<SampahRelasi>,
    private val onItemClick: (SampahRelasi) -> Unit
) : RecyclerView.Adapter<SampahAdapter.SampahViewHolder>() {

    private val rupiah: NumberFormat = NumberFormat.getNumberInstance(Locale("id","ID"))

    inner class SampahViewHolder(private val binding: ItemTotalSampahBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: SampahRelasi) {
            binding.jenis.text   = data.sphJenis ?: "-"
            binding.kode.text    = data.sphKode?.takeIf { it.isNotBlank() } ?: "-"
            binding.nominal.text = "Rp ${rupiah.format(data.sphHarga ?: 0)}"

            binding.root.setOnClickListener { onItemClick(data) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SampahViewHolder {
        val binding = ItemTotalSampahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SampahViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SampahViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}