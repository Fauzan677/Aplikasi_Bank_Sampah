package com.gemahripah.banksampah.ui.gabungan.adapter.pengumuman

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.databinding.ItemListPengumumanBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PengumumanPagingAdapter(
    private val onItemClick: (Pengumuman) -> Unit
) : PagingDataAdapter<Pengumuman, PengumumanPagingAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val binding: ItemListPengumumanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListPengumumanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.binding.judul.text = item.pmnJudul ?: "-"

        item.created_at?.let {
            try {
                val parsedDate = OffsetDateTime.parse(it)
                val formatted = parsedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id","ID")))
                holder.binding.tanggal.text = formatted
            } catch (e: Exception) {
                holder.binding.tanggal.text = it
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Pengumuman>() {
            override fun areItemsTheSame(old: Pengumuman, new: Pengumuman) = old.pmnId == new.pmnId
            override fun areContentsTheSame(old: Pengumuman, new: Pengumuman) = old == new
        }
    }
}