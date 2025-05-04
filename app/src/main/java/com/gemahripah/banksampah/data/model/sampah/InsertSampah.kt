package com.gemahripah.banksampah.data.model.sampah

import kotlinx.serialization.Serializable

@Serializable
data class InsertSampah(
    val kategori: Long,
    val jenis: String,
    val satuan: String,
    val harga: Long,
    val keterangan: String? = null
)
