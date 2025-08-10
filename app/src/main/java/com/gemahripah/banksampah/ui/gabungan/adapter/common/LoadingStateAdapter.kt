package com.gemahripah.banksampah.ui.gabungan.adapter.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gemahripah.banksampah.databinding.ItemLoadingFooterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<LoadingStateAdapter.LoadingViewHolder>() {

    inner class LoadingViewHolder(
        private val binding: ItemLoadingFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.retryButton.setOnClickListener {
                binding.retryButton.visibility = View.GONE
                binding.errorText.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE

                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000L)
                    retry.invoke()
                }
            }
        }

        fun bind(loadState: LoadState) = with(binding) {
            when (loadState) {
                is LoadState.Loading -> {
                    loading.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    retryButton.visibility = View.GONE
                }
                is LoadState.Error -> {
                    loading.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                }
                else -> {
                    loading.visibility = View.GONE
                    errorText.visibility = View.GONE
                    retryButton.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingViewHolder {
        val binding = ItemLoadingFooterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LoadingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoadingViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }
}