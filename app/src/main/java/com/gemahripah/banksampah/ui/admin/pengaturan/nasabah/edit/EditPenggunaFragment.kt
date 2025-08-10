package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah.edit

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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentTambahPenggunaBinding
import kotlinx.coroutines.launch

class EditPenggunaFragment : Fragment() {

    private var _binding: FragmentTambahPenggunaBinding? = null
    private val binding get() = _binding!!

    private val vm: EditPenggunaViewModel by viewModels()

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

        val pengguna = arguments?.let { EditPenggunaFragmentArgs.fromBundle(it).pengguna }
        if (pengguna == null) {
            Toast.makeText(requireContext(), "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        vm.init(pengguna)
        setupUI(pengguna)
        setupPasswordToggle()
        collectVm()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI(pengguna: Pengguna) {
        binding.judul.text = "Edit Pengguna"
        binding.nama.setText(pengguna.pgnNama ?: "")
        binding.email.setText(pengguna.pgnEmail ?: "")

        binding.konfirmasi.setOnClickListener {
            val namaBaru = binding.nama.text.toString()
            val emailBaru = binding.email.text.toString()
            val passwordBaru = binding.password.text.toString()
            vm.submitEdit(namaBaru, emailBaru, passwordBaru)
        }

        binding.hapus.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus pengguna ini?")
                .setPositiveButton("Hapus") { _, _ -> vm.deleteUser() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.isLoading.collect { showLoading(it) } }
                launch { vm.toast.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() } }
                launch {
                    vm.navigateBack.collect {
                        findNavController().navigate(
                            R.id.action_editPenggunaFragment_to_penggunaFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.editPenggunaFragment, true) // hapus dari back stack
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
                val endDrawable = binding.password.compoundDrawables[drawableEnd]
                if (endDrawable != null &&
                    event.rawX >= (binding.password.right - endDrawable.bounds.width() - binding.password.paddingEnd)
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
        binding.hapus.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}