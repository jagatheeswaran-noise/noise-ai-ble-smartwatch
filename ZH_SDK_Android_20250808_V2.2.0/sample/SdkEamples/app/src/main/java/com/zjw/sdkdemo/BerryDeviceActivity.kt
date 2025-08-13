package com.zjw.sdkdemo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.BerryBluetoothService
import com.zhapp.ble.BerryRingService
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.LogFileStatusBean
import com.zhapp.ble.bean.DeviceFileUploadStatusBean
import com.zhapp.ble.bean.RealTimeBean
import com.zhapp.ble.callback.AgpsCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.DeviceLargeFileStatusListener
import com.zhapp.ble.callback.BerryFirmwareLogCallBack
import com.zhapp.ble.callback.RealTimeDataCallBack
import com.zhapp.ble.callback.UnbindDeviceCallBack
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.utils.BleLogger
import com.zhapp.ble.utils.BleUtils
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityBerryBinding
import com.zjw.sdkdemo.function.berry.BerryDialActivity
import com.zjw.sdkdemo.function.berry.BerryOtherSetActivity
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import com.zjw.sdkdemo.utils.AssetUtils
import com.zjw.sdkdemo.utils.ToastDialog
import com.zjw.sdkdemo.utils.customdialog.CustomDialog
import com.zjw.sdkdemo.utils.customdialog.MyDialog
import java.io.File

/**
 * Created by Android on 2024/9/20.
 */
@SuppressLint("SetTextI18n")
class BerryDeviceActivity : BaseActivity() {
    val binding by lazy { ActivityBerryBinding.inflate(layoutInflater) }
    private val TAG: String = BerryDeviceActivity::class.java.simpleName
    var deviceProtocol: String = ""
    var deviceAddress: String = ""
    var deviceName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "SDK demo"
        setContentView(binding.root)
        inits()
        clicks()
    }

    private fun inits() {
        if (intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_ADDRESS) != null && intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_ADDRESS) != "") {
            deviceAddress = intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_ADDRESS) ?: ""
        }
        if (intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_NAME) != null && intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_NAME) != "") {
            deviceName = intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_NAME) ?: ""
        }
        if (intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_PROTOCOL) != null && intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_PROTOCOL) != "") {
            deviceProtocol = intent.getStringExtra(BraceletActivity.EXTRA_DEVICE_PROTOCOL) ?: ""
        }

        initProtocolUI()

        FileUtils.createOrExistsDir(mFilePath)
        binding.tvTip1.setText("操作方法\n\n1.将[OTA文件(.bin)]，放到[${mFilePath}]目录下\n\n2.点击[选择文件]按钮，选择文件\n\n3.点击[发送OTA文件]开始升级")

        initObserve()
    }

    private fun initProtocolUI() {
        if (TextUtils.equals(deviceProtocol, BleCommonAttributes.DEVICE_PROTOCOL_BERRY) ||
            TextUtils.equals(deviceProtocol, BleCommonAttributes.DEVICE_PROTOCOL_BERRY_RING)
        ) {
            binding.cvLog.visibility = View.VISIBLE
            binding.berryG1.visibility = View.VISIBLE
            binding.berryG2.visibility = View.VISIBLE
            binding.berryTest1.visibility = View.GONE
        } else if (TextUtils.equals(deviceProtocol, BleCommonAttributes.DEVICE_PROTOCOL_BERRY_TEST)) {
            binding.cvLog.visibility = View.GONE
            binding.berryG1.visibility = View.GONE
            binding.berryG2.visibility = View.GONE
            binding.berryTest1.visibility = View.VISIBLE
        }
    }

    private fun initObserve() {

        BleConnectState.getInstance().observe(this) { integer ->
            when (integer) {
                BleCommonAttributes.STATE_CONNECTED -> {
                    Log.i(TAG, "BleConnectState = STATE_CONNECTED")
                    val title = getString(R.string.app_name) + " : " + getString(R.string.ble_connected_tips)
                    setTitle(title)
                    if (deviceInfo == null) {
                        binding.btnGetDeviceInfo.callOnClick()
                    }

                    ThreadUtils.runOnUiThread {
                        if (isOtaLoop || isOtaResumeLoop || isOtaLoopResumeLoop) {
                            if (otaLoopNum > 0) {
                                BleLogger.d("ota", "ota 剩余次数：${otaLoopNum - 1}")
                                BleLogger.d("ota", "ota 成功次数：$completeNum")
                                binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                                    text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :ota 剩余次数：${otaLoopNum - 1}"
                                    textSize = 10.0f
                                    setTextColor(Color.RED)
                                }, 0)
                                binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                                    text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :ota 成功次数：$completeNum"
                                    textSize = 10.0f
                                    setTextColor(Color.RED)
                                }, 0)
                            }
                        }
                    }
                    ThreadUtils.runOnUiThreadDelayed({
                        if (isOtaLoop || isOtaResumeLoop || isOtaLoopResumeLoop) {
                            if (ControlBleTools.getInstance().isConnect) {
                                if (isOtaLoop) {
                                    if (isOtaComplete) otaLoopNum -= 1
                                    if (otaLoopNum > 0) {
                                        binding.btnOta.callOnClick()
                                    } else {
                                        isOtaLoop = false
                                    }
                                } else if (isOtaResumeLoop) {
                                    if (isOtaComplete) otaLoopNum -= 1
                                    if (otaLoopNum > 0) {
                                        binding.btnOta.callOnClick()
                                    } else {
                                        isOtaResumeLoop = false
                                    }
                                } else if (isOtaLoopResumeLoop) {
                                    if (isOtaComplete) otaLoopNum -= 1
                                    if (otaLoopNum > 0) {
                                        binding.btnOta.callOnClick()
                                    } else {
                                        isOtaResumeLoop = false
                                    }
                                }
                            }
                        }
                    }, 10 * 1000)

                }

                BleCommonAttributes.STATE_CONNECTING -> {
                    Log.i(TAG, "BleConnectState = STATE_CONNECTING")
                    val title = getString(R.string.app_name) + " : " + getString(R.string.ble_connecting_tips)
                    setTitle(title)
                }

                BleCommonAttributes.STATE_DISCONNECTED -> {
                    Log.i(TAG, "BleConnectState = STATE_DISCONNECTED")
                    title = getString(R.string.app_name) + " : " + getString(R.string.ble_disconnect_tips)
                }

                BleCommonAttributes.STATE_TIME_OUT -> {
                    Log.i(TAG, "BleConnectState = STATE_TIME_OUT")
                    val title = getString(R.string.app_name) + " : " + getString(R.string.ble_connect_time_out_tips)
                    setTitle(title)
                    if (BraceletActivity.deviceAddress != null) {
                        Log.i(
                            TAG,
                            "connect() deviceProtocol = " + BraceletActivity.deviceProtocol + " deviceName = " + BraceletActivity.deviceName + " deviceAddress = " + BraceletActivity.deviceAddress
                        )
                        ControlBleTools.getInstance().connect(BraceletActivity.deviceName, BraceletActivity.deviceAddress, BraceletActivity.deviceProtocol)
                    }
                }
            }
        }


    }

    override fun onResume() {
        super.onResume()
        initSppLogCallBack()
        initDeviceInfoCallBack()
        initAgpsCallBack()
        initLogFileCallBack()
        initUnbindCallBack()
    }

    private fun initSppLogCallBack() {
        BerryBluetoothService.getInstance().setSppDataListener(object : BerryBluetoothService.SppDataListener {
            @SuppressLint("SetTextI18n")
            override fun onWLog(data: ByteArray?) {
                ThreadUtils.runOnUiThread {
                    if (data != null) {
                        binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Spp write --------> : ${BleUtils.bytes2HexString(data)}"
                            textSize = 10.0f
                        }, 0)

                        binding.llTestLog.addView(TextView(this@BerryDeviceActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} write --------> : ${BleUtils.bytes2HexString(data)}"
                            textSize = 10.0f
                        }, 0)
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onRLog(data: ByteArray?) {
                ThreadUtils.runOnUiThread {
                    if (data != null) {
                        binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Spp received --> : ${BleUtils.bytes2HexString(data)}"
                            textSize = 10.0f
                        }, 0)

                        binding.llTestLog.addView(TextView(this@BerryDeviceActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} received --> : ${BleUtils.bytes2HexString(data)}"
                            textSize = 10.0f
                        }, 0)
                    }
                }
            }

            override fun onString(data: String?) {
                ThreadUtils.runOnUiThread {
                    if (data != null) {
                        binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Parsing --> : ${data}"
                            textSize = 10.0f
                            if (data.contains("Exception")) {
                                setTextColor(Color.RED)
                            }
                        }, 0)

                        binding.llTestLog.addView(TextView(this@BerryDeviceActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Parsing --> : ${data}"
                            textSize = 10.0f
                            if (data.contains("Exception")) {
                                setTextColor(Color.RED)
                            }
                        }, 0)
                    }
                }
            }
        })
    }

    private fun initUnbindCallBack() {
        CallBackUtils.unbindDeviceCallBack = object : UnbindDeviceCallBack {
            override fun unbindDeviceSuccess() {
                ControlBleTools.getInstance().disconnect()
                AppUtils.relaunchApp(true)
            }
        }
    }


    private fun clicks() {
        click(binding.llLog) {
            val log = StringBuilder()
            for (i in 0 until binding.llLog.childCount) {
                log.append((binding.llLog.getChildAt(i) as TextView).text.toString().trim()).append("\n")
            }
            ClipboardUtils.copyText(log)
            ToastUtils.showShort("LOG COPY")
        }

        binding.llLog.setOnLongClickListener {
            binding.llLog.removeAllViews()
            ToastUtils.showShort("LOG CLEAR")
            return@setOnLongClickListener true
        }

        click(binding.llTestLog) {
            val log = StringBuilder()
            for (i in 0 until binding.llTestLog.childCount) {
                log.append((binding.llTestLog.getChildAt(i) as TextView).text.toString().trim()).append("\n")
            }
            ClipboardUtils.copyText(log)
            ToastUtils.showShort("LOG COPY")
        }

        binding.llTestLog.setOnLongClickListener {
            binding.llTestLog.removeAllViews()
            ToastUtils.showShort("LOG CLEAR")
            return@setOnLongClickListener true
        }

        click(binding.btnConnect) {
            ControlBleTools.getInstance().setBleStateCallBack { state ->
                Log.i(TAG, "onConnectState state  = $state")
                BleConnectState.getInstance().postValue(state)
            }
            Log.i(TAG, "connect()01 deviceProtocol = $deviceProtocol deviceName = $deviceName deviceAddress = $deviceAddress")
            ControlBleTools.getInstance().connect(deviceName, deviceAddress, deviceProtocol)
        }

        click(binding.btnDisconnect) {
            ControlBleTools.getInstance().disconnect()
        }

        click(binding.btnGetProtocol) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().internalGetProtocol(baseSendCmdStateListener)
            }
        }

        click(binding.btnSetUserId) {
            if (ControlBleTools.getInstance().isConnect) {
                if (TextUtils.isEmpty(binding.etUserId.text.toString().trim())) {
                    ToastUtils.showShort("请输入用户id")
                    return@click
                }
                ControlBleTools.getInstance()
                    .setUserIdByBerryProtocol(binding.etUserId.text.toString().trim(), DeviceUtils.getModel(), DeviceUtils.getSDKVersionName(), baseSendCmdStateListener)
            }
        }

        click(binding.btnBind) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().bindDevice(baseSendCmdStateListener)
            }
        }

        click(binding.btnBindSuc) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().bindDeviceSucByBerryProtocol(true,baseSendCmdStateListener)
            }
        }

        click(binding.btnUnbind) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().unbindDevice(baseSendCmdStateListener)
            }
        }

        click(binding.btnOneKeyBind) {
            if (ControlBleTools.getInstance().isConnect) {
                if (TextUtils.isEmpty(binding.etUserId.text.toString().trim())) {
                    ToastUtils.showShort("请输入用户id")
                    return@click
                }
                ControlBleTools.getInstance()
                    .setUserIdByBerryProtocol(binding.etUserId.text.toString().trim(), DeviceUtils.getModel(), DeviceUtils.getSDKVersionName(), object : SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
                            if (state == SendCmdState.SUCCEED) {
                                ControlBleTools.getInstance().bindDevice(object : SendCmdStateListener() {
                                    override fun onState(state: SendCmdState?) {
                                        if (state == SendCmdState.SUCCEED) {
                                            ThreadUtils.runOnUiThreadDelayed({
                                                ControlBleTools.getInstance().bindDeviceSucByBerryProtocol(true,baseSendCmdStateListener)
                                            }, 1000)
                                        }
                                    }
                                })
                            }
                        }
                    })


            }
        }

        click(binding.btnOtherSet) {
            startActivity(Intent(this, BerryOtherSetActivity::class.java))
        }

        click(binding.btnGetDeviceInfo) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
            }
        }

        click(binding.btnReqAgps) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().requestAgpsState(baseSendCmdStateListener)
            }
        }

        click(binding.btnSendAgps2) {
            FileUtils.deleteAllInDir(getExternalFilesDir("agps"))

            if (deviceInfo == null) {
                ToastUtils.showShort("请先查询设备信息")
                return@click
            }

            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceLargeFileStateByBerry(
                    AssetUtils.getAssetBytes(this, "Agps.brm"),
                    BleCommonAttributes.UPLOAD_BIG_DATA_LTO,
                    deviceInfo!!.equipmentNumber, deviceInfo!!.firmwareVersion, agpsFileStatusListener
                )
            }
        }

        click(binding.btnSendAgps) {

            FileUtils.deleteAllInDir(getExternalFilesDir("agps"))

            if (deviceInfo == null) {
                ToastUtils.showShort("请先查询设备信息")
                return@click
            }

            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceLargeFileStateByBerry(
                    AssetUtils.getAssetBytes(this, "Agps.brm"),
                    BleCommonAttributes.UPLOAD_BIG_DATA_LTO,
                    deviceInfo!!.equipmentNumber, deviceInfo!!.firmwareVersion, agpsFileStatusListener
                )
            }
        }

        binding.rgLog.check(R.id.rb1)
        binding.rgLog.setOnCheckedChangeListener { group, checkedId ->
            logFileStatus = null
        }

        click(binding.btnReqLog) {
            if (deviceInfo == null) {
                ToastUtils.showShort("请先查询设备信息")
                return@click
            }
            var type = 8
            when (binding.rgLog.checkedRadioButtonId) {
                R.id.rb1 -> type = 8
                R.id.rb2 -> type = 9
                R.id.rb3 -> type = 10
                R.id.rb4 -> type = 11
                R.id.rb5 -> type = 12
                R.id.rb6 -> type = 13
            }
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().requestLogFileStatusByBerry(
                    type, "10086",
                    DeviceUtils.getManufacturer() + " - " + DeviceUtils.getModel() + " - " + DeviceUtils.getSDKVersionCode(),
                    AppUtils.getAppVersionName(),
                    deviceInfo!!.equipmentNumber,
                    baseSendCmdStateListener
                )
            }
        }

        click(binding.btnStartLog) {
            if (logFileStatus == null) {
                ToastUtils.showShort("请先查询日志文件状态")
                return@click
            }
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().requestUploadLogFileByBerry(true, logFileStatus!!.type, logFileStatus!!.fileSize, baseSendCmdStateListener)
            }
        }

        click(binding.btnEndLog) {
            if (logFileStatus == null) {
                ToastUtils.showShort("请先查询日志文件状态")
                return@click
            }
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().requestUploadLogFileByBerry(false, logFileStatus!!.type, logFileStatus!!.fileSize, baseSendCmdStateListener)
            }
        }

        click(binding.btnGetOtaFile) {
            val files = FileUtils.listFilesInDir(mFilePath)
            if (files.isNullOrEmpty()) {
                ToastUtils.showShort("$mFilePath 目录文件为空")
                return@click
            }
            showListDialog(files)
        }

        click(binding.btnOta) {
            FileUtils.deleteAllInDir(getExternalFilesDir("agps"))
            if (deviceInfo == null) {
                ToastUtils.showShort("请先查询设备信息")
                return@click
            }
            if (!::file.isInitialized) {
                ToastUtils.showShort("请先选择文件")
                return@click
            }
            if (ControlBleTools.getInstance().isConnect) {
                isOta = true
                ControlBleTools.getInstance().getDeviceLargeFileStateByBerry(
                    FileIOUtils.readFile2BytesByStream(file),
                    BleCommonAttributes.UPLOAD_BIG_DATA_OTA,
                    deviceInfo!!.equipmentNumber, deviceInfo!!.firmwareVersion, otaFileStatusListener
                )
            }
        }

        binding.btnSendAppN.setOnClickListener {
            CallBackUtils.realTimeDataCallback = object : RealTimeDataCallBack {
                override fun onResult(bean: RealTimeBean) {
                    ToastDialog.showToast(this@BerryDeviceActivity,bean.toString())
                }

                override fun onFail() {
                }
            }

            if (ControlBleTools.getInstance().isConnect) {
                isTest = !isTest
                ControlBleTools.getInstance().realTimeDataSwitch(isTest,baseSendCmdStateListener)
            }
        }


        click(binding.btnOta2) {
            if (binding.etOtaLoop.text.toString().trim().isNullOrEmpty()) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            otaLoopNum = binding.etOtaLoop.text.toString().trim().toInt()
            isOtaLoop = true
            isOtaResumeLoop = false
            isOtaLoopResumeLoop = false
            completeNum = 0
            binding.btnOta.callOnClick()
        }

        click(binding.btnOta3) {
            if (binding.etOtaLoop.text.toString().trim().isNullOrEmpty()) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            otaLoopNum = binding.etOtaLoop.text.toString().trim().toInt()
            isOtaResumeLoop = true
            isOtaLoop = false
            isOtaLoopResumeLoop = false
            completeNum = 0
            binding.btnOta.callOnClick()
        }

        click(binding.btnOta4) {
            if (binding.etOtaLoop.text.toString().trim().isNullOrEmpty()) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            otaLoopNum = binding.etOtaLoop.text.toString().trim().toInt()
            isOtaLoopResumeLoop = true
            isOtaResumeLoop = false
            isOtaLoop = false
            completeNum = 0
            binding.btnOta.callOnClick()
        }

        click(binding.btnDail) {
            startActivity(Intent(this, BerryDialActivity::class.java))
        }

        click(binding.btnTestCmd) {
            if (TextUtils.isEmpty(binding.etTestCmd.text.toString().trim())) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            BerryRingService.getInstance().writeTest03Characteristic(BleUtils.hexString2bytes(binding.etTestCmd.text.toString().trim()))
        }

    }

    var isTest = false

    //region agps
    private var deviceInfo: DeviceInfoBean? = null
    private fun initDeviceInfoCallBack() {
        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean?) {
                //ToastDialog.showToast(this@BerryDeviceActivity, GsonUtils.toJson(deviceInfoBean))
                deviceInfo = deviceInfoBean
            }

            override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {
                var state = getString(R.string.s208)
                if (chargeStatus == DeviceInfoCallBack.ChargeStatus.UNKNOWN.state) {
                    state = getString(R.string.s208)
                } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.CHARGING.state) {
                    state = getString(R.string.s209)
                } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.NOT_CHARGING.state) {
                    state = getString(R.string.s210)
                } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.FULL.state) {
                    state = getString(R.string.s211)
                }
                val tmp = """${getString(R.string.s212)}$capacity ${getString(R.string.s213)}$chargeStatus $state"""
                //ToastDialog.showToast(this@BerryDeviceActivity, tmp)
            }

        }
    }

    private val agpsFileStatusListener: AgpsFileStatusListener by lazy { AgpsFileStatusListener() }

    inner class AgpsFileStatusListener : DeviceLargeFileStatusListener {
        override fun onSuccess(statusValue: Int, statusName: String?) {
            if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.READY.state || statusName == "READY") {
                ControlBleTools.getInstance().startUploadBigDataByBerry(
                    BleCommonAttributes.UPLOAD_BIG_DATA_LTO,
                    AssetUtils.getAssetBytes(this@BerryDeviceActivity, "Agps.brm"),
                    uploadBigDataListener
                )
            } else {
                MyApplication.showToast(getString(R.string.s221) + ": " + statusName)
            }
        }

        override fun timeOut() {
            MyApplication.showToast(getString(R.string.s221) + ": timeOut !!! ")
        }

    }

    private val uploadBigDataListener: MyUploadBigDataListener by lazy { MyUploadBigDataListener() }

    private fun initAgpsCallBack() {
        CallBackUtils.agpsCallBack = AgpsCallBack { bean ->
            ToastDialog.showToast(this@BerryDeviceActivity, GsonUtils.toJson(bean))
            if (bean?.isNeed == true) {
                binding.btnSendAgps.visibility = View.VISIBLE
                binding.btnSendAgps2.visibility = View.VISIBLE
            }
        }
    }

    inner class MyUploadBigDataListener : UploadBigDataListener {
        override fun onSuccess() {
            binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :LargeFile --> onSuccess()"
                textSize = 10.0f
                setTextColor(Color.RED)
            }, 0)
            if (isOta) {
                isOta = false
                binding.btnConnect.callOnClick()
            }
        }

        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            ThreadUtils.runOnUiThread {
                val percentage = curPiece * 100 / dataPackTotalPieceLength
                binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                    text =
                        "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :LargeFile --> cur = $curPiece, all = $dataPackTotalPieceLength, progress = $percentage%"
                    textSize = 10.0f
                    setTextColor(Color.RED)
                }, 0)
                if (curPiece == dataPackTotalPieceLength / 2) {
                    if (isOtaResumeLoop) {
                        if (isResumeOta) {
                            isResumeOta = false
                        } else {
                            isResumeOta = true
                            binding.btnConnect.callOnClick()
                        }
                    }
                    if (isOtaLoopResumeLoop && otaLoopNum % 2 == 0) {
                        if (isResumeOta) {
                            isResumeOta = false
                        } else {
                            isResumeOta = true
                            binding.btnConnect.callOnClick()
                        }
                    }
                }
                isOtaComplete = false
                if (percentage == 100) {
                    if (isOtaLoop || isOtaResumeLoop || isOtaLoopResumeLoop) {
                        completeNum += 1
                        if (otaLoopNum > 0) {
                            BleLogger.d("ota", "ota 剩余次数：${otaLoopNum - 1}")
                            BleLogger.d("ota", "ota 成功次数：$completeNum")
                            binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                                text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :ota 剩余次数：${otaLoopNum - 1}"
                                textSize = 10.0f
                                setTextColor(Color.RED)
                            }, 0)
                            binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                                text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :ota 成功次数：$completeNum"
                                textSize = 10.0f
                                setTextColor(Color.RED)
                            }, 0)
                        }
                    }
                    isOtaComplete = true
                }
            }
        }

        override fun onTimeout(msg: String?) {
            binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :LargeFile --> onTimeout --> $msg"
                textSize = 10.0f
                setTextColor(Color.RED)
            }, 0)
        }
    }

    //endregion

    //region 日志
    private var logFileStatus: LogFileStatusBean? = null
    private var isAutoLog = false

    private fun initLogFileCallBack() {
        CallBackUtils.berryFirmwareLogCallBack = object : BerryFirmwareLogCallBack {
            override fun onDeviceRequestAppGetLog() {
                ToastUtils.showShort("on Device Request App Get Log")
                isAutoLog = true
                ControlBleTools.getInstance().requestLogFileStatusByBerry(
                    8, "10086",
                    DeviceUtils.getManufacturer() + " - " + DeviceUtils.getModel() + " - " + DeviceUtils.getSDKVersionCode(),
                    AppUtils.getAppVersionName(),
                    deviceInfo!!.equipmentNumber,
                    null
                )
            }

            override fun onLogFileStatus(bean: LogFileStatusBean?) {
                logFileStatus = bean

                if (isAutoLog) {
                    if (logFileStatus!!.fileSize != 0) {
                        ControlBleTools.getInstance().requestUploadLogFileByBerry(true, logFileStatus!!.type, logFileStatus!!.fileSize, object : SendCmdStateListener() {
                            override fun onState(state: SendCmdState?) {
                                if (state != SendCmdState.SUCCEED) {
                                    isAutoLog = false
                                }
                            }
                        })
                    } else {
                        isAutoLog = false
                    }
                }
            }

            override fun onLogProgress(curSize: Int, allSize: Int) {
                ThreadUtils.runOnUiThread {
                    val percentage = curSize * 100 / allSize
                    binding.llLog.addView(TextView(this@BerryDeviceActivity).apply {
                        text =
                            "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :Receiving logs --> cur = $curSize, all = $allSize, progress = $percentage%"
                        textSize = 10.0f
                        setTextColor(Color.RED)
                    }, 0)
                }
            }

            override fun onLogFileUploadStatus(bean: DeviceFileUploadStatusBean?) {
                if (bean != null) {
                    ToastDialog.showToast(this@BerryDeviceActivity, GsonUtils.toJson(bean))
                    if (bean.isSuccessful) binding.btnEndLog.callOnClick()

                    if (isAutoLog) {
                        if (bean.type == 8) {
                            ControlBleTools.getInstance().requestLogFileStatusByBerry(
                                9, "10086",
                                DeviceUtils.getManufacturer() + " - " + DeviceUtils.getModel() + " - " + DeviceUtils.getSDKVersionCode(),
                                AppUtils.getAppVersionName(),
                                deviceInfo!!.equipmentNumber,
                                object : SendCmdStateListener() {
                                    override fun onState(state: SendCmdState?) {
                                        if (state != SendCmdState.SUCCEED) {
                                            isAutoLog = false
                                        }
                                    }
                                }
                            )
                        } else if (bean.type == 9) {
                            isAutoLog = false
                        }
                    }
                }
            }

            override fun onLogFilePath(type: Int, path: String?) {

            }

        }
    }
    //endregion

    //region ota
    private var otaLoopNum = 0
    private var isOtaLoop = false
    private var isOtaResumeLoop = false
    private var isOtaLoopResumeLoop = false
    private var isResumeOta = false
    private var isOtaComplete = false
    private var completeNum = 0
    private var isOta = false
    private lateinit var file: File
    private val mFilePath = PathUtils.getExternalAppCachePath() + "/otaTest"

    private var dialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        dialog = CustomDialog.builder(this)
            .setContentView(R.layout.dialog_debug_bin_list)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .build()
        for (file in files) {
            if (file.name.endsWith(".bin")) {
                val view = Button(this)
                view.isAllCaps = false
                view.text = file.name
                view.setOnClickListener {
                    this.file = file
                    binding.tvName.text = "${getString(R.string.s513)} ${file.name}"
                    dialog?.dismiss()
                }
                dialog?.findViewById<LinearLayout>(R.id.listLayout)?.addView(view)
            }
        }
        dialog?.show()
    }

    private val otaFileStatusListener: OtaFileStatusListener by lazy { OtaFileStatusListener() }

    inner class OtaFileStatusListener : DeviceLargeFileStatusListener {
        override fun onSuccess(statusValue: Int, statusName: String?) {
            if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.READY.state || statusName == "READY") {
                ControlBleTools.getInstance().startUploadBigDataByBerry(
                    BleCommonAttributes.UPLOAD_BIG_DATA_OTA,
                    FileIOUtils.readFile2BytesByStream(file),
                    uploadBigDataListener
                )
            } else {
                MyApplication.showToast(getString(R.string.s221) + ": " + statusName)
            }
        }

        override fun timeOut() {
            MyApplication.showToast(getString(R.string.s221) + ": timeOut !!! ")
        }

    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        ControlBleTools.getInstance().disconnect()
        CallBackUtils.unbindDeviceCallBack = null
        CallBackUtils.deviceInfoCallBack = null
        CallBackUtils.agpsCallBack = null
        CallBackUtils.berryFirmwareLogCallBack = null
    }

}