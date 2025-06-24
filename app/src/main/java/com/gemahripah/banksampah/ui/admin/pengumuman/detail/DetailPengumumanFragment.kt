package com.gemahripah.banksampah.ui.admin.pengumuman.detail

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailPengumumanBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetailPengumumanFragment : Fragment() {

    private var _binding: FragmentDetailPengumumanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailPengumumanViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pengumuman = arguments?.let {
            DetailPengumumanFragmentArgs.fromBundle(it).pengumuman
        }

        pengumuman?.let {
            viewModel.setPengumuman(it)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.pengumuman.observe(viewLifecycleOwner) { pengumuman ->
            tampilkanDetailPengumuman(pengumuman)
            aturAksiKlik(pengumuman)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
    }

    private fun tampilkanDetailPengumuman(pengumuman: Pengumuman) {
        binding.judul.text = pengumuman.pmnJudul
        binding.isiPengumuman.text = pengumuman.pmnIsi

        if (!pengumuman.updated_at.isNullOrBlank()) {
            binding.update.text = formatTanggalUpdate(pengumuman.updated_at)
            binding.update.visibility = View.VISIBLE
        } else {
            binding.update.visibility = View.GONE
        }

        if (!pengumuman.pmnGambar.isNullOrBlank()) {
            binding.gambar.visibility = View.VISIBLE
            val imageUrlWithUpdatedAt = "${pengumuman.pmnGambar}?v=${pengumuman.updated_at}"

            Glide.with(requireContext())
                .load(imageUrlWithUpdatedAt)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.gambar)
        } else {
            binding.gambar.visibility = View.GONE
        }
    }

    private fun aturAksiKlik(pengumuman: Pengumuman) {
        binding.gambar.setOnClickListener {
            pengumuman.pmnGambar?.let {
                tampilkanDialogZoomGambar(it, pengumuman.updated_at)
            }
        }

        binding.edit.setOnClickListener {
            val action = DetailPengumumanFragmentDirections
                .actionDetailPengumumanFragmentToEditPengumumanFragment(pengumuman)
            findNavController().navigate(action)
        }

        binding.hapus.setOnClickListener {
            pengumuman.pmnId?.let { id ->
                tampilkanDialogKonfirmasiHapus(id, pengumuman.pmnGambar) {
                    Toast.makeText(requireContext(), "Pengumuman berhasil dihapus", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun tampilkanDialogZoomGambar(gambarUrl: String, updatedAt: String?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_zoom_image, null)
        val photoView = dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.photo_view)

        val urlWithVersion = "$gambarUrl?v=$updatedAt"

        Glide.with(requireContext())
            .load(urlWithVersion)
            .into(photoView)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun tampilkanDialogKonfirmasiHapus(
        pengumumanId: Long,
        gambarUrl: String?,
        onHapusConfirmed: () -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah kamu yakin ingin menghapus pengumuman ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.hapusPengumuman(pengumumanId, gambarUrl) {
                    onHapusConfirmed()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun formatTanggalUpdate(updatedAt: String): String {
        return try {
            val zonedDateTime = ZonedDateTime.parse(updatedAt)
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))
            "Terakhir Diupdate: ${zonedDateTime.format(formatter)}"
        } catch (e: Exception) {
            ""
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        binding.hapus.isEnabled = !isLoading
        binding.edit.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}