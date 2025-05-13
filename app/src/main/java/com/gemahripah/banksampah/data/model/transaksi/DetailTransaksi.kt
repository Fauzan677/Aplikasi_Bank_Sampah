package com.gemahripah.banksampah.data.model.transaksi

import kotlinx.serialization.Serializable

@Serializable
data class DetailTransaksi(
    val dtlId: Long? = null,
    val created_at: String? = null,
    val dtlTskId: Long? = null,
    val dtlSphId: Long? = null,
    val dtlJumlah: Double? = null
)

