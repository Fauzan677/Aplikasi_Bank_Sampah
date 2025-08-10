package com.gemahripah.banksampah.ui.nasabah.profil.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val nasabahViewModel: NasabahViewModel by activityViewModels()
    private val editViewModel: EditProfilViewModel by viewModels()

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
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
        // Prefill data pengguna saat tersedia
        nasabahViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            pengguna?.let {
                binding.nama.setText(it.pgnNama.orEmpty())
                editViewModel.setInitialData(it)
            }
        }

        // Collect state & event dengan lifecycle-aware
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Loading
                launch {
                    editViewModel.isLoading.collectLatest { loading ->
                        binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
                        binding.layoutKonten.alpha = if (loading) 0.3f else 1f
                        binding.konfirmasi.isEnabled = !loading
                    }
                }
                // Toast event
                launch {
                    editViewModel.toast.collectLatest { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
                // Success event -> update VM bersama + navigate
                launch {
                    editViewModel.success.collectLatest {
                        val current = nasabahViewModel.pengguna.value
                        val namaBaru = binding.nama.text.toString().trim()
                        nasabahViewModel.setPengguna(current?.copy(pgnNama = namaBaru))

                        findNavController().navigate(
                            R.id.action_editProfilFragment_to_navigation_notifications
                        )
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()

            if (namaBaru.isEmpty()) {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
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
        binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_off, 0)
        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEndIndex = 2
                val drawable = binding.password.compoundDrawables[drawableEndIndex]
                val tapped = event.rawX >= (binding.password.right - drawable.bounds.width() - binding.password.paddingEnd)
                if (drawable != null && tapped) {
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
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_off, 0)
        } else {
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_on, 0)
        }
        isPasswordVisible = !isPasswordVisible
        binding.password.setSelection(selection)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
