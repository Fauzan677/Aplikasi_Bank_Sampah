package com.gemahripah.banksampah.ui.admin.transaksi.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DetailTransaksiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DetailTransaksiUiState>(DetailTransaksiUiState.Loading)
    val uiState: StateFlow<DetailTransaksiUiState> = _uiState.asStateFlow()

    fun getDetailTransaksi(idTransaksi: Long) {
        _uiState.value = DetailTransaksiUiState.Loading
        viewModelScope.launch {
            try {
                val state = withContext(Dispatchers.IO) {
                    // query + parse di IO
                    val columns = Columns.raw("""
                    dtlId, dtlJumlah, dtlSphId (sphJenis)
                """.trimIndent())

                    val response = SupabaseProvider.client
                        .from("detail_transaksi")
                        .select(columns) { filter { eq("dtlTskId", idTransaksi) } }

                    val detailList = Json.decodeFromString<List<DetailTransaksiRelasi>>(response.data)

                    // hitung hargaDetail paralel di IO
                    val enriched = supervisorScope {
                        detailList.mapNotNull { detail ->
                            detail.dtlId?.let { dtlId ->
                                async {
                                    try {
                                        val r = SupabaseProvider.client.postgrest.rpc(
                                            "hitung_harga_detail",
                                            buildJsonObject { put("dtl_id_input", dtlId) }
                                        )
                                        val harga = r.data.toDoubleOrNull() ?: 0.0
                                        detail.copy(hargaDetail = harga)
                                    } catch (_: Exception) { null }
                                }
                            }
                        }.map { it.await() }.filterNotNull()
                    }

                    DetailTransaksiUiState.Success(enriched)
                }
                _uiState.value = state
            } catch (e: Exception) {
                _uiState.value = DetailTransaksiUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun deleteTransaksi(tskId: Long) {
        viewModelScope.launch {
            _uiState.value = DetailTransaksiUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client.from("detail_transaksi").delete { filter { eq("dtlTskId", tskId) } }
                    SupabaseProvider.client.from("transaksi").delete { filter { eq("tskId", tskId) } }
                }
                _uiState.value = DetailTransaksiUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = DetailTransaksiUiState.Error(e.message ?: "Gagal menghapus transaksi")
            }
        }
    }
}