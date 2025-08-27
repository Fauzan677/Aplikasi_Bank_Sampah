package com.gemahripah.banksampah.ui.admin.transaksi.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.TransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class DetailTransaksiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DetailTransaksiUiState>(DetailTransaksiUiState.Loading)
    val uiState: StateFlow<DetailTransaksiUiState> = _uiState.asStateFlow()

    fun getDetailTransaksi(idTransaksi: Long) {
        _uiState.value = DetailTransaksiUiState.Loading
        viewModelScope.launch {
            try {
                val state = withContext(Dispatchers.IO) {
                    val columns = Columns.raw(
                        """
                        dtlId,
                        dtlJumlah,
                        dtlNominal,
                        dtlTskId ( tskKeterangan ),
                        dtlSphId ( sphJenis, sphSatuan )
                        """.trimIndent()
                    )

                    val response = SupabaseProvider.client
                        .from("detail_transaksi")
                        .select(columns) { filter { eq("dtlTskId", idTransaksi) } }

                    // PENTING: pakai decoder bawaan Supabase, bukan Json.decodeFromString
                    val detailList = response.decodeList<DetailTransaksiRelasi>()

                    DetailTransaksiUiState.Success(detailList)
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
                    // 1) Ambil tipe transaksi + data pengguna (pgnId & pgnSaldo)
                    val trx = SupabaseProvider.client
                        .from("transaksi")
                        .select(
                            Columns.raw(
                                "tskTipe, tskIdPengguna ( pgnId, pgnSaldo )"
                            )
                        ) { filter { eq("tskId", tskId) } }
                        .decodeSingle<TransaksiRelasi>()

                    val tipe       = trx.tskTipe?.trim().orEmpty()          // "Masuk" / "Keluar"
                    val userId     = trx.tskIdPengguna?.pgnId ?: error("Pengguna tidak ditemukan")
                    val saldoLama  = trx.tskIdPengguna.pgnSaldo ?: BigDecimal.ZERO

                    // 2) Hitung total nominal detail transaksi
                    val details = SupabaseProvider.client
                        .from("detail_transaksi")
                        .select(Columns.list("dtlNominal")) {
                            filter { eq("dtlTskId", tskId) }
                        }
                        .decodeList<DetailTransaksiRelasi>()

                    val totalNominal = details.fold(BigDecimal.ZERO) { acc, d ->
                        acc + (d.dtlNominal ?: BigDecimal.ZERO)
                    }

                    // 3) Tentukan perubahan saldo saat penghapusan
                    //    Masuk: revert kenaikan -> saldo - total
                    //    Keluar: revert penurunan -> saldo + total
                    val delta = when {
                        tipe.equals("masuk", ignoreCase = true)  -> totalNominal.negate()
                        tipe.equals("keluar", ignoreCase = true) -> totalNominal
                        else -> BigDecimal.ZERO
                    }
                    val saldoBaru = saldoLama + delta

                    // 4) Update saldo pengguna (kalau ada perubahan)
                    if (delta != BigDecimal.ZERO) {
                        SupabaseProvider.client
                            .from("pengguna")
                            .update(mapOf("pgnSaldo" to saldoBaru)) {
                                filter { eq("pgnId", userId) }
                            }
                    }

                    // 5) Hapus detail lalu header transaksi
                    SupabaseProvider.client
                        .from("detail_transaksi")
                        .delete { filter { eq("dtlTskId", tskId) } }

                    SupabaseProvider.client
                        .from("transaksi")
                        .delete { filter { eq("tskId", tskId) } }
                }

                _uiState.value = DetailTransaksiUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = DetailTransaksiUiState.Error(e.message ?: "Gagal menghapus transaksi")
            }
        }
    }
}