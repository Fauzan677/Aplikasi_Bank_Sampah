package com.gemahripah.banksampah.ui.nasabah.pengumuman

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.databinding.FragmentPengumumanBinding
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.pengumuman.PengumumanPagingAdapter
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PengumumanFragment : Fragment(), Reloadable {

    private var _binding: FragmentPengumumanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PengumumanViewModel by viewModels()
    private lateinit var pagingAdapter: PengumumanPagingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()

        binding.swipeRefresh.setOnRefreshListener { reloadData() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1) Listen loadState untuk state UI & swipeRefresh
                launch {
                    pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                        val isLoading = loadStates.refresh is LoadState.Loading
                        val isError = loadStates.refresh is LoadState.Error
                        val isEmpty = loadStates.refresh is LoadState.NotLoading && pagingAdapter.itemCount == 0

                        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.rvPengumuman.visibility = if (!isLoading && !isError) View.VISIBLE else View.GONE
                        binding.pengumumanKosong.visibility = if (isEmpty) View.VISIBLE else View.GONE

                        // hentikan animasi swipeRefresh ketika selesai refresh
                        if (!isLoading) binding.swipeRefresh.isRefreshing = false
                    }
                }

                // 2) Collect data paging dari ViewModel
                launch {
                    viewModel.pagingData().collectLatest { data ->
                        pagingAdapter.submitData(data)
                    }
                }
            }
        }

        updateInternetCard()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        pagingAdapter.refresh()
    }

    private fun setupUI() {
        binding.tambah.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        pagingAdapter = PengumumanPagingAdapter { pengumuman ->
            val action = PengumumanFragmentDirections
                .actionNavigationDashboardToDetailPengumumanFragment(pengumuman)
            findNavController().navigate(action)
        }

        binding.rvPengumuman.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pagingAdapter.withLoadStateHeaderAndFooter(
                header = LoadingStateAdapter { pagingAdapter.retry() },
                footer = LoadingStateAdapter { pagingAdapter.retry() }
            )
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? NasabahActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}