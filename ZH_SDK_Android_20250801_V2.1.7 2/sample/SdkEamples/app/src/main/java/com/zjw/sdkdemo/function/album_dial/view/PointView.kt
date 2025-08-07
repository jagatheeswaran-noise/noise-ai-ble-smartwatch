package com.zjw.sdkdemo.function.album_dial.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.zjw.sdkdemo.R

class PointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pointColor : Int ?= 0
    init {
        initView(context,attrs)
    }

    private val paint by lazy {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        pointColor?.let { paint.color = it }
        paint
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        attrs?.let {
            val obtainStyledAttributes = context.obtainStyledAttributes(it, R.styleable.PointView)
            pointColor = obtainStyledAttributes.getColor(
                R.styleable.PointView_color_point,
                context.resources.getColor(android.R.color.holo_red_light))
            obtainStyledAttributes.recycle()
        }
    }

    fun setPointColor(color: Int){
        pointColor = color
        paint.color = color
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.let {
            it.drawCircle(width/2f,height/2f,width/2f,paint)
        }
    }
}