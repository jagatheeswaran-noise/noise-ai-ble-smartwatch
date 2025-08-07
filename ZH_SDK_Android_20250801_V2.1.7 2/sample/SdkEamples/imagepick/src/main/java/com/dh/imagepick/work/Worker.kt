package com.dh.imagepick.work

import com.dh.imagepick.callback.CallBack

interface Worker<Builder, ResultData> {

    fun start(formerResult: Any?, callBack: CallBack<ResultData>)

}