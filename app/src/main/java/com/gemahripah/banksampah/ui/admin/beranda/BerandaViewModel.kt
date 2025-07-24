package com.gemahripah.banksampah.ui.admin.beranda

import android.util.Log
import androidx.lifecycle.*
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.text.NumberFormat
import java.util.*

class BerandaViewModel : ViewModel() {

    private val _isLoadingNasabah = MutableLiveData<Boolean>()
    val isLoadingNasabah: LiveData<Boolean> = _isLoadingNasabah

    private val _nasabahList = MutableLiveData<List<Pengguna>>()
    val nasabahList: LiveData<List<Pengguna>> = _nasabahList

    private val _totalSaldo = MutableLiveData<String>()
    val totalSaldo: LiveData<String> = _totalSaldo

    private val _totalNasabah = MutableLiveData<String>()
    val totalNasabah: LiveData<String> = _totalNasabah

    private val _totalTransaksi = MutableLiveData<String>()
    val totalTransaksi: LiveData<String> = _totalTransaksi

    private val _totalSetoran = MutableLiveData<String>()
    val totalSetoran: LiveData<String> = _totalSetoran

    private val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    fun fetchDashboardData() {
        getPengguna()
        getTotalSaldo()
        getTotalNasabah()
        getTotalSetoran()
    }

    fun getPengguna() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingNasabah.postValue(true)
            try {
                val list = SupabaseProvider.client
                    .from("pengguna")
                    .select {
                        filter { eq("pgnIsAdmin", "False") }
                    }
                    .decodeList<Pengguna>()
                _nasabahList.postValue(list)
            } catch (e: Exception) {
                _nasabahList.postValue(emptyList())
            } finally {
                _isLoadingNasabah.postValue(false)
            }
        }
    }

    fun getTotalSaldo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_saldo_pengguna")
                val saldo = result.data.toDoubleOrNull() ?: 0.0
                _totalSaldo.postValue(formatter.format(saldo))
            } catch (e: Exception) {
                _totalSaldo.postValue("Gagal memuat")
            }
        }
    }

    fun getTotalNasabah() {
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
                val params = buildJsonObject {
                    if (filter == "Transaksi Keluar") {
                        put("tipe_transaksi", "Keluar")
                    }
                    if (startDate != null) put("start_date", startDate.toString()) else put("start_date", JsonNull)
                    if (endDate != null) put("end_date", endDate.toString()) else put("end_date", JsonNull)
                }

                val total = when (filter) {
                    "Transaksi Masuk" -> {
                        val result = SupabaseProvider.client.postgrest.rpc(
                            "hitung_total_transaksi_masuk", params
                        )
                        result.data.toDoubleOrNull() ?: 0.0
                    }

                    "Transaksi Keluar" -> {
                        val result = SupabaseProvider.client.postgrest.rpc(
                            "hitung_total_jumlah_berdasarkan_tipe", params
                        )
                        result.data.toDoubleOrNull() ?: 0.0
                    }

                    else -> 0.0
                }

                _totalTransaksi.postValue(formatter.format(total))
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal memuat total transaksi", e)
                _totalTransaksi.postValue("Gagal memuat")
            }
        }
    }

    fun getTotalSetoran(start: LocalDate? = null, end: LocalDate? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val params = mutableMapOf(
                    "tipe_transaksi" to "Masuk"
                )

                start?.let { params["start_date"] = it.toString() }
                end?.let { params["end_date"] = it.toString() }
                val result = SupabaseProvider.client.postgrest.rpc(
                    "hitung_total_jumlah_berdasarkan_tipe",
                    params
                )

                val total = result.data.toDoubleOrNull() ?: 0.0
                _totalSetoran.postValue("$total Kg")
            } catch (e: Exception) {
                Log.e("Supabase", "Gagal memuat total setoran", e)
                _totalSetoran.postValue("Gagal memuat")
            }
        }
    }
}