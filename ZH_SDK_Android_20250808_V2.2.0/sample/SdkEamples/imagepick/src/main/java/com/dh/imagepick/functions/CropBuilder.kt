package com.dh.imagepick.functions

import com.dh.imagepick.callback.CropCallBack
import com.dh.imagepick.crop.CropImageView
import com.dh.imagepick.pojo.CropResult
import com.dh.imagepick.work.CropWorker
import com.dh.imagepick.work.FunctionManager
import com.dh.imagepick.work.Worker
import java.io.File

class CropBuilder(fm: FunctionManager) :
    BaseFunctionBuilder<CropBuilder, CropResult>(fm) {

    var originFile: File? = null

    var savedResultFile: File? = null

    var cropWidth: Int = 600

    var cropHeight: Int = 600

    var style = CropImageView.Style.CIRCLE
    internal var cropCallBack: CropCallBack? = null

    /**
     * crop image callback
     * @param callBack CropCallBack
     * @return CropBuilder
     */
    fun callBack(callBack: CropCallBack): CropBuilder {
        this.cropCallBack = callBack
        return this
    }

    /**
     * init crop file
     * @param originFile File? the file you want crop
     * @return CropBuilder
     */
    fun file(originFile: File?): CropBuilder {
        this.originFile = originFile
        return this
    }

    /**
     * init crop size
     * @param cropWidth Int
     * @param cropHeight Int
     * @param cropWidth Int Int the crop width and height you want
     * @param cropHeight Int Int the crop width and height you want
     * @return CropBuilder
     */
    fun cropSize(cropWidth: Int, cropHeight: Int): CropBuilder {
        this.cropWidth = cropWidth
        this.cropHeight = cropHeight
        return this
    }

    fun cropSize(cropWidth: Int, cropHeight: Int,style: CropImageView.Style): CropBuilder {
        this.cropWidth = cropWidth
        this.cropHeight = cropHeight
        this.style = style
        return this
    }
    /**
     * if not set , will create an temple file to save the crop result
     * @param fileToSaveResult File
     * @return CropBuilder
     */
    fun fileToSaveResult(fileToSaveResult: File): CropBuilder {
        this.savedResultFile = fileToSaveResult
        return this
    }

    override fun getParamsBuilder(): CropBuilder {
        return this
    }

    override fun generateWorker(builder: CropBuilder): Worker<CropBuilder, CropResult> {
        return CropWorker(functionManager.container, builder)
    }

}