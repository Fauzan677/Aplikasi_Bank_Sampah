package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.edit

import android.util.Log
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
import kotlinx.serialization.json.buildJsonObject
import org.apache.logging.log4j.CloseableThreadContext.put
import java.util.Locale

class EditPenggunaViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client
    private val admin get() = SupabaseAdminProvider.client

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private var original: Pengguna? = null

    fun init(pengguna: Pengguna) {
        original = pengguna
    }

    fun submitEdit(
        namaBaru: String,
        emailBaru: String,
        passwordBaru: String,
        rekeningBaru: String?,
        alamatBaru: String?,
        saldoInputBaru: String
    ) {
        val current = original
        if (current == null) {
            _toast.tryEmit("Data pengguna tidak tersedia")
            return
        }

        val namaTrim   = namaBaru.trim()
        val emailTrim  = emailBaru.trim()
        val emailLower = emailTrim.lowercase(Locale.ROOT)
        val pwdTrim    = passwordBaru.trim()
        val rekTrim    = rekeningBaru?.trim().orEmpty()
        val alamatTrim = alamatBaru?.trim().orEmpty()

        if (namaTrim.isEmpty() || emailTrim.isEmpty()) {
            _toast.tryEmit("Nama dan Email tidak boleh kosong")
            return
        }

        // Apakah ada perubahan?
        val isNamaBerubah     = namaTrim   != (current.pgnNama ?: "")
        val isEmailBerubah    = emailTrim  != (current.pgnEmail ?: "")
        val isPasswordBerubah = pwdTrim.isNotEmpty()
        val isRekBerubah      = rekTrim    != (current.pgnRekening ?: "")
        val isAlamatBerubah   = alamatTrim != (current.pgnAlamat ?: "")

        // Saldo: kosong = tidak diubah. Kalau diisi -> parse & bandingkan.
        val saldoParsedBaru = parseDecimalOrNull(saldoInputBaru)
        if (saldoInputBaru.isNotBlank() && saldoParsedBaru == null) {
            _toast.tryEmit("Saldo tidak valid")
            return
        }
        val isSaldoBerubah = when {
            saldoInputBaru.isBlank() -> false // user tidak mengubah saldo
            current.pgnSaldo == null && saldoParsedBaru != null -> true
            current.pgnSaldo != null && saldoParsedBaru == null -> true
            else -> current.pgnSaldo?.compareTo(saldoParsedBaru) != 0
        }

        if (!isNamaBerubah && !isEmailBerubah && !isPasswordBerubah &&
            !isRekBerubah && !isAlamatBerubah && !isSaldoBerubah) {
            _toast.tryEmit("Tidak ada perubahan data")
            return
        }

        // Validasi format bila perlu
        if (isEmailBerubah) {
            val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()
            if (!emailOk) {
                _toast.tryEmit("Format email tidak valid")
                return
            }
        }
        if (isPasswordBerubah && pwdTrim.length < 6) {
            _toast.tryEmit("Password minimal 6 karakter")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Cek unik saat berubah (exclude diri sendiri)
                if (isNamaBerubah && isNamaDipakai(namaTrim, current.pgnId!!)) {
                    _toast.emit("Nama sudah digunakan, gunakan nama lain")
                    _isLoading.value = false; return@launch
                }
                if (isEmailBerubah && isEmailDipakai(emailLower, current.pgnId!!)) {
                    _toast.emit("Email sudah digunakan, gunakan email lain")
                    _isLoading.value = false; return@launch
                }

                // 1) Update Auth (email/password) bila perlu
                // 1) Update Auth (email/password) bila perlu
                if (isEmailBerubah || isPasswordBerubah) {
                    try {
                        admin.auth.admin.updateUserById(uid = current.pgnId!!) {
                            if (isEmailBerubah) email = emailLower
                            if (isPasswordBerubah) password = pwdTrim
                            userMetadata = buildJsonObject {
                                put("name", namaTrim)
                                if (rekTrim.isNotBlank()) put("rekening", rekTrim)
                                if (alamatTrim.isNotBlank()) put("alamat", alamatTrim)
                            }
                        }
                    } catch (e: AuthWeakPasswordException) {
                        _toast.emit("Password minimal 6 karakter")
                        _isLoading.value = false; return@launch
                    } catch (e: AuthRestException) {
                        val msg = e.message?.lowercase().orElseEmpty()
                        val invalidEmail =
                            e.error == "validation_failed" ||
                                    msg.contains("unable to validate email address") ||
                                    msg.contains("invalid format")
                        Log.e("EditPenggunaVM", "Admin API error: ${e.message}", e)
                        _toast.emit(if (invalidEmail) "Format email tidak valid" else "Gagal memperbarui email atau password")
                        _isLoading.value = false; return@launch
                    }
                }

                // 2) Update tabel aplikasi "pengguna" hanya pada field yang berubah
                client.from("pengguna").update({
                    if (isNamaBerubah)   set("pgnNama", namaTrim)
                    if (isEmailBerubah)  set("pgnEmail", emailLower)
                    if (isRekBerubah)    set("pgnRekening", if (rekTrim.isBlank()) null else rekTrim)
                    if (isAlamatBerubah) set("pgnAlamat",   if (alamatTrim.isBlank()) null else alamatTrim)
                    if (isSaldoBerubah)  set("pgnSaldo",    saldoParsedBaru) // boleh null / BigDecimal
                }) {
                    filter { eq("pgnId", current.pgnId!!) }
                }

                // Simpan state baru untuk VM
                original = current.copy(
                    pgnNama     = namaTrim,
                    pgnEmail    = emailTrim,
                    pgnRekening = if (rekTrim.isBlank()) null else rekTrim,
                    pgnAlamat   = if (alamatTrim.isBlank()) null else alamatTrim,
                    pgnSaldo    = if (isSaldoBerubah) saldoParsedBaru else current.pgnSaldo
                )

                _toast.emit("Pengguna berhasil diperbarui")
                _navigateBack.emit(Unit)

            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("EditPenggunaVM", "Gagal update: ${e.message}", e)
                _toast.emit("Gagal memperbarui pengguna, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
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
                    // 1.234,56 -> hapus titik pemisah ribuan, ganti koma ke titik
                    s.replace(".", "").replace(',', '.')
                } else {
                    // 1,234.56 -> hapus koma pemisah ribuan
                    s.replace(",", "")
                }
            }
            hasComma -> s.replace(',', '.')
            else     -> s
        }
        return runCatching { java.math.BigDecimal(normalized) }.getOrNull()
    }

    fun deleteUser() {
        val current = original
        if (current?.pgnId.isNullOrBlank()) {
            _toast.tryEmit("Data pengguna tidak tersedia")
            return
        }
        val id = current!!.pgnId!!

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // (opsional) bisa Auth dulu, lalu tabel — tergantung kebijakan bisnis
                client.from("pengguna").delete {
                    filter { eq("pgnId", id) }
                }
                admin.auth.admin.deleteUser(uid = id)

                _toast.emit("Pengguna berhasil dihapus")
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                Log.e("EditPenggunaVM", "Gagal hapus: ${e.message}", e)
                _toast.emit("Gagal menghapus pengguna, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Cek unik (case-insensitive) dengan ilike + exclude current ID ---

    private suspend fun isNamaDipakai(nama: String, excludeId: String): Boolean {
        return try {
            client
                .from("pengguna")
                .select(columns = Columns.list("pgnId")) {
                    filter {
                        ilike("pgnNama", nama) // exact (case-insensitive)
                        neq("pgnId", excludeId)
                    }
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("EditPenggunaVM", "Cek nama unik gagal: ${e.message}", e)
            false
        }
    }

    private suspend fun isEmailDipakai(email: String, excludeId: String): Boolean {
        return try {
            client
                .from("pengguna")
                .select(columns = Columns.list("pgnId")) {
                    filter {
                        ilike("pgnEmail", email.trim())   // exact match
                        neq("pgnId", excludeId)        // exclude diri sendiri
                    }
                }
                .decodeList<Pengguna>()
                .isNotEmpty()
        } catch (e: Exception) {
            Log.e("EditPenggunaVM", "Cek email unik gagal: ${e.message}", e)
            false
        }
    }
}

private fun String?.orElseEmpty(): String = this ?: ""