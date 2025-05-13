package com.gemahripah.banksampah.ui.admin.ui.transaksi.masuk

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import com.gemahripah.banksampah.utils.reduceFileImage
import com.gemahripah.banksampah.utils.uriToFile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.io.File

class SetorSampahFragment : Fragment() {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null

    private var jumlahInput = 1 // Awalnya 1 form
    private var jenisList: List<String> = emptyList() // Global list jenis transaksi
    private var jenisToSatuanMap: Map<String, String> = emptyMap()
    private val tambahanSampahList = mutableListOf<ItemSetorSampahBinding>()

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCameraInternal() // jalankan kamera jika izin diberikan
        } else {
            Toast.makeText(requireContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetorSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadNamaNasabah()
        loadJenisSampah()

        binding.tambah.setOnClickListener {
            tambahInputSampah()
        }

        binding.gambarFile.setOnClickListener {
            showImagePickerDialog()
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                binding.selectedImageView.setImageURI(currentImageUri)
                binding.selectedImageView.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.selectedImageView.setImageURI(it)
                binding.selectedImageView.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
            }
        }

        binding.konfirmasi.setOnClickListener {
            val namaNasabah = binding.nama.text.toString()
            val userId = selectedUserId
            val keterangan = binding.keterangan.text.toString()

            if (userId == null) {
                Toast.makeText(requireContext(), "Silakan pilih nama nasabah dari daftar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // 1. Simpan transaksi ke Supabase
                    val transaksi = Transaksi(
                        tskIdPengguna = userId,
                        tskKeterangan = keterangan,
                        tskTipe = "Masuk"
                    )

                    val inserted = SupabaseProvider.client
                        .from("transaksi")
                        .insert(transaksi) {
                            select(Columns.list("tskId"))
                        }
                        .decodeSingle<Transaksi>()

                    val transaksiId = inserted.tskId

                    if (transaksiId == null) {
                        Toast.makeText(requireContext(), "Gagal menyimpan transaksi", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // 2. Simpan detail untuk input pertama (jenis1, jumlah1)
                    val jenisUtama = binding.jenis1.text.toString()
                    val sampahIdUtama = namaToIdMap[jenisUtama]
                    val jumlahUtama = binding.jumlah1.text.toString().toDoubleOrNull() ?: 0.0

                    if (sampahIdUtama != null && jumlahUtama > 0) {
                        val detailUtama = DetailTransaksi(
                            dtlTskId = transaksiId,
                            dtlSphId = sampahIdUtama,
                            dtlJumlah = jumlahUtama
                        )

                        SupabaseProvider.client
                            .from("detail_transaksi")
                            .insert(detailUtama)
                    }

                    // 3. Simpan detail untuk tambahan input dinamis
                    for (item in tambahanSampahList) {
                        val jenis = item.autoCompleteJenis.text.toString()
                        val sampahId = namaToIdMap[jenis]
                        val jumlah = item.editTextJumlah.text.toString().toDoubleOrNull() ?: 0.0

                        if (sampahId != null && jumlah > 0) {
                            val detail = DetailTransaksi(
                                dtlTskId = transaksiId,
                                dtlSphId = sampahId,
                                dtlJumlah = jumlah
                            )

                            SupabaseProvider.client
                                .from("detail_transaksi")
                                .insert(detail)
                        }
                    }

                    Toast.makeText(requireContext(), "Data berhasil disimpan", Toast.LENGTH_SHORT).show()

                    // Optional: reset form atau navigasi
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal menyimpan data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCameraInternal()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun openCameraInternal() {
        val imageFile = File.createTempFile("IMG_", ".jpg", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        currentImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
        currentImageUri?.let { uri ->
            cameraLauncher.launch(uri)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun uploadImage() {
        currentImageUri?.let { uri ->
            val imageFile = uriToFile(uri, requireContext()).reduceFileImage()
            // Lanjutkan proses upload dengan imageFile
        } ?: Toast.makeText(requireContext(), "Gambar belum dipilih", Toast.LENGTH_SHORT).show()
    }

    private var namaToIdMap: Map<String, Long> = emptyMap()

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun loadJenisSampah() {
        lifecycleScope.launch {
            try {
                val data = SupabaseProvider.client
                    .from("transaksi")
                    .select()
                    .decodeList<Sampah>()

                // Mapping nama jenis → id
                val namaList = data.mapNotNull { it.sphJenis } // This will exclude null values
                namaToIdMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                jenisToSatuanMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
                jenisList = namaList

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)
                binding.jenis1.setAdapter(adapter)
                binding.jenis1.threshold = 1
                binding.jenis1.setOnTouchListener { _, _ ->
                    binding.jenis1.showDropDown()
                    false
                }

                binding.jenis1.setOnItemClickListener { _, _, position, _ ->
                    val selectedJenis = namaList[position]
                    val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
                    binding.jumlahLabel1.text = "Jumlah Setor ($satuan)"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat jenis transaksi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var selectedUserId: String? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun loadNamaNasabah() {
        lifecycleScope.launch {
            try {
                val penggunaList = SupabaseProvider.client
                    .from("pengguna")
                    .select()
                    .decodeList<Pengguna>()

                Log.d("SetorSampahFragment", "Jumlah pengguna yang diambil: ${penggunaList.size}")

                // Buat map nama -> pengguna untuk referensi nanti
                val namaToPenggunaMap = penggunaList.associateBy { it.pgnNama }

                val namaList = penggunaList.mapNotNull { it.pgnNama }.distinct()

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)
                binding.nama.setAdapter(adapter)

                // Biarkan bisa diketik manual dan juga tampilkan dropdown saat disentuh
                binding.nama.threshold = 1
                binding.nama.setOnTouchListener { _, _ ->
                    binding.nama.showDropDown()
                    false
                }

                // Simpan id pengguna ketika nama dipilih
                binding.nama.setOnItemClickListener { _, _, position, _ ->
                    val selectedNama = adapter.getItem(position)
                    val selectedPengguna = namaToPenggunaMap[selectedNama]
                    selectedUserId = selectedPengguna?.pgnId // <-- simpan ID-nya di variabel global
                    Log.d("SetorSampahFragment", "ID pengguna terpilih: $selectedUserId")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat nama nasabah", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun tambahInputSampah() {
        jumlahInput++

        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)

        itemBinding.jenisSampahLabel.text = "Jenis Sampah $jumlahInput"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor $jumlahInput"

        itemBinding.autoCompleteJenis.setTag("jenis$jumlahInput")
        itemBinding.editTextJumlah.setTag("jumlah$jumlahInput")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jenisList)
        itemBinding.autoCompleteJenis.setAdapter(adapter)

        itemBinding.autoCompleteJenis.inputType = 0
        itemBinding.autoCompleteJenis.isFocusable = false
        itemBinding.autoCompleteJenis.isClickable = true
        itemBinding.autoCompleteJenis.setOnTouchListener { _, _ ->
            itemBinding.autoCompleteJenis.showDropDown()
            true
        }

        // Ubah label jumlah sesuai satuan
        itemBinding.autoCompleteJenis.setOnItemClickListener { _, _, position, _ ->
            val selectedJenis = jenisList[position]
            val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor $jumlahInput ($satuan)"
        }

        val index = binding.containerInput.indexOfChild(binding.tambah)
        tambahanSampahList.add(itemBinding)
        binding.containerInput.addView(itemBinding.root, index)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}