package com.gemahripah.banksampah.ui.admin.transaksi.keluar

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPenarikanSaldoBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class PenarikanSaldoFragment : Fragment(), Reloadable {

    private var _binding: FragmentPenarikanSaldoBinding? = null
    private val binding get() = _binding!!

    private val vm: PenarikanSaldoViewModel by viewModels()

    private val rupiah: NumberFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPenarikanSaldoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUiActions()
        collectVm()

        if (!updateInternetCard()) return
        vm.loadPengguna()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        vm.loadPengguna()
        vm.selectedPgnId.value?.let { vm.fetchSaldo(it) }
    }

    private fun setupUiActions() {
        // Konfirmasi
        binding.konfirmasi.setOnClickListener {
            val jumlah = binding.jumlah.text.toString().toDoubleOrNull()
            val keterangan = binding.keterangan.text.toString()
            vm.submitPenarikan(jumlah, keterangan)
        }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

                // Loading
                launch {
                    vm.isLoading.collect { showLoading(it) }
                }

                // Daftar nama -> adapter (dengan dropdown saat mengetik)
                launch {
                    vm.penggunaList.collect { list ->
                        val namaList = list.mapNotNull { it.pgnNama }.distinct()
                        val adapter = ArrayAdapter(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, namaList)

                        binding.nama.apply {
                            setAdapter(adapter)
                            threshold = 1

                            // muncul saat mengetik
                            setOnTouchListener(null)
                            addTextChangedListener(onTextChanged = { text, _, _, _ ->
                                if (hasFocus()) {
                                    if (text.isNullOrEmpty()) dismissDropDown()
                                    else if (!isPopupShowing) showDropDown()
                                }
                            })
                            setOnFocusChangeListener { _, hasFocus ->
                                if (hasFocus && !text.isNullOrEmpty()) post { showDropDown() }
                            }

                            setOnItemClickListener { _: AdapterView<*>, _, position, _ ->
                                val selectedNama = adapter.getItem(position) ?: return@setOnItemClickListener
                                vm.onNamaDipilih(selectedNama)
                            }
                        }
                    }
                }

                // Saldo text
                launch {
                    vm.saldoText.collect { binding.saldo.text = it }
                }

                // Saldo kurang -> set error di jumlah
                launch {
                    vm.saldoKurang.collect { sisa ->
                        val formatted = rupiah.format(sisa)
                        binding.jumlah.requestFocus()
                        binding.jumlah.error = "Saldo tidak mencukupi. Sisa saldo: Rp $formatted"
                    }
                }

                // Toast
                launch {
                    vm.toast.collect { requireContext().toast(it) }
                }

                // Navigasi selesai + bersihkan back stack fragment ini
                launch {
                    vm.navigateBack.collect {
                        findNavController().navigate(
                            R.id.navigation_transaksi,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.penarikanSaldoFragment, true)
                                .setLaunchSingleTop(true)
                                .build()
                        )
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