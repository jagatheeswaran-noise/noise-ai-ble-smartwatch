package com.zjw.sdkdemo.function

import android.os.Bundle
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.TimeUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.EvDataInfoBean
import com.zhapp.ble.bean.TimeBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.EVCarReqCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityEvBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.ToastDialog
import java.util.Calendar

/**
 * Created by Android on 2024/8/30.
 */
class EvActivity : BaseActivity() {

    private val binding by lazy { ActivityEvBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s552)
        setContentView(binding.root)
        initData()

//        btnGetEv.setOnClickListener {
//            if (!ControlBleTools.getInstance().isConnect) {
//                ToastDialog.showToast(this, getString(R.string.s294))
//                return@setOnClickListener
//            }
//            ControlBleTools.getInstance().getEvDataInfo(object : SendCmdStateListener(lifecycle) {
//                override fun onState(state: SendCmdState) {
//                    when (state) {
//                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
//                        else -> MyApplication.showToast(getString(R.string.s221))
//                    }
//                }
//            })
//        }

        binding.btnSetEv.setOnClickListener {
            setEvData()
        }

//        btnGetEvRemind.setOnClickListener {
//            if (!ControlBleTools.getInstance().isConnect) {
//                ToastDialog.showToast(this, getString(R.string.s294))
//                return@setOnClickListener
//            }
//            ControlBleTools.getInstance().getEvRemindStatus(object : SendCmdStateListener(lifecycle) {
//                override fun onState(state: SendCmdState) {
//                    when (state) {
//                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
//                        else -> MyApplication.showToast(getString(R.string.s221))
//                    }
//                }
//            })
//        }

        binding.btnSetEvRemind.setOnClickListener {
            setEvRemind()
        }
    }

    private fun initData() {
        CallBackUtils.evCarReqCallBack = object : EVCarReqCallBack {
            override fun onReqEvData() {
                ThreadUtils.runOnUiThread {
                    ToastDialog.showToast(this@EvActivity, getString(R.string.s570))
                }
            }
        }

        DeviceSettingLiveData.getInstance().getmEvDataInfoBean().observe(this) { bean ->

            ToastDialog.showToast(this, "EvDataInfo：--》" + GsonUtils.toJson(bean))
        }


        DeviceSettingLiveData.getInstance().getmRemindType().observe(this) { remind ->
            ToastDialog.showToast(this, "RemindType：--》" + remind)

//            SettingMenuCallBack.EvRemindType.EV_REMIND_NULL.type
//            SettingMenuCallBack.EvRemindType.EV_TRALIER_STEAL_REMIND.type
//            SettingMenuCallBack.EvRemindType.EV_LOCATION_REMIND.type
//            SettingMenuCallBack.EvRemindType.EV_CRASH_FALL_REMIND.type
//            SettingMenuCallBack.EvRemindType.EV_LOW_POWER_REMIND.type
//            SettingMenuCallBack.EvRemindType.EV_FULL_POWER_REMIND.type
//            SettingMenuCallBack.EvRemindType.EV_FULL_POWER_ERROR.type
        }
    }


    private fun setEvRemind() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastDialog.showToast(this, getString(R.string.s294))
            return
        }
        try {
            val remind = binding.etEvRemind.text.toString().trim()
            val rem = remind.toInt()
            ControlBleTools.getInstance().setEvRemindStatus(rem, object : SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this, getString(R.string.s238))
        }
    }

    private fun setEvData() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastDialog.showToast(this, getString(R.string.s294))
            return
        }
        try {

            val evDataInfo = EvDataInfoBean()
            evDataInfo.refreshInterval = 10 // 刷新间隔 刷新频率（目前由设备强制10分钟） Refresh Interval Refresh frequency (currently forced by the device to 10 minutes)

            //see SettingMenuCallBack.EvConnectType
            evDataInfo.dataConnectType = binding.etDataConnectType.text.toString().trim().toInt()

            // 如果连接成功 If the connection is successful
            // evDataInfo.dataConnectType = 2
            // evDataInfo.applicationData = data

            // 如果连接失败  If the connection fails
            // evDataInfo.dataConnectType = 4
            // evDataInfo.dataFailType = @see SettingMenuCallBack.DataFailType
            val failStatus = binding.etFailStatus.text.toString().trim()
            if(failStatus.isNotEmpty()){
                evDataInfo.dataFailType = failStatus.toInt()
            }

            val app = EvDataInfoBean.ApplicationData()
            app.time = TimeBean().apply {
                val data = TimeUtils.string2Date(binding.etTime.text.toString().trim(), "yyyy-MM-dd HH:mm:ss")
                val cal = Calendar.getInstance()
                cal.time = data
                year = cal.get(Calendar.YEAR) // 年
                month = cal.get(Calendar.MONTH) + 1 // 月
                day = cal.get(Calendar.DAY_OF_MONTH) // 日
                hour = cal.get(Calendar.HOUR_OF_DAY) // 时
                minute = cal.get(Calendar.MINUTE) // 分
                second = cal.get(Calendar.SECOND) // 秒
            }
            //see SettingMenuCallBack.EvCarStatus
            app.carStatus = binding.etCarStatus.text.toString().trim().toInt()
            val chargeStatus = EvDataInfoBean.ApplicationData.ChargeStatus()
            //see SettingMenuCallBack.EvChargeStatus
            chargeStatus.status = binding.etChargeStatus.text.toString().trim().toInt()
            chargeStatus.currentCharge = binding.etCurrentCharge.text.toString().trim().toInt()
            chargeStatus.fullChargeTime = binding.etFullChargeTime.text.toString().trim().toInt()
            val remainingMile = EvDataInfoBean.ApplicationData.RemainingMile()
            remainingMile.ecoMode = binding.etEcoMode.text.toString().trim().toInt()
            remainingMile.sportMode = binding.etSportMode.text.toString().trim().toInt()
            remainingMile.powerMode = binding.etPowerMode.text.toString().trim().toInt()
            val tireGauge = EvDataInfoBean.ApplicationData.TireGauge()
            tireGauge.frontWheel = binding.etFrontWheel.text.toString().trim().toInt()
            tireGauge.rearWheel = binding.etRearWheel.text.toString().trim().toInt()
            val display = EvDataInfoBean.ApplicationData.Display()
            display.totalDistance = binding.etTotalDistance.text.toString().trim().toInt()
            display.totalCarbonEmission = binding.etTotalCarbonEmission.text.toString().trim().toDouble()
            display.curDayTotalDistance = binding.etCurDayTotalDistance.text.toString().trim().toInt()
            display.curDayAvgSpeed = binding.etCurDayAvgSpeed.text.toString().trim().toDouble()
            display.curDayTotalDrivingTime = binding.etCurDayTotalDrivingTime.text.toString().trim().toInt()
            app.chargeStatus = chargeStatus
            app.remainingMile = remainingMile
            app.tireGauge = tireGauge
            app.display = display
            evDataInfo.applicationData = app

            ControlBleTools.getInstance().setEvDataInfo(evDataInfo, object : SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this, getString(R.string.s238))
        }
    }


}