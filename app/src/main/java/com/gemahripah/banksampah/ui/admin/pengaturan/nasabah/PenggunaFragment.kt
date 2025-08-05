package com.gemahripah.banksampah.ui.admin.pengaturan.nasabah

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentPenggunaAdminBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.admin.beranda.adapter.NasabahAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.launch

class PenggunaFragment : Fragment(), Reloadable {

    private var _binding: FragmentPenggunaAdminBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PenggunaViewModel by viewModels()
    private lateinit var nasabahAdapter: NasabahAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPenggunaAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initListeners()
        observeViewModel()
        setupBackPressHandling()

        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }

        if (!updateInternetCard()) return
        viewModel.ambilData()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return

        binding.scrollView.post {
            binding.scrollView.scrollTo(0, 0)
        }
        binding.searchNasabah.setText("")
        binding.searchNasabah.clearFocus()
        viewModel.ambilData()

        binding.swipeRefresh.isRefreshing = false
    }

    private fun initRecyclerView() {
        nasabahAdapter = NasabahAdapter(emptyList()) { pengguna ->
            val action = PenggunaFragmentDirections.actionPenggunaFragmentToEditPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }
        binding.rvListNasabah.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListNasabah.adapter = nasabahAdapter
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        binding.tambah.setOnClickListener {
            findNavController().navigate(R.id.action_penggunaFragment_to_tambahPenggunaFragment)
        }

        binding.searchNasabah.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.scrollView.post {
                    val y = binding.pengguna.top
                    binding.scrollView.scrollTo(0, y)
                }
            }
            false
        }

        binding.searchNasabah.doAfterTextChanged { text ->
            viewModel.cariPengguna(text.toString())
        }

        binding.searchNasabah.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v)
                binding.searchNasabah.clearFocus()
                true
            } else false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.nasabahList.collect { list ->
                    nasabahAdapter.updateData(list)
                }
            }

            launch {
                viewModel.isLoading.collect { isLoading ->
                    showLoading(isLoading)
                }
            }

            launch {
                viewModel.totalNasabah.collect { total ->
                    binding.jumlah.text = total.toString()
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.rvListNasabah.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.searchNasabah.hasFocus()) {
                binding.searchNasabah.clearFocus()
                binding.scrollView.scrollTo(0, 0)

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchNasabah.windowToken, 0)
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
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