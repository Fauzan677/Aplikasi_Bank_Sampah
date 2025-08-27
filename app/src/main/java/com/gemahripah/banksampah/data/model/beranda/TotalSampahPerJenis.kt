package com.gemahripah.banksampah.data.model.beranda

import kotlinx.serialization.Serializable

@Serializable
data class TotalSampahPerJenis(
    val sphKode: String,
    val jenis_sampah: String,
    val total_berat: Double,
    val sphSatuan: String? = null
)

