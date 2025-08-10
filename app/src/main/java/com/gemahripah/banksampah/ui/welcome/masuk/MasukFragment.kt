package com.gemahripah.banksampah.ui.welcome.masuk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentMasukBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity

class MasukFragment : Fragment() {

    private var _binding: FragmentMasukBinding? = null
    private val binding get() = _binding!!

    private val masukViewModel: MasukViewModel by viewModels()

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMasukBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
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
            // trigger login di ViewModel
            masukViewModel.login(email, password)
        }

        setupPasswordToggle()
        observeViewModel()
    }

    private fun observeViewModel() {
        masukViewModel.isLoading.observe(viewLifecycleOwner) { showLoading(it) }

        masukViewModel.toast.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                showToast(it)
                masukViewModel.consumeToast()
            }
        }

        masukViewModel.loginSuccess.observe(viewLifecycleOwner) { pengguna ->
            pengguna?.let {
                navigateToNextScreen(it)
                masukViewModel.consumeLoginSuccess()
            }
        }
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
        startActivity(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEndIndex = 2
                val drawableEnd = binding.password.compoundDrawables[drawableEndIndex]
                if (drawableEnd != null &&
                    event.rawX >= (binding.password.right - drawableEnd.bounds.width() - binding.password.paddingEnd)
                ) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
