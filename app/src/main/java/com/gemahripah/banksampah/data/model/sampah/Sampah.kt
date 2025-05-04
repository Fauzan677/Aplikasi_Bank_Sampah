package com.gemahripah.banksampah.data.model.sampah

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Sampah(
    val id: Long? = null,
    val created_at: String? = null,
    val kategori: Long,
    val jenis: String,
    val satuan: String? = null,
    val harga: Int? = null,
    val keterangan: String? = null
) : Parcelable