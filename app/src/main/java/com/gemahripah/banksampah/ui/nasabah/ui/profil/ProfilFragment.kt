package com.gemahripah.banksampah.ui.nasabah.ui.profil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.MainActivity
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentProfilBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NasabahViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nasabah.visibility = View.GONE
        binding.jenis.visibility = View.GONE
        binding.btCetak.visibility = View.GONE

        viewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            if (pengguna != null) {
                binding.nama.text = pengguna.pgnNama ?: "Nama tidak tersedia"
                binding.email.text = pengguna.pgnEmail ?: "Email tidak tersedia"
            }
        }

        binding.btProfil.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_notifications_to_editProfilFragment2)
        }

        binding.logout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseProvider.client.auth.signOut()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
                } catch (e: RestException) {
                    Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}