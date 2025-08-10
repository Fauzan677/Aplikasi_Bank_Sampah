package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.tambah

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import kotlinx.coroutines.launch

class TambahPenggunaFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val vm: TambahPenggunaViewModel by viewModels()

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupPasswordToggle()
        collectVm()
    }

    private fun setupUI() {
        binding.hapus.visibility = View.GONE

        binding.konfirmasi.setOnClickListener {
            val nama = binding.nama.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            // Serahin semua ke VM (validasi + proses)
            vm.submit(nama, email, password)
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.isLoading.collect { showLoading(it) }
                }
                launch {
                    vm.toast.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    vm.navigateBack.collect {
                        // pop Edit dari backstack (inclusive) supaya tidak bisa balik ke form
                        findNavController().navigate(
                            R.id.action_tambahPenggunaFragment_to_penggunaFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.tambahPenggunaFragment, true)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding.password.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.password.compoundDrawables[drawableEnd]
                if (drawable != null &&
                    event.rawX >= (binding.password.right - drawable.bounds.width() - binding.password.paddingEnd)
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
            binding.password.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_off, 0)
        } else {
            binding.password.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.visibility_on, 0)
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
        _binding = null
        super.onDestroyView()
    }
}