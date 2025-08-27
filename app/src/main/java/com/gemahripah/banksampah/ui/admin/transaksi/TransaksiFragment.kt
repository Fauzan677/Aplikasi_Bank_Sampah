package com.gemahripah.banksampah.ui.admin.transaksi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentTransaksiBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.RiwayatPagingAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class TransaksiFragment : Fragment(), Reloadable {

    private var _binding: FragmentTransaksiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransaksiViewModel by viewModels()

    private lateinit var pagingAdapter: RiwayatPagingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNavigation()
        setupSearchFilter()
        setupDatePickers()
        handleBackButton()

        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }

        binding.koneksiRiwayat.setOnClickListener {
            binding.koneksiRiwayat.visibility = View.GONE
            binding.loading.visibility = View.VISIBLE

            lifecycleScope.launch {
                delay(1000L)
                pagingAdapter.retry()
            }
        }

        updateInternetCard()
        setupRecyclerView()
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
        pagingAdapter = RiwayatPagingAdapter { riwayat ->
            val action = TransaksiFragmentDirections
                .actionNavigationTransaksiToDetailTransaksiFragment(riwayat)
            findNavController().navigate(action)
        }

        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = pagingAdapter.withLoadStateHeaderAndFooter(
            header = LoadingStateAdapter { pagingAdapter.retry() },
            footer = LoadingStateAdapter { pagingAdapter.retry() }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pagingAdapter.loadStateFlow.collectLatest { loadState ->
                    val isLoading = loadState.refresh is LoadState.Loading
                    val isError   = loadState.refresh is LoadState.Error
                    val isEmpty   = loadState.refresh is LoadState.NotLoading && pagingAdapter.itemCount == 0

                    binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.rvRiwayat.visibility = if (!isLoading && !isError) View.VISIBLE else View.GONE
                    binding.koneksiRiwayat.visibility = if (isError) View.VISIBLE else View.GONE
                    binding.riwayatKosong.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagingData.collectLatest { data ->
                    pagingAdapter.submitData(data)
                }
            }
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
                binding.rvRiwayat.smoothScrollToPosition(0)
                binding.appbar.setExpanded(true, true) // opsional, buka appbar penuh
            }
            false
        }

        binding.searchContainer.setEndIconOnClickListener {
            binding.searchRiwayat.text?.clear()
            binding.searchRiwayat.clearFocus()
            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchRiwayat.windowToken, 0)
            // (opsional) scroll ke atas list
            binding.rvRiwayat.smoothScrollToPosition(0)
            binding.appbar.setExpanded(true, true) // opsional, buka appbar penuh

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
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
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
                binding.rvRiwayat.smoothScrollToPosition(0)
                binding.appbar.setExpanded(true, true) // opsional, buka appbar penuh

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

        viewModel.setSearchQuery("")
        viewModel.setStartDate(null)
        viewModel.setEndDate(null)
        pagingAdapter.refresh()

        binding.rvRiwayat.smoothScrollToPosition(0)
        binding.appbar.setExpanded(true, true) // opsional, buka appbar penuh

        binding.searchRiwayat.setText("")
        binding.searchRiwayat.clearFocus()
        binding.startDateEditText.setText("")
        binding.endDateEditText.setText("")
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