package com.dh.imagepick.dispose

import com.dh.imagepick.constant.CompressStrategy


object CompressFactory {

    fun create(strategy: CompressStrategy): ICompress {
        return when (strategy) {
            CompressStrategy.MATRIX -> {
                MatrixCompressor()
            }
            CompressStrategy.QUALITY -> {
                QualityCompressor()
            }
        }
    }
}