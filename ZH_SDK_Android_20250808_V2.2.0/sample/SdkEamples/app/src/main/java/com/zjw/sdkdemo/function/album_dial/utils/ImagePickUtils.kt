package com.zjw.sdkdemo.function.album_dial.utils

import android.app.Activity
import androidx.fragment.app.Fragment
import com.dh.imagepick.ImagePickClient
import com.dh.imagepick.callback.CallBack
import com.dh.imagepick.crop.CropImageView
import com.dh.imagepick.pojo.CropResult
import com.dh.imagepick.pojo.PickResult

/**
 * 图片选择工具
 */
object ImagePickUtils {

    /**
     * 拍照选择
     */
    fun takePhotoCrop(context: Activity,backBlock: (params : String) -> Unit,errorBack: (params : String) -> Unit){
        ImagePickClient.with(context).take().then().crop()
            .start(object : CallBack<CropResult> {
                override fun onSuccess(data: CropResult) {
                    data.savedFile?.let { file ->
                        backBlock(file.absolutePath)
                    } ?: errorBack("file path is null")
                }

                override fun onFailed(exception: Exception) {
                    errorBack(exception.message ?: "pickImage unknown error")
                }
            })
    }

    /**
     * 拍照选择
     */
    fun takePhotoCrop(context: Fragment,path : String,backBlock: (params : String) -> Unit,errorBack: (params : String) -> Unit){
        ImagePickClient.with(context).path(path).take().then().crop()
            .start(object : CallBack<CropResult> {
                override fun onSuccess(data: CropResult) {
                    data.savedFile?.let { file ->
                        backBlock(file.absolutePath)
                    } ?: errorBack("file path is null")
                }

                override fun onFailed(exception: Exception) {
                    errorBack(exception.message ?: "pickImage unknown error")
                }
            })
    }

    /**
     * 系统相册选择,带图片裁剪
     */
    fun pickImageCrop(context: Activity,backBlock: (params : String) -> Unit,errorBack: (params : String) -> Unit){
        ImagePickClient.with(context).pick().then().crop()
            .start(object : CallBack<CropResult> {
                override fun onSuccess(data: CropResult) {
                    data.savedFile?.let { file ->
                        backBlock(file.absolutePath)
                    } ?: errorBack("file path is null")
                }

                override fun onFailed(exception: Exception) {
                    errorBack(exception.message ?: "pickImage unknown error")
                }
            })
    }

    /**
     * 系统相册选择
     */
    fun pickImage(context: Activity, backBlock: (params : String) -> Unit, errorBack: (params : String) -> Unit) {
        ImagePickClient.with(context).pick().start(object : CallBack<PickResult> {
            override fun onSuccess(data: PickResult) {
                data.localPath?.let {
                    backBlock(it)
                } ?: errorBack("file path is null")
            }

            override fun onFailed(exception: Exception) {
                errorBack(exception.message ?: "pickImage unknown error")
            }
        })
    }

    /**
     * 系统相册选择,带图片裁剪
     */
    fun pickImageCrop(context: Activity, width: Int, height: Int, dialType: Int,
                      backBlock: (params : String) -> Unit, errorBack: (params : String) -> Unit) {
        val style = if (dialType == 1) {
            CropImageView.Style.CIRCLE
        } else {
            CropImageView.Style.RECTANGLE
        }
        ImagePickClient.with(context).pick().then().crop().cropSize(
            width, height, style).start(object : CallBack<CropResult> {
            override fun onSuccess(data: CropResult) {
                data.savedFile?.let { file ->
                    backBlock(file.absolutePath)
                } ?: errorBack("file path is null")
            }

            override fun onFailed(exception: Exception) {
                errorBack(exception.message ?: "pickImage unknown error")
            }
        })
    }
}