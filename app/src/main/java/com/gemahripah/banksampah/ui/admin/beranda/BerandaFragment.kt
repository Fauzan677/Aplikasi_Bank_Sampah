package com.gemahripah.banksampah.ui.admin.beranda

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.TextView
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
import com.gemahripah.banksampah.databinding.FragmentBerandaAdminBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.gabungan.adapter.listNasabah.NasabahPagingAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class BerandaFragment : Fragment(), Reloadable {
    private var _binding: FragmentBerandaAdminBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BerandaViewModel by viewModels()
    private lateinit var pagingAdapter: NasabahPagingAdapter

    private var firstResume = true
    private var suppressSpinnerEvent = false

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

        // 1) Setup komponen UI & listeners
        setupRecyclerView()
        setupSearch()
        setupSpinnerFilterTransaksi()
        setupSwipeToRefresh()
        setupBackPressHandler()
        setupRetryCard()

        // 2) Observasi data dari ViewModel
        observePager()
        observeLoadState()
        observeDashboardNumbers()

        // 3) Guard koneksi awal
        if (!updateInternetCard()) return

        // 4) Setup filter tanggal dengan helper generik
        setupDateRangeFilters()

        // 5) Muat data awal
        viewModel.fetchDashboardData()
    }

    // -- Public API --
    override fun reloadData() {
        if (!updateInternetCard()) return
        binding.swipeRefresh.isRefreshing = false
        refreshDashboardData()
    }

    // -- Setup UI --
    private fun setupRecyclerView() {
        pagingAdapter = NasabahPagingAdapter { pengguna ->
            val action = BerandaFragmentDirections.actionNavigationBerandaToDetailPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }

        binding.rvListNasabah.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pagingAdapter.withLoadStateHeaderAndFooter(
                header = LoadingStateAdapter { pagingAdapter.retry() },
                footer = LoadingStateAdapter { pagingAdapter.retry() }
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearch() {
        // Scroll ke list saat kolom search disentuh
        binding.searchNasabah.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.scrollView.post {
                    binding.scrollView.scrollTo(0, binding.listNasabah.top)
                }
            }
            false
        }

        // Query berubah -> push ke ViewModel (debounce ada di layer Pager jika perlu)
        binding.searchNasabah.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        // Tekan imeActionSearch -> tutup keyboard
        binding.searchNasabah.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v)
                binding.searchNasabah.clearFocus()
                true
            } else false
        }
    }

    private fun setupSpinnerFilterTransaksi() {
        binding.spinnerFilterTransaksi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                binding.tvTanggalTransaksi.gone()

                if (suppressSpinnerEvent || !updateInternetCard()) return

                val filter = parent.getItemAtPosition(position).toString()
                viewModel.getTotalTransaksi(filter)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener { reloadData() }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.searchNasabah.hasFocus()) {
                binding.searchNasabah.clearFocus()
                binding.scrollView.scrollTo(0, 0)
                hideKeyboard(binding.searchNasabah)
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupRetryCard() {
        binding.koneksiNasabah.setOnClickListener {
            binding.koneksiNasabah.gone()
            binding.loading.visible()
            lifecycleScope.launch {
                delay(1000L)
                pagingAdapter.retry()
            }
        }
    }

    // -- Observers --
    private fun observePager() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pager.collectLatest { pagingData ->
                    pagingAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeLoadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                    renderListState(loadStates)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeDashboardNumbers() {
        viewModel.totalSaldo.observe(viewLifecycleOwner) {
            binding.nominal.text = "Rp $it"
        }
        viewModel.totalNasabah.observe(viewLifecycleOwner) {
            binding.nasabah.text = it
        }
        viewModel.totalTransaksi.observe(viewLifecycleOwner) {
            binding.transaksi.text = "Rp $it"
        }
        viewModel.totalSetoran.observe(viewLifecycleOwner) {
            binding.setoran.text = it
        }
    }

    // -- Rendering --
    private fun renderListState(loadStates: androidx.paging.CombinedLoadStates) {
        val isLoading = loadStates.refresh is LoadState.Loading
        val isError = loadStates.refresh is LoadState.Error
        val isEmpty = loadStates.refresh is LoadState.NotLoading && pagingAdapter.itemCount == 0

        binding.loading.setVisible(isLoading)
        binding.rvListNasabah.setVisible(!isLoading && !isError)
        binding.koneksiNasabah.setVisible(isError)
        binding.nasabahKosong.setVisible(isEmpty)
    }

    // -- Date Range Filters (modular & reusable) --
    @SuppressLint("SetTextI18n")
    private fun setupDateRangeFilters() {
        // Transaksi (dengan spinner filter)
        attachDateRangeFilter(
            triggerView = binding.tanggalTransaksi,
            labelView = binding.tvTanggalTransaksi,
            title = "Pilih Rentang Tanggal Transaksi",
            onRangeSelected = { start, end ->
                if (!updateInternetCard()) return@attachDateRangeFilter
                val selectedFilter = binding.spinnerFilterTransaksi.selectedItem?.toString().orEmpty()
                viewModel.getTotalTransaksi(selectedFilter, start, end)
            },
            onReset = {
                if (!updateInternetCard()) return@attachDateRangeFilter
                val selectedFilter = binding.spinnerFilterTransaksi.selectedItem?.toString().orEmpty()
                viewModel.getTotalTransaksi(selectedFilter)
            }
        )

        // Setoran
        attachDateRangeFilter(
            triggerView = binding.tanggalSetoran,
            labelView = binding.tvTanggalSetoran,
            title = "Pilih Rentang Tanggal Setoran",
            onRangeSelected = { start, end ->
                if (!updateInternetCard()) return@attachDateRangeFilter
                viewModel.getTotalSetoran(start, end)
            },
            onReset = {
                if (!updateInternetCard()) return@attachDateRangeFilter
                viewModel.getTotalSetoran()
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun attachDateRangeFilter(
        triggerView: View,
        labelView: TextView,
        title: String,
        onRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit,
        onReset: () -> Unit
    ) {
        val formatter = DateUtils.formatter

        triggerView.setOnClickListener {
            showDateRangePicker(title) { startDate, endDate ->
                onRangeSelected(startDate, endDate)
                labelView.text = "(${startDate.format(formatter)} - ${endDate.format(formatter)})"
                labelView.visible()
            }
        }

        triggerView.setOnLongClickListener {
            onReset()
            labelView.gone()
            true
        }
    }

    private fun showDateRangePicker(
        title: String,
        onDateRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit
    ) {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(title)
            .build()

        dateRangePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startMillis = selection.first
            val endMillis = selection.second
            if (startMillis != null && endMillis != null) {
                val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val endDate = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                onDateRangeSelected(startDate, endDate)
            }
        }
    }

    // -- Data Ops --
    private fun refreshDashboardData() {
        binding.apply {
            searchNasabah.setText("")
            searchNasabah.clearFocus()
            tvTanggalTransaksi.gone()
            tvTanggalSetoran.gone()
            scrollView.post { scrollView.scrollTo(0, 0) }
            suppressSpinnerEvent = true
            spinnerFilterTransaksi.setSelection(0)
            val filter0 = spinnerFilterTransaksi.selectedItem?.toString().orEmpty()
            suppressSpinnerEvent = false

            // panggil manual supaya pasti refresh transaksi
            viewModel.getTotalTransaksi(filter0)

        }
        pagingAdapter.refresh()
        viewModel.fetchDashboardData()
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    // -- Lifecycle --
    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
        } else {
            reloadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -- Utils & Extensions --
    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun View.visible() { this.visibility = View.VISIBLE }
    private fun View.gone() { this.visibility = View.GONE }
    private fun View.setVisible(visible: Boolean) { this.visibility = if (visible) View.VISIBLE else View.GONE }

    private object DateUtils {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
    }
}