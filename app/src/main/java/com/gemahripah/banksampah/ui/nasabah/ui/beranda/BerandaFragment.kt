package com.gemahripah.banksampah.ui.nasabah.ui.beranda

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentBerandaNasabahBinding
import com.gemahripah.banksampah.ui.admin.transaksi.TransaksiFragmentDirections
import com.gemahripah.banksampah.ui.admin.transaksi.adapter.RiwayatTransaksiAdapter
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import com.gemahripah.banksampah.ui.nasabah.ui.beranda.adapter.RiwayatAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.log

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaNasabahBinding? = null

    private val binding get() = _binding!!
    private lateinit var nasabahViewModel: NasabahViewModel
    private var semuaRiwayat: List<RiwayatTransaksi> = emptyList()
    private var startDate: String? = null
    private var endDate: String? = null


    private val client = SupabaseProvider.client

    private var selectedFilter: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaNasabahBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val spinner = binding.root.findViewById<Spinner>(R.id.spinner_filter_transaksi)
        val items = resources.getStringArray(R.array.filter_transaksi)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedFilter = items[position]
                nasabahViewModel.pengguna.value?.pgnId?.let {
                    getTotalTransaksi(it, selectedFilter)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        nasabahViewModel = ViewModelProvider(requireActivity())[NasabahViewModel::class.java]

        nasabahViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                pengguna.pgnId?.let { pgnId ->
                    getSaldo(pgnId)
                    getTotalTransaksi(pgnId, selectedFilter)
                    getTotalSetoran(pgnId)
                    fetchRiwayatTransaksi(pgnId)
                }
            }
        }

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

        return root
    }

    private fun formatToIndoDate(date: String): String {
        val parsed = OffsetDateTime.parse("${date}T00:00:00+07:00")
        return parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pilih Tanggal")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(selection),
                java.time.ZoneId.systemDefault()
            )
            val formatted = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            onDateSelected(formatted)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    @SuppressLint("SetTextI18n")
    private fun getTotalTransaksi(pgnId: String, filter: String) {
        val params = buildJsonObject {
            put("pgn_id_input", pgnId)
        }

        lifecycleScope.launch {
            var totalMasuk = 0.0
            var totalKeluar = 0.0

            try {
                if (filter == "Transaksi Masuk") {
                    val responseMasuk = withContext(Dispatchers.IO) {
                        SupabaseProvider.client
                            .postgrest
                            .rpc("hitung_total_transaksi_masuk_per_pengguna", params)
                    }
                    totalMasuk = responseMasuk.data.toString().toDoubleOrNull() ?: 0.0
                }

                if (filter == "Transaksi Keluar") {
                    val responseKeluar = withContext(Dispatchers.IO) {
                        SupabaseProvider.client
                            .postgrest
                            .rpc("hitung_total_jumlah_per_pengguna_keluar", params)
                    }
                    totalKeluar = responseKeluar.data.toString().toDoubleOrNull() ?: 0.0
                }

                val total = when (filter) {
                    "Transaksi Masuk" -> totalMasuk
                    "Transaksi Keluar" -> totalKeluar
                    else -> {}
                }

                val formattedTotal = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(total)
                binding.transaksi.text = formattedTotal

            } catch (e: Exception) {
                Log.e("BerandaFragment", "Gagal mendapatkan total transaksi", e)
                binding.transaksi.text = "Rp 0"
            }
        }
    }

    private fun fetchRiwayatTransaksi(pgnId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transaksiList = client.postgrest.from("transaksi")
                    .select {
                        filter {
                            eq("tskIdPengguna", pgnId)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<Transaksi>()

                val hasil = transaksiList.map { transaksi ->
                    val pengguna = client.postgrest.from("pengguna")
                        .select {
                            filter {
                                eq("pgnId", transaksi.tskIdPengguna!!)
                            }
                        }
                        .decodeSingle<Pengguna>()

                    val totalBerat = if (transaksi.tskTipe == "Masuk") {
                        val response = client.postgrest.rpc(
                            "hitung_total_jumlah",
                            buildJsonObject {
                                put("tsk_id_input", transaksi.tskId)
                            }
                        )
                        response.data?.toDoubleOrNull()
                    } else null

                    val totalHarga = if (transaksi.tskTipe == "Keluar") {
                        val detailList = client.postgrest.from("detail_transaksi")
                            .select {
                                filter {
                                    transaksi.tskId?.let { eq("dtlTskId", it) }
                                }
                            }
                            .decodeList<DetailTransaksi>()

                        detailList.sumOf { it.dtlJumlah ?: 0.0 }
                    } else {
                        client.postgrest.rpc(
                            "hitung_total_harga",
                            buildJsonObject {
                                put("tsk_id_input", transaksi.tskId)
                            }
                        ).data?.toDoubleOrNull()
                    }

                    val dateTime = OffsetDateTime.parse(transaksi.created_at)
                    val formatterTanggal = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
                    val formatterHari = DateTimeFormatter.ofPattern("EEEE", Locale("id"))
                    val tanggalFormatted = dateTime.format(formatterTanggal)
                    val hariFormatted = dateTime.format(formatterHari)

                    RiwayatTransaksi(
                        tskId = transaksi.tskId!!,
                        tskIdPengguna = transaksi.tskIdPengguna,
                        nama = pengguna.pgnNama ?: "Tidak Diketahui",
                        tanggal = tanggalFormatted,
                        tipe = transaksi.tskTipe ?: "Masuk",
                        tskKeterangan = transaksi.tskKeterangan,
                        totalBerat = totalBerat,
                        totalHarga = totalHarga,
                        hari = hariFormatted,
                        createdAt = transaksi.created_at ?: ""
                    )
                }

                withContext(Dispatchers.Main) {
                    semuaRiwayat = hasil
                    tampilkanRiwayat(semuaRiwayat)
                }

            } catch (e: Exception) {
                Log.e("BerandaFragment", "Error saat fetch riwayat transaksi", e)
            }
        }
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
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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

    @SuppressLint("SetTextI18n")
    private fun getSaldo(pgnId: String) {
        val rpcParams = buildJsonObject {
            put("pgn_id_input", pgnId)
        }

        Log.d("BerandaFragment", "Memanggil getSaldo() untuk pgnId: $pgnId")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .postgrest
                        .rpc("hitung_saldo_pengguna", rpcParams)
                }

                Log.d("BerandaFragment", "Response data: ${response.data}")

                val saldo = response.data.toDoubleOrNull()

                Log.d("BerandaFragment", "Saldo hasil konversi: $saldo")

                val formatted = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(saldo)
                binding.nominal.text = formatted
            } catch (e: Exception) {
                Log.e("BerandaFragment", "Gagal mendapatkan saldo", e)
                binding.nominal.text = "Rp 0"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getTotalSetoran(pgnId: String) {
        val rpcParams = buildJsonObject {
            put("pgn_id_input", pgnId)
        }

        Log.d("BerandaFragment", "Memanggil getTotalSetoran() untuk pgnId: $pgnId")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .postgrest
                        .rpc("hitung_total_jumlah_per_pengguna_masuk", rpcParams)
                }

                Log.d("BerandaFragment", "Response total setoran: ${response.data}")

                val totalSetoran = response.data.toDoubleOrNull()
                val formatted = NumberFormat.getNumberInstance(Locale("in", "ID")).format(totalSetoran)
                binding.setoran.text = "$formatted Kg"
            } catch (e: Exception) {
                Log.e("BerandaFragment", "Gagal mendapatkan total setoran", e)
                binding.setoran.text = "0 Kg"
            }
        }
    }

    override fun onResume() {
        super.onResume()

        startDate = null
        endDate = null

        binding.startDateEditText.setText("")
        binding.endDateEditText.setText("")

        tampilkanRiwayat(semuaRiwayat)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}