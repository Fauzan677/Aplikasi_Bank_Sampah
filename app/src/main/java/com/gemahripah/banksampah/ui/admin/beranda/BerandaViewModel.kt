package com.gemahripah.banksampah.ui.admin.beranda

import android.util.Log
import androidx.lifecycle.*
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.text.NumberFormat
import java.util.*

class BerandaViewModel : ViewModel() {

    private val _totalSaldo = MutableLiveData<String>()
    val totalSaldo: LiveData<String> = _totalSaldo

    private val _totalNasabah = MutableLiveData<String>()
    val totalNasabah: LiveData<String> = _totalNasabah

    private val _totalTransaksi = MutableLiveData<String>()
    val totalTransaksi: LiveData<String> = _totalTransaksi

    private val _totalSetoran = MutableLiveData<String>()
    val totalSetoran: LiveData<String> = _totalSetoran

    private val formatter2 = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

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
            Pager(PagingConfig(
                pageSize = 5,
                initialLoadSize = 5,
                enablePlaceholders = false
            )) {
                NasabahPagingSource(query)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun fetchDashboardData() {
        getTotalSaldo()
        getTotalNasabah()
        getTotalSetoran()
    }

    private fun getTotalSaldo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_saldo_pengguna")
                val saldo = result.data.toDoubleOrNull() ?: 0.0
                _totalSaldo.postValue(formatter2.format(saldo))
            } catch (e: Exception) {
                _totalSaldo.postValue("Gagal memuat")
            }
        }
    }

    private fun getTotalNasabah() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_nasabah")
                val jumlah = result.data.toIntOrNull() ?: 0
                _totalNasabah.postValue(jumlah.toString())
            } catch (e: Exception) {
                _totalNasabah.postValue("Gagal memuat")
            }
        }
    }

    fun getTotalTransaksi(
        filter: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Map pilihan spinner -> argumen fungsi
                val tipe = when (filter) {
                    "Transaksi Masuk"  -> "Masuk"
                    "Transaksi Keluar" -> "Keluar"
                    else               -> null   // NULL = semua tipe
                }

                val params = buildJsonObject {
                    if (tipe != null) put("tipe_transaksi", tipe) else put("tipe_transaksi", JsonNull)
                    if (startDate != null) put("start_date", startDate.toString()) else put("start_date", JsonNull)
                    if (endDate != null) put("end_date", endDate.toString()) else put("end_date", JsonNull)
                }

                // Panggil fungsi terpadu
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_transaksi", params)

                // Fungsi DB sudah return numeric(20,2), parse ke Double untuk diformat
                val total = result.data.toDoubleOrNull() ?: 0.0
                _totalTransaksi.postValue(formatter2.format(total))
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal memuat total transaksi", e)
                _totalTransaksi.postValue("Gagal memuat")
            }
        }
    }

    fun getTotalSetoran(start: LocalDate? = null, end: LocalDate? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val params = buildJsonObject {
                    if (start != null) put("start_date", start.toString()) else put("start_date", JsonNull)
                    if (end   != null) put("end_date",   end.toString())   else put("end_date",   JsonNull)
                }

                val result = SupabaseProvider.client.postgrest
                    .rpc("hitung_total_jumlah_setoran_seluruh", params)

                val total = result.data.toDoubleOrNull() ?: 0.0
                _totalSetoran.postValue("${formatter2.format(total)} Kg")
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal memuat total setoran", e)
                _totalSetoran.postValue("Gagal memuat")
            }
        }
    }
}