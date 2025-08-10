package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfilViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private var userId: String? = null
    private var namaLama: String? = null

    // UI state
    val isLoading = MutableStateFlow(false)

    // One-shot events (tanpa sealed class)
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast = _toast.asSharedFlow()

    private val _success = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val success = _success.asSharedFlow()

    fun setInitialData(pengguna: Pengguna) {
        userId = pengguna.pgnId
        namaLama = pengguna.pgnNama
    }

    private suspend fun isNamaDipakai(nama: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client
                .from("pengguna")
                .select(columns = Columns.list("pgnNama")) {
                    filter { ilike("pgnNama", nama.trim()) } // exact (case-insensitive)
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("EditProfilVM", "Cek nama unik gagal: ${e.message}", e)
            false
        }
    }

    fun updateProfil(namaBaruInput: String, passwordBaru: String) {
        if (isLoading.value) return
        val namaBaru = namaBaruInput.trim()

        viewModelScope.launch {
            isLoading.value = true
            try {
                // Tidak ada perubahan
                val noChange = (namaBaru == namaLama) && passwordBaru.isBlank()
                if (noChange) {
                    _toast.emit("Tidak ada perubahan yang dilakukan")
                    return@launch
                }

                // Cek nama unik hanya jika berubah
                if (namaBaru != namaLama && isNamaDipakai(namaBaru)) {
                    _toast.emit("Nama sudah digunakan, gunakan nama lain")
                    return@launch
                }

                // Update nama (hanya jika berubah)
                if (namaBaru != namaLama) {
                    withContext(Dispatchers.IO) {
                        client.from("pengguna").update({
                            set("pgnNama", namaBaru)
                        }) {
                            filter { userId?.let { eq("pgnId", it) } }
                        }
                    }
                    namaLama = namaBaru
                }

                // Update password (opsional)
                if (passwordBaru.isNotBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            client.auth.updateUser { password = passwordBaru }
                        }
                    } catch (e: RestException) {
                        val msg = e.message?.lowercase().orEmpty()
                        _toast.emit(if (msg.contains("weak_password")) "Password minimal 6 karakter"
                        else "Gagal memperbarui password")
                        return@launch
                    } catch (_: Exception) {
                        _toast.emit("Gagal memperbarui password")
                        return@launch
                    }
                }

                _toast.emit("Data pengguna berhasil diperbarui")
                _success.emit(Unit)

            } catch (_: Exception) {
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                isLoading.value = false
            }
        }
    }
}