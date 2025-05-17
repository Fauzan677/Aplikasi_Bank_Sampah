package com.gemahripah.banksampah.ui.nasabah.ui.profil.edit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import com.gemahripah.banksampah.ui.admin.pengaturan.profil.EditProfilFragmentArgs
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NasabahViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var userId: String? = null // Dideklarasikan di sini agar bisa diakses di seluruh fungsi

        viewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                binding.nama.setText(pengguna.pgnNama ?: "")
                binding.email.setText(pengguna.pgnEmail ?: "")
                userId = pengguna.pgnId // Diinisialisasi saat observer terpanggil
            }
        }



        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString().trim()
            val emailBaru = binding.email.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()

            if (namaBaru.isEmpty() || emailBaru.isEmpty()) {
                Toast.makeText(requireContext(), "Nama dan Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Jalankan pembaruan data dengan Supabase
            lifecycleScope.launch {
                try {
                    SupabaseProvider.client.from("pengguna").update(
                        {
                            set("pgnNama", namaBaru)
                            set("pgnEmail", emailBaru)
                        }
                    ) {
                        filter {
                            if (userId != null) {
                                eq("pgnId", userId!!)
                            }
                        }
                    }

                    if (passwordBaru.isNotEmpty()) {
                        try {
                            SupabaseProvider.client.auth.updateUser {
                                password = passwordBaru
                            }
                            Toast.makeText(requireContext(), "Password berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal update password: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Toast.makeText(requireContext(), "Data pengguna berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editProfilFragment_to_navigation_pengaturan)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}