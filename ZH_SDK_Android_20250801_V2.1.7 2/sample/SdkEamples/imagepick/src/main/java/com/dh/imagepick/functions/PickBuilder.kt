package com.dh.imagepick.functions

import com.dh.imagepick.annotations.FileType
import com.dh.imagepick.annotations.PickRange
import com.dh.imagepick.callback.PickCallBack
import com.dh.imagepick.constant.Type
import com.dh.imagepick.constant.Range
import com.dh.imagepick.pojo.PickResult
import com.dh.imagepick.work.FunctionManager
import com.dh.imagepick.work.PickPhotoWorker
import com.dh.imagepick.work.Worker

class PickBuilder(fm: FunctionManager) :
    BaseFunctionBuilder<PickBuilder, PickResult>(fm) {

    internal var pickRange = Range.PICK_DICM

    internal var fileType = Type.ALL

    internal var pickCallBack: PickCallBack? = null

    fun callBack(callBack: PickCallBack): PickBuilder {
        this.pickCallBack = callBack
        return this
    }

    /**
     *
     * @param pickRange the range you can choose
     * @see Range.PICK_DICM  the system gallery
     * @see Range.PICK_CONTENT the system content file
     *
     * if you choose the  PICK_CONTENT, you can also use type to file the type you want
     */
    fun range(@PickRange pickRange: Int = Range.PICK_DICM): PickBuilder {
        this.pickRange = pickRange
        return this
    }

    /**
     * set the file type to be choose from file system when use in
     * @see Range.PICK_CONTENT
     * @param type the file type to be filter
     */
    fun type(@FileType type: Int = Type.ALL): PickBuilder {
        this.fileType = type
        return this
    }

    override fun getParamsBuilder(): PickBuilder {
        return this
    }

    override fun generateWorker(builder: PickBuilder): Worker<PickBuilder, PickResult> {
        return PickPhotoWorker(functionManager.container, builder)
    }

}