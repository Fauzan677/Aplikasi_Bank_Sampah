package com.gemahripah.banksampah.data.model.transaksi.gabungan

import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import kotlinx.serialization.Serializable


@Serializable
data class DetailTransaksiRelasi(
    val dtlId: Long? = null,
    val created_at: String? = null,
    val dtlTskId: Transaksi? = null,   // relasi lengkap ke Transaksi
    val dtlSphId: Sampah? = null,      // relasi lengkap ke Sampah
    val dtlJumlah: Double? = null
)
