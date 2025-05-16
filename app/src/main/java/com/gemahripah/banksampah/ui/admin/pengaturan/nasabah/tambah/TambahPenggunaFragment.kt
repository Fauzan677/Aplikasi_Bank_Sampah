package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.tambah

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseAdminProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

class TambahPenggunaFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TambahPenggunaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)

        binding.konfirmasi.setOnClickListener {
            val nama = binding.nama.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val client = SupabaseAdminProvider.client

                    // 1. Buat user dengan email & password
                    val userWithEmail = client.auth.admin.createUserWithEmail {
                        this.email = email
                        this.password = password
                        userMetadata {
                            put("name", nama)
                        }
                    }

                    // 2. Ambil ID dari user yang baru dibuat
                    val userId = userWithEmail.id

                    // 3. Buat objek Pengguna dan masukkan ke tabel "pengguna"
                    val penggunaBaru = Pengguna(
                        pgnId = userId,
                        pgnNama = nama,
                        pgnEmail = email
                        // created_at dan pgnIsAdmin dibiarkan null
                    )

                    client.from("pengguna").insert(penggunaBaru)

                    Toast.makeText(requireContext(), "Pengguna berhasil dibuat", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_tambahPenggunaFragment_to_penggunaFragment)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal membuat pengguna", Toast.LENGTH_SHORT).show()
                }
            }

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}