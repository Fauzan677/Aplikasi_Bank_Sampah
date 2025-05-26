package com.gemahripah.banksampah.ui.admin.transaksi.detail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailTransaksiBinding
import com.gemahripah.banksampah.ui.admin.transaksi.adapter.DetailTransaksiAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DetailTransaksiFragment : Fragment() {

    private val args: DetailTransaksiFragmentArgs by navArgs()
    private var _binding: FragmentDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    private var enrichedList: List<DetailTransaksiRelasi> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailTransaksiBinding.inflate(inflater, container, false)

        val riwayat = args.riwayat
        val idTransaksi = riwayat.tskId

        binding.nominal.text = riwayat.totalHarga.toString()
        binding.keterangan.text = riwayat.tskKeterangan

        when (riwayat.tipe.lowercase()) {
            "masuk" -> {
                binding.jenis.setCardBackgroundColor(resources.getColor(R.color.hijau, null))
                binding.transaksi.text = "Transaksi Masuk"
            }
            "keluar" -> {
                binding.detail.visibility = View.GONE
                binding.rvDetail.visibility = View.GONE

                binding.jenis.setCardBackgroundColor(resources.getColor(R.color.merah, null))
                binding.transaksi.text = "Transaksi Keluar"
            }
        }


        getDetailTransaksi(idTransaksi)

        binding.edit.setOnClickListener {
            when (riwayat.tipe.lowercase()) {
                "masuk" -> {
                    if (enrichedList.isNotEmpty()) {
                        val action = DetailTransaksiFragmentDirections
                            .actionDetailTransaksiFragmentToEditTransaksiMasukFragment(
                                riwayat = riwayat,
                                enrichedList = enrichedList.toTypedArray()
                            )
                        findNavController().navigate(action)
                    }
                }
                "keluar" -> {
                    val action = DetailTransaksiFragmentDirections
                        .actionDetailTransaksiFragmentToEditTransaksiKeluarFragment(
                            riwayat = riwayat,
                            enrichedList = enrichedList.toTypedArray()
                        )
                    findNavController().navigate(action)
                }
                else -> {
                    Log.w("DetailTransaksi", "Tipe transaksi tidak diketahui: ${riwayat.tipe}")
                }
            }
        }

        binding.hapus.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        return binding.root
    }

    private fun showDeleteConfirmationDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Hapus Transaksi")
        builder.setMessage("Apakah Anda yakin ingin menghapus transaksi ini? Semua data terkait akan hilang.")

        builder.setPositiveButton("Hapus") { _, _ ->
            deleteTransaksi()
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun deleteTransaksi() {
        CoroutineScope(Dispatchers.IO).launch {
            val tskId = args.riwayat.tskId

            try {
                SupabaseProvider.client
                    .from("detail_transaksi")
                    .delete {
                        filter {
                            eq("dtlTskId", tskId)
                        }
                    }

                // Hapus transaksi utama
                SupabaseProvider.client
                    .from("transaksi")
                    .delete {
                        filter {
                            eq("tskId", tskId)
                        }
                    }

                withContext(Dispatchers.Main) {
                    // Navigasi kembali setelah penghapusan
                    findNavController().popBackStack()
                }

            } catch (e: Exception) {
                Log.e("DetailTransaksi", "Gagal hapus data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(requireContext(), "Gagal menghapus transaksi", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getDetailTransaksi(idTransaksi: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val columns = Columns.raw(
                    """
                    dtlId,
                    dtlJumlah,
                    dtlSphId (
                        sphJenis
                    )
                    """.trimIndent()
                )

                val response = SupabaseProvider.client
                    .from("detail_transaksi")
                    .select(columns) {
                        filter {
                            eq("dtlTskId", idTransaksi)
                        }
                    }

                val detailList = Json.decodeFromString<List<DetailTransaksiRelasi>>(response.data.toString())

                enrichedList = detailList.mapNotNull { detail ->
                    val dtlId = detail.dtlId
                    if (dtlId != null) {
                        try {
                            val hargaDetailResponse = SupabaseProvider.client.postgrest.rpc(
                                "hitung_harga_detail",
                                buildJsonObject {
                                    put("dtl_id_input", dtlId)
                                }
                            )

                            val hargaDetail = hargaDetailResponse.data.toDoubleOrNull()
                            detail.copy(hargaDetail = hargaDetail ?: 0.0)

                        } catch (e: Exception) {
                            Log.e("DetailTransaksi", "Gagal hitung harga detail: ${e.message}", e)
                            null
                        }
                    } else null
                }

                withContext(Dispatchers.Main) {
                    binding.rvDetail.layoutManager = GridLayoutManager(requireContext(), 2)
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