package com.zjw.sdkdemo.function.sync

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ActivityDurationBean
import com.zhapp.ble.bean.AutoActiveSportBean
import com.zhapp.ble.bean.ContinuousBloodOxygenBean
import com.zhapp.ble.bean.ContinuousHeartRateBean
import com.zhapp.ble.bean.ContinuousPressureBean
import com.zhapp.ble.bean.ContinuousTemperatureBean
import com.zhapp.ble.bean.DailyBean
import com.zhapp.ble.bean.EffectiveStandingBean
import com.zhapp.ble.bean.ExaminationBean
import com.zhapp.ble.bean.FitnessRTCOffsetBean
import com.zhapp.ble.bean.OffEcgDataBean
import com.zhapp.ble.bean.OfflineBloodOxygenBean
import com.zhapp.ble.bean.OfflineHeartRateBean
import com.zhapp.ble.bean.OfflinePressureDataBean
import com.zhapp.ble.bean.OfflineTemperatureDataBean
import com.zhapp.ble.bean.OverallDayMovementData
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
import com.zhapp.ble.callback.FitnessDataCallBack
import com.zhapp.ble.callback.FitnessRTCDataCallBack
import com.zhapp.ble.callback.FitnessRTCOffsetCallBack
import com.zhapp.ble.manager.FitnessRTCManager
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivitySyncFitnessBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import java.util.ArrayList

/**
 * Created by Android on 2024/4/13.
 */
class SyncRingRTCFitnessActivity : BaseActivity() {

    private val TAG = "SyncFitnessActivity"

    //结果 Result
    private var dailyResult = StringBuffer()

    //RTC时间偏移数据  RTC time offset data
    private var rtcOffsets: ArrayList<FitnessRTCOffsetBean> = arrayListOf()

    private val binding by lazy { ActivitySyncFitnessBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s550)
        setContentView(binding.root)

        inits()

        //COPY
        if (binding.tvDailyResult != null) {
            binding.tvDailyResult.setOnLongClickListener(View.OnLongClickListener {
                ClipboardUtils.copyText(binding.tvDailyResult.getText().toString().trim { it <= ' ' })
                ToastUtils.showShort("copy complete")
                false
            })
        }
    }

    fun getDailyHistoryData(view: View?) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().setTime(System.currentTimeMillis(),null)
            ControlBleTools.getInstance().getDailyHistoryData(null)
        }
    }

    private fun inits() {
        /**
         * 连接状态  connect state
         */
        BleConnectState.getInstance().observe(this, Observer<Int?> { integer ->
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
        })

        /**
         * RTC时间偏移数据回调  RTC time offset data callback
         */
        CallBackUtils.fitnessRTCOffsetCallBack = object : FitnessRTCOffsetCallBack {
            override fun onOffsetList(offsets: ArrayList<FitnessRTCOffsetBean>?) {
                if (offsets != null) {
                    rtcOffsets.clear()
                    rtcOffsets.addAll(offsets)

                    dailyResult.append("\n\nFitnessRTCOffset : ${GsonUtils.toJson(offsets)}")

                    FitnessRTCManager.initFitnessRTCDataCallBack(rtcOffsets, MyFitnessRTCDataCallBack())
                }
            }
        }


        /**
         * 日常数据回调   daily data callback
         */
        CallBackUtils.fitnessDataCallBack = object : FitnessDataCallBack {
            override fun onProgress(progress: Int, total: Int) {
                Log.e(TAG, "onProgress : progress $progress  total $total")

                /****** fill Progress ******/
                FitnessRTCManager.fillProgress(progress, total)

                if (progress == 0) {
                    dailyResult = StringBuffer()
                    binding.tvDailyResult.text = ""
                    rtcOffsets.clear()
                }
                binding.tvDailyProgress.text = "onProgress :progress  $progress  total $total"
                if (progress == total) {
                    binding.tvDailyProgress.text = ""
                    binding.tvDailyResult.text = dailyResult.toString()
                }
            }

            override fun onDailyData(data: DailyBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onDailyData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nonDailyData : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onSleepData(data: SleepBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onSleepData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nSleepBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)

            }

            override fun onContinuousHeartRateData(data: ContinuousHeartRateBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onContinuousHeartRateData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nContinuousHeartRateBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onOfflineHeartRateData(data: OfflineHeartRateBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onOfflineHeartRateData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nOfflineHeartRateBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onContinuousBloodOxygenData(data: ContinuousBloodOxygenBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onContinuousBloodOxygenData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nContinuousBloodOxygenBean : $data")
                    return
                }
                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onOfflineBloodOxygenData(data: OfflineBloodOxygenBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onOfflineBloodOxygenData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nOfflineBloodOxygenBean : $data")
                    return
                }
                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onContinuousPressureData(data: ContinuousPressureBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onContinuousPressureData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nContinuousPressureBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onOfflinePressureData(data: OfflinePressureDataBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onOfflinePressureData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nOfflinePressureDataBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onContinuousTemperatureData(data: ContinuousTemperatureBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onContinuousTemperatureData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nContinuousTemperatureBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onOfflineTemperatureData(data: OfflineTemperatureDataBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onOfflineTemperatureData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nOfflineTemperatureDataBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onEffectiveStandingData(data: EffectiveStandingBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onEffectiveStandingData : $data")
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nEffectiveStandingBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onActivityDurationData(data: ActivityDurationBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onActivityDurationData : " + GsonUtils.toJson(data))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nActivityDurationBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onOffEcgData(data: OffEcgDataBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onOffEcgData : " + GsonUtils.toJson(data))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nOffEcgDataBean : $data")
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onExaminationData(data: ExaminationBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "ExaminationBean : " + GsonUtils.toJson(data))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nExaminationBean : ${GsonUtils.toJson(data)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onRingTodayActiveTypeData(bean: TodayActiveTypeData) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "TodayActivityIndicatorsBean : " + GsonUtils.toJson(bean))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nTodayActivityIndicatorsBean : ${GsonUtils.toJson(bean)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(bean)
            }

            override fun onRingOverallDayMovementData(bean: OverallDayMovementData) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onRingOverallDayMovementData : " + GsonUtils.toJson(bean))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nOverallDayMovementData : ${GsonUtils.toJson(bean)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(bean)
            }

            override fun onRingTodayRespiratoryRateData(bean: TodayRespiratoryRateData) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onRingTodayRespiratoryRateData : " + GsonUtils.toJson(bean))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nTodayRespiratoryRateData : ${GsonUtils.toJson(bean)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(bean)
            }

            override fun onRingHealthScore(bean: RingHealthScoreBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "RingHealthScoreBean : " + GsonUtils.toJson(bean))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nRingHealthScoreBean : ${GsonUtils.toJson(bean)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(bean)
            }

            override fun onRingSleepResult(bean: RingSleepResultBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "RingSleepResultBean : " + GsonUtils.toJson(bean))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nRingSleepResultBean : ${GsonUtils.toJson(bean)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(bean)
            }

            override fun onRingBatteryData(bean: RingBatteryBean?) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onRingBatteryData : " + GsonUtils.toJson(bean))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nonRingBatteryData : ${GsonUtils.toJson(bean)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                //FitnessRTCManager.fillPendingData(bean)
            }

            override fun onDrinkWaterData(bean: DrinkWaterBean?) {

            }

            override fun onRingSleepNAP(list: List<RingSleepNapBean>) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "RingSleepNapBean : " + GsonUtils.toJson(list))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nRingSleepNapBean : ${GsonUtils.toJson(list)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(list)
            }

            override fun onRingAutoActiveSportData(data: AutoActiveSportBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "AutoActiveSportData : " + GsonUtils.toJson(data))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nAutoActiveSportData : ${GsonUtils.toJson(data)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onRingBodyBatteryData(data: RingBodyBatteryBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onRingBodyBatteryData : " + GsonUtils.toJson(data))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nRingBodyBatteryBean : ${GsonUtils.toJson(data)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }

            override fun onRingStressDetectionData(data: RingStressDetectionBean) {
                if (rtcOffsets.isEmpty()) {
                    Log.e(TAG, "onRingStressDetectionData : " + GsonUtils.toJson(data))
                    //之前的逻辑  previous logic
                    dailyResult.append("\n\nRingStressDetectionBean : ${GsonUtils.toJson(data)}".trimIndent())
                    return
                }

                /****** fill Pending Data ******/
                FitnessRTCManager.fillPendingData(data)
            }
        }
    }


    public inner class MyFitnessRTCDataCallBack : FitnessRTCDataCallBack {
        /**
         * @param offsetState
         * 偏移状态 ： 1 无需偏移  ,2 偏移  ,3 偏移值超出合理时间范围（1H - 1Y）
         * Offset status: 1 No offset required, 2 Offset, 3 Offset value exceeds reasonable time range (1H - 1Y)
         * @param data
         * 数据 data
         */
        override fun onDailyData(offsetState: Int, data: MutableList<DailyBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onDailyData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState, onDailyData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onSleepData(offsetState: Int, data: MutableList<SleepBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onSleepData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onSleepData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onContinuousHeartRateData(offsetState: Int, data: MutableList<ContinuousHeartRateBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onContinuousHeartRateData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onContinuousHeartRateData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onOfflineHeartRateData(offsetState: Int, data: MutableList<OfflineHeartRateBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onOfflineHeartRateData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onOfflineHeartRateData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onContinuousBloodOxygenData(offsetState: Int, data: MutableList<ContinuousBloodOxygenBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onContinuousBloodOxygenData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onContinuousBloodOxygenData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onOfflineBloodOxygenData(offsetState: Int, data: MutableList<OfflineBloodOxygenBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onOfflineBloodOxygenData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onOfflineBloodOxygenData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onContinuousPressureData(offsetState: Int, data: MutableList<ContinuousPressureBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onContinuousPressureData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onContinuousPressureData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onOfflinePressureData(offsetState: Int, data: MutableList<OfflinePressureDataBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onOfflinePressureData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onOfflinePressureData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onContinuousTemperatureData(offsetState: Int, data: MutableList<ContinuousTemperatureBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onContinuousTemperatureData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onContinuousTemperatureData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onOfflineTemperatureData(offsetState: Int, data: MutableList<OfflineTemperatureDataBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onOfflineTemperatureData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onOfflineTemperatureData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onEffectiveStandingData(offsetState: Int, data: MutableList<EffectiveStandingBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onEffectiveStandingData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onEffectiveStandingData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onActivityDurationData(offsetState: Int, data: MutableList<ActivityDurationBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onActivityDurationData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onActivityDurationData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onOffEcgData(offsetState: Int, data: MutableList<OffEcgDataBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onOffEcgData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onOffEcgData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onExaminationData(offsetState: Int, data: MutableList<ExaminationBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onExaminationData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onExaminationData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingTodayActiveTypeData(offsetState: Int, data: MutableList<TodayActiveTypeData>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingTodayActiveTypeData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingTodayActiveTypeData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingOverallDayMovementData(offsetState: Int, data: MutableList<OverallDayMovementData>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingOverallDayMovementData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingOverallDayMovementData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingTodayRespiratoryRateData(offsetState: Int, data: MutableList<TodayRespiratoryRateData>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingTodayRespiratoryRateData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingTodayRespiratoryRateData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingHealthScore(offsetState: Int, data: MutableList<RingHealthScoreBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingHealthScore : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingHealthScore : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingSleepResult(offsetState: Int, data: MutableList<RingSleepResultBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingSleepResult : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingSleepResult : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingSleepNAP(offsetState: Int, data: MutableList<RingSleepNapBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingSleepNAP : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingSleepNAP : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingAutoActiveSportData(offsetState: Int, data: MutableList<AutoActiveSportBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingAutoActiveSportData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingAutoActiveSportData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingBodyBatteryData(offsetState: Int, data: MutableList<RingBodyBatteryBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingBodyBatteryData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingBodyBatteryData : ${GsonUtils.toJson(item)}")
            }
        }

        override fun onRingStressDetectionData(offsetState: Int, data: MutableList<RingStressDetectionBean>?) {
            if (data == null) return
            for (item in data) {
                Log.e(TAG, "offsetState :$offsetState ,onRingStressDetectionData : ${GsonUtils.toJson(item)}")
                dailyResult.append("\n\noffsetState：$offsetState,onRingStressDetectionData : ${GsonUtils.toJson(item)}")
            }
        }

    }


}