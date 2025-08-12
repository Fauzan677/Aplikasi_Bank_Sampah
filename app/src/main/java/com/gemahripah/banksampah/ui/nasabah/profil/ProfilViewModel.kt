package com.gemahripah.banksampah.ui.nasabah.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilViewModel : ViewModel() {

    // Loading untuk enable/disable tombol
    val isLoading = MutableStateFlow(false)

    // Event sekali-tembak TANPA sealed class
    private val _logoutSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutSuccess = _logoutSuccess.asSharedFlow()

    private val _logoutError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val logoutError = _logoutError.asSharedFlow()

    fun logout() {
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client.auth.signOut()
                }
                _logoutSuccess.emit(Unit)
            } catch (e: RestException) {
                _logoutError.emit(e.message ?: "Logout gagal")
            } catch (_: Exception) {
                _logoutError.emit("Logout gagal, periksa koneksi internet")
            } finally {
                isLoading.value = false
            }
        }
    }
}