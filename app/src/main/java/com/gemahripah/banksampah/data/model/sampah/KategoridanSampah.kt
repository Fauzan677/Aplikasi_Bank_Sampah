package com.gemahripah.banksampah.data.model.sampah

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class KategoridanSampah(
    val sampah: Sampah,
    val namaKategori: String?
) : Parcelable


