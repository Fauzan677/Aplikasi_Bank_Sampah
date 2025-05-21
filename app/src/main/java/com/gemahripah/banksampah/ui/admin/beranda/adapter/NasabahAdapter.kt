package com.gemahripah.banksampah.ui.admin.beranda.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.ItemListNasabahBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

class NasabahAdapter(
    private var list: List<Pengguna>,
    private val onItemClick: (Pengguna) -> Unit
) : RecyclerView.Adapter<NasabahAdapter.ViewHolder>() {

    private var listFull: List<Pengguna> = ArrayList(list) // Salin data asli

    inner class ViewHolder(val binding: ItemListNasabahBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pengguna = list[adapterPosition]
                onItemClick(pengguna)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListNasabahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pengguna = list[position]
        val binding = holder.binding

        binding.nama.text = pengguna.pgnNama ?: "-"

        pengguna.pgnId?.let { penggunaId ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resultBerat = SupabaseProvider.client.postgrest.rpc(
                        "hitung_total_jumlah_per_pengguna_masuk",
                        buildJsonObject { put("pgn_id_input", penggunaId) }
                    )
                    val berat = resultBerat.data.toString().toDoubleOrNull() ?: 0.0

                    val resultSaldo = SupabaseProvider.client.postgrest.rpc(
                        "hitung_saldo_pengguna",
                        buildJsonObject { put("pgn_id_input", penggunaId) }
                    )
                    val saldo = resultSaldo.data.toString().toDoubleOrNull() ?: 0.0

                    launch(Dispatchers.Main) {
                        binding.berat.text = "%.2f Kg".format(berat)
                        binding.nominal.text = "Rp%,.0f".format(saldo).replace(",", ".")
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        binding.berat.text = "Error"
                        binding.nominal.text = "Error"
                    }
                }
            }
        } ?: run {
            binding.berat.text = "-"
            binding.nominal.text = "-"
        }
    }

    // Fungsi filter cari berdasarkan nama
    fun filterList(query: String) {
        val filteredList = if (query.isEmpty()) {
            listFull
        } else {
            listFull.filter { it.pgnNama?.lowercase(Locale.getDefault())?.contains(query.lowercase(Locale.getDefault())) == true }
        }
        list = filteredList
        notifyDataSetChanged()
    }

    // Update data (misal setelah fetch ulang dari server)
    fun updateData(newList: List<Pengguna>) {
        listFull = ArrayList(newList)
        list = newList
        notifyDataSetChanged()
    }
}