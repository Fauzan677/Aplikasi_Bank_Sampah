package com.gemahripah.banksampah.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Pengguna(
    val pgnId: String? = null,
    val created_at: String? = null,
    val pgnNama: String? = null,
    val pgnEmail: String? = null,
    val pgnIsAdmin: Boolean? = null
)


