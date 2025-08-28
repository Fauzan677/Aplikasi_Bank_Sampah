package com.gemahripah.banksampah.ui.admin.transaksi.masuk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SetorSampahFragment : Fragment(), Reloadable {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SetorSampahViewModel by viewModels()

    // Menampung view input tambahan
    private val tambahanSampahList = mutableListOf<ItemSetorSampahBinding>()

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

        bindArgsIfAny()
        setupActions()
        setupCollectors()

        if (!updateInternetCard()) return
        requestInitialData()
    }

    // --- Public API ---
    override fun reloadData() {
        if (!updateInternetCard()) return
        requestInitialData()
    }

    // --- Setup & wiring ---
    private fun bindArgsIfAny() {
        val pengguna = arguments?.let { SetorSampahFragmentArgs.fromBundle(it).pengguna }
        pengguna?.let {
            binding.nama.setText(it.pgnNama, false)
            viewModel.selectedUserId = it.pgnId
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupActions() {
        binding.tambah.setOnClickListener { tambahInputSampah() }
        binding.konfirmasi.setOnClickListener { onKonfirmasiClicked() }
    }

    private fun setupCollectors() {
        // Pengguna
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.penggunaList.collect { penggunaList ->
                    setupNamaAutoComplete(penggunaList)
                }
            }
        }

        // Sampah
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sampahList.collect { sampahList ->
                    setupJenisUtamaAutoComplete(sampahList)
                }
            }
        }

        // Loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { showLoading(it) }
            }
        }
    }

    private fun requestInitialData() {
        viewModel.loadPengguna()
        viewModel.loadSampah()
    }

    // --- UI builders / adapters ---
    private fun setupNamaAutoComplete(penggunaList: List<Pengguna>) {
        val namaList = penggunaList.mapNotNull { it.pgnNama }.distinct()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, namaList)

        binding.nama.apply {
            setAdapter(adapter)
            threshold = 1

            setOnTouchListener(null)

            addTextChangedListener(onTextChanged = { text, _, _, _ ->
                if (hasFocus()) {
                    if (text.isNullOrEmpty()) {
                        dismissDropDown()
                    } else {
                        if (!isPopupShowing) showDropDown()
                    }
                }
            })

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && !text.isNullOrEmpty()) {
                    post { showDropDown() }
                }
            }

            setOnItemClickListener { _, _, position, _ ->
                val selectedNama = adapter.getItem(position)
                val namaToId = penggunaList.associateBy { it.pgnNama }
                viewModel.selectedUserId = namaToId[selectedNama]?.pgnId
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupJenisUtamaAutoComplete(sampahList: List<Sampah>) {
        val jenisList = sampahList.mapNotNull { it.sphJenis }.distinct()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jenisList)

        binding.jenis1.apply {
            setAdapter(adapter)
            threshold = 1
            // jangan paksa showDropDown onTouch; biar mirip field nama
            setOnTouchListener(null)

            addTextChangedListener(onTextChanged = { text, _, _, _ ->
                if (hasFocus()) {
                    if (text.isNullOrEmpty()) dismissDropDown()
                    else if (!isPopupShowing) showDropDown()
                }
            })

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && !text.isNullOrEmpty()) post { showDropDown() }
            }

            setOnItemClickListener { parent, _, position, _ ->
                val selectedJenis = parent.getItemAtPosition(position) as String  // adapter aktif
                val satuan = viewModel.jenisToSatuanMap[selectedJenis] ?: "Unit"
                binding.jumlahLabel1.text = "Jumlah Setor ($satuan)"
                updateAllAdapters()
            }
        }
    }

    // --- Actions ---
    private fun onKonfirmasiClicked() {
        val input = validateAndBuildInput() ?: return
        viewModel.simpanTransaksi(
            keterangan = input.keterangan,
            userId = input.userId,
            inputUtama = input.main,
            inputTambahan = input.tambahan,
            onSuccess = {
                requireContext().toast("Data berhasil disimpan")

                // kunci tombol lalu langsung pop back
                binding.konfirmasi.isEnabled = false
                findNavController().popBackStack()
            },
            onError = {
                requireContext().toast("Gagal menyimpan, periksa koneksi internet")
            }
        )
    }

    // --- Validation + collect inputs ---
    private data class InputData(
        val keterangan: String,
        val userId: String,
        val main: Pair<String, BigDecimal>,
        val tambahan: List<Pair<String, BigDecimal>>,
    )

    private fun validateAndBuildInput(): InputData? {
        val userId = viewModel.selectedUserId
        val keterangan = binding.keterangan.text.toString()

        if (userId == null) {
            requireContext().toast("Silakan pilih nama nasabah dari daftar")
            return null
        }

        val jenisUtama = binding.jenis1.text.toString()
        if (jenisUtama.isBlank() || !isInAdapter(binding.jenis1, jenisUtama)) {
            requireContext().toast("Pilih jenis sampah dari daftar")
            return null
        }

        val jumlahUtama = binding.jumlah1.text.toString().toBigDecimalFlexible()
        if (jumlahUtama == null || jumlahUtama <= BigDecimal.ZERO) {
            requireContext().toast("Jumlah sampah harus lebih dari 0")
            return null
        }

        val tambahan = mutableListOf<Pair<String, BigDecimal>>()
        tambahanSampahList.forEachIndexed { index, item ->
            val jenis = item.autoCompleteJenis.text.toString()
            if (jenis.isBlank() || !isInAdapter(item.autoCompleteJenis, jenis)) {
                requireContext().toast("Jenis sampah ${index + 2} tidak valid. Pilih dari daftar")
                return null
            }
            val jumlah = item.editTextJumlah.text.toString().toBigDecimalFlexible()
            if (jumlah == null || jumlah <= BigDecimal.ZERO) {
                requireContext().toast("Jumlah sampah ${index + 2} harus lebih dari 0")
                return null
            }
            tambahan += jenis to jumlah
        }

        return InputData(
            keterangan = keterangan,
            userId = userId,
            main = jenisUtama to jumlahUtama,
            tambahan = tambahan
        )
    }

    // --- Dynamic additional inputs ---
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun tambahInputSampah() {
        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)

        val nextIndex = tambahanSampahList.size + 2
        itemBinding.jenisSampahLabel.text = "Jenis Sampah $nextIndex"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor $nextIndex"

        val currentJenisList = viewModel.sampahList.value.mapNotNull { it.sphJenis }.distinct()
        val jenisAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currentJenisList)

        with(itemBinding.autoCompleteJenis) {
            setAdapter(jenisAdapter)
            threshold = 1
            setOnTouchListener(null)

            addTextChangedListener(onTextChanged = { text, _, _, _ ->
                if (hasFocus()) {
                    if (text.isNullOrEmpty()) dismissDropDown()
                    else if (!isPopupShowing) showDropDown()
                }
            })
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && !text.isNullOrEmpty()) post { showDropDown() }
            }

            setOnItemClickListener { parent, _, position, _ ->
                val jenis = parent.getItemAtPosition(position) as String          // adapter aktif
                val satuan = viewModel.jenisToSatuanMap[jenis] ?: "Unit"
                itemBinding.jumlahSetorLabel.text = "Jumlah Setor ($satuan)"
                updateAllAdapters()
            }
        }

        itemBinding.hapusTambahan.setOnClickListener {
            binding.containerTambahan.removeView(itemBinding.root)
            tambahanSampahList.remove(itemBinding)
            perbaruiLabelInput()
            updateAllAdapters()
        }

        binding.containerTambahan.addView(itemBinding.root)
        tambahanSampahList.add(itemBinding)
        updateAllAdapters()
    }

    @SuppressLint("SetTextI18n")
    private fun perbaruiLabelInput() {
        tambahanSampahList.forEachIndexed { idx, item ->
            item.jenisSampahLabel.text = "Jenis Sampah ${idx + 2}"
            item.jumlahSetorLabel.text = "Jumlah Setor ${idx + 2}"
        }
    }

    // --- Jenis helpers (anti duplikasi pilihan) ---
    private fun getAvailableJenis(currentJenis: String?): List<String> {
        val selected = mutableSetOf<String>()
        binding.jenis1.text?.toString()?.takeIf { it.isNotBlank() }?.let { selected += it }
        tambahanSampahList.forEach { item ->
            val j = item.autoCompleteJenis.text?.toString()
            if (!j.isNullOrBlank()) selected += j
        }
        // boleh tetap terlihat pada field yang sedang aktif
        currentJenis?.let { selected.remove(it) }

        val all = viewModel.sampahList.value.mapNotNull { it.sphJenis }.distinct()
        return (all.filter { it.isNotBlank() && it !in selected } +
                listOfNotNull(currentJenis?.takeIf { it.isNotBlank() })
                ).distinct()
    }

    private fun updateAllAdapters() {
        // utama
        val currentMain = binding.jenis1.text.toString()
        binding.jenis1.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, getAvailableJenis(currentMain))
        )

        // tambahan
        tambahanSampahList.forEach { item ->
            val cur = item.autoCompleteJenis.text.toString()
            item.autoCompleteJenis.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, getAvailableJenis(cur))
            )
        }

        // toggle tombol tambah kalau semua jenis sudah terpakai
        val semuaJenis = viewModel.sampahList.value.mapNotNull { it.sphJenis }
        val semuaTerpakai = semuaJenis.all { jenis ->
            jenis == binding.jenis1.text.toString() ||
                    tambahanSampahList.any { it.autoCompleteJenis.text.toString() == jenis }
        }
        binding.tambah.isEnabled = !semuaTerpakai
        binding.tambah.alpha = if (binding.tambah.isEnabled) 1f else 0.5f
    }

    // --- Misc / UI utils ---
    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? AdminActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
        binding.tambah.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        binding.containerTambahan.removeAllViews()
        tambahanSampahList.clear()
        _binding = null
        super.onDestroyView()
    }

    // --- Extensions kecil ---
    private fun Context.toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // Cek apakah value ada di adapter aktif
    private fun isInAdapter(view: AutoCompleteTextView, value: String): Boolean {
        val a = view.adapter ?: return false
        val v = value.trim()
        for (i in 0 until a.count) {
            val item = a.getItem(i)?.toString() ?: continue
            if (item.equals(v, ignoreCase = true)) return true
        }
        return false
    }

    private fun String.toBigDecimalFlexible(): BigDecimal? =
        this.trim()
            .replace(",", ".")           // terima koma sebagai desimal
            .toBigDecimalOrNull()
}