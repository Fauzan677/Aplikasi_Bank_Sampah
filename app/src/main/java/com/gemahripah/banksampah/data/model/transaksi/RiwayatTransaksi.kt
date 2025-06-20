package com.gemahripah.banksampah.data.model.transaksi

import android.os.Parcelable
import kotlinx.serialization.Serializable

@kotlinx.parcelize.Parcelize
@Serializable
data class RiwayatTransaksi(
    val tskId: Long,
    val createdAt: String,
    val tskIdPengguna: String?,
    val nama: String,
    val tanggal: String,
    val tipe: String,
    val tskKeterangan: String?,
    val totalBerat: Double?, // null jika tipe keluar
    val totalHarga: Double?, // selalu tampil
    val hari: String?
) : Parcelable
