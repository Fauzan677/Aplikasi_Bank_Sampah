package com.gemahripah.banksampah.ui.admin.pengumuman.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.databinding.ItemListPengumumanBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class PengumumanAdapter(
    private val list: List<Pengumuman>,
    private val onItemClick: (Pengumuman) -> Unit
) : RecyclerView.Adapter<PengumumanAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListPengumumanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListPengumumanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.binding.judul.text = item.pmnJudul ?: "-"

        // Format tanggal (jika ada)
        item.created_at?.let {
            try {
                val parsedDate = OffsetDateTime.parse(it)
                val formatted = parsedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                holder.binding.tanggal.text = formatted
            } catch (e: Exception) {
                holder.binding.tanggal.text = it
            }
        }

        // Tampilkan gambar jika ada
        val url = item.pmnGambar
        if (!url.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(url)
                .into(holder.binding.root.findViewById(R.id.gambar))
        }
    }
}