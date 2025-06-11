package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.tambah

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahJenisSampahBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahJenisSampahFragment : Fragment(R.layout.fragment_tambah_jenis_sampah) {

    private lateinit var binding: FragmentTambahJenisSampahBinding
    private val supabase: SupabaseClient by lazy { SupabaseProvider.client }

    private var listKategori: List<Kategori> = emptyList()
    private var selectedKategoriId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTambahJenisSampahBinding.bind(view)

        binding.hapus.visibility = View.GONE

        setupKategoriDropdown()
        setupSatuanDropdown()
        setupKonfirmasiButton()
    }

    private fun setupKategoriDropdown() {
        val kategoriView = binding.kategori

        viewLifecycleOwner.lifecycleScope.launch {
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

                kategoriView.setOnClickListener {
                    kategoriView.clearFocus()
                    kategoriView.showDropDown()
                    hideKeyboard()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal memuat kategori")
            }
        }
    }

    private fun setupSatuanDropdown() {
        val satuanView = binding.satuan

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    supabase
                        .from("sampah")
                        .select(columns = Columns.list("sphSatuan"))
                        .decodeList<Sampah>()
                }

                val satuanList = result.mapNotNull { it.sphSatuan }.distinct()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, satuanList)

                satuanView.threshold = 1 // Tampilkan saat ketik 1 huruf
                satuanView.setAdapter(adapter)

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal memuat data satuan")
            }
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


            submitSampah(data)
        }
    }

    private fun submitSampah(data: Sampah) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabase.postgrest["sampah"].insert(data)
                }
                showToast("Jenis sampah berhasil ditambahkan")
                findNavController().navigate(R.id.action_tambahJenisSampahFragment_to_jenisSampahFragment)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal menambahkan data")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}