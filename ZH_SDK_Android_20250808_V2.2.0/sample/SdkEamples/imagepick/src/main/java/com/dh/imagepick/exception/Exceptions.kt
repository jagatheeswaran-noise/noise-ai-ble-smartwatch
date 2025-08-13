package com.dh.imagepick.exception

import com.dh.imagepick.DevUtil
import com.dh.imagepick.constant.Constant
import com.dh.imagepick.pojo.PickResult

/**
 * @author cd5160866
 */
open class BaseException(override val message: String) : IllegalStateException() {
    init {
        DevUtil.d(Constant.TAG, message)
    }
}

class ImagePickException(override val message: String) : BaseException(message)

class CompressFailedException(override val message: String) : BaseException(message)

class NoFileProvidedException(val tag: String) :
    BaseException("you need provide a file to finish the operate see:$tag")

class MissCompressStrategyException : BaseException("compress image must have a strategy")

class PickNoResultException : BaseException("try to get local image with no result")

class ActivityStatusException :
    BaseException("activity is destroyed or in a error status check your current activity status before use imagePick")

class BadConvertException(val result: PickResult) :
    BaseException("convert uri to path failed") {
}