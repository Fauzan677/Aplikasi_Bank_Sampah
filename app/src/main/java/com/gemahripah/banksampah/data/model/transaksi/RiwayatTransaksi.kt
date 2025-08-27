package com.gemahripah.banksampah.data.model.transaksi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Parcelize
@Serializable
data class RiwayatTransaksi(
    val tskId: Long,
    val createdAt: String,
    val tskIdPengguna: String?,
    val nama: String,
    val tanggal: String,
    val tipe: String,
    val tskKeterangan: String?,
    @Contextual val totalBerat: BigDecimal?,
    @Contextual val totalHarga: BigDecimal?,
    val hari: String?
) : Parcelable
