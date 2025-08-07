package com.dh.imagepick.callback

import com.dh.imagepick.pojo.DisposeResult


internal interface CompressListener {

    /**
     * 开始压缩
     */
    fun onStart(path: String)

    /**
     * 任务结束
     */
    fun onFinish(disposeResult: DisposeResult)

    /**
     * 发生错误
     */
    fun onError(e: Exception)

}