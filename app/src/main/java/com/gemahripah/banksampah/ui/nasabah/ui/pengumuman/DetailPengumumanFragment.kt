package com.gemahripah.banksampah.ui.nasabah.ui.pengumuman

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemahripah.banksampah.R

class DetailPengumumanFragment : Fragment() {

    companion object {
        fun newInstance() = DetailPengumumanFragment()
    }

    private val viewModel: DetailPengumumanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_detail_pengumuman_nasabah, container, false)
    }
}