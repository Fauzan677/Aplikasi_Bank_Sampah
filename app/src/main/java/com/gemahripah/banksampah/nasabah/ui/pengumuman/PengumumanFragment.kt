package com.gemahripah.banksampah.nasabah.ui.pengumuman

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gemahripah.banksampah.databinding.FragmentPengumumanNasabahBinding

class PengumumanFragment : Fragment() {

    private var _binding: FragmentPengumumanNasabahBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pengumumanViewModel =
            ViewModelProvider(this).get(PengumumanViewModel::class.java)

        _binding = FragmentPengumumanNasabahBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}