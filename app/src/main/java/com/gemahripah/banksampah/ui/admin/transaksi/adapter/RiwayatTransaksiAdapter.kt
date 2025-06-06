package com.gemahripah.banksampah.ui.admin.transaksi.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.databinding.ItemRiwayatBinding

class RiwayatTransaksiAdapter(
    private var list: List<RiwayatTransaksi>,
    private val onItemClick: (RiwayatTransaksi) -> Unit
) : RecyclerView.Adapter<RiwayatTransaksiAdapter.ViewHolder>() {

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

        @SuppressLint("SetTextI18n")
        fun bind(item: RiwayatTransaksi) = with(binding) {
            resetView()

            nama.text = item.hari ?: item.nama
            tanggal.text = item.tanggal
            nominal.text = "Rp ${item.totalHarga?.toInt()}"

            if (item.tipe == "Masuk") {
                berat.visibility = View.VISIBLE
                berat.text = "${item.totalBerat ?: 0.0} Kg"
            } else {
                cardRiwayat.setCardBackgroundColor(merah)
                lurus.visibility = View.GONE
                berat.visibility = View.GONE

                (nominal.layoutParams as ConstraintLayout.LayoutParams).topToTop =
                    ConstraintLayout.LayoutParams.PARENT_ID

                tegak.setBackgroundColor(hitam)

                nama.typeface = boldFont
                tanggal.typeface = boldFont
                nominal.typeface = boldFont
            }

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<RiwayatTransaksi>) {
        list = newList
        notifyDataSetChanged()
    }
}