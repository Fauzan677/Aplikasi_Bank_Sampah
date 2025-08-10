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

                namaToIdMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                jenisToSatuanMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
                _sampahList.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun simpanTransaksi(
        keterangan: String,
        userId: String,
        inputUtama: Pair<String, Double>,
        inputTambahan: List<Pair<String, Double>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val transaksi = Transaksi(tskIdPengguna = userId, tskKeterangan = keterangan, tskTipe = "Masuk")
                    val inserted = SupabaseProvider.client
                        .from("transaksi")
                        .insert(transaksi) { select(Columns.list("tskId")) }
                        .decodeSingle<Transaksi>()

                    val transaksiId = inserted.tskId ?: error("Gagal menyimpan transaksi.")

                    // insert detail utama
                    val detailUtama = DetailTransaksi(
                        dtlTskId = transaksiId,
                        dtlSphId = namaToIdMap[inputUtama.first] ?: error("Jenis tidak ditemukan"),
                        dtlJumlah = inputUtama.second
                    )
                    SupabaseProvider.client.from("detail_transaksi").insert(detailUtama)

                    val bulk = inputTambahan.mapNotNull { (jenis, jumlah) ->
                        val id = namaToIdMap[jenis] ?: return@mapNotNull null
                        if (jumlah <= 0) return@mapNotNull null
                        DetailTransaksi(dtlTskId = transaksiId, dtlSphId = id, dtlJumlah = jumlah)
                    }
                    if (bulk.isNotEmpty()) {
                        SupabaseProvider.client.from("detail_transaksi").insert(bulk)
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}