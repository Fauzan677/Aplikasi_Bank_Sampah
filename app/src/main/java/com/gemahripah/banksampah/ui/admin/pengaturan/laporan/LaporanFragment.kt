package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentLaporanBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch

class LaporanFragment : Fragment(), Reloadable {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    private var loadingDialog: AlertDialog? = null
    private val vm: LaporanViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transaksi.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            confirm("Konfirmasi", "Apakah Anda ingin menyimpan laporan ini?") {
                vm.exportTransaksiLengkap()
            }
        }
        binding.setoran.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            confirm("Konfirmasi", "Apakah Anda ingin menyimpan laporan ini?") {
                vm.exportSetoran()
            }
        }
        binding.nasabah.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            confirm("Konfirmasi", "Apakah Anda ingin menyimpan data nasabah?") {
                vm.exportNasabah()
            }
        }
        binding.sampah.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            confirm("Konfirmasi", "Apakah Anda ingin menyimpan data sampah?") {
                vm.exportSampah()
            }
        }

        collectVm()

        if (!updateInternetCard()) return
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.isLoading.collect { if (it) showLoading() else hideLoading() } }
                launch { vm.toast.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() } }
            }
        }
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
    }

    private fun confirm(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ya") { _, _ -> onConfirm() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLoading() {
        if (loadingDialog?.isShowing == true) return
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
        loadingDialog?.window?.setDimAmount(0.3f)
        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
