package com.gemahripah.banksampah.ui.admin.pengaturan

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.ui.MainActivity
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentProfilBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.admin.AdminViewModel
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch

class PengaturanFragment : Fragment(), Reloadable {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val adminViewModel: AdminViewModel by activityViewModels()
    private val pengaturanViewModel: PengaturanViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObserver()
        setupListeners()
        collectVm()
        setupSwipeRefresh()

        // Tampilkan/sembunyikan kartu koneksi + muat data awal
        reloadData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }
    }

    override fun reloadData() {
        // Kalau offline: tampilkan kartu & hentikan animasi swipe
        if (!updateInternetCard()) {
            binding.swipeRefresh.isRefreshing = false
            return
        }

        // Online -> load ulang by id
        val id = adminViewModel.pengguna.value?.pgnId
        if (id != null) {
            adminViewModel.loadPenggunaById(id)
        } else {
            // Tidak ada id di VM -> hentikan swipe biar tidak muter terus
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupObserver() {
        adminViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            binding.nama.text  = pengguna?.pgnNama ?: "-"
            binding.email.text = pengguna?.pgnEmail ?: "-"
            // hentikan animasi swipe tiap ada update (sukses/empty/error trigger dari VM)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupListeners() {
        binding.btProfil.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_pengaturan_to_editProfilFragment)
        }
        binding.btCetak.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_pengaturan_to_laporanFragment)
        }
        binding.jenis.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_pengaturan_to_jenisSampahFragment)
        }
        binding.nasabah.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_pengaturan_to_penggunaFragment)
        }
        binding.keluar.setOnClickListener { tampilkanDialogKonfirmasiKeluar() }
    }

    private fun tampilkanDialogKonfirmasiKeluar() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Keluar") { _, _ -> pengaturanViewModel.logout() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.keluar.isEnabled = !isLoading
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    pengaturanViewModel.isLoading.collect { showLoading(it) }
                }
                launch {
                    pengaturanViewModel.toast.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    pengaturanViewModel.logoutSuccess.collect {
                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // refresh tiap kembali ke layar
        reloadData()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
