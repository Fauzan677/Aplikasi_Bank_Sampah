package com.gemahripah.banksampah.ui.nasabah.beranda

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.ui.gabungan.paging.riwayatTransaksi.RiwayatTransaksiPagingSource
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BerandaViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _totalTransaksi = MutableLiveData<String>()
    val totalTransaksi: LiveData<String> = _totalTransaksi

    private val _saldo = MutableLiveData<String>()
    val saldo: LiveData<String> = _saldo

    private val _setoran = MutableLiveData<String>()
    val setoran: LiveData<String> = _setoran

    fun getTotalTransaksi(
        pgnId: String,
        filter: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        val params = buildJsonObject {
            put("pgn_id_input", pgnId)
            if (startDate != null) put("start_date", startDate.toString()) else put("start_date", JsonNull)
            if (endDate != null) put("end_date", endDate.toString()) else put("end_date", JsonNull)
            if (filter == "Transaksi Keluar") {
                put("tipe_transaksi", "Keluar")
            }
        }

        viewModelScope.launch {
            try {
                val total = when (filter) {
                    "Transaksi Masuk" -> {
                        val response = client.postgrest.rpc("hitung_total_transaksi_masuk_per_pengguna", params)
                        response.data.toDoubleOrNull() ?: 0.0
                    }
                    "Transaksi Keluar" -> {
                        val response = client.postgrest.rpc("hitung_total_jumlah_per_pengguna", params)
                        response.data.toDoubleOrNull() ?: 0.0
                    }
                    else -> 0.0
                }

                val formatted = NumberFormat.getNumberInstance(Locale("in", "ID")).format(total)
                _totalTransaksi.postValue("Rp $formatted")
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal menghitung total transaksi", e)
                _totalTransaksi.postValue("Rp 0")
            }
        }
    }

    fun getSaldo(pgnId: String) {
        val params = buildJsonObject { put("pgn_id_input", pgnId) }

        viewModelScope.launch {
            try {
                val response = client.postgrest.rpc("hitung_saldo_pengguna", params)
                val formatted = NumberFormat.getNumberInstance(Locale("in", "ID"))
                    .format(response.data.toDoubleOrNull() ?: 0.0)
                _saldo.postValue("Rp $formatted")
            } catch (e: Exception) {
                _saldo.postValue("Rp 0")
            }
        }
    }

    fun getTotalSetoran(
        pgnId: String,
        start: LocalDate? = null,
        end: LocalDate? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val params = buildJsonObject {
                    put("pgn_id_input", pgnId)
                    put("tipe_transaksi", "Masuk")
                    if (start != null) put("start_date", start.toString()) else put("start_date", JsonNull)
                    if (end != null) put("end_date", end.toString()) else put("end_date", JsonNull)
                }

                val result = client.postgrest.rpc(
                    "hitung_total_jumlah_per_pengguna",
                    params
                )

                val total = result.data.toDoubleOrNull() ?: 0.0

                _setoran.postValue("$total Kg")
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal memuat total setoran", e)
                _setoran.postValue("0 Kg")
            }
        }
    }

    fun pagingData(
        pgnId: String,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<PagingData<RiwayatTransaksi>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5,
                initialLoadSize = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                RiwayatTransaksiPagingSource(
                    pgnId = pgnId,
                    startDate = startDate?.let { it + "T00:00:00+00" },
                    endDate = endDate?.let { it + "T23:59:59+00" }
                )
            }
        ).flow.cachedIn(viewModelScope)
    }
}