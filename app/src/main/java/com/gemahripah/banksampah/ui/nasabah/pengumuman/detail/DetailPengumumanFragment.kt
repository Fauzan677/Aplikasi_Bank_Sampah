package com.gemahripah.banksampah.ui.nasabah.pengumuman.detail

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.databinding.FragmentDetailPengumumanBinding
import com.gemahripah.banksampah.ui.admin.pengumuman.detail.DetailPengumumanFragmentArgs
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetailPengumumanFragment : Fragment() {

    private var _binding: FragmentDetailPengumumanBinding? = null
    private val binding get() = _binding!!

    private var imageUrlWithUpdatedAt: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoading(true)

        val pengumuman = arguments?.let {
            DetailPengumumanFragmentArgs.fromBundle(it).pengumuman
        }

        binding.edit.visibility = View.GONE
        binding.hapus.visibility = View.GONE

        pengumuman?.let {
            tampilkanDetailPengumuman(it)
            setupImageClick(it)
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
            imageUrlWithUpdatedAt = "${pengumuman.pmnGambar}?v=${pengumuman.updated_at}"
            binding.gambar.visibility = View.VISIBLE
            binding.gambar.loadImageNoCache(imageUrlWithUpdatedAt!!) {
                showLoading(false)
            }
        } else {
            binding.gambar.visibility = View.GONE
            showLoading(false)
        }
    }

    private fun setupImageClick(pengumuman: Pengumuman) {
        binding.gambar.setOnClickListener {
            pengumuman.pmnGambar?.let {
                tampilkanDialogZoomGambar()
            }
        }
    }

    private fun tampilkanDialogZoomGambar() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout
            .dialog_zoom_image, null)
        val photoView = dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id
            .photo_view)

        Glide.with(requireContext())
            .load(imageUrlWithUpdatedAt)
            .into(photoView)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.setOnClickListener { dialog.dismiss() }
        dialog.show()
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

    private fun ImageView.loadImageNoCache(url: String, onImageFinished: () -> Unit) {
        val teksError = binding.teksErrorGambar
        val progress = binding.loading

        teksError.visibility = View.GONE

        Glide.with(this.context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    progress.visibility = View.GONE
                    teksError.visibility = View.VISIBLE
                    onImageFinished()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    progress.visibility = View.GONE
                    teksError.visibility = View.GONE
                    onImageFinished()
                    return false
                }
            })
            .into(this)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutKonten.alpha = if (isLoading) 0.3f else 1f
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}