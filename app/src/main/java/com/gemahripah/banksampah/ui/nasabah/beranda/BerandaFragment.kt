package com.gemahripah.banksampah.ui.nasabah.beranda

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.databinding.FragmentBerandaNasabahBinding
import com.gemahripah.banksampah.ui.gabungan.adapter.transaksi.RiwayatTransaksiAdapter
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaNasabahBinding? = null
    private val binding get() = _binding!!

    private val nasabahViewModel: NasabahViewModel by activityViewModels()
    private val berandaViewModel: BerandaViewModel by viewModels()

    private var semuaRiwayat: List<RiwayatTransaksi> = emptyList()
    private var startDate: String? = null
    private var endDate: String? = null
    private var selectedFilter: String = ""

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
        setupTransaksi()
        setupSetoran()
        setupDatePickers()
    }

    private fun observeViewModel() {
        nasabahViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            pengguna?.pgnId?.let { pgnId ->
                berandaViewModel.getSaldo(pgnId)
                berandaViewModel.getTotalSetoran(pgnId)
                berandaViewModel.fetchRiwayat(pgnId)
            }
        }

        berandaViewModel.loadingRiwayat.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingRiwayat.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvRiwayat.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        berandaViewModel.saldo.observe(viewLifecycleOwner) {
            binding.nominal.text = it
        }

        berandaViewModel.totalTransaksi.observe(viewLifecycleOwner) {
            binding.transaksi.text = it
        }

        berandaViewModel.setoran.observe(viewLifecycleOwner) {
            binding.setoran.text = it
        }

        berandaViewModel.riwayat.observe(viewLifecycleOwner) { list ->
            semuaRiwayat = list
            tampilkanRiwayat(list)
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
                filterRiwayatIfDateSelected()
            }
        }

        binding.endDateEditText.setOnClickListener {
            showDatePicker { selectedDate ->
                endDate = selectedDate
                binding.endDateEditText.setText(formatToIndoDate(selectedDate))
                filterRiwayatIfDateSelected()
            }
        }

        binding.startDateEditText.setOnLongClickListener {
            startDate = null
            binding.startDateEditText.setText("")
            filterRiwayatIfDateSelected()
            true
        }

        binding.endDateEditText.setOnLongClickListener {
            endDate = null
            binding.endDateEditText.setText("")
            filterRiwayatIfDateSelected()
            true
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pilih Tanggal")
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

    private fun tampilkanRiwayat(data: List<RiwayatTransaksi>) {
        binding.rvRiwayat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = RiwayatTransaksiAdapter(data) { riwayat ->
                val action = BerandaFragmentDirections
                    .actionNavigationHomeToDetailTransaksiFragment(riwayat)
                findNavController().navigate(action)
            }
        }
    }

    private fun filterRiwayatIfDateSelected() {
        val filtered = semuaRiwayat.filter { riwayat ->
            val tanggalRiwayat = OffsetDateTime.parse(
                "${riwayat.tanggal}T00:00:00+07:00",
                DateTimeFormatter.ofPattern("dd MMM yyyy'T'HH:mm:ssXXX", Locale("id"))
            )

            when {
                !startDate.isNullOrEmpty() && !endDate.isNullOrEmpty() -> {
                    val start = OffsetDateTime.parse("${startDate}T00:00:00+07:00")
                    val end = OffsetDateTime.parse("${endDate}T23:59:59+07:00")
                    !tanggalRiwayat.isBefore(start) && !tanggalRiwayat.isAfter(end)
                }
                !startDate.isNullOrEmpty() -> {
                    val start = OffsetDateTime.parse("${startDate}T00:00:00+07:00")
                    !tanggalRiwayat.isBefore(start)
                }
                !endDate.isNullOrEmpty() -> {
                    val end = OffsetDateTime.parse("${endDate}T23:59:59+07:00")
                    !tanggalRiwayat.isAfter(end)
                }
                else -> true
            }
        }

        tampilkanRiwayat(filtered)
    }

    private fun setupTransaksi() {
        binding.tanggalTransaksi.setOnClickListener {
            val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
            nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
                showDateRangePicker { startDate, endDate ->
                    berandaViewModel.getTotalTransaksi(pgnId, selectedItem ?: "", startDate, endDate)

                    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
                    val startText = startDate.format(formatter)
                    val endText = endDate.format(formatter)

                    binding.tvTanggalTransaksi.apply {
                        text = "($startText - $endText)"
                        visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.tanggalTransaksi.setOnLongClickListener {
            val selectedItem = binding.spinnerFilterTransaksi.selectedItem?.toString()
            nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
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
                berandaViewModel.getTotalSetoran(pgnId)
            }

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
        startDate = null
        endDate = null

        binding.tvTanggalTransaksi.visibility = View.GONE
        binding.tvTanggalSetoran.visibility = View.GONE
        binding.startDateEditText.setText("")
        binding.endDateEditText.setText("")

        val spinner = binding.spinnerFilterTransaksi
        spinner.setSelection(0, false)
        selectedFilter = resources.getStringArray(R.array.filter_transaksi)[0]

        nasabahViewModel.pengguna.value?.pgnId?.let { pgnId ->
            berandaViewModel.getTotalSetoran(pgnId)
            berandaViewModel.getTotalTransaksi(pgnId, selectedFilter)
            berandaViewModel.fetchRiwayat(pgnId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}