package com.zjw.sdkdemo.function.sifli

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.sifli.watchfacelibraryzh.SifliWatchfaceService
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WatchFaceInstallResultBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener
import com.zhapp.ble.callback.WatchFaceInstallCallBack
import com.zjw.sdkdemo.BraceletActivity
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivitySifliPhotoDialBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.AssetUtils
import com.zjw.sdkdemo.utils.ToastDialog
import java.io.File

/**
 * Created by Android on 2023/11/15.
 * !!!  此为设备号 50000 的测试资源（由资源A501FF0090001.zip解压后获取），对应项目需更换：assets/sifli/photo/...
 */
class SifliPhotoDialActivity : BaseActivity() {

    private var receiver: DfuReceiver? = null

    var faceProgress = -1

    private var loadingDialog: Dialog? = null //loadingDialog = LoadingDialog.show(this)

    private var colorR = 255
    private var colorG = 255
    private var colorB = 255
    private var sourceBinFileName: String? = null
    private var textFileNmae: String? = null
    private var bgFileName: String? = null
    private var bgBitmap: Bitmap? = null
    private var textBitmap: Bitmap? = null
    private var sourceData: ByteArray? = null

    private val binding by lazy { ActivitySifliPhotoDialBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s21)
        setContentView(binding.root)
        inits()
        clicks()
    }

    private fun inits() {
        //由资源zip解压后获取
        sourceBinFileName = "A501FF0090001.bin"
        sourceData = AssetUtils.getAssetBytes(this, "sifli/photo/A501FF0090001" + File.separator + sourceBinFileName)
        textFileNmae = "img_zdy_text.png"
        textBitmap = AssetUtils.getAssetBitmap(this, "sifli/photo/A501FF0090001" + File.separator + textFileNmae)
        bgFileName = "img_zdy_bg.png"
        bgBitmap = AssetUtils.getAssetBitmap(this, "sifli/photo/A501FF0090001" + File.separator + bgFileName)
        updateUi()

        //注册本地广播，获取结果，进度
        val intentFilter = IntentFilter()
        intentFilter.addAction(SifliWatchfaceService.BROADCAST_WATCHFACE_STATE)
        intentFilter.addAction(SifliWatchfaceService.BROADCAST_WATCHFACE_PROGRESS)
        receiver = DfuReceiver()
        //通过第三方sdk注册本地广播
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver!!, intentFilter)

        //监听表盘安装结果
        CallBackUtils.setWatchFaceInstallCallBack(object : WatchFaceInstallCallBack {
            override fun onresult(result: WatchFaceInstallResultBean?) {
                com.blankj.utilcode.util.LogUtils.d("表盘安装结果：$result")

                when (result?.code) {
                    WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_SUCCESS.state -> {
                        //安装成功

                    }

                    WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_FAILED.state -> {
                        //安装失败
                    }

                    WatchFaceInstallCallBack.WatchFaceInstallCode.VERIFY_FAILED.state -> {
                        //验证失败

                    }
                }
            }
        })

    }

    private fun updateUi() {
        //获取修改后的效果图 Get the modified rendering
        ControlBleTools.getInstance().myCustomClockUtils(sourceData, colorR, colorG, colorB, bgBitmap, textBitmap) { result: Bitmap? ->
            binding.ivEffect.setImageBitmap(result)
        }
    }

    private fun clicks() {
        click(binding.ivColor1) {
            colorR = 51
            colorG = 153
            colorB = 255
            updateUi()
        }
        click(binding.ivColor2) {
            colorR = 255
            colorG = 153
            colorB = 51
            updateUi()
        }
        click(binding.ivColor3) {
            colorR = 51
            colorG = 255
            colorB = 153
            updateUi()
        }

        click(binding.btnWatchface) {
            ControlBleTools.getInstance().getDeviceWatchFace("watch face id", sourceData!!.size, true, object : DeviceWatchFaceFileStatusListener {
                override fun onSuccess(statusValue: Int, statusName: String) {
                    if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.READY.state) {
                        sendWatchData()
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
                }
            })
        }

        click(binding.btnClearLog) {
            binding.llLog.removeAllViews()
        }
    }

    private fun sendWatchData() {
        ControlBleTools.getInstance().newCustomClockDialData(
            sourceData, colorR, colorG,
            colorB, bgBitmap, textBitmap, { data ->
                val fileDestPath = PathUtils.getExternalAppFilesPath() + "/dial" + File.separator + "sifliPhoto" + System.currentTimeMillis() + ".zip"
                val fileDest = getBytesToZipCachePath(data,fileDestPath)
                SifliWatchfaceService.startActionWatchface(this@SifliPhotoDialActivity, fileDest, BraceletActivity.deviceAddress, 0, 1)
            }, true
        )
    }

    private fun getBytesToZipCachePath(bFile: ByteArray?, fileDest: String?): String? {
        FileIOUtils.writeFileFromIS(fileDest, ConvertUtils.bytes2InputStream(bFile))
        return fileDest
    }

    //region 本地超时

    private inner class SifliDFUTask : ThreadUtils.SimpleTask<Int>() {
        var i = 0
        var isOk = false

        fun finish(isOk: Boolean) {
            this.isOk = isOk
            i = 30
        }

        override fun doInBackground(): Int {
            while (i <= 30) {
                i++
                Thread.sleep(1000)
            }
            return 0
        }

        override fun onSuccess(result: Int?) {
            //超时 或者 完成（成功失败）
            //DialogUtils.dismissDialog(dialog)
            //清除已使用的文件
            FileUtils.deleteAllInDir(PathUtils.getExternalAppFilesPath() + "/dial/")
            if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog?.dismiss()
            if (!isOk) {
                ToastDialog.showToast(this@SifliPhotoDialActivity, getString(R.string.s221))
            } else {
                ToastDialog.showToast(this@SifliPhotoDialActivity, getString(R.string.s220))
            }
        }
    }

    private var sifliDFUTask: SifliDFUTask? = null
    private fun startOrRefSifliTimeOut() {
        if (sifliDFUTask != null) {
            ThreadUtils.cancel(sifliDFUTask)
        }
        sifliDFUTask = SifliDFUTask()
        ThreadUtils.executeByIo(sifliDFUTask)
    }

    //endregion


    private fun sifliFaceProgress(progress: Int?) {
        startOrRefSifliTimeOut()
        progress?.let {
            ToastUtils.showShort("progress：$progress")
        }
    }

    private fun sifliFaceState(state: Int, rsp: Int) {
        if (state == 2 && rsp == 37) { //设备内存不足，弹出相应提示
            ToastDialog.showToast(this,getString(R.string.s508))
            //固件的设备日志内存占用导致内存不足，需要调用获取设备固件日志接口释放内存，参考如下：
            //1.设置获取固件日志回调
            //CallBackUtils.setFirmwareLogStateCallBack(object : FirmwareLogStateCallBack {
            //    override fun onFirmwareLogState(state: Int) {
            //        when (state) {
            //            FirmwareLogStateCallBack.FirmwareLogState.START.state -> {
            //                LogUtils.d("start upload Firmware log")
            //                delayDismissDialog()
            //            }
            //
            //            FirmwareLogStateCallBack.FirmwareLogState.UPLOADING.state -> {
            //                LogUtils.d("uploading Firmware log")
            //                delayDismissDialog()
            //            }
            //
            //            FirmwareLogStateCallBack.FirmwareLogState.END.state -> {
            //                LogUtils.d("end upload Firmware log")
            //                delayDismissDialog()
            //            }
            //        }
            //    }
            //
            //    override fun onFirmwareLogFilePath(filePath: String?) {
            //
            //    }
            //
            //})
            //2.获取固件日志
            //ControlBleTools.getInstance().getFirmwareLog(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
            //    override fun onState(state: SendCmdState?) {
            //        if (state != SendCmdState.SUCCEED) {
            //            ToastUtils.showLong(getString(R.string.set_fail) + ":" + state)
            //        }
            //    }
            //})
        }
        sifliDFUTask?.finish(state == 0)
    }

    //endregion


    //region 状态进度广播
    inner class DfuReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                when (intent.action) {
                    SifliWatchfaceService.BROADCAST_WATCHFACE_STATE -> {
                        val state = intent.getIntExtra(SifliWatchfaceService.EXTRA_WATCHFACE_STATE, -1)
                        val rsp = intent.getIntExtra(SifliWatchfaceService.EXTRA_WATCHFACE_STATE_RSP, 0)

                        Log.d("FACE STATE", "state:$state, rsp:$rsp")
                        binding.llLog.addView(TextView(this@SifliPhotoDialActivity).apply {
                            text = "FACE STATE: state:$state, rsp:$rsp"
                        }, 0)
//                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_FACE_STATE, SifliFaceState(state, rsp)))
                        faceProgress = -1
                        sifliFaceState(state, rsp)
                    }

                    SifliWatchfaceService.BROADCAST_WATCHFACE_PROGRESS -> {
                        val progress = intent.getIntExtra(SifliWatchfaceService.EXTRA_WATCHFACE_PROGRESS, 0)
                        binding.llLog.addView(TextView(this@SifliPhotoDialActivity).apply {
                            text = "FACE PROGRESS: progress:$progress"
                        }, 0)
                        if (faceProgress != progress) {
                            faceProgress = progress
                            Log.d("FACE PROGRESS", "progress:$progress")
                            sifliFaceProgress(faceProgress)
                        }
                    }

                }
            }
        }
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (receiver != null) {
                //注解广播
                LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}