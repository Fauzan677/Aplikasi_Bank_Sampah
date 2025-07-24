package com.gemahripah.banksampah.ui.admin.transaksi.detail

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
import androidx.recyclerview.widget.GridLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentDetailTransaksiBinding
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.DetailTransaksiAdapter
import kotlinx.coroutines.launch

class DetailTransaksiFragment : Fragment() {

    private var _binding: FragmentDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    private val args: DetailTransaksiFragmentArgs by navArgs()
    private val viewModel: DetailTransaksiViewModel by viewModels()

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
        viewModel.getDetailTransaksi(args.riwayat.tskId)
    }

    private fun setupUI() {
        val riwayat = args.riwayat

        binding.nama.text = riwayat.nama
        binding.tanggal.text = riwayat.tanggal
        binding.nominal.text = riwayat.totalHarga.toString()
        binding.keterangan.text = riwayat.tskKeterangan

        if (riwayat.tipe.lowercase() == "keluar") {
            binding.detail.visibility = View.GONE
            binding.rvDetail.visibility = View.GONE
            binding.jenis.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color
                .merah))
            binding.transaksi.text = "Transaksi Keluar"
        } else {
            binding.jenis.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.hijau))
            binding.transaksi.text = "Transaksi Masuk"
        }
    }

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
                            binding.rvDetail.apply {
                                layoutManager = GridLayoutManager(requireContext(), 2)
                                adapter = DetailTransaksiAdapter(state.data)
                            }

                            binding.edit.setOnClickListener {
                                val action = if (args.riwayat.tipe.lowercase() == "masuk") {
                                    DetailTransaksiFragmentDirections
                                        .actionDetailTransaksiFragmentToEditTransaksiMasukFragment(
                                            riwayat = args.riwayat,
                                            enrichedList = state.data.toTypedArray()
                                        )
                                } else {
                                    DetailTransaksiFragmentDirections
                                        .actionDetailTransaksiFragmentToEditTransaksiKeluarFragment(
                                            riwayat = args.riwayat,
                                            enrichedList = state.data.toTypedArray()
                                        )
                                }
                                findNavController().navigate(action)
                            }
                        }

                        is DetailTransaksiUiState.Deleted -> {
                            findNavController().popBackStack()
                        }

                        is DetailTransaksiUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.hapus.setOnClickListener {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}