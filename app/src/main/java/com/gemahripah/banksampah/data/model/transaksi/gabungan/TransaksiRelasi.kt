package com.gemahripah.banksampah.data.model.transaksi.gabungan

import android.os.Parcelable
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TransaksiRelasi(
    val tskId: Long? = null,
    val created_at: String? = null,
    val tskIdPengguna: Pengguna? = null,
    val tskGambar: String? = null,
    val tskKeterangan: String? = null,
    val tskTipe: String? = null,
) : Parcelable
