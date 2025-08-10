package com.gemahripah.banksampah.ui.admin.transaksi

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.ZoneId

class TransaksiViewModel : ViewModel() {
    private val _query = MutableStateFlow<String?>(null)
    private val _startDate = MutableStateFlow<String?>(null)
    private val _endDate = MutableStateFlow<String?>(null)

    private fun toUtcStartInclusive(date: String): String {
        val zone = ZoneId.systemDefault()
        return LocalDate.parse(date)
            .atStartOfDay(zone)
            .toInstant()
            .toString() // 2025-05-25T17:00:00Z (untuk WIB)
    }

    private fun toUtcEndExclusive(date: String): String {
        val zone = ZoneId.systemDefault()
        return LocalDate.parse(date)
            .plusDays(1)                 // awal hari berikutnya
            .atStartOfDay(zone)
            .toInstant()
            .toString()                  // eksklusif
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pagingData = combine(_query, _startDate, _endDate) { q, s, e ->
        Triple(q?.trim().orEmpty(), s, e)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (q, s, e) ->
            val startUtc = s?.let { toUtcStartInclusive(it) }
            val endUtc   = e?.let { toUtcEndExclusive(it) }

            Pager(
                config = PagingConfig(pageSize = 5, initialLoadSize = 5, enablePlaceholders = false)
            ) {
                TransaksiPagingSource(
                    client = SupabaseProvider.client,
                    query  = q.ifBlank { null },
                    startDate = startUtc,
                    endDate   = endUtc
                )
            }.flow
        }
        .cachedIn(viewModelScope)

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