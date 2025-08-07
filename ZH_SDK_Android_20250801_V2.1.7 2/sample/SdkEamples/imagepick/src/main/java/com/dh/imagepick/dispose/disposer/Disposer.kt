package com.dh.imagepick.dispose.disposer

import com.dh.imagepick.pojo.DisposeResult
import java.io.File

interface Disposer {

    /**
     * how to dispose your file, it will work on backGround thread
     *
     * @param originPath the origin file path to dispose
     * @param targetToSaveResult the file that you can write your result
     */
    fun disposeFile(originPath: String, targetToSaveResult: File?): DisposeResult

}