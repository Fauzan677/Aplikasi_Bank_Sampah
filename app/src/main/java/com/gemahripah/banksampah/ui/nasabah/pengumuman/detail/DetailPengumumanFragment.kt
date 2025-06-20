package com.gemahripah.banksampah.ui.nasabah.pengumuman.detail

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentDetailPengumumanNasabahBinding
import com.gemahripah.banksampah.ui.admin.pengumuman.detail.DetailPengumumanFragmentArgs

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

        binding.edit.visibility = View.GONE
        binding.hapus.visibility = View.GONE

        val pengumuman = arguments?.let {
            DetailPengumumanFragmentArgs.fromBundle(it).pengumuman
        }

        binding.judul.text = pengumuman?.pmnJudul
        binding.isiPengumuman.text = pengumuman?.pmnIsi

        pengumuman?.pmnGambar?.let { url ->
            Glide.with(requireContext()).load(url).into(binding.gambar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}