package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.network.sockets.SocketTimeoutException
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
                // 1) Validasi awal
                val noChange = (namaBaru == namaLama) && passwordBaru.isBlank()
                if (noChange) {
                    _toast.emit("Tidak ada perubahan yang dilakukan")
                    return@launch
                }

                // Cek unik nama hanya jika berubah (sebelum update apa pun)
                if (namaBaru != namaLama && isNamaDipakai(namaBaru)) {
                    _toast.emit("Nama sudah digunakan, gunakan nama lain")
                    return@launch
                }

                // 2) UPDATE AUTH PASSWORD DULU (jika ada)
                if (passwordBaru.isNotBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            client.auth.updateUser { password = passwordBaru }
                        }
                    }
                    // Koneksi/errors spesifik
                    catch (e: SocketTimeoutException) {
                        Log.e("EditProfilVM", "Timeout update password", e)
                        _toast.emit("Gagal memperbarui password: koneksi timeout")
                        return@launch
                    } catch (e: HttpRequestException) {
                        Log.e("EditProfilVM", "No internet update password", e)
                        _toast.emit("Tidak ada koneksi internet")
                        return@launch
                    } catch (e: io.github.jan.supabase.auth.exception.AuthRestException) {
                        Log.e(
                            "EditProfilVM",
                            "Auth update password gagal code=${e.statusCode} err=${e.error} desc=${e.description}",
                            e
                        )
                        when {
                            e.error.contains("weak_password", true) ->
                                _toast.emit("Password minimal 6 karakter")
                            e.statusCode == 401 || e.error.contains("token", true) || e.error.contains("expired", true) ->
                                _toast.emit("Sesi kedaluwarsa, silakan login ulang")
                            e.statusCode == 400 && e.error.contains("password", true) ->
                                _toast.emit("Format password tidak valid")
                            else ->
                                _toast.emit("Gagal memperbarui password: ${e.error}")
                        }
                        return@launch
                    } catch (e: RestException) {
                        Log.e("EditProfilVM", "REST update password gagal: ${e.message}", e)
                        _toast.emit("Gagal memperbarui password: ${e.message ?: "kesalahan server"}")
                        return@launch
                    } catch (e: Exception) {
                        Log.e("EditProfilVM", "Update password exception umum", e)
                        _toast.emit("Gagal memperbarui password")
                        return@launch
                    }
                }

                // 3) BARU UPDATE NAMA DI TABEL `pengguna` (jika berubah)
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

                _toast.emit("Data pengguna berhasil diperbarui")
                _success.emit(Unit)

            } catch (e: Exception) {
                Log.e("EditProfilVM", "Update profil gagal: ${e.message}", e)
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                isLoading.value = false
            }
        }
    }
}