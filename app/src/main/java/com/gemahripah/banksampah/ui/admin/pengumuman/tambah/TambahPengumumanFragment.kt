package com.gemahripah.banksampah.ui.admin.pengumuman.tambah

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPengumumanBinding
import com.gemahripah.banksampah.utils.reduceFileImage
import com.gemahripah.banksampah.utils.uriToFile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TambahPengumumanFragment : Fragment() {

    private var _binding: FragmentTambahPengumumanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null

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
        setupViewListeners()
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
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && currentImageUri != null) {
                    updateSelectedImage(currentImageUri!!)
                }
            }
    }

    private fun setupGalleryLauncher() {
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { updateSelectedImage(it) }
            }
    }

    private fun updateSelectedImage(uri: Uri) {
        currentImageUri = uri
        binding.selectedImageView.setImageURI(uri)
        binding.selectedImageView.visibility = View.VISIBLE
        binding.uploadText.visibility = View.GONE
    }

    private fun setupViewListeners() {
        binding.gambarFile.setOnClickListener { showImagePickerDialog() }
        binding.konfirmasi.setOnClickListener { validateAndUploadPengumuman() }
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

    private fun validateAndUploadPengumuman() {
        val judul = binding.nama.text.toString()
        val isi = binding.edtTextarea.text.toString()

        if (judul.isBlank() || isi.isBlank()) {
            showToast("Judul dan isi wajib diisi")
            return
        }

        uploadPengumuman(judul, isi)
    }

    private fun uploadPengumuman(judul: String, isi: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val imageUrl = currentImageUri?.let { uploadImage(it) }

                val pengumuman = com.gemahripah.banksampah.data.model.pengumuman.Pengumuman(
                    pmnJudul = judul,
                    pmnIsi = isi,
                    pmnGambar = imageUrl,
                    pmnIsPublic = true,
                    pmnPin = false
                )

                withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .from("pengumuman")
                        .insert(pengumuman)
                }

                showToast("Pengumuman berhasil ditambahkan")
                findNavController().navigateUp()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal menyimpan: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val file = copyUriToTempFile(uri, requireContext()).reduceFileImage()
            val byteArray = file.readBytes()
            file.delete()

            SupabaseProvider.client.storage
                .from("pengumuman")
                .upload("images/${file.name}", byteArray) {
                    upsert = false
                }

            "https://gxqnvejigdthwlkeshks.supabase.co/storage/v1/object/public/pengumuman/images/${file.name}"
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