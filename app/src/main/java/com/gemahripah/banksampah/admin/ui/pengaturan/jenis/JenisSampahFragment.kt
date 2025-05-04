package com.gemahripah.banksampah.admin.ui.pengaturan.jenis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.admin.ui.pengaturan.adapter.KategoriAdapter
import com.gemahripah.banksampah.databinding.FragmentJenisSampahBinding

class JenisSampahFragment : Fragment() {

    private lateinit var viewModel: JenisSampahViewModel
    private var _binding: FragmentJenisSampahBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJenisSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Adapter
        val kategoriAdapter = KategoriAdapter(emptyList()) { selected ->
            val action = JenisSampahFragmentDirections
                .actionJenisSampahFragmentToEditJenisSampahFragment(selected)
            findNavController().navigate(action)

        }


        binding.rvJenisSampah.layoutManager = LinearLayoutManager(context)
        binding.rvJenisSampah.adapter = kategoriAdapter  // Pasang adapter terlebih dahulu

        // Inisialisasi ViewModel
        viewModel = ViewModelProvider(this).get(JenisSampahViewModel::class.java)

        // Mengobservasi data kategori dari ViewModel
        viewModel.kategoriList.observe(viewLifecycleOwner, Observer { kategoriList ->
            println("Jumlah kategori yang diterima: ${kategoriList.size}")
            kategoriAdapter.updateKategoriList(kategoriList)  // Perbarui daftar kategori
        })

        // Memuat data kategori
        viewModel.loadKategori()  // Panggil setelah adapter terpasang

        // Navigasi untuk tombol tambah kategori dan jenis sampah
        binding.tambahJenis.setOnClickListener {
            findNavController().navigate(R.id.action_jenisSampahFragment_to_tambahJenisSampahFragment)
        }

        binding.tambahKategori.setOnClickListener {
            findNavController().navigate(R.id.action_jenisSampahFragment_to_tambahKategoriFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
