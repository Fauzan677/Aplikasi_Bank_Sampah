package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.sampah.Kategori
import com.gemahripah.banksampah.data.supabase.SupabaseProvider.client
import com.gemahripah.banksampah.databinding.FragmentTambahKategoriBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditKategoriFragment : Fragment() {

    private var _binding: FragmentTambahKategoriBinding? = null
    private val binding get() = _binding!!

    private val args: EditKategoriFragmentArgs by navArgs()
    private lateinit var kategori: Kategori

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        kategori = args.kategori

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.judul.text = "Edit Kategori Sampah"
        binding.kategori.setText(kategori.ktgNama)
    }

    private fun setupListeners() {
        binding.konfirmasi.setOnClickListener {
            updateKategori()
        }

        binding.hapus.setOnClickListener {
            deleteKategori()
        }
    }

    private fun updateKategori() {
        val namaBaru = binding.kategori.text.toString().trim()
        val id = kategori.ktgId

        if (id != null && namaBaru.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    client.from("kategori").update(
                        {
                            set("ktgNama", namaBaru)
                        }
                    ) {
                        filter {
                            eq("ktgId", id)
                        }
                    }

                    showToast("Kategori berhasil diperbarui")
                    navigateToJenisSampah()

                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Gagal memperbarui kategori")
                }
            }
        } else {
            showToast("Nama kategori tidak boleh kosong")
        }
    }

    private fun deleteKategori() {
        val id = kategori.ktgId

        if (id != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    client.from("kategori").delete {
                        filter {
                            eq("ktgId", id)
                        }
                    }

                    showToast("Kategori berhasil dihapus")
                    navigateToJenisSampah()

                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Gagal menghapus kategori")
                }
            }
        }
    }

    private fun navigateToJenisSampah() {
        findNavController().navigate(
            R.id.action_editKategoriFragment_to_jenisSampahFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.editKategoriFragment, true) // agar tidak bisa kembali
                .build()
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}