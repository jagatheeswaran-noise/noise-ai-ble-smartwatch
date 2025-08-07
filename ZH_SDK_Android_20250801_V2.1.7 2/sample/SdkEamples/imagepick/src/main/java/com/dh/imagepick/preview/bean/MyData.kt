package com.dh.imagepick.preview.bean

import com.dh.imagepick.preview.adapter.ItemType
import com.dh.imagepick.preview.core.Photo
import com.dh.imagepick.preview.utils.VideoUtils


data class MyData(val id: Long,
                  val url: String,
                  val subsampling: Boolean = false,
                  val desc: String = "[$id] Caption or other information for this picture [$id]") :
    Photo {
    override fun id(): Long = id
    override fun itemType(): Int {
        return when {
            VideoUtils.isVideoSource(url) -> ItemType.VIDEO
            subsampling -> ItemType.SUBSAMPLING
            else -> ItemType.PHOTO
        }
    }
}
