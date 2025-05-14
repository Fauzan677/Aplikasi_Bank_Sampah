package com.gemahripah.banksampah.ui.admin.pengaturan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.KategoridanSampah
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.ItemJenisSampahBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KategoriAdapter(
    private var kategoriList: List<Kategori>,
    private val onSampahClick: (KategoridanSampah) -> Unit  // ubah param
) : RecyclerView.Adapter<KategoriAdapter.KategoriViewHolder>() {

    fun updateKategoriList(newList: List<Kategori>) {
        kategoriList = newList
        notifyDataSetChanged()  // atau gunakan DiffUtil untuk performa lebih baik
    }

    inner class KategoriViewHolder(private val binding: ItemJenisSampahBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(kategori: Kategori) {
            binding.kategori.text = kategori.ktgNama

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val jenisList = SupabaseProvider.client
                        .from("sampah")
                        .select() {
                            filter {
                                eq("sphKtgId", kategori.ktgId ?: 0)
                            }
                        }
                        .decodeList<Sampah>()

                    // Buat list gabungan
                    val jenisWithKategori = jenisList.map {
                        KategoridanSampah(it, kategori.ktgNama)
                    }

                    val adapter = SampahAdapter(jenisWithKategori, onSampahClick)
                    binding.kartuSampah.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
                    binding.kartuSampah.adapter = adapter
                } catch (e: Exception) {
                    println("Gagal memuat jenis: ${e.message}")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KategoriViewHolder {
        val binding = ItemJenisSampahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KategoriViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KategoriViewHolder, position: Int) {
        holder.bind(kategoriList[position])
    }

    override fun getItemCount(): Int = kategoriList.size
}