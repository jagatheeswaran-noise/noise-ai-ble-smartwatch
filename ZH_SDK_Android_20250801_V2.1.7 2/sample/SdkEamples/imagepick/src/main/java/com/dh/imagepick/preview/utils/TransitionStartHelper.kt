package com.dh.imagepick.preview.utils

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.dh.imagepick.preview.core.Components.requireImageLoader
import com.dh.imagepick.preview.core.Photo
import com.dh.imagepick.preview.viewholders.PhotoViewHolder
import com.dh.imagepick.preview.viewholders.SubsamplingViewHolder
import com.dh.imagepick.preview.viewholders.VideoViewHolder
import com.dh.imagepick.R

object TransitionStartHelper {
    val transitionAnimating get() = animating
    private var animating = false

    fun start(owner: LifecycleOwner, startView: View?, holder: RecyclerView.ViewHolder) {
        beforeTransition(startView, holder)
        val doTransition = {
            TransitionManager.beginDelayedTransition(holder.itemView as ViewGroup, transitionSet().also {
                it.addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionStart(transition: Transition) {
                        animating = true
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        if (!animating) return
                        animating = false
                        afterTransition(holder)
                    }
                })
            })
            transition(holder)
        }
        holder.itemView.postDelayed(doTransition, 50)

        owner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                owner.lifecycle.removeObserver(this)
                animating = false
                holder.itemView.removeCallbacks(doTransition)
                TransitionManager.endTransitions(holder.itemView as ViewGroup)
            }
        })
    }

    private fun beforeTransition(startView: View?, holder: RecyclerView.ViewHolder) {
        when (holder) {
            is PhotoViewHolder -> {
                holder.binding.photoView.scaleType = (startView as? ImageView?)?.scaleType
                        ?: ImageView.ScaleType.FIT_CENTER
                holder.binding.photoView.layoutParams = holder.binding.photoView.layoutParams.apply {
                    width = startView?.width ?: width
                    height = startView?.height ?: height
                    val location = IntArray(2)
                    getLocationOnScreen(startView, location)
                    if (this is ViewGroup.MarginLayoutParams) {
                        marginStart = location[0]
                        topMargin = location[1] - Config.TRANSITION_OFFSET_Y
                    }
                }
            }
            is SubsamplingViewHolder -> {
                holder.binding.subsamplingView.layoutParams = holder.binding.subsamplingView.layoutParams.apply {
                    width = startView?.width ?: width
                    height = startView?.height ?: height
                    val location = IntArray(2)
                    getLocationOnScreen(startView, location)
                    if (this is ViewGroup.MarginLayoutParams) {
                        marginStart = location[0]
                        topMargin = location[1] - Config.TRANSITION_OFFSET_Y
                    }
                }
            }
            is VideoViewHolder -> {
                holder.binding.imageView.layoutParams = holder.binding.imageView.layoutParams.apply {
                    width = startView?.width ?: width
                    height = startView?.height ?: height
                    val location = IntArray(2)
                    getLocationOnScreen(startView, location)
                    if (this is ViewGroup.MarginLayoutParams) {
                        marginStart = location[0]
                        topMargin = location[1] - Config.TRANSITION_OFFSET_Y
                    }
                }
            }
        }
    }

    private fun transition(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is PhotoViewHolder -> {
                holder.binding.photoView.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.binding.photoView.layoutParams = holder.binding.photoView.layoutParams.apply {
                    width = MATCH_PARENT
                    height = MATCH_PARENT
                    if (this is ViewGroup.MarginLayoutParams) {
                        marginStart = 0
                        topMargin = 0
                    }
                }
            }
            is SubsamplingViewHolder -> {
                holder.binding.subsamplingView.layoutParams = holder.binding.subsamplingView.layoutParams.apply {
                    width = MATCH_PARENT
                    height = MATCH_PARENT
                    if (this is ViewGroup.MarginLayoutParams) {
                        marginStart = 0
                        topMargin = 0
                    }
                }
            }
            is VideoViewHolder -> {
                holder.binding.imageView.layoutParams = holder.binding.imageView.layoutParams.apply {
                    width = MATCH_PARENT
                    height = MATCH_PARENT
                    if (this is ViewGroup.MarginLayoutParams) {
                        marginStart = 0
                        topMargin = 0
                    }
                }
            }
        }
    }

    private fun transitionSet(): Transition {
        return TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeImageTransform())
            // https://github.com/davemorrissey/subsampling-scale-image-view/issues/313
            duration = Config.DURATION_TRANSITION
            interpolator = DecelerateInterpolator()
        }
    }

    private fun getLocationOnScreen(startView: View?, location: IntArray) {
        startView?.getLocationOnScreen(location)

        if (location[0] == 0) {
            location[0] = (startView?.getTag(R.id.viewer_start_view_location_0) as? Int) ?: 0
        }
        if (location[1] == 0) {
            location[1] = (startView?.getTag(R.id.viewer_start_view_location_1) as? Int) ?: 0
        }

        if (startView?.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL) {
            location[0] = startView.context.resources.displayMetrics.widthPixels - location[0] - startView.width
        }
    }

    private fun afterTransition(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is PhotoViewHolder -> {
                val photo = holder.binding.photoView.getTag(R.id.viewer_adapter_item_data) as Photo
                requireImageLoader().load(holder.binding.photoView, photo, holder)
            }
        }
    }
}