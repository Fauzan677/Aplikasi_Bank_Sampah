package com.gemahripah.banksampah.ui.admin.pengumuman

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.gemahripah.banksampah.ui.gabungan.paging.pengumuman.PengumumanPagingSource

class PengumumanViewModel : ViewModel() {

    val pager = Pager(PagingConfig(
        pageSize = 5,
        initialLoadSize = 5,
        enablePlaceholders = false
    )) {
        PengumumanPagingSource()
    }.flow.cachedIn(viewModelScope)
}