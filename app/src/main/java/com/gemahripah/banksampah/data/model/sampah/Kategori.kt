package com.gemahripah.banksampah.data.model.sampah

import kotlinx.serialization.Serializable

@Serializable
data class Kategori(
    val id: Long? = null,
    val created_at: String? = null,
    val nama: String
)

