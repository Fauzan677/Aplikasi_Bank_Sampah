package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.tambah.sampah

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahJenisSampahBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahJenisSampahFragment :
    Fragment(R.layout.fragment_tambah_jenis_sampah), Reloadable {

    private lateinit var binding: FragmentTambahJenisSampahBinding
    private val vm: TambahJenisSampahViewModel by viewModels()

    private var listKategori: List<Kategori> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTambahJenisSampahBinding.bind(view)

        binding.hapus.visibility = View.GONE

        if (!updateInternetCard()) return

        setupKategoriDropdown()
        setupSatuanDropdown()
        setupKonfirmasiButton()
        collectVm()

        vm.loadAwal()
    }

    private fun setupKategoriDropdown() {
        val kategoriView = binding.kategori

        // UI interactions
        kategoriView.setOnClickListener {
            kategoriView.clearFocus()
            kategoriView.showDropDown()
            hideKeyboard()
        }

        // Data binding
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.kategori.collect { list ->
                    listKategori = list
                    val namaList = list.mapNotNull { it.ktgNama }
                    kategoriView.setAdapter(
                        ArrayAdapter(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, namaList)
                    )
                }
            }
        }

        kategoriView.setOnItemClickListener { _, _, position, _ ->
            val selectedNama = kategoriView.adapter.getItem(position) as? String
            val selected = listKategori.firstOrNull { it.ktgNama == selectedNama }
            vm.setSelectedKategoriId(selected?.ktgId)
        }
    }

    private fun setupSatuanDropdown() {
        val satuanView = binding.satuan
        satuanView.threshold = 1

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.satuanSuggestions.collect { list ->
                    satuanView.setAdapter(
                        ArrayAdapter(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, list)
                    )
                }
            }
        }
    }

    private fun setupKonfirmasiButton() {
        binding.konfirmasi.setOnClickListener {
            val jenis = binding.jenis.text.toString().trim()
            val satuan = binding.satuan.text.toString().trim()
            val harga = binding.harga.text.toString().trim().toDoubleOrNull()
            val keterangan = binding.keterangan.text.toString().trim()

            vm.submit(jenis, satuan, harga, keterangan)
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.isLoading.collect { loading ->
                        binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
                        binding.layoutKonten.alpha = if (loading) 0.3f else 1f
                        binding.konfirmasi.isEnabled = !loading
                    }
                }
                launch {
                    vm.toast.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    vm.navigateBack.collect {
                        findNavController().navigate(
                            R.id.jenisSampahFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.tambahJenisSampahFragment, true)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                }
            }
        }
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        vm.loadAwal()
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}