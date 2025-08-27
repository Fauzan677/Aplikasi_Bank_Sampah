package com.gemahripah.banksampah.ui.admin.transaksi.masuk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SetorSampahViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _penggunaList = MutableStateFlow<List<Pengguna>>(emptyList())
    val penggunaList: StateFlow<List<Pengguna>> get() = _penggunaList

    private val _sampahList = MutableStateFlow<List<Sampah>>(emptyList())
    val sampahList: StateFlow<List<Sampah>> get() = _sampahList

    var selectedUserId: String? = null
    var namaToIdMap: Map<String, Long> = emptyMap()
    var jenisToSatuanMap: Map<String, String> = emptyMap()
    var jenisToHargaMap: Map<String, BigDecimal> = emptyMap()

    fun loadPengguna() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pengguna = SupabaseProvider.client
                    .from("pengguna")
                    .select {
                        filter { eq("pgnIsAdmin", false) }
                    }
                    .decodeList<Pengguna>()
                _penggunaList.value = pengguna
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSampah() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = SupabaseProvider.client
                    .from("sampah")
                    .select()
                    .decodeList<Sampah>()

                namaToIdMap       = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                jenisToSatuanMap  = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
                jenisToHargaMap   = data.associate { (it.sphJenis ?: "Unknown") to
                        BigDecimal.valueOf((it.sphHarga ?: 0).toLong())
                }

                _sampahList.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun simpanTransaksi(
        keterangan: String,
        userId: String,
        inputUtama: Pair<String, BigDecimal>,
        inputTambahan: List<Pair<String, BigDecimal>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    // 1) buat transaksi
                    val transaksi = Transaksi(tskIdPengguna = userId, tskKeterangan = keterangan, tskTipe = "Masuk")
                    val inserted = SupabaseProvider.client
                        .from("transaksi")
                        .insert(transaksi) { select(Columns.list("tskId")) }
                        .decodeSingle<Transaksi>()

                    val transaksiId = inserted.tskId ?: error("Gagal menyimpan transaksi.")

                    // 2) hitung & insert detail
                    val (jenisUtama, jumlahUtama) = inputUtama
                    val sphIdUtama   = namaToIdMap[jenisUtama] ?: error("Jenis tidak ditemukan")
                    val hargaUtama   = jenisToHargaMap[jenisUtama] ?: BigDecimal.ZERO
                    val nominalUtama = jumlahUtama.multiply(hargaUtama)

                    SupabaseProvider.client.from("detail_transaksi").insert(
                        DetailTransaksi(
                            dtlTskId = transaksiId,
                            dtlSphId = sphIdUtama,
                            dtlJumlah = jumlahUtama,
                            dtlNominal = nominalUtama
                        )
                    )

                    var totalNominal = nominalUtama

                    val bulk = inputTambahan.mapNotNull { (jenis, jumlah) ->
                        val id = namaToIdMap[jenis] ?: return@mapNotNull null
                        if (jumlah.compareTo(BigDecimal.ZERO) <= 0) return@mapNotNull null

                        val harga   = jenisToHargaMap[jenis] ?: BigDecimal.ZERO
                        val nominal = jumlah.multiply(harga)
                        totalNominal = totalNominal.add(nominal)

                        DetailTransaksi(
                            dtlTskId = transaksiId,
                            dtlSphId = id,
                            dtlJumlah = jumlah,
                            dtlNominal = nominal
                        )
                    }
                    if (bulk.isNotEmpty()) {
                        SupabaseProvider.client.from("detail_transaksi").insert(bulk)
                    }

                    // 3) update saldo pengguna = saldo lama + totalNominal
                    updateSaldoPengguna(userId, totalNominal)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Tambah saldo pengguna secara sederhana (ambil → tambah → update). */
    private suspend fun updateSaldoPengguna(userId: String, tambah: BigDecimal) {
        // Ambil saldo saat ini
        val pengguna = SupabaseProvider.client
            .from("pengguna")
            .select { filter { eq("pgnId", userId) } }
            .decodeSingle<Pengguna>()

        val saldoLama = pengguna.pgnSaldo ?: BigDecimal.ZERO
        val saldoBaru = saldoLama.add(tambah)

        // Update saldo
        SupabaseProvider.client
            .from("pengguna")
            .update(mapOf("pgnSaldo" to saldoBaru)) { filter { eq("pgnId", userId) } }
    }
}