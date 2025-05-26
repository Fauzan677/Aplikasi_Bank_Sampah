package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentPenggunaAdminBinding
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.ui.admin.beranda.BerandaFragmentDirections
import com.gemahripah.banksampah.ui.admin.beranda.adapter.NasabahAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PenggunaFragment : Fragment() {

    private var _binding: FragmentPenggunaAdminBinding? = null
    private val binding get() = _binding!!

    private var nasabahList = listOf<Pengguna>()
    private lateinit var nasabahAdapter: NasabahAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPenggunaAdminBinding.inflate(inflater, container, false)

        nasabahAdapter = NasabahAdapter(nasabahList) { pengguna ->
            val action = PenggunaFragmentDirections.actionPenggunaFragmentToEditPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }

        binding.rvListNasabah.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListNasabah.adapter = nasabahAdapter

        binding.tambah.setOnClickListener {
            findNavController().navigate(R.id.action_penggunaFragment_to_tambahPenggunaFragment)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchNasabah.doAfterTextChanged { text ->
            val keyword = text.toString().trim()
            if (keyword.isEmpty()) {
                nasabahAdapter = NasabahAdapter(nasabahList) { pengguna ->
                    val action = PenggunaFragmentDirections.actionPenggunaFragmentToEditPenggunaFragment(pengguna)
                    findNavController().navigate(action)
                }
                binding.rvListNasabah.adapter = nasabahAdapter
            } else {
                cariPengguna(keyword)
            }
        }

        binding.searchNasabah.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v)
                true
            } else {
                false
            }
        }

        ambilPengguna()
        ambilTotalNasabah()
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun cariPengguna(keyword: String) {
        val hasilFilter = nasabahList.filter {
            it.pgnNama?.contains(keyword, ignoreCase = true) == true
        }

        nasabahAdapter = NasabahAdapter(hasilFilter) { pengguna ->
            val action = PenggunaFragmentDirections.actionPenggunaFragmentToEditPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }
        binding.rvListNasabah.adapter = nasabahAdapter
    }


    private fun ambilPengguna() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val penggunaList = SupabaseProvider.client
                    .from("pengguna")
                    .select {
                        filter {
                            eq("pgnIsAdmin", "False")
                        }
                    }
                    .decodeList<Pengguna>()

                nasabahList = penggunaList

                launch(Dispatchers.Main) {
                    nasabahAdapter = NasabahAdapter(nasabahList) { pengguna ->
                        val action = PenggunaFragmentDirections.actionPenggunaFragmentToEditPenggunaFragment(pengguna)
                        findNavController().navigate(action)
                    }
                    binding.rvListNasabah.adapter = nasabahAdapter
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal memuat data pengguna", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ambilTotalNasabah() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseProvider.client.postgrest.rpc("hitung_total_nasabah")
                val jumlah = result.data.toString().toIntOrNull() ?: 0

                launch(Dispatchers.Main) {
                    binding.jumlah.text = jumlah.toString()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.jumlah.text = "Gagal memuat"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.searchNasabah.setText("")
        ambilPengguna()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}