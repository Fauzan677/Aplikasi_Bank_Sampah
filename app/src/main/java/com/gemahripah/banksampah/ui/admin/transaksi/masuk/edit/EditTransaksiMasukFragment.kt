package com.gemahripah.banksampah.ui.admin.transaksi.masuk.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class EditTransaksiMasukFragment : Fragment() {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private var jumlahInput = 1
    private var jenisList: List<String> = emptyList()
    private var jenisToSatuanMap: Map<String, String> = emptyMap()
    private val tambahanSampahList = mutableListOf<ItemSetorSampahBinding>()
    private var selectedUserId: String? = null

    private val args: EditTransaksiMasukFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetorSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadNamaNasabah()

        val riwayat = args.riwayat
        val enrichedList = args.enrichedList

        binding.nama.setText(riwayat.nama)
        binding.keterangan.setText(riwayat.tskKeterangan)

        // Simpan ID user
        selectedUserId = riwayat.tskIdPengguna

        // Isi data jenis sampah dan jumlah dari enrichedList
        lifecycleScope.launch {
            // Pastikan jenisList dan namaToIdMap sudah terisi
            loadJenisSampah()

            if (enrichedList.isNotEmpty()) {
                // Item pertama isi di input utama
                val first = enrichedList[0]
                binding.jenis1.setText(first.dtlSphId?.sphJenis, false)
                binding.jumlah1.setText(first.dtlJumlah.toString())

                // Sisanya dimasukkan sebagai tambahan
                for (i in 1 until enrichedList.size) {
                    val item = enrichedList[i]
                    tambahInputSampah()

                    val inputView = tambahanSampahList.last()
                    inputView.autoCompleteJenis.setText(item.dtlSphId?.sphJenis, false)
                    inputView.editTextJumlah.setText(item.dtlJumlah.toString())
                }
            }
        }

        binding.tambah.setOnClickListener {
            tambahInputSampah()
        }

        binding.konfirmasi.setOnClickListener {
            val userId = selectedUserId
            val keterangan = binding.keterangan.text.toString()

            if (userId == null) {
                Toast.makeText(requireContext(), "Silakan pilih nama nasabah dari daftar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaksiId = args.riwayat.tskId

            lifecycleScope.launch {
                try {
                    // 1. Update transaksi
                    SupabaseProvider.client
                        .from("transaksi")
                        .update(
                            {
                                set("tskIdPengguna", userId)
                                set("tskKeterangan", keterangan)
                                set("tskTipe", "Masuk")
                            }
                        ) {
                            filter {
                                eq("tskId", transaksiId)
                            }
                        }

                    // 2. Hapus semua detail_transaksi lama
                    SupabaseProvider.client
                        .from("detail_transaksi")
                        .delete {
                            filter {
                                eq("dtlTskId", transaksiId)
                            }
                        }

                    // 3. Tambahkan ulang detail_transaksi berdasarkan input saat ini
                    val jenisUtama = binding.jenis1.text.toString()
                    val sampahIdUtama = namaToIdMap[jenisUtama]
                    val jumlahUtama = binding.jumlah1.text.toString().toDoubleOrNull() ?: 0.0

                    if (sampahIdUtama != null && jumlahUtama > 0) {
                        val detailUtama = DetailTransaksi(
                            dtlTskId = transaksiId,
                            dtlSphId = sampahIdUtama,
                            dtlJumlah = jumlahUtama
                        )

                        SupabaseProvider.client
                            .from("detail_transaksi")
                            .insert(detailUtama)
                    }

                    // Tambahan input
                    for (item in tambahanSampahList) {
                        val jenis = item.autoCompleteJenis.text.toString()
                        val sampahId = namaToIdMap[jenis]
                        val jumlah = item.editTextJumlah.text.toString().toDoubleOrNull() ?: 0.0

                        if (sampahId != null && jumlah > 0) {
                            val detail = DetailTransaksi(
                                dtlTskId = transaksiId,
                                dtlSphId = sampahId,
                                dtlJumlah = jumlah
                            )

                            SupabaseProvider.client
                                .from("detail_transaksi")
                                .insert(detail)
                        }
                    }

                    Toast.makeText(requireContext(), "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editTransaksiMasukFragment_to_navigation_transaksi)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memperbarui data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }


    }

    private var namaToIdMap: Map<String, Long> = emptyMap()

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun loadJenisSampah() {
        lifecycleScope.launch {
            try {
                val data = SupabaseProvider.client
                    .from("sampah")
                    .select()
                    .decodeList<Sampah>()

                // Mapping nama jenis → id
                val namaList = data.mapNotNull { it.sphJenis } // This will exclude null values
                namaToIdMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                jenisToSatuanMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
                jenisList = namaList

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)
                binding.jenis1.setAdapter(adapter)
                binding.jenis1.threshold = 1
                binding.jenis1.setOnTouchListener { _, _ ->
                    binding.jenis1.showDropDown()
                    false
                }

                binding.jenis1.setOnItemClickListener { _, _, position, _ ->
                    val selectedJenis = namaList[position]
                    val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
                    binding.jumlahLabel1.text = "Jumlah Setor ($satuan)"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat jenis transaksi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadNamaNasabah() {
        lifecycleScope.launch {
            try {
                val penggunaList = SupabaseProvider.client
                    .from("pengguna")
                    .select()
                    .decodeList<Pengguna>()

                Log.d("SetorSampahFragment", "Jumlah pengguna yang diambil: ${penggunaList.size}")

                // Buat map nama -> pengguna untuk referensi nanti
                val namaToPenggunaMap = penggunaList.associateBy { it.pgnNama }

                val namaList = penggunaList.mapNotNull { it.pgnNama }.distinct()

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)
                binding.nama.setAdapter(adapter)

                // Biarkan bisa diketik manual dan juga tampilkan dropdown saat disentuh
                binding.nama.threshold = 1
                binding.nama.setOnTouchListener { _, _ ->
                    binding.nama.showDropDown()
                    false
                }

                // Simpan id pengguna ketika nama dipilih
                binding.nama.setOnItemClickListener { _, _, position, _ ->
                    val selectedNama = adapter.getItem(position)
                    val selectedPengguna = namaToPenggunaMap[selectedNama]
                    selectedUserId = selectedPengguna?.pgnId // <-- simpan ID-nya di variabel global
                    Log.d("SetorSampahFragment", "ID pengguna terpilih: $selectedUserId")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat nama nasabah", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun tambahInputSampah() {
        jumlahInput++

        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)

        itemBinding.jenisSampahLabel.text = "Jenis Sampah $jumlahInput"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor $jumlahInput"

        itemBinding.autoCompleteJenis.setTag("jenis$jumlahInput")
        itemBinding.editTextJumlah.setTag("jumlah$jumlahInput")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jenisList)
        itemBinding.autoCompleteJenis.setAdapter(adapter)

        itemBinding.autoCompleteJenis.inputType = 0
        itemBinding.autoCompleteJenis.isFocusable = false
        itemBinding.autoCompleteJenis.isClickable = true
        itemBinding.autoCompleteJenis.setOnTouchListener { _, _ ->
            itemBinding.autoCompleteJenis.showDropDown()
            true
        }

        // Ubah label jumlah sesuai satuan
        itemBinding.autoCompleteJenis.setOnItemClickListener { _, _, position, _ ->
            val selectedJenis = jenisList[position]
            val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor $jumlahInput ($satuan)"
        }

        val index = binding.containerInput.indexOfChild(binding.tambah)
        tambahanSampahList.add(itemBinding)
        binding.containerInput.addView(itemBinding.root, index)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}