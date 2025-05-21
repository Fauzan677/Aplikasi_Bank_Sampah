package com.gemahripah.banksampah.data.model.sampah.gabungan

import android.os.Parcelable
import com.gemahripah.banksampah.data.model.sampah.Kategori
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SampahRelasi(
    val sphId: Long? = null,
    val created_at: String? = null,
    val sphKtgId: Kategori? = null,
    val sphJenis: String? = null,
    val sphSatuan: String? = null,
    val sphHarga: Double? = null,
    val sphKeterangan: String? = null
) : Parcelable
