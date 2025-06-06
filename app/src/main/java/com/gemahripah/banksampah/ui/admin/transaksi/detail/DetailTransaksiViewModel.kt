package com.gemahripah.banksampah.ui.admin.transaksi.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
                val columns = Columns.raw(
                    """
                    dtlId,
                    dtlJumlah,
                    dtlSphId (
                        sphJenis
                    )
                    """.trimIndent()
                )

                val response = SupabaseProvider.client
                    .from("detail_transaksi")
                    .select(columns) {
                        filter {
                            eq("dtlTskId", idTransaksi)
                        }
                    }

                val detailList = try {
                    Json.decodeFromString<List<DetailTransaksiRelasi>>(response.data)
                } catch (e: Exception) {
                    Log.e("DetailVM", "Gagal parse JSON: ${e.message}", e)
                    _uiState.value = DetailTransaksiUiState.Error("Gagal memuat data detail")
                    return@launch
                }

                val enrichedList = detailList.mapNotNull { detail ->
                    detail.dtlId?.let { dtlId ->
                        try {
                            val hargaDetailResponse = SupabaseProvider.client.postgrest.rpc(
                                "hitung_harga_detail",
                                buildJsonObject {
                                    put("dtl_id_input", dtlId)
                                }
                            )
                            val hargaDetail = hargaDetailResponse.data.toDoubleOrNull()
                            detail.copy(hargaDetail = hargaDetail ?: 0.0)
                        } catch (e: Exception) {
                            Log.e("DetailVM", "Gagal hitung harga: ${e.message}", e)
                            null
                        }
                    }
                }

                _uiState.value = DetailTransaksiUiState.Success(enrichedList)

            } catch (e: Exception) {
                Log.e("DetailVM", "Gagal ambil data: ${e.message}", e)
                _uiState.value = DetailTransaksiUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun deleteTransaksi(tskId: Long) {
        viewModelScope.launch {
            try {
                SupabaseProvider.client
                    .from("detail_transaksi")
                    .delete {
                        filter { eq("dtlTskId", tskId) }
                    }

                SupabaseProvider.client
                    .from("transaksi")
                    .delete {
                        filter { eq("tskId", tskId) }
                    }

                _uiState.value = DetailTransaksiUiState.Deleted

            } catch (e: Exception) {
                Log.e("DetailVM", "Gagal hapus: ${e.message}", e)
                _uiState.value = DetailTransaksiUiState.Error(e.message ?: "Gagal menghapus transaksi")
            }
        }
    }
}
