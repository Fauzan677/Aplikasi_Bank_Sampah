package com.gemahripah.banksampah.data.model.sampah

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Sampah(
    val sphId: Long? = null,           // int8
    val created_at: String? = null,    // timestamptz (ISO 8601 string)
    val sphKtgId: Long? = null,        // int8
    val sphJenis: String? = null,      // text
    val sphSatuan: String? = null,     // text
    val sphHarga: Long? = null,        // int8 -> Long (harga tanpa desimal)
    val sphKeterangan: String? = null, // text
    val sphKode: String? = null        // text
) : Parcelable