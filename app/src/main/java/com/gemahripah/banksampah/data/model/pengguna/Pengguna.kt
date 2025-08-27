package com.gemahripah.banksampah.data.model.pengguna

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Parcelize
@Serializable
data class Pengguna(
    val pgnId: String? = null,
    val created_at: String? = null,
    val pgnNama: String? = null,
    val pgnEmail: String? = null,
    val pgnIsAdmin: Boolean? = null,
    @Contextual
    val pgnSaldo: BigDecimal? = null,   // ← ganti jadi BigDecimal? langsung
    val pgnAlamat: String? = null,
    val pgnRekening: String? = null
) : Parcelable
