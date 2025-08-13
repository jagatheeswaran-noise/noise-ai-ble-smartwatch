package com.zjw.sdkdemo.function.sifli

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.ZipUtils
import com.sifli.siflidfu.DFUImagePath
import com.sifli.siflidfu.Protocol
import com.sifli.siflidfu.SifliDFUService
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.SifliDfuDeviceCallBack
import com.zjw.sdkdemo.BraceletActivity
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivitySifliOtaBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.LoadingDialog
import com.zjw.sdkdemo.utils.ToastDialog
import com.zjw.sdkdemo.utils.Utils
import java.util.ArrayList
import java.util.Locale

/**
 * Created by Android on 2023/8/2.
 *
 * 如果ota失败，固件将进入ota模式，ota模式无法回连，蓝牙名和蓝牙广播将发生变化，升级模式下设备页面上会有一个二维码。
 * 通过以下方式检测设备是否进入ota模式
 * 1.扫描设备二维码
 * 使用DeviceScanQrCodeBean bean = new DeviceScanQrCodeBean("二维码内容")；
 * if(bean.getDfu() == "1" || bean.isDfu()) 则是 升级模式的设备，可以直接调用ota升级方法继续升级
 * 2.扫描设备
 * ControlBleTools.getInstance().startScanDevice(ScanDeviceCallBack)
 * 回调ScanDeviceCallBack.onBleScan(ScanDeviceBean bean)
 * if(bean.isDfu()) 则是 升级模式的设备，可以直接调用ota升级方法继续升级
 * 3.设备回连时，通过监听思澈ota模式回调
 * （只会返回当前连接的设备，只有思澈平台时设置次回调，解绑启用其它设备时请设置null'CallBackUtils.setSifliDfuDeviceCallBack(null)'）
 * CallBackUtils.setSifliDfuDeviceCallBack(SifliDfuDeviceCallBack)
 * 回调：SifliDfuDeviceCallBack.onSifliDfuMac(String mac)
 * 接收到回调则当前连接的设备是ota模式，可以直接调用ota升级方法继续升级
 *
 * -----------------------------------------------------------------------------------------------------------------------------------
 *
 * If ota fails, the firmware will enter ota mode, which cannot be called back. The Bluetooth name and Bluetooth broadcast
 * will change. There will be a QR code on the device page in upgrade mode.
 * Detect whether the device enters ota mode through the following methods
 * 1. Scan the device QR code
 * Use DeviceScanQrCodeBean bean = new DeviceScanQrCodeBean("QR code content");
 * if(bean.getDfu() == "1" || bean.isDfu()) is a device in upgrade mode. You can directly call the ota upgrade method to continue the upgrade.
 * 2. Scanning device
 * ControlBleTools.getInstance().startScanDevice(ScanDeviceCallBack)
 * CallbackScanDeviceCallBack.onBleScan(ScanDeviceBean bean)
 * if(bean.isDfu()) is a device in upgrade mode. You can directly call the ota upgrade method to continue the upgrade.
 * 3. When the device connects back, it calls back by monitoring the Scheota mode.
 * (Only the currently connected device will be returned. When only Sich platform is used, set the callback. When unbinding and enabling other devices,
 * please set null'CallBackUtils.setSifliDfuDeviceCallBack(null)')
 * CallBackUtils.setSifliDfuDeviceCallBack(SifliDfuDeviceCallBack)
 * Callback: SifliDfuDeviceCallBack.onSifliDfuMac(String mac)
 * When the callback is received, the currently connected device is in ota mode, and you can directly call the ota upgrade method to continue the upgrade.
 *
 */
class SifliOtaActivity : BaseActivity() {

    private var receiver: DfuReceiver? = null

    private var loadingDialog: Dialog? = null

    //ota 文件列表
    private var slfliOtaFiles = ArrayList<DFUImagePath>()

    var dfuProgress = -1

    private val binding by lazy { ActivitySifliOtaBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s37)
        setContentView(binding.root)
        inits()
        clicks()
    }

    private fun clicks() {
        click(binding.btnWatchface) {
            //将文件保存至本地（或从服务器下载ota文件）并解压
            if (saveFileAndUnZip()) {
                loadingDialog = LoadingDialog.show(this)
                //启用本地超时
                startOrRefSifliTimeOut()
                //执行思澈dfu方法
                //ResumeMode:
                //设置为0时，OTA将始终重新开始传输
                //设置为1时，SDK会自动判断续传条件，会在可以续传的时候尝试续传，不能续传的时候也会重新开始传输。
                SifliDFUService.startActionDFUNorExt(this, BraceletActivity.deviceAddress, slfliOtaFiles, 1, 0)
                //断连设备
                ControlBleTools.getInstance().disconnect()
            }
        }

        click(binding.btnClearLog) {
            binding.llLog.removeAllViews()
        }
    }

    private fun inits() {

        //注册本地广播，获取结果，进度
        val intentFilter = IntentFilter()
        intentFilter.addAction(SifliDFUService.BROADCAST_DFU_LOG)
        intentFilter.addAction(SifliDFUService.BROADCAST_DFU_STATE)
        intentFilter.addAction(SifliDFUService.BROADCAST_DFU_PROGRESS)
        receiver = DfuReceiver()
        //通过第三方sdk注册本地广播
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver!!, intentFilter)

        //当前为思澈平台的设备时设置
        CallBackUtils.setSifliDfuDeviceCallBack(object : SifliDfuDeviceCallBack {
            override fun onSifliDfuMac(mac: String?) {
                ThreadUtils.runOnUiThread {
                    if (TextUtils.equals(mac, BraceletActivity.deviceAddress)) {
                        //执行ota
                        binding.btnWatchface.callOnClick()
                    }
                }
            }
        })
    }

    //region 处理文件
    private fun saveFileAndUnZip(): Boolean {
        try {
            //清除已使用的文件
            FileUtils.deleteAllInDir(PathUtils.getExternalAppFilesPath() + "/ota/")
            //获取文件资源或从服务器下载ota文件
            val fileByte = Utils.getBytesByAssets(this@SifliOtaActivity, "sifli/sifli_ota_2.zip")
            val fileDir = PathUtils.getExternalAppDownloadPath()
            val filePath = fileDir + "sifli_ota_" + System.currentTimeMillis() + ".zip"
            val inputStream = ConvertUtils.bytes2InputStream(fileByte)
            //保存文件
            FileIOUtils.writeFileFromIS(filePath, inputStream)
            //获取文件
            val zipFile = FileUtils.getFileByPath(filePath)
            //创建解压路径
            val unZipDirPath = PathUtils.getExternalAppFilesPath() + "/ota/" + FileUtils.getFileNameNoExtension(zipFile)
            FileUtils.createOrExistsDir(unZipDirPath)
            //解压文件
            ZipUtils.unzipFile(zipFile, FileUtils.getFileByPath(unZipDirPath))
            //遍历文件
            val result = FileUtils.listFilesInDir(unZipDirPath, true)
            slfliOtaFiles.clear()
            for (file in result) {
                //填充文件
                val name: String = FileUtils.getFileNameNoExtension(file)
                var dfuFile: DFUImagePath? = null
                if (name.uppercase(Locale.ENGLISH).contains("ctrl_packet".uppercase(Locale.ENGLISH))) {
                    dfuFile = DFUImagePath(null, UriUtils.file2Uri(file), Protocol.IMAGE_ID_CTRL)
                } else if (name.uppercase(Locale.ENGLISH).contains("outapp".uppercase(Locale.ENGLISH)) || name.uppercase(Locale.ENGLISH)
                        .contains("outcom_app".uppercase(Locale.ENGLISH))
                ) {
                    dfuFile = DFUImagePath(null, UriUtils.file2Uri(file), Protocol.IMAGE_ID_HCPU)
                } else if (name.uppercase(Locale.ENGLISH).contains("outex".uppercase(Locale.ENGLISH)) || name.uppercase(Locale.ENGLISH)
                        .contains("outcom_ex".uppercase(Locale.ENGLISH))
                ) {
                    dfuFile = DFUImagePath(null, UriUtils.file2Uri(file), Protocol.IMAGE_ID_EX)
                } else if (name.uppercase(Locale.ENGLISH).contains("outfont".uppercase(Locale.ENGLISH)) || name.uppercase(Locale.ENGLISH)
                        .contains("outcom_font".uppercase(Locale.ENGLISH))
                ) {
                    dfuFile = DFUImagePath(null, UriUtils.file2Uri(file), Protocol.IMAGE_ID_FONT)
                } else if (name.uppercase(Locale.ENGLISH).contains("outres".uppercase(Locale.ENGLISH)) || name.uppercase(Locale.ENGLISH)
                        .contains("outcom_res".uppercase(Locale.ENGLISH))
                ) {
                    dfuFile = DFUImagePath(null, UriUtils.file2Uri(file), Protocol.IMAGE_ID_RES)
                }
                if (dfuFile != null) {
                    slfliOtaFiles.add(dfuFile)
                }
            }
            //无有效文件
            if (slfliOtaFiles.isNullOrEmpty()) {
                ToastDialog.showToast(this@SifliOtaActivity, getString(R.string.s221))
                return false
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@SifliOtaActivity, getString(R.string.s221))
        }
        return false
    }
    //endregion

    //region 本地超时

    private inner class SifliDFUTask : ThreadUtils.SimpleTask<Int>() {
        var i = 0
        var isOk = false

        fun finish(isOk: Boolean) {
            this.isOk = isOk
            i = 60
        }

        override fun doInBackground(): Int {
            while (i < 60) {
                i++
                Thread.sleep(1000)
            }
            return 0
        }

        override fun onSuccess(result: Int?) {
            //超时 或者 完成（成功失败）

            //清除已使用的文件
            FileUtils.deleteAllInDir(PathUtils.getExternalAppFilesPath() + "/ota/")
            if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
            if (!isOk) {
                dfuProgress = -1
                ToastDialog.showToast(this@SifliOtaActivity, getString(R.string.s221))
            } else {
                dfuProgress = -1
                ToastDialog.showToast(this@SifliOtaActivity, getString(R.string.s220))
            }
            //重连设备
            ControlBleTools.getInstance().connect(BraceletActivity.deviceName, BraceletActivity.deviceAddress, BraceletActivity.deviceProtocol)
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

    //region 处理状态
    private fun sifliOtaState(state: Int, stateResult: Int) {
        if (stateResult != 0) {
            //失败
            //DialogUtils.dismissDialog(slfliLoading)
            sifliDFUTask?.finish(false)
            dfuProgress = -1
        } else {
            if (state == Protocol.DFU_SERVICE_EXIT) {
                //成功
                sifliDFUTask?.finish(true)
                dfuProgress = -1
            }
        }
    }
    //endregion

    //region 状态进度广播
    inner class DfuReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                when (intent.action) {

                    //日志
                    SifliDFUService.BROADCAST_DFU_LOG -> {
                        val log = intent.getStringExtra(SifliDFUService.EXTRA_LOG_MESSAGE)
                        binding.llLog.addView(TextView(this@SifliOtaActivity).apply {
                            text = "DFU LOG:$log"
                        }, 0)

                        Log.d("DFU LOG", log ?: "null")
                    }

                    //ota状态
                    SifliDFUService.BROADCAST_DFU_STATE -> {
                        val state = intent.getIntExtra(SifliDFUService.EXTRA_DFU_STATE, 0)
                        val stateResult = intent.getIntExtra(SifliDFUService.EXTRA_DFU_STATE_RESULT, 0)

                        binding.llLog.addView(TextView(this@SifliOtaActivity).apply {
                            text = "DFU STATE: state:$state,stateResult:$stateResult"
                        }, 0)

                        Log.d("DFU STATE", " state:$state,stateResult:$stateResult")

                        //EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_DFU_STATE, DFUState(state, stateResult)))

                        sifliOtaState(state, stateResult)
                    }

                    //Progress是0~100的整数
                    //如果是nand升级，资源(zip)和hcpu(bin)的进度是分开的，用PROGRESS_TYPE_IMAGE和PROGRESS_TYPE_FILE区分
                    SifliDFUService.BROADCAST_DFU_PROGRESS -> {
                        val progress = intent.getIntExtra(SifliDFUService.EXTRA_DFU_PROGRESS, 0)
                        val progressType = intent.getIntExtra(SifliDFUService.EXTRA_DFU_PROGRESS_TYPE, 0)

                        binding.llLog.addView(TextView(this@SifliOtaActivity).apply {
                            text = "DFU PROGRESS: progress:$progress,progressType:$progressType"
                        }, 0)

                        if (dfuProgress != progress) {
                            dfuProgress = progress
                            Log.d("DFU PROGRESS", "progress:$progress,progressType:$progressType")
                            //EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_DFU_PROGRESS, DFUProgress(dfuProgress, progressType)))
                            //刷新进度
                            startOrRefSifliTimeOut()
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