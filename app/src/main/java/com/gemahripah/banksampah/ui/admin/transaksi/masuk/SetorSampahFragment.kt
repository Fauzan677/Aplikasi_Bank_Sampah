package com.gemahripah.banksampah.ui.admin.transaksi.masuk

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import kotlinx.coroutines.launch

class SetorSampahFragment : Fragment() {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SetorSampahViewModel by viewModels()

    private val tambahanSampahList = mutableListOf<ItemSetorSampahBinding>()
    private var jenisList: List<String> = emptyList()

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
                jenisList = viewModel.sampahList.value.mapNotNull { it.sphJenis }
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
                    updateAllAdapters()
                }
            }

        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun tambahInputSampah() {
        val index = tambahanSampahList.size + 2
        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)
        itemBinding.jenisSampahLabel.text = "Jenis Sampah $index"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor $index"

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
            updateAllAdapters()
        }

        val dynamicId = View.generateViewId()
        itemBinding.hapusTambahan.id = dynamicId

        itemBinding.hapusTambahan.setOnClickListener {
            binding.containerTambahan.removeView(itemBinding.root)
            tambahanSampahList.remove(itemBinding)
            perbaruiLabelInput()
            updateAllAdapters()
        }

        binding.containerTambahan.addView(itemBinding.root)
        tambahanSampahList.add(itemBinding)
        updateAllAdapters()
    }

    private fun perbaruiLabelInput() {
        tambahanSampahList.forEachIndexed { index, itemBinding ->
            itemBinding.jenisSampahLabel.text = "Jenis Sampah ${index + 2}"
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor ${index + 2}"
            itemBinding.hapusTambahan.id = View.generateViewId()
        }
    }


    private fun getAvailableJenis(currentJenis: String?): List<String> {
        val selectedJenisList = mutableSetOf<String>()

        binding.jenis1.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            selectedJenisList.add(it)
        }

        tambahanSampahList.forEach { item ->
            val jenis = item.autoCompleteJenis.text?.toString()
            if (!jenis.isNullOrEmpty()) {
                selectedJenisList.add(jenis)
            }
        }

        currentJenis?.let {
            selectedJenisList.remove(it)
        }

        val allJenis = viewModel.sampahList.value.mapNotNull { it.sphJenis }.distinct()

        return (allJenis
            .filter { it.isNotBlank() && it !in selectedJenisList } +
                listOfNotNull(currentJenis?.takeIf { it.isNotBlank() }))
            .distinct()
    }

    private fun updateAllAdapters() {
        val jenisUtama = binding.jenis1.text.toString()
        val availableJenisUtama = getAvailableJenis(jenisUtama)

        val adapterUtama = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableJenisUtama)
        binding.jenis1.setAdapter(adapterUtama)

        tambahanSampahList.forEach { item ->
            val currentJenis = item.autoCompleteJenis.text.toString()
            val availableJenis = getAvailableJenis(currentJenis)

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableJenis)
            item.autoCompleteJenis.setAdapter(adapter)
        }

        val semuaJenisTerpakai = viewModel.sampahList.value.mapNotNull { it.sphJenis }.all { jenis ->
            jenis == binding.jenis1.text.toString() ||
                    tambahanSampahList.any { it.autoCompleteJenis.text.toString() == jenis }
        }

        binding.tambah.isEnabled = !semuaJenisTerpakai
        binding.tambah.alpha = if (binding.tambah.isEnabled) 1f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}