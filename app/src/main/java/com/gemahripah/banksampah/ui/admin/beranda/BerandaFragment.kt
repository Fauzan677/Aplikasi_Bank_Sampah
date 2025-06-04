package com.gemahripah.banksampah.ui.admin.beranda

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.databinding.FragmentBerandaAdminBinding
import com.gemahripah.banksampah.ui.admin.beranda.adapter.NasabahAdapter

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaAdminBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BerandaViewModel by viewModels()
    private lateinit var adapter: NasabahAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupSearchListener()
        setupSpinner()
        viewModel.fetchDashboardData()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.searchNasabah.hasFocus()) {

                binding.searchNasabah.clearFocus()
                binding.scrollView.scrollTo(0, 0)

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchNasabah.windowToken, 0)

            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

    }

    private fun setupRecyclerView() {
        adapter = NasabahAdapter(emptyList()) { pengguna ->
            val action = BerandaFragmentDirections.actionNavigationBerandaToDetailPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }
        binding.rvListNasabah.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListNasabah.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.nasabahList.observe(viewLifecycleOwner) {
            adapter.updateData(it)
        }

        viewModel.isLoadingNasabah.observe(viewLifecycleOwner) { isLoading ->
            binding.progressNasabah.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.totalSaldo.observe(viewLifecycleOwner) {
            binding.nominal.text = it
        }

        viewModel.totalNasabah.observe(viewLifecycleOwner) {
            binding.nasabah.text = it
        }

        viewModel.totalTransaksi.observe(viewLifecycleOwner) {
            binding.transaksi.text = it
        }

        viewModel.totalSetoran.observe(viewLifecycleOwner) {
            binding.setoran.text = it
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchListener() {
        binding.searchNasabah.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.scrollView.post {
                    val y = binding.listNasabah.top
                    binding.scrollView.scrollTo(0, y)
                }
            }

            false
        }


        binding.searchNasabah.addTextChangedListener { text ->
            adapter.filterList(text.toString())
        }

        binding.searchNasabah.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                adapter.filterList(binding.searchNasabah.text.toString())

                val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                binding.searchNasabah.clearFocus()
                true
            } else {
                false
            }
        }
    }

    private fun setupSpinner() {
        binding.spinnerFilterTransaksi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (parent.getItemAtPosition(position).toString()) {
                    "Transaksi Masuk" -> viewModel.getTotalTransaksiMasuk()
                    "Transaksi Keluar" -> viewModel.getTotalTransaksiKeluar()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerFilterSetoran.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val filter = when (parent.getItemAtPosition(position).toString()) {
                    "Hari Ini" -> "hari_ini"
                    "Minggu Ini" -> "minggu_ini"
                    "Bulan Ini" -> "bulan_ini"
                    "3 Bulan Terakhir" -> "3_bulan"
                    else -> "semua"
                }
                viewModel.getTotalSetoran(filter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onResume() {
        super.onResume()
        binding.searchNasabah.setText("")
        binding.searchNasabah.clearFocus()
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}