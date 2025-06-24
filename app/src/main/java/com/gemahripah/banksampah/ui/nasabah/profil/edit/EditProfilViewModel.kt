package com.gemahripah.banksampah.ui.nasabah.profil.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfilViewModel : ViewModel() {

    private var userId: String? = null
    private var emailLama: String? = null

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> get() = _toastMessage

    private val _successUpdate = MutableLiveData<Boolean>()
    val successUpdate: LiveData<Boolean> get() = _successUpdate

    fun setInitialData(pengguna: Pengguna) {
        userId = pengguna.pgnId
        emailLama = pengguna.pgnEmail
    }

    fun updateProfil(namaBaru: String, emailBaru: String, passwordBaru: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    SupabaseProvider.client.from("pengguna").update({
                        set("pgnNama", namaBaru)
                        set("pgnEmail", emailBaru)
                    }) {
                        filter {
                            userId?.let { eq("pgnId", it) }
                        }
                    }

                    if (emailBaru != emailLama) {
                        try {
                            SupabaseProvider.client.auth.updateUser {
                                email = emailBaru
                            }
                            _toastMessage.postValue("Kami telah mengirimkan tautan verifikasi ke email baru Anda")
                        } catch (e: Exception) {
                            _toastMessage.postValue("Gagal update email auth: ${e.message}")
                        }
                    }

                    if (passwordBaru.isNotEmpty()) {
                        try {
                            SupabaseProvider.client.auth.updateUser {
                                password = passwordBaru
                            }
                        } catch (e: Exception) {
                            _toastMessage.postValue("Gagal update password: ${e.message}")
                        }
                    }

                    _toastMessage.postValue("Data pengguna berhasil diperbarui")
                    _successUpdate.postValue(true)
                } catch (e: Exception) {
                    _toastMessage.postValue("Terjadi kesalahan: ${e.message}")
                }
            }
        }
    }
}