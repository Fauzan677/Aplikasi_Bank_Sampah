package com.gemahripah.banksampah.ui.admin.transaksi.keluar.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPenarikanSaldoBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EditTransaksiKeluarFragment : Fragment() {
    private var _binding: FragmentPenarikanSaldoBinding? = null
    private val binding get() = _binding!!

    private val args: EditTransaksiKeluarFragmentArgs by navArgs()

    // Menyimpan id pengguna yang dipilih
    private var selectedPgnId: String? = null

    // Map nama -> pengguna
    private val namaToPenggunaMap = mutableMapOf<String, Pengguna>()

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
        fetchNamaPengguna()
        setupKonfirmasiButton()

        riwayat = args.riwayat
        enrichedList = args.enrichedList
        selectedPgnId = riwayat.tskIdPengguna

        // Tampilkan data sebelumnya di UI
        binding.nama.setText(riwayat.nama, false)
        binding.jumlah.setText(enrichedList[0].dtlJumlah.toString())
        binding.keterangan.setText(riwayat.tskKeterangan ?: "")
        selectedPgnId?.let { tampilkanSaldoPengguna(it) }

    }

    private fun fetchNamaPengguna() {
        lifecycleScope.launch {
            try {
                val penggunaList = SupabaseProvider.client
                    .postgrest["pengguna"]
                    .select()
                    .decodeList<Pengguna>()

                // Buat map nama -> Pengguna
                namaToPenggunaMap.clear()
                penggunaList.forEach { pengguna ->
                    pengguna.pgnNama?.let {
                        namaToPenggunaMap[it] = pengguna
                    }
                }

                val namaList = namaToPenggunaMap.keys.toList()

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    namaList
                )

                binding.nama.setAdapter(adapter)
                binding.nama.threshold = 1

                // Set listener saat nama dipilih
                binding.nama.setOnItemClickListener { _: AdapterView<*>, _, position, _ ->
                    val selectedNama = adapter.getItem(position)
                    val penggunaTerpilih = namaToPenggunaMap[selectedNama]
                    selectedPgnId = penggunaTerpilih?.pgnId

                    if (selectedPgnId != null) {
                        tampilkanSaldoPengguna(selectedPgnId!!)
                    }
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupKonfirmasiButton() {
        binding.konfirmasi.setOnClickListener {
            val jumlahInput = binding.jumlah.text.toString().toDoubleOrNull()
            val keterangan = binding.keterangan.text.toString()

            if (selectedPgnId.isNullOrEmpty()) {
                binding.nama.error = "Silakan pilih nasabah"
                return@setOnClickListener
            }

            if (jumlahInput == null || jumlahInput <= 0) {
                binding.jumlah.error = "Jumlah penarikan tidak valid"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Ambil saldo pengguna terlebih dahulu
                    val saldoResponse = SupabaseProvider.client.postgrest.rpc(
                        "hitung_saldo_pengguna",
                        buildJsonObject {
                            put("pgn_id_input", selectedPgnId!!)
                        }
                    )
                    val saldo = saldoResponse.decodeAs<Double>()

                    val jumlahSebelumnya = enrichedList.getOrNull(0)?.dtlJumlah ?: 0.0
                    val saldoSementara = saldo + jumlahSebelumnya

                    if (saldoSementara < jumlahInput) {
                        binding.jumlah.requestFocus()
                        binding.jumlah.error = "Saldo tidak mencukupi. Sisa saldo tersedia: Rp %.2f".format(saldoSementara)
                        return@launch
                    }


                    // Ambil data transaksi dan detail dari args
                    val riwayat = args.riwayat
                    val enrichedList = args.enrichedList
                    val transaksiId = riwayat.tskId
                    val detailId = enrichedList.getOrNull(0)?.dtlId

                    // ✅ Update tabel transaksi
                    SupabaseProvider.client.from("transaksi").update({
                        set("tskIdPengguna", selectedPgnId!!)
                        set("tskKeterangan", keterangan)
                    }) {
                        filter {
                            eq("tskId", transaksiId)
                        }
                    }

                    // ✅ Update tabel detail_transaksi
                    if (detailId != null) {
                        SupabaseProvider.client.from("detail_transaksi").update({
                            set("dtlJumlah", jumlahInput)
                        }) {
                            filter {
                                eq("dtlId", detailId)
                            }
                        }
                    }

                    Toast.makeText(requireContext(), "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editTransaksiKeluarFragment_to_navigation_transaksi)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Terjadi kesalahan saat memperbarui transaksi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun tampilkanSaldoPengguna(pgnId: String) {
        lifecycleScope.launch {
            try {
                val response = SupabaseProvider.client.postgrest.rpc(
                    "hitung_saldo_pengguna",
                    buildJsonObject {
                        put("pgn_id_input", pgnId)
                    }
                )

                val saldo = response.decodeAs<Double>()

                if (saldo != null) {
                    // Ambil jumlah sebelumnya dari detail transaksi
                    val jumlahSebelumnya = enrichedList.getOrNull(0)?.dtlJumlah ?: 0.0
                    val saldoTotal = saldo + jumlahSebelumnya

                    Log.d("PenarikanSaldo", "Saldo asli: $saldo, + jumlah sebelumnya: $jumlahSebelumnya")

                    // Tampilkan saldo total di UI
                    binding.saldo.text = "Rp %.2f".format(saldoTotal)
                } else {
                    Log.d("PenarikanSaldo", "Saldo tidak valid: ${response.data}")
                    binding.saldo.text = "Rp 0"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                binding.saldo.text = "Rp 0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}