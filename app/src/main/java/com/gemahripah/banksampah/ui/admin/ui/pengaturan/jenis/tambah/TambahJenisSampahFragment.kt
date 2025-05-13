package com.gemahripah.banksampah.ui.admin.ui.pengaturan.jenis.tambah

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.InsertSampah
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahJenisSampahBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahJenisSampahFragment : Fragment(R.layout.fragment_tambah_jenis_sampah) {

    private lateinit var binding: FragmentTambahJenisSampahBinding
    private var listKategori: List<Kategori> = emptyList()
    private var selectedKategoriId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTambahJenisSampahBinding.bind(view)

        fetchKategoriSampah()
        setupKonfirmasiButton()
    }

    private fun fetchKategoriSampah() {
        val supabase = SupabaseProvider.client
        val kategoriView = binding.kategori // AutoCompleteTextView

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    supabase.postgrest["kategori"]
                        .select()
                        .decodeList<Kategori>()
                }

                listKategori = result

                val namaList = result.map { it.ktgNama }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)
                kategoriView.setAdapter(adapter)

                kategoriView.setOnItemClickListener { _, _, position, _ ->
                    val selectedNama = adapter.getItem(position)
                    val selectedKategori = listKategori.find { it.ktgNama == selectedNama }
                    selectedKategoriId = selectedKategori?.ktgId
                    Log.d("KategoriSelected", "ID: $selectedKategoriId")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat kategori", Toast.LENGTH_SHORT).show()
            }
        }

        kategoriView.setOnClickListener {
            kategoriView.clearFocus()
            kategoriView.showDropDown()
            hideKeyboard()
        }
    }

    private fun setupKonfirmasiButton() {
        binding.konfirmasi.setOnClickListener {
            val jenis = binding.jenis.text.toString().trim()
            val satuan = binding.satuan.text.toString().trim()
            val harga = binding.harga.text.toString().trim().toDoubleOrNull()
            val keterangan = binding.keterangan.text.toString().trim()

            if (selectedKategoriId == null) {
                Toast.makeText(requireContext(), "Silakan pilih kategori transaksi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (jenis.isEmpty() || satuan.isEmpty() || harga == null) {
                Toast.makeText(requireContext(), "Isi semua data dengan benar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = Sampah(
                sphKtgId = selectedKategoriId!!,
                sphJenis = jenis,
                sphSatuan = satuan,
                sphHarga = harga,
                sphKeterangan = if (keterangan.isNotEmpty()) keterangan else null
            )

            lifecycleScope.launch {
                try {
                    val supabase = SupabaseProvider.client

                    withContext(Dispatchers.IO) {
                        supabase.postgrest["transaksi"].insert(data)
                    }

                    Toast.makeText(requireContext(), "Jenis transaksi berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_tambahJenisSampahFragment_to_jenisSampahFragment)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal menambahkan data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetForm() {
        binding.kategori.setText("")
        binding.jenis.setText("")
        binding.satuan.setText("")
        binding.harga.setText("")
        binding.keterangan.setText("")
        selectedKategoriId = null
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}