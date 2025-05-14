package com.gemahripah.banksampah.ui.admin.pengaturan.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.databinding.ItemTotalSampahBinding
import android.view.LayoutInflater
import com.gemahripah.banksampah.data.model.sampah.KategoridanSampah

class SampahAdapter(
    private val list: List<KategoridanSampah>,
    private val onItemClick: (KategoridanSampah) -> Unit
) : RecyclerView.Adapter<SampahAdapter.SampahViewHolder>() {

    inner class SampahViewHolder(private val binding: ItemTotalSampahBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: KategoridanSampah) {
            binding.jenis.text = data.sampah.sphJenis
            binding.nominal.text = "${data.sampah.sphHarga ?: 0}"

            binding.root.setOnClickListener {
                onItemClick(data)
            }
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
