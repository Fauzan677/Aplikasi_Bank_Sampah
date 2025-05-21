package com.gemahripah.banksampah.ui.welcome.daftar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDaftarBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class DaftarFragment : Fragment() {

    private var _binding: FragmentDaftarBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDaftarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tombol kembali
        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_daftarFragment_to_landingFragment)
        }

        // Tombol daftar
        binding.daftar.setOnClickListener {
            val nama = binding.nama.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Sign up user ke Supabase Auth
                    SupabaseProvider.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    val session = SupabaseProvider.client.auth.currentSessionOrNull()
                    val user = session?.user

                    if (user != null) {
                        val userName = nama
                        val userEmail = email

                        // Masukkan data pengguna ke tabel "pengguna" di Supabase
                        try {
                            SupabaseProvider.client.from("pengguna").insert(
                                mapOf(
                                    "pgnNama" to userName,
                                    "pgnEmail" to userEmail
                                )
                            )

                            Toast.makeText(requireContext(), "Pendaftaran berhasil", Toast.LENGTH_SHORT).show()

                            SupabaseProvider.client.auth.signOut()
                            findNavController().navigate(R.id.action_daftarFragment_to_landingFragment)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(requireContext(), "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    } else {
                        Toast.makeText(requireContext(), "Gagal mendapatkan data pengguna", Toast.LENGTH_LONG).show()
                    }

                } catch (e: BadRequestRestException) {
                    Toast.makeText(requireContext(), "Email sudah digunakan", Toast.LENGTH_LONG).show()
                } catch (e: RestException) {
                    Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal daftar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}