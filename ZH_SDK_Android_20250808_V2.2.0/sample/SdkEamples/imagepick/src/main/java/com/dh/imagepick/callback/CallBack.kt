package com.dh.imagepick.callback


interface CallBack<ResultData> {

    /**
     * get final result data
     * @param data ImagePickClient.with.xxx.then.xxx.start ->  result data
     */
    fun onSuccess(data: ResultData)

    /**
     * failed in any position
     * @param exception
     */
    fun onFailed(exception: Exception)

}