package com.gemahripah.banksampah.ui.nasabah.ui.beranda

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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

    private lateinit var riwayatAdapter: RiwayatAdapter

    private val client = SupabaseProvider.client

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaNasabahBinding.inflate(inflater, container, false)
        val root: View = binding.root

        nasabahViewModel = ViewModelProvider(requireActivity())[NasabahViewModel::class.java]

        nasabahViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                pengguna.pgnId?.let { pgnId ->
                    getSaldo(pgnId)
                    getTotalSetoran(pgnId)
                    fetchRiwayatTransaksi(pgnId)
                }
            }
        }

        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

                    Log.d("TransaksiFragment", "ID Transaksi: ${transaksi.tskId}")

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
                        hari = hariFormatted
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.rvRiwayat.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = RiwayatTransaksiAdapter(hasil) { riwayat ->
                            val action = BerandaFragmentDirections
                                .actionNavigationHomeToDetailTransaksiFragment(riwayat)
                            findNavController().navigate(action)
                            Log.d("TransaksiFragment", "Navigating to DetailTransaksi with Riwayat: $riwayat")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("BerandaFragment", "Error saat fetch riwayat transaksi", e)
            }
        }
    }

    private fun getRiwayatTransaksi(pgnId: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .postgrest
                        .from("transaksi")
                        .select() {
                            filter {
                                eq("tskIdPengguna", pgnId)
                            }
                        }
                }

                val data = response.data
                if (data != null) {
                    val transaksiList = kotlinx.serialization.json.Json.decodeFromString<List<Transaksi>>(data.toString())
                } else {
                }
            } catch (e: Exception) {
                Log.e("BerandaFragment", "Gagal fetch riwayat transaksi", e)
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}