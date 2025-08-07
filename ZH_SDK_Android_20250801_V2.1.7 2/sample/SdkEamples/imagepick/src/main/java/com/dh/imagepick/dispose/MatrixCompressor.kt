package com.dh.imagepick.dispose

import android.graphics.Bitmap
import android.graphics.Matrix
import com.dh.imagepick.DevUtil
import com.dh.imagepick.Utils
import com.dh.imagepick.constant.Constant
import kotlin.jvm.Throws


/**
 * @author cd5160866
 */
class MatrixCompressor : ICompress {

    @Throws(Exception::class)
    override fun compress(path: String, degree: Int): Bitmap? {
        val finalDegree: Int = when {
            degree <= 0 -> 1
            degree > 100 -> 100
            else -> degree
        }
        val degreeF = finalDegree.toFloat() / 100f
        var bitmap = Utils.getBitmapFromFile(path)
        if (null == bitmap) {
            return bitmap
        }
        val height = bitmap.height
        val width = bitmap.width
        val matrix = Matrix()
        matrix.postScale(degreeF, degreeF)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        DevUtil.d(Constant.TAG, "MatrixCompressor bitmap=${bitmap}")
        return bitmap
    }

}