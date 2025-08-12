package com.gemahripah.banksampah.ui.admin.transaksi.masuk.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditTransaksiMasukViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Master data
    private val _jenisList = MutableStateFlow<List<String>>(emptyList())
    val jenisList: StateFlow<List<String>> = _jenisList.asStateFlow()

    private val _namaToIdMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val namaToIdMap: StateFlow<Map<String, Long>> = _namaToIdMap.asStateFlow()

    private val _jenisToSatuanMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val jenisToSatuanMap: StateFlow<Map<String, String>> = _jenisToSatuanMap.asStateFlow()

    // UI one-shot events
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    /** Load master ‘sampah’ sekali */
    fun loadSampah() {
        if (_jenisList.value.isNotEmpty()) return // sudah pernah load
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val data = client
                    .from("sampah")
                    .select()
                    .decodeList<Sampah>()

                val listJenis = data.mapNotNull { it.sphJenis?.takeIf { s -> s.isNotBlank() } }
                _jenisList.value = listJenis

                _namaToIdMap.value = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                _jenisToSatuanMap.value = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
            } catch (e: Exception) {
                viewModelScope.launch { _toast.emit("Gagal memuat jenis transaksi") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Satuan untuk label jumlah */
    fun getSatuan(jenis: String?): String = jenisToSatuanMap.value[jenis] ?: "Unit"

    fun getAvailableJenis(currentJenis: String?, selectedSemua: Set<String>): List<String> {
        val all = jenisList.value
        val withoutSelected = all.filter { it.isNotBlank() && it !in selectedSemua }
        val keepCurrent = currentJenis?.takeIf { it.isNotBlank() }
        return (withoutSelected + listOfNotNull(keepCurrent)).distinct()
    }

    /** Submit update transaksi + replace detail (IO di ViewModel) */
    fun submitEditTransaksiMasuk(
        transaksiId: Long,
        userId: String,
        keterangan: String,
        main: Pair<String, Double>,
        tambahan: List<Pair<String, Double>>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // update header
                client
                    .from("transaksi")
                    .update({
                        set("tskIdPengguna", userId)
                        set("tskKeterangan", keterangan)
                        set("tskTipe", "Masuk")
                    }) {
                        filter { eq("tskId", transaksiId) }
                    }

                // replace detail
                deleteOldDetails(transaksiId)
                insertNewDetails(transaksiId, main, tambahan)

                _toast.emit("Data berhasil diperbarui")
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun deleteOldDetails(transaksiId: Long) {
        client
            .from("detail_transaksi")
            .delete {
                filter { eq("dtlTskId", transaksiId) }
            }
    }

    private suspend fun insertNewDetails(
        transaksiId: Long,
        main: Pair<String, Double>,
        tambahan: List<Pair<String, Double>>
    ) {
        val map = namaToIdMap.value

        suspend fun insertOne(jenis: String, jumlah: Double) {
            val sphId = map[jenis] ?: return
            if (jumlah <= 0.0) return
            val detail = DetailTransaksi(
                dtlTskId = transaksiId,
                dtlSphId = sphId,
                dtlJumlah = jumlah
            )
            client.from("detail_transaksi").insert(detail)
        }

        // utama
        insertOne(main.first, main.second)
        // tambahan
        tambahan.forEach { (j, n) -> insertOne(j, n) }
    }
}