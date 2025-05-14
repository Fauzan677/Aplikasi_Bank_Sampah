package com.gemahripah.banksampah.ui.admin.pengaturan.jenis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class JenisSampahViewModel : ViewModel() {

    private val _kategoriList = MutableLiveData<List<Kategori>>()
    val kategoriList: LiveData<List<Kategori>> = _kategoriList

    fun loadKategori() {
        viewModelScope.launch {
            try {
                // Melakukan query SELECT dan mendekode hasilnya ke dalam List<Kategori>
                val response = SupabaseProvider.client
                    .from("kategori")
                    .select()
                    .decodeList<Kategori>()

                println("Jumlah data kategori yang diterima: ${response.size}")  // Tambahkan ini


                // Logging jumlah kategori yang diterima
                println("Jumlah kategori yang diterima: ${response.size}")

                // Posting data kategori ke LiveData
                _kategoriList.postValue(response)
            } catch (e: Exception) {
                // Menangani error jika ada
                _kategoriList.postValue(emptyList())
                println("Error: ${e.message}")
            }
        }
    }
}