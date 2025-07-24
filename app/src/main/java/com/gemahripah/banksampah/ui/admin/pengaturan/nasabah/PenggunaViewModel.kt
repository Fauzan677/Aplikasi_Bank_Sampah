package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PenggunaViewModel : ViewModel() {

    private val _nasabahList = MutableStateFlow<List<Pengguna>>(emptyList())
    val nasabahList: StateFlow<List<Pengguna>> = _nasabahList

    private var semuaNasabah: List<Pengguna> = emptyList()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _totalNasabah = MutableStateFlow(0)
    val totalNasabah: StateFlow<Int> = _totalNasabah

    fun ambilData() {
        ambilPengguna()
        ambilTotalNasabah()
    }

    private fun ambilPengguna() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val penggunaList = SupabaseProvider.client
                    .from("pengguna")
                    .select {
                        filter {
                            eq("pgnIsAdmin", "False")
                        }
                    }
                    .decodeList<Pengguna>()

                semuaNasabah = penggunaList
                _nasabahList.value = penggunaList
            } catch (_: Exception) {

            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun ambilTotalNasabah() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_nasabah")
                _totalNasabah.value = result.data.toIntOrNull() ?: 0
            } catch (e: Exception) {
                _totalNasabah.value = 0
            }
        }
    }

    fun cariPengguna(keyword: String) {
        val result = if (keyword.isBlank()) {
            semuaNasabah
        } else {
            semuaNasabah.filter {
                it.pgnNama?.contains(keyword, ignoreCase = true) == true
            }
        }
        _nasabahList.value = result
    }
}