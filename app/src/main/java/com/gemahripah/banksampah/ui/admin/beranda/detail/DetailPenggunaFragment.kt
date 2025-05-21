package com.gemahripah.banksampah.ui.admin.beranda.detail

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.beranda.TotalSampahPerJenis
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailPenggunaBinding
import com.gemahripah.banksampah.ui.admin.beranda.adapter.TotalSampahAdapter
import com.gemahripah.banksampah.ui.admin.transaksi.adapter.RiwayatTransaksiAdapter
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DetailPenggunaFragment : Fragment() {
    private var _binding: FragmentDetailPenggunaBinding? = null
    private val binding get() = _binding!!

    private val client = SupabaseProvider.client

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPenggunaBinding.inflate(inflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Siapkan layout manager sebelum adapter di-assign
        binding.rvTotal.layoutManager =
            GridLayoutManager(requireContext(), 2)

        val pengguna = arguments?.let { DetailPenggunaFragmentArgs.fromBundle(it).pengguna }

        if (pengguna != null) {
            binding.nama.text = pengguna.pgnNama.toString()
            pengguna.pgnId?.let {
                getSaldo(it)
                getTotalSampah(it)
                fetchRiwayatTransaksi(it)
            }
        }

        binding.menabung.setOnClickListener {
            val action = pengguna?.let { it1 ->
                DetailPenggunaFragmentDirections
                    .actionDetailPenggunaFragmentToSetorSampahFragment(it1)
            }
            if (action != null) {
                findNavController().navigate(action)
            }
        }

        binding.menarik.setOnClickListener {
            val action = pengguna?.let { it1 ->
                DetailPenggunaFragmentDirections
                    .actionDetailPenggunaFragmentToPenarikanSaldoFragment(it1)
            }
            if (action != null) {
                findNavController().navigate(action)
            }
        }

        binding.rvRiwayat.post {
            val maxHeight = resources.getDimensionPixelSize(R.dimen.recycler_max_height) // misalnya 300dp di dimen
            if (binding.rvRiwayat.height > maxHeight) {
                binding.rvRiwayat.layoutParams.height = maxHeight
                binding.rvRiwayat.requestLayout()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRiwayatTransaksi(pgnId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transaksiList = client.postgrest.from("transaksi")
                    .select {
                        order("created_at", order = Order.DESCENDING)
                        filter {
                            eq("tskIdPengguna", pgnId!!)
                        }
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

                    Log.d("TransaksiFragment", "ID Transaksi: ${transaksi.tskId}")

                    // Cek apakah tipe transaksi "masuk" atau "keluar"
                    val totalBerat = if (transaksi.tskTipe == "Masuk") {
                        val response = client.postgrest.rpc(
                            "hitung_total_jumlah",
                            buildJsonObject {
                                put("tsk_id_input", transaksi.tskId)
                            }
                        )
                        Log.d("TransaksiFragment", "totalBerat response: ${response.data}")
                        response.data?.toDoubleOrNull()
                    } else null

                    // Jika tipe transaksi adalah "keluar", gunakan dtlJumlah
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

                    val (tanggalFormatted, hari) = try {
                        val dateTime = OffsetDateTime.parse(transaksi.created_at)
                        val tanggalFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
                        val hariFormat = DateTimeFormatter.ofPattern("EEEE", Locale("id")) // nama hari penuh
                        dateTime.format(tanggalFormat) to dateTime.format(hariFormat)
                    } catch (e: Exception) {
                        "Tanggal tidak valid" to "Hari tidak valid"
                    }

                    RiwayatTransaksi(
                        tskId = transaksi.tskId!!,
                        tskIdPengguna = transaksi.tskIdPengguna,
                        nama = pengguna.pgnNama ?: "Tidak Diketahui",
                        tanggal = tanggalFormatted,
                        tipe = transaksi.tskTipe ?: "Masuk",
                        tskKeterangan = transaksi.tskKeterangan,
                        totalBerat = totalBerat,
                        totalHarga = totalHarga,
                        hari = hari
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.rvRiwayat.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = RiwayatTransaksiAdapter(hasil) { riwayat ->
                            val action = DetailPenggunaFragmentDirections
                                .actionDetailPenggunaFragmentToDetailTransaksiFragment(riwayat)
                            findNavController().navigate(action)
                            Log.d("TransaksiFragment", "Navigating to DetailTransaksi with Riwayat: $riwayat")
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getTotalSampah(pgnId: String) {
        lifecycleScope.launch {
            try {
                val result = SupabaseProvider.client
                    .postgrest
                    .rpc(
                        "get_riwayat_setoran_pengguna_berdasarkan_jenis",
                        buildJsonObject {
                            put("pgn_id_input", pgnId)
                        }
                    )

                Log.d("DetailPenggunaFragment", "Raw result: ${result.data}")

                val list = result.decodeList<TotalSampahPerJenis>()

                if (list.isNotEmpty()) {
                    val adapter = TotalSampahAdapter(list)
                    binding.rvTotal.adapter = adapter
                    binding.rvTotal.visibility = View.VISIBLE
                } else {
                    binding.rvTotal.visibility = View.GONE
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getSaldo(pgnId: String) {
        lifecycleScope.launch {
            try {
                val result = SupabaseProvider.client
                    .postgrest
                    .rpc(
                        "hitung_saldo_pengguna",
                        buildJsonObject {
                            put("pgn_id_input", pgnId)
                        }
                    )

                val saldo = result.data.toDoubleOrNull() ?: 0.0

                val formatted = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(saldo)
                binding.nominal.text = formatted

            } catch (e: Exception) {
                e.printStackTrace()
                binding.nominal.text = "Rp 0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}