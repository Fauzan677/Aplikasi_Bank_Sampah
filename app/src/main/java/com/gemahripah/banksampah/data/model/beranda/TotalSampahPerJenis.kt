package com.gemahripah.banksampah.data.model.beranda

import kotlinx.serialization.Serializable

@Serializable
data class TotalSampahPerJenis(
    val jenis_sampah: String,
    val total_berat: Double
)

