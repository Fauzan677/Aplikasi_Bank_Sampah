package com.gemahripah.banksampah.ui.admin.transaksi.masuk.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTransaksiMasukFragment : Fragment() {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private var namaToIdMap: Map<String, Long> = emptyMap()
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
        loadJenisSampahDenganDataAwal()
        setupListeners()
    }

    private fun setupListeners() {
        binding.tambah.setOnClickListener {
            tambahInputSampah()
        }

        binding.konfirmasi.setOnClickListener {
            val userId = selectedUserId
            val keterangan = binding.keterangan.text.toString()
            val transaksiId = args.riwayat.tskId

            if (userId == null) {
                Toast.makeText(requireContext(), "Silakan pilih nama nasabah dari daftar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateTransaksi(transaksiId, userId, keterangan)
        }
    }

    private fun updateTransaksi(transaksiId: Long, userId: String, keterangan: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .from("transaksi")
                        .update({
                            set("tskIdPengguna", userId)
                            set("tskKeterangan", keterangan)
                            set("tskTipe", "Masuk")
                        }) {
                            filter { eq("tskId", transaksiId) }
                        }

                    hapusDetailTransaksiLama(transaksiId)
                    tambahDetailBaru(transaksiId)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Data berhasil diperbarui", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().navigate(R.id.action_editTransaksiMasukFragment_to_navigation_transaksi)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal memperbarui data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun hapusDetailTransaksiLama(transaksiId: Long) {
        SupabaseProvider.client
            .from("detail_transaksi")
            .delete {
                filter { eq("dtlTskId", transaksiId) }
            }
    }

    private suspend fun tambahDetailBaru(transaksiId: Long) {
        val jenisUtama = binding.jenis1.text.toString()
        val sampahIdUtama = namaToIdMap[jenisUtama]
        val jumlahUtama = binding.jumlah1.text.toString().toDoubleOrNull() ?: 0.0

        if (sampahIdUtama != null && jumlahUtama > 0) {
            val detailUtama = DetailTransaksi(
                dtlTskId = transaksiId,
                dtlSphId = sampahIdUtama,
                dtlJumlah = jumlahUtama
            )
            SupabaseProvider.client.from("detail_transaksi").insert(detailUtama)
        }

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
                SupabaseProvider.client.from("detail_transaksi").insert(detail)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun loadJenisSampahDenganDataAwal() {
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .from("sampah")
                        .select()
                        .decodeList<Sampah>()
                }

                val namaList = data.mapNotNull { it.sphJenis?.takeIf { jenis -> jenis.isNotBlank() } }

                namaToIdMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                jenisToSatuanMap = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
                jenisList = namaList

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        namaList
                    )
                    binding.jenis1.setAdapter(adapter)
                    binding.jenis1.setOnTouchListener { _, _ ->
                        binding.jenis1.showDropDown()
                        false
                    }
                    binding.jenis1.setOnItemClickListener { _, _, position, _ ->
                        val selectedJenis = namaList[position]
                        val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
                        binding.jumlahLabel1.text = "Jumlah Setor ($satuan)"
                    }

                    val riwayat = args.riwayat
                    val enrichedList = args.enrichedList

                    binding.judul.text = "Edit Menabung Sampah"
                    binding.nama.setText(riwayat.nama)
                    binding.keterangan.setText(riwayat.tskKeterangan)
                    selectedUserId = riwayat.tskIdPengguna

                    if (enrichedList.isNotEmpty()) {
                        val first = enrichedList[0]
                        val jenisPertama = first.dtlSphId?.sphJenis.orEmpty()
                        val jumlahPertama = first.dtlJumlah

                        binding.jenis1.setText(jenisPertama, false)
                        binding.jumlah1.setText(jumlahPertama.toString())

                        val satuanPertama = jenisToSatuanMap[jenisPertama] ?: "Unit"
                        binding.jumlahLabel1.text = "Jumlah Setor ($satuanPertama)"

                        for (i in 1 until enrichedList.size) {
                            val item = enrichedList[i]
                            tambahInputSampah()

                            val inputView = tambahanSampahList.last()
                            val jenis = item.dtlSphId?.sphJenis.orEmpty()
                            val jumlah = item.dtlJumlah

                            inputView.autoCompleteJenis.post {
                                inputView.autoCompleteJenis.setText(jenis, false)
                                inputView.editTextJumlah.setText(jumlah.toString())
                                updateAllAdapters()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat jenis transaksi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadNamaNasabah() {
        lifecycleScope.launch {
            try {
                val penggunaList = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .from("pengguna")
                        .select()
                        .decodeList<Pengguna>()
                }

                Log.d("SetorSampahFragment", "Jumlah pengguna yang diambil: ${penggunaList.size}")

                val namaToPenggunaMap = penggunaList.associateBy { it.pgnNama }
                val namaList = penggunaList.mapNotNull { it.pgnNama }.distinct()

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        namaList
                    )
                    binding.nama.setAdapter(adapter)

                    binding.nama.threshold = 1
                    binding.nama.setOnTouchListener { _, _ ->
                        binding.nama.showDropDown()
                        false
                    }

                    binding.nama.setOnItemClickListener { _, _, position, _ ->
                        val selectedNama = adapter.getItem(position)
                        val selectedPengguna = namaToPenggunaMap[selectedNama]
                        selectedUserId = selectedPengguna?.pgnId
                        Log.d("SetorSampahFragment", "ID pengguna terpilih: $selectedUserId")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat nama nasabah",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun tambahInputSampah() {
        val index = tambahanSampahList.size
        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)

        setupAutoCompleteJenis(itemBinding, index)

        val dynamicId = View.generateViewId()
        itemBinding.hapusTambahan.id = dynamicId

        itemBinding.hapusTambahan.setOnClickListener {
            binding.containerInput.removeView(itemBinding.root)
            tambahanSampahList.remove(itemBinding)
            perbaruiLabelInput()
            updateAllAdapters()
        }

        tambahanSampahList.add(itemBinding)
        val position = binding.containerInput.indexOfChild(binding.tambah)
        binding.containerInput.addView(itemBinding.root, position)

        updateAllAdapters()
    }

    @SuppressLint("SetTextI18n")
    private fun perbaruiLabelInput() {
        tambahanSampahList.forEachIndexed { index, itemBinding ->
            itemBinding.jenisSampahLabel.text = "Jenis Sampah ${index + 2}"
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor ${index + 2}"
            itemBinding.hapusTambahan.id = View.generateViewId()
        }
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setupAutoCompleteJenis(
        itemBinding: ItemSetorSampahBinding,
        index: Int
    ) {
        itemBinding.jenisSampahLabel.text = "Jenis Sampah ${index + 2}"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor ${index + 2}"

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jenisList)
        itemBinding.autoCompleteJenis.setAdapter(adapter)

        itemBinding.autoCompleteJenis.inputType = InputType.TYPE_NULL
        itemBinding.autoCompleteJenis.isFocusable = true
        itemBinding.autoCompleteJenis.isFocusableInTouchMode = true
        itemBinding.autoCompleteJenis.isClickable = true

        itemBinding.autoCompleteJenis.setOnTouchListener { _, _ ->
            itemBinding.autoCompleteJenis.showDropDown()
            true
        }

        itemBinding.autoCompleteJenis.setOnItemClickListener { _, _, position, _ ->
            val selectedJenis = jenisList[position]
            val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor ${index + 2} ($satuan)"
            updateAllAdapters()
        }
    }

    private fun getAvailableJenis(currentJenis: String?): List<String> {
        val selectedJenisList = mutableSetOf<String>()

        binding.jenis1.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            selectedJenisList.add(it)
        }

        tambahanSampahList.forEach { item ->
            val jenis = item.autoCompleteJenis.text?.toString()
            if (!jenis.isNullOrEmpty()) {
                selectedJenisList.add(jenis)
            }
        }

        currentJenis?.let {
            selectedJenisList.remove(it)
        }

        return (jenisList
            .filter { it.isNotBlank() && it !in selectedJenisList } +
                listOfNotNull(currentJenis?.takeIf { it.isNotBlank() }))
            .distinct()
    }

    private fun updateAllAdapters() {
        val jenisUtama = binding.jenis1.text.toString()
        val availableJenisUtama = getAvailableJenis(jenisUtama)
        binding.jenis1.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableJenisUtama))

        tambahanSampahList.forEach { item ->
            val currentJenis = item.autoCompleteJenis.text.toString()
            val availableJenis = getAvailableJenis(currentJenis)
            item.autoCompleteJenis.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableJenis))
        }

        val semuaJenisTerpakai = getAvailableJenis(null).isEmpty()

        binding.tambah.isEnabled = !semuaJenisTerpakai
        binding.tambah.alpha = if (binding.tambah.isEnabled) 1f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}