package com.gemahripah.banksampah.ui.admin.transaksi.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.databinding.ItemRiwayatBinding

class RiwayatTransaksiAdapter(
    private val list: List<RiwayatTransaksi>,
    private val onItemClick: (RiwayatTransaksi) -> Unit
) : RecyclerView.Adapter<RiwayatTransaksiAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRiwayatBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: RiwayatTransaksi) = with(binding) {
            nama.text = item.hari ?: item.nama
            tanggal.text = item.tanggal
            nominal.text = "Rp ${item.totalHarga?.toInt()}"
            if (item.tipe == "Masuk") {
                berat.visibility = View.VISIBLE
                berat.text = "${item.totalBerat ?: 0.0} Kg"
            } else {
                berat.visibility = View.GONE
                root.setCardBackgroundColor(root.context.getColor(R.color.merah))
            }

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

}


