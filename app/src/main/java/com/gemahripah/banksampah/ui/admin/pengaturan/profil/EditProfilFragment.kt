package com.gemahripah.banksampah.ui.admin.pengaturan.profil

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import com.gemahripah.banksampah.ui.admin.AdminViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val adminViewModel: AdminViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.judul.text = "Edit Pengguna"
        binding.hapus.visibility = View.GONE

        adminViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                binding.nama.setText(pengguna.pgnNama ?: "")
                binding.email.setText(pengguna.pgnEmail ?: "")

                binding.konfirmasi.setOnClickListener {
                    updatePengguna(pengguna)
                }
            }
        }
    }

    private fun updatePengguna(pengguna: Pengguna) {
        val namaBaru = binding.nama.text.toString().trim()
        val emailBaru = binding.email.text.toString().trim()
        val passwordBaru = binding.password.text.toString().trim()

        if (namaBaru.isEmpty() || emailBaru.isEmpty()) {
            Toast.makeText(requireContext(), "Nama dan Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val isNamaBerubah = namaBaru != pengguna.pgnNama
        val isEmailBerubah = emailBaru != pengguna.pgnEmail
        val isPasswordBerubah = passwordBaru.isNotEmpty()

        if (!isNamaBerubah && !isEmailBerubah && !isPasswordBerubah) {
            Toast.makeText(requireContext(), "Tidak ada perubahan data", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)

            try {
                val userId = pengguna.pgnId

                if (isNamaBerubah || isEmailBerubah) {
                    SupabaseProvider.client.from("pengguna").update({
                        if (isNamaBerubah) set("pgnNama", namaBaru)
                        if (isEmailBerubah) set("pgnEmail", emailBaru)
                    }) {
                        filter {
                            if (userId != null) {
                                eq("pgnId", userId)
                            }
                        }
                    }
                }

                if (isEmailBerubah || isPasswordBerubah) {
                    try {
                        SupabaseProvider.client.auth.updateUser {
                            if (isEmailBerubah) email = emailBaru
                            if (isPasswordBerubah) password = passwordBaru
                        }
                    } catch (_: Exception) {
                    }
                }

                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.editProfilFragment, true)
                    .build()

                Toast.makeText(requireContext(), "Data pengguna berhasil diperbarui", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.navigation_pengaturan, null, navOptions)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal edit pengguna, periksa koneksi internet", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}