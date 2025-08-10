package com.gemahripah.banksampah.ui.welcome.masuk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MasukViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> get() = _toast

    private val _loginSuccess = MutableLiveData<Pengguna?>()
    val loginSuccess: LiveData<Pengguna?> get() = _loginSuccess

    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) sign in
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // 2) ambil session
                val session = client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (userId.isNullOrBlank()) {
                    _toast.postValue("User ID tidak ditemukan")
                    return@launch
                }

                // 3) ambil data pengguna
                val pengguna = getPenggunaById(userId)
                if (pengguna == null) {
                    _toast.postValue("Data pengguna tidak ditemukan")
                    return@launch
                }

                // 4) sukses
                _loginSuccess.postValue(pengguna)
                _toast.postValue("Login berhasil")
            } catch (e: HttpRequestException) {
                _toast.postValue("Tidak ada koneksi internet")
            } catch (e: AuthRestException) {
                val msg = e.error.lowercase()
                if (msg.contains("invalid_credentials")) {
                    _toast.postValue("Email atau Password salah")
                } else {
                    _toast.postValue("Terjadi kesalahan saat login")
                }
            } catch (e: Exception) {
                Log.e("MasukViewModel", "Login error: ${e.message}", e)
                _toast.postValue("Terjadi kesalahan saat login")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun getPenggunaById(userId: String): Pengguna? = withContext(Dispatchers.IO) {
        client.postgrest
            .from("pengguna")
            .select {
                filter { eq("pgnId", userId) }
                limit(1)
            }
            .decodeSingleOrNull<Pengguna>()
    }

    /** panggil setelah Fragment selesai navigate supaya event tidak terulang */
    fun consumeLoginSuccess() {
        _loginSuccess.value = null
    }

    /** opsional: reset toast supaya tidak tampil ulang saat rotasi */
    fun consumeToast() {
        _toast.value = null
    }
}
