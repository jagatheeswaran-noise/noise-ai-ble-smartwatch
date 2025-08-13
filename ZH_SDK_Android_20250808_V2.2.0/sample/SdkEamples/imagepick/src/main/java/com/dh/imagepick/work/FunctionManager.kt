package com.dh.imagepick.work

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import com.dh.imagepick.agent.AcceptActivityResultHandlerFactory
import com.dh.imagepick.agent.IContainer
import com.dh.imagepick.dispose.disposer.DefaultImageDisposer
import com.dh.imagepick.dispose.disposer.Disposer
import com.dh.imagepick.functions.CropBuilder
import com.dh.imagepick.functions.DisposeBuilder
import com.dh.imagepick.functions.PickBuilder
import com.dh.imagepick.functions.TakeBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FunctionManager(internal val container: IContainer) {

    internal val workerFlows = ArrayList<Any>()

    private var filePath : String ?= null
    /**
     * take photo from system camera
     * @see TakeBuilder
     */
    fun take(): TakeBuilder =
        TakeBuilder(this).fileToSave(createSDCardFile(container.provideActivity()?.applicationContext))

    private fun createSDCardFile(context: Context?): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = filePath?.let {  File("$filePath/$timeStamp") }
            ?: File(context?.cacheDir!!.path + "/" + timeStamp)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_demo", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    fun path(path : String) : FunctionManager{
        filePath = path
        return this
    }
    /**
     * select a photo from system gallery or file system
     * @see PickBuilder
     */
    fun pick(): PickBuilder =
        PickBuilder(this)

    /**
     * dispose an file in background thread ,and will bind the lifecycle with current container
     * @param disposer you can also custom disposer
     * @see Disposer
     */
    @JvmOverloads
    fun dispose(disposer: Disposer = DefaultImageDisposer.get()): DisposeBuilder =
        DisposeBuilder(this)
            .disposer(disposer)

    /**
     * crop image by system crop function
     * @param originFile the file to crop,
     * if use pick or take before crop ,the origin file will be set form former result automatic
     * @return com.qingniu.imagepick.functions.CropBuilder
     * @see CropBuilder
     */
    @JvmOverloads
    fun crop(originFile: File? = null): CropBuilder = CropBuilder(this)
        .file(originFile)

    companion object {
        internal fun create(activity: Activity) =
            FunctionManager(AcceptActivityResultHandlerFactory.create(activity))

        internal fun create(fragment: Fragment) =
            FunctionManager(AcceptActivityResultHandlerFactory.create(fragment))

        internal fun create(fragment: android.app.Fragment) =
            FunctionManager(AcceptActivityResultHandlerFactory.create(fragment))

    }

}