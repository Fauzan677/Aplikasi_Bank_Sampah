package com.gemahripah.banksampah.ui.admin.pengumuman

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
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPengumumanBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.pengumuman.PengumumanPagingAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PengumumanFragment : Fragment(), Reloadable {

    private var _binding: FragmentPengumumanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PengumumanViewModel by viewModels()
    private lateinit var adapter: PengumumanPagingAdapter

    private lateinit var loadStateListener: (CombinedLoadStates) -> Unit

    val client = SupabaseProvider.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tambah.setOnClickListener {
            findNavController().navigate(
                R.id.action_navigation_pengumuman_to_tambahPengumumanFragment
            )
        }

        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }

        adapter = PengumumanPagingAdapter { pengumuman ->
            val action = PengumumanFragmentDirections
                .actionNavigationPengumumanToDetailPengumumanFragment(pengumuman)
            findNavController().navigate(action)
        }

        binding.rvPengumuman.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPengumuman.adapter = adapter.withLoadStateHeaderAndFooter(
            header = LoadingStateAdapter { adapter.retry() },
            footer = LoadingStateAdapter { adapter.retry() }
        )

        loadStateListener = { state ->
            _binding?.let { b ->
                val isLoading = state.refresh is LoadState.Loading
                val isError = state.refresh is LoadState.Error

                b.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
                b.rvPengumuman.visibility = if (!isLoading && !isError) View.VISIBLE else View.GONE

                if (b.swipeRefresh.isRefreshing &&
                    (state.refresh is LoadState.NotLoading || state.refresh is LoadState.Error)
                ) {
                    b.swipeRefresh.isRefreshing = false
                }
            }
        }

        adapter.addLoadStateListener(loadStateListener)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pager.collectLatest { adapter.submitData(it) }
            }
        }

        updateInternetCard()
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        adapter.refresh()
    }

    override fun onResume() {
        super.onResume()
        reloadData()
    }

    override fun onDestroyView() {
        adapter.removeLoadStateListener(loadStateListener)
        _binding = null
        super.onDestroyView()
    }
}