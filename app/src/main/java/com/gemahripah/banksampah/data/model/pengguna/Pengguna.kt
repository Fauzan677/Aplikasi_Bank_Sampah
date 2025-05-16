package com.gemahripah.banksampah.data.model.pengguna

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Pengguna(
    val pgnId: String? = null,
    val created_at: String? = null,
    val pgnNama: String? = null,
    val pgnEmail: String? = null,
    val pgnIsAdmin: Boolean? = null
) : Parcelable

