package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit.sampah

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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

class EditJenisSampahViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _kategoriList = MutableStateFlow<List<Kategori>>(emptyList())
    val kategoriList: StateFlow<List<Kategori>> = _kategoriList

    private val _satuanList = MutableStateFlow<List<String>>(emptyList())
    val satuanList: StateFlow<List<String>> = _satuanList

    private val _selectedKategoriId = MutableStateFlow<Long?>(null)

    private val _usedInDetail = MutableStateFlow<Boolean?>(null)   // null = gagal cek/belum
    val usedInDetail: StateFlow<Boolean?> = _usedInDetail.asStateFlow()

    // Events
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    // Data awal untuk perbandingan
    private var originalRelasi: SampahRelasi? = null

    fun initFromArgs(relasi: SampahRelasi) {
        originalRelasi = relasi
        _selectedKategoriId.value = relasi.sphKtgId?.ktgId
    }

    fun setSelectedKategoriId(id: Long?) {
        _selectedKategoriId.value = id
    }

    fun loadKategori() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = client.postgrest["kategori"]
                    .select()
                    .decodeList<Kategori>()
                _kategoriList.value = result

                if (_selectedKategoriId.value == null) {
                    originalRelasi?.sphKtgId?.ktgNama?.let { nama ->
                        _selectedKategoriId.value = result.find { it.ktgNama == nama }?.ktgId
                    }
                }
            } catch (e: Exception) {
                Log.e("EditJenisVM", "loadKategori gagal: ${e.message}", e)
                _toast.tryEmit("Gagal memuat kategori")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDistinctSatuan() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = client
                    .from("sampah")
                    .select(columns = Columns.list("sphSatuan"))
                    .decodeList<Sampah>()
                    .mapNotNull { it.sphSatuan }
                    .distinct()

                _satuanList.value = result
            } catch (e: Exception) {
                Log.e("EditJenisVM", "loadDistinctSatuan gagal: ${e.message}", e)
                _toast.tryEmit("Gagal memuat data satuan")
            }
        }
    }

    fun submitUpdate(
        inputJenis: String,
        inputKode: String,
        inputSatuan: String,
        inputHarga: Long?,
        inputKeterangan: String
    ) {
        val old = originalRelasi ?: run {
            _toast.tryEmit("Data tidak ditemukan"); return
        }
        val id = old.sphId ?: run {
            _toast.tryEmit("ID tidak ditemukan"); return
        }

        val kategoriIdBaru = _selectedKategoriId.value
        val jenisBaru = inputJenis.trim()
        val kodeBaru  = inputKode.trim()
        val satuanBaru = inputSatuan.trim()
        val ketBaru = inputKeterangan.trim()

        if (kategoriIdBaru == null || jenisBaru.isEmpty() || kodeBaru.isEmpty() ||
            satuanBaru.isEmpty() || inputHarga == null) {
            _toast.tryEmit("Isi semua data dengan benar")
            return
        }

        // Tidak ada perubahan?
        val noChange =
            (kategoriIdBaru == (old.sphKtgId?.ktgId)) &&
                    (jenisBaru == (old.sphJenis ?: "")) &&
                    (kodeBaru == (old.sphKode ?: "")) &&
                    (satuanBaru == (old.sphSatuan ?: "")) &&
                    (inputHarga == (old.sphHarga ?: -1L)) &&
                    (ketBaru == (old.sphKeterangan ?: ""))

        if (noChange) {
            _toast.tryEmit("Tidak ada perubahan data")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Cek duplikat ketika berubah
                if (jenisBaru != old.sphJenis && isJenisSampahDipakai(jenisBaru, id)) {
                    _toast.emit("Jenis sampah sudah digunakan, gunakan nama lain")
                    _isLoading.value = false; return@launch
                }
                if (kodeBaru != (old.sphKode ?: "") && isKodeDipakai(kodeBaru, id)) {
                    _toast.emit("Kode sampah sudah digunakan, gunakan kode lain")
                    _isLoading.value = false; return@launch
                }

                client.from("sampah").update({
                    set("sphKtgId", kategoriIdBaru)
                    set("sphJenis", jenisBaru)
                    set("sphKode",  kodeBaru)
                    set("sphSatuan", satuanBaru)
                    set("sphHarga", inputHarga)
                    set("sphKeterangan", ketBaru.ifEmpty { null })
                }) {
                    filter { eq("sphId", id) }
                }

                // Perbarui cache lokal
                originalRelasi = old.copy(
                    sphKtgId = old.sphKtgId?.copy(ktgId = kategoriIdBaru) ?: old.sphKtgId,
                    sphJenis = jenisBaru,
                    sphKode = kodeBaru,
                    sphSatuan = satuanBaru,
                    sphHarga = inputHarga,
                    sphKeterangan = ketBaru.ifEmpty { null }
                )

                _toast.emit("Data berhasil diperbarui")
                _navigateBack.emit(Unit)

            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("EditJenisVM", "update gagal: ${e.message}", e)
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCurrent() {
        val id = originalRelasi?.sphId ?: run {
            _toast.tryEmit("ID tidak ditemukan"); return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                client.from("sampah").delete {
                    filter { eq("sphId", id) }
                }
                _toast.emit("Data berhasil dihapus")
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                Log.e("EditJenisVM", "delete gagal: ${e.message}", e)
                _toast.emit("Gagal menghapus data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Cek unik (case-insensitive) + exclude current row ---
    private suspend fun isJenisSampahDipakai(jenis: String, excludeId: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val list = client
                    .from("sampah")
                    .select(columns = Columns.list("sphId")) {
                        filter {
                            ilike("sphJenis", jenis.trim())   // exact, case-insensitive
                            neq("sphId", excludeId)
                        }
                    }
                    .decodeList<Sampah>()
                list.isNotEmpty()
            } catch (e: Exception) {
                Log.e("EditJenisVM", "cek jenis unik gagal: ${e.message}", e)
                false
            }
        }

    private suspend fun isKodeDipakai(kode: String, excludeId: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val list = client
                    .from("sampah")
                    .select(columns = Columns.list("sphId")) {
                        filter {
                            ilike("sphKode", kode.trim())     // exact, case-insensitive
                            neq("sphId", excludeId)
                        }
                    }
                    .decodeList<Sampah>()
                list.isNotEmpty()
            } catch (e: Exception) {
                Log.e("EditJenisVM", "cek kode unik gagal: ${e.message}", e)
                false
            }
        }

    fun checkUsedInDetail(sphId: Long?) {
        if (sphId == null) { _usedInDetail.value = null; return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = client
                    .from("detail_transaksi")
                    .select(columns = Columns.list("dtlId")) {
                        filter { eq("dtlSphId", sphId) }
                        limit(1)
                    }
                    .decodeList<DetailTransaksiRelasi>()   // cukup dtlId saja

                _usedInDetail.value = list.isNotEmpty()
            } catch (e: Exception) {
                _usedInDetail.value = null
            }
        }
    }
}