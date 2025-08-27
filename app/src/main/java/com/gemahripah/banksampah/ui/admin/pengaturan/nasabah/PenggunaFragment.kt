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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.FragmentPenggunaAdminBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.gabungan.adapter.listNasabah.NasabahPagingAdapter
import com.gemahripah.banksampah.ui.gabungan.adapter.common.LoadingStateAdapter
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PenggunaFragment : Fragment(), Reloadable {

    private var _binding: FragmentPenggunaAdminBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PenggunaViewModel by viewModels()
    private lateinit var adapter: NasabahPagingAdapter

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

        observeViewModel()
        setupSearch()
        setupBackPressHandling()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tambah.setOnClickListener {
            findNavController().navigate(R.id.action_penggunaFragment_to_tambahPenggunaFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            reloadData()
        }

        binding.koneksiNasabah.setOnClickListener {
            binding.koneksiNasabah.visibility = View.GONE
            binding.loading.visibility = View.VISIBLE

            lifecycleScope.launch {
                delay(1000L)
                adapter.retry()
            }
        }

        if (!updateInternetCard()) return
        viewModel.ambilData()
    }

    override fun reloadData() {
        if (!updateInternetCard()) return

        binding.rvListNasabah.post { binding.rvListNasabah.scrollToPosition(0) }
        binding.appbar.setExpanded(true, true)

        binding.searchNasabah.setText("")
        binding.searchNasabah.clearFocus()

        adapter.refresh()
        viewModel.ambilData()

        binding.swipeRefresh.isRefreshing = false
    }

    private fun initRecyclerView() {
        adapter = NasabahPagingAdapter { pengguna ->
            val action = PenggunaFragmentDirections.actionPenggunaFragmentToEditPenggunaFragment(pengguna)
            findNavController().navigate(action)
        }

        binding.rvListNasabah.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListNasabah.adapter = adapter.withLoadStateHeaderAndFooter(
            header = LoadingStateAdapter { adapter.retry() },
            footer = LoadingStateAdapter { adapter.retry() }
        )

        adapter.addLoadStateListener { loadState ->
            val isLoading = loadState.source.refresh is LoadState.Loading
            val isError = loadState.source.refresh is LoadState.Error
            val isEmpty = loadState.source.refresh is LoadState.NotLoading && adapter.itemCount == 0

            binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvListNasabah.visibility = if (!isLoading && !isError) View.VISIBLE else View.GONE
            binding.koneksiNasabah.visibility = if (isError) View.VISIBLE else View.GONE
            binding.nasabahKosong.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearch() {

        // Saat TextInputLayout disentuh -> collapse header supaya searchBar menempel di atas
        binding.searchBar.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) {
                binding.appbar.setExpanded(false, true)   // collapse dengan animasi
            }
            false
        }

        // Saat EditText fokus -> juga collapse
        binding.searchNasabah.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.appbar.setExpanded(false, true)
            }
        }

        // (opsional) saat klik area barisan list_nasabah, expand lagi
        binding.rvListNasabah.setOnTouchListener { _, _ ->
            if (binding.searchNasabah.hasFocus()) {
                binding.searchNasabah.clearFocus()
            }
            false
        }

        // existing behavior Anda
        binding.searchNasabah.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text.toString())
        }
        binding.searchNasabah.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v)
                binding.searchNasabah.clearFocus()
                true
            } else false
        }

        binding.searchBar.setEndIconOnClickListener {
            // Karena kita override listener-nya, kita clear sendiri teksnya:
            binding.searchNasabah.setText("")

            // Tutup keyboard & hilangkan fokus
            hideKeyboard(binding.searchNasabah)
            binding.searchNasabah.clearFocus()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pager.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
                launch {
                    viewModel.totalNasabah.collect { total ->
                        binding.jumlah.text = total.toString()
                    }
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.searchNasabah.hasFocus()) {
                binding.searchNasabah.clearFocus()
                binding.appbar.setExpanded(true, true)
                binding.rvListNasabah.scrollToPosition(0)
                hideKeyboard(binding.searchNasabah)
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