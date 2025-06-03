package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseAdminProvider
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
class EditPenggunaFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditPenggunaViewModel by viewModels()
    private var pengguna: Pengguna? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)

        binding.judul.text = "Edit Pengguna"

        pengguna = arguments?.let {
            EditPenggunaFragmentArgs.fromBundle(it).pengguna
        }
        val userId = pengguna?.pgnId

        binding.nama.setText(pengguna?.pgnNama)
        binding.email.setText(pengguna?.pgnEmail)

        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString().trim()
            val emailBaru = binding.email.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()

            if (userId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "ID pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val namaLama = pengguna?.pgnNama ?: ""
            val emailLama = pengguna?.pgnEmail ?: ""

            val isNamaBerubah = namaBaru != namaLama
            val isEmailBerubah = emailBaru != emailLama
            val isPasswordBerubah = passwordBaru.isNotEmpty()

            if (!isNamaBerubah && !isEmailBerubah && !isPasswordBerubah) {
                Toast.makeText(requireContext(), "Tidak ada perubahan data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val client = SupabaseProvider.client
                    val adminClient = SupabaseAdminProvider.client

                    if (isNamaBerubah || isEmailBerubah) {
                        client.from("pengguna").update(
                            {
                                if (isNamaBerubah) set("pgnNama", namaBaru)
                                if (isEmailBerubah) set("pgnEmail", emailBaru)
                            }
                        ) {
                            filter {
                                eq("pgnId", userId)
                            }
                        }
                    }

                    if (isEmailBerubah || isPasswordBerubah) {
                        adminClient.auth.admin.updateUserById(uid = userId) {
                            if (isEmailBerubah) email = emailBaru
                            if (isPasswordBerubah) password = passwordBaru
                        }
                    }

                    findNavController().navigate(
                        R.id.action_editPenggunaFragment_to_penggunaFragment,
                        null,
                        androidx.navigation.navOptions {
                            popUpTo(R.id.editPenggunaFragment) {
                                inclusive = true
                            }
                        }
                    )

                    Toast.makeText(requireContext(), "Pengguna berhasil diperbarui", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memperbarui pengguna", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.hapus.setOnClickListener {

            if (userId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "ID pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val client = SupabaseProvider.client
                    val adminClient = SupabaseAdminProvider.client

                    client.from("pengguna").delete {
                        filter {
                            eq("pgnId", userId)
                        }
                    }

                    adminClient.auth.admin.deleteUser(uid = userId)

                    findNavController().navigate(
                        R.id.action_editPenggunaFragment_to_penggunaFragment,
                        null,
                        androidx.navigation.navOptions {
                            popUpTo(R.id.editPenggunaFragment) {
                                inclusive = true
                            }
                        }
                    )

                    Toast.makeText(requireContext(), "Pengguna berhasil dihapus", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal menghapus pengguna", Toast.LENGTH_SHORT).show()
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