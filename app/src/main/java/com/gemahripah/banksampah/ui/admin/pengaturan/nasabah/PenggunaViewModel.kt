package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.ui.gabungan.paging.listNasabah.NasabahPagingSource
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class PenggunaViewModel : ViewModel() {

    private val _totalNasabah = MutableStateFlow(0)
    val totalNasabah: StateFlow<Int> = _totalNasabah

    private val _searchQuery = MutableStateFlow("")
    private val searchQuery: StateFlow<String> = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pager = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            Pager(
                PagingConfig(
                pageSize = 5,
                initialLoadSize = 5,
                enablePlaceholders = false
            )
            ) {
                NasabahPagingSource(query)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun ambilData() {
        ambilTotalNasabah()
    }

    private fun ambilTotalNasabah() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_nasabah")
                _totalNasabah.value = result.data.toIntOrNull() ?: 0
            } catch (e: Exception) {
                _totalNasabah.value = 0
            }
        }
    }
}