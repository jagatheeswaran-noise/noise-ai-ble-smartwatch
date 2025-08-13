package com.dh.imagepick.preview.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

val appContext get() = App.context!!
fun toast(message: String?) {
    if (message.isNullOrEmpty()) return
    runOnUIThread { Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show() }
}

fun log(message: String?) {
    if (message.isNullOrEmpty()) return
    if (false) Log.i("viewer_demo", message)
}

fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
fun runOnUIThread(block: () -> Unit) {
    if (isMainThread()) block() else Handler(Looper.getMainLooper()).post(block)
}

fun statusBarHeight(): Int {
    var height = 0
    val resourceId = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        height = appContext.resources.getDimensionPixelSize(resourceId)
    }
    return height
}

object App {
    var context: Context? = null
}




