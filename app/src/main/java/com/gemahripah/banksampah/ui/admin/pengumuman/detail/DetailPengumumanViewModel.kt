package com.gemahripah.banksampah.ui.admin.pengumuman.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailPengumumanViewModel : ViewModel() {

    private val _pengumuman = MutableLiveData<Pengumuman>()
    val pengumuman: LiveData<Pengumuman> get() = _pengumuman

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val client = SupabaseProvider.client

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun setPengumuman(pengumuman: Pengumuman) {
        _pengumuman.value = pengumuman
    }

    fun hapusPengumuman(id: Long, gambarUrl: String?, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                hapusDataPengumuman(id)
                hapusGambarDariStorage(gambarUrl)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("HapusPengumuman", "Terjadi error: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun hapusDataPengumuman(id: Long) {
        client.from("pengumuman").delete {
            filter { eq("pmnId", id) }
        }
    }

    private suspend fun hapusGambarDariStorage(url: String?) {
        if (url.isNullOrBlank()) return

        val fileName = url.substringAfterLast("/")
        val path = "images/$fileName"
        try {
            client.storage.from("pengumuman").delete(path)
        } catch (e: Exception) {
            Log.e("HapusGambar", "Gagal hapus gambar: ${e.message}")
        }
    }
}