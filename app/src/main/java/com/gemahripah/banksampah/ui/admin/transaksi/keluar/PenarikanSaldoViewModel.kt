package com.gemahripah.banksampah.ui.admin.transaksi.keluar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class PenarikanSaldoViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Master pengguna
    private val _penggunaList = MutableStateFlow<List<Pengguna>>(emptyList())
    val penggunaList: StateFlow<List<Pengguna>> = _penggunaList.asStateFlow()

    // Selected user
    private val _selectedPgnId = MutableStateFlow<String?>(null)
    val selectedPgnId: StateFlow<String?> = _selectedPgnId.asStateFlow()

    // Saldo (teks sudah terformat)
    private val _saldoText = MutableStateFlow("Rp 0")
    val saldoText: StateFlow<String> = _saldoText.asStateFlow()

    // Event UI
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    // Event: saldo kurang -> kirim sisa saldo (Double)
    private val _saldoKurang = MutableSharedFlow<Double>(replay = 0, extraBufferCapacity = 1)
    val saldoKurang: SharedFlow<Double> = _saldoKurang

    // Event: navigasi selesai
    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private val rupiah: NumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    /** Ambil daftar pengguna (non-admin). Selalu refresh agar up-to-date. */
    fun loadPengguna() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = client
                    .from("pengguna")
                    .select {
                        filter { eq("pgnIsAdmin", false) }
                    }
                    .decodeList<Pengguna>()
                _penggunaList.value = list
            } catch (e: Exception) {
                _toast.tryEmit("Gagal memuat daftar nasabah")
            }
        }
    }

    fun preselectPengguna(p: Pengguna) {
        val id = p.pgnId ?: return
        _selectedPgnId.value = id
        fetchSaldo(id)
    }

    /** Dipanggil saat user dipilih dari AutoComplete (berdasarkan NAMA). */
    fun onNamaDipilih(nama: String) {
        val id = _penggunaList.value.firstOrNull { it.pgnNama == nama }?.pgnId
        _selectedPgnId.value = id
        if (id != null) fetchSaldo(id)
    }

    /** Ambil saldo & tampilkan. */
    fun fetchSaldo(pgnId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saldo = client.postgrest.rpc(
                    "hitung_saldo_pengguna",
                    buildJsonObject { put("pgn_id_input", pgnId) }
                ).data.toDoubleOrNull() ?: 0.0

                _saldoText.value = "Rp ${rupiah.format(saldo)}"
            } catch (_: Exception) {
                _saldoText.value = "Rp 0"
            }
        }
    }

    /** Proses penarikan: validasi ringan + cek saldo via RPC + insert transaksi & detail. */
    fun submitPenarikan(jumlah: Double?, keterangan: String) {
        val pgnId = _selectedPgnId.value
        if (pgnId.isNullOrBlank()) {
            _toast.tryEmit("Silakan pilih nasabah terlebih dahulu")
            return
        }

        if (jumlah == null || jumlah <= 0.0) {
            _toast.tryEmit("Jumlah penarikan tidak valid")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Cek saldo
                val saldo = client.postgrest.rpc(
                    "hitung_saldo_pengguna",
                    buildJsonObject { put("pgn_id_input", pgnId) }
                ).data.toString().toDoubleOrNull() ?: 0.0

                if (saldo < jumlah) {
                    _saldoKurang.emit(saldo) // biar fragment bisa set error di EditText
                    _isLoading.value = false
                    return@launch
                }

                // Insert transaksi 'Keluar'
                val transaksi = client.postgrest["transaksi"]
                    .insert(buildJsonObject {
                        put("tskIdPengguna", pgnId)
                        put("tskKeterangan", keterangan)
                        put("tskTipe", "Keluar")
                    }) { select() }
                    .decodeSingle<Transaksi>()

                val transaksiId = transaksi.tskId
                if (transaksiId == null) {
                    _toast.emit("Gagal membuat transaksi")
                    _isLoading.value = false
                    return@launch
                }

                // Insert detail (hanya jumlah)
                client.postgrest["detail_transaksi"]
                    .insert(buildJsonObject {
                        put("dtlTskId", transaksiId)
                        put("dtlJumlah", jumlah)
                    })

                _toast.emit("Penarikan saldo berhasil")
                _navigateBack.emit(Unit)
            } catch (_: Exception) {
                _toast.emit("Penarikan saldo gagal, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }
}