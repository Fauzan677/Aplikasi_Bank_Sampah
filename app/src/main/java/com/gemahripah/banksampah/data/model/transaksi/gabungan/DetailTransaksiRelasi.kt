package com.gemahripah.banksampah.data.model.transaksi.gabungan

import android.os.Parcelable
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class DetailTransaksiRelasi(
    val dtlId: Long? = null,
    val created_at: String? = null,
    val dtlTskId: TransaksiRelasi? = null,
    val dtlSphId: SampahRelasi? = null,
    val dtlJumlah: Double? = null,
    val hargaDetail: Double? = null
) : Parcelable
