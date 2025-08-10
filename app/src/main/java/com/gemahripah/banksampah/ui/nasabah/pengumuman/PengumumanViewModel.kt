package com.gemahripah.banksampah.ui.nasabah.pengumuman

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.ui.gabungan.paging.pengumuman.PengumumanPagingSource
import kotlinx.coroutines.flow.Flow

class PengumumanViewModel : ViewModel() {
    fun pagingData(): Flow<PagingData<Pengumuman>> =
        Pager(
            config = PagingConfig(
                pageSize = 5,
                initialLoadSize = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PengumumanPagingSource() }
        ).flow.cachedIn(viewModelScope)
}