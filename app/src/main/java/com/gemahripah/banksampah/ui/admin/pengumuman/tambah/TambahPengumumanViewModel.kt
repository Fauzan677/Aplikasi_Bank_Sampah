package com.gemahripah.banksampah.ui.admin.pengumuman.tambah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class TambahPengumumanViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // Loading untuk UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Event UI
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    // Sukses -> suruh Fragment kembali
    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    /** Submit pengumuman. `imageBytes` opsional (boleh null jika tanpa gambar). */
    fun submitPengumuman(
        judul: String,
        isi: String,
        imageBytes: ByteArray?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val imageUrl: String? = if (imageBytes != null) {
                    uploadImageToSupabase(imageBytes)
                } else null

                val pengumuman = Pengumuman(
                    pmnJudul = judul,
                    pmnIsi = isi,
                    pmnGambar = imageUrl,
                    pmnIsPublic = true,
                    pmnPin = false
                )

                client.from("pengumuman").insert(pengumuman)

                _toast.emit("Pengumuman berhasil ditambahkan")
                _navigateBack.emit(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toast.emit("Gagal menyimpan, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Upload ke Supabase Storage dan balikin public URL. */
    private suspend fun uploadImageToSupabase(bytes: ByteArray): String {
        val folder = "images"
        val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val path = "$folder/$fileName"

        client.storage
            .from("pengumuman")
            .upload(path, bytes) {
                upsert = false
            }

        return client.storage
            .from("pengumuman")
            .publicUrl(path)
    }
}