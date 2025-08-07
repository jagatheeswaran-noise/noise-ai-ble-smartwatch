package com.zjw.sdkdemo.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Switch
import kotlin.math.abs

@SuppressLint("UseSwitchCompatOrMaterialCode")
class CustomMaterialSwitch @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : Switch(context, attrs) {

    private var touchX = 0f
    private var touchY = 0f

    private var isCallOnClick = false

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x
                val y = ev.y
                touchX = x
                touchY = y
                isCallOnClick = false
            }

            MotionEvent.ACTION_MOVE -> {
                val x = ev.x
                val y = ev.y
                if (abs(x - touchX) > 50 ||
                    abs(y - touchY) > 50
                ) {
                    isCallOnClick = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isCallOnClick && super.onTouchEvent(ev)) {
                    callOnClick()
                }
            }
        }
        return super.onTouchEvent(ev)
    }
}