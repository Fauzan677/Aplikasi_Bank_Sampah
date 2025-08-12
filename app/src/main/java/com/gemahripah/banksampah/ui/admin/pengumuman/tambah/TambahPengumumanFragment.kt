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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gemahripah.banksampah.databinding.FragmentTambahPengumumanBinding
import com.gemahripah.banksampah.utils.reduceFileImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TambahPengumumanFragment : Fragment() {

    private var _binding: FragmentTambahPengumumanBinding? = null
    private val binding get() = _binding!!

    private val vm: TambahPengumumanViewModel by viewModels()

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
        collectVm()
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.isLoading.collect { showLoading(it) }
                }
                launch {
                    vm.toast.collect { showToast(it) }
                }
                launch {
                    vm.navigateBack.collect { requireActivity().onBackPressedDispatcher.onBackPressed() }
                }
            }
        }
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

    private fun setupViewListeners() {
        binding.gambarFile.setOnClickListener { showImagePickerDialog() }
        binding.gambarFile.setOnLongClickListener {
            showDeleteImageConfirmation()
            true
        }
        binding.konfirmasi.setOnClickListener { validateAndSubmit() }
    }

    private fun showDeleteImageConfirmation() {
        if (currentImageUri == null) {
            showToast("Belum ada gambar yang dipilih")
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Gambar")
            .setMessage("Apakah Anda yakin ingin menghapus gambar yang dipilih?")
            .setPositiveButton("Hapus") { _, _ -> clearSelectedImage() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clearSelectedImage() {
        currentImageUri = null
        binding.selectedImageView.setImageDrawable(null)
        binding.selectedImageView.visibility = View.GONE
        binding.uploadText.visibility = View.VISIBLE
        showToast("Gambar dihapus")
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
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
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
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun updateSelectedImage(uri: Uri) {
        currentImageUri = uri
        binding.selectedImageView.setImageURI(uri)
        binding.selectedImageView.visibility = View.VISIBLE
        binding.uploadText.visibility = View.GONE
    }

    private fun validateAndSubmit() {
        val judul = binding.nama.text.toString()
        val isi = binding.edtTextarea.text.toString()

        if (judul.isBlank() || isi.isBlank()) {
            showToast("Judul dan isi wajib diisi")
            return
        }

        // Siapkan bytes gambar (kalau ada) di background thread
        viewLifecycleOwner.lifecycleScope.launch {
            val imageBytes: ByteArray? = withContext(Dispatchers.IO) {
                currentImageUri?.let { uri ->
                    val file = copyUriToTempFile(uri, requireContext()).reduceFileImage()
                    val bytes = file.readBytes()
                    file.delete()
                    bytes
                }
            }
            vm.submitPengumuman(judul, isi, imageBytes)
        }
    }

    /** Salin Uri ke file sementara (agar mudah dibaca sebagai bytes). */
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
        _binding = null
        super.onDestroyView()
    }
}