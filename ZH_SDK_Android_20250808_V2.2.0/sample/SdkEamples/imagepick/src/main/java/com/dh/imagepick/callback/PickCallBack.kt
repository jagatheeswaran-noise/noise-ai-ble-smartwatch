package com.dh.imagepick.callback

import com.dh.imagepick.pojo.PickResult

interface PickCallBack : BaseFunctionCallBack {

    /**
     * do when get the pick result
     */
    fun onFinish(result: PickResult)

    /**
     * do when pick canceled
     */
    fun onCancel()
}