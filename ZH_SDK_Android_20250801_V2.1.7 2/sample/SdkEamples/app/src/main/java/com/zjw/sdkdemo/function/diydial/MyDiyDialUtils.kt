package com.zjw.sdkdemo.function.diydial

import android.content.Context
import android.text.TextUtils
import com.zhapp.ble.callback.DiyWatchFaceCallBack
import com.zjw.sdkdemo.R

/**
 * Created by Android on 2023/2/17.
 */
object MyDiyDialUtils {

    /**
     * 获取位置昵称
     */
    fun getFunctionsLocationNameByType(context: Context, type: Int): String {
        return when (type) {
            DiyWatchFaceCallBack.DiyWatchFaceLocation.MIDDLE_UP.location -> context.getString(R.string.s369)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.LEFT_MIDDLE.location -> context.getString(R.string.s370)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.RIGHT_MIDDLE.location -> context.getString(R.string.s371)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.MIDDLE_DOWN.location -> context.getString(R.string.s372)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.LEFT_UP.location -> context.getString(R.string.s431)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.RIGHT_UP.location -> context.getString(R.string.s432)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.MIDDLE.location -> context.getString(R.string.s433)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.LEFT_DOWN.location -> context.getString(R.string.s434)
            DiyWatchFaceCallBack.DiyWatchFaceLocation.RIGHT_DOWN.location -> context.getString(R.string.s435)
            else -> ""
        }
    }

    /**
     * 获取复杂功能昵称
     */
    fun getFunctionsDetailNameByType(context: Context, type: Int): String {
        return when (type) {
            DiyWatchFaceCallBack.DiyWatchFaceFunction.STEP.function -> context.getString(R.string.s373)
            DiyWatchFaceCallBack.DiyWatchFaceFunction.BATTERY.function -> context.getString(R.string.s374)
            DiyWatchFaceCallBack.DiyWatchFaceFunction.CALORIE.function -> context.getString(R.string.s375)
            DiyWatchFaceCallBack.DiyWatchFaceFunction.GENERAL_DATE.function -> context.getString(R.string.s376)
            DiyWatchFaceCallBack.DiyWatchFaceFunction.HEART_RATE.function -> context.getString(R.string.s405)
            DiyWatchFaceCallBack.DiyWatchFaceFunction.OFF.function -> context.getString(R.string.s506)
            else -> ""
        }
    }

    /**
     * 根据复杂功能类型名获取类型
     */
    fun getDiyWatchFaceFunctionByTypeName(typeName: String): DiyWatchFaceCallBack.DiyWatchFaceFunction {
        var function = DiyWatchFaceCallBack.DiyWatchFaceFunction.BATTERY
        val fStr: String = typeName.uppercase()
        if (TextUtils.equals(fStr, "Kwh".uppercase())) {
            function = DiyWatchFaceCallBack.DiyWatchFaceFunction.BATTERY
        } else if (TextUtils.equals(fStr, "GeneralDate".uppercase())) {
            function = DiyWatchFaceCallBack.DiyWatchFaceFunction.GENERAL_DATE
        } else if (TextUtils.equals(fStr, "Step".uppercase())) {
            function = DiyWatchFaceCallBack.DiyWatchFaceFunction.STEP
        } else if (TextUtils.equals(fStr, "HeartRate".uppercase())) {
            function = DiyWatchFaceCallBack.DiyWatchFaceFunction.HEART_RATE
        } else if (TextUtils.equals(fStr, "Calorie".uppercase())) {
            function = DiyWatchFaceCallBack.DiyWatchFaceFunction.CALORIE
        }
        return function
    }

    /**
     * 获取DIY json
     */
    /*fun getDiyDialJsonByDiyDialInfoResponse(data: DiyDialInfoResponse): String {
        val zhDiyDialBean = ZhDiyDialBean()
        try {
            zhDiyDialBean.apply {
                id = data.code
                watchVersion = data.deviceParsingRules.toInt()
                shape = ""
                dpi = data.dpi
                thumbnailDpi = data.thumbnailDpi
                thumbnailOffset = data.thumbnailOffset
                type = "DIY"
                author = ""
                code = ""
                watchName = data.name
                watchDesc = data.desc
                background =
                    ZhDiyDialBean.BackgroundBean().apply {
                        md5Background = ""
                        backgroundImgPath = data.defaultBackgroundImage
                        md5Thumbnail = ""
                        thumbnailImgPath = data.renderings
                        overlayImgPath = data.backgroundOverlay
                        designsketchImgPath = ""
                    }
                complex =
                    ZhDiyDialBean.ComplexBean().apply {
                        isCompress = TextUtils.equals(data.complexBinCompressed, "1")
                        md5 = ""
                        path = data.complicationsBin
                        val infoList = mutableListOf<ZhDiyDialBean.ComplexBean.InfosBean>()
                        if (data.positionList != null && data.positionList!!.isNotEmpty()) {
                            for (p in data.positionList!!) {
                                infoList.add(ZhDiyDialBean.ComplexBean.InfosBean().apply {
                                    location = when (p.positionCode) {
                                        "1" -> "LeftUp"
                                        "2" -> "MidUp"
                                        "3" -> "RightUp"
                                        "4" -> "LeftMid"
                                        "5" -> "Mid"
                                        "6" -> "RightMid"
                                        "7" -> "LeftBottom"
                                        "8" -> "MidBottom"
                                        "9" -> "RightBottom"
                                        else -> "LeftUp"
                                    }
                                    val details = mutableListOf<ZhDiyDialBean.ComplexBean.InfosBean.DetailBean>()
                                    if (p.dataElementList != null && p.dataElementList!!.isNotEmpty()) {
                                        for (elemen in p.dataElementList!!) {
                                            details.add(ZhDiyDialBean.ComplexBean.InfosBean.DetailBean().apply {
                                                isDefault = TextUtils.equals(elemen.selected, "1")
                                                typeName = when (elemen.dataElementCode) {
                                                    "1" -> "Kwh"
                                                    "2" -> "GeneralDate"
                                                    "3" -> "Step"
                                                    "4" -> "HeartRate"
                                                    "5" -> "Calorie"
                                                    else -> "Kwh"
                                                }
                                                pointX = elemen.coordinateX.toInt()
                                                pointY = elemen.coordinateY.toInt()
                                                picPath = elemen.imgUrl
                                            })
                                        }
                                    }
                                    detail = details
                                })
                            }
                        }
                        infos = infoList
                    }
                val ps = mutableListOf<ZhDiyDialBean.PointersBean>()
                val ts = mutableListOf<ZhDiyDialBean.TimesBean>()
                if (data.pointerList != null && data.pointerList!!.isNotEmpty()) {
                    for (p in data.pointerList!!) {
                        if (p.type == "1") {
                            ps.add(ZhDiyDialBean.PointersBean().apply {
                                isCompress = TextUtils.equals(data.pointerCompressed, "1")
                                md5 = ""
                                pointerImgPath = p.renderingsUrl
                                pointerDataPath = p.binUrl
                                pointerImgPath = p.renderingsUrl
                                pointerOverlayPath = p.pointerPictureUrl
                                //p.pointerCoordinatesX
                                //p.pointerCoordinatesY
                            })
                        } else if (p.type == "2") {
                            ts.add(ZhDiyDialBean.TimesBean().apply {
                                isCompress = TextUtils.equals(data.pointerCompressed, "1")
                                md5 = ""
                                timeImgPath = p.renderingsUrl
                                timeDataPath = p.binUrl
                                timeImgPath = p.renderingsUrl
                                timeOverlayPath = p.pointerPictureUrl
                                //p.pointerCoordinatesX
                                //p.pointerCoordinatesY
                            })
                        }
                    }
                }
                pointers = ps
                time = ts
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return GsonUtils.toJson(zhDiyDialBean)
    }*/


}