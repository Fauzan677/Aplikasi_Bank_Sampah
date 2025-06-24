package com.gemahripah.banksampah.ui.nasabah.pengumuman

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PengumumanViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _pengumumanList = MutableLiveData<List<Pengumuman>>()
    val pengumumanList: LiveData<List<Pengumuman>> = _pengumumanList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPengumuman() {
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = client
                    .from("pengumuman")
                    .select()
                    .decodeList<Pengumuman>()

                _pengumumanList.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _pengumumanList.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
