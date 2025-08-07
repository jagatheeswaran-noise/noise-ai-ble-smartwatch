package com.dh.imagepick.functions

import com.dh.imagepick.callback.CallBack
import com.dh.imagepick.dispose.WorkThread
import com.dh.imagepick.work.FunctionManager
import com.dh.imagepick.work.Worker

abstract class BaseFunctionBuilder<Builder, Result>(
    internal val functionManager: FunctionManager
) {

    /**
     * make you can convert to other operate to combine other functions
     * take().then().dispose()....
     */
    fun then(): FunctionManager {
        this.functionManager.workerFlows.add(generateWorker(getParamsBuilder()))
        return this.functionManager
    }

    /**
     * call start to begin the workflow
     */
    fun start(callback: CallBack<Result>) {
        synchronized(functionManager) {
            this.functionManager.workerFlows.add(generateWorker(getParamsBuilder()))
            val iterator = functionManager.workerFlows.iterator()
            if (!iterator.hasNext()) {
                return
            }
            realApply(null, iterator, callback)
        }
    }

    private fun realApply(
        formerResult: Any?,
        iterator: MutableIterator<Any>,
        callback: CallBack<Result>
    ) {
        val worker: Worker<Builder, Result> = iterator.next() as Worker<Builder, Result>
        worker.start(formerResult, object : CallBack<Result> {

            override fun onSuccess(data: Result) {
                if (iterator.hasNext()) {
                    iterator.remove()
                    realApply(data, iterator, callback)
                } else {
                    callback.onSuccess(data)
                    //final release
                    WorkThread.release()
                }
            }

            override fun onFailed(exception: Exception) {
                callback.onFailed(exception)
                //final release
                WorkThread.release()
            }
        })
    }

    internal abstract fun getParamsBuilder(): Builder

    internal abstract fun generateWorker(builder: Builder): Worker<Builder, Result>

}