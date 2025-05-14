package com.gemahripah.banksampah.ui.admin.transaksi.keluar

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.SaldoPengguna
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPenarikanSaldoBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PenarikanSaldoFragment : Fragment() {

    private var _binding: FragmentPenarikanSaldoBinding? = null
    private val binding get() = _binding!!

    // Menyimpan id pengguna yang dipilih
    private var selectedPgnId: String? = null

    // Map nama -> pengguna
    private val namaToPenggunaMap = mutableMapOf<String, Pengguna>()

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
                    // Panggil function hitung_saldo_pengguna di Supabase
                    val rpcParams = buildJsonObject {
                        put("pgn_id_input", selectedPgnId!!)
                    }

                    val response = SupabaseProvider.client.postgrest.rpc(
                        "hitung_saldo_pengguna",
                        rpcParams
                    )

                    Log.d("PenarikanSaldo", "Mengambil saldo untuk: $selectedPgnId")
                    Log.d("PenarikanSaldo", "Param terkirim ke RPC: $rpcParams")
                    Log.d("PenarikanSaldo", "Response JSON: ${response.data}")


                    val saldo: Double = response.decodeAs<Double>()

                    Log.d("PenarikanSaldo", "Saldo: $saldo")

                    if (saldo < jumlahInput) {
                        binding.jumlah.requestFocus()
                        binding.jumlah.error = "Saldo tidak mencukupi. Sisa saldo: Rp %.2f".format(saldo)
                    } else {
                        try {
                            // Upload transaksi (Keluar)
                            val transaksiResponse = SupabaseProvider.client.postgrest["transaksi"]
                                .insert(
                                    buildJsonObject {
                                        put("tskIdPengguna", selectedPgnId!!)
                                        put("tskKeterangan", keterangan)
                                        put("tskTipe", "Keluar")
                                    }
                                ) {
                                    select()
                                }.decodeSingle<Transaksi>()

                            val transaksiId = transaksiResponse.tskId
                            if (transaksiId != null) {
                                // Masukkan detail transaksi
                                SupabaseProvider.client.postgrest["detail_transaksi"]
                                    .insert(
                                        buildJsonObject {
                                            put("dtlTskId", transaksiId)
                                            put("dtlJumlah", jumlahInput)
                                        }
                                    )

                                // Berhasil melakukan penarikan
                                Toast.makeText(requireContext(), "Penarikan saldo berhasil", Toast.LENGTH_SHORT).show()

                                // Navigasi kembali ke daftar transaksi (ubah ID sesuai NavGraph kamu)
                                findNavController().navigate(R.id.action_penarikanSaldoFragment_to_navigation_transaksi)

                            } else {
                                println("Gagal mendapatkan ID transaksi")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Tampilkan error UI jika perlu
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
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

                // Cek apakah response.data berisi nilai yang valid
                val saldo = response.data.toDoubleOrNull()

                if (saldo != null) {
                    Log.d("PenarikanSaldo", "Saldo: $saldo")
                    // Tampilkan saldo di UI
                    binding.saldo.text = "Rp %.2f".format(saldo)
                } else {
                    Log.d("PenarikanSaldo", "Saldo tidak valid: ${response.data}")
                    // Tampilkan pesan error jika saldo tidak valid
                    binding.saldo.text = "0"
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}