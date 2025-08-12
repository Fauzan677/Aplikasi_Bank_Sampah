package com.gemahripah.banksampah.ui.admin.transaksi.masuk.edit

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch

class EditTransaksiMasukFragment : Fragment(), Reloadable {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private val vm: EditTransaksiMasukViewModel by viewModels()
    private val args: EditTransaksiMasukFragmentArgs by navArgs()

    private val tambahanSampahList = mutableListOf<ItemSetorSampahBinding>()
    private var selectedUserId: String? = null
    private var prefillDone = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetorSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUiStatic()
        setupListeners()
        collectVm()

        if (!updateInternetCard()) return

        vm.loadSampah()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
        vm.loadSampah()
    }

    // --- UI static ---
    @SuppressLint("SetTextI18n")
    private fun setupUiStatic() {
        val riwayat = args.riwayat
        binding.judul.text = "Edit Menabung Sampah"
        binding.nama.setText(riwayat.nama)
        binding.keterangan.setText(riwayat.tskKeterangan)

        // nama tidak bisa diubah (sesuai kode lama)
        binding.nama.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isEnabled = false
        }
        selectedUserId = riwayat.tskIdPengguna
    }

    private fun setupListeners() {
        binding.tambah.setOnClickListener { tambahInputSampah() }
        binding.konfirmasi.setOnClickListener { onKonfirmasiClicked() }
    }

    private fun collectVm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

                launch {
                    vm.isLoading.collect { showLoading(it) }
                }

                launch {
                    vm.toast.collect { requireContext().toast(it) }
                }

                launch {
                    vm.navigateBack.collect {
                        findNavController().navigate(
                            R.id.navigation_transaksi, null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.editTransaksiMasukFragment, true)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                }

                // Master jenis => set adapter utama + prefill dari args sekali
                launch {
                    vm.jenisList.collect { jenisList ->
                        if (jenisList.isEmpty()) return@collect

                        setAdapterJenisUtama(jenisList)

                        if (!prefillDone) {
                            prefillFromArgs(jenisList)
                            prefillDone = true
                        }

                        updateAllAdapters() // refresh opsi anti-duplikasi
                    }
                }
            }
        }
    }

    // Prefill field utama + item tambahan dari args.enrichedList
    @SuppressLint("SetTextI18n")
    private fun prefillFromArgs(jenisList: List<String>) {
        val enrichedList = args.enrichedList
        if (enrichedList.isEmpty()) return

        val first = enrichedList[0]
        val jenisPertama = first.dtlSphId?.sphJenis.orEmpty()
        val jumlahPertama = first.dtlJumlah
        binding.jenis1.setText(jenisPertama, false)
        binding.jumlah1.setText(jumlahPertama.toString())
        binding.jumlahLabel1.text = "Jumlah Setor (${vm.getSatuan(jenisPertama)})"

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

    // --- Adapters ---
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setAdapterJenisUtama(list: List<String>) {
        binding.jenis1.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list))
        binding.jenis1.setOnTouchListener { _, _ ->
            binding.jenis1.showDropDown()
            false
        }
        binding.jenis1.setOnItemClickListener { _, _, position, _ ->
            val selected = list[position]
            binding.jumlahLabel1.text = "Jumlah Setor (${vm.getSatuan(selected)})"
            updateAllAdapters()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setupAutoCompleteJenis(itemBinding: ItemSetorSampahBinding, index: Int, list: List<String>) {
        itemBinding.jenisSampahLabel.text = "Jenis Sampah ${index + 2}"
        itemBinding.jumlahSetorLabel.text = "Jumlah Setor ${index + 2}"

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list)
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
            val selected = list[position]
            itemBinding.jumlahSetorLabel.text = "Jumlah Setor ${index + 2} (${vm.getSatuan(selected)})"
            updateAllAdapters()
        }
    }

    // --- Dynamic row ---
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun tambahInputSampah() {
        val index = tambahanSampahList.size
        val itemBinding = ItemSetorSampahBinding.inflate(layoutInflater)

        setupAutoCompleteJenis(itemBinding, index, vm.jenisList.value)

        itemBinding.hapusTambahan.setOnClickListener {
            binding.layoutKonten.removeView(itemBinding.root)
            tambahanSampahList.remove(itemBinding)
            perbaruiLabelInput()
            updateAllAdapters()
        }

        tambahanSampahList.add(itemBinding)
        val position = binding.layoutKonten.indexOfChild(binding.tambah)
        binding.layoutKonten.addView(itemBinding.root, position)

        updateAllAdapters()
    }

    @SuppressLint("SetTextI18n")
    private fun perbaruiLabelInput() {
        tambahanSampahList.forEachIndexed { idx, item ->
            item.jenisSampahLabel.text = "Jenis Sampah ${idx + 2}"
            item.jumlahSetorLabel.text = "Jumlah Setor ${idx + 2}"
        }
    }

    // Hitung pilihan yang tersedia (anti-duplikasi) via ViewModel
    private fun updateAllAdapters() {
        val selectedNow = buildSetSelectedJenis()

        // utama
        val currentMain = binding.jenis1.text?.toString()
        val availMain = vm.getAvailableJenis(currentMain, selectedNow - currentMain.orEmpty())
        binding.jenis1.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availMain))

        // tambahan
        tambahanSampahList.forEach { item ->
            val cur = item.autoCompleteJenis.text?.toString()
            val avail = vm.getAvailableJenis(cur, selectedNow - cur.orEmpty())
            item.autoCompleteJenis.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, avail)
            )
        }

        // toggle tombol tambah
        val noMore = vm.getAvailableJenis(null, selectedNow).isEmpty()
        binding.tambah.isEnabled = !noMore
        binding.tambah.alpha = if (binding.tambah.isEnabled) 1f else 0.5f
    }

    private fun buildSetSelectedJenis(): Set<String> {
        val set = mutableSetOf<String>()
        binding.jenis1.text?.toString()?.takeIf { it.isNotBlank() }?.let { set += it }
        tambahanSampahList.forEach { binding ->
            binding.autoCompleteJenis.text?.toString()?.takeIf { it.isNotBlank() }?.let { set += it }
        }
        return set
    }

    // --- Submit ---
    private fun onKonfirmasiClicked() {
        val userId = selectedUserId
        val keterangan = binding.keterangan.text.toString()

        if (userId == null) {
            requireContext().toast("Silakan pilih nama nasabah dari daftar")
            return
        }

        val jenisUtama = binding.jenis1.text.toString()
        if (jenisUtama.isBlank()) {
            requireContext().toast("Jenis sampah tidak boleh kosong")
            return
        }
        val jumlahUtama = binding.jumlah1.text.toString().toDoubleOrNull()
        if (jumlahUtama == null || jumlahUtama <= 0.0) {
            requireContext().toast("Jumlah sampah harus lebih dari 0")
            return
        }

        val tambahan = mutableListOf<Pair<String, Double>>()
        tambahanSampahList.forEachIndexed { idx, item ->
            val jenis = item.autoCompleteJenis.text.toString()
            if (jenis.isBlank()) {
                requireContext().toast("Jenis sampah ${idx + 2} tidak boleh kosong")
                return
            }
            val jumlah = item.editTextJumlah.text.toString().toDoubleOrNull()
            if (jumlah == null || jumlah <= 0.0) {
                requireContext().toast("Jumlah sampah ${idx + 2} harus lebih dari 0")
                return
            }
            tambahan += jenis to jumlah
        }

        vm.submitEditTransaksiMasuk(
            transaksiId = args.riwayat.tskId,
            userId = userId,
            keterangan = keterangan,
            main = jenisUtama to jumlahUtama,
            tambahan = tambahan
        )
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.konfirmasi.isEnabled = !isLoading
        binding.tambah.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun String?.orEmpty() = this ?: ""

    private fun android.content.Context.toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}