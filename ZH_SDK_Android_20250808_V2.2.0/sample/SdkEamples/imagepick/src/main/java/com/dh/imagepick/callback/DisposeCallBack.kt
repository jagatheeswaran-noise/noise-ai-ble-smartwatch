package com.dh.imagepick.callback

import com.dh.imagepick.pojo.DisposeResult

interface DisposeCallBack : BaseFunctionCallBack {
    fun onFinish(result: DisposeResult)
}