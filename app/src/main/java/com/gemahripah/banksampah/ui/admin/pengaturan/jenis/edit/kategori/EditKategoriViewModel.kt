package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit.kategori

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditKategoriViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // UI Events
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private val _inUse = MutableStateFlow<Boolean?>(null)
    val inUse: StateFlow<Boolean?> = _inUse.asStateFlow()

    // Data awal
    private var original: Kategori? = null

    fun init(kategori: Kategori) {
        original = kategori
    }

    fun updateKategori(namaBaru: String) {
        val current = original ?: run {
            _toast.tryEmit("Data tidak ditemukan")
            return
        }
        val id = current.ktgId
        val lama = current.ktgNama.orEmpty()

        val baru = namaBaru.trim()
        if (baru.isEmpty()) {
            _toast.tryEmit("Nama kategori tidak boleh kosong")
            return
        }
        if (baru == lama) {
            _toast.tryEmit("Tidak ada perubahan data")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Cek duplikasi nama (case-insensitive, exclude current id)
                if (isKategoriDipakai(baru, excludeId = id)) {
                    _toast.emit("Nama kategori sudah digunakan, gunakan nama lain")
                    _isLoading.value = false
                    return@launch
                }

                client.from("kategori").update({
                    set("ktgNama", baru)
                }) {
                    filter { if (id != null) eq("ktgId", id) }
                }

                // Update cache lokal
                original = current.copy(ktgNama = baru)

                _toast.emit("Kategori berhasil diperbarui")
                _navigateBack.emit(Unit)

            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("EditKategoriVM", "Gagal update kategori: ${e.message}", e)
                _toast.emit("Gagal memperbarui kategori, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteKategori() {
        val id = original?.ktgId ?: run {
            _toast.tryEmit("Data tidak ditemukan")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                client.from("kategori").delete {
                    filter { eq("ktgId", id) }
                }

                _toast.emit("Kategori berhasil dihapus")
                _navigateBack.emit(Unit)

            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("EditKategoriVM", "Gagal hapus kategori: ${e.message}", e)
                _toast.emit("Gagal menghapus kategori, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun isKategoriDipakai(nama: String, excludeId: Long?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val list = client
                    .from("kategori")
                    .select(columns = Columns.list("ktgId, ktgNama")) {
                        filter {
                            ilike("ktgNama", nama.trim()) // exact case-insensitive
                            if (excludeId != null) neq("ktgId", excludeId)
                        }
                    }
                    .decodeList<Kategori>()
                list.isNotEmpty()
            } catch (e: Exception) {
                Log.e("EditKategoriVM", "Cek duplikat gagal: ${e.message}", e)
                false
            }
        }

    fun checkRelasiSampah() {
        val id = original?.ktgId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = client
                    .from("sampah")
                    .select(columns = Columns.list("sphId")) {
                        filter { eq("sphKtgId", id) }
                        limit(1) // cukup tahu eksistensi
                    }
                    .decodeList<SampahRelasi>()  // ← pakai model yang ada

                _inUse.value = list.isNotEmpty()
            } catch (e: Exception) {
                _inUse.value = null  // konservatif: anggap tidak aman menghapus
            }
        }
    }
}