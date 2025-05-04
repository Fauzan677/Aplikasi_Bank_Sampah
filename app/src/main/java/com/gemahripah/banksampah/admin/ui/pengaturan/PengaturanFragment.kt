package com.gemahripah.banksampah.admin.ui.pengaturan

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.MainActivity
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.datastore.SessionPreference
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentProfilBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class PengaturanFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!


    private val viewModel: PengaturanViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Lifecycle", "onViewCreated Dipanggil")

        Log.d("NavController", "NavController is null: ${findNavController() == null}")


        binding.jenis.setOnClickListener {
            Log.d("Navigasi", "Tombol Jenis Sampah Ditekan")
            findNavController().navigate(R.id.action_navigation_pengaturan_to_jenisSampahFragment)
        }

        binding.logout.setOnClickListener {
            lifecycleScope.launch {

                val supabase = SupabaseProvider.client
                // Logout dari Supabase
                supabase.auth.signOut()

                // Arahkan kembali ke halaman login (atau WelcomeActivity)
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}