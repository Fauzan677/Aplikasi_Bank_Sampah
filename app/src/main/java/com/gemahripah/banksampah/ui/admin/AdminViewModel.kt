package com.gemahripah.banksampah.ui.admin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _pengguna = MutableLiveData<Pengguna?>()
    val pengguna: LiveData<Pengguna?> get() = _pengguna

    fun setPengguna(data: Pengguna?) {
        // Selalu post supaya observer terpanggil (berhentiin swipe juga)
        _pengguna.postValue(data)
    }

    fun loadPenggunaById(pgnId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fresh: Pengguna? = client
                    .from("pengguna")
                    .select {
                        filter { eq("pgnId", pgnId) }
                        limit(1)
                    }
                    .decodeSingleOrNull()

                // Selalu post walau sama -> observer di Fragment tetap jalan
                _pengguna.postValue(fresh)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Gagal load pengguna: ${e.message}", e)
                // Trigger observer juga agar swipe berhenti walau error
                _pengguna.postValue(_pengguna.value)
            }
        }
    }
}