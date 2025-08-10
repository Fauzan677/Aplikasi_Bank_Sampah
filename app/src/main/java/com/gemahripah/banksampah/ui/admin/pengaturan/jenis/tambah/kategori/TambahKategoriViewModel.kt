package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.tambah.kategori

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
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
import kotlin.coroutines.cancellation.CancellationException

class TambahKategoriViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _done = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val done: SharedFlow<Unit> = _done

    /** Cek duplikasi kategori (case-insensitive, exact match). */
    suspend fun isKategoriDipakai(nama: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = nama.trim()
            client
                .from("kategori")
                .select(columns = Columns.list("ktgNama")) {
                    filter { ilike("ktgNama", normalized) } // exact, ignore case (tanpa %)
                }
                .decodeList<Kategori>()
                .isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /** Insert kategori baru. */
    fun tambahKategori(namaKategori: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                client.from("kategori").insert(mapOf("ktgNama" to namaKategori))
                _toast.emit("Kategori berhasil ditambahkan")
                _done.emit(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _toast.emit("Gagal menambahkan kategori, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
