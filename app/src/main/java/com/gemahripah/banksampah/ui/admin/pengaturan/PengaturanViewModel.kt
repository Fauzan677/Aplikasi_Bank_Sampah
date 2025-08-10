package com.gemahripah.banksampah.ui.admin.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PengaturanViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // Loading state (untuk disable tombol, dsb.)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // One-shot events
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _logoutSuccess = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val logoutSuccess: SharedFlow<Unit> = _logoutSuccess

    /** Jalankan signOut di background (IO dispatcher). */
    fun logout() {
        if (_isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                client.auth.signOut()
                _toast.emit("Logout berhasil")
                _logoutSuccess.emit(Unit)
            } catch (e: Exception) {
                _toast.emit("Gagal logout, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }
}