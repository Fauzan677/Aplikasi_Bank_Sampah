package com.gemahripah.banksampah.ui.admin.beranda.detail

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gemahripah.banksampah.data.model.beranda.TotalSampahPerJenis
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.ui.gabungan.paging.riwayatTransaksi.RiwayatTransaksiPagingSource
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DetailPenggunaViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _saldo = MutableStateFlow("Rp 0")
    val saldo: StateFlow<String> = _saldo.asStateFlow()

    private val _totalSampah = MutableStateFlow<List<TotalSampahPerJenis>>(emptyList())
    val totalSampah: StateFlow<List<TotalSampahPerJenis>> = _totalSampah.asStateFlow()

    val pager = MutableStateFlow<Flow<PagingData<RiwayatTransaksi>>?>(null)
    private val pgnIdFlow = MutableStateFlow<String?>(null)

    fun loadPagingRiwayat(pgnId: String) {
        pgnIdFlow.value = pgnId
    }

    val pagingData: Flow<PagingData<RiwayatTransaksi>> =
        pgnIdFlow.filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { id ->
                Pager(
                    PagingConfig(pageSize = 5, initialLoadSize = 5, enablePlaceholders = false)
                ) { RiwayatTransaksiPagingSource(id) }
                    .flow
            }
            .cachedIn(viewModelScope)

    fun loadData(pgnId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                val saldoJob = async { getSaldo(pgnId) }
                val totalJob = async { getTotalSampah(pgnId) }
                // biar dua-duanya tetap jalan walau salah satu error (error ditangani di dalam fungsinya)
                saldoJob.await()
                totalJob.await()
            }
        }
    }

    private suspend fun getSaldo(pgnId: String) = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest.rpc("hitung_saldo_pengguna",
                buildJsonObject { put("pgn_id_input", pgnId) })
            val saldoDouble = result.data.toDoubleOrNull() ?: 0.0
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(saldoDouble)
            _saldo.value = formatted
        } catch (e: Exception) {
            _saldo.value = "Rp 0"
        }
    }

    private suspend fun getTotalSampah(pgnId: String) = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest.rpc("get_riwayat_setoran_pengguna_berdasarkan_jenis",
                buildJsonObject { put("pgn_id_input", pgnId) })
            _totalSampah.value = result.decodeList<TotalSampahPerJenis>()
        } catch (e: Exception) {
            _totalSampah.value = emptyList()
        }
    }
}