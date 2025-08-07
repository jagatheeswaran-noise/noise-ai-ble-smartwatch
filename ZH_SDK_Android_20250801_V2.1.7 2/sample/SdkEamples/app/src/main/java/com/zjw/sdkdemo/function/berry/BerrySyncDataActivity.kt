package com.zjw.sdkdemo.function.berry

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.zh.ble.sa_wear.protobuf.UserProfilesProtos
import com.zh.ble.wear.protobuf.UserProfilesProtos.SELanguageId
import com.zhapp.ble.BleCommonAttributes
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
import com.zhapp.ble.bean.berry.DrinkWaterBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.FitnessDataCallBack
import com.zhapp.ble.callback.RealTimeDataCallBack
import com.zhapp.ble.callback.SportParsingProgressCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityBerrySyncDataBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.DevSportManager
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2024/9/28.
 */
@SuppressLint("SetTextI18n")
class BerrySyncDataActivity : BaseActivity() {

    private val TAG = BerrySyncDataActivity::class.java.simpleName

    private val binding: ActivityBerrySyncDataBinding by lazy { ActivityBerrySyncDataBinding.inflate(layoutInflater) }

    private var isRealTimeDataOpen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s11)
        setContentView(binding.root)
        initDailyData()
        clicks()
    }

    override fun onResume() {
        super.onResume()
        initDeviceCallBack()
        initFitnessSyncCallBack()

    }

    private fun initDailyData() {
        BleConnectState.getInstance().observe(this) { integer ->
            when (integer) {
                BleCommonAttributes.STATE_CONNECTED -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connected_tips)
                }

                BleCommonAttributes.STATE_CONNECTING -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connecting_tips)
                }

                BleCommonAttributes.STATE_DISCONNECTED -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_disconnect_tips)
                }

                BleCommonAttributes.STATE_TIME_OUT -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connect_time_out_tips)
                }
            }
        }

        DeviceSettingLiveData.getInstance().getmSimpleSettingSummaryBean().observe(this) {
            if (it != null) ToastDialog.showToast(this, GsonUtils.toJson(it))
        }

        initSportDataObserve()
    }

    private fun initDeviceCallBack() {
        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean?) {
                ThreadUtils.runOnUiThread {
                    ToastDialog.showToast(this@BerrySyncDataActivity, GsonUtils.toJson(deviceInfoBean))
                }
            }

            override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {
                ThreadUtils.runOnUiThread {
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
                    ToastDialog.showToast(this@BerrySyncDataActivity, tmp)
                }
            }
        }

        CallBackUtils.realTimeDataCallback = object : RealTimeDataCallBack {
            override fun onResult(bean: RealTimeBean) {
                binding.tvRealTimeData.setText(bean.toString())
            }

            override fun onFail() {
            }
        }
    }

    private fun clicks() {
        click(binding.btnSetTime) {
            if (ControlBleTools.getInstance().isConnect) {
                //ControlBleTools.getInstance().setTime(System.currentTimeMillis(),baseSendCmdStateListener)
                ControlBleTools.getInstance().setTime(System.currentTimeMillis(), binding.chbTimeFormat.isChecked, baseSendCmdStateListener)
            }
        }

        click(binding.btnGetDeviceInfo) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
            }
        }

        click(binding.btnGetDeviceBatteryInfo) {
            ControlBleTools.getInstance().getDeviceBattery(baseSendCmdStateListener)
        }

        click(binding.btnGetSimpleSet) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getSimpleSetting(baseSendCmdStateListener)
            }
        }

        click(binding.btnRealTimeOpen) {
            if (ControlBleTools.getInstance().isConnect) {
                if (isRealTimeDataOpen) {
                    ControlBleTools.getInstance().realTimeDataSwitch(isRealTimeDataOpen, baseSendCmdStateListener)
                    isRealTimeDataOpen = false
                } else {
                    ControlBleTools.getInstance().realTimeDataSwitch(isRealTimeDataOpen, baseSendCmdStateListener)
                    isRealTimeDataOpen = true
                }
            }
        }

        click(binding.btnGetFitness) {
            binding.tvDailyProgress.setText("")
            binding.tvDailyResult.setText("")
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDailyHistoryData(baseSendCmdStateListener)
            }
        }

        click(binding.btnGetDevSportStatus) {
            DevSportManager.getInstance().getDevSportStatus()
        }

        click(binding.btnDevConnect) {
            DevSportManager.getInstance().testDevConnected()
        }

        click(binding.btnBleClose) {
            DevSportManager.getInstance().testBleClose()
        }

        click(binding.btnGetSportData) {
            DevSportManager.getInstance().getFitnessSportIdsData()
        }

    }

    private var dailyResult = StringBuffer()

    private fun initFitnessSyncCallBack() {
        CallBackUtils.fitnessDataCallBack = object : FitnessDataCallBack {
            override fun onProgress(progress: Int, total: Int) {
                ThreadUtils.runOnUiThread {
                    Log.e(TAG, "onProgress : progress $progress  total $total")
                    when (progress) {
                        0 -> {
                            dailyResult = StringBuffer()
                            binding.tvDailyResult.setText("")
                        }

                        -1 -> {
                            dailyResult.setLength(0)
                            dailyResult.append("getDailyHistoryData fitnessDataCallBack call Timeout!!!")
                            binding.tvDailyResult.setText(dailyResult.toString())
                            binding.tvDailyProgress.setText("")
                        }

                        else -> {
                            binding.tvDailyProgress.setText("onProgress :progress  $progress  total $total")
                        }
                    }
                    if (total == 0) {
                        dailyResult.setLength(0)
                        dailyResult.append("getDailyHistoryData fitnessDataCallBack No Data")
                        binding.tvDailyResult.setText(dailyResult.toString())
                        return@runOnUiThread
                    }
                }
            }

            override fun onDailyData(data: DailyBean?) {
                Log.e(TAG, "onDailyData : " + data.toString())
                dailyResult.append("\n\n onDailyData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onSleepData(data: SleepBean?) {
                Log.e(TAG, "onSleepData : " + data.toString())
                dailyResult.append("\n\n onSleepData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onContinuousHeartRateData(data: ContinuousHeartRateBean?) {
                Log.e(TAG, "onContinuousHeartRateData : " + data.toString())
                dailyResult.append("\n\n onContinuousHeartRateData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onOfflineHeartRateData(data: OfflineHeartRateBean?) {
                Log.e(TAG, "onOfflineHeartRateData : " + data.toString())
                dailyResult.append("\n\n onOfflineHeartRateData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onContinuousBloodOxygenData(data: ContinuousBloodOxygenBean?) {
                Log.e(TAG, "onContinuousBloodOxygenData : " + data.toString())
                dailyResult.append("\n\n onContinuousBloodOxygenData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onOfflineBloodOxygenData(data: OfflineBloodOxygenBean?) {
                Log.e(TAG, "onOfflineBloodOxygenData : " + data.toString())
                dailyResult.append("\n\n onOfflineBloodOxygenData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onContinuousPressureData(data: ContinuousPressureBean?) {
                Log.e(TAG, "onContinuousPressureData : " + data.toString())
                dailyResult.append("\n\n onOfflineBloodOxygenData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onOfflinePressureData(data: OfflinePressureDataBean?) {
                Log.e(TAG, "onOfflinePressureData : " + data.toString())
                dailyResult.append("\n\n onOfflinePressureData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onContinuousTemperatureData(data: ContinuousTemperatureBean?) {
                Log.e(TAG, "onContinuousTemperatureData : " + data.toString())
                dailyResult.append("\n\n onContinuousTemperatureData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onOfflineTemperatureData(data: OfflineTemperatureDataBean?) {
                Log.e(TAG, "onOfflineTemperatureData : " + data.toString())
                dailyResult.append("\n\n onOfflineTemperatureData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onEffectiveStandingData(data: EffectiveStandingBean?) {
                Log.e(TAG, "onEffectiveStandingData : " + data.toString())
                dailyResult.append("\n\n onEffectiveStandingData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onActivityDurationData(data: ActivityDurationBean?) {
                Log.e(TAG, "onActivityDurationData : " + data.toString())
                dailyResult.append("\n\n onActivityDurationData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onOffEcgData(data: OffEcgDataBean?) {
                Log.e(TAG, "onOffEcgData : " + data.toString())
                dailyResult.append("\n\n onOffEcgData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onExaminationData(data: ExaminationBean?) {
                Log.e(TAG, "onExaminationData : " + data.toString())
                dailyResult.append("\n\n onExaminationData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingTodayActiveTypeData(bean: TodayActiveTypeData?) {
                Log.e(TAG, "onRingTodayActiveTypeData : " + bean.toString())
                dailyResult.append("\n\n onRingTodayActiveTypeData : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingOverallDayMovementData(bean: OverallDayMovementData?) {
                Log.e(TAG, "onRingOverallDayMovementData : " + bean.toString())
                dailyResult.append("\n\n onRingOverallDayMovementData : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingTodayRespiratoryRateData(bean: TodayRespiratoryRateData?) {
                Log.e(TAG, "onRingTodayRespiratoryRateData : " + bean.toString())
                dailyResult.append("\n\n onRingTodayRespiratoryRateData : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingHealthScore(bean: RingHealthScoreBean?) {
                Log.e(TAG, "onRingHealthScore : " + bean.toString())
                dailyResult.append("\n\n onRingHealthScore : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingSleepResult(bean: RingSleepResultBean?) {
                Log.e(TAG, "onRingSleepResult : " + bean.toString())
                dailyResult.append("\n\n onRingSleepResult : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingBatteryData(bean: RingBatteryBean?) {
                Log.e(TAG, "onRingBatteryData : " + bean.toString())
                dailyResult.append("\n\n onRingBatteryData : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onDrinkWaterData(bean: DrinkWaterBean?) {
                Log.e(TAG, "onDrinkWaterData : " + bean.toString())
                dailyResult.append("\n\n onDrinkWaterData : ${bean.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingSleepNAP(list: MutableList<RingSleepNapBean>?) {
                Log.e(TAG, "onRingSleepNAP : " + list.toString())
                dailyResult.append("\n\n onRingSleepNAP : ${list.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingAutoActiveSportData(data: AutoActiveSportBean?) {
                Log.e(TAG, "onRingAutoActiveSportData : " + data.toString())
                dailyResult.append("\n\n onRingAutoActiveSportData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingBodyBatteryData(data: RingBodyBatteryBean?) {
                Log.e(TAG, "onRingBodyBatteryData : " + data.toString())
                dailyResult.append("\n\n onRingBodyBatteryData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

            override fun onRingStressDetectionData(data: RingStressDetectionBean?) {
                Log.e(TAG, "onRingStressDetectionData : " + data.toString())
                dailyResult.append("\n\n onRingStressDetectionData : ${data.toString()}")
                binding.tvDailyResult.setText(dailyResult.toString())
            }

        }

        CallBackUtils.sportParsingProgressCallBack = object : SportParsingProgressCallBack {
            override fun onProgress(progress: Int, total: Int) {
                Log.d(TAG, MyApplication.context.getString(R.string.s243) + "：--->total = " + total + ", progress = " + progress)
                MyApplication.showToast("sport Progress --->total = $total, progress = $progress")
            }

        }

    }

    private fun initSportDataObserve() {

        DevSportManager.getInstance().setListener { data ->
            val old: String = binding.tvSportData.getText().toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(old)) {
                binding.tvSportData.text = "${getString(R.string.s166)}---------》\n$data"
            } else {
                binding.tvSportData.text = "$old\n---------》\n$data"
            }
        }

        binding.tvSportData.setOnLongClickListener {
            ClipboardUtils.copyText(binding.tvSportData.text.trim())
            ToastUtils.showShort("LOG COPY")
            return@setOnLongClickListener true
        }
    }

}