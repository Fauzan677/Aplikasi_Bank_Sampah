package com.gemahripah.banksampah.ui.nasabah.pengumuman

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.gemahripah.banksampah.databinding.FragmentPengumumanBinding
import com.gemahripah.banksampah.ui.gabungan.adapter.pengumuman.PengumumanAdapter

class PengumumanFragment : Fragment() {

    private var _binding: FragmentPengumumanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PengumumanViewModel by viewModels()

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
        observeViewModel()
        viewModel.loadPengumuman()

    }

    private fun setupUI() {
        binding.tambah.visibility = View.GONE

        val layoutParams = binding.rvPengumuman.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = (40 * resources.displayMetrics.density).toInt()
        binding.rvPengumuman.layoutParams = layoutParams
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvPengumuman.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.pengumumanList.observe(viewLifecycleOwner) { list ->
            binding.rvPengumuman.apply {
                layoutManager = GridLayoutManager(requireContext(), 2)
                adapter = PengumumanAdapter(list) { pengumuman ->
                    val action = PengumumanFragmentDirections
                        .actionNavigationDashboardToDetailPengumumanFragment(pengumuman)
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}