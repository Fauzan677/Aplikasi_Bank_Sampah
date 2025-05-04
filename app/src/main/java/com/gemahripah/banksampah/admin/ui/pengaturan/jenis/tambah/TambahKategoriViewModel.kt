package com.gemahripah.banksampah.admin.ui.pengaturan.jenis.tambah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class TambahKategoriViewModel : ViewModel() {

    fun tambahKategori(namaKategori: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val supabase = SupabaseProvider.client

                supabase.postgrest["kategori"].insert(
                    mapOf("nama" to namaKategori)
                )

                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

}