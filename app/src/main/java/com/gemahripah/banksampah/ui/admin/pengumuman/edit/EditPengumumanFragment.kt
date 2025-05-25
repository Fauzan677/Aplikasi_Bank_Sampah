package com.gemahripah.banksampah.ui.admin.pengumuman.edit

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
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
import com.gemahripah.banksampah.utils.reduceFileImage
import com.gemahripah.banksampah.utils.uriToFile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.File

class EditPengumumanFragment : Fragment() {

    private var _binding: FragmentTambahPengumumanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
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

        // Inisialisasi launcher kamera
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = currentImageUri
            if (success && uri != null) {
                binding.selectedImageView.setImageURI(uri)
                binding.selectedImageView.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
            }
        }

        // Inisialisasi launcher galeri
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                currentImageUri = it
                binding.selectedImageView.setImageURI(it)
                binding.selectedImageView.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
            }
        }

        binding.gambarFile.setOnClickListener {
            showImagePickerDialog()
        }

        val pengumuman = arguments?.let {
            EditPengumumanFragmentArgs.fromBundle(it).pengumuman
        }

        pengumuman?.let { data ->
            binding.nama.setText(data.pmnJudul)
            binding.edtTextarea.setText(data.pmnIsi)

            if (!data.pmnGambar.isNullOrEmpty()) {
                binding.selectedImageView.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
                Glide.with(requireContext())
                    .load(data.pmnGambar)
                    .into(binding.selectedImageView)
            }
        }

        binding.konfirmasi.setOnClickListener {
            pengumuman?.let { data ->
                editPengumuman(data)
            }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera dibutuhkan", Toast.LENGTH_SHORT).show()
        }
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
        val context = requireContext()
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
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
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun editPengumuman(pengumuman: Pengumuman) {
        val judul = binding.nama.text.toString()
        val isi = binding.edtTextarea.text.toString()

        if (judul.isBlank() || isi.isBlank()) {
            Toast.makeText(requireContext(), "Judul dan isi wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                var imageUrl = pengumuman.pmnGambar // default: gambar lama
                var isImageChanged = false

                currentImageUri?.let { uri ->
                    val file = uriToFile(uri, requireContext()).reduceFileImage()
                    val byteArray = file.readBytes()
                    val bucket = SupabaseProvider.client.storage.from("pengumuman")
                    val oldFileName = imageUrl?.substringAfterLast("/") ?: return@let

                    bucket.update("images/$oldFileName", byteArray) {
                        upsert = false
                    }

                    isImageChanged = true
                }

                val isJudulChanged = judul != pengumuman.pmnJudul
                val isIsiChanged = isi != pengumuman.pmnIsi

                if (isJudulChanged || isIsiChanged || isImageChanged) {
                    val supabase = SupabaseProvider.client

                    supabase.from("pengumuman").update({
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

                    Toast.makeText(requireContext(), "Pengumuman berhasil diupdate", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Tidak ada perubahan yang disimpan", Toast.LENGTH_SHORT).show()
                }

                findNavController().navigate(R.id.action_editPengumumanFragment_to_navigation_pengumuman)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}