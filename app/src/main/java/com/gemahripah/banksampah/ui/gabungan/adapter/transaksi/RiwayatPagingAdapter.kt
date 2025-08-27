package com.gemahripah.banksampah.ui.gabungan.adapter.transaksi

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.databinding.ItemRiwayatBinding
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class RiwayatPagingAdapter(
    private val onItemClick: (RiwayatTransaksi) -> Unit
) : PagingDataAdapter<RiwayatTransaksi, RiwayatPagingAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RiwayatTransaksi>() {
            override fun areItemsTheSame(old: RiwayatTransaksi, new: RiwayatTransaksi) =
                old.tskId == new.tskId

            override fun areContentsTheSame(old: RiwayatTransaksi, new: RiwayatTransaksi) =
                old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val boldFont = ResourcesCompat.getFont(parent.context, R.font.nunito_semibold)
        val merah = ContextCompat.getColor(parent.context, R.color.merah)
        val hitam = ContextCompat.getColor(parent.context, R.color.black)
        val putih = ContextCompat.getColor(parent.context, R.color.white)
        val abu = ContextCompat.getColor(parent.context, R.color.abu2)
        return ViewHolder(binding, boldFont, merah, hitam, putih, abu)
    }

    inner class ViewHolder(
        private val binding: ItemRiwayatBinding,
        private val boldFont: Typeface?,
        private val merah: Int,
        private val hitam: Int,
        private val putih: Int,
        private val abu: Int
    ) : RecyclerView.ViewHolder(binding.root) {

        private fun resetView() = with(binding) {

            cardRiwayat.setCardBackgroundColor(putih)
            tegak.setBackgroundColor(abu)

            lurus.visibility = View.VISIBLE
            berat.visibility = View.GONE

            nama.setTextColor(hitam)
            tanggal.setTextColor(hitam)
            nominal.setTextColor(hitam)

            nama.typeface = null
            tanggal.typeface = null
            nominal.typeface = null

            val params = nominal.layoutParams as ConstraintLayout.LayoutParams
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            nominal.layoutParams = params
        }

        private val NF2 = NumberFormat.getNumberInstance(Locale("id","ID")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: RiwayatTransaksi) = with(binding) {
            resetView()

            val hargaFormatted = NF2.format(item.totalHarga ?: java.math.BigDecimal.ZERO)
            nominal.text = "Rp $hargaFormatted"

            nama.text = item.hari ?: item.nama
            tanggal.text = item.tanggal

            if (item.tipe == "Masuk") {
                berat.visibility = View.VISIBLE
                berat.text = "${NF2.format(item.totalBerat ?: java.math.BigDecimal.ZERO)} Kg"
            } else {
                cardRiwayat.setCardBackgroundColor(merah)
                lurus.visibility = View.GONE
                berat.visibility = View.GONE

                val params = nominal.layoutParams as ConstraintLayout.LayoutParams
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                nominal.layoutParams = params
                tegak.setBackgroundColor(hitam)

                nama.typeface = boldFont
                tanggal.typeface = boldFont
                nominal.typeface = boldFont
            }

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
}