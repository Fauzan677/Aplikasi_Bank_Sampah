package com.gemahripah.banksampah.data.model.pengumuman

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Pengumuman(
    val pmnId: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val pmnIsPublic: Boolean? = null,
    val pmnJudul: String? = null,
    val pmnIsi: String? = null,
    val pmnGambar: String? = null,
    val pmnPin: Boolean? = null
) : Parcelable

