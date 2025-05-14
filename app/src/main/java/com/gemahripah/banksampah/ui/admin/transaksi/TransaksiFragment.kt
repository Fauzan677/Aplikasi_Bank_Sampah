package com.gemahripah.banksampah.ui.admin.transaksi

import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.gemahripah.banksampah.databinding.FragmentTransaksiBinding
import com.gemahripah.banksampah.ui.admin.transaksi.adapter.RiwayatTransaksiAdapter
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import io.github.jan.supabase.decode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class TransaksiFragment : Fragment() {

    private var _binding: FragmentTransaksiBinding? = null
    private val binding get() = _binding!!

    private val client = SupabaseProvider.client

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val transaksiViewModel =
            ViewModelProvider(this).get(com.gemahripah.banksampah.ui.admin.transaksi.TransaksiViewModel::class.java)

        _binding = FragmentTransaksiBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.menabung.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_sampah_to_setorSampahFragment)
        }

        binding.menarik.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_transaksi_to_penarikanSaldoFragment)
        }

        fetchRiwayatTransaksi()

        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRiwayatTransaksi() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transaksiList = client.postgrest.from("transaksi")
                    .select {
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

                    val tanggalFormatted = try {
                        val dateTime = OffsetDateTime.parse(transaksi.created_at)
                        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
                        dateTime.format(formatter)
                    } catch (e: Exception) {
                        "Tanggal tidak valid"
                    }

                    RiwayatTransaksi(
                        tskId = transaksi.tskId!!,
                        tskIdPengguna = transaksi.tskIdPengguna,
                        nama = pengguna.pgnNama ?: "Tidak Diketahui",
                        tanggal = tanggalFormatted,
                        tipe = transaksi.tskTipe ?: "Masuk",
                        tskKeterangan = transaksi.tskKeterangan,
                        totalBerat = totalBerat,
                        totalHarga = totalHarga
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.rvRiwayat.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = RiwayatTransaksiAdapter(hasil) { riwayat ->
                            val action = TransaksiFragmentDirections
                                .actionNavigationTransaksiToDetailTransaksiFragment(riwayat)
                            findNavController().navigate(action)
                            Log.d("TransaksiFragment", "Navigating to DetailTransaksi with Riwayat: $riwayat")
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Tambahkan error handling UI di sini kalau perlu
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}