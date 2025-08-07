package com.zjw.sdkdemo.function

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.blankj.utilcode.util.GsonUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ActivityDurationBean
import com.zhapp.ble.bean.AutoActiveSportBean
import com.zhapp.ble.bean.ContinuousBloodOxygenBean
import com.zhapp.ble.bean.ContinuousHeartRateBean
import com.zhapp.ble.bean.ContinuousPressureBean
import com.zhapp.ble.bean.ContinuousTemperatureBean
import com.zhapp.ble.bean.DailyBean
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.EffectiveStandingBean
import com.zhapp.ble.bean.ExaminationBean
import com.zhapp.ble.bean.OffEcgDataBean
import com.zhapp.ble.bean.OfflineBloodOxygenBean
import com.zhapp.ble.bean.OfflineHeartRateBean
import com.zhapp.ble.bean.OfflinePressureDataBean
import com.zhapp.ble.bean.OfflineTemperatureDataBean
import com.zhapp.ble.bean.OverallDayMovementData
import com.zhapp.ble.bean.RealTimeBean
import com.zhapp.ble.bean.RingBatteryBean
import com.zhapp.ble.bean.RingBodyBatteryBean
import com.zhapp.ble.bean.RingHealthScoreBean
import com.zhapp.ble.bean.RingSleepNapBean
import com.zhapp.ble.bean.RingSleepResultBean
import com.zhapp.ble.bean.RingStressDetectionBean
import com.zhapp.ble.bean.SleepBean
import com.zhapp.ble.bean.TodayActiveTypeData
import com.zhapp.ble.bean.TodayRespiratoryRateData
import com.zhapp.ble.bean.UserInfo
import com.zhapp.ble.bean.berry.DrinkWaterBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.FitnessDataCallBack
import com.zhapp.ble.callback.RealTimeDataCallBack
import com.zhapp.ble.callback.VerifyUserIdCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityTestSyncBinding
import com.zjw.sdkdemo.function.language.BaseActivity

/**
 * Created by Android on 2023/11/28.
 */
class TestSyncActivity : BaseActivity() {

    private val TAG = TestSyncActivity::class.java.simpleName

    // 是否同步数据中 Whether the data is being synchronized
    private var isSyncing = false


    // 日常数据 daily datadaily data
    private var dailyResult = StringBuffer()

    private var mHandler: Handler? = null

    // 同步中延时其他指令时长
    private val DE_TIME = 3 * 1000L

    private val binding by lazy { ActivityTestSyncBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s525)
        setContentView(binding.root)

        mHandler = Handler(Looper.getMainLooper())

        initDataCallBack()


    }

    private fun initDataCallBack() {

        CallBackUtils.fitnessDataCallBack = object : FitnessDataCallBack {
            override fun onProgress(progress: Int, total: Int) {
                Log.e(TAG, "onProgress : progress $progress  total $total")
                if (progress == 0) {
                    //同步开始  Start syncing
                    isSyncing = true

                    dailyResult = StringBuffer()
                    binding.tvDailyResult.setText("")
                }
                binding.tvDailyProgress.setText("onProgress :progress  $progress  total $total")
                if (progress == total) {
                    //同步完成 Synchronization completed
                    isSyncing = false

                    binding.tvDailyResult.setText(dailyResult.toString())
                    binding.tvDailyProgress.setText("")

                }
            }

            override fun onDailyData(data: DailyBean) {
                Log.e(TAG, "onDailyData : $data")
                dailyResult.append("\n\nonDailyData : $data")
            }

            override fun onSleepData(data: SleepBean) {
                Log.e(TAG, "onSleepData : $data")
                dailyResult.append("\n\nSleepBean : $data")
            }

            override fun onContinuousHeartRateData(data: ContinuousHeartRateBean) {
                Log.e(TAG, "onContinuousHeartRateData : $data")
                dailyResult.append("\n\nContinuousHeartRateBean : $data")
            }

            override fun onOfflineHeartRateData(data: OfflineHeartRateBean) {
                Log.e(TAG, "onOfflineHeartRateData : $data")
                dailyResult.append("\n\nOfflineHeartRateBean : $data")
            }

            override fun onContinuousBloodOxygenData(data: ContinuousBloodOxygenBean) {
                Log.e(TAG, "onContinuousBloodOxygenData : $data")
                dailyResult.append("\n\nContinuousBloodOxygenBean : $data")
            }

            override fun onOfflineBloodOxygenData(data: OfflineBloodOxygenBean) {
                Log.e(TAG, "onOfflineBloodOxygenData : $data")
                dailyResult.append("\n\nOfflineBloodOxygenBean : $data")
            }

            override fun onContinuousPressureData(data: ContinuousPressureBean) {
                Log.e(TAG, "onContinuousPressureData : $data")
                dailyResult.append("\n\nContinuousPressureBean : $data")
            }

            override fun onOfflinePressureData(data: OfflinePressureDataBean) {
                Log.e(TAG, "onOfflinePressureData : $data")
                dailyResult.append("\n\nOfflinePressureDataBean : $data")
            }

            override fun onContinuousTemperatureData(data: ContinuousTemperatureBean) {
                Log.e(TAG, "onContinuousTemperatureData : $data")
                dailyResult.append("\n\nContinuousTemperatureBean : $data")
            }

            override fun onOfflineTemperatureData(data: OfflineTemperatureDataBean) {
                Log.e(TAG, "onOfflineTemperatureData : $data")
                dailyResult.append("\n\nOfflineTemperatureDataBean : $data")
            }

            override fun onEffectiveStandingData(data: EffectiveStandingBean) {
                Log.e(TAG, "onEffectiveStandingData : $data")
                dailyResult.append("\n\nEffectiveStandingBean : $data")
            }

            override fun onActivityDurationData(data: ActivityDurationBean) {
                dailyResult.append("\n\nActivityDurationBean : $data")
            }

            override fun onOffEcgData(data: OffEcgDataBean) {
                dailyResult.append("\n\nOffEcgDataBean : $data")
            }

            override fun onExaminationData(data: ExaminationBean) {
                Log.e(TAG, "ExaminationBean : " + GsonUtils.toJson(data))
                dailyResult.append("\n\nExaminationBean : ${GsonUtils.toJson(data)}")
            }

            override fun onRingTodayActiveTypeData(bean: TodayActiveTypeData) {
                Log.e(TAG, "TodayActivityIndicatorsBean : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nTodayActivityIndicatorsBean : ${GsonUtils.toJson(bean)}")

            }

            override fun onRingOverallDayMovementData(bean: OverallDayMovementData) {
                Log.e(TAG, "onRingOverallDayMovementData : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nTodayActivityIndicatorsBean : ${GsonUtils.toJson(bean)}")
            }

            override fun onRingTodayRespiratoryRateData(bean: TodayRespiratoryRateData) {
                Log.e(TAG, "onRingTodayRespiratoryRateData : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nonRingTodayRespiratoryRateData : ${GsonUtils.toJson(bean)}")
            }

            override fun onRingHealthScore(bean: RingHealthScoreBean) {
                Log.e(TAG, "RingHealthScoreBean : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nRingHealthScoreBean : ${GsonUtils.toJson(bean)}")
            }

            override fun onRingSleepResult(bean: RingSleepResultBean) {
                Log.e(TAG, "RingSleepResultBean : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nRingSleepResultBean : ${GsonUtils.toJson(bean)}")
            }

            override fun onRingBatteryData(bean: RingBatteryBean?) {
                Log.e(TAG, "RingSleepResultBean : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nRingSleepResultBean : ${GsonUtils.toJson(bean)}")
            }

            override fun onDrinkWaterData(bean: DrinkWaterBean?) {
                Log.e(TAG, "DrinkWaterBean : " + GsonUtils.toJson(bean))
                dailyResult.append("\n\nDrinkWaterBean : ${GsonUtils.toJson(bean)}")
            }

            override fun onRingSleepNAP(list: List<RingSleepNapBean>) {
                Log.e(TAG, "RingSleepNapBean : " + GsonUtils.toJson(list))
                dailyResult.append("\n\nRingSleepNapBean : ${GsonUtils.toJson(list)}")
            }

            override fun onRingAutoActiveSportData(data: AutoActiveSportBean) {
                Log.e(TAG, "AutoActiveSportData : " + GsonUtils.toJson(data))
                dailyResult.append("\n\nAutoActiveSportData : ${GsonUtils.toJson(data)}")
            }

            override fun onRingBodyBatteryData(data: RingBodyBatteryBean?) {
                Log.e(TAG, "onRingBodyBatteryData : " + GsonUtils.toJson(data))
                dailyResult.append("\n\nRingBodyBatteryBean : ${GsonUtils.toJson(data)}")
            }

            override fun onRingStressDetectionData(data: RingStressDetectionBean?) {
                Log.e(TAG, "onRingStressDetectionData : " + GsonUtils.toJson(data))
                dailyResult.append("\n\nRingStressDetectionBean : ${GsonUtils.toJson(data)}")
            }
        }

        CallBackUtils.realTimeDataCallback = object : RealTimeDataCallBack {
            override fun onResult(bean: RealTimeBean) {
                binding.tvOtherData.setText(bean.toString() + "\n\n" + binding.tvOtherData.text.toString())
            }

            override fun onFail() {}
        }

        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean) {
                val tmp = getString(R.string.s204) + deviceInfoBean.firmwareVersion + " \n" + getString(R.string.s205) + deviceInfoBean.equipmentNumber +
                        " \n" + getString(R.string.s206) + deviceInfoBean.mac + " \n" + getString(R.string.s207) + deviceInfoBean.serialNumber
                binding.tvOtherData.setText(tmp + "\n\n" + binding.tvOtherData.text.toString())
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
                val tmp = getString(R.string.s212) + capacity + " \n" + getString(R.string.s213) + chargeStatus + " \n " + state
                binding.tvOtherData.setText(tmp + "\n\n" + binding.tvOtherData.text.toString())
            }
        }

        CallBackUtils.verifyUserIdCallBack = object :VerifyUserIdCallBack {
            override fun onVerifyState(state: Int) {
                binding.tvOtherData.setText("onVerifyState:"+state + "\n\n" + binding.tvOtherData.text.toString())
            }
        }

    }

    /**
     * 清除日志  clear log
     */
    fun clearLog(view: View?) {
        binding.tvDailyResult.setText("")
        binding.tvDailyProgress.setText("")
        binding.tvOtherData.setText("")
    }

    /**
     * 获取日常数据  Get daily data
     */
    fun getDailyHistoryData(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            //同步开始  Start syncing
            isSyncing = true
            ControlBleTools.getInstance().getDailyHistoryData(object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> {
                            isSyncing = false
                            MyApplication.showToast(R.string.s221)
                        }
                    }
                }
            })
        }
    }


    //region BUTTON A

    val runnableByRealTimeDataSwitch: Runnable = Runnable {
        realTimeDataSwitch()
    }

    fun btnRealTimeDataSwitch(view: View?) {
        if (isSyncing) {
            mHandler?.removeCallbacks(runnableByRealTimeDataSwitch)
            mHandler?.postDelayed(runnableByRealTimeDataSwitch, DE_TIME)
        } else {
            realTimeDataSwitch()
        }
    }

    private fun realTimeDataSwitch() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().realTimeDataSwitch(true, object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }
    }
    //endregion

    //region BUTTON B

    val runnableByGetDeviceInfo: Runnable = Runnable {
        getDeviceInfo()
    }

    fun btnGetDeviceInfo(view: View?) {
        if (isSyncing) {
            mHandler?.removeCallbacks(runnableByGetDeviceInfo)
            mHandler?.postDelayed(runnableByGetDeviceInfo, DE_TIME)
        } else {
            getDeviceInfo()
        }
    }

    private fun getDeviceInfo() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getDeviceInfo(object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }
    }
    //endregion

    //region BUTTON C

    val runnableBySetUserProfile: Runnable = Runnable {
        setUserProfile()
    }

    fun btnSetUserProfile(view: View?) {
        if (isSyncing) {
            mHandler?.removeCallbacks(runnableBySetUserProfile)
            mHandler?.postDelayed(runnableBySetUserProfile, DE_TIME)
        } else {
            getDeviceInfo()
        }
    }

    private fun setUserProfile() {
        if (ControlBleTools.getInstance().isConnect) {
            val userInfo = UserInfo()
            ControlBleTools.getInstance().setUserProfile(userInfo,object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }
    }
    //endregion

    //region BUTTON D

    val runnableByUserInformation: Runnable = Runnable {
        setUserInformation()
    }

    fun btnSetUserInformation(view: View?) {
        if (isSyncing) {
            mHandler?.removeCallbacks(runnableByUserInformation)
            mHandler?.postDelayed(runnableByUserInformation, DE_TIME)
        } else {
            getDeviceInfo()
        }
    }

    private fun setUserInformation() {
        if (ControlBleTools.getInstance().isConnect) {
            val userInfo = UserInfo()
            ControlBleTools.getInstance().setUserInformation(0,0,userInfo,object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }
    }
    //endregion



    //region BUTTON E

    val runnableByVerifyUserId: Runnable = Runnable {
        verifyUserId()
    }

    fun btnVerifyUserId(view: View?) {
        if (isSyncing) {
            mHandler?.removeCallbacks(runnableByVerifyUserId)
            mHandler?.postDelayed(runnableByVerifyUserId, DE_TIME)
        } else {
            getDeviceInfo()
        }
    }

    private fun verifyUserId() {
        if (ControlBleTools.getInstance().isConnect) {
            val userInfo = UserInfo()
            ControlBleTools.getInstance().verifyUserId("",object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }
    }
    //endregion


    override fun onDestroy() {
        super.onDestroy()
        mHandler?.removeCallbacksAndMessages(null)
    }

}