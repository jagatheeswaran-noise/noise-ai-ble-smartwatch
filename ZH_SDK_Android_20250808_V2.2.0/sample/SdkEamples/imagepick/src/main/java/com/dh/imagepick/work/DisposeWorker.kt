package com.dh.imagepick.work

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import com.dh.imagepick.DevUtil
import com.dh.imagepick.Utils
import com.dh.imagepick.agent.IContainer
import com.dh.imagepick.callback.CallBack
import com.dh.imagepick.callback.CompressListener
import com.dh.imagepick.constant.Constant
import com.dh.imagepick.constant.Host
import com.dh.imagepick.dispose.WorkThread
import com.dh.imagepick.dispose.disposer.Disposer
import com.dh.imagepick.exception.BadConvertException
import com.dh.imagepick.exception.PickNoResultException
import com.dh.imagepick.functions.DisposeBuilder
import com.dh.imagepick.pojo.CropResult
import com.dh.imagepick.pojo.DisposeResult
import com.dh.imagepick.pojo.PickResult
import com.dh.imagepick.pojo.TakeResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.NullPointerException

class DisposeWorker(handler: IContainer, builder: DisposeBuilder) :
    BaseWorker<DisposeBuilder, DisposeResult>(handler, builder) {

    override fun start(
        formerResult: Any?,
        callBack: CallBack<DisposeResult>
    ) {
        try {
            //if former has result  convert to current params
            convertFormerResultToCurrent(formerResult)
            dispose(
                iContainer.getLifecycleHost(),
                mParams.originPath,
                mParams.targetFile,
                mParams.disposer,
                runCompressListener(callBack)
            )
        } catch (e: Exception) {
            if (e is BadConvertException) {
                generateLocalPathAndHandResultWhenConvertUriFailed(
                    iContainer.provideActivity()!!,
                    iContainer.getLifecycleHost(),
                    e.result,
                    callBack
                )
            } else {
                callBack.onFailed(e)
            }
        }
    }

    private fun convertFormerResultToCurrent(
        formerResult: Any?
    ) {
        if (null == formerResult) {
            return
        }

        if (formerResult is TakeResult) {
            mParams.originPath = formerResult.savedFile!!.absolutePath
            if (null == mParams.targetFile) {
                mParams.targetFile = formerResult.savedFile
            }
        }

        if (formerResult is CropResult) {
            mParams.originPath = formerResult.savedFile!!.absolutePath
            if (null == mParams.targetFile) {
                mParams.targetFile = formerResult.savedFile
            }
        }

        if (formerResult is PickResult) {
            var localPath: String? = null
            try {
                localPath =
                    Utils.uriToImagePath(iContainer.provideActivity()!!, formerResult.originUri)
            } catch (e: Exception) {
                DevUtil.e(Constant.TAG, e.toString())
            }
            if (!TextUtils.isEmpty(localPath)) {
                mParams.originPath = localPath
            } else {
                throw BadConvertException(formerResult)
            }
        }
    }

    private fun dispose(
        lifecycleHost: Host,
        originPath: String?,
        targetSaveFile: File?,
        disposer: Disposer,
        listener: CompressListener
    ) {
        if (TextUtils.isEmpty(originPath)) {
            listener.onError(NullPointerException("try to dispose image with an null path"))
            return
        }
        listener.onStart(originPath!!)
        WorkThread.addWork(Runnable {
            try {
                val result = disposer.disposeFile(originPath, targetSaveFile)
                if (!checkContainerStatus(lifecycleHost, "dispose the Image")) {
                    return@Runnable
                }
                //post result
                Handler(Looper.getMainLooper()).post {
                    DevUtil.d(Constant.TAG, "all dispose ok")
                    listener.onFinish(result)
                }
            } catch (e: Exception) {
                DevUtil.d(Constant.TAG, e.toString())
                if (!checkContainerStatus(lifecycleHost, "dispose Image")) {
                    return@Runnable
                }
                listener.onError(e)
            }
        })
    }

    /**
     * 当将uri 转化为 path失败时 将原图在应用路径内生成并处理
     */
    private fun generateLocalPathAndHandResultWhenConvertUriFailed(
        activity: Activity,
        host: Host,
        result: PickResult,
        callBack: CallBack<DisposeResult>

    ) {
        WorkThread.addWork(Runnable {
            try {
                //generate local path
                val bitmap: Bitmap? =
                    MediaStore.Images.Media.getBitmap(activity.contentResolver, result.originUri)
                if (null == bitmap) {
                    runOnUIThread(Runnable { callBack.onFailed(PickNoResultException()) })
                    return@Runnable
                }
                val file =
                    File(activity.externalCacheDir!!.path + "/" + System.currentTimeMillis() + ".jpg")
                if (!file.exists()) file.createNewFile()
                result.localPath = file.path
                val fos = FileOutputStream(file)
                fos.write(bitmap2Bytes(bitmap))
                fos.close()
                if (!checkContainerStatus(host, "generate local path")) {
                    return@Runnable
                }
                //dispose
                runOnUIThread(Runnable {
                    dispose(
                        host,
                        result.localPath,
                        mParams.targetFile,
                        mParams.disposer,
                        runCompressListener(callBack)
                    )
                })
            } catch (e: Exception) {
                runOnUIThread(Runnable { callBack.onFailed(e) })
            }
        })
    }

    private fun runCompressListener(
        callBack: CallBack<DisposeResult>
    ): CompressListener {
        return object : CompressListener {
            override fun onStart(path: String) {
                if (null != mParams.disposeCallBack) {
                    mParams.disposeCallBack!!.onStart()
                }
            }

            override fun onFinish(disposeResult: DisposeResult) {
                DevUtil.d(Constant.TAG, "onSuccess $disposeResult")
                if (null != mParams.disposeCallBack) {
                    mParams.disposeCallBack!!.onFinish(disposeResult)
                }
                callBack.onSuccess(disposeResult)
            }

            override fun onError(e: Exception) {
                if (Utils.isOnMainThread()) {
                    callBack.onFailed(e)
                    return
                }
                Handler(Looper.getMainLooper()).post {
                    callBack.onFailed(e)
                }
            }
        }
    }

    private fun checkContainerStatus(host: Host, currentStepDesc: String): Boolean {
        if (!Utils.isHostAvailable(host)) {
            DevUtil.d(Constant.TAG, "host is disabled after $currentStepDesc")
            return false
        }
        return true
    }

    private fun runOnUIThread(runnable: Runnable) {
        Handler(Looper.getMainLooper()).post(runnable)
    }

    private fun bitmap2Bytes(bm: Bitmap): ByteArray? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }

}