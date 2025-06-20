package com.gemahripah.banksampah.ui.welcome

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.databinding.FragmentLandingBinding
import com.gemahripah.banksampah.R

class LandingFragment : Fragment() {

    private var _binding: FragmentLandingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.masuk.setOnClickListener {
            findNavController().navigate(
                R.id.action_landingFragment_to_masukFragment
            )
        }

        binding.daftar.setOnClickListener {
            findNavController().navigate(
                R.id.action_landingFragment_to_daftarFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}