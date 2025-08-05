package com.gemahripah.banksampah.ui.admin.beranda.detail

import androidx.lifecycle.*
import com.gemahripah.banksampah.data.model.beranda.TotalSampahPerJenis
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private val _riwayatTransaksi = MutableStateFlow<List<RiwayatTransaksi>>(emptyList())
    val riwayatTransaksi: StateFlow<List<RiwayatTransaksi>> = _riwayatTransaksi.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadData(pgnId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val jobs = listOf(
                    async { getSaldo(pgnId) },
                    async { getTotalSampah(pgnId) },
                    async { fetchRiwayatTransaksi(pgnId) }
                )
                jobs.awaitAll()
            } finally {
                _isLoading.value = false
            }
        }
    }


    private suspend fun getSaldo(pgnId: String) {
        try {
            val result = client.postgrest.rpc(
                "hitung_saldo_pengguna",
                buildJsonObject { put("pgn_id_input", pgnId) }
            )
            val saldoDouble = result.data.toDoubleOrNull() ?: 0.0
            val formatted = NumberFormat.getNumberInstance(Locale("in", "ID")).format(saldoDouble)
            _saldo.value = formatted
        } catch (e: Exception) {
            _saldo.value = "Rp 0"
        }
    }

    private suspend fun getTotalSampah(pgnId: String) {
        try {
            val result = client.postgrest.rpc(
                "get_riwayat_setoran_pengguna_berdasarkan_jenis",
                buildJsonObject { put("pgn_id_input", pgnId) }
            )
            val list = result.decodeList<TotalSampahPerJenis>()
            _totalSampah.value = list
        } catch (e: Exception) {
            _totalSampah.value = emptyList()
        }
    }

    private suspend fun fetchRiwayatTransaksi(pgnId: String) {
        try {
            val transaksiList = client.postgrest.from("transaksi")
                .select {
                    order("created_at", Order.DESCENDING)
                    filter { eq("tskIdPengguna", pgnId) }
                }
                .decodeList<Transaksi>()

            val hasil = transaksiList.map { transaksi ->
                val pengguna = client.postgrest.from("pengguna")
                    .select { filter { eq("pgnId", transaksi.tskIdPengguna!!) } }
                    .decodeSingle<Pengguna>()

                val totalBerat = if (transaksi.tskTipe == "Masuk") {
                    client.postgrest.rpc(
                        "hitung_total_jumlah",
                        buildJsonObject { put("tsk_id_input", transaksi.tskId!!) }
                    ).data.toDoubleOrNull()
                } else null

                val totalHarga = if (transaksi.tskTipe == "Keluar") {
                    client.postgrest.from("detail_transaksi")
                        .select { filter { eq("dtlTskId", transaksi.tskId!!) } }
                        .decodeList<DetailTransaksi>()
                        .sumOf { it.dtlJumlah ?: 0.0 }
                } else {
                    client.postgrest.rpc(
                        "hitung_total_harga",
                        buildJsonObject { put("tsk_id_input", transaksi.tskId!!) }
                    ).data.toDoubleOrNull()
                }

                val (tanggalFormatted, hari) = try {
                    val dateTime = OffsetDateTime.parse(transaksi.created_at)
                    val tanggalFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
                    val hariFormat = DateTimeFormatter.ofPattern("EEEE", Locale("id"))
                    dateTime.format(tanggalFormat) to dateTime.format(hariFormat)
                } catch (e: Exception) {
                    "Tanggal tidak valid" to "Hari tidak valid"
                }

                RiwayatTransaksi(
                    tskId = transaksi.tskId!!,
                    tskIdPengguna = transaksi.tskIdPengguna,
                    nama = pengguna.pgnNama ?: "Tidak Diketahui",
                    tanggal = tanggalFormatted,
                    tipe = transaksi.tskTipe ?: "Masuk",
                    tskKeterangan = transaksi.tskKeterangan,
                    totalBerat = totalBerat,
                    totalHarga = totalHarga,
                    hari = hari,
                    createdAt = transaksi.created_at ?: ""
                )
            }

            _riwayatTransaksi.value = hasil
        } catch (e: Exception) {
            _riwayatTransaksi.value = emptyList()
        }
    }
}