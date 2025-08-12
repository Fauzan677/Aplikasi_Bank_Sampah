package com.gemahripah.banksampah.ui.welcome.daftar

import android.util.Log
import androidx.lifecycle.*
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.Locale

class DaftarViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> get() = _toast

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> get() = _registerSuccess

    /** Cek nama (case-insensitive) — dijalankan di IO */
    private suspend fun isNamaExists(nama: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client
                .from("pengguna")
                .select(columns = Columns.list("pgnNama")) {
                    filter { ilike("pgnNama", nama.trim()) } // exact (tanpa %) tapi case-insensitive
                    limit(1)
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("DaftarViewModel", "Cek nama unik gagal: ${e.message}", e)
            false
        }
    }

    /** Insert ke tabel pengguna (IO) */
    private suspend fun insertUserToDatabase(nama: String, email: String) = withContext(Dispatchers.IO) {
        val emailNormalized = email.trim().lowercase(Locale.ROOT)
        try {
            client.from("pengguna").insert(
                mapOf(
                    "pgnNama" to nama.trim(),
                    "pgnEmail" to emailNormalized
                )
            )
        } catch (e: RestException) {
            Log.e("DaftarViewModel", "RestException insert: ${e.message}", e)
            val msg = e.message?.lowercase().orEmpty()
            if (msg.contains("duplicate") && msg.contains("pgnnama")) {
                throw IllegalStateException("Nama sudah digunakan")
            }
            throw Exception("Gagal menyimpan data pengguna")
        } catch (e: Exception) {
            Log.e("DaftarViewModel", "Exception insert: ${e.message}", e)
            throw Exception("Gagal menyimpan data pengguna")
        }
    }

    /** Orkestrasi daftar lengkap — semua di background */
    fun register(nama: String, email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) cek nama
                if (isNamaExists(nama)) {
                    _toast.postValue("Nama sudah digunakan, gunakan nama lain")
                    return@launch
                }

                // 2) sign up
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // 3) insert ke tabel pengguna
                insertUserToDatabase(nama, email)

                // 4) sign out (opsional kalau email verification flow)
                client.auth.signOut()

                _registerSuccess.postValue(true)
                _toast.postValue("Pendaftaran berhasil")
            } catch (e: AuthWeakPasswordException) {
                _toast.postValue("Password minimal 6 karakter")
            } catch (e: AuthRestException) {
                val msg = e.error.lowercase()
                if (msg.contains("already_exists")) {
                    _toast.postValue("Email sudah digunakan, gunakan email lain")
                } else {
                    _toast.postValue("Gagal daftar, periksa koneksi internet")
                }
            } catch (e: BadRequestRestException) {
                _toast.postValue("Email tidak valid atau sudah terdaftar")
            } catch (e: IllegalStateException) {
                // dari insertUserToDatabase untuk nama duplikat
                _toast.postValue(e.message ?: "Nama sudah digunakan")
            } catch (e: Exception) {
                Log.e("DaftarViewModel", "Register error: ${e.message}", e)
                _toast.postValue("Gagal daftar, periksa koneksi internet")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /** Konsumsi event sekali pakai */
    fun consumeToast() { _toast.value = null }
    fun consumeSuccess() { _registerSuccess.value = false }
}