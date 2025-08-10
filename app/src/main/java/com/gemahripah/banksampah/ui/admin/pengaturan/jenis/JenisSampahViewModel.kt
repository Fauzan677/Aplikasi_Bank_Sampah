package com.gemahripah.banksampah.ui.admin.pengaturan.jenis

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JenisSampahViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    private val _kategoriList = MutableLiveData<List<Kategori>>()
    val kategoriList: LiveData<List<Kategori>> get() = _kategoriList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadKategori() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val response = client
                    .from("kategori")
                    .select()
                    .decodeList<Kategori>()
                _kategoriList.postValue(response)
            } catch (e: Exception) {
                _kategoriList.postValue(emptyList())
                Log.e("JenisSampahVM", "Gagal memuat kategori", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
