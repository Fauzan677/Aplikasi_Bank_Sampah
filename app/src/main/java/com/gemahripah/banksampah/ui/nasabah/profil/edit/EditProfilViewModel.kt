package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfilViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    // UI state
    val isLoading = MutableStateFlow(false)

    // one-shot events
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast = _toast.asSharedFlow()

    private val _success = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val success = _success.asSharedFlow()

    fun updatePassword(passwordBaru: String) {
        if (isLoading.value) return

        viewModelScope.launch {
            isLoading.value = true
            try {
                if (passwordBaru.length < 6) {
                    _toast.emit("Password minimal 6 karakter")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    client.auth.updateUser { password = passwordBaru }
                }

                _toast.emit("Password berhasil diperbarui")
                _success.emit(Unit)

            } catch (e: SocketTimeoutException) {
                Log.e("EditProfilVM", "Timeout update password", e)
                _toast.emit("Gagal memperbarui password: koneksi timeout")
            } catch (e: HttpRequestException) {
                Log.e("EditProfilVM", "No internet update password", e)
                _toast.emit("Tidak ada koneksi internet")
            } catch (e: io.github.jan.supabase.auth.exception.AuthRestException) {
                Log.e("EditProfilVM", "Auth update password gagal code=${e.statusCode} err=${e.error} desc=${e.description}", e)
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
            } catch (e: RestException) {
                Log.e("EditProfilVM", "REST update password gagal: ${e.message}", e)
                _toast.emit("Gagal memperbarui password: ${e.message ?: "kesalahan server"}")
            } catch (e: Exception) {
                Log.e("EditProfilVM", "Update password exception umum", e)
                _toast.emit("Gagal memperbarui password")
            } finally {
                isLoading.value = false
            }
        }
    }
}