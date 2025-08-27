package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit.sampah

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.databinding.FragmentTambahJenisSampahBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import kotlinx.coroutines.launch


class EditJenisSampahFragment : Fragment() {

    private var _binding: FragmentTambahJenisSampahBinding? = null
    private val binding get() = _binding!!

    private val args: EditJenisSampahFragmentArgs by navArgs()
    private val vm: EditJenisSampahViewModel by viewModels()

    private var kategoriAdapter: ArrayAdapter<String>? = null
    private var satuanAdapter: ArrayAdapter<String>? = null
    private var kategoriList: List<Kategori> = emptyList()

    private var lastLoading = false
    private var canDeleteByUsage = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahJenisSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun applyDeleteEnabled() {
        val enabled = !lastLoading && canDeleteByUsage
        binding.hapus.isEnabled = enabled
        binding.hapus.alpha = if (enabled) 1f else 0.5f
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.judul.text = "Edit Jenis Sampah"

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val relasi: SampahRelasi = args.SampahRelasi
        val namaKategoriAwal = relasi.sphKtgId?.ktgNama.orEmpty()

        // Prefill form UI
        binding.kategori.setText(namaKategoriAwal, false)
        binding.jenis.setText(relasi.sphJenis ?: "")
        binding.kode.setText(relasi.sphKode ?: "")
        binding.satuan.setText(relasi.sphSatuan ?: "")
        binding.harga.setText(relasi.sphHarga?.toString() ?: "")
        binding.keterangan.setText(relasi.sphKeterangan ?: "")

        // Init VM state dari relasi
        vm.initFromArgs(relasi)

        setupDropdownInteractions()
        setupButtons()
        collectVm()

        if (!updateInternetCard()) return
        vm.checkUsedInDetail(relasi.sphId)
        vm.loadKategori()
        vm.loadDistinctSatuan()
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Loading
                launch {
                    vm.isLoading.collect {
                        lastLoading = it
                        showLoading(it)
                        applyDeleteEnabled()
                    }
                }

                // Toast
                launch { vm.toast.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() } }

                // Kategori list
                launch {
                    vm.kategoriList.collect { list ->
                        kategoriList = list
                        val names = list.mapNotNull { it.ktgNama }
                        kategoriAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            names
                        )
                        binding.kategori.setAdapter(kategoriAdapter)
                        binding.kategori.threshold = 0

                        val current = binding.kategori.text?.toString()
                        if (!current.isNullOrBlank()) {
                            val id = list.find { it.ktgNama == current }?.ktgId
                            vm.setSelectedKategoriId(id)
                        }
                    }
                }

                // Satuan list
                launch {
                    vm.satuanList.collect { list ->
                        satuanAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            list
                        )
                        binding.satuan.setAdapter(satuanAdapter)
                        binding.satuan.threshold = 1
                    }
                }

                launch {
                    vm.usedInDetail.collect { inUse ->
                        canDeleteByUsage = (inUse == false)
                        applyDeleteEnabled()
                    }
                }

                // Navigate back
                launch {
                    vm.navigateBack.collect {
                        findNavController().navigate(
                            R.id.jenisSampahFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.editJenisSampahFragment, true)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                }
            }
        }
    }

    private fun setupDropdownInteractions() {
        // Kategori
        binding.kategori.setOnItemClickListener { _, _, position, _ ->
            val name = kategoriAdapter?.getItem(position)
            val selected = kategoriList.find { it.ktgNama == name }
            vm.setSelectedKategoriId(selected?.ktgId)
        }
        binding.kategori.setOnClickListener {
            binding.kategori.clearFocus()
            hideKeyboard()
            binding.kategori.post { binding.kategori.showDropDown() }
        }

        // Satuan
        binding.satuan.setOnClickListener {
            binding.satuan.clearFocus()
            binding.satuan.showDropDown()
            hideKeyboard()
        }
    }

    private fun setupButtons() {
        binding.konfirmasi.setOnClickListener {
            val jenis = binding.jenis.text.toString().trim()
            val kode  = binding.kode.text.toString().trim()
            val satuan = binding.satuan.text.toString().trim()
            val harga = binding.harga.text.toString().toLongOrNull()
            val ket = binding.keterangan.text.toString().trim()
            vm.submitUpdate(jenis, kode, satuan, harga, ket)
        }

        binding.hapus.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah kamu yakin ingin menghapus data ini?")
                .setPositiveButton("Ya") { _, _ -> vm.deleteCurrent() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}