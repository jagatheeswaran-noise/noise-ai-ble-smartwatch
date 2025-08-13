package com.dh.imagepick.agent

import android.app.Activity
import android.content.Intent
import com.dh.imagepick.constant.Host

interface IContainer {

    fun provideActivity(): Activity?

    fun startActivityResult(intent: Intent, requestCode: Int,
                            callback: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit
    )

    fun getLifecycleHost(): Host

}