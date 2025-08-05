package com.gemahripah.banksampah.ui.admin.transaksi.keluar.edit

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPenarikanSaldoBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class EditTransaksiKeluarFragment : Fragment() {
    private var _binding: FragmentPenarikanSaldoBinding? = null
    private val binding get() = _binding!!

    private val args: EditTransaksiKeluarFragmentArgs by navArgs()
    private var selectedPgnId: String? = null

    private lateinit var riwayat: RiwayatTransaksi
    private lateinit var enrichedList: Array<DetailTransaksiRelasi>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPenarikanSaldoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        riwayat = args.riwayat
        enrichedList = args.enrichedList
        selectedPgnId = riwayat.tskIdPengguna

        setupFormAwal()
        setupKonfirmasiButton()
    }

    private fun setupFormAwal() {
        binding.judul.text = "Edit Penarikan Saldo"
        binding.nama.setText(riwayat.nama, false)
        binding.jumlah.setText(enrichedList[0].dtlJumlah.toString())
        binding.keterangan.setText(riwayat.tskKeterangan ?: "")
        selectedPgnId?.let { tampilkanSaldoPengguna(it) }

        binding.nama.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isEnabled = false
        }
    }

    private fun setupKonfirmasiButton() {
        binding.konfirmasi.setOnClickListener {
            val jumlahInput = binding.jumlah.text.toString().toDoubleOrNull()
            val keterangan = binding.keterangan.text.toString()

            if (!isValidInput(jumlahInput)) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    val saldoTotal = hitungSaldoDenganPenyesuaian(selectedPgnId!!, enrichedList[0].dtlJumlah)

                    if (saldoTotal < jumlahInput!!) {
                        binding.jumlah.requestFocus()
                        binding.jumlah.error = ("Saldo tidak mencukupi. Sisa saldo tersedia: Rp %" +
                                ".2f").format(saldoTotal)
                        return@launch
                    }

                    updateTransaksiDanDetail(jumlahInput, keterangan)
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Terjadi kesalahan saat memperbarui transaksi")
                }
            }
        }
    }

    private fun isValidInput(jumlahInput: Double?): Boolean {
        if (selectedPgnId.isNullOrEmpty()) {
            binding.nama.error = "Silakan pilih nasabah"
            return false
        }

        if (jumlahInput == null || jumlahInput <= 0) {
            binding.jumlah.error = "Jumlah penarikan tidak valid"
            return false
        }

        return true
    }

    private suspend fun hitungSaldoDenganPenyesuaian(pgnId: String, jumlahSebelumnya: Double?):
            Double {
        val saldo = SupabaseProvider.client.postgrest.rpc(
            "hitung_saldo_pengguna",
            buildJsonObject { put("pgn_id_input", pgnId) }
        ).decodeAs<Double>()

        return saldo + jumlahSebelumnya!!
    }

    private fun updateTransaksiDanDetail(jumlahInput: Double, keterangan: String) {
        val transaksiId = riwayat.tskId
        val detailId = enrichedList.getOrNull(0)?.dtlId

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client.from("transaksi").update({
                        set("tskIdPengguna", selectedPgnId!!)
                        set("tskKeterangan", keterangan)
                    }) {
                        filter { eq("tskId", transaksiId) }
                    }

                    detailId?.let {
                        SupabaseProvider.client.from("detail_transaksi").update({
                            set("dtlJumlah", jumlahInput)
                        }) {
                            filter { eq("dtlId", it) }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editTransaksiKeluarFragment_to_navigation_transaksi)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal memperbarui data, periksa koneksi internet",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun tampilkanSaldoPengguna(pgnId: String) {
        lifecycleScope.launch {
            try {
                val saldo = hitungSaldoDenganPenyesuaian(pgnId, enrichedList[0].dtlJumlah)
                val formattedSaldo = NumberFormat.getNumberInstance(Locale("in", "ID")).format(saldo)
                binding.saldo.text = "Rp $formattedSaldo"
            } catch (e: Exception) {
                e.printStackTrace()
                binding.saldo.text = "Rp 0"
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}