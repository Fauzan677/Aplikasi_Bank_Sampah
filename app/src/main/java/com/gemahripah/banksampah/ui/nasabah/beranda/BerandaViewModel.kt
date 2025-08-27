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
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.ui.gabungan.paging.riwayatTransaksi.RiwayatTransaksiPagingSource
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.json.JsonPrimitive

class BerandaViewModel : ViewModel() {
    private val client = SupabaseProvider.client

    private val _totalTransaksi = MutableLiveData<String>()
    val totalTransaksi: LiveData<String> = _totalTransaksi

    private val _setoran = MutableLiveData<String>()
    val setoran: LiveData<String> = _setoran

    private val NF_2 = NumberFormat.getNumberInstance(Locale("id","ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    private val NF_RP2 = NumberFormat.getNumberInstance(Locale("id","ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    private fun Any?.toBigDecimalOrNull(): BigDecimal? = when (this) {
        null -> null
        is BigDecimal -> this
        is Number -> BigDecimal(this.toString())
        is JsonPrimitive -> if (isString) BigDecimal(content) else BigDecimal(toString())
        is String -> this.takeIf { it.isNotBlank() }?.let { BigDecimal(it) }
        else -> runCatching { BigDecimal(toString()) }.getOrNull()
    }

    fun getTotalTransaksi(
        pgnId: String,
        filter: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        val tipe = if (filter == "Transaksi Keluar") "Keluar" else "Masuk"

        val params = buildJsonObject {
            put("pgn_id_input", pgnId)
            put("tipe_transaksi", tipe)                      // <— WAJIB, untuk Masuk & Keluar
            if (startDate != null) put("start_date", startDate.toString()) else put("start_date", JsonNull)
            if (endDate   != null) put("end_date",   endDate.toString())   else put("end_date",   JsonNull)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bd = client.postgrest
                    .rpc("hitung_total_transaksi_per_pengguna", params)
                    .data
                    .toBigDecimalOrNull()
                    ?: BigDecimal.ZERO

                // Rupiah biasanya tanpa desimal
                val nilai = bd.setScale(2, RoundingMode.HALF_UP)
                _totalTransaksi.postValue("Rp ${NF_RP2.format(nilai)}")
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal menghitung total transaksi", e)
                _totalTransaksi.postValue("Rp 0")
            }
        }
    }

    fun getTotalSetoran(pgnId: String, start: LocalDate? = null, end: LocalDate? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val params = buildJsonObject {
                    put("pgn_id_input", pgnId)
                    if (start != null) put("start_date", start.toString()) else put("start_date", JsonNull)
                    if (end != null) put("end_date", end.toString()) else put("end_date", JsonNull)
                }

                val bd = client.postgrest
                    .rpc("hitung_total_jumlah_per_pengguna", params)
                    .data
                    .toBigDecimalOrNull()
                    ?.setScale(2, RoundingMode.HALF_UP)
                    ?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

                _setoran.postValue("${NF_2.format(bd)} Kg")
            } catch (e: Exception) {
                _setoran.postValue("0.00 Kg")
            }
        }
    }

    // BerandaViewModel
    fun pagingData(pgnId: String, startDate: String?, endDate: String?): Flow<PagingData<RiwayatTransaksi>> {
        val startSql = startDate // sudah "yyyy-MM-dd" dari UI
        val endSqlExclusive = endDate?.let { LocalDate.parse(it).plusDays(1).toString() } // eksklusif

        return Pager(
            config = PagingConfig(pageSize = 5, initialLoadSize = 5, enablePlaceholders = false),
            pagingSourceFactory = {
                RiwayatTransaksiPagingSource(
                    pgnId = pgnId,
                    startDate = startSql,
                    endDate   = endSqlExclusive
                )
            }
        ).flow.cachedIn(viewModelScope)
    }
}