package com.gemahripah.banksampah.ui.admin.pengaturan.profil

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseAdminProvider
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import com.gemahripah.banksampah.ui.admin.AdminViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfilFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val adminViewModel: AdminViewModel by activityViewModels()
    private val vm: EditProfilViewModel by viewModels()

    private var isPasswordVisible = false
    private var currentPengguna: Pengguna? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.judul.text = "Edit Pengguna"
        binding.hapus.visibility = View.GONE

        // Prefill dari AdminViewModel (activity scope)
        adminViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            currentPengguna = pengguna
            if (pengguna != null) {
                binding.nama.setText(pengguna.pgnNama ?: "")
                binding.email.setText(pengguna.pgnEmail ?: "")
            }
        }

        // Actions
        binding.konfirmasi.setOnClickListener {
            val pengguna = currentPengguna ?: return@setOnClickListener
            val namaBaru = binding.nama.text.toString().trim()
            val emailBaru = binding.email.text.toString().trim()
            val passwordBaru = binding.password.text.toString().trim()
            vm.submitUpdate(pengguna, namaBaru, emailBaru, passwordBaru)
        }

        setupPasswordToggle()
        collectVm()
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
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.editProfilFragment, true)
                            .build()
                        findNavController().navigate(R.id.navigation_pengaturan, null, navOptions)
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
        binding.konfirmasi.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}