package com.gemahripah.banksampah.data.model.sampah

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Sampah(
    val sphId: Long? = null,
    val created_at: String? = null,
    val sphKtgId: Long? = null,
    val sphJenis: String? = null,
    val sphSatuan: String? = null,
    val sphHarga: Double? = null,
    val sphKeterangan: String? = null
) : Parcelable