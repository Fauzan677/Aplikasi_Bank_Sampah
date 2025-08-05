package com.gemahripah.banksampah.ui.welcome.masuk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentMasukBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MasukFragment : Fragment() {

    private var _binding: FragmentMasukBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMasukBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_masukFragment_to_landingFragment)
        }

        binding.masuk.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Email dan Password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }

        setupPasswordToggle()
    }

    private fun login(email: String, password: String) {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    SupabaseProvider.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    val session = SupabaseProvider.client.auth.currentSessionOrNull()
                    if (session?.user?.id == null) {
                        showToast("User ID tidak ditemukan")
                        return@repeatOnLifecycle
                    }

                    val userId = session.user!!.id
                    val pengguna = getPenggunaById(userId)
                    if (pengguna == null) {
                        showToast("Data pengguna tidak ditemukan")
                        return@repeatOnLifecycle
                    }

                    navigateToNextScreen(pengguna)
                } catch (e: BadRequestRestException) {
                    logError("BadRequest", e)
                    showToast("Email atau Password salah")
                } catch (e: RestException) {
                    logError("RestException", e)
                    showToast("Gagal masuk, periksa koneksi internet")
                } catch (e: Exception) {
                    logError("Exception", e)
                    showToast("Gagal masuk, periksa koneksi internet")
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private suspend fun getPenggunaById(userId: String): Pengguna? {
        return SupabaseProvider.client.postgrest
            .from("pengguna")
            .select {
                filter {
                    eq("pgnId", userId)
                }
                limit(1)
            }
            .decodeSingleOrNull()
    }

    private fun navigateToNextScreen(pengguna: Pengguna) {
        val isAdmin = pengguna.pgnIsAdmin ?: false
        val intent = if (isAdmin) {
            Intent(requireContext(), AdminActivity::class.java)
        } else {
            Intent(requireContext(), NasabahActivity::class.java)
        }.apply {
            putExtra("EXTRA_PENGGUNA", pengguna)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        showToast("Login berhasil")
        startActivity(intent)
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
        binding.masuk.isEnabled = !isLoading
        binding.email.isEnabled = !isLoading
        binding.password.isEnabled = !isLoading
        binding.loading.isInvisible = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun logError(tag: String, e: Exception) {
        Log.e("MasukFragment-$tag", e.localizedMessage ?: "Unknown error")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}