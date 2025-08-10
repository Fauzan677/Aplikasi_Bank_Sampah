package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.tambah

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

class TambahPenggunaViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client
    private val admin get() = SupabaseAdminProvider.client

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    /** Submit tambah pengguna. Validasi, cek unik, buat akun auth, lalu insert row `pengguna`. */
    fun submit(nama: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (nama.isBlank() || email.isBlank() || password.isBlank()) {
                _toast.emit("Semua isian wajib diisi")
                return@launch
            }
            if (password.length < 6) {
                _toast.emit("Password minimal 6 karakter")
                return@launch
            }

            _isLoading.value = true
            try {
                // Cek unik (case-insensitive)
                if (isNamaDipakai(nama)) {
                    _toast.emit("Nama sudah digunakan, gunakan nama lain")
                    return@launch
                }
                if (isEmailDipakai(email)) {
                    _toast.emit("Email sudah digunakan, gunakan email lain")
                    return@launch
                }

                // Buat user di Auth (autoConfirm) + metadata
                val created = admin.auth.admin.createUserWithEmail {
                    this.email = email
                    this.password = password
                    userMetadata { put("name", nama) }
                    autoConfirm = true
                }

                // Insert ke tabel aplikasi
                val penggunaBaru = Pengguna(
                    pgnId = created.id,
                    pgnNama = nama,
                    pgnEmail = email
                )
                client.from("pengguna").insert(penggunaBaru)

                _toast.emit("Pengguna berhasil dibuat")
                _navigateBack.emit(Unit)
            } catch (e: AuthWeakPasswordException) {
                _toast.emit("Password minimal 6 karakter")
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("TambahPenggunaVM", "Gagal membuat pengguna: ${e.message}", e)
                _toast.emit("Gagal membuat pengguna, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Cek nama unik (case-insensitive) */
    private suspend fun isNamaDipakai(nama: String): Boolean {
        return try {
            client
                .from("pengguna")
                .select(columns = Columns.list("pgnNama")) {
                    filter { ilike("pgnNama", nama.trim()) } // exact match, ignore case
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("TambahPenggunaVM", "Gagal cek nama unik: ${e.message}", e)
            false
        }
    }

    /** Cek email unik (case-insensitive) */
    private suspend fun isEmailDipakai(email: String): Boolean {
        return try {
            client
                .from("pengguna")
                .select(columns = Columns.list("pgnEmail")) {
                    filter { ilike("pgnEmail", email.trim()) } // exact match, ignore case
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("TambahPenggunaVM", "Gagal cek email unik: ${e.message}", e)
            false
        }
    }
}