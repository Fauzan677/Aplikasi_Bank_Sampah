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
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.databinding.FragmentBerandaAdminBinding
import com.gemahripah.banksampah.ui.admin.beranda.adapter.NasabahAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

        viewModel.fetchDashboardData()

        setupRecyclerView()
        observeViewModel()
        setupSearchListener()
        setupSpinner()
        setupTransaksi()
        setupSetoran()

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
            binding.rvListNasabah.visibility = if (isLoading) View.GONE else View.VISIBLE
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
                binding.tvTanggalTransaksi.visibility = View.GONE

                val filter = parent.getItemAtPosition(position).toString()
                viewModel.getTotalTransaksi(filter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupTransaksi() {
        binding.tanggalTransaksi.setOnClickListener {
            val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
            showDateRangePicker { startDate, endDate ->
                viewModel.getTotalTransaksi(selectedItem ?: "", startDate, endDate)

                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
                val startText = startDate.format(formatter)
                val endText = endDate.format(formatter)

                binding.tvTanggalTransaksi.apply {
                    text = "($startText - $endText)"
                    visibility = View.VISIBLE
                }
            }
        }

        binding.tanggalTransaksi.setOnLongClickListener {
            val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
            viewModel.getTotalTransaksi(selectedItem ?: "")

            binding.tvTanggalTransaksi.visibility = View.GONE
            true
        }
    }

    private fun setupSetoran() {
        binding.tanggalSetoran.setOnClickListener {
            showDateRangePicker { startDate, endDate ->
                viewModel.getTotalSetoran(startDate, endDate)

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
            viewModel.getTotalSetoran()
            binding.tvTanggalSetoran.visibility = View.GONE
            true
        }
    }

    private fun showDateRangePicker(onDateRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit) {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Tanggal Setoran")
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

    override fun onResume() {
        super.onResume()
        binding.searchNasabah.setText("")
        binding.searchNasabah.clearFocus()
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, 0)
        }
        binding.tvTanggalTransaksi.visibility = View.GONE
        binding.tvTanggalSetoran.visibility = View.GONE

        viewModel.fetchDashboardData()
        val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
        viewModel.getTotalTransaksi(selectedItem ?: "")
        viewModel.getTotalSetoran()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}