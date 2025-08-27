package com.gemahripah.banksampah.data.model.transaksi.gabungan

import android.os.Parcelable
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Parcelize
@Serializable
data class DetailTransaksiRelasi(
    val dtlId: Long? = null,
    val created_at: String? = null,
    val dtlTskId: TransaksiRelasi? = null,   // relasi ke transaksi (opsional)
    val dtlSphId: SampahRelasi? = null,      // relasi ke sampah (opsional)

    @Contextual val dtlJumlah: BigDecimal? = null,   // numeric(…)
    @Contextual val dtlNominal: BigDecimal? = null   // numeric(…)
) : Parcelable