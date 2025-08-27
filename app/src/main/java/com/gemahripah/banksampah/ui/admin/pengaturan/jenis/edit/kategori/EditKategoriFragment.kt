package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit.kategori

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.gemahripah.banksampah.databinding.FragmentTambahKategoriBinding
import kotlinx.coroutines.launch

class EditKategoriFragment : Fragment() {

    private var _binding: FragmentTambahKategoriBinding? = null
    private val binding get() = _binding!!

    private val args: EditKategoriFragmentArgs by navArgs()
    private val vm: EditKategoriViewModel by viewModels()

    private lateinit var kategori: Kategori

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        kategori = args.kategori
        vm.init(kategori)

        setupUI()
        setupListeners()
        collectVm()

        vm.checkRelasiSampah()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        binding.judul.text = "Edit Kategori Sampah"
        binding.kategori.setText(kategori.ktgNama)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupListeners() {
        binding.konfirmasi.setOnClickListener {
            vm.updateKategori(binding.kategori.text.toString())
        }

        binding.hapus.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus kategori ini?")
                .setPositiveButton("Hapus") { _, _ -> vm.deleteKategori() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.isLoading.collect { showLoading(it) }
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
                                .setPopUpTo(R.id.editKategoriFragment, true)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                }

                launch {
                    vm.inUse.collect { inUse ->
                        val disabled = (inUse == true) || (inUse == null)
                        binding.hapus.isEnabled = !disabled
                        binding.hapus.alpha = if (disabled) 0.5f else 1f
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
        binding.hapus.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}