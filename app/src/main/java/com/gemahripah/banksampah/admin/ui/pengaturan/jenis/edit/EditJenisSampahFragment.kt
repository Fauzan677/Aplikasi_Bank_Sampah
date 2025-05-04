package com.gemahripah.banksampah.admin.ui.pengaturan.jenis.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class EditJenisSampahFragment : Fragment() {

    private var _binding: FragmentTambahJenisSampahBinding? = null
    private val binding get() = _binding!!
    private val args: EditJenisSampahFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTambahJenisSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.judul.text = "Edit Jenis Sampah"
        binding.subJudul.text = "Data sampah"

        val data = args.kategoridanSampah
        val sampah = data.sampah
        val namaKategori = data.namaKategori

        binding.kategori.setText(namaKategori)
        binding.jenis.setText(sampah.jenis)          // EditText
        binding.satuan.setText(sampah.satuan)          // EditText
        binding.harga.setText(sampah.harga.toString()) // EditText, pastikan convert ke string
        binding.keterangan.setText(sampah.keterangan ?: "") // EditText, nullable

        binding.konfirmasi.setOnClickListener {
            val id = args.kategoridanSampah.sampah.id
            if (id == null) {
                Toast.makeText(requireContext(), "ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val kategoriId = args.kategoridanSampah.sampah.kategori
            val jenis = binding.jenis.text.toString()
            val satuan = binding.satuan.text.toString()
            val harga = binding.harga.text.toString().toLongOrNull() ?: 0L
            val keterangan = binding.keterangan.text.toString()

            // Panggil Supabase update
            lifecycleScope.launch {
                try {
                    SupabaseProvider.client.from("sampah").update(
                        {
                            set("kategori", kategoriId)
                            set("jenis", jenis)
                            set("satuan", satuan)
                            set("harga", harga)
                            set("keterangan", keterangan)
                        }
                    ) {
                        filter {
                            eq("id", id)
                        }
                    }

                    // Jika tidak error, lanjut navigasi
                    Toast.makeText(requireContext(), "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editJenisSampahFragment_to_jenisSampahFragment)

                } catch (e: Exception) {
                    Log.e("UpdateSampah", "Error: ${e.message}")
                    Toast.makeText(requireContext(), "Terjadi kesalahan saat memperbarui data", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.hapus.setOnClickListener {
            val id = args.kategoridanSampah.sampah.id
            if (id == null) {
                Toast.makeText(requireContext(), "ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    SupabaseProvider.client.from("sampah").delete {
                        filter {
                            eq("id", id)
                        }
                    }

                    Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editJenisSampahFragment_to_jenisSampahFragment)
                } catch (e: Exception) {
                    Log.e("HapusSampah", "Error: ${e.message}")
                    Toast.makeText(requireContext(), "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}