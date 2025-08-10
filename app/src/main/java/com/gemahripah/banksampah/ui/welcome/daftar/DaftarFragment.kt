package com.gemahripah.banksampah.ui.welcome.daftar

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentDaftarBinding

class DaftarFragment : Fragment() {

    private var _binding: FragmentDaftarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DaftarViewModel by viewModels()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDaftarBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
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
            viewModel.register(nama, email, password)
        }

        setupPasswordToggle()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { showLoading(it) }

        viewModel.toast.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                showToast(it)
                viewModel.consumeToast()
            }
        }

        viewModel.registerSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                findNavController().navigate(R.id.action_daftarFragment_to_landingFragment)
                viewModel.consumeSuccess()
            }
        }
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding.password.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val end = 2
                val d = binding.password.compoundDrawables[end]
                if (d != null && event.rawX >= (binding.password.right - d.bounds.width() - binding.password.paddingEnd)) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        val sel = binding.password.selectionEnd
        if (isPasswordVisible) {
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_off, 0)
        } else {
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_on, 0)
        }
        isPasswordVisible = !isPasswordVisible
        binding.password.setSelection(sel)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.daftar.isEnabled = !isLoading
        binding.nama.isEnabled = !isLoading
        binding.email.isEnabled = !isLoading
        binding.password.isEnabled = !isLoading
        binding.loading.isInvisible = !isLoading
    }

    private fun showToast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
