package com.gemahripah.banksampah.ui.admin.pengaturan.jenis

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.admin.pengaturan.jenis.adapter.KategoriAdapter
import com.gemahripah.banksampah.databinding.FragmentJenisSampahBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable

class JenisSampahFragment : Fragment(), Reloadable {

    private var _binding: FragmentJenisSampahBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JenisSampahViewModel by viewModels()
    private lateinit var kategoriAdapter: KategoriAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJenisSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeKategoriList()
        setupClickListeners()

        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }

        if (!updateInternetCard()) return
        viewModel.loadKategori()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        binding.swipeRefresh.isRefreshing = false

        viewModel.loadKategori()
    }

    private fun setupRecyclerView() {
        kategoriAdapter = KategoriAdapter(emptyList()) { selected ->
            val action = JenisSampahFragmentDirections
                .actionJenisSampahFragmentToEditJenisSampahFragment(selected)
            findNavController().navigate(action)
        }

        binding.rvJenisSampah.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kategoriAdapter
        }
    }

    private fun observeKategoriList() {
        viewModel.kategoriList.observe(viewLifecycleOwner) { list ->
            kategoriAdapter.updateKategoriList(list)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
    }

    private fun setupClickListeners() {
        binding.tambahJenis.setOnClickListener {
            findNavController().navigate(
                R.id.action_jenisSampahFragment_to_tambahJenisSampahFragment
            )
        }

        binding.tambahKategori.setOnClickListener {
            findNavController().navigate(
                R.id.action_jenisSampahFragment_to_tambahKategoriFragment
            )
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