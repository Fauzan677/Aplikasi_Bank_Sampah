package com.gemahripah.banksampah.ui.welcome.daftar

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isInvisible
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DaftarFragment : Fragment() {

    private var _binding: FragmentDaftarBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false
    private val supabaseClient = SupabaseProvider.client

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
        return if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("Semua field harus diisi")
            false
        } else true
    }

    private suspend fun handleRegistrationResult(nama: String, email: String, password: String) {
        try {
            registerUser(email, password)

            val user = supabaseClient.auth.currentSessionOrNull()?.user

            if (user != null) {
                insertUserToDatabase(nama, email)
                supabaseClient.auth.signOut()

                withContext(Dispatchers.Main) {
                    showToast("Pendaftaran berhasil")
                    findNavController().navigate(R.id.action_daftarFragment_to_landingFragment)
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("Gagal mendapatkan data pengguna")
                }
            }
        } catch (e: BadRequestRestException) {
            withContext(Dispatchers.Main) {
                showToast("Email sudah digunakan")
            }
        } catch (e: RestException) {
            withContext(Dispatchers.Main) {
                showToast("Gagal daftar, periksa koneksi internet")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("Gagal daftar, periksa koneksi internet")
            }
        } finally {
            withContext(Dispatchers.Main) {
                showLoading(false)
            }
        }
    }

    private suspend fun registerUser(email: String, password: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    private suspend fun insertUserToDatabase(nama: String, email: String) {
        try {
            supabaseClient.from("pengguna").insert(
                mapOf(
                    "pgnNama" to nama,
                    "pgnEmail" to email
                )
            )
        } catch (e: Exception) {
            throw Exception("Gagal menyimpan data pengguna")
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