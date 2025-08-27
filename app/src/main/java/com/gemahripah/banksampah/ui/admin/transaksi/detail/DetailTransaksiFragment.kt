package com.gemahripah.banksampah.ui.admin.transaksi.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentDetailTransaksiBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.DetailTransaksiAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class DetailTransaksiFragment : Fragment(), Reloadable {

    private var _binding: FragmentDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    private val args: DetailTransaksiFragmentArgs by navArgs()
    private val viewModel: DetailTransaksiViewModel by viewModels()

    private var latestKeterangan: String? = null
    private var latestTotalNominal: BigDecimal? = null


    private val nf2 = NumberFormat.getNumberInstance(Locale("id","ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        observeUiState()
        setupClickListeners()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (!updateInternetCard()) return@setOnRefreshListener
            reloadData()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val riwayat = args.riwayat

        binding.nama.text = riwayat.nama
        binding.tanggal.text = riwayat.tanggal

        val formattedHarga = nf2.format(riwayat.totalHarga ?: BigDecimal.ZERO)
        binding.nominal.text = "Rp $formattedHarga"

        if (riwayat.tskKeterangan.isNullOrBlank()) {
            binding.keterangan.visibility = View.GONE
            binding.textKosongKeterangan.visibility = View.VISIBLE
        } else {
            binding.keterangan.text = riwayat.tskKeterangan
            binding.keterangan.visibility = View.VISIBLE
        }

        if (riwayat.tipe.equals("keluar", ignoreCase = true)) {
            binding.detail.visibility = View.GONE
            binding.rvDetail.visibility = View.GONE
            binding.jenis.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.merah))
            binding.transaksi.text = "Transaksi Keluar"
        } else {
            binding.jenis.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.hijau))
            binding.transaksi.text = "Transaksi Masuk"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isLoading = state is DetailTransaksiUiState.Loading

                    binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
                    binding.layoutKonten.isEnabled = !isLoading

                    when (state) {
                        is DetailTransaksiUiState.Loading -> Unit

                        is DetailTransaksiUiState.Success -> {
                            // set adapter seperti biasa
                            binding.rvDetail.apply {
                                layoutManager = LinearLayoutManager(requireContext())
                                adapter = DetailTransaksiAdapter(state.data)
                            }

                            val totalNominal = state.data.fold(BigDecimal.ZERO) { acc, d ->
                                acc + (d.dtlNominal ?: BigDecimal.ZERO)
                            }
                            binding.nominal.text = "Rp ${nf2.format(totalNominal)}"
                            latestTotalNominal = totalNominal

                            val ketBaru = state.data.firstOrNull()?.dtlTskId?.tskKeterangan
                            latestKeterangan = ketBaru
                            if (ketBaru.isNullOrBlank()) {
                                binding.keterangan.visibility = View.GONE
                                binding.textKosongKeterangan.visibility = View.VISIBLE
                            } else {
                                binding.keterangan.text = ketBaru
                                binding.keterangan.visibility = View.VISIBLE
                                binding.textKosongKeterangan.visibility = View.GONE
                            }

                            binding.edit.setOnClickListener {
                                val riwayatUpdated = args.riwayat.copy(
                                    tskKeterangan = latestKeterangan,
                                    totalHarga    = latestTotalNominal
                                )

                                val action = if (riwayatUpdated.tipe.lowercase() == "masuk") {
                                    DetailTransaksiFragmentDirections
                                        .actionDetailTransaksiFragmentToEditTransaksiMasukFragment(
                                            riwayat = riwayatUpdated,
                                            enrichedList = state.data.toTypedArray()
                                        )
                                } else {
                                    DetailTransaksiFragmentDirections
                                        .actionDetailTransaksiFragmentToEditTransaksiKeluarFragment(
                                            riwayat = riwayatUpdated,
                                            enrichedList = state.data.toTypedArray()
                                        )
                                }
                                findNavController().navigate(action)
                            }
                        }

                        is DetailTransaksiUiState.Deleted -> {
                            binding.loading.visibility = View.GONE
                            Toast.makeText(requireContext(), "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }

                        is DetailTransaksiUiState.Error -> {
                        }
                    }
                }
            }
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    private fun setupClickListeners() {
        binding.hapus.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener

            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Transaksi")
                .setMessage("Yakin ingin menghapus transaksi ini?")
                .setPositiveButton("Hapus") { _, _ ->
                    viewModel.deleteTransaksi(args.riwayat.tskId)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        viewModel.getDetailTransaksi(args.riwayat.tskId)
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        if (!updateInternetCard()) return
        viewModel.getDetailTransaksi(args.riwayat.tskId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}