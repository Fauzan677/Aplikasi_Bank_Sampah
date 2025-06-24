package com.gemahripah.banksampah.ui.admin.beranda.detail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentDetailPenggunaBinding
import com.gemahripah.banksampah.ui.admin.beranda.adapter.TotalSampahAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.RiwayatTransaksiAdapter
import kotlinx.coroutines.launch

class DetailPenggunaFragment : Fragment() {

    private var _binding: FragmentDetailPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailPenggunaViewModel by viewModels()

    private var pengguna: Pengguna? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pengguna = arguments?.let { DetailPenggunaFragmentArgs.fromBundle(it).pengguna }

        setupUI()
        setupClickListeners()
        setupRecyclerViews()
        setupObservers()

        pengguna?.pgnId?.let { viewModel.loadData(it) }
    }

    private fun setupUI() {
        binding.nama.text = pengguna?.pgnNama ?: "Tidak diketahui"
    }

    private fun setupClickListeners() {
        binding.menabung.setOnClickListener {
            pengguna?.let {
                val action = DetailPenggunaFragmentDirections
                    .actionDetailPenggunaFragmentToSetorSampahFragment(it)
                findNavController().navigate(action)
            }
        }

        binding.menarik.setOnClickListener {
            pengguna?.let {
                val action = DetailPenggunaFragmentDirections
                    .actionDetailPenggunaFragmentToPenarikanSaldoFragment(it)
                findNavController().navigate(action)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.rvTotal.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.post {
            val maxHeight = resources.getDimensionPixelSize(R.dimen.recycler_max_height)
            if (binding.rvRiwayat.height > maxHeight) {
                binding.rvRiwayat.layoutParams.height = maxHeight
                binding.rvRiwayat.requestLayout()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.saldo.collect { binding.nominal.text = it }
                }
                launch {
                    viewModel.totalSampah.collect { list ->
                        if (list.isNotEmpty()) {
                            binding.rvTotal.adapter = TotalSampahAdapter(list)
                            binding.rvTotal.visibility = View.VISIBLE
                            binding.textKosongTotal.visibility = View.GONE
                        } else {
                            binding.rvTotal.visibility = View.GONE
                            binding.textKosongTotal.visibility = View.VISIBLE
                        }
                    }
                }
                launch {
                    viewModel.riwayatTransaksi.collect { list ->
                        if (list.isNotEmpty()) {
                            binding.rvRiwayat.adapter = RiwayatTransaksiAdapter(list) { riwayat ->
                                val action = DetailPenggunaFragmentDirections
                                    .actionDetailPenggunaFragmentToDetailTransaksiFragment(riwayat)
                                findNavController().navigate(action)
                            }
                            binding.rvRiwayat.visibility = View.VISIBLE
                            binding.textKosongRiwayat.visibility = View.GONE
                        } else {
                            binding.rvRiwayat.visibility = View.GONE
                            binding.textKosongRiwayat.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}