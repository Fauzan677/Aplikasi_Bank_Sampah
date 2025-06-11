package com.gemahripah.banksampah.ui.admin.pengumuman

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentPengumumanBinding
import com.gemahripah.banksampah.ui.admin.pengumuman.adapter.PengumumanAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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

        binding.tambah.setOnClickListener {
            findNavController().navigate(
                R.id.action_navigation_pengumuman_to_tambahPengumumanFragment
            )
        }

        loadPengumuman()
    }

    private fun loadPengumuman() {
        lifecycleScope.launch {
            val pengumumanList = withContext(Dispatchers.IO) {
                try {
                    SupabaseProvider.client
                        .from("pengumuman")
                        .select(columns = Columns.list("*")) {
                            order(column = "updated_at", order = Order.DESCENDING)
                        }
                        .decodeList<Pengumuman>()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }

            binding.rvPengumuman.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            binding.rvPengumuman.adapter = PengumumanAdapter(pengumumanList) { pengumuman ->
                val action = PengumumanFragmentDirections
                    .actionNavigationPengumumanToDetailPengumumanFragment(pengumuman)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}