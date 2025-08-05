package com.gemahripah.banksampah.ui.admin.transaksi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTransaksiBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.RiwayatTransaksiAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class TransaksiFragment : Fragment(), Reloadable {

    private var _binding: FragmentTransaksiBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RiwayatTransaksiAdapter
    private val viewModel: TransaksiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupNavigation()
        setupObservers()
        setupSearchFilter()
        setupDatePickers()
        handleBackButton()

        binding.swipeRefresh.setOnRefreshListener {
            if (!updateInternetCard()) return@setOnRefreshListener
            reloadData()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.koneksiRiwayat.setOnClickListener {
            binding.koneksiRiwayat.visibility = View.GONE
            binding.progressRiwayat.visibility = View.VISIBLE
            binding.loadingOverlay.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.fetchRiwayat()
            }, 1000)
        }

        if (!updateInternetCard()) return

        viewModel.fetchRiwayat()
    }

    private fun setupNavigation() {
        binding.menabung.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_sampah_to_setorSampahFragment)
        }

        binding.menarik.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_transaksi_to_penarikanSaldoFragment)
        }
    }

    private fun setupRecyclerView() {
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        adapter = RiwayatTransaksiAdapter(emptyList()) { riwayat ->
            val action = TransaksiFragmentDirections
                .actionNavigationTransaksiToDetailTransaksiFragment(riwayat)
            findNavController().navigate(action)
        }
        binding.rvRiwayat.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.filteredRiwayat.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressRiwayat.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isError.observe(viewLifecycleOwner) { isError ->
            binding.koneksiRiwayat.visibility = if (isError) View.VISIBLE else View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchFilter() {
        binding.searchRiwayat.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        binding.searchRiwayat.setOnEditorActionListener { v, _, _ ->
            binding.searchRiwayat.clearFocus()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            true
        }

        binding.searchRiwayat.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.scrollView.post {
                    binding.scrollView.scrollTo(0, binding.riwayatTransaksi.top)
                }
            }
            false
        }
    }

    private fun setupDatePickers() {
        binding.startDateEditText.setOnClickListener {
            showDatePicker { selectedDate ->
                val iso = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                binding.startDateEditText.setText(
                    selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
                )
                viewModel.setStartDate(iso)
            }
        }

        binding.endDateEditText.setOnClickListener {
            showDatePicker { selectedDate ->
                val iso = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                binding.endDateEditText.setText(
                    selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
                )
                viewModel.setEndDate(iso)
            }
        }

        binding.startDateEditText.setOnLongClickListener {
            binding.startDateEditText.text?.clear()
            viewModel.setStartDate(null)
            true
        }

        binding.endDateEditText.setOnLongClickListener {
            binding.endDateEditText.text?.clear()
            viewModel.setEndDate(null)
            true
        }
    }

    private fun showDatePicker(onDateSelected: (OffsetDateTime) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pilih Tanggal")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(selection),
                java.time.ZoneId.systemDefault()
            )
            onDateSelected(date)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun handleBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.searchRiwayat.hasFocus()) {
                binding.searchRiwayat.clearFocus()
                binding.scrollView.scrollTo(0, 0)

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchRiwayat.windowToken, 0)
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    override fun reloadData() {
        if (!updateInternetCard()) return

        viewModel.fetchRiwayat()

        binding.searchRiwayat.setText("")
        binding.searchRiwayat.clearFocus()
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, 0)
        }
        binding.swipeRefresh.isRefreshing = false
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