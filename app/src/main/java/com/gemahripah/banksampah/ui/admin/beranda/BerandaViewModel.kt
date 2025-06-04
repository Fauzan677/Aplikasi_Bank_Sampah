package com.gemahripah.banksampah.ui.admin.beranda

import androidx.lifecycle.*
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        getTotalTransaksiMasuk()
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

    fun getTotalTransaksiMasuk() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_transaksi_masuk")
                val total = result.data.toDoubleOrNull() ?: 0.0
                _totalTransaksi.postValue(formatter.format(total))
            } catch (e: Exception) {
                _totalTransaksi.postValue("Gagal memuat")
            }
        }
    }

    fun getTotalTransaksiKeluar() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc(
                    "hitung_total_jumlah_berdasarkan_tipe",
                    mapOf("tipe_transaksi" to "Keluar")
                )
                val total = result.data.toDoubleOrNull() ?: 0.0
                _totalTransaksi.postValue(formatter.format(total))
            } catch (e: Exception) {
                _totalTransaksi.postValue("Gagal memuat")
            }
        }
    }

    fun getTotalSetoran(filter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc(
                    "hitung_total_jumlah_berdasarkan_tipe",
                    mapOf("tipe_transaksi" to "Masuk", "filter_waktu" to filter)
                )
                val total = result.data.toDoubleOrNull() ?: 0.0
                _totalSetoran.postValue("$total Kg")
            } catch (e: Exception) {
                _totalSetoran.postValue("Gagal memuat")
            }
        }
    }
}
