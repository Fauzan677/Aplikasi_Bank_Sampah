package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NasabahViewModel by activityViewModels()
    private val editViewModel: EditProfilViewModel by viewModels()

    private var isPasswordVisible = false

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
        binding.tvEmail.visibility = View.GONE
        binding.email.visibility = View.GONE
        binding.hapus.visibility = View.GONE

        observeViewModel()
        setupListeners()
        setupPasswordToggle()
    }

    private fun observeViewModel() {
        viewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                binding.nama.setText(pengguna.pgnNama ?: "")
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

                viewModel.setPengguna(
                    pengguna?.copy(pgnNama = namaBaru)
                )

                findNavController().navigate(R.id.action_editProfilFragment_to_navigation_notifications)
            }
        }

        editViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
    }

    private fun setupListeners() {
        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()

            if (namaBaru.isEmpty()) {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Perubahan")
                .setMessage("Apakah Anda yakin ingin menyimpan perubahan profil?")
                .setPositiveButton("Simpan") { _, _ ->
                    editViewModel.updateProfil(namaBaru, passwordBaru)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2

                val drawable = binding.password.compoundDrawables[drawableEnd]
                if (drawable != null && event.rawX >= (binding.password.right - drawable.bounds
                        .width() - binding.password.paddingEnd)) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        val selection = binding.password.selectionEnd
        if (isPasswordVisible) {
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType
                .TYPE_TEXT_VARIATION_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_off, 0)
        } else {
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType
                .TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable
                .visibility_on, 0)
        }
        isPasswordVisible = !isPasswordVisible
        binding.password.setSelection(selection)
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