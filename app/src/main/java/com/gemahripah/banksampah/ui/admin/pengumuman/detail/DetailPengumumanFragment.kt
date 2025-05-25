package com.gemahripah.banksampah.ui.admin.pengumuman.detail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentDetailPengumumanNasabahBinding

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
