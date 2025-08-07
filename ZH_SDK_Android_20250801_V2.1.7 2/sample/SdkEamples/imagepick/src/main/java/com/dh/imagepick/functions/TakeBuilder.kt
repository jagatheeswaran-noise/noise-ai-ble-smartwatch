package com.dh.imagepick.functions

import com.dh.imagepick.DevUtil
import com.dh.imagepick.annotations.CameraFace
import com.dh.imagepick.callback.TakeCallBack
import com.dh.imagepick.constant.Constant
import com.dh.imagepick.constant.Face
import com.dh.imagepick.pojo.TakeResult
import com.dh.imagepick.work.FunctionManager
import com.dh.imagepick.work.TakePhotoWorker
import com.dh.imagepick.work.Worker
import java.io.File

class TakeBuilder(fm: FunctionManager) :
    BaseFunctionBuilder<TakeBuilder, TakeResult>(fm) {

    internal var cameraFace = Face.BACK

    internal var fileToSave: File? = null

    internal var takeCallBack: TakeCallBack? = null

    fun callBack(callBack: TakeCallBack): TakeBuilder {
        this.takeCallBack = callBack
        return this
    }

    /**
     * the result of capture result will be saved
     */
    fun fileToSave(fileToSave: File): TakeBuilder {
        DevUtil.d(
            Constant.TAG,
            "capture: saveFilePath: " + (fileToSave.path ?: "originUri is null")
        )
        this.fileToSave = fileToSave
        return this
    }

    /**
     * choose the face of camera
     * but this method may not covered for all phone
     * @see Face
     */
    fun cameraFace(@CameraFace face: Int = Face.BACK): TakeBuilder {
        this.cameraFace = face
        return this
    }

    override fun getParamsBuilder(): TakeBuilder {
        return this
    }

    override fun generateWorker(builder: TakeBuilder): Worker<TakeBuilder, TakeResult> {
        return TakePhotoWorker(functionManager.container, builder)
    }
}