package com.gemahripah.banksampah.ui.nasabah.beranda.detail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailTransaksiBinding
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.DetailTransaksiAdapter
import com.gemahripah.banksampah.ui.admin.transaksi.detail.DetailTransaksiFragmentArgs
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DetailTransaksiFragment : Fragment() {

    private val args: DetailTransaksiFragmentArgs by navArgs()
    private var _binding: FragmentDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailTransaksiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
        loadData()
    }

    private fun setupUI() {
        val riwayat = args.riwayat

        binding.edit.visibility = View.GONE
        binding.hapus.visibility = View.GONE
        binding.nominal.text = riwayat.totalHarga.toString()
        binding.keterangan.text = riwayat.tskKeterangan

        when (riwayat.tipe.lowercase()) {
            "masuk" -> {
                binding.jenis.setCardBackgroundColor(resources.getColor(R.color.hijau, null))
                binding.transaksi.text = "Transaksi Masuk"
            }
            "keluar" -> {
                binding.detail.visibility = View.GONE
                binding.rvDetail.visibility = View.GONE
                binding.jenis.setCardBackgroundColor(resources.getColor(R.color.merah, null))
                binding.transaksi.text = "Transaksi Keluar"
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.detailList.observe(viewLifecycleOwner) { list ->
            setupRecyclerView(list)
        }
    }

    private fun loadData() {
        val idTransaksi = args.riwayat.tskId
        viewModel.loadDetailTransaksi(idTransaksi)
    }

    private fun setupRecyclerView(detailList: List<DetailTransaksiRelasi>) {
        binding.rvDetail.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = DetailTransaksiAdapter(detailList)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}