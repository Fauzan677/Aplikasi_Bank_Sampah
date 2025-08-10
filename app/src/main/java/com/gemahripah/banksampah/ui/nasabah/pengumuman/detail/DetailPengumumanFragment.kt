package com.gemahripah.banksampah.ui.nasabah.pengumuman.detail

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
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
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetailPengumumanFragment : Fragment(), Reloadable {

    private var _binding: FragmentDetailPengumumanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailPengumumanViewModel by viewModels()

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

        binding.edit.visibility = View.GONE
        binding.hapus.visibility = View.GONE

        showLoading(true)
        reloadData()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return

        val pengumuman: Pengumuman? =
            arguments?.let { DetailPengumumanFragmentArgs.fromBundle(it).pengumuman }

        pengumuman?.let { showDetail(it) }
    }

    private fun showDetail(p: Pengumuman) {
        // Teks dasar
        binding.judul.text = p.pmnJudul
        binding.isiPengumuman.text = p.pmnIsi

        // Tanggal update (tanpa state)
        val formatted = viewModel.formatUpdatedAt(p.updated_at)
        if (formatted.isNullOrBlank()) {
            binding.update.visibility = View.GONE
        } else {
            binding.update.text = formatted
            binding.update.visibility = View.VISIBLE
        }

        // Gambar (tanpa state)
        imageUrlWithUpdatedAt = viewModel.buildImageUrlWithVersion(p.pmnGambar, p.updated_at)
        val url = imageUrlWithUpdatedAt

        if (url.isNullOrBlank()) {
            binding.gambar.visibility = View.GONE
            binding.teksErrorGambar.visibility = View.GONE
            showLoading(false)
        } else {
            binding.gambar.visibility = View.VISIBLE
            binding.gambar.loadImageNoCache(url) {
                showLoading(false)
            }
            binding.gambar.setOnClickListener { showZoomDialog(url) }
        }
    }

    private fun showZoomDialog(imageUrl: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_zoom_image, null)
        val photoView =
            dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.photo_view)

        Glide.with(requireContext())
            .load(imageUrl)
            .into(photoView)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.setOnClickListener { dialog.dismiss() }
        dialog.show()
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

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        (activity as? NasabahActivity)?.showNoInternetCard(!isConnected)
        return isConnected
    }

    private fun showLoading(isLoading: Boolean) {
        _binding?.let { b ->
            b.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
            b.layoutKonten.alpha = if (isLoading) 0.3f else 1f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}