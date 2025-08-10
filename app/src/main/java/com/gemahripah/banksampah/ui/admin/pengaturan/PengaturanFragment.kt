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
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentProfilBinding
import com.gemahripah.banksampah.ui.admin.AdminViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class PengaturanFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    // Tetap pakai AdminViewModel untuk menampilkan nama/email (activity scope)
    private val adminViewModel: AdminViewModel by activityViewModels()

    // ViewModel baru untuk handle logout + event
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
    }

    private fun setupObserver() {
        adminViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            binding.nama.text = pengguna.pgnNama
            binding.email.text = pengguna.pgnEmail
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Loading -> disable tombol keluar biar gak dobel klik
                launch {
                    pengaturanViewModel.isLoading.collect { loading ->
                        showLoading(loading)
                    }
                }

                // Toast
                launch {
                    pengaturanViewModel.toast.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }

                // Sukses logout -> clear task dan buka MainActivity
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

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.keluar.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}