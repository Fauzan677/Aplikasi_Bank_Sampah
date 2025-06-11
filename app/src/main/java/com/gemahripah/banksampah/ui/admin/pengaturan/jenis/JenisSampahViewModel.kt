package com.gemahripah.banksampah.ui.admin.pengaturan.jenis

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class JenisSampahViewModel : ViewModel() {

    private val _kategoriList = MutableLiveData<List<Kategori>>()
    val kategoriList: LiveData<List<Kategori>> get() = _kategoriList

    fun loadKategori() {
        viewModelScope.launch {
            try {
                val response = SupabaseProvider.client
                    .from("kategori")
                    .select()
                    .decodeList<Kategori>()

                _kategoriList.postValue(response)
            } catch (e: Exception) {
                _kategoriList.postValue(emptyList())
                Log.e("JenisSampahViewModel", "Gagal memuat kategori", e)
            }
        }
    }
}