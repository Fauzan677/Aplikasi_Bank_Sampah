package com.gemahripah.banksampah.ui.welcome.daftar

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDaftarBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DaftarFragment : Fragment() {

    private var _binding: FragmentDaftarBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false

    private val viewModel: DaftarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDaftarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_daftarFragment_to_landingFragment)
        }

        binding.daftar.setOnClickListener {
            val nama = binding.nama.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (!validateInput(nama, email, password)) return@setOnClickListener

            showLoading(true)

            viewLifecycleOwner.lifecycleScope.launch {
                handleRegistrationResult(nama, email, password)
            }
        }

        setupPasswordToggle()
    }

    private fun validateInput(nama: String, email: String, password: String): Boolean {
        if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("Semua field harus diisi")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Format email tidak valid")
            return false
        }

        return true
    }


    private suspend fun handleRegistrationResult(nama: String, email: String, password: String) {
        try {
            if (viewModel.isNamaExists(nama)) {
                Log.d("DaftarFragment", "Nama sudah digunakan, proses dihentikan.")
                withContext(Dispatchers.Main) {
                    showToast("Nama sudah digunakan, gunakan nama lain")
                }
                return
            }

            Log.d("DaftarFragment", "Lanjut ke registerUser()")

            viewModel.registerUser(email, password)
            viewModel.insertUserToDatabase(nama, email)
            viewModel.signOut()

            withContext(Dispatchers.Main) {
                showToast("Pendaftaran berhasil")
                findNavController().navigate(R.id.action_daftarFragment_to_landingFragment)
            }
        } catch (e: Exception) {
            Log.e("DaftarFragment", "Registration error final: ${e.message}", e)
            withContext(Dispatchers.Main) {
                when {
                    e is AuthWeakPasswordException -> {
                        showToast("Password minimal 6 karakter")
                    }

                    e is AuthRestException && e.error.contains("already_exists", ignoreCase = true) ->
                        showToast("Email sudah digunakan")

                    e.message?.contains("Nama sudah digunakan", ignoreCase = true) == true ->
                        showToast("Nama sudah digunakan, gunakan nama lain")

                    e is BadRequestRestException ->
                        showToast("Email tidak valid atau sudah terdaftar")

                    else -> showToast("Gagal daftar, periksa koneksi internet")
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                showLoading(false)
            }
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

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.daftar.isEnabled = !isLoading
        binding.email.isEnabled = !isLoading
        binding.password.isEnabled = !isLoading
        binding.loading.isInvisible = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}