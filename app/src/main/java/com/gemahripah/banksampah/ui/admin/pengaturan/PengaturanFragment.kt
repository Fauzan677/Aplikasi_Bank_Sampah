package com.gemahripah.banksampah.ui.admin.pengaturan

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
import com.gemahripah.banksampah.ui.MainActivity
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentProfilBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class PengaturanFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            try {
                val supabase = SupabaseProvider.client
                val user = supabase.auth.retrieveUserForCurrentSession(updateSession = true)
                val userId = user?.id ?: return@launch

                val pengguna = supabase.from("pengguna")
                    .select {
                        filter { eq("pgnId", userId) }
                    }
                    .decodeSingle<Pengguna>()

                binding.nama.text = pengguna.pgnNama
                binding.email.text = pengguna.pgnEmail

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.btProfil.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val supabase = SupabaseProvider.client
                    val user = supabase.auth.retrieveUserForCurrentSession(updateSession = true)
                    val userId = user?.id ?: return@launch

                    val pengguna = supabase.from("pengguna")
                        .select {
                            filter { eq("pgnId", userId) }
                        }
                        .decodeSingle<Pengguna>()

                    val action = PengaturanFragmentDirections
                        .actionNavigationPengaturanToEditProfilFragment(pengguna)

                    findNavController().navigate(action)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.btCetak.setOnClickListener{
            findNavController().navigate(R.id.action_navigation_pengaturan_to_laporanFragment)
        }

        binding.jenis.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_pengaturan_to_jenisSampahFragment)
        }

        binding.nasabah.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_pengaturan_to_penggunaFragment)
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