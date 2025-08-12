package com.gemahripah.banksampah.ui.nasabah.profil

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.MainActivity
import com.gemahripah.banksampah.databinding.FragmentProfilBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import com.gemahripah.banksampah.ui.nasabah.NasabahViewModel
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfilFragment : Fragment(), Reloadable {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val nasabahViewModel: NasabahViewModel by activityViewModels()
    private val profilViewModel: ProfilViewModel by viewModels()

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

        observeViewModel()
        setupListeners()
        setupSwipeRefresh()

        // Tampilkan/ sembunyikan kartu internet, lalu muat data awal
        reloadData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }
    }

    override fun reloadData() {
        // Cek koneksi. Jika offline: tampilkan kartu & hentikan animasi swipe
        if (!updateInternetCard()) {
            binding.swipeRefresh.isRefreshing = false
            return
        }

        // Online: minta data terbaru
        val id = nasabahViewModel.pengguna.value?.pgnId
        if (id != null) {
            nasabahViewModel.loadPenggunaById(id)
        } else {
            // Tidak ada id -> hentikan swipe supaya tidak muter terus
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        nasabahViewModel.pengguna.observe(viewLifecycleOwner) { pengguna ->
            pengguna?.let {
                binding.nama.text = it.pgnNama ?: "Nama tidak tersedia"
                binding.email.text = it.pgnEmail ?: "Email tidak tersedia"
            }
            // Hentikan animasi swipe ketika ada respons (sukses/empty)
            binding.swipeRefresh.isRefreshing = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Loading state logout
                launch {
                    profilViewModel.isLoading.collectLatest { loading ->
                        binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
                        binding.layoutKonten.alpha = if (loading) 0.3f else 1f
                        binding.keluar.isEnabled = !loading
                    }
                }
                // Event sukses logout
                launch {
                    profilViewModel.logoutSuccess.collectLatest {
                        Toast.makeText(requireContext(), "Berhasil Keluar", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                }
                // Event error logout
                launch {
                    profilViewModel.logoutError.collectLatest { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btProfil.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_notifications_to_editProfilFragment)
        }

        binding.keluar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Keluar") { _, _ -> profilViewModel.logout() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? NasabahActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    override fun onResume() {
        super.onResume()
        reloadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}