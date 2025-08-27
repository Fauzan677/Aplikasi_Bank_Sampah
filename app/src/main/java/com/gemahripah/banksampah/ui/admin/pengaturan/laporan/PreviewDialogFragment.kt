package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gemahripah.banksampah.databinding.DialogPreviewBinding
import kotlinx.coroutines.launch

class PreviewDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogPreviewBinding? = null
    private val binding get() = _binding!!

    private val vm: LaporanViewModel by viewModels(ownerProducer = { requireParentFragment() })

    companion object {
        private const val KEY_TITLE = "title"
        fun newInstance(title: String) = PreviewDialogFragment().apply {
            arguments = bundleOf(KEY_TITLE to title) // HTML tidak dikirim via arguments!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = requireArguments().getString(KEY_TITLE).orEmpty()
        binding.title.text = title

        binding.web.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        binding.web.settings.apply {
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        // Ambil HTML dari ViewModel (aman untuk tabel besar)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.lastHtml.collect { html ->
                    if (!html.isNullOrBlank()) {
                        binding.web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                }
            }
        }

        binding.btnSave.setOnClickListener { vm.saveCurrentPreviewAsExcel() }
        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}