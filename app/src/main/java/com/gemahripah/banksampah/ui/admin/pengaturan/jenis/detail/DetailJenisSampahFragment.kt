package com.gemahripah.banksampah.ui.admin.pengaturan.jenis.detail

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemahripah.banksampah.R

class DetailJenisSampahFragment : Fragment() {

    companion object {
        fun newInstance() = DetailJenisSampahFragment()
    }

    private val viewModel: DetailJenisSampahViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_detail_jenis_sampah, container, false)
    }
}