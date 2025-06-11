package com.gemahripah.banksampah.ui.admin.transaksi.detail

import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi

sealed class DetailTransaksiUiState {
    object Loading : DetailTransaksiUiState()
    data class Success(val data: List<DetailTransaksiRelasi>) : DetailTransaksiUiState()
    data class Error(val message: String) : DetailTransaksiUiState()
    object Deleted : DetailTransaksiUiState()
}