package com.gemahripah.banksampah.ui

import androidx.lifecycle.*
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class MainViewModel : ViewModel() {

    private val supabase = SupabaseProvider.client

    private val _pengguna = MutableLiveData<Pengguna?>()
    val pengguna: LiveData<Pengguna?> = _pengguna

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun checkSession() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Muat session dari storage (jika ada)
                supabase.auth.loadFromStorage()

                val session = supabase.auth.currentSessionOrNull()
                val user = session?.user
                val isValid = session != null && session.expiresAt > Clock.System.now()

                if (isValid && user != null) {
                    val penggunaResult = supabase.postgrest
                        .from("pengguna")
                        .select {
                            filter { eq("pgnId", user.id) }
                            limit(1)
                        }
                        .decodeSingleOrNull<Pengguna>()

                    if (penggunaResult != null) {
                        _pengguna.postValue(penggunaResult)
                    } else {
                        // Session valid tapi data user tidak ada → clear session
                        supabase.auth.clearSession()
                        _error.postValue("Data pengguna tidak ditemukan")
                        _pengguna.postValue(null)
                    }
                } else {
                    // Session tidak valid / tidak ada
                    supabase.auth.clearSession()
                    _pengguna.postValue(null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Pada error jaringan, tetap clear session agar alur ke layar login konsisten
                supabase.auth.clearSession()
                _pengguna.postValue(null)
                _error.postValue("Terjadi kesalahan: ${e.localizedMessage ?: "Tidak diketahui"}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
