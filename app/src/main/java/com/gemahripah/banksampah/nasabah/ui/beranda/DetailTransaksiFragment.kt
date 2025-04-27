package com.gemahripah.banksampah.nasabah.ui.beranda

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemahripah.banksampah.R
import com.gemahripah.banksampahapp.nasabah.ui.home.DetailTransaksiViewModel

class DetailTransaksiFragment : Fragment() {

    companion object {
        fun newInstance() = DetailTransaksiFragment()
    }

    private val viewModel: DetailTransaksiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_detail_transaksi_nasabah, container, false)
    }
}