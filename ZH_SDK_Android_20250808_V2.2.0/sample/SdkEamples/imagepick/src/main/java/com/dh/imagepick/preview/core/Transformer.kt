package com.dh.imagepick.preview.core

import android.widget.ImageView

interface Transformer {
    fun getView(key: Long): ImageView? = null
}

