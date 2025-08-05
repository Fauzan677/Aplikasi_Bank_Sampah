package com.gemahripah.banksampah.ui.nasabah.profil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.launch

class ProfilViewModel : ViewModel() {

    private val _pengguna = MutableLiveData<Pengguna?>()
    val pengguna: LiveData<Pengguna?> get() = _pengguna

    private val _logoutStatus = MutableLiveData<Boolean>()
    val logoutStatus: LiveData<Boolean> get() = _logoutStatus

    private val _logoutError = MutableLiveData<String?>()
    val logoutError: LiveData<String?> get() = _logoutError

    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseProvider.client.auth.signOut()
                _pengguna.postValue(null)
                _logoutStatus.postValue(true)
            } catch (e: RestException) {
                _logoutError.postValue(e.message ?: "Logout gagal")
            }
        }
    }
}