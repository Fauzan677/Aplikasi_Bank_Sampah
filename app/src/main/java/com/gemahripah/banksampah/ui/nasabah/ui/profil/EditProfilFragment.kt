package com.gemahripah.banksampah.ui.nasabah.ui.profil

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemahripah.banksampah.R

class EditProfilFragment : Fragment() {

    companion object {
        fun newInstance() = EditProfilFragment()
    }

    private val viewModel: EditProfilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_edit_profil_nasabah, container, false)
    }
}