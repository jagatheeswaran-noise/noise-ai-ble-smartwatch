package com.dh.imagepick.preview.viewer

import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.dh.imagepick.R
import com.dh.imagepick.preview.ImageViewerDialogFragment

/**
 * 自定义ImageViewerDialogFragment
 * 此类主要对于 window 进行个性化再定制
 */
class FullScreenImageViewerDialogFragment : ImageViewerDialogFragment() {
    override fun setWindow(win: Window) {
        super.setWindow(win)
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.statusBarColor = Color.BLACK
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.statusBarColor = context?.let { ContextCompat.getColor(it, R.color.colorPrimaryDark) }
                    ?: Color.TRANSPARENT
        }
    }
}