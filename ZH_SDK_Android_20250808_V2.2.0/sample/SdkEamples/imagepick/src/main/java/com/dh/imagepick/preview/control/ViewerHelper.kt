package com.dh.imagepick.preview.control

import androidx.fragment.app.FragmentActivity
import com.dh.imagepick.preview.viewer.MyImageLoader
import com.dh.imagepick.preview.viewer.MyTransformer
import com.dh.imagepick.preview.ImageViewerBuilder
import com.dh.imagepick.preview.ImageViewerDialogFragment
import com.dh.imagepick.preview.bean.MyData
import com.dh.imagepick.preview.core.DataProvider
import com.dh.imagepick.preview.viewer.FullScreenImageViewerDialogFragment
import com.dh.imagepick.preview.viewer.provideViewerDataProvider

/**
 * viewer的自定义初始化方案
 */
object ViewerHelper {
    private var myData: List<MyData> = emptyList()
    var orientationH: Boolean = true
    var loadAllAtOnce: Boolean = false
    var fullScreen: Boolean = false
    var simplePlayVideo: Boolean = true

    fun provideImageViewerBuilder(context: FragmentActivity, clickedData: MyData, pageKey: String,listData : List<MyData>): ImageViewerBuilder {
        // viewer 构造的基本元素
        myData = listData
        val builder = ImageViewerBuilder(
                context = context,
                initKey = clickedData.id,
                dataProvider = myDataProvider(clickedData),
                imageLoader = MyImageLoader(),
                transformer = MyTransformer(pageKey)
        )

        MyViewerCustomizer().process(context, builder) // 添加自定义业务逻辑和UI处理

        if (fullScreen) {
            builder.setViewerFactory(object : ImageViewerDialogFragment.Factory() {
                override fun build() = FullScreenImageViewerDialogFragment()
            })
        }
        return builder
    }

    // 数据提供者 一次加载 or 分页
    private fun myDataProvider(clickedData: MyData): DataProvider {
//        return if (loadAllAtOnce) {
//            provideViewerDataProvider { myData }
//        } else {
//            provideViewerDataProvider(
//                    loadInitial = { listOf(clickedData) },
//                    loadAfter = { id, callback -> Api.asyncQueryAfter(id, callback) },
//                    loadBefore = { id, callback -> Api.asyncQueryBefore(id, callback) }
//            )
//        }
        return  provideViewerDataProvider { myData }
    }
}

