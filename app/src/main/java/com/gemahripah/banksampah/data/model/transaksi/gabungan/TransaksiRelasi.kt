package com.gemahripah.banksampah.data.model.transaksi.gabungan

import com.gemahripah.banksampah.data.model.pengguna.Pengguna

data class TransaksiRelasi(
    val tskId: Long,
    val created_at: String,
    val tskIdPengguna: Pengguna,  // Relasi dengan Pengguna
    val tskGambar: String?,
    val tskKeterangan: String?,
    val tskTipe: String?
)
