package com.gemahripah.banksampah.ui.admin.transaksi.keluar.edit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.databinding.FragmentPenarikanSaldoBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class EditTransaksiKeluarFragment : Fragment(), Reloadable {

    private var _binding: FragmentPenarikanSaldoBinding? = null
    private val binding get() = _binding!!

    private val args: EditTransaksiKeluarFragmentArgs by navArgs()
    private val vm: EditTransaksiKeluarViewModel by viewModels()

    private val rupiah: NumberFormat =
        NumberFormat.getNumberInstance(Locale("id","ID")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPenarikanSaldoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prefill dari args
        binding.judul.text = "Edit Penarikan Saldo"
        binding.nama.setText(args.riwayat.nama, false)
        binding.keterangan.setText(args.riwayat.tskKeterangan ?: "")

        // jumlah awal ambil dari NOMINAL
        val jumlahAwal: BigDecimal? = args.enrichedList.firstOrNull()?.dtlNominal
        binding.jumlah.setText(jumlahAwal?.toPlainString() ?: "")

        // nama tidak bisa diubah
        binding.nama.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isEnabled = false
        }

        // Init VM
        vm.setArgs(args.riwayat, args.enrichedList)

        // Actions
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.konfirmasi.setOnClickListener {
            val jumlahBaru = binding.jumlah.text.toString().toDoubleOrNull()
            val keterangan = binding.keterangan.text.toString()
            vm.submitUpdate(jumlahBaru, keterangan)
        }

        // Collect VM state & events
        collectVm()

        if (!updateInternetCard()) return
        vm.refreshSaldo()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        vm.refreshSaldo()
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    vm.isLoading.collect { showLoading(it) }
                }

                launch {
                    vm.saldoText.collect { binding.saldo.text = it }
                }

                launch {
                    vm.saldoKurang.collect { sisa ->
                        val formatted = rupiah.format(sisa)
                        binding.jumlah.requestFocus()
                        binding.jumlah.error = "Saldo tidak mencukupi. Sisa saldo: Rp $formatted"
                    }
                }

                launch {
                    vm.toast.collect { requireContext().toast(it) }
                }

                // Sukses → kunci tombol & kembali ke layar sebelumnya
                launch {
                    vm.navigateBack.collect {
                        binding.konfirmasi.isEnabled = false
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
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

    private fun Context.toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}