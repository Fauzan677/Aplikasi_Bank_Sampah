package com.gemahripah.banksampah.data.model.pengumuman

import kotlinx.serialization.Serializable

@Serializable
data class Pengumuman(
    val pmnId: Long? = null,
    val created_at: String? = null,
    val pmnIsPublic: Boolean? = null,
    val pmnJudul: String? = null,
    val pmnIsi: String? = null,
    val pmnGambar: String? = null,
    val pmnPin: Boolean? = null
)

