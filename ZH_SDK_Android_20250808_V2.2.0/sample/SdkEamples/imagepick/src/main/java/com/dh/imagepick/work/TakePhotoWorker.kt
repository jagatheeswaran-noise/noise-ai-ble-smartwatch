package com.dh.imagepick.work

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dh.imagepick.DevUtil
import com.dh.imagepick.Utils
import com.dh.imagepick.agent.IContainer
import com.dh.imagepick.callback.CallBack
import com.dh.imagepick.constant.Constant
import com.dh.imagepick.constant.Face
import com.dh.imagepick.functions.TakeBuilder
import com.dh.imagepick.pojo.TakeResult

class TakePhotoWorker(handler: IContainer, builder: TakeBuilder) :
    BaseWorker<TakeBuilder, TakeResult>(handler, builder) {

    var uri : Uri? =null

    override fun start(
        formerResult: Any?,
        callBack: CallBack<TakeResult>
    ) {
        val activity = iContainer.provideActivity()
        activity ?: return
        if (null != mParams.takeCallBack) {
            mParams.takeCallBack!!.onStart()
        }
        takePhoto(activity, callBack)
    }

    private fun takePhoto(activity: Activity, callBack: CallBack<TakeResult>) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        setCameraFace(takePictureIntent)
        //用户指定了目标文件路径
        if (null != mParams.fileToSave) {
            uri = Utils.createUriFromFile((activity) as Context, mParams.fileToSave!!)
            DevUtil.d(Constant.TAG, " uri:" + uri.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.grantUriPermission(
                    activity.packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                activity.revokeUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        try {
            iContainer.startActivityResult(
                takePictureIntent, Constant.REQUEST_CODE_IMAGE_CAPTURE
            ) { _: Int, resultCode: Int, _: Intent? ->
                handleResult(resultCode, callBack)
            }
        } catch (e: Exception) {
            callBack.onFailed(e)
        }
    }

    private fun setCameraFace(intent: Intent) {
        when (mParams.cameraFace) {
            Face.FRONT -> {
                intent.putExtra(
                    "android.intent.extras.CAMERA_FACING",
                    Camera.CameraInfo.CAMERA_FACING_FRONT
                )
            }
        }
    }

    private fun handleResult(
        resultCode: Int,
        callBack: CallBack<TakeResult>
    ) {
        if (resultCode == Activity.RESULT_CANCELED) {
            if (null != mParams.takeCallBack) {
                mParams.takeCallBack!!.onCancel()
            }
            return
        }
        if (null != mParams.fileToSave) {
            val result = TakeResult()
            result.savedFile = mParams.fileToSave!!
            if (null != mParams.takeCallBack) {
                mParams.takeCallBack!!.onFinish(result)
            }
            callBack.onSuccess(result)
        }
    }



}