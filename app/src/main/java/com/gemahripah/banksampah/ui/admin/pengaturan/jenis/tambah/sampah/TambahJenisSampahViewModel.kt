package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.tambah.sampah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahJenisSampahViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _kategori = MutableStateFlow<List<Kategori>>(emptyList())
    val kategori: StateFlow<List<Kategori>> = _kategori

    private val _satuanSuggestions = MutableStateFlow<List<String>>(emptyList())
    val satuanSuggestions: StateFlow<List<String>> = _satuanSuggestions

    // UI events
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    // selection
    private var selectedKategoriId: Long? = null
    fun setSelectedKategoriId(id: Long?) { selectedKategoriId = id }

    fun loadAwal() {
        loadKategori()
        loadSatuanSuggestions()
    }

    private fun loadKategori() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = client.postgrest["kategori"]
                    .select()
                    .decodeList<Kategori>()
                _kategori.value = list
            } catch (_: Exception) {
                _kategori.value = emptyList()
                _toast.tryEmit("Gagal memuat kategori")
            }
        }
    }

    private fun loadSatuanSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rows = client
                    .from("sampah")
                    .select(columns = Columns.list("sphSatuan"))
                    .decodeList<Sampah>()
                _satuanSuggestions.value = rows.mapNotNull { it.sphSatuan }.distinct()
            } catch (_: Exception) {
                _satuanSuggestions.value = emptyList()
                _toast.tryEmit("Gagal memuat satuan")
            }
        }
    }

    /** Cek duplikasi jenis (case-insensitive, exact). */
    private suspend fun isJenisDipakai(jenis: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = jenis.trim()
            client
                .from("sampah")
                .select(columns = Columns.list("sphJenis")) {
                    filter { ilike("sphJenis", normalized) } // exact, ignore case
                }
                .decodeList<Sampah>()
                .isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun isKodeDipakai(kode: String): Boolean = withContext(Dispatchers.IO) {
        if (kode.isBlank()) return@withContext false
        try {
            client.from("sampah")
                .select(columns = Columns.list("sphKode")) {
                    filter { ilike("sphKode", kode.trim().uppercase()) }
                }
                .decodeList<Sampah>()
                .isNotEmpty()
        } catch (_: Exception) { false }
    }

    /** Validasi + insert data. Semua di background, aman lifecycle. */
    fun submit(
        jenis: String,
        kode: String?,                  // <-- baru
        satuan: String,
        harga: Long?,                   // <-- sesuaikan dengan toLongOrNull() di Fragment
        keterangan: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val ktgId = selectedKategoriId
                if (ktgId == null) {
                    _toast.emit("Silakan pilih kategori transaksi")
                    return@launch
                }
                if (jenis.isBlank() || satuan.isBlank() || harga == null) {
                    _toast.emit("Isi semua data dengan benar")
                    return@launch
                }

                // Normalisasi & cek unik kode (jika diisi)
                val kodeFix = kode?.trim()?.uppercase()?.ifBlank { null }
                if (!kodeFix.isNullOrEmpty() && isKodeDipakai(kodeFix)) {
                    _toast.emit("Kode sudah digunakan")
                    return@launch
                }

                // (opsional) kalau mau batasi panjang/karakter:
                // if (!kodeFix.isNullOrEmpty() && kodeFix.length > 20) { ... }

                // Cek duplikat jenis (logika lama kamu)
                if (isJenisDipakai(jenis)) {
                    _toast.emit("Jenis sampah sudah digunakan, gunakan nama lain")
                    return@launch
                }

                val data = Sampah(
                    sphKtgId = ktgId,
                    sphJenis = jenis.trim(),
                    sphSatuan = satuan.trim(),
                    sphHarga  = harga,
                    sphKeterangan = keterangan?.ifBlank { null },
                    sphKode = kodeFix                  // <-- simpan kode
                )

                client.postgrest["sampah"].insert(data)

                _toast.emit("Jenis sampah berhasil ditambahkan")
                _navigateBack.emit(Unit)

            } catch (_: Exception) {
                _toast.emit("Gagal menambahkan data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }
}