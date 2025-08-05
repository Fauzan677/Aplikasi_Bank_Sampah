package com.gemahripah.banksampah.ui.admin.pengumuman.edit

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPengumumanBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.reduceFileImage
import com.gemahripah.banksampah.utils.uriToFile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class EditPengumumanFragment : Fragment() {

    private var _binding: FragmentTambahPengumumanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null

    private var existingImageUrl: String? = null
    private var gambarDihapus: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissionLauncher()
        setupCameraLauncher()
        setupGalleryLauncher()

        binding.judul.text = "Edit Pengumuman"

        binding.gambarFile.setOnClickListener {
            showImagePickerDialog()
        }

        val pengumuman = arguments?.let {
            EditPengumumanFragmentArgs.fromBundle(it).pengumuman
        }

        pengumuman?.let {
            loadPengumumanData(it)
            binding.konfirmasi.setOnClickListener {
                editPengumuman(pengumuman)
            }
        }
    }

    private fun loadPengumumanData(pengumuman: Pengumuman) {
        binding.nama.setText(pengumuman.pmnJudul)
        binding.edtTextarea.setText(pengumuman.pmnIsi)

        existingImageUrl = pengumuman.pmnGambar

        if (!pengumuman.pmnGambar.isNullOrEmpty()) {
            binding.selectedImageView.visibility = View.VISIBLE
            binding.uploadText.visibility = View.GONE
            Glide.with(requireContext())
                .load("${pengumuman.pmnGambar}?v=${pengumuman.updated_at}")
                .into(binding.selectedImageView)
        }

        binding.selectedImageView.setOnLongClickListener {
            showDeleteImageConfirmation()
            true
        }
    }

    private fun showDeleteImageConfirmation() {
        if (currentImageUri == null && existingImageUrl.isNullOrEmpty()) {
            showToast("Belum ada gambar yang dipilih")
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Gambar")
            .setMessage("Apakah Anda yakin ingin menghapus gambar yang dipilih?")
            .setPositiveButton("Hapus") { _, _ ->
                clearSelectedImage()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clearSelectedImage() {
        binding.selectedImageView.setImageDrawable(null)
        binding.selectedImageView.visibility = View.GONE
        binding.uploadText.visibility = View.VISIBLE

        gambarDihapus = true
        currentImageUri = null
        existingImageUrl = null

        showToast("Gambar dihapus")
    }

    private fun setupPermissionLauncher() {
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) launchCamera()
            else showToast("Izin kamera dibutuhkan")
        }
    }

    private fun setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                updateSelectedImage(currentImageUri!!)
            }
        }
    }

    private fun setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { updateSelectedImage(it) }
        }
    }

    private fun updateSelectedImage(uri: Uri) {
        currentImageUri = uri
        binding.selectedImageView.setImageURI(uri)
        binding.selectedImageView.visibility = View.VISIBLE
        binding.uploadText.visibility = View.GONE
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val context = requireContext()
        val imageFile = File.createTempFile("IMG_", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        currentImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun openGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun editPengumuman(pengumuman: Pengumuman) {
        val judul = binding.nama.text.toString()
        val isi = binding.edtTextarea.text.toString()

        if (judul.isBlank() || isi.isBlank()) {
            showToast("Judul dan isi wajib diisi")
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            val supabase = SupabaseProvider.client
            try {
                var imageUrl = existingImageUrl
                var imageChanged = false

                if (gambarDihapus) {
                    hapusGambarDariStorage(supabase, existingImageUrl)
                    imageUrl = null
                    imageChanged = true
                } else if (currentImageUri != null) {
                    val result = handleImageUpload(currentImageUri, existingImageUrl)
                    imageUrl = result.first
                    imageChanged = result.second
                }

                val needsUpdate = shouldUpdatePengumuman(judul, isi, imageChanged, pengumuman)

                if (needsUpdate) {
                    updatePengumumanData(pengumuman, judul, isi, imageUrl)
                    showToast("Pengumuman berhasil diupdate")
                } else {
                    showToast("Tidak ada perubahan yang disimpan")
                }

                showLoading(false)
                findNavController().navigate(R.id.action_editPengumumanFragment_to_navigation_pengumuman)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal menyimpan, periksa koneksi internet")
                showLoading(false)
            }
        }
    }

    private suspend fun hapusGambarDariStorage(supabase: SupabaseClient, url: String?) {
        if (url.isNullOrBlank()) return

        val fileName = url.substringAfterLast("/")
        val path = "images/$fileName"
        val bucket = supabase.storage.from("pengumuman")

        try {
            bucket.delete(path)
        } catch (e: Exception) {
            Log.e("HapusGambar", "Gagal hapus gambar: ${e.message}")
        }
    }

    private suspend fun handleImageUpload(
        uri: Uri?,
        existingUrl: String?
    ): Pair<String?, Boolean> {
        val supabase = SupabaseProvider.client
        if (uri == null) return Pair(existingUrl, false)

        val file = copyUriToTempFile(uri, requireContext()).reduceFileImage()
        val byteArray = file.readBytes()
        file.delete()

        val bucket = supabase.storage.from("pengumuman")
        return try {
            val imageUrl = if (existingUrl.isNullOrEmpty()) {
                val fileName = "images/${file.name}"
                bucket.upload(fileName, byteArray) { upsert = false }
                bucket.publicUrl(fileName)
            } else {
                val fileName = existingUrl.substringAfterLast("/")
                bucket.update("images/$fileName", byteArray) { upsert = false }
                bucket.publicUrl("images/$fileName")
            }
            Pair(imageUrl, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(existingUrl, false)
        }
    }

    private fun shouldUpdatePengumuman(
        judul: String,
        isi: String,
        imageChanged: Boolean,
        pengumuman: Pengumuman
    ): Boolean {
        return judul != pengumuman.pmnJudul ||
                isi != pengumuman.pmnIsi ||
                imageChanged
    }

    private suspend fun updatePengumumanData(
        pengumuman: Pengumuman,
        judul: String,
        isi: String,
        imageUrl: String?
    ) {
        val supabase = SupabaseProvider.client
        supabase.from("pengumuman").update({
            set("updated_at", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSXXX")))
            set("pmnJudul", judul)
            set("pmnIsi", isi)
            set("pmnGambar", imageUrl)
            set("pmnIsPublic", true)
            set("pmnPin", pengumuman.pmnPin)
        }) {
            filter {
                pengumuman.pmnId?.let { eq("pmnId", it) }
            }
        }
    }

    private fun copyUriToTempFile(uri: Uri, context: Context): File {
        val file = File.createTempFile("selected_image", ".jpg", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }
        return file
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}