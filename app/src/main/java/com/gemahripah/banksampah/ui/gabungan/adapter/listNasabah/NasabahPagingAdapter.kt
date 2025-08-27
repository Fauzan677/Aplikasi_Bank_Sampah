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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class NasabahPagingAdapter(
    private val onClick: (Pengguna) -> Unit
) : PagingDataAdapter<Pengguna, NasabahPagingAdapter.NasabahViewHolder>(DIFF_CALLBACK) {

    private val nf2 = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

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

            val penggunaId = pengguna.pgnId
            if (penggunaId == null) {
                binding.berat.text = "-"
                binding.nominal.text = "-"
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // === BERAT: pakai hitung_total_jumlah_per_pengguna (start/end null) ===
                    val resultBerat = SupabaseProvider.client.postgrest.rpc(
                        "hitung_total_jumlah_per_pengguna",
                        buildJsonObject {
                            put("pgn_id_input", penggunaId)
                            put("start_date", JsonNull)   // kirim null eksplisit
                            put("end_date", JsonNull)
                        }
                    )
                    val berat = resultBerat.data.toDoubleOrNull() ?: 0.0

                    // === SALDO: langsung dari field pengguna (tanpa RPC) ===
                    val saldo = pengguna.pgnSaldo?.toDouble() ?: 0.0

                    withContext(Dispatchers.Main) {
                        binding.berat.text = "${nf2.format(berat)} Kg"
                        binding.nominal.text = "Rp ${nf2.format(saldo)}"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.berat.text = "Error"
                        binding.nominal.text = "Error"
                    }
                }
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