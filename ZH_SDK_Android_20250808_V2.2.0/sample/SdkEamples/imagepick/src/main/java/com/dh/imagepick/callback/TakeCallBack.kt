package com.dh.imagepick.callback

import com.dh.imagepick.pojo.TakeResult

interface TakeCallBack : BaseFunctionCallBack {

    /**
     * do when get the take result
     */
    fun onFinish(result: TakeResult)

    /**
     * do when take canceled
     */
    fun onCancel()

}