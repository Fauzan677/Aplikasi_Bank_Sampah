package com.gemahripah.banksampah.ui.nasabah.pengumuman.detail

import androidx.lifecycle.ViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetailPengumumanViewModel : ViewModel() {

    fun formatUpdatedAt(updatedAt: String?): String? {
        if (updatedAt.isNullOrBlank()) return null
        return try {
            val zdt = ZonedDateTime.parse(updatedAt)
            val fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))
            "Terakhir Diupdate: ${zdt.format(fmt)}"
        } catch (_: Exception) {
            null
        }
    }

    fun buildImageUrlWithVersion(baseUrl: String?, updatedAt: String?): String? {
        val url = baseUrl?.takeIf { it.isNotBlank() } ?: return null
        val ver = updatedAt ?: System.currentTimeMillis().toString()
        return "$url?v=$ver"
    }
}