package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.KategoridanSampah
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.ItemJenisSampahBinding
import com.gemahripah.banksampah.ui.admin.pengaturan.jenis.JenisSampahFragmentDirections
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KategoriAdapter(
    private var kategoriList: List<Kategori>,
    // onClick sekarang mengembalikan SampahRelasi
    private val onSampahClick: (SampahRelasi) -> Unit
) : RecyclerView.Adapter<KategoriAdapter.KategoriViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateKategoriList(newList: List<Kategori>) {
        kategoriList = newList
        notifyDataSetChanged()
    }

    inner class KategoriViewHolder(private val binding: ItemJenisSampahBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(kategori: Kategori) {
            binding.kategori.text = kategori.ktgNama

            binding.kategori.setOnClickListener {
                val action = JenisSampahFragmentDirections
                    .actionJenisSampahFragmentToEditKategoriFragment(kategori)
                itemView.findNavController().navigate(action)
            }

            // ambil sampah dalam kategori ini sebagai SampahRelasi (embed kategori)
            CoroutineScope(Dispatchers.IO).launch {
                val columns = Columns.raw("""
                    sphId,
                    created_at,
                    sphJenis,
                    sphSatuan,
                    sphHarga,
                    sphKeterangan,
                    sphKode,
                    sphKtgId:kategori(
                      ktgId,
                      created_at,
                      ktgNama
                    )
                """.trimIndent())

                try {
                    val jenisList: List<SampahRelasi> = SupabaseProvider.client
                        .from("sampah")
                        .select(columns = columns) {
                            filter { eq("sphKtgId", kategori.ktgId ?: -1) }
                        }
                        .decodeList()

                    withContext(Dispatchers.Main) {
                        binding.kartuSampah.apply {
                            layoutManager = GridLayoutManager(binding.root.context, 2)
                            isNestedScrollingEnabled = false
                            setHasFixedSize(false)
                            adapter = SampahAdapter(jenisList, onSampahClick)
                        }
                    }
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