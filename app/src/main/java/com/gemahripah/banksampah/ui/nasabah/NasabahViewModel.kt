package com.gemahripah.banksampah.ui.nasabah

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NasabahViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _pengguna = MutableLiveData<Pengguna?>()
    val pengguna: LiveData<Pengguna?> = _pengguna

    fun setPengguna(pengguna: Pengguna?) {
        // selalu set supaya observer terpanggil dan swipe berhenti
        _pengguna.postValue(pengguna)
    }

    fun loadPenggunaById(pgnId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fresh: Pengguna? = client.postgrest
                    .from("pengguna")
                    .select {
                        filter { eq("pgnId", pgnId) }
                        limit(1)
                    }
                    .decodeSingleOrNull()

                // selalu post, walau sama -> observer di Fragment akan stop animasi
                _pengguna.postValue(fresh)
            } catch (e: Exception) {
                Log.e("NasabahViewModel", "Gagal load pengguna: ${e.message}", e)
                // tetap hentikan swipe walau error dengan memicu observer:
                _pengguna.postValue(_pengguna.value)
            }
        }
    }
}