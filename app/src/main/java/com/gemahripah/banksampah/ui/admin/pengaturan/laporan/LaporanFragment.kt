package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentLaporanBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch

class LaporanFragment : Fragment(), Reloadable {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AlertDialog? = null
    private val vm: LaporanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.chipFilter.setOnClickListener { showDateRangePicker() }
        binding.chipFilter.setOnCloseIconClickListener { vm.clearDateRange() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.dateRange.collect { r ->
                    binding.chipFilter.text = if (r == null) "Semua tanggal"
                    else "${r.start.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))} – ${r.end.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
                }
            }
        }

        // Tombol: PREVIEW dulu
        binding.transaksi.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            vm.previewTransaksi()
        }
        binding.setoran.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            vm.previewSetoran()
        }
        binding.nasabah.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            vm.previewNasabah()
        }
        binding.sampah.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            vm.previewSampah()
        }

        collectVm()
        if (!updateInternetCard()) return
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.isLoading.collect { if (it) showLoading() else hideLoading() } }
                launch { vm.toast.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() } }
                // Tampilkan dialog preview saat diterima (title saja; HTML diambil di dialog dari VM)
                launch {
                    vm.preview.collect { signal ->
                        PreviewDialogFragment.newInstance(signal.title)
                            .show(childFragmentManager, "preview_laporan")
                    }
                }
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih rentang tanggal")
            .build()
        picker.addOnPositiveButtonClickListener { sel ->
            val start = sel?.first ?: return@addOnPositiveButtonClickListener
            val end = sel.second ?: sel.first
            vm.setDateRangeByEpoch(start, end)
        }
        picker.show(childFragmentManager, "date_range_picker")
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
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

    private fun hideLoading() { loadingDialog?.dismiss() }

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