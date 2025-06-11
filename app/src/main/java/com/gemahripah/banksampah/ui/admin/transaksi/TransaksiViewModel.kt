package com.gemahripah.banksampah.ui.admin.transaksi

import androidx.lifecycle.*
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TransaksiViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _riwayatList = MutableLiveData<List<RiwayatTransaksi>>()
    val riwayatList: LiveData<List<RiwayatTransaksi>> = _riwayatList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _filteredRiwayat = MutableLiveData<List<RiwayatTransaksi>>()
    val filteredRiwayat: LiveData<List<RiwayatTransaksi>> = _filteredRiwayat

    private var currentQuery: String = ""
    private var currentStartDate: String? = null
    private var currentEndDate: String? = null

    fun fetchRiwayat() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transaksiList = client.postgrest.from("transaksi")
                    .select {
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
                        client.postgrest.rpc("hitung_total_jumlah", buildJsonObject {
                            put("tsk_id_input", transaksi.tskId)
                        }).data.toDoubleOrNull()
                    } else null

                    val totalHarga = if (transaksi.tskTipe == "Keluar") {
                        val detailList = client.postgrest.from("detail_transaksi")
                            .select {
                                filter { transaksi.tskId?.let { eq("dtlTskId", it) } }
                            }.decodeList<DetailTransaksi>()
                        detailList.sumOf { it.dtlJumlah ?: 0.0 }
                    } else {
                        client.postgrest.rpc("hitung_total_harga", buildJsonObject {
                            put("tsk_id_input", transaksi.tskId)
                        }).data.toDoubleOrNull()
                    }

                    val tanggalFormatted = try {
                        OffsetDateTime.parse(transaksi.created_at).format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
                        )
                    } catch (_: Exception) {
                        "Tanggal tidak valid"
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
                        hari = null,
                        createdAt = transaksi.created_at ?: ""
                    )
                }

                withContext(Dispatchers.Main) {
                    _riwayatList.value = hasil
                    applyFilters()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        applyFilters()
    }

    fun setStartDate(date: String?) {
        currentStartDate = date
        applyFilters()
    }

    fun setEndDate(date: String?) {
        currentEndDate = date
        applyFilters()
    }

    private fun applyFilters() {
        val all = _riwayatList.value ?: return

        val filtered = all.filter { riwayat ->
            val matchNama = riwayat.nama.lowercase(Locale.getDefault())
                .contains(currentQuery.lowercase(Locale.getDefault()))

            val itemDate = try {
                OffsetDateTime.parse(riwayat.createdAt)
            } catch (_: Exception) {
                null
            }

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

            val matchStart = currentStartDate?.let {
                val start = OffsetDateTime.parse("${it}T00:00:00+07:00", formatter)
                itemDate?.isAfter(start.minusNanos(1)) ?: true
            } ?: true

            val matchEnd = currentEndDate?.let {
                val end = OffsetDateTime.parse("${it}T23:59:59+07:00", formatter)
                itemDate?.isBefore(end.plusNanos(1)) ?: true
            } ?: true

            matchNama && matchStart && matchEnd
        }

        _filteredRiwayat.value = filtered
    }
}
