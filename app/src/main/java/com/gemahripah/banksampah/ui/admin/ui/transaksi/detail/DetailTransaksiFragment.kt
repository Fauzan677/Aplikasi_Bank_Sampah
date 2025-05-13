package com.gemahripah.banksampah.ui.admin.ui.transaksi.detail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksiItem
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentDetailTransaksiBinding
import com.gemahripah.banksampah.ui.admin.ui.transaksi.adapter.DetailTransaksiAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailTransaksiFragment : Fragment() {

    private val args: DetailTransaksiFragmentArgs by navArgs()

    private var _binding: FragmentDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailTransaksiBinding.inflate(inflater, container, false)

        val riwayat = args.riwayat
        val idTransaksi = riwayat.tskId

        binding.nominal.text = riwayat.totalHarga.toString()


        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
