package com.gemahripah.banksampah.ui.gabungan.adapter.transaksi

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.databinding.ItemDetailTransaksiBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.text.NumberFormat

class DetailTransaksiAdapter(
    private val detailList: List<DetailTransaksiRelasi>
) : RecyclerView.Adapter<DetailTransaksiAdapter.DetailTransaksiViewHolder>() {

    // formatter 2 desimal dengan locale Indonesia
    private val nf2 = NumberFormat.getNumberInstance(Locale("id","ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailTransaksiViewHolder {
        val binding = ItemDetailTransaksiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DetailTransaksiViewHolder(binding, nf2)
    }

    override fun onBindViewHolder(holder: DetailTransaksiViewHolder, position: Int) {
        holder.bind(detailList[position])
    }

    override fun getItemCount(): Int = detailList.size

    class DetailTransaksiViewHolder(
        private val binding: ItemDetailTransaksiBinding,
        private val nf2: NumberFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(detail: DetailTransaksiRelasi) {
            // Jenis (sudah benar)
            binding.jenis.text = detail.dtlSphId?.sphJenis ?: "-"

            // Berat + satuan dari sampah.sphSatuan (default "Kg" jika kosong/null)
            val jumlah = (detail.dtlJumlah ?: BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP)
            val satuan = detail.dtlSphId?.sphSatuan?.takeIf { it.isNotBlank() } ?: "Kg"
            binding.berat.text = "${nf2.format(jumlah)} $satuan"

            // Harga dari dtlNominal (BigDecimal) dengan 3 desimal Indonesia
            val nominal = (detail.dtlNominal ?: BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP)
            binding.harga.text = nf2.format(nominal)
        }
    }
}