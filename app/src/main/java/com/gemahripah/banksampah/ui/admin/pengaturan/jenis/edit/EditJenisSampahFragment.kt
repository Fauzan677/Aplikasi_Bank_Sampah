package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahJenisSampahBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.Sampah
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

class EditJenisSampahFragment : Fragment() {

    private var _binding: FragmentTambahJenisSampahBinding? = null
    private val binding get() = _binding!!

    private val args: EditJenisSampahFragmentArgs by navArgs()
    private val supabase = SupabaseProvider.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTambahJenisSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupSatuanDropdown()
        setupButtonActions()
    }

    private fun setupUI() {
        binding.judul.text = "Edit Jenis Sampah"
        binding.subJudul.text = "Data Sampah"

        val data = args.kategoridanSampah
        val sampah = data.sampah

        binding.kategori.setText(data.namaKategori)
        binding.jenis.setText(sampah.sphJenis)
        binding.satuan.setText(sampah.sphSatuan)
        binding.harga.setText(sampah.sphHarga.toString())
        binding.keterangan.setText(sampah.sphKeterangan ?: "")
    }

    private fun setupSatuanDropdown() {
        val satuanView = binding.satuan

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val satuanList = fetchDistinctSatuan()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, satuanList)

                satuanView.threshold = 1
                satuanView.setAdapter(adapter)

                satuanView.setOnClickListener {
                    satuanView.clearFocus()
                    satuanView.showDropDown()
                    hideKeyboard()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal memuat data satuan")
            }
        }
    }

    private suspend fun fetchDistinctSatuan(): List<String> = withContext(Dispatchers.IO) {
        supabase
            .from("sampah")
            .select(columns = Columns.list("sphSatuan"))
            .decodeList<Sampah>()
            .mapNotNull { it.sphSatuan }
            .distinct()
    }

    private fun setupButtonActions() {
        binding.konfirmasi.setOnClickListener { updateData() }
        binding.hapus.setOnClickListener { deleteData() }
    }

    private fun updateData() {
        val id = args.kategoridanSampah.sampah.sphId ?: run {
            showToast("ID tidak ditemukan")
            return
        }

        val kategoriId = args.kategoridanSampah.sampah.sphKtgId
        val jenis = binding.jenis.text.toString().trim()
        val satuan = binding.satuan.text.toString().trim()
        val harga = binding.harga.text.toString().toLongOrNull() ?: 0L
        val keterangan = binding.keterangan.text.toString().trim()

        lifecycleScope.launch {
            try {
                if (kategoriId != null) {
                    updateSampah(id, kategoriId, jenis, satuan, harga, keterangan)
                }
                showToast("Data berhasil diperbarui")
                navigateBack()
            } catch (e: Exception) {
                Log.e("UpdateSampah", "Error: ${e.message}")
                showToast("Terjadi kesalahan saat memperbarui data")
            }
        }
    }

    private suspend fun updateSampah(
        id: Long,
        kategoriId: Long,
        jenis: String,
        satuan: String,
        harga: Long,
        keterangan: String
    ) = withContext(Dispatchers.IO) {
        supabase.from("sampah").update({
            set("sphKtgId", kategoriId)
            set("sphJenis", jenis)
            set("sphSatuan", satuan)
            set("sphHarga", harga)
            set("sphKeterangan", keterangan)
        }) {
            filter { eq("sphId", id) }
        }
    }

    private fun deleteData() {
        val id = args.kategoridanSampah.sampah.sphId ?: run {
            showToast("ID tidak ditemukan")
            return
        }

        lifecycleScope.launch {
            try {
                deleteSampah(id)
                showToast("Data berhasil dihapus")
                navigateBack()
            } catch (e: Exception) {
                Log.e("HapusSampah", "Error: ${e.message}")
                showToast("Gagal menghapus data")
            }
        }
    }

    private suspend fun deleteSampah(id: Long) = withContext(Dispatchers.IO) {
        supabase.from("sampah").delete {
            filter { eq("sphId", id) }
        }
    }

    private fun navigateBack() {
        findNavController().navigate(R.id.action_editJenisSampahFragment_to_jenisSampahFragment)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}