package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.tambah

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseAdminProvider
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
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
import java.util.Locale

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
    fun submit(
        nama: String,
        email: String,
        password: String,
        rekening: String?,
        alamat: String?,
        saldoInput: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val namaTrim   = nama.trim()
            val emailTrim  = email.trim()
            val emailLower = emailTrim.lowercase(Locale.ROOT)
            val pwdTrim    = password.trim()
            val rekeningTrim = rekening?.trim().orEmpty()
            val alamatTrim   = alamat?.trim().orEmpty()

            if (namaTrim.isBlank() || emailLower.isBlank() || pwdTrim.isBlank()) {
                _toast.emit("Semua isian wajib diisi")
                return@launch
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailLower).matches()) {
                _toast.emit("Format email tidak valid")
                return@launch
            }
            if (pwdTrim.length < 6) {
                _toast.emit("Password minimal 6 karakter")
                return@launch
            }

            // Parse saldo (boleh kosong). Dukung koma/titik.
            val saldoParsed = parseDecimalOrNull(saldoInput)
            if (saldoInput.isNotBlank() && saldoParsed == null) {
                _toast.emit("Saldo awal tidak valid")
                return@launch
            }

            _isLoading.value = true
            try {
                if (isNamaDipakai(namaTrim)) {
                    _toast.emit("Nama sudah digunakan, gunakan nama lain")
                    return@launch
                }
                if (isEmailDipakai(emailLower)) {
                    _toast.emit("Email sudah digunakan, gunakan email lain")
                    return@launch
                }

                // 1) Buat user auth + metadata opsional
                val created = admin.auth.admin.createUserWithEmail {
                    this.email = emailTrim
                    this.password = pwdTrim
                    userMetadata {
                        put("name", namaTrim)
                        if (rekeningTrim.isNotBlank()) put("rekening", rekeningTrim)
                        if (alamatTrim.isNotBlank())   put("alamat",   alamatTrim)
                    }
                    autoConfirm = true
                }

                // 2) Insert ke tabel aplikasi lengkap
                val penggunaBaru = Pengguna(
                    pgnId       = created.id,
                    pgnNama     = namaTrim,
                    pgnEmail    = emailLower,
                    pgnRekening = rekeningTrim.ifBlank { null },
                    pgnAlamat   = alamatTrim.ifBlank { null },
                    pgnSaldo    = saldoParsed // null jika kosong
                )
                client.from("pengguna").insert(penggunaBaru)

                _toast.emit("Pengguna berhasil dibuat")
                _navigateBack.emit(Unit)

            } catch (e: AuthWeakPasswordException) {
                _toast.emit("Password minimal 6 karakter")
            } catch (e: AuthRestException) {
                val msg = e.message?.lowercase().orEmpty()
                val invalidEmail = e.error == "validation_failed" ||
                        msg.contains("unable to validate email address") ||
                        msg.contains("invalid format")
                _toast.emit(if (invalidEmail) "Format email tidak valid" else "Gagal membuat pengguna, periksa koneksi internet")
                Log.e("TambahPenggunaVM", "Auth error: ${e.message}", e)
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
                    filter { ilike("pgnEmail", email.trim()) }
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("TambahPenggunaVM", "Gagal cek email unik: ${e.message}", e)
            false
        }
    }

    private fun parseDecimalOrNull(input: String): java.math.BigDecimal? {
        val s = input.trim()
        if (s.isBlank()) return null

        val hasComma = s.contains(',')
        val hasDot   = s.contains('.')
        val normalized = when {
            hasComma && hasDot -> {
                val lastComma = s.lastIndexOf(',')
                val lastDot   = s.lastIndexOf('.')
                if (lastComma > lastDot) {
                    // 1.234,56 -> remove group dots, replace decimal comma to dot
                    s.replace(".", "").replace(',', '.')
                } else {
                    // 1,234.56 -> remove group commas (already dot decimal)
                    s.replace(",", "")
                }
            }
            hasComma -> s.replace(',', '.')
            else     -> s
        }
        return runCatching { java.math.BigDecimal(normalized) }.getOrNull()
    }
}