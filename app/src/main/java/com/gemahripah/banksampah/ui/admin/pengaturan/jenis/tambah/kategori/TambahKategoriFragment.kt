package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.tambah.kategori

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTambahKategoriBinding
import kotlinx.coroutines.launch

class TambahKategoriFragment : Fragment() {

    private var _binding: FragmentTambahKategoriBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TambahKategoriViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.hapus.visibility = View.GONE

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.konfirmasi.setOnClickListener {
            val nama = binding.kategori.text.toString().trim()
            if (nama.isEmpty()) {
                Toast.makeText(requireContext(), "Isi kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pakai viewLifecycleOwner agar aman saat view dihancurkan
            viewLifecycleOwner.lifecycleScope.launch {
                // Opsional: biarkan VM yang atur loading; atau tampilkan awal di sini
                // showLoading(true)
                val sudahAda = viewModel.isKategoriDipakai(nama)
                if (sudahAda) {
                    Toast.makeText(requireContext(), "Kategori sudah ada, masukkan kategori lain", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                viewModel.tambahKategori(nama)
            }
        }

        // Collect state & event
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
                        binding.layoutKonten.alpha = if (loading) 0.3f else 1f
                        binding.konfirmasi.isEnabled = !loading
                    }
                }
                launch { viewModel.toast.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                } }
                launch { viewModel.done.collect {
                    // Pop this screen from back stack
                    findNavController().navigate(
                        R.id.jenisSampahFragment, null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.tambahKategoriFragment, /*inclusive=*/true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                } }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
