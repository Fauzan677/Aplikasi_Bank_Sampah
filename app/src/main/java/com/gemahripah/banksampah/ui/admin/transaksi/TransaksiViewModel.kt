package com.gemahripah.banksampah.ui.admin.transaksi

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

class TransaksiViewModel : ViewModel() {
    private val _query = MutableStateFlow<String?>(null)
    private val _startDate = MutableStateFlow<String?>(null)
    private val _endDate = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData = combine(_query, _startDate, _endDate) { query, start, end ->
        Triple(query, start, end)
    }.debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (query, start, end) ->
        Pager(
            config = PagingConfig(
                pageSize = 5,
                initialLoadSize = 5,
                enablePlaceholders = false
            )
        ) {
            TransaksiPagingSource(SupabaseProvider.client, query, start, end)
        }.flow
    }.cachedIn(viewModelScope)

    fun setSearchQuery(query: String?) {
        _query.value = query?.trim() ?: ""
    }

    fun setStartDate(date: String?) {
        _startDate.value = date
    }

    fun setEndDate(date: String?) {
        _endDate.value = date
    }
}