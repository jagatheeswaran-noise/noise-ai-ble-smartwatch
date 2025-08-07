package com.dh.imagepick

import android.app.Activity
import android.app.Fragment
import com.dh.imagepick.exception.ActivityStatusException
import com.dh.imagepick.work.FunctionManager


object ImagePickClient {

    @JvmStatic
    fun with(activity: Activity): FunctionManager {
        checkStatusFirst(activity)
        return FunctionManager.create(activity)
    }

    @JvmStatic
    fun with(fragment: androidx.fragment.app.Fragment): FunctionManager {
        checkStatusFirst(fragment.activity)
        return FunctionManager.create(fragment)
    }

    @JvmStatic
    fun with(fragment: Fragment): FunctionManager {
        checkStatusFirst(fragment.activity)
        return FunctionManager.create(fragment)
    }

    private fun checkStatusFirst(activity: Activity?) {
        if (!Utils.isActivityAvailable(activity)) {
            throw ActivityStatusException()
        }
    }

}
