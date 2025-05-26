package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.supabase.SupabaseProvider.client
import com.gemahripah.banksampah.databinding.FragmentTambahKategoriBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditKategoriFragment : Fragment() {

    private var _binding: FragmentTambahKategoriBinding? = null
    private val binding get() = _binding!!

    private val args: EditKategoriFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.judul.text = "Edit Kategori Sampah"

        val kategori: Kategori = args.kategori

        binding.kategori.setText(kategori.ktgNama)

        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.kategori.text.toString().trim()
            val id = kategori.ktgId

            if (id != null && namaBaru.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        client.from("kategori").update(
                            {
                                set("ktgNama", namaBaru)
                            }
                        ) {
                            filter {
                                eq("ktgId", id)
                            }
                        }

                        Toast.makeText(requireContext(), "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_editKategoriFragment_to_jenisSampahFragment)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        binding.hapus.setOnClickListener {
            val id = kategori.ktgId

            if (id != null) {
                lifecycleScope.launch {
                    try {
                        client.from("kategori").delete {
                            filter {
                                eq("ktgId", id)
                            }
                        }

                        Toast.makeText(requireContext(), "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_editKategoriFragment_to_jenisSampahFragment)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}