package com.gemahripah.banksampah.ui.nasabah.beranda

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentBerandaNasabahBinding
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.RiwayatPagingAdapter
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

class BerandaFragment : Fragment(), Reloadable {

    private var _binding: FragmentBerandaNasabahBinding? = null
    private val binding get() = _binding!!

    private val nasabahViewModel: NasabahViewModel by activityViewModels()
    private val berandaViewModel: BerandaViewModel by viewModels()

    private var startDate: String? = null
    private var endDate: String? = null
    private var selectedFilter: String = ""

    private lateinit var pagingAdapter: RiwayatPagingAdapter
    private var pagingJob: Job? = null

    private val nf2 = NumberFormat.getNumberInstance(Locale("id","ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaNasabahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupSpinner()
        setupDatePickers()
        setupRecyclerView()

        binding.swipeRefresh.setOnRefreshListener { reloadData() }

        binding.koneksiRiwayat.setOnClickListener {
            binding.koneksiRiwayat.visibility = View.GONE
            binding.loading.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000L)
                pagingAdapter.retry()
            }
        }

        if (!updateInternetCard()) return
        setupTransaksi()
        setupSetoran()
    }

    private fun formatRupiah2(bd: BigDecimal?): String {
        val v = (bd ?: BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP)
        return "Rp ${nf2.format(v)}"   // contoh: Rp 1.234,567
    }

    override fun reloadData() {
        if (!updateInternetCard()) return

        nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
            nasabahViewModel.loadPenggunaById(pgnId)
        }

        binding.swipeRefresh.isRefreshing = false
        refreshDashboardData()
    }

    private fun refreshDashboardData() {
        startDate = null
        endDate = null

        binding.tvTanggalTransaksi.visibility = View.GONE
        binding.tvTanggalSetoran.visibility = View.GONE
        binding.startDateEditText.setText("")
        binding.endDateEditText.setText("")

        val spinner = binding.spinnerFilterTransaksi
        spinner.setSelection(0, false)
        selectedFilter = resources.getStringArray(R.array.filter_transaksi)[0]

        nasabahViewModel.pengguna.value?.let { pengguna ->
            binding.nominal.text = formatRupiah2(pengguna.pgnSaldo)
            pengguna.pgnId?.let { pgnId ->
                berandaViewModel.getTotalSetoran(pgnId)
                berandaViewModel.getTotalTransaksi(pgnId, selectedFilter)
                observePaging(pgnId)
            }
        }
    }

    private fun setupRecyclerView() {
        pagingAdapter = RiwayatPagingAdapter { riwayat ->
            val action = BerandaFragmentDirections
                .actionNavigationHomeToDetailTransaksiFragment(riwayat)
            findNavController().navigate(action)
        }

        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = pagingAdapter.withLoadStateHeaderAndFooter(
            header = LoadingStateAdapter { pagingAdapter.retry() },
            footer = LoadingStateAdapter { pagingAdapter.retry() }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                    val isLoading = loadStates.refresh is LoadState.Loading
                    val isError = loadStates.refresh is LoadState.Error
                    val isEmpty = loadStates.refresh is LoadState.NotLoading && pagingAdapter.itemCount == 0

                    binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.rvRiwayat.visibility = if (!isLoading && !isError) View.VISIBLE else View.GONE
                    binding.koneksiRiwayat.visibility = if (isError) View.VISIBLE else View.GONE
                    binding.riwayatKosong.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            }
        }
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage")
    private fun observePaging(pgnId: String) {
        pagingJob?.cancel()
        pagingJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                berandaViewModel
                    .pagingData(pgnId, startDate, endDate)
                    .collectLatest { pagingAdapter.submitData(it) }
            }
        }
    }

    private fun observeViewModel() {
        nasabahViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            val pgnId = pengguna?.pgnId ?: return@observe
            if (!updateInternetCard()) return@observe

            binding.nominal.text = formatRupiah2(pengguna.pgnSaldo)

            // Pastikan filter punya nilai default
            if (selectedFilter.isBlank()) {
                selectedFilter = resources.getStringArray(R.array.filter_transaksi)[0]
            }

            // Muat semua ringkasan di header begitu pgnId tersedia
            berandaViewModel.getTotalSetoran(pgnId)                       // <-- ini yang hilang
            berandaViewModel.getTotalTransaksi(pgnId, selectedFilter)     // <-- panggil juga
            observePaging(pgnId)

            binding.swipeRefresh.isRefreshing = false
        }

        berandaViewModel.totalTransaksi.observe(viewLifecycleOwner) {
            binding.transaksi.text = it
        }
        berandaViewModel.setoran.observe(viewLifecycleOwner) {
            binding.setoran.text = it
        }
    }

    private fun setupSpinner() {
        val spinner = binding.spinnerFilterTransaksi
        val items = resources.getStringArray(R.array.filter_transaksi)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                binding.tvTanggalTransaksi.visibility = View.GONE

                selectedFilter = items[position]
                nasabahViewModel.pengguna.value?.pgnId?.let {
                    if (!updateInternetCard()) return@onItemSelected
                    berandaViewModel.getTotalTransaksi(it, selectedFilter)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupDatePickers() {
        binding.startDateEditText.setOnClickListener {
            showDatePicker { selectedDate ->
                startDate = selectedDate
                binding.startDateEditText.setText(formatToIndoDate(selectedDate))
                nasabahViewModel.pengguna.value?.pgnId?.let { observePaging(it) }
            }
        }

        binding.endDateEditText.setOnClickListener {
            showDatePicker { selectedDate ->
                endDate = selectedDate
                binding.endDateEditText.setText(formatToIndoDate(selectedDate))
                nasabahViewModel.pengguna.value?.pgnId?.let { observePaging(it) }
            }
        }

        binding.startDateEditText.setOnLongClickListener {
            startDate = null
            binding.startDateEditText.setText("")
            nasabahViewModel.pengguna.value?.pgnId?.let { observePaging(it) }
            true
        }

        binding.endDateEditText.setOnLongClickListener {
            endDate = null
            binding.endDateEditText.setText("")
            nasabahViewModel.pengguna.value?.pgnId?.let { observePaging(it) }
            true
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pilih Tanggal")
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(selection),
                ZoneId.systemDefault()
            )
            val formatted = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            onDateSelected(formatted)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun formatToIndoDate(date: String): String {
        val parsed = OffsetDateTime.parse("${date}T00:00:00+07:00")
        return parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
    }

    private fun setupTransaksi() {
        binding.tanggalTransaksi.setOnClickListener {
            val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
            nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
                showDateRangePicker { start, end ->
                    startDate = start.toString()
                    endDate = end.toString()

                    if (!updateInternetCard()) return@showDateRangePicker
                    berandaViewModel.getTotalTransaksi(pgnId, selectedItem ?: "", start, end)

                    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
                    binding.tvTanggalTransaksi.text = "(${start.format(fmt)} - ${end.format(fmt)})"
                    binding.tvTanggalTransaksi.visibility = View.VISIBLE
                }
            }
        }

        binding.tanggalTransaksi.setOnLongClickListener {
            val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
            nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
                startDate = null
                endDate = null

                if (!updateInternetCard()) return@let
                berandaViewModel.getTotalTransaksi(pgnId, selectedItem ?: "")
            }
            binding.tvTanggalTransaksi.visibility = View.GONE
            true
        }
    }

    private fun setupSetoran() {
        binding.tanggalSetoran.setOnClickListener {
            showDateRangePicker { startDate, endDate ->
                nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
                    if (!updateInternetCard()) return@let
                    berandaViewModel.getTotalSetoran(pgnId, startDate, endDate)
                }

                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
                val startText = startDate.format(formatter)
                val endText = endDate.format(formatter)

                binding.tvTanggalSetoran.apply {
                    text = "($startText - $endText)"
                    visibility = View.VISIBLE
                }
            }
        }

        binding.tanggalSetoran.setOnLongClickListener {
            nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
                if (!updateInternetCard()) return@let
                berandaViewModel.getTotalSetoran(pgnId)
            }

            binding.tvTanggalSetoran.visibility = View.GONE
            true
        }
    }

    private fun showDateRangePicker(onDateRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit) {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Tanggal")
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
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


    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? NasabahActivity)?.showNoInternetCard(showCard)
        return isConnected
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