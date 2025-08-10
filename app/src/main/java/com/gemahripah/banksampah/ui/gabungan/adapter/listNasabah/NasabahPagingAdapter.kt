package com.gemahripah.banksampah.ui.gabungan.adapter.listNasabah

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.ItemListNasabahBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class NasabahPagingAdapter(
    private val onClick: (Pengguna) -> Unit
) : PagingDataAdapter<Pengguna, NasabahPagingAdapter.NasabahViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NasabahViewHolder {
        val binding = ItemListNasabahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NasabahViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NasabahViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class NasabahViewHolder(val binding: ItemListNasabahBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(pengguna: Pengguna) {
            binding.nama.text = pengguna.pgnNama ?: "-"

            binding.root.setOnClickListener { onClick(pengguna) }

            pengguna.pgnId?.let { penggunaId ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resultBerat = SupabaseProvider.client.postgrest.rpc(
                            "hitung_total_jumlah_per_pengguna_masuk",
                            buildJsonObject { put("pgn_id_input", penggunaId) }
                        )
                        val berat = resultBerat.data.toDoubleOrNull() ?: 0.0

                        val resultSaldo = SupabaseProvider.client.postgrest.rpc(
                            "hitung_saldo_pengguna",
                            buildJsonObject { put("pgn_id_input", penggunaId) }
                        )
                        val saldo = resultSaldo.data.toDoubleOrNull() ?: 0.0

                        withContext(Dispatchers.Main) {
                            val formattedSaldo = NumberFormat.getNumberInstance(Locale("in", "ID")).format(saldo.toInt())

                            binding.berat.text = "%.2f Kg".format(berat)
                            binding.nominal.text = "Rp $formattedSaldo"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
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
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Pengguna>() {
            override fun areItemsTheSame(oldItem: Pengguna, newItem: Pengguna): Boolean {
                return oldItem.pgnId == newItem.pgnId
            }

            override fun areContentsTheSame(oldItem: Pengguna, newItem: Pengguna): Boolean {
                return oldItem == newItem
            }
        }
    }
}