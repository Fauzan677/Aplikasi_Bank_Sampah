package com.gemahripah.banksampah.ui.admin.pengumuman.detail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailPengumumanNasabahBinding
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

    private var _binding: FragmentDetailPengumumanNasabahBinding? = null
    private val binding get() = _binding!!

    private var imageUrlWithUpdatedAt: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPengumumanNasabahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pengumuman = arguments?.let {
            DetailPengumumanFragmentArgs.fromBundle(it).pengumuman
        }

        pengumuman?.let {
            tampilkanDetailPengumuman(it)
            aturAksiKlik(it)
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
            imageUrlWithUpdatedAt = "${pengumuman.pmnGambar}?v=${pengumuman.updated_at}"

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
                tampilkanDialogZoomGambar()
            }
        }

        binding.edit.setOnClickListener {
            val action = DetailPengumumanFragmentDirections
                .actionDetailPengumumanFragmentToEditPengumumanFragment(pengumuman)
            findNavController().navigate(action)
        }

        binding.hapus.setOnClickListener {
            pengumuman.pmnId?.let { id ->
                hapusPengumuman(id, pengumuman.pmnGambar)
            }
        }
    }

    private fun tampilkanDialogZoomGambar() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_zoom_image, null)
        val photoView = dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.photo_view)

        Glide.with(requireContext())
            .load(imageUrlWithUpdatedAt)
            .into(photoView)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun hapusPengumuman(pengumumanId: Long, gambarUrl: String?) {
        val supabase = SupabaseProvider.client

        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    hapusDataPengumumanDariDatabase(supabase, pengumumanId)
                    hapusGambarDariStorage(supabase, gambarUrl)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Pengumuman berhasil dihapus", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    Log.e(
                        "HapusPengumuman",
                        "Terjadi error saat menghapus pengumuman: ${e.message}"
                    )
                } finally {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                    }
                }
            }
        }
    }

    private suspend fun hapusDataPengumumanDariDatabase(supabase: SupabaseClient, id: Long) {
        supabase.from("pengumuman").delete {
            filter { eq("pmnId", id) }
        }
    }

    private suspend fun hapusGambarDariStorage(supabase: SupabaseClient, url: String?) {
        if (url.isNullOrBlank()) return

        val fileName = url.substringAfterLast("/")
        val path = "images/$fileName"
        val bucket = supabase.storage.from("pengumuman")

        try {
            bucket.delete(path)
        } catch (e: Exception) {
            Log.e("HapusGambar", "Gagal hapus gambar: ${e.message}")
        }
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