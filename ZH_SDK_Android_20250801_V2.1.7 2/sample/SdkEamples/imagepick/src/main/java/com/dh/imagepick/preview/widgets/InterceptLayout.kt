package com.dh.imagepick.preview.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.dh.imagepick.preview.utils.TransitionEndHelper
import com.dh.imagepick.preview.utils.TransitionStartHelper

class InterceptLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    override fun onInterceptTouchEvent(ev: MotionEvent?) = TransitionStartHelper.transitionAnimating || TransitionEndHelper.transitionAnimating
}