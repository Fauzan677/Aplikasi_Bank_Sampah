package com.gemahripah.banksampah.ui.admin.pengumuman.edit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.databinding.FragmentTambahPengumumanBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import com.gemahripah.banksampah.utils.reduceFileImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EditPengumumanFragment : Fragment(), Reloadable {

    private var _binding: FragmentTambahPengumumanBinding? = null
    private val binding get() = _binding!!

    private val vm: EditPengumumanViewModel by viewModels()

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null

    private var existingImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissionLauncher()
        setupCameraLauncher()
        setupGalleryLauncher()

        binding.judul.text = "Edit Pengumuman"

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val pengumuman = arguments?.let {
            EditPengumumanFragmentArgs.fromBundle(it).pengumuman
        }
        if (pengumuman == null) {
            toast("Data tidak ditemukan")
            findNavController().navigateUp()
            return
        }

        // Prefill UI
        loadPengumumanData(pengumuman)
        // Init VM state
        vm.initFromPengumuman(pengumuman)

        // Actions
        binding.gambarFile.setOnClickListener { showImagePickerDialog() }
        binding.selectedImageView.setOnLongClickListener {
            showDeleteImageConfirmation()
            true
        }
        binding.konfirmasi.setOnClickListener { validateAndSubmit() }

        collectVm()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return

        if (currentImageUri == null && !existingImageUrl.isNullOrEmpty()) {
            binding.selectedImageView.visibility = View.VISIBLE
            binding.uploadText.visibility = View.GONE
            Glide.with(requireContext())
                .load("${existingImageUrl}?v=${System.currentTimeMillis()}")
                .into(binding.selectedImageView)
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.isLoading.collect { showLoading(it) } }
                launch { vm.toast.collect { toast(it) } }

                launch {
                    vm.navigateBack.collect {
                        findNavController().popBackStack(R.id.navigation_pengumuman, false)
                    }
                }
            }
        }
    }

    private fun loadPengumumanData(p: Pengumuman) {
        binding.nama.setText(p.pmnJudul)
        binding.edtTextarea.setText(p.pmnIsi)

        existingImageUrl = p.pmnGambar
        if (!existingImageUrl.isNullOrEmpty()) {
            binding.selectedImageView.visibility = View.VISIBLE
            binding.uploadText.visibility = View.GONE

            if (!updateInternetCard()) return
            Glide.with(requireContext())
                .load("${existingImageUrl}?v=${p.updated_at}")
                .into(binding.selectedImageView)
        } else {
            binding.selectedImageView.visibility = View.GONE
            binding.uploadText.visibility = View.VISIBLE
        }
    }

    private fun showDeleteImageConfirmation() {
        if (currentImageUri == null && existingImageUrl.isNullOrEmpty()) {
            toast("Belum ada gambar yang dipilih")
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Gambar")
            .setMessage("Apakah Anda yakin ingin menghapus gambar yang dipilih?")
            .setPositiveButton("Hapus") { _, _ ->
                clearSelectedImage()
                vm.markImageDeleted(true)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clearSelectedImage() {
        currentImageUri = null
        existingImageUrl = null
        binding.selectedImageView.setImageDrawable(null)
        binding.selectedImageView.visibility = View.GONE
        binding.uploadText.visibility = View.VISIBLE
        toast("Gambar dihapus")
    }

    // --- Activity Result setup ---

    private fun setupPermissionLauncher() {
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) launchCamera() else toast("Izin kamera dibutuhkan")
        }
    }

    private fun setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && currentImageUri != null) {
                updateSelectedImage(currentImageUri!!)
            }
        }
    }

    private fun setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let { updateSelectedImage(it) }
        }
    }

    private fun updateSelectedImage(uri: Uri) {
        vm.markImageDeleted(false) // ada gambar baru, batal status "hapus"
        currentImageUri = uri
        existingImageUrl = null // preview pakai URI baru
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val imageFile = File.createTempFile("IMG_", ".jpg", ctx.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        val uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            imageFile
        )
        currentImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun openGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun validateAndSubmit() {
        val judul = binding.nama.text.toString()
        val isi = binding.edtTextarea.text.toString()

        if (judul.isBlank() || isi.isBlank()) {
            toast("Judul dan isi wajib diisi")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val imageBytes: ByteArray? = withContext(Dispatchers.IO) {
                currentImageUri?.let { uri ->
                    val file = copyUriToTempFile(uri, requireContext()).reduceFileImage()
                    val bytes = file.readBytes()
                    file.delete()
                    bytes
                }
            }
            vm.submitEdit(judul, isi, imageBytes)
        }
    }

    // --- Utils ---

    private fun copyUriToTempFile(uri: Uri, context: Context): File {
        val file = File.createTempFile("selected_image", ".jpg", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }
        return file
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}