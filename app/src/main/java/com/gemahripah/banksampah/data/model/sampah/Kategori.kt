package com.gemahripah.banksampah.data.model.sampah

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Kategori(
    val ktgId: Long? = null,
    val created_at: String? = null,
    val ktgNama: String? = null
) : Parcelable