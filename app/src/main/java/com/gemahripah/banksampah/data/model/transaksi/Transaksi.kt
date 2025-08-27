package com.gemahripah.banksampah.data.model.transaksi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Transaksi(
    val tskId: Long? = null,
    val created_at: String? = null,
    val tskIdPengguna: String? = null,
    val tskKeterangan: String? = null,
    val tskTipe: String? = null
) : Parcelable