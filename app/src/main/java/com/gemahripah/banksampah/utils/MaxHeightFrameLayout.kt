package com.gemahripah.banksampah.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.gemahripah.banksampah.R
import androidx.core.content.withStyledAttributes

class MaxHeightFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var maxHeightPx: Int = Int.MAX_VALUE

    init {
        context.withStyledAttributes(attrs, R.styleable.MaxHeightFrameLayout) {
            maxHeightPx = getDimensionPixelSize(
                R.styleable.MaxHeightFrameLayout_maxHeight,
                Int.MAX_VALUE
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentMode = MeasureSpec.getMode(heightMeasureSpec)
        val parentSize = MeasureSpec.getSize(heightMeasureSpec)

        // Tentukan batas tinggi yang dipakai untuk mengukur child
        val limit = when (parentMode) {
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> {
                // hormati batas dari parent juga
                kotlin.math.min(parentSize, maxHeightPx)
            }
            else -> maxHeightPx // UNSPECIFIED
        }

        val limitedHeightSpec = MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, limitedHeightSpec)
        // Tidak perlu setMeasuredDimension manual lagi; hasil super sudah <= limit
    }
}