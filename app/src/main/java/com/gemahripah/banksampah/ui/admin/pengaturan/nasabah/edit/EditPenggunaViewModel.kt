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
import java.math.BigDecimal
import java.math.RoundingMode
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

        val isNamaBerubah     = namaTrim   != (current.pgnNama ?: "")
        val isEmailBerubah    = emailTrim  != (current.pgnEmail ?: "")
        val isPasswordBerubah = pwdTrim.isNotEmpty()
        val isRekBerubah      = rekTrim    != (current.pgnRekening ?: "")
        val isAlamatBerubah   = alamatTrim != (current.pgnAlamat ?: "")

        val parsed = if (saldoInputBaru.isBlank()) {
            BigDecimal.ZERO
        } else {
            parseDecimalOrNull(saldoInputBaru) ?: run {
                _toast.tryEmit("Saldo tidak valid")
                return
            }
        }

        val targetSaldo = parsed.setScale(2, RoundingMode.HALF_UP)
        if (targetSaldo < BigDecimal.ZERO) {
            _toast.tryEmit("Saldo tidak boleh negatif")
            return
        }

        val currentSaldo = (current.pgnSaldo ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        val isSaldoBerubah = currentSaldo.compareTo(targetSaldo) != 0

        if (!isNamaBerubah && !isEmailBerubah && !isPasswordBerubah &&
            !isRekBerubah && !isAlamatBerubah && !isSaldoBerubah) {
            _toast.tryEmit("Tidak ada perubahan data")
            return
        }

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
                    if (isRekBerubah)    set("pgnRekening", rekTrim.ifBlank { null })
                    if (isAlamatBerubah) set("pgnAlamat", alamatTrim.ifBlank { null })
                    if (isSaldoBerubah)  set("pgnSaldo", targetSaldo)   // ← pakai targetSaldo
                }) {
                    filter { eq("pgnId", current.pgnId!!) }
                }

                original = current.copy(
                    pgnNama     = namaTrim,
                    pgnEmail    = emailTrim,
                    pgnRekening = rekTrim.ifBlank { null },
                    pgnAlamat   = alamatTrim.ifBlank { null },
                    pgnSaldo    = if (isSaldoBerubah) targetSaldo else current.pgnSaldo
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

    private fun parseDecimalOrNull(input: String): BigDecimal? {
        val s = input.trim()
        if (s.isBlank()) return null
        val hasComma = s.contains(',')
        val hasDot   = s.contains('.')
        val normalized = when {
            hasComma && hasDot -> {
                val lastComma = s.lastIndexOf(',')
                val lastDot   = s.lastIndexOf('.')
                if (lastComma > lastDot) {
                    s.replace(".", "").replace(',', '.')
                } else {
                    s.replace(",", "")
                }
            }
            hasComma -> s.replace(',', '.')
            else     -> s
        }
        return runCatching { BigDecimal(normalized) }.getOrNull()
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