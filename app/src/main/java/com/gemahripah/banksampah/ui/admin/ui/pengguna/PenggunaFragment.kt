package com.gemahripah.banksampah.ui.admin.ui.pengguna

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gemahripah.banksampah.databinding.FragmentPenggunaAdminBinding

class PenggunaFragment : Fragment() {

    private var _binding: FragmentPenggunaAdminBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val penggunaViewModel =
            ViewModelProvider(this).get(PenggunaViewModel::class.java)

        _binding = FragmentPenggunaAdminBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}