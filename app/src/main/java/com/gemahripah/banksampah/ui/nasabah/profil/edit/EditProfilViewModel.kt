package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfilViewModel : ViewModel() {

    private var userId: String? = null
    private var namaLama: String? = null

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> get() = _toastMessage

    private val _successUpdate = MutableLiveData<Boolean>()
    val successUpdate: LiveData<Boolean> get() = _successUpdate

    fun setInitialData(pengguna: Pengguna) {
        userId = pengguna.pgnId
        namaLama = pengguna.pgnNama

    }

    private suspend fun isNamaDipakai(nama: String): Boolean {
        return try {
            val normalizedNama = nama.trim()
            val response = SupabaseProvider.client
                .from("pengguna")
                .select(columns = Columns.list("pgnNama")) {
                    filter {
                        eq("pgnNama", normalizedNama)
                    }
                }
                .decodeList<Pengguna>()

            Log.d("EditProfilViewModel", "Cek nama dipakai: $response")
            response.isNotEmpty()
        } catch (e: Exception) {
            Log.e("EditProfilViewModel", "Gagal cek nama unik: ${e.message}", e)
            false
        }
    }

    fun updateProfil(namaBaru: String, passwordBaru: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            withContext(Dispatchers.IO) {
                try {
                    val tidakAdaYangDiubah = namaBaru == namaLama &&
                            passwordBaru.isEmpty()

                    if (tidakAdaYangDiubah) {
                        _toastMessage.postValue("Tidak ada perubahan yang dilakukan")
                        _isLoading.postValue(false)
                        return@withContext
                    }

                    if (namaBaru != namaLama && isNamaDipakai(namaBaru)) {
                        _toastMessage.postValue("Nama sudah digunakan, gunakan nama lain")
                        _isLoading.postValue(false)
                        return@withContext
                    }

                    SupabaseProvider.client.from("pengguna").update({
                        set("pgnNama", namaBaru)
                    }) {
                        filter {
                            userId?.let { eq("pgnId", it) }
                        }
                    }

                    if (passwordBaru.isNotEmpty()) {
                        try {
                            SupabaseProvider.client.auth.updateUser {
                                password = passwordBaru
                            }
                        } catch (e: Exception) {
                            Log.e("EditProfilViewModel", "Gagal update password: ${e.message}", e)

                            val msg = e.message?.lowercase() ?: ""
                            if (msg.contains("weak_password")) {
                                _toastMessage.postValue("Password minimal 6 karakter")
                            } else {
                                _toastMessage.postValue("Gagal memperbarui password")
                            }

                            _isLoading.postValue(false)
                            return@withContext
                        }
                    }

                    _toastMessage.postValue("Data pengguna berhasil diperbarui")
                    _successUpdate.postValue(true)
                } catch (e: Exception) {
                    Log.e("EditProfilViewModel", "Gagal update profil: ${e.message}", e)
                    _toastMessage.postValue("Gagal memperbarui data, periksa koneksi internet")
                } finally {
                    _isLoading.postValue(false)
                }
            }
        }
    }
}