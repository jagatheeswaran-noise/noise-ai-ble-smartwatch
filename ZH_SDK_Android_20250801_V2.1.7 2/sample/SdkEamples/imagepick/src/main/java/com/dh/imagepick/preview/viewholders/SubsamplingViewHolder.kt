package com.dh.imagepick.preview.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dh.imagepick.preview.ImageViewerAdapterListener
import com.dh.imagepick.preview.adapter.ItemType
import com.dh.imagepick.preview.core.Components.requireImageLoader
import com.dh.imagepick.preview.core.Components.requireVHCustomizer
import com.dh.imagepick.preview.core.Photo
import com.dh.imagepick.preview.utils.Config
import com.dh.imagepick.preview.widgets.SubsamplingScaleImageView2
import com.dh.imagepick.R
import com.dh.imagepick.databinding.ItemImageviewerSubsamplingBinding

class SubsamplingViewHolder(
    parent: ViewGroup,
    callback: ImageViewerAdapterListener,
    val binding: ItemImageviewerSubsamplingBinding =
        ItemImageviewerSubsamplingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.subsamplingView.setMinimumScaleType(Config.SUBSAMPLING_SCALE_TYPE)
        binding.subsamplingView.setListener(object : SubsamplingScaleImageView2.Listener {
            override fun onDrag(view: SubsamplingScaleImageView2, fraction: Float) = callback.onDrag(this@SubsamplingViewHolder, view, fraction)
            override fun onRestore(view: SubsamplingScaleImageView2, fraction: Float) = callback.onRestore(this@SubsamplingViewHolder, view, fraction)
            override fun onRelease(view: SubsamplingScaleImageView2) = callback.onRelease(this@SubsamplingViewHolder, view)
        })
        requireVHCustomizer().initialize(ItemType.SUBSAMPLING, this)
    }

    fun bind(item: Photo) {
        binding.subsamplingView.setTag(R.id.viewer_adapter_item_key, item.id())
        binding.subsamplingView.setTag(R.id.viewer_adapter_item_data, item)
        binding.subsamplingView.setTag(R.id.viewer_adapter_item_holder, this)
        requireVHCustomizer().bind(ItemType.SUBSAMPLING, item, this)
        requireImageLoader().load(binding.subsamplingView, item, this)
    }
}


