package com.dh.imagepick.work

import android.app.Activity
import android.content.Intent
import com.dh.imagepick.Configs
import com.dh.imagepick.Utils
import com.dh.imagepick.agent.IContainer
import com.dh.imagepick.callback.CallBack
import com.dh.imagepick.constant.Constant
import com.dh.imagepick.crop.ImageCropActivity
import com.dh.imagepick.crop.ImagePicker
import com.dh.imagepick.crop.bean.ImageItem
import com.dh.imagepick.exception.BadConvertException
import com.dh.imagepick.exception.BaseException
import com.dh.imagepick.exception.NoFileProvidedException
import com.dh.imagepick.functions.CropBuilder
import com.dh.imagepick.pojo.CropResult
import com.dh.imagepick.pojo.DisposeResult
import com.dh.imagepick.pojo.PickResult
import com.dh.imagepick.pojo.TakeResult
import java.io.File


class CropWorker(handler: IContainer, builder: CropBuilder) :
    BaseWorker<CropBuilder, CropResult>(handler, builder) {

    override fun start(formerResult: Any?, callBack: CallBack<CropResult>) {
        addConfigFromConfig()
        try {
            convertFormerResultToCurrent(formerResult)
        } catch (e: Exception) {
            callBack.onFailed(e)
            return
        }
        if (mParams.cropCallBack != null) {
            mParams.cropCallBack!!.onStart()
        }
        if (mParams.originFile == null) {
            callBack.onFailed(BaseException("crop file is null"))
            return
        }
        val activity = iContainer.provideActivity()
        activity ?: return
        val imagePicker = ImagePicker.getInstance()
        imagePicker.isShowCamera = false //显示拍照按钮

        imagePicker.isMultiMode = false //是否多选模式

        imagePicker.isCrop = true //允许裁剪（单选才有效）

        imagePicker.selectLimit = 1 //选中数量限制

        imagePicker.style = mParams.style //裁剪框的形状

        imagePicker.focusWidth = mParams.cropWidth //裁剪框的宽度。单位像素（圆形自动取宽高最小值）

        imagePicker.focusHeight = mParams.cropHeight //裁剪框的高度。单位像素（圆形自动取宽高最小值）

        imagePicker.outPutX = mParams.cropWidth  //保存文件的宽度。单位像素

        imagePicker.outPutY = mParams.cropHeight //保存文件的高度。单位像素
        imagePicker.clearSelectedImages()

        val imageItem = ImageItem()
        imageItem.path = mParams.originFile!!.absolutePath

        imagePicker.addSelectedImageItem(0, imageItem, true)
        var intent = Intent(activity, ImageCropActivity::class.java)

        iContainer.startActivityResult(
            intent,
            Constant.REQUEST_CODE_CORP_IMAGE
        ) { _: Int, resultCode: Int, intent: Intent? ->
            this.handleResult(resultCode, intent, callBack)
        }
    }

    private fun addConfigFromConfig() {
        if (null != Configs.cropsResultFile) {
            mParams.savedResultFile = File(Configs.cropsResultFile)
        }
    }

    private fun handleResult(
        resultCode: Int,
        data: Intent?,
        callBack: CallBack<CropResult>
    ) {
        if (resultCode == Activity.RESULT_CANCELED) {
            if (null != mParams.cropCallBack) {
                mParams.cropCallBack!!.onCancel()
            }
            return
        }
        if (null == data?.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS)) return
        val arrayList =
            data?.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS) as ArrayList<ImageItem>
        if (arrayList.isEmpty()) return
        val imageItem = arrayList[0]

        val result = CropResult()
        result.originFile = mParams.originFile
        result.savedFile = File(imageItem.path)
        result.cropBitmap = Utils.getBitmapFromFile(imageItem.path)!!
        if (null != mParams.cropCallBack) {
            mParams.cropCallBack!!.onFinish(result)
        }
        callBack.onSuccess(result)
    }

    private fun convertFormerResultToCurrent(
        formerResult: Any?
    ) {
        if (null == formerResult) {
            return
        }
        if (formerResult is TakeResult) {
            mParams.originFile = formerResult.savedFile!!
        }
        if (formerResult is PickResult) {
            val localPath =
                Utils.uriToImagePath(iContainer.provideActivity()!!, formerResult.originUri)
            if (!localPath.isNullOrBlank()) {
                val f = File(localPath)
                if (f.exists()) {
                    mParams.originFile = f
                } else {
                    throw BadConvertException(formerResult)
                }
            }
            if (localPath.isNullOrBlank()) {
                throw BadConvertException(formerResult)
            }
        }
        if (formerResult is DisposeResult) {
            if (null == formerResult.savedFile) {
                throw NoFileProvidedException("DisposeBuilder.fileToSaveResult")
            }
            mParams.originFile = formerResult.savedFile
        }
    }

}