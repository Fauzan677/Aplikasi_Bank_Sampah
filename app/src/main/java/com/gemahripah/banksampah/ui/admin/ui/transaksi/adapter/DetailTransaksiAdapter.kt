package com.gemahripah.banksampah.ui.admin.ui.transaksi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksiItem
import com.gemahripah.banksampah.databinding.ItemDetailTransaksiBinding

class DetailTransaksiAdapter(
    private val detailList: List<DetailTransaksiItem>
) : RecyclerView.Adapter<DetailTransaksiAdapter.DetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemDetailTransaksiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        val detail = detailList[position]
        holder.bind(detail)
    }

    override fun getItemCount(): Int = detailList.size

    inner class DetailViewHolder(private val binding: ItemDetailTransaksiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(detail: DetailTransaksiItem) {
            // Mengisi jenis, berat, dan harga di TextView sesuai dengan data
            binding.jenis.text = detail.jenis
            binding.berat.text = detail.berat.toString()
            binding.harga.text = detail.harga.toString()
        }
    }
}
