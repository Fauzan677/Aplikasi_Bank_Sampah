package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.tambah

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTambahKategoriBinding

class TambahKategoriFragment : Fragment() {

    private var _binding: FragmentTambahKategoriBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TambahKategoriViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.hapus.visibility = View.GONE

        binding.konfirmasi.setOnClickListener {
            val nama = binding.kategori.text.toString().trim()
            if (nama.isNotEmpty()) {
                viewModel.tambahKategori(nama,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_tambahKategoriFragment_to_jenisSampahFragment)
                    },
                    onError = { e ->
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Isi kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}