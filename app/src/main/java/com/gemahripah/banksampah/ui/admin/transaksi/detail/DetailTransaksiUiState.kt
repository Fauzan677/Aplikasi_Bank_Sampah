package com.gemahripah.banksampah.ui.admin.transaksi.detail

import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi

sealed class DetailTransaksiUiState {
    data object Loading : DetailTransaksiUiState()
    data class Success(val data: List<DetailTransaksiRelasi>) : DetailTransaksiUiState()
    data class Error(val message: String) : DetailTransaksiUiState()
    data object Deleted : DetailTransaksiUiState()
}