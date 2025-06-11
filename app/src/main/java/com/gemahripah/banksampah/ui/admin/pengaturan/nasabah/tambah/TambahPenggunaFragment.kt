package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.tambah

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.put

class TambahPenggunaFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.hapus.visibility = View.GONE

        binding.konfirmasi.setOnClickListener {
            val nama = binding.nama.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (!isValidInput(nama, email, password)) {
                showToast("Semua field wajib diisi")
                return@setOnClickListener
            }

            tambahPengguna(nama, email, password)
        }
    }

    private fun isValidInput(nama: String, email: String, password: String): Boolean {
        return nama.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
    }

    private fun tambahPengguna(nama: String, email: String, password: String) {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = SupabaseAdminProvider.client

                val userWithEmail = client.auth.admin.createUserWithEmail {
                    this.email = email
                    this.password = password
                    userMetadata {
                        put("name", nama)
                    }
                }

                val userId = userWithEmail.id

                val penggunaBaru = Pengguna(
                    pgnId = userId,
                    pgnNama = nama,
                    pgnEmail = email
                )

                client.from("pengguna").insert(penggunaBaru)

                withContext(Dispatchers.Main) {
                    showToast("Pengguna berhasil dibuat")
                    findNavController().navigate(R.id.action_tambahPenggunaFragment_to_penggunaFragment)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Gagal membuat pengguna")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}