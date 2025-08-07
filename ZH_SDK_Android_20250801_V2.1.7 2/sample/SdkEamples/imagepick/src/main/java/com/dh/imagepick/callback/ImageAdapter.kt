package com.dh.imagepick.callback

abstract class ImageAdapter<ResultData> : CallBack<ResultData> {

    override fun onFailed(exception: Exception) {}

    override fun onSuccess(data: ResultData) {}

}