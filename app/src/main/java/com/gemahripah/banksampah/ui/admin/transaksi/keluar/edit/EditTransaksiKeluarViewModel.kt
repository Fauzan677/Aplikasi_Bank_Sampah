package com.gemahripah.banksampah.ui.admin.transaksi.keluar.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class EditTransaksiKeluarViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saldoText = MutableStateFlow("Rp 0,00")
    val saldoText: StateFlow<String> = _saldoText.asStateFlow()

    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _saldoKurang = MutableSharedFlow<BigDecimal>(replay = 0, extraBufferCapacity = 1)
    val saldoKurang: SharedFlow<BigDecimal> = _saldoKurang

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private var transaksiId: Long? = null
    private var detailId: Long? = null
    private var selectedPgnId: String? = null

    // Nominal lama (dtlNominal) untuk penyesuaian saldo saat edit
    private var jumlahSebelumnya: BigDecimal = BigDecimal.ZERO

    private val rupiah: NumberFormat =
        NumberFormat.getNumberInstance(Locale("id","ID")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }

    fun setArgs(riwayat: RiwayatTransaksi, enrichedList: Array<DetailTransaksiRelasi>) {
        transaksiId = riwayat.tskId
        selectedPgnId = riwayat.tskIdPengguna
        val first = enrichedList.firstOrNull()
        detailId = first?.dtlId
        jumlahSebelumnya = first?.dtlNominal ?: BigDecimal.ZERO
    }

    /** Tampilkan saldo yang tersedia untuk edit = pgnSaldo + jumlahSebelumnya */
    fun refreshSaldo() {
        selectedPgnId?.let { pgnId ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val pengguna = client
                        .from("pengguna")
                        .select { filter { eq("pgnId", pgnId) } }
                        .decodeSingle<Pengguna>()

                    val pgnSaldo = pengguna.pgnSaldo ?: BigDecimal.ZERO
                    val available = pgnSaldo.add(jumlahSebelumnya)
                    _saldoText.value = "Rp ${rupiah.format(available)}"
                } catch (_: Exception) {
                    _saldoText.value = "Rp 0,00"
                }
            }
        }
    }

    fun submitUpdate(jumlahBaru: Double?, keterangan: String) {
        val pgnId = selectedPgnId
        val tId = transaksiId
        val dId = detailId

        if (pgnId.isNullOrBlank()) { _toast.tryEmit("Silakan pilih nasabah terlebih dahulu"); return }
        if (tId == null || dId == null) { _toast.tryEmit("Data transaksi tidak lengkap"); return }

        val jumlahBaruBD = jumlahBaru?.let { BigDecimal(it.toString()) }?.setScale(2, RoundingMode.HALF_UP)
        if (jumlahBaruBD == null || jumlahBaruBD <= BigDecimal.ZERO) {
            _toast.tryEmit("Jumlah penarikan tidak valid"); return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Ambil saldo terkini dari tabel pengguna
                val pengguna = client
                    .from("pengguna")
                    .select { filter { eq("pgnId", pgnId) } }
                    .decodeSingle<Pengguna>()

                val pgnSaldo = pengguna.pgnSaldo ?: BigDecimal.ZERO
                val available = pgnSaldo.add(jumlahSebelumnya)

                // Validasi saldo cukup
                if (available < jumlahBaruBD) {
                    _saldoKurang.emit(available)
                    _isLoading.value = false
                    return@launch
                }

                // Update header
                client.from("transaksi").update({
                    set("tskIdPengguna", pgnId)
                    set("tskKeterangan", keterangan)
                    set("tskTipe", "Keluar")
                }) {
                    filter { eq("tskId", tId) }
                }

                // Update detail → dtlNominal
                client.from("detail_transaksi").update({
                    set("dtlNominal", jumlahBaruBD)
                }) {
                    filter { eq("dtlId", dId) }
                }

                // Hitung & update saldo pengguna
                val saldoBaru = pgnSaldo.add(jumlahSebelumnya).subtract(jumlahBaruBD)
                client.from("pengguna").update(
                    mapOf("pgnSaldo" to saldoBaru)
                ) { filter { eq("pgnId", pgnId) } }

                _saldoText.value = "Rp ${rupiah.format(saldoBaru)}"
                _toast.emit("Transaksi berhasil diperbarui")
                _navigateBack.emit(Unit)
            } catch (_: Exception) {
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // helper
    private operator fun BigDecimal.compareTo(other: BigDecimal): Int = this.compareTo(other)
}