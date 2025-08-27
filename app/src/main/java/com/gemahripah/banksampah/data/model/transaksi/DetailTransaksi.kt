package com.gemahripah.banksampah.data.model.transaksi

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class DetailTransaksi(
    val dtlId: Long? = null,           // int8
    val created_at: String? = null,    // date ("yyyy-MM-dd")
    val dtlTskId: Long? = null,        // int8
    val dtlSphId: Long? = null,        // int8
    @Contextual
    val dtlJumlah: BigDecimal? = null, // numeric (kg, 3 desimal)
    @Contextual
    val dtlNominal: BigDecimal? = null // numeric (rupiah)
)