package com.gemahripah.banksampah.admin.ui.sampah.tambah

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
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentSetorSampahBinding
import com.gemahripah.banksampah.databinding.ItemSetorSampahBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class SetorSampahFragment : Fragment() {

    private var _binding: FragmentSetorSampahBinding? = null
    private val binding get() = _binding!!

    private var jumlahInput = 1 // Awalnya 1 form
    private var jenisList: List<String> = emptyList() // Global list jenis sampah
    private var jenisToSatuanMap: Map<String, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetorSampahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadJenisSampah()

        binding.tambah.setOnClickListener {
            tambahInputSampah()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadJenisSampah() {
        lifecycleScope.launch {
            try {
                val data = SupabaseProvider.client
                    .from("sampah")
                    .select(columns = Columns.list("jenis", "satuan"))
                    .decodeList<Sampah>()

                jenisList = data.map { it.jenis }
                jenisToSatuanMap = data.associate { it.jenis to it.satuan } as Map<String, String>

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jenisList)
                binding.jenis1.setAdapter(adapter)

                binding.jenis1.setOnTouchListener { _, _ ->
                    binding.jenis1.showDropDown()
                    true
                }

                // Ubah label sesuai satuan ketika dipilih
                binding.jenis1.setOnItemClickListener { _, _, position, _ ->
                    val selectedJenis = jenisList[position]
                    val satuan = jenisToSatuanMap[selectedJenis] ?: "Unit"
                    binding.jumlahLabel1.text = "Jumlah Setor ($satuan)"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat jenis sampah", Toast.LENGTH_SHORT).show()
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
        binding.containerInput.addView(itemBinding.root, index)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}