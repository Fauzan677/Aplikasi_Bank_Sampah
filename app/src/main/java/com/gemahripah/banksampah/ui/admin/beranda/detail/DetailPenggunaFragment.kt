package com.gemahripah.banksampah.ui.admin.beranda.detail

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentDetailPenggunaBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.admin.beranda.adapter.TotalSampahAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.RiwayatPagingAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class DetailPenggunaFragment : Fragment(), Reloadable {

    private var _binding: FragmentDetailPenggunaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailPenggunaViewModel by viewModels()

    private lateinit var pagingAdapter: RiwayatPagingAdapter

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

        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        pengguna?.pgnId?.let { id -> viewModel.loadPagingRiwayat(id) }

        if (updateInternetCard()) {
            pengguna?.pgnId?.let { viewModel.loadData(it) }
        }
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        pengguna?.pgnId?.let { id ->
            viewModel.loadData(id)
            pagingAdapter.refresh()
        }
        binding.swipeRefresh.isRefreshing = false
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

        pagingAdapter = RiwayatPagingAdapter { item ->
            val action = DetailPenggunaFragmentDirections
                .actionDetailPenggunaFragmentToDetailTransaksiFragment(item)
            findNavController().navigate(action)
        }

        binding.rvRiwayat.adapter = pagingAdapter.withLoadStateHeaderAndFooter(
            header = LoadingStateAdapter { pagingAdapter.retry() },
            footer = LoadingStateAdapter { pagingAdapter.retry() }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.saldo.collect {
                        binding.nominal.text = " Rp $it"
                    }
                }

                launch {
                    viewModel.pagingData.collectLatest { paging ->
                        pagingAdapter.submitData(paging)
                    }
                }

                launch {
                    viewModel.totalSampah.collectLatest { list ->
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
                    pagingAdapter.loadStateFlow
                        .distinctUntilChangedBy { it.refresh }
                        .collectLatest { loadState ->
                            when (loadState.refresh) {
                                is LoadState.Loading -> showLoading(true)
                                is LoadState.NotLoading -> showLoading(false)
                                is LoadState.Error -> {
                                    showLoading(false)
                                }
                            }
                        }
                }

                launch {
                    pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                        val isEmpty = loadStates.refresh is LoadState.NotLoading &&
                                pagingAdapter.itemCount == 0
                        binding.textKosongRiwayat.visibility = if (isEmpty) View.VISIBLE else View.GONE
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

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.menarik.isEnabled = !isLoading
        binding.menabung.isEnabled = !isLoading
    }

    override fun onResume() {
        super.onResume()
        reloadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}