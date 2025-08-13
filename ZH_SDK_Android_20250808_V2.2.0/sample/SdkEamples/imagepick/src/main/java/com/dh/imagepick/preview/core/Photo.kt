package com.dh.imagepick.preview.core

import com.dh.imagepick.preview.adapter.ItemType

interface Photo {
    fun id(): Long
    fun itemType(): @ItemType.Type Int
}
