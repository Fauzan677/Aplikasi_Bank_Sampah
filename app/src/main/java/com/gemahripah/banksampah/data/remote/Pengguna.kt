package com.gemahripah.banksampah.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Pengguna(
    val id: String,
    val created_at: String?,
    val nama: String?,
    val email: String?,
    val admin: Boolean
)

