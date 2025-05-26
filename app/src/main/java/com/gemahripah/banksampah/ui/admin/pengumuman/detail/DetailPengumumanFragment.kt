package com.gemahripah.banksampah.ui.admin.pengumuman.detail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailPengumumanNasabahBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class DetailPengumumanFragment : Fragment() {

    private var _binding: FragmentDetailPengumumanNasabahBinding? = null
    private val binding get() = _binding!!

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

        binding.judul.text = pengumuman?.pmnJudul
        binding.isiPengumuman.text = pengumuman?.pmnIsi

        pengumuman?.pmnGambar?.let { url ->
            Glide.with(requireContext()).load(url).into(binding.gambar)
        }

        binding.gambar.setOnClickListener {
            pengumuman?.pmnGambar?.let { imageUrl ->
                showZoomDialog(imageUrl)
            }
        }

        binding.edit.setOnClickListener {
            pengumuman?.let {
                val action = DetailPengumumanFragmentDirections
                    .actionDetailPengumumanFragmentToEditPengumumanFragment(it)
                findNavController().navigate(action)
            }
        }

        binding.hapus.setOnClickListener {
            pengumuman?.let { pengumuman ->
                pengumuman.pmnId?.let { it1 -> hapusPengumuman(it1, pengumuman.pmnGambar) }
            }
        }


    }

    private fun showZoomDialog(imageUrl: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_zoom_image, null)
        val photoView = dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.photo_view)

        Glide.with(requireContext()).load(imageUrl).into(photoView)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.setOnClickListener { dialog.dismiss() }

        dialog.show()

    }

    private fun hapusPengumuman(pengumumanId: Long, gambarUrl: String?) {
        val supabase = SupabaseProvider.client

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                supabase.from("pengumuman").delete {
                    filter {
                        eq("pmnId", pengumumanId)
                    }
                }

                gambarUrl?.let { url ->
                    val fileName = url.substringAfterLast("/")
                    val path = "images/$fileName"
                    val bucket = supabase.storage.from("pengumuman")

                    try {
                        bucket.delete(path)
                    } catch (e: Exception) {
                        Log.e("HapusGambar", "Gagal hapus gambar: ${e.message}")
                    }
                }

                findNavController().popBackStack()

            } catch (e: Exception) {
                Log.e("HapusPengumuman", "Terjadi error saat menghapus pengumuman: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
