package com.gemahripah.banksampah.ui.nasabah.beranda

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
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

    private val _loadingRiwayat = MutableLiveData<Boolean>()
    val loadingRiwayat: LiveData<Boolean> = _loadingRiwayat

    private val _totalTransaksi = MutableLiveData<String>()
    val totalTransaksi: LiveData<String> = _totalTransaksi

    private val _saldo = MutableLiveData<String>()
    val saldo: LiveData<String> = _saldo

    private val _setoran = MutableLiveData<String>()
    val setoran: LiveData<String> = _setoran

    private val _riwayat = MutableLiveData<List<RiwayatTransaksi>>()
    val riwayat: LiveData<List<RiwayatTransaksi>> = _riwayat

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

    fun fetchRiwayat(pgnId: String) {
        viewModelScope.launch {
            _loadingRiwayat.postValue(true)
            try {
                val transaksiList = client.postgrest.from("transaksi")
                    .select {
                        filter { eq("tskIdPengguna", pgnId) }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<Transaksi>()

                val hasil = transaksiList.map { transaksi ->
                    val pengguna = client.postgrest.from("pengguna")
                        .select {
                            filter { eq("pgnId", transaksi.tskIdPengguna!!) }
                        }
                        .decodeSingle<Pengguna>()

                    val totalBerat = if (transaksi.tskTipe == "Masuk") {
                        val res = client.postgrest.rpc("hitung_total_jumlah", buildJsonObject {
                            put("tsk_id_input", transaksi.tskId)
                        })
                        res.data.toDoubleOrNull()
                    } else null

                    val totalHarga = if (transaksi.tskTipe == "Keluar") {
                        val detail = client.postgrest.from("detail_transaksi")
                            .select {
                                filter { transaksi.tskId?.let { eq("dtlTskId", it) } }
                            }
                            .decodeList<DetailTransaksi>()
                        detail.sumOf { it.dtlJumlah ?: 0.0 }
                    } else {
                        client.postgrest.rpc("hitung_total_harga", buildJsonObject {
                            put("tsk_id_input", transaksi.tskId)
                        }).data.toDoubleOrNull()
                    }

                    val dateTime = OffsetDateTime.parse(transaksi.created_at)
                    val tanggal = dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
                    val hari = dateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale("id")))

                    RiwayatTransaksi(
                        tskId = transaksi.tskId!!,
                        tskIdPengguna = transaksi.tskIdPengguna,
                        nama = pengguna.pgnNama ?: "Tidak Diketahui",
                        tanggal = tanggal,
                        tipe = transaksi.tskTipe ?: "Masuk",
                        tskKeterangan = transaksi.tskKeterangan,
                        totalBerat = totalBerat,
                        totalHarga = totalHarga,
                        hari = hari,
                        createdAt = transaksi.created_at ?: ""
                    )
                }

                _riwayat.postValue(hasil)
            } catch (e: Exception) {
                Log.e("BerandaViewModel", "fetchRiwayat: gagal", e)
                _riwayat.postValue(emptyList())
            } finally {
                _loadingRiwayat.postValue(false)
            }
        }
    }
}