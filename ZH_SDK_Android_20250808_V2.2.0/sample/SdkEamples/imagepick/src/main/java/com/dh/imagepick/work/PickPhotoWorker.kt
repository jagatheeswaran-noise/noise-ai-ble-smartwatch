package com.dh.imagepick.work

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.dh.imagepick.DevUtil
import com.dh.imagepick.Utils
import com.dh.imagepick.agent.IContainer
import com.dh.imagepick.callback.CallBack
import com.dh.imagepick.constant.Constant
import com.dh.imagepick.constant.Type
import com.dh.imagepick.exception.BaseException
import com.dh.imagepick.functions.PickBuilder
import com.dh.imagepick.pojo.PickResult


/**
 * Created by rocket on 2019/6/18.
 */
class PickPhotoWorker(iContainer: IContainer, builder: PickBuilder) :
    BaseWorker<PickBuilder, PickResult>(iContainer, builder) {

    override fun start(
        formerResult: Any?,
        callBack: CallBack<PickResult>
    ) {
        val activity = iContainer.provideActivity()
        activity ?: return
        if (null != mParams.pickCallBack) {
            mParams.pickCallBack!!.onStart()
        }
        pickPhoto(activity, callBack)
    }

    private fun pickPhoto(activity: Activity, callBack: CallBack<PickResult>) {
        val pickIntent = Intent(Intent.ACTION_GET_CONTENT, null).also {
                when (mParams.fileType) {
                    Type.ALL -> {
                        it.type = "image/*"
                    }
                    Type.GIF -> {
                        it.type = "image/gif"
                    }
                    Type.PNG -> {
                        it.type = "image/png"
                    }
                    Type.JPEG -> {
                        it.type = "image/jpeg"
                    }
                }
            it.addCategory("android.intent.category.OPENABLE")
            }
        try {
            iContainer.startActivityResult(
                pickIntent, Constant.REQUEST_CODE_IMAGE_PICK
            ) { _: Int, resultCode: Int, data: Intent? ->
                handleResult(resultCode, data, callBack)
            }
        } catch (e: Exception) {
            callBack.onFailed(e)
        }
    }

    private fun handleResult(
        resultCode: Int,
        intentData: Intent?,
        callBack: CallBack<PickResult>
    ) {
        if (resultCode == Activity.RESULT_CANCELED) {
            if (null != mParams.pickCallBack) {
                mParams.pickCallBack!!.onCancel()
            }
            return
        }
        if (null != intentData && null != intentData.data) {
            val result = PickResult()
            result.originUri = intentData.data!!
            var localPath: String? = null
            try {
                localPath = Utils.uriToImagePath(iContainer.provideActivity()!!, intentData.data!!)
            } catch (e: Exception) {
                DevUtil.e(Constant.TAG, e.toString())
            }
            if (!TextUtils.isEmpty(localPath)) {
                result.localPath = localPath!!
            }
            if (null != mParams.pickCallBack) {
                mParams.pickCallBack!!.onFinish(result)
            }
            callBack.onSuccess(result)
        } else {
            callBack.onFailed(BaseException("null result intentData"))
        }
    }


}