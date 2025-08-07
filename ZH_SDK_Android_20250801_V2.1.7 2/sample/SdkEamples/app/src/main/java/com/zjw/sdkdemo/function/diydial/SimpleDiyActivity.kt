package com.zjw.sdkdemo.function.diydial

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ResourceUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.zhapp.ble.bean.WatchFaceInstallResultBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener
import com.zhapp.ble.callback.DiyDialDataCallBack
import com.zhapp.ble.callback.DiyWatchFaceCallBack
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.callback.WatchFaceInstallCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivitySimpleDiyBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.AssetUtils
import com.zjw.sdkdemo.utils.ToastDialog
import com.zjw.sdkdemo.utils.Utils

/**
 * Created by Android on 2023/10/13.
 */
class SimpleDiyActivity : BaseActivity() {

    private var numberJsonStr = ""

    private var pointerJsonStr = ""

    private var diyWatchFaceConfig: DiyWatchFaceConfigBean? = null

    private var isSendPointer = false

    private val binding by lazy { ActivitySimpleDiyBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s361)
        setContentView(binding.root)

        //数字表盘信息 Number dial information
        numberJsonStr = ResourceUtils.readAssets2String("diy/number_diy/watch.json")
        LogUtils.d("numberJsonStr:" + GsonUtils.toJson(numberJsonStr))

        //指针表盘信息 Pointer dial information
        pointerJsonStr = ResourceUtils.readAssets2String("diy/pointer_diy/watch.json")
        LogUtils.d("numberJsonStr:" + GsonUtils.toJson(pointerJsonStr))

        diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(numberJsonStr)

        events()

        initCallBack()
    }

    private fun events() {
        click(binding.getJsonObject) {
            getJsonObject()
        }

        click(binding.sendNumber) {
            getNumberRuleCount()
        }

        click(binding.sendPointer) {
            getPointerRuleCount()
        }
    }

    public fun getJsonObject() {
        val numberDiyDialBean = ControlBleTools.getInstance().getZhDiyDialBeanByJson(numberJsonStr)
        //Dial Width Height
        val numberDialWidth = numberDiyDialBean.diyDialWH[0]
        val numberDialHeight = numberDiyDialBean.diyDialWH[1]
        //Thumbnail Width Height
        val numberThumbnailWidth = numberDiyDialBean.diyThumbnailWH[0]
        val numberThumbnailHeight = numberDiyDialBean.diyThumbnailWH[1]
        LogUtils.d("numberDiyDialBean:" + GsonUtils.toJson(numberDiyDialBean))

        val pointerDiyDialBean = ControlBleTools.getInstance().getZhDiyDialBeanByJson(pointerJsonStr)
        //Dial Width Height
        val pointerWidth = pointerDiyDialBean.diyDialWH[0]
        val pointerHeight = pointerDiyDialBean.diyDialWH[1]
        //Thumbnail Width Height
        val pointerThumbnailWidth = pointerDiyDialBean.diyThumbnailWH[0]
        val pointerThumbnailHeight = pointerDiyDialBean.diyThumbnailWH[1]
        LogUtils.d("pointerDiyDialBean:" + GsonUtils.toJson(pointerDiyDialBean))

        ToastDialog.showToast(
            this, "Number Dial \n width:$numberDialWidth, height:$numberDialHeight \n" +
                    "Number Thumbnail \n width:$numberThumbnailWidth, height:$numberThumbnailHeight \n" +
                    "Pointer Dial \n width:$pointerWidth, height:$pointerHeight \n" +
                    "Pointer Thumbnail \n width:$pointerThumbnailWidth, height:$pointerThumbnailHeight \n"
        )
    }


    public fun getPointerRuleCount() {
        isSendPointer = true
        diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(pointerJsonStr)

        //获取设备表盘功能状态
        ControlBleTools.getInstance().getDiyWatchFaceConfig(diyWatchFaceConfig?.id, object : ParsingStateManager.SendCmdStateListener() {
            override fun onState(state: SendCmdState?) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s220)
                    else -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s221)
                }
            }
        })

    }

    fun sendPointerDiy() {
        ControlBleTools.getInstance().getSimplePointerDiyDialData(
            //TODO  backgroundBm requires the background and overlay to be merged into one bitmap
            mutableListOf<Bitmap?>().apply {
                add(
                    Utils.combineBitmap(
                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/pointer_diy/background/background.png"),
                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/pointer_diy/overlay/overlay_00.png"), 0, 0
                    )
                )
//                add(
//                    Utils.combineBitmap(
//                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy_photo/photo_1.png"),
//                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/pointer_diy/overlay/overlay_00.png"), 0, 0
//                    )
//                )
            },
            AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/pointer_diy/background/thumbnail.png"),
            ConvertUtils.inputStream2Bytes(assets.open("diy/pointer_diy/pointer/3101_Data.bin")),
            ConvertUtils.inputStream2Bytes(assets.open("diy/pointer_diy/complex/complex.bin")),
            diyWatchFaceConfig, pointerJsonStr,
            object : DiyDialDataCallBack {
                override fun onDialData(diyDialId: String?, data: ByteArray?, configBean: DiyWatchFaceConfigBean?) {
                    if (diyDialId == null || data == null || configBean == null) {
                        MyApplication.showToast(R.string.s221)
                        if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                        return
                    }

                    LogUtils.d("diyDialId:" + diyDialId + ",Data size = ${data.size}" + "configBean:" + GsonUtils.toJson(configBean))
                    //需要更新文件和配置类
                    uploadWatch(diyDialId, data, configBean)
                }

                override fun onChangeConfig(configBean: DiyWatchFaceConfigBean?) {
                    if (configBean == null) {
                        MyApplication.showToast(R.string.s221)
                        if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                        return
                    }

                    //只需要更新配置类
                    ControlBleTools.getInstance().setDiyWatchFaceConfig(configBean, object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
                            if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                            when (state) {
                                SendCmdState.SUCCEED -> {
                                    MyApplication.showToast(R.string.s220)

                                    diyWatchFaceConfig = configBean
                                }

                                else -> MyApplication.showToast(R.string.s221)
                            }
                        }
                    })
                }

                override fun onError(errMsg: String?) {
                    LogUtils.e(errMsg)
                    errMsg?.let { MyApplication.showToast(it) }
                }

            }
        )
    }


    public fun getNumberRuleCount() {
        isSendPointer = false
        diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(numberJsonStr)

        //获取设备表盘功能状态  Get device dial faceConfig
        ControlBleTools.getInstance().getDiyWatchFaceConfig(diyWatchFaceConfig?.id, object : ParsingStateManager.SendCmdStateListener() {
            override fun onState(state: SendCmdState?) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s220)
                    else -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s221)
                }
            }
        })
    }

    fun sendNumberDiy() {
        ControlBleTools.getInstance().getSimpleNumberDiyDialData(
            //TODO  backgroundBm requires the background and overlay to be merged into one bitmap
            mutableListOf<Bitmap?>().apply {
                add(
                    Utils.combineBitmap(
                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/number_diy/background/background.png"),
                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/number_diy/overlay/overlay_00.png"), 0, 0
                    )
                )
//                add(
//                    Utils.combineBitmap(
//                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy_photo/photo_1.png"),
//                        AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/number_diy/overlay/overlay_00.png"), 0, 0
//                    )
//                )
            },
            AssetUtils.getAssetBitmap(this@SimpleDiyActivity, "diy/number_diy/background/designsketch.png"),
            "G1_MidUp", //TODO from json 'fontsName_locationName'
            ConvertUtils.inputStream2Bytes(assets.open("diy/number_diy/time/G1/G1_Data.bin")),
            ConvertUtils.inputStream2Bytes(assets.open("diy/number_diy/complex/complex.bin")),
            mutableListOf<IntArray?>().apply {
                add(intArrayOf(255, 255, 255))
//                add(intArrayOf(255, 255, 255))
            },
            diyWatchFaceConfig, numberJsonStr, object : DiyDialDataCallBack {
                override fun onDialData(diyDialId: String?, data: ByteArray?, configBean: DiyWatchFaceConfigBean?) {
                    if (diyDialId == null || data == null || configBean == null) {
                        MyApplication.showToast(R.string.s221)
                        if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                        return
                    }

                    LogUtils.d("diyDialId:" + diyDialId + ",Data size = ${data.size}" + "configBean:" + GsonUtils.toJson(configBean))
                    //需要更新文件和配置类
                    uploadWatch(diyDialId, data, configBean)
                }

                override fun onChangeConfig(configBean: DiyWatchFaceConfigBean?) {
                    if (configBean == null) {
                        MyApplication.showToast(R.string.s221)
                        if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                        return
                    }

                    //只需要更新配置类
                    ControlBleTools.getInstance().setDiyWatchFaceConfig(configBean, object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
                            if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                            when (state) {
                                SendCmdState.SUCCEED -> {
                                    MyApplication.showToast(R.string.s220)

                                    diyWatchFaceConfig = configBean
                                }

                                else -> MyApplication.showToast(R.string.s221)
                            }
                        }
                    })
                }

                override fun onError(errMsg: String?) {
                    LogUtils.e(errMsg)
                    errMsg?.let { ToastDialog.showToast(this@SimpleDiyActivity,errMsg) }
                }

            }
        )
    }


    private var loadingDialog: Dialog? = null
    private fun uploadWatch(watchId: String, data: ByteArray, configBean: DiyWatchFaceConfigBean) {
        if (!ControlBleTools.getInstance().isConnect) {
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }
            return
        }
        ControlBleTools.getInstance().getDeviceDiyWatchFace(watchId, data.size, true, configBean, object : DeviceWatchFaceFileStatusListener {
            override fun onSuccess(statusValue: Int, statusName: String) {
                if (loadingDialog != null && loadingDialog!!.isShowing) {
                    loadingDialog!!.dismiss()
                }
                if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.READY.state) {
                    sendWatchData(data, configBean)
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.BUSY.state) {
                    MyApplication.showToast(getString(R.string.s223))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.DUPLICATED.state) {
                    MyApplication.showToast(getString(R.string.s224))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_STORAGE.state) {
                    MyApplication.showToast(getString(R.string.s224))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_BATTERY.state) {
                    MyApplication.showToast(getString(R.string.s225))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.DOWNGRADE.state) {
                    MyApplication.showToast(getString(R.string.s224))
                }
            }

            override fun timeOut() {
                MyApplication.showToast("timeOut")
                if (loadingDialog != null && loadingDialog!!.isShowing) {
                    loadingDialog!!.dismiss()
                }
            }
        })
    }

    private fun sendWatchData(data: ByteArray, configBean: DiyWatchFaceConfigBean) {
        showDialog()
        ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_WATCH, data, true, object : UploadBigDataListener {
            override fun onSuccess() {
//                if (progressDialog != null && progressDialog!!.isShowing) {
//                    progressDialog!!.dismiss()
//                }
                diyWatchFaceConfig = configBean
            }

            override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                val progress = curPiece * 100 / dataPackTotalPieceLength
                progressBar?.progress = progress
                tvDeviceUpdateProgress?.setText("${progress}%")
            }

            override fun onTimeout(msg:String?) {
                Log.e("PhotoDialActivity", "startUploadBigData timeOut")
                if (progressDialog != null && progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
            }
        })
    }

    private var progressDialog: Dialog? = null
    var msg: TextView? = null
    var tvDeviceUpdateProgress: TextView? = null
    private var progressBar: ProgressBar? = null

    private fun showDialog() {
        if (progressDialog == null) {
            progressDialog = Dialog(this, R.style.progress_dialog)
            progressDialog!!.setContentView(R.layout.update_layout)
            progressDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }
        msg = progressDialog!!.findViewById<View>(R.id.tvLoading) as TextView
        progressBar = progressDialog!!.findViewById(R.id.progressBar)
        tvDeviceUpdateProgress = progressDialog!!.findViewById<TextView>(R.id.tvDeviceUpdateProgress)
        progressBar?.setVisibility(View.VISIBLE)
        progressDialog!!.show()
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.setOnDismissListener { dialog: DialogInterface? -> }
    }

    private fun initCallBack() {
        //获取设备的规则
        CallBackUtils.diyWatchFaceCallBack = object : DiyWatchFaceCallBack {
            override fun onDiyWatchFaceStatus(config: DiyWatchFaceConfigBean?) {
                if (config != null) {
                    LogUtils.d("config:" + GsonUtils.toJson(config))
                    diyWatchFaceConfig?.ruleCount = config.ruleCount
                }
                if (isSendPointer) {
                    sendPointerDiy()
                } else {
                    sendNumberDiy()
                }
            }
        }

        /**
         * 表盘文件安装结果回调
         */
        CallBackUtils.watchFaceInstallCallBack = object : WatchFaceInstallCallBack {
            override fun onresult(result: WatchFaceInstallResultBean?) {
                if (progressDialog != null && progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
                if (result != null) {
                    when (result?.code) {
                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_SUCCESS.state -> {
                            //安装成功
                            MyApplication.showToast(R.string.s497)
                        }

                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_FAILED.state -> {
                            //安装失败
                            MyApplication.showToast(R.string.s498)
                        }

                        WatchFaceInstallCallBack.WatchFaceInstallCode.VERIFY_FAILED.state -> {
                            //验证失败
                            MyApplication.showToast(R.string.s499)
                        }
                    }
                }

            }
        }
    }
    //endregion
}