package com.gemahripah.banksampah.ui.admin.pengumuman.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class EditPengumumanViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    // Loading untuk UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Event UI
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    // State internal (data awal & status gambar)
    private var original: Pengumuman? = null
    private var existingImageUrl: String? = null
    private var imageDeleted: Boolean = false

    fun initFromPengumuman(p: Pengumuman) {
        original = p
        existingImageUrl = p.pmnGambar
        imageDeleted = false
    }

    /** Tandai gambar dihapus (true) atau dibatalkan (false). */
    fun markImageDeleted(deleted: Boolean) {
        imageDeleted = deleted
    }

    fun submitEdit(judul: String, isi: String, newImageBytes: ByteArray?) {
        val current = original
        if (current == null) {
            _toast.tryEmit("Data tidak tersedia")
            return
        }
        if (judul.isBlank() || isi.isBlank()) {
            _toast.tryEmit("Judul dan isi wajib diisi")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                var newUrl = existingImageUrl
                var imageChanged = false
                val bucket = client.storage.from("pengumuman")

                // 1) Hapus gambar jika ditandai
                if (imageDeleted) {
                    deleteImageIfExists(bucket, existingImageUrl)
                    newUrl = null
                    imageChanged = true
                } else if (newImageBytes != null) {
                    // 2) Jika ada gambar baru: upload baru ATAU update file lama
                    newUrl = if (existingImageUrl.isNullOrBlank()) {
                        uploadNewImage(bucket, newImageBytes)
                    } else {
                        updateExistingImage(bucket, existingImageUrl!!, newImageBytes)
                    }
                    imageChanged = true
                }

                // 3) Tentukan apakah ada perubahan data teks/gambar
                val needsUpdate = (current.pmnJudul != judul) ||
                        (current.pmnIsi != isi) ||
                        imageChanged

                if (!needsUpdate) {
                    _toast.emit("Tidak ada perubahan data")
                    _isLoading.value = false
                    return@launch
                }

                // 4) Update row
                val now = OffsetDateTime.now()
                client.from("pengumuman").update({
                    set("updated_at", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSXXX")))
                    set("pmnJudul", judul)
                    set("pmnIsi", isi)
                    set("pmnGambar", newUrl)
                    set("pmnIsPublic", current.pmnIsPublic ?: true)
                    set("pmnPin", current.pmnPin ?: false)
                }) {
                    filter { current.pmnId?.let { eq("pmnId", it) } }
                }

                // simpan state baru ke memori
                existingImageUrl = newUrl
                original = current.copy(
                    pmnJudul = judul,
                    pmnIsi = isi,
                    pmnGambar = newUrl
                )
                imageDeleted = false

                _toast.emit("Pengumuman berhasil diupdate")
                _navigateBack.emit(Unit)

            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                _toast.emit("Gagal menyimpan, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Storage helpers ---

    private suspend fun deleteImageIfExists(bucket: BucketApi, url: String?) {
        if (url.isNullOrBlank()) return
        val key = urlToStorageKey(url) ?: return
        try {
            bucket.delete(key)
        } catch (_: Exception) {
            // tidak fatal
        }
    }

    private suspend fun uploadNewImage(bucket: BucketApi, bytes: ByteArray): String {
        val fileName = "images/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        bucket.upload(fileName, bytes) { upsert = false }
        return bucket.publicUrl(fileName)
    }

    private suspend fun updateExistingImage(bucket: BucketApi, existingUrl: String, bytes: ByteArray): String {
        val key = urlToStorageKey(existingUrl) ?: return uploadNewImage(bucket, bytes) // fallback
        bucket.update(key, bytes) { upsert = false }
        return bucket.publicUrl(key)
    }

    private fun urlToStorageKey(url: String): String? {
        // hapus query param (?v=...)
        val clean = url.substringBefore('?')
        val lastSlash = clean.lastIndexOf('/')
        if (lastSlash == -1) return null
        val fileNameEncoded = clean.substring(lastSlash + 1)
        val fileName = URLDecoder.decode(fileNameEncoded, StandardCharsets.UTF_8.name())
        return "images/$fileName"
    }
}