package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseAdminProvider
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditPenggunaFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private var pengguna: Pengguna? = null
    private val userId: String? get() = pengguna?.pgnId

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pengguna = arguments?.let { EditPenggunaFragmentArgs.fromBundle(it).pengguna }
        setupUI()
        isiForm()
    }

    private fun setupUI() {
        binding.judul.text = "Edit Pengguna"
        binding.hapus.visibility = View.VISIBLE

        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString().trim()
            val emailBaru = binding.email.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()

            if (userId.isNullOrEmpty()) {
                showToast("ID pengguna tidak ditemukan")
                return@setOnClickListener
            }

            if (!isDataBerubah(namaBaru, emailBaru, passwordBaru)) {
                showToast("Tidak ada perubahan data")
                return@setOnClickListener
            }

            updatePengguna(namaBaru, emailBaru, passwordBaru)
        }

        binding.hapus.setOnClickListener {
            if (userId.isNullOrEmpty()) {
                showToast("ID pengguna tidak ditemukan")
                return@setOnClickListener
            }

            hapusPengguna()
        }
    }

    private fun isiForm() {
        binding.nama.setText(pengguna?.pgnNama)
        binding.email.setText(pengguna?.pgnEmail)
    }

    private fun isDataBerubah(nama: String, email: String, password: String): Boolean {
        val namaLama = pengguna?.pgnNama ?: ""
        val emailLama = pengguna?.pgnEmail ?: ""
        return nama != namaLama || email != emailLama || password.isNotEmpty()
    }

    private fun updatePengguna(nama: String, email: String, password: String) {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = SupabaseProvider.client
                val adminClient = SupabaseAdminProvider.client

                if (nama != pengguna?.pgnNama || email != pengguna?.pgnEmail) {
                    client.from("pengguna").update({
                        if (nama != pengguna?.pgnNama) set("pgnNama", nama)
                        if (email != pengguna?.pgnEmail) set("pgnEmail", email)
                    }) {
                        filter { eq("pgnId", userId!!) }
                    }
                }

                if (email != pengguna?.pgnEmail || password.isNotEmpty()) {
                    adminClient.auth.admin.updateUserById(uid = userId!!) {
                        if (email != pengguna?.pgnEmail) this.email = email
                        if (password.isNotEmpty()) this.password = password
                    }
                }

                withContext(Dispatchers.Main) {
                    showToast("Pengguna berhasil diperbarui")
                    navigateBack()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Gagal memperbarui pengguna, periksa koneksi internet")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun hapusPengguna() {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = SupabaseProvider.client
                val adminClient = SupabaseAdminProvider.client

                client.from("pengguna").delete {
                    filter { eq("pgnId", userId!!) }
                }

                adminClient.auth.admin.deleteUser(uid = userId!!)

                withContext(Dispatchers.Main) {
                    showToast("Pengguna berhasil dihapus")
                    navigateBack()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Gagal menghapus pengguna, periksa koneksi internet")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun navigateBack() {
        findNavController().navigate(
            R.id.action_editPenggunaFragment_to_penggunaFragment,
            null,
            androidx.navigation.navOptions {
                popUpTo(R.id.editPenggunaFragment) { inclusive = true }
            }
        )
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