package com.gemahripah.banksampah.data.model.sampah.gabungan

import android.os.Parcelable
import com.gemahripah.banksampah.data.model.sampah.Kategori
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SampahRelasi(
    val sphId: Long? = null,
    val created_at: String? = null,          // timestamptz → String
    val sphKtgId: Kategori? = null,          // relasi kategori
    val sphJenis: String? = null,
    val sphSatuan: String? = null,
    val sphHarga: Long? = null,              // int8 → Long?
    val sphKeterangan: String? = null,
    val sphKode: String? = null              // ada di schema
) : Parcelable