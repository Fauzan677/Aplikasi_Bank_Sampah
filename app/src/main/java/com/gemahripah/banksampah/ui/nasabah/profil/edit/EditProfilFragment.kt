package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NasabahViewModel by activityViewModels()
    private val editViewModel: EditProfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.judul.text = "Edit Pengguna"
        binding.hapus.visibility = View.GONE

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        viewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                binding.nama.setText(pengguna.pgnNama ?: "")
                binding.email.setText(pengguna.pgnEmail ?: "")
                editViewModel.setInitialData(pengguna)
            }
        }

        editViewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        editViewModel.successUpdate.observe(viewLifecycleOwner) { success ->
            if (success) {
                val pengguna = viewModel.pengguna.value
                val namaBaru = binding.nama.text.toString().trim()
                val emailBaru = binding.email.text.toString().trim()

                viewModel.setPengguna(
                    pengguna?.copy(pgnNama = namaBaru, pgnEmail = emailBaru)
                )

                findNavController().navigate(R.id.action_editProfilFragment_to_navigation_notifications)
            }
        }
    }

    private fun setupListeners() {
        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString().trim()
            val emailBaru = binding.email.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()

            if (namaBaru.isEmpty() || emailBaru.isEmpty()) {
                Toast.makeText(requireContext(), "Nama dan Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Perubahan")
                .setMessage("Apakah Anda yakin ingin menyimpan perubahan profil?")
                .setPositiveButton("Simpan") { _, _ ->
                    editViewModel.updateProfil(namaBaru, emailBaru, passwordBaru)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}