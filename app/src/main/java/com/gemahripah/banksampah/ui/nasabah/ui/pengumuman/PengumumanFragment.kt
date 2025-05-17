package com.gemahripah.banksampah.ui.nasabah.ui.pengumuman

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPengumumanBinding
import com.gemahripah.banksampah.ui.admin.pengumuman.PengumumanFragmentDirections
import com.gemahripah.banksampah.ui.admin.pengumuman.adapter.PengumumanAdapter
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengumumanFragment : Fragment() {

    private var _binding: FragmentPengumumanBinding? = null
    private val binding get() = _binding!!

    val client = SupabaseProvider.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadPengumuman()
    }

    private fun loadPengumuman() {
        lifecycleScope.launch {
            val pengumumanList = withContext(Dispatchers.IO) {
                try {
                    SupabaseProvider.client
                        .from("pengumuman")
                        .select()
                        .decodeList<Pengumuman>()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }

            // Set adapter ke RecyclerView
            binding.rvPengumuman.layoutManager = LinearLayoutManager(requireContext())
            binding.rvPengumuman.adapter = PengumumanAdapter(pengumumanList) { pengumuman ->
                val action = com.gemahripah.banksampah.ui.nasabah.ui.pengumuman.PengumumanFragmentDirections
                    .actionNavigationDashboardToDetailPengumumanFragment2(pengumuman)
                findNavController().navigate(action)
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}