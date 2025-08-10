package com.gemahripah.banksampah.ui.admin.pengaturan.profil

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseAdminProvider
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class EditProfilViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client
    private val adminClient get() = SupabaseAdminProvider.client

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // One-shot events
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    fun submitUpdate(
        pengguna: Pengguna,
        namaBaru: String,
        emailBaru: String,
        passwordBaru: String
    ) {
        val isNamaBerubah = namaBaru != (pengguna.pgnNama ?: "")
        val isEmailBerubah = emailBaru != (pengguna.pgnEmail ?: "")
        val isPasswordBerubah = passwordBaru.isNotEmpty()

        if (namaBaru.isBlank() || emailBaru.isBlank()) {
            _toast.tryEmit("Nama dan Email tidak boleh kosong")
            return
        }
        if (!isNamaBerubah && !isEmailBerubah && !isPasswordBerubah) {
            _toast.tryEmit("Tidak ada perubahan data")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Cek unik bila berubah
                if (isNamaBerubah && isNamaDipakai(namaBaru)) {
                    _toast.emit("Nama sudah digunakan, gunakan nama lain")
                    _isLoading.value = false
                    return@launch
                }
                if (isEmailBerubah && isEmailDipakai(emailBaru)) {
                    _toast.emit("Email sudah digunakan, gunakan email lain")
                    _isLoading.value = false
                    return@launch
                }

                val userId = pengguna.pgnId

                // Update tabel "pengguna" (profil app)
                if (isNamaBerubah || isEmailBerubah) {
                    client.from("pengguna").update({
                        if (isNamaBerubah) set("pgnNama", namaBaru)
                        if (isEmailBerubah) set("pgnEmail", emailBaru)
                    }) {
                        filter { userId?.let { eq("pgnId", it) } }
                    }
                }

                // Update auth (email/password) via Admin API
                if ((isEmailBerubah || isPasswordBerubah) && userId != null) {
                    try {
                        adminClient.auth.admin.updateUserById(uid = userId) {
                            if (isEmailBerubah) email = emailBaru
                            if (isPasswordBerubah) password = passwordBaru
                        }
                    } catch (e: AuthWeakPasswordException) {
                        Log.e("EditProfilVM", "Password terlalu lemah: ${e.message}", e)
                        _toast.emit("Password minimal 6 karakter")
                        _isLoading.value = false
                        return@launch
                    } catch (e: Exception) {
                        Log.e("EditProfilVM", "Gagal update email/password via admin API: ${e.message}", e)
                        _toast.emit("Gagal memperbarui email atau password")
                        _isLoading.value = false
                        return@launch
                    }
                }

                _toast.emit("Data pengguna berhasil diperbarui")
                _navigateBack.emit(Unit)

            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("EditProfilVM", "Gagal edit pengguna: ${e.message}", e)
                _toast.emit("Gagal edit pengguna, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Helpers ---

    private suspend fun isNamaDipakai(nama: String): Boolean {
        return try {
            val normalized = nama.trim()
            val response = client
                .from("pengguna")
                .select(columns = Columns.list("pgnNama")) {
                    filter { ilike("pgnNama", normalized) }
                }
                .decodeList<Pengguna>()
            response.isNotEmpty()
        } catch (e: Exception) {
            Log.e("EditProfilVM", "Gagal cek nama unik: ${e.message}", e)
            false
        }
    }

    private suspend fun isEmailDipakai(email: String): Boolean {
        return try {
            val response = client
                .from("pengguna")
                .select(columns = Columns.list("pgnEmail")) {
                    filter { eq("pgnEmail", email.trim()) }
                }
                .decodeList<Pengguna>()
            response.isNotEmpty()
        } catch (e: Exception) {
            Log.e("EditProfilVM", "Gagal cek email unik: ${e.message}", e)
            false
        }
    }
}