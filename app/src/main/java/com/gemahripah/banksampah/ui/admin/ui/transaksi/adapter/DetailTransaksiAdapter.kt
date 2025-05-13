package com.gemahripah.banksampah.ui.admin.ui.transaksi.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksiItem
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.databinding.ItemDetailTransaksiBinding

class DetailTransaksiAdapter(
    private val detailList: List<DetailTransaksiRelasi>
) : RecyclerView.Adapter<DetailTransaksiAdapter.DetailTransaksiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailTransaksiViewHolder {
        val binding = ItemDetailTransaksiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailTransaksiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailTransaksiViewHolder, position: Int) {
        val detail = detailList[position]
        holder.bind(detail)
    }

    override fun getItemCount(): Int = detailList.size

    class DetailTransaksiViewHolder(
        private val binding: ItemDetailTransaksiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(detail: DetailTransaksiRelasi) {
            binding.berat.text = "${detail.dtlJumlah ?: 0.0} kg"
            binding.jenis.text = detail.dtlSphId?.sphJenis ?: "-"
            binding.harga.text = detail.dtlJumlah.toString()
        }
    }
}


