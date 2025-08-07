package com.dh.imagepick.callback

import com.dh.imagepick.pojo.CropResult

interface CropCallBack : BaseFunctionCallBack {

    /**
     * do when get the crop result
     */
    fun onFinish(result: CropResult)

    /**
     * do when crop canceled
     */
    fun onCancel()
}