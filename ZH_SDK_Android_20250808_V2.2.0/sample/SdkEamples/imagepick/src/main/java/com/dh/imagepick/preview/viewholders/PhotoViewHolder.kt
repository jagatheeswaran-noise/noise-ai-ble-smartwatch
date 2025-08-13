package com.dh.imagepick.preview.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dh.imagepick.preview.ImageViewerAdapterListener
import com.dh.imagepick.preview.adapter.ItemType
import com.dh.imagepick.preview.core.Components.requireImageLoader
import com.dh.imagepick.preview.core.Components.requireVHCustomizer
import com.dh.imagepick.preview.core.Photo
import com.dh.imagepick.preview.widgets.PhotoView2
import com.dh.imagepick.R
import com.dh.imagepick.databinding.ItemImageviewerPhotoBinding

class PhotoViewHolder(
    parent: ViewGroup,
    callback: ImageViewerAdapterListener,
    val binding: ItemImageviewerPhotoBinding =
        ItemImageviewerPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.photoView.setListener(object : PhotoView2.Listener {
            override fun onDrag(view: PhotoView2, fraction: Float) = callback.onDrag(this@PhotoViewHolder, view, fraction)
            override fun onRestore(view: PhotoView2, fraction: Float) = callback.onRestore(this@PhotoViewHolder, view, fraction)
            override fun onRelease(view: PhotoView2) = callback.onRelease(this@PhotoViewHolder, view)
        })
        requireVHCustomizer().initialize(ItemType.PHOTO, this)
    }

    fun bind(item: Photo) {
        binding.photoView.setTag(R.id.viewer_adapter_item_key, item.id())
        binding.photoView.setTag(R.id.viewer_adapter_item_data, item)
        binding.photoView.setTag(R.id.viewer_adapter_item_holder, this)
        requireVHCustomizer().bind(ItemType.PHOTO, item, this)
        requireImageLoader().load(binding.photoView, item, this)
    }
}