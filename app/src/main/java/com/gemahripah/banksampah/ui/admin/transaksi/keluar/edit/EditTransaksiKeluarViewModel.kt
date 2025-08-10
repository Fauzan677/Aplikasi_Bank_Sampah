package com.gemahripah.banksampah.ui.admin.transaksi.keluar.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
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

class EditTransaksiKeluarViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Saldo terformat untuk UI
    private val _saldoText = MutableStateFlow("Rp 0")
    val saldoText: StateFlow<String> = _saldoText.asStateFlow()

    // Event UI
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    // Saat saldo tidak cukup untuk jumlah baru -> kirim sisa saldo yang tersedia
    private val _saldoKurang = MutableSharedFlow<Double>(replay = 0, extraBufferCapacity = 1)
    val saldoKurang: SharedFlow<Double> = _saldoKurang

    // Sukses update -> perintahkan Fragment untuk navigate dan pop back stack
    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private var transaksiId: Long? = null
    private var detailId: Long? = null
    private var selectedPgnId: String? = null
    private var jumlahSebelumnya: Double = 0.0

    private val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))

    fun setArgs(riwayat: RiwayatTransaksi, enrichedList: Array<DetailTransaksiRelasi>) {
        transaksiId = riwayat.tskId
        selectedPgnId = riwayat.tskIdPengguna

        val first = enrichedList.firstOrNull()
        detailId = first?.dtlId
        jumlahSebelumnya = (first?.dtlJumlah ?: 0.0)
    }

    fun refreshSaldo() {
        selectedPgnId?.let { fetchSaldoDenganPenyesuaian(it) }
    }

    fun fetchSaldoDenganPenyesuaian(pgnId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saldo = client.postgrest.rpc(
                    "hitung_saldo_pengguna",
                    buildJsonObject { put("pgn_id_input", pgnId) }
                ).data.toDoubleOrNull() ?: 0.0

                val available = saldo + jumlahSebelumnya
                _saldoText.value = "Rp ${rupiah.format(available)}"
            } catch (_: Exception) {
                _saldoText.value = "Rp 0"
            }
        }
    }


    fun submitUpdate(jumlahBaru: Double?, keterangan: String) {
        val pgnId = selectedPgnId
        val tId = transaksiId
        val dId = detailId

        if (pgnId.isNullOrBlank()) {
            _toast.tryEmit("Silakan pilih nasabah terlebih dahulu")
            return
        }
        if (jumlahBaru == null || jumlahBaru <= 0.0) {
            _toast.tryEmit("Jumlah penarikan tidak valid")
            return
        }
        if (tId == null || dId == null) {
            _toast.tryEmit("Data transaksi tidak lengkap")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // cek saldo tersedia (saldo + jumlah sebelumnya)
                val saldo = client.postgrest.rpc(
                    "hitung_saldo_pengguna",
                    buildJsonObject { put("pgn_id_input", pgnId) }
                ).data.toString().toDoubleOrNull() ?: 0.0
                val available = saldo + jumlahSebelumnya

                if (available < jumlahBaru) {
                    _saldoKurang.emit(available)
                    _isLoading.value = false
                    return@launch
                }

                // update header
                client.from("transaksi").update({
                    set("tskIdPengguna", pgnId)
                    set("tskKeterangan", keterangan)
                }) {
                    filter { eq("tskId", tId) }
                }

                // update detail (jumlah)
                client.from("detail_transaksi").update({
                    set("dtlJumlah", jumlahBaru)
                }) {
                    filter { eq("dtlId", dId) }
                }

                _toast.emit("Transaksi berhasil diperbarui")
                _navigateBack.emit(Unit)

            } catch (_: Exception) {
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }
}