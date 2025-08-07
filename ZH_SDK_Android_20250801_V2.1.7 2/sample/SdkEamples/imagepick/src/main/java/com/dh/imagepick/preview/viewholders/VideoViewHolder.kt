package com.dh.imagepick.preview.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dh.imagepick.preview.ImageViewerAdapterListener
import com.dh.imagepick.preview.adapter.ItemType
import com.dh.imagepick.preview.core.Components.requireImageLoader
import com.dh.imagepick.preview.core.Components.requireVHCustomizer
import com.dh.imagepick.preview.core.Photo
import com.dh.imagepick.preview.widgets.video.ExoVideoView2
import com.dh.imagepick.R
import com.dh.imagepick.databinding.ItemImageviewerVideoBinding

class VideoViewHolder(
    parent: ViewGroup,
    callback: ImageViewerAdapterListener,
    val binding: ItemImageviewerVideoBinding =
        ItemImageviewerVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.videoView.addListener(object : ExoVideoView2.Listener {
            override fun onDrag(view: ExoVideoView2, fraction: Float) = callback.onDrag(this@VideoViewHolder, view, fraction)
            override fun onRestore(view: ExoVideoView2, fraction: Float) = callback.onRestore(this@VideoViewHolder, view, fraction)
            override fun onRelease(view: ExoVideoView2) = callback.onRelease(this@VideoViewHolder, view)
        })
        requireVHCustomizer().initialize(ItemType.VIDEO, this)
    }

    fun bind(item: Photo) {
        binding.videoView.setTag(R.id.viewer_adapter_item_key, item.id())
        binding.videoView.setTag(R.id.viewer_adapter_item_data, item)
        binding.videoView.setTag(R.id.viewer_adapter_item_holder, this)
        requireVHCustomizer().bind(ItemType.VIDEO, item, this)
        requireImageLoader().load(binding.videoView, item, this)
    }
}