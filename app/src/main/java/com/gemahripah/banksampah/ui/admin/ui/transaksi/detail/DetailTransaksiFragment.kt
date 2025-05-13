package com.gemahripah.banksampah.ui.admin.ui.transaksi.detail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailTransaksiBinding
import com.gemahripah.banksampah.ui.admin.ui.transaksi.adapter.DetailTransaksiAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DetailTransaksiFragment : Fragment() {

    private val args: DetailTransaksiFragmentArgs by navArgs()

    private var _binding: FragmentDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailTransaksiBinding.inflate(inflater, container, false)

        val riwayat = args.riwayat
        val idTransaksi = riwayat.tskId

        binding.nominal.text = riwayat.totalHarga.toString()

        // Ambil data detail transaksi berdasarkan idTransaksi
        getDetailTransaksi(idTransaksi)

        return binding.root
    }

    private fun getDetailTransaksi(idTransaksi: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ambil dtlId, dtlJumlah, dan sphJenis
                val columns = Columns.raw("""
                dtlId,
                dtlJumlah,
                dtlSphId (
                    sphJenis
                )
            """.trimIndent())

                val response = SupabaseProvider.client
                    .from("detail_transaksi")
                    .select(columns) {
                        filter {
                            eq("dtlTskId", idTransaksi)
                        }
                    }

                val detailList = Json.decodeFromString<List<DetailTransaksiRelasi>>(response.data.toString())

                // Panggil fungsi hitung_harga_detail untuk tiap dtlId
                val enrichedList = detailList.mapNotNull { detail ->
                    val dtlId = detail.dtlId
                    if (dtlId != null) {
                        try {
                            val hargaDetailResponse = SupabaseProvider.client.postgrest.rpc(
                                "hitung_harga_detail",
                                buildJsonObject {
                                    put("dtl_id_input", dtlId)  // Gunakan dtlId untuk parameter
                                }
                            )

                            // Mengambil harga detail dengan mengonversi data response
                            val hargaDetail = hargaDetailResponse.data.toDoubleOrNull()

                            // Menambahkan harga detail pada setiap item jika berhasil
                            detail.copy(dtlJumlah = hargaDetail ?: 0.0)

                        } catch (e: Exception) {
                            Log.e("DetailTransaksi", "Gagal hitung harga detail: ${e.message}", e)
                            null
                        }
                    } else null
                }

                withContext(Dispatchers.Main) {
                    binding.rvDetail.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvDetail.adapter = DetailTransaksiAdapter(enrichedList)
                }

            } catch (e: Exception) {
                Log.e("DetailTransaksi", "Gagal ambil data: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}