package com.gemahripah.banksampah.ui.admin.transaksi

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemahripah.banksampah.R

class TotalSampahFragment : Fragment() {

    companion object {
        fun newInstance() = TotalSampahFragment()
    }

    private val viewModel: TotalSampahViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_total_sampah, container, false)
    }
}