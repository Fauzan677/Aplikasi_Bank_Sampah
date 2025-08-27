package com.gemahripah.banksampah.data.model.transaksi.gabungan

import android.os.Parcelable
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TransaksiRelasi(
    val tskId: Long? = null,
    val created_at: String? = null,          // date (yyyy-MM-dd)
    val tskIdPengguna: Pengguna? = null,     // relasi pengguna
    val tskKeterangan: String? = null,
    val tskTipe: String? = null              // "Masuk" / "Keluar"
) : Parcelable