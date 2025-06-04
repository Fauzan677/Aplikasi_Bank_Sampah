package com.gemahripah.banksampah.ui.admin.beranda

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentBerandaAdminBinding
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.ui.admin.beranda.adapter.NasabahAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.R
import kotlinx.coroutines.withContext

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaAdminBinding? = null
    private val binding get() = _binding!!

    private var nasabahList = listOf<Pengguna>()
    private lateinit var nasabahAdapter: NasabahAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaAdminBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchListener()
        setupRecyclerView()
        setupFocusScroll()
        setupFilterTransaksi()
        setupFilterSetoran()
        fetchAllDashboardData()
    }

    private fun setupSearchListener() {
        binding.searchNasabah.addTextChangedListener { text ->
            nasabahAdapter.filterList(text.toString())
        }

        binding.searchNasabah.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchNasabah.text.toString()
                nasabahAdapter.filterList(query)

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }
    }

    private fun setupRecyclerView() {
        nasabahAdapter = NasabahAdapter(nasabahList) { pengguna ->
            val action = BerandaFragmentDirections.actionNavigationBerandaToDetailPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }

        binding.rvListNasabah.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = nasabahAdapter
        }
    }

    private fun setupFocusScroll() {
        binding.searchNasabah.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollView.postDelayed({
                    binding.scrollView.smoothScrollTo(0, binding.listNasabah.top)
                }, 200)
            }
        }
    }

    private fun setupFilterTransaksi() {
        binding.spinnerFilterTransaksi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (parent.getItemAtPosition(position).toString()) {
                    "Transaksi Masuk" -> ambilTotalTransaksiMasuk()
                    "Transaksi Keluar" -> ambilTotalTransaksiKeluar()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupFilterSetoran() {
        binding.spinnerFilterSetoran.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val filterWaktu = when (parent.getItemAtPosition(position).toString()) {
                    "Hari Ini" -> "hari_ini"
                    "Minggu Ini" -> "minggu_ini"
                    "Bulan Ini" -> "bulan_ini"
                    "3 Bulan Terakhir" -> "3_bulan"
                    "Total Setoran" -> "semua"
                    else -> "semua"
                }
                ambilTotalSetoranDenganFilter(filterWaktu)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchAllDashboardData() {
        ambilTotalSaldo()
        ambilTotalTransaksiMasuk()
        ambilTotalTransaksiKeluar()
        ambilTotalNasabah()
        ambilPengguna()

        binding.scrollView.scrollTo(0, 0)
    }

    private fun ambilPengguna() {
        binding.progressNasabah.visibility = View.VISIBLE

        lifecycleScope.launch {

            try {
                val penggunaList = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .from("pengguna")
                        .select {
                            filter { eq("pgnIsAdmin", "False") }
                        }
                        .decodeList<Pengguna>()
                }

                nasabahAdapter.updateData(penggunaList)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat data pengguna", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressNasabah.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ambilTotalNasabah() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_nasabah")
                val jumlah = result.data.toString().toIntOrNull() ?: 0

                launch(Dispatchers.Main) {
                    binding.nasabah.text = jumlah.toString()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.nasabah.text = "Gagal memuat"
                }
            }
        }
    }

    private fun ambilTotalTransaksiMasuk() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_transaksi_masuk")
                val totalMasuk = result.data.toDoubleOrNull() ?: 0.0
                launch(Dispatchers.Main) {
                    binding.transaksi.text = formatRupiah(totalMasuk)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.transaksi.text = getString(R.string.failed_to_load)
                }
            }
        }
    }

    private fun ambilTotalTransaksiKeluar() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseProvider.client.postgrest.rpc(
                    function = "hitung_total_jumlah_berdasarkan_tipe",
                    parameters = mapOf("tipe_transaksi" to "Keluar")
                )
                val totalKeluar = result.data.toDoubleOrNull() ?: 0.0
                launch(Dispatchers.Main) {
                    binding.transaksi.text = formatRupiah(totalKeluar)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.transaksi.text = getString(R.string.failed_to_load)
                }
            }
        }
    }

    private fun ambilTotalSetoranDenganFilter(filter: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseProvider.client.postgrest.rpc(
                    function = "hitung_total_jumlah_berdasarkan_tipe",
                    parameters = mapOf(
                        "tipe_transaksi" to "Masuk",
                        "filter_waktu" to filter
                    )
                )
                val total = result.data.toDoubleOrNull()
                withContext(Dispatchers.Main) {
                    binding.setoran.text = total?.let { "$it Kg" } ?: "0 Kg"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.setoran.text = getString(R.string.failed_to_load)
                }
            }
        }
    }

    private fun ambilTotalSaldo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_saldo_pengguna")
                val saldo = result.data.toDoubleOrNull()

                launch(Dispatchers.Main) {
                    binding.nominal.text = saldo?.let { formatRupiah(it) }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.nominal.text = getString(R.string.failed_to_load)
                }
            }
        }
    }

    private fun formatRupiah(number: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(number)
    }

    override fun onResume() {
        super.onResume()
        binding.searchNasabah.setText("")
        ambilPengguna()

        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_UP)
        }

        binding.rvListNasabah.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}