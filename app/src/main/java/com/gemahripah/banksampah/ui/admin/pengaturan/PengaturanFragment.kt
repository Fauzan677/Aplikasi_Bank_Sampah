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
import androidx.lifecycle.lifecycleScope
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

    private val adminViewModel: AdminViewModel by activityViewModels()

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
    }

    private fun setupObserver() {
        adminViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            binding.nama.text = pengguna.pgnNama
            binding.email.text = pengguna.pgnEmail
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

        binding.keluar.setOnClickListener {
            tampilkanDialogKonfirmasiKeluar()
        }
    }

    private fun tampilkanDialogKonfirmasiKeluar() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Keluar") { _, _ -> keluar() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun keluar() {
        lifecycleScope.launch {
            try {
                SupabaseProvider.client.auth.signOut()

                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)

                Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal logout: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}