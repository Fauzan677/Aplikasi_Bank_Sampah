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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

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

        // Ubah judul & sembunyikan field yang tidak dipakai
        binding.judul.text = "Ubah Password"

        binding.tvRekening.visibility = View.GONE
        binding.rekening.visibility = View.GONE

        binding.tvNama.visibility = View.GONE
        binding.nama.visibility = View.GONE

        binding.tvEmail.visibility = View.GONE
        binding.email.visibility = View.GONE

        binding.tvAlamat.visibility = View.GONE
        binding.alamat.visibility = View.GONE

        binding.tvSaldo.visibility = View.GONE
        binding.saldo.visibility = View.GONE

        binding.hapus.visibility = View.GONE

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        observeViewModel()
        setupListeners()
        setupPasswordToggle()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    editViewModel.isLoading.collectLatest { loading ->
                        binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
                        binding.layoutKonten.alpha = if (loading) 0.3f else 1f
                        binding.konfirmasi.isEnabled = !loading
                    }
                }
                launch {
                    editViewModel.toast.collectLatest { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    editViewModel.success.collectLatest {
                        // Selesai: kembali ke layar sebelumnya
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.konfirmasi.setOnClickListener {
            val passwordBaru = binding.password.text.toString().trim()

            if (passwordBaru.isEmpty()) {
                Toast.makeText(requireContext(), "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Ubah password akun Anda?")
                .setPositiveButton("Ubah") { _, _ ->
                    editViewModel.updatePassword(passwordBaru)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_off, 0)
        binding.password.setOnTouchListener { _, event ->
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