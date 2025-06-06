package com.gemahripah.banksampah.ui.admin.transaksi.masuk

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class SetorSampahFragment : Fragment() {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SetorSampahViewModel by viewModels()

    private var jumlahInput = 1
    private val tambahanSampahList = mutableListOf<ItemSetorSampahBinding>()

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

        viewModel.loadPengguna()
        viewModel.loadSampah()

        observePengguna()
        observeSampah()

        val pengguna = arguments?.let { SetorSampahFragmentArgs.fromBundle(it).pengguna }
        pengguna?.let {
            binding.nama.setText(it.pgnNama, false)
            viewModel.selectedUserId = it.pgnId
        }

        binding.tambah.setOnClickListener {
            tambahInputSampah()
        }

        binding.konfirmasi.setOnClickListener {
            val userId = viewModel.selectedUserId
            val keterangan = binding.keterangan.text.toString()

            if (userId == null) {
                Toast.makeText(requireContext(), "Silakan pilih nama nasabah dari daftar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jenisUtama = binding.jenis1.text.toString()
            val jumlahUtama = binding.jumlah1.text.toString().toDoubleOrNull() ?: 0.0

            val inputTambahan = tambahanSampahList.mapNotNull {
                val jenis = it.autoCompleteJenis.text.toString()
                val jumlah = it.editTextJumlah.text.toString().toDoubleOrNull() ?: 0.0
                if (jenis.isNotBlank() && jumlah > 0) Pair(jenis, jumlah) else null
            }

            viewModel.simpanTransaksi(
                keterangan,
                userId,
                Pair(jenisUtama, jumlahUtama),
                inputTambahan,
                onSuccess = {
                    Toast.makeText(requireContext(), "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_setorSampahFragment_to_navigation_transaksi)
                },
                onError = {
                    Toast.makeText(requireContext(), "Gagal menyimpan: $it", Toast.LENGTH_SHORT).show()
                }
            )

        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observePengguna() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.penggunaList.collect { penggunaList ->
                val namaList = penggunaList.mapNotNull { it.pgnNama }.distinct()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)
                binding.nama.setAdapter(adapter)

                binding.nama.setOnTouchListener { _, _ ->
                    binding.nama.showDropDown()
                    false
                }

                val namaToId = penggunaList.associateBy { it.pgnNama }
                binding.nama.setOnItemClickListener { _, _, position, _ ->
                    val selectedNama = adapter.getItem(position)
                    viewModel.selectedUserId = namaToId[selectedNama]?.pgnId
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun observeSampah() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sampahList.collect { sampahList ->
                val jenisList = sampahList.mapNotNull { it.sphJenis }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jenisList)
                binding.jenis1.setAdapter(adapter)

                binding.jenis1.setOnTouchListener { _, _ ->
                    binding.jenis1.showDropDown()
                    false
                }

                binding.jenis1.setOnItemClickListener { _, _, position, _ ->
                    val selectedJenis = jenisList[position]
                    val satuan = viewModel.jenisToSatuanMap[selectedJenis] ?: "Unit"
                    binding.jumlahLabel1.text = "Jumlah Setor ($satuan)"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun tambahInputSampah() {
        jumlahInput++
        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)
        itemBinding.jenisSampahLabel.text = "Jenis Sampah $jumlahInput"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor $jumlahInput"

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            viewModel.sampahList.value.mapNotNull { it.sphJenis }
        )

        itemBinding.autoCompleteJenis.setAdapter(adapter)
        itemBinding.autoCompleteJenis.setOnTouchListener { _, _ ->
            itemBinding.autoCompleteJenis.showDropDown()
            false
        }

        itemBinding.autoCompleteJenis.setOnItemClickListener { _, _, position, _ ->
            val jenis = adapter.getItem(position) ?: return@setOnItemClickListener
            val satuan = viewModel.jenisToSatuanMap[jenis] ?: "Unit"
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor ($satuan)"
        }

        binding.containerTambahan.addView(itemBinding.root)
        tambahanSampahList.add(itemBinding)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}