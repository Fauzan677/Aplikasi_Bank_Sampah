package com.gemahripah.banksampah.data.remote

data class SessionData(
    val userId: String,
    val accessToken: String,
    val isAdmin: Boolean
)