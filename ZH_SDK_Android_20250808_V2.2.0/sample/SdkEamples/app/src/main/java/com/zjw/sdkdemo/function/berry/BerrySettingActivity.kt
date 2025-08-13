package com.zjw.sdkdemo.function.berry

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ClassicBluetoothStateBean
import com.zhapp.ble.bean.ClockInfoBean
import com.zhapp.ble.bean.ClockInfoBean.DataBean
import com.zhapp.ble.bean.CommonReminderBean
import com.zhapp.ble.bean.ContinuousBloodOxygenSettingsBean
import com.zhapp.ble.bean.DialStyleBean
import com.zhapp.ble.bean.DoNotDisturbModeBean
import com.zhapp.ble.bean.EventInfoBean
import com.zhapp.ble.bean.FindWearSettingsBean
import com.zhapp.ble.bean.HeartRateMonitorBean
import com.zhapp.ble.bean.NotificationSettingsBean
import com.zhapp.ble.bean.PhysiologicalCycleBean
import com.zhapp.ble.bean.PhysiologicalCycleBean.DateBean
import com.zhapp.ble.bean.PressureModeBean
import com.zhapp.ble.bean.SchedulerBean
import com.zhapp.ble.bean.SchedulerBean.AlertBean
import com.zhapp.ble.bean.SchedulerBean.HabitBean
import com.zhapp.ble.bean.SchedulerBean.ReminderBean
import com.zhapp.ble.bean.SchoolBean
import com.zhapp.ble.bean.ScreenDisplayBean
import com.zhapp.ble.bean.ScreenSettingBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.bean.SleepModeBean
import com.zhapp.ble.bean.SleepReminder
import com.zhapp.ble.bean.StockInfoBean
import com.zhapp.ble.bean.StockSymbolBean
import com.zhapp.ble.bean.TimeBean
import com.zhapp.ble.bean.WristScreenBean
import com.zhapp.ble.bean.berry.MorningPostBean
import com.zhapp.ble.bean.berry.RecordingCmdBean
import com.zhapp.ble.bean.berry.VaultInfoBean
import com.zhapp.ble.bean.berry.VaultSimpleBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.MorningPostCallBack
import com.zhapp.ble.callback.PhysiologicalCycleCallBack
import com.zhapp.ble.callback.RecordingCallBack
import com.zhapp.ble.callback.StockCallBack
import com.zhapp.ble.callback.VaultCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.utils.BleUtils
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityBerrySettingBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2024/10/25.
 */
class BerrySettingActivity : BaseActivity() {
    private var TAG = "BerrySettingActivity"
    private val binding: ActivityBerrySettingBinding by lazy { ActivityBerrySettingBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s17)
        setContentView(binding.root)
        initListener()
        initCallBack()
    }

    private fun initListener() {

        click(binding.btnGetHr) {
            ControlBleTools.getInstance().getHeartRateMonitor(baseSendCmdStateListener)
        }

        click(binding.btnSetHr) {
            sendHeartRate()
        }

        click(binding.btnGetSleepMode) {
            ControlBleTools.getInstance().getSleepMode(baseSendCmdStateListener)
        }

        click(binding.btnSetSleepMode) {
            sendSleepMode()
        }

        click(binding.btnGetPressure) {
            ControlBleTools.getInstance().getPressureMode(baseSendCmdStateListener)
        }

        click(binding.btnSetPressure) {
            setPressureMode()
        }

        click(binding.btnSetV) {
            sendVibration()
        }

        click(binding.btnGetV) {
            ControlBleTools.getInstance().getDeviceVibrationIntensity(baseSendCmdStateListener)
        }

        click(binding.btnSetVD) {
            sendVibrationDuration()
        }

        click(binding.btnGetVD) {
            ControlBleTools.getInstance().getDeviceVibrationDuration(baseSendCmdStateListener)
        }

        click(binding.btnSetP) {
            sendPowerSaving()
        }

        click(binding.btnGetP) {
            ControlBleTools.getInstance().getPowerSaving(baseSendCmdStateListener)
        }

        click(binding.btnSetO) {
            sendOverlayScreen()
        }

        click(binding.btnGetO) {
            ControlBleTools.getInstance().getOverlayScreen(baseSendCmdStateListener)
        }

        click(binding.btnSetR) {
            sendRapidEyeMovement()
        }

        click(binding.btnGetR) {
            ControlBleTools.getInstance().getRapidEyeMovement(baseSendCmdStateListener)
        }

        click(binding.btnSetW) {
            sendWristScreen()
        }

        click(binding.btnGetW) {
            ControlBleTools.getInstance().getWristScreen(baseSendCmdStateListener)
        }

        click(binding.btnSetN) {
            sendDoNotDisturbMode()
        }

        click(binding.btnGetN) {
            ControlBleTools.getInstance().getDoNotDisturbMode(baseSendCmdStateListener)
        }

        click(binding.btnSetD) {
            sendScreenDisplay()
        }

        click(binding.btnGetD) {
            ControlBleTools.getInstance().getScreenDisplay(baseSendCmdStateListener)
        }

        click(binding.btnSetS) {
            sendScreenSetting()
        }

        click(binding.btnGetS) {
            ControlBleTools.getInstance().getScreenSetting(baseSendCmdStateListener)
        }

        click(binding.btnSetSR) {
            sendSedentaryReminder()
        }

        click(binding.btnGetSR) {
            ControlBleTools.getInstance().getSedentaryReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetRD) {
            sendDrinkWaterReminder()
        }

        click(binding.btnGetRD) {
            ControlBleTools.getInstance().getDrinkWaterReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetRM) {
            sendMedicationReminder()
        }

        click(binding.btnGetRM) {
            ControlBleTools.getInstance().getMedicationReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetRH) {
            sendHaveMealsReminder()
        }

        click(binding.btnGetRH) {
            ControlBleTools.getInstance().getHaveMealsReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetRW) {
            sendWashHandReminder()
        }

        click(binding.btnGetRW) {
            ControlBleTools.getInstance().getWashHandReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetSleep) {
            sendSleepReminder()
        }

        click(binding.btnGetSleep) {
            ControlBleTools.getInstance().getSleepReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetEvent) {
            sendEventInfo()
        }

        click(binding.btnGetEvent) {
            ControlBleTools.getInstance().getEventInfoList(baseSendCmdStateListener)
        }

        click(binding.btnSetC) {
            sendClockInfo()
        }

        click(binding.btnGetC) {
            ControlBleTools.getInstance().getClockInfoList(baseSendCmdStateListener)
        }

        click(binding.btnSetBtSwitch) {
            setClassicBluetoothState()
        }

        click(binding.btnGetBtSwitch) {
            ControlBleTools.getInstance().getClassicBluetoothState(baseSendCmdStateListener)
        }

        click(binding.btnSetSchoolMode) {
            sendSchoolMode()
        }

        click(binding.btnGetSchoolMode) {
            ControlBleTools.getInstance().getSchoolMode(baseSendCmdStateListener)
        }

        click(binding.btnSetScheduler) {
            sendScheduler()
        }

        click(binding.btnGetScheduler) {
            ControlBleTools.getInstance().getScheduleReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetPh) {
            sendPhysiologicalCycle()
        }

        click(binding.btnGetPh) {
            ControlBleTools.getInstance().getPhysiologicalCycle(baseSendCmdStateListener)
        }

        click(binding.btnSyncStock) {
            ControlBleTools.getInstance().syncStockInfoList(stockInfos, baseSendCmdStateListener)
        }

        click(binding.btnSetStockOrder) {
            val stockSymbolBeans: MutableList<StockSymbolBean> = mutableListOf()
            for (i in stockInfos.indices) {
                val symbolBean = StockSymbolBean()
                symbolBean.symbol = stockInfos[i].symbol
                symbolBean.isWidget = false
                symbolBean.order = i
                stockSymbolBeans.add(symbolBean)
            }
            ControlBleTools.getInstance().setStockSymbolOrder(stockSymbolBeans, baseSendCmdStateListener)
        }

        click(binding.btnGetStockOrder) {
            ControlBleTools.getInstance().getStockSymbolList(baseSendCmdStateListener)
        }

        click(binding.btnDelStock) {
            ControlBleTools.getInstance().deleteStockBySymbol(stockInfos.get(0).symbol, baseSendCmdStateListener)
        }

        click(binding.btnSetBoMode) {
            ControlBleTools.getInstance().getContinuousBloodOxygenSettings(baseSendCmdStateListener)
        }

        click(binding.btnGetBoMode) {
            setContinuousBloodOxygenSettings()
        }

        click(binding.btnSetFw) {
            setFindWearSetting()
        }

        click(binding.btnGetFw) {
            ControlBleTools.getInstance().getFindWearSettings(baseSendCmdStateListener)
        }

        click(binding.btnSetNoticeSettings) {
            setNoticeSettings()
        }

        click(binding.btnGetNoticeSettings) {
            ControlBleTools.getInstance().getNotificationSettings(baseSendCmdStateListener)
        }

        click(binding.btnGetCustomizeSet) {
            ControlBleTools.getInstance().getCustomizeSet(baseSendCmdStateListener)
        }

        click(binding.btnSetCustomizeSet) {
            setCustomizeSet()
        }

        click(binding.btnSetAutoSport) {
            sendAutoSport()
        }

        click(binding.btnGetAutoSport) {
            ControlBleTools.getInstance().getMotionRecognition(baseSendCmdStateListener)
        }

        click(binding.btnSetMorningPost) {
            setMorningPost()
        }

        click(binding.btnSetVault) {
            sendVault()
        }

        click(binding.btnGetSimpleVault) {
            ControlBleTools.getInstance().getSimpleVaultInfoList(baseSendCmdStateListener)
        }

        click(binding.btnGetVaultInfo) {
            getVaultInfoById()
        }

        click(binding.btnDelVaultInfo) {
            delVaultInfoByIds()
        }

        click(binding.btnAiCmd) {
            sendRecordingCmd()
        }
    }

    private fun showTips(msg: String) {
        runOnUiThread { Toast.makeText(this@BerrySettingActivity, msg, Toast.LENGTH_LONG).show() }
    }

    //region 心率检测
    private fun sendHeartRate() {
        val bean = HeartRateMonitorBean()
        try {
            val warningValue: Int = binding.etHeartRate.getText().toString().trim { it <= ' ' }.toInt()
            val sportWarningValue: Int = binding.etHeartSport.getText().toString().trim { it <= ' ' }.toInt()
            bean.mode = if (binding.cbHrModeSwitch.isChecked()) 0 else 1
            bean.isWarning = binding.cbHrIsWarningSwitch.isChecked()
            bean.warningValue = warningValue
            bean.isSportWarning = binding.cbHrIsSport.isChecked()
            bean.sportWarningValue = sportWarningValue
            bean.frequency = 0 //设备内部写死5
            bean.continuousHeartRateMode = if (binding.cbHeartRateMode.isChecked()) 1 else 0
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setHeartRateMonitor(bean, baseSendCmdStateListener)
    }

    private fun initHeartRateMonitorData() {
        DeviceSettingLiveData.getInstance().getmHeartRateMonitor().observe(this, Observer<HeartRateMonitorBean?> { bean ->
            if (bean == null) return@Observer
            Log.i(TAG, "HeartRateMonitorBean == $bean")
            ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s306)}：$bean")
        })
    }
    //endregion

    //region 睡眠模式
    private fun sendSleepMode() {
        val sleepModeBean = SleepModeBean()
        try {
            val sh: Int = binding.etSleepSH.getText().toString().trim { it <= ' ' }.toInt()
            val sm: Int = binding.etSleepSM.getText().toString().trim { it <= ' ' }.toInt()
            val eh: Int = binding.etSleepEH.getText().toString().trim { it <= ' ' }.toInt()
            val em: Int = binding.etSleepEM.getText().toString().trim { it <= ' ' }.toInt()
            sleepModeBean.sleepModeSwitch = binding.cbSleepSwitch.isChecked()
            sleepModeBean.rapidEyeMovement = binding.cbSleepREM.isChecked()
            sleepModeBean.smartSwitch = binding.cbSleepSmartSwitch.isChecked()
            sleepModeBean.minimizeScreen = binding.cbSleepMinimizeScreen.isChecked()
            sleepModeBean.startTime = SettingTimeBean(sh, sm)
            sleepModeBean.endTime = SettingTimeBean(eh, em)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setSleepMode(sleepModeBean, baseSendCmdStateListener)
    }

    private fun initSleepModeObserve() {
        DeviceSettingLiveData.getInstance().getmSleepMode().observe(this, Observer<SleepModeBean?> { bean ->
            if (bean == null) return@Observer
            Log.i(TAG, "SleepModeBean == $bean")
            ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s614)}：$bean")
        })
    }
    //endregion

    //region 压力模式选择
    private fun initPressureModeData() {
        DeviceSettingLiveData.getInstance().getmPressureMode().observe(this) { pressureModeBean ->
            if (pressureModeBean != null) {
                Log.i(TAG, "PressureModeBean == $pressureModeBean")
                ToastDialog.showToast(
                    this@BerrySettingActivity, "${getString(R.string.s329)}：$pressureModeBean".trimIndent()
                )
                binding.cbPrModeSwitch.isChecked = pressureModeBean.pressureMode
                binding.cbPrReminderSwitch.isChecked = pressureModeBean.relaxationReminder
            }
        }
    }

    private fun setPressureMode() {
        val bean = PressureModeBean()
        try {
            bean.pressureMode = binding.cbPrModeSwitch.isChecked
            bean.relaxationReminder = binding.cbPrReminderSwitch.isChecked
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setPressureMode(bean, baseSendCmdStateListener)
    }

    //endregion

    //region 震动等级
    private fun sendVibration() {
        try {
            val intensity = binding.etVibration.text.toString().trim { it <= ' ' }.toInt()
            ControlBleTools.getInstance().setDeviceVibrationIntensity(intensity, baseSendCmdStateListener)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
    }

    private fun initVibrationData() {
        DeviceSettingLiveData.getInstance().getmVibrationMode().observe(this, object : Observer<Int?> {
            override fun onChanged(intensity: Int?) {
                if (intensity == null) return
                Log.i(TAG, "VibrationMode == $intensity")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s142)}：$intensity")
            }
        })
    }
    //endregion

    //region 震动时长
    private fun sendVibrationDuration() {
        try {
            val duration = binding.etVibrationD.text.toString().trim { it <= ' ' }.toInt()
            ControlBleTools.getInstance().setDeviceVibrationDuration(duration, baseSendCmdStateListener)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
    }

    private fun initVibrationDurationData() {
        DeviceSettingLiveData.getInstance().getmVibrationDuration().observe(this) {
            if (it == null) return@observe
            Log.i(TAG, "Duration == $it")
            ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s618)}：$it")
        }
    }
    //endregion

    //region 省电模式
    private fun sendPowerSaving() {
        ControlBleTools.getInstance().setPowerSaving(binding.cbPSwitch.isChecked, baseSendCmdStateListener)
    }

    private fun initPowerSavingData() {
        DeviceSettingLiveData.getInstance().getmPowerSaving().observe(this, object : Observer<Boolean?> {
            override fun onChanged(isOpen: Boolean?) {
                if (isOpen == null) return
                binding.cbPSwitch.isChecked = isOpen
                Log.i(TAG, "isOpen == $isOpen")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s130)}：$isOpen")
            }
        })
    }

    //endregion

    //region 覆盖息屏
    private fun sendOverlayScreen() {
        ControlBleTools.getInstance().setOverlayScreen(binding.cbOSwitch.isChecked, baseSendCmdStateListener)
    }

    private fun initOverlayScreenData() {
        DeviceSettingLiveData.getInstance().getmOverlayScreen().observe(this, object : Observer<Boolean?> {
            override fun onChanged(isOpen: Boolean?) {
                if (isOpen == null) return
                binding.cbOSwitch.isChecked = isOpen
                Log.i(TAG, "isOpen == $isOpen")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s133)}：$isOpen")
            }
        })
    }
    //endregion

    //region 快速眼动
    private fun sendRapidEyeMovement() {
        ControlBleTools.getInstance().setRapidEyeMovement(binding.cbRSwitch.isChecked, baseSendCmdStateListener)
    }

    private fun initRapidEyeMovementData() {
        DeviceSettingLiveData.getInstance().getmRapidEyeMovement().observe(this, object : Observer<Boolean?> {
            override fun onChanged(isOpen: Boolean?) {
                if (isOpen == null) return
                binding.cbRSwitch.isChecked = isOpen
                Log.i(TAG, "isOpen == $isOpen")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s136)}：$isOpen")
            }
        })
    }
    //endregion

    //region 抬腕亮屏设置
    private fun sendWristScreen() {
        val wristScreenBean = WristScreenBean()
        try {
            val mode = binding.etWMode.text.toString().trim { it <= ' ' }.toInt()
            val sh = binding.etWSH.text.toString().trim { it <= ' ' }.toInt()
            val sm = binding.etWSM.text.toString().trim { it <= ' ' }.toInt()
            val eh = binding.etWEH.text.toString().trim { it <= ' ' }.toInt()
            val em = binding.etWEM.text.toString().trim { it <= ' ' }.toInt()
            val sensitivity = binding.etWSensitivity.text.toString().trim { it <= ' ' }.toInt()
            wristScreenBean.timingMode = mode
            wristScreenBean.startTime = SettingTimeBean(sh, sm)
            wristScreenBean.endTime = SettingTimeBean(eh, em)
            wristScreenBean.sensitivityMode = sensitivity
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setWristScreen(wristScreenBean, baseSendCmdStateListener)
    }

    private fun initWristScreenData() {
        DeviceSettingLiveData.getInstance().getmWristScreen().observe(this, object : Observer<WristScreenBean?> {
            override fun onChanged(wristScreenBean: WristScreenBean?) {
                if (wristScreenBean == null) return
                Log.i(TAG, "EventInfoBeans == $wristScreenBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s125)}：$wristScreenBean")
            }
        })
    }

    //endregion

    //region 午休免打扰
    private fun sendDoNotDisturbMode() {
        val doNotDisturbMode = DoNotDisturbModeBean()
        try {
            val sh = binding.etNSH.text.toString().trim { it <= ' ' }.toInt()
            val sm = binding.etNSM.text.toString().trim { it <= ' ' }.toInt()
            val eh = binding.etNEH.text.toString().trim { it <= ' ' }.toInt()
            val em = binding.etNEM.text.toString().trim { it <= ' ' }.toInt()
            doNotDisturbMode.isSwitch = binding.cbNSwitch1.isChecked
            doNotDisturbMode.isSmartSwitch = binding.cbNSwitch2.isChecked
            doNotDisturbMode.startTime = SettingTimeBean(sh, sm)
            doNotDisturbMode.endTime = SettingTimeBean(eh, em)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setDoNotDisturbMode(doNotDisturbMode, baseSendCmdStateListener)
    }

    private fun initDoNotDisturbModeData() {
        DeviceSettingLiveData.getInstance().getmDoNotDisturbMode().observe(this, object : Observer<DoNotDisturbModeBean?> {
            override fun onChanged(doNotDisturbModeBean: DoNotDisturbModeBean?) {
                if (doNotDisturbModeBean == null) return
                Log.i(TAG, "doNotDisturbModeBean == $doNotDisturbModeBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s154)}：$doNotDisturbModeBean")
            }
        })
    }
    //endregion

    //region 息屏显示
    private fun sendScreenDisplay() {
        val displayBean = ScreenDisplayBean()
        try {
            val mode = binding.etDMode.text.toString().trim { it <= ' ' }.toInt()
            val sh = binding.etDSH.text.toString().trim { it <= ' ' }.toInt()
            val sm = binding.etDSM.text.toString().trim { it <= ' ' }.toInt()
            val eh = binding.etDEH.text.toString().trim { it <= ' ' }.toInt()
            val em = binding.etDEM.text.toString().trim { it <= ' ' }.toInt()
            val style = binding.etDStyle.text.toString().trim { it <= ' ' }.toInt()
            displayBean.timingMode = mode
            displayBean.startTime = SettingTimeBean(sh, sm)
            displayBean.endTime = SettingTimeBean(eh, em)
            displayBean.dialStyle = DialStyleBean(style)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setScreenDisplay(displayBean, baseSendCmdStateListener)
    }

    private fun initScreenDisplayData() {
        DeviceSettingLiveData.getInstance().getmScreenDisplay().observe(this, object : Observer<ScreenDisplayBean?> {
            override fun onChanged(screenDisplayBean: ScreenDisplayBean?) {
                if (screenDisplayBean == null) return
                Log.i(TAG, "screenDisplayBean == $screenDisplayBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s143)}：$screenDisplayBean")
            }
        })
    }
    //endregion

    //region 屏幕设置
    private fun sendScreenSetting() {
        val screenSetting = ScreenSettingBean()
        try {
            val level = binding.etLevel.text.toString().trim { it <= ' ' }.toInt()
            val duration = binding.etDuration.text.toString().trim { it <= ' ' }.toInt()
            screenSetting.level = level
            screenSetting.duration = duration
            screenSetting.isSwitch = binding.cbSSwitch1.isChecked
            screenSetting.doubleClick = binding.cbSSwitch2.isChecked
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setScreenSetting(screenSetting, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                    else -> MyApplication.showToast(getString(R.string.s221))
                }
            }
        })
    }

    private fun initScreenSettingData() {
        DeviceSettingLiveData.getInstance().getmScreenSetting().observe(this, object : Observer<ScreenSettingBean?> {
            override fun onChanged(screenSettingBean: ScreenSettingBean?) {
                if (screenSettingBean == null) return
                Log.i(TAG, "screenDisplayBean == $screenSettingBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s150)}：$screenSettingBean")
            }
        })
    }
    //endregion

    //region 久坐提醒
    private fun sendSedentaryReminder() {
        val reminder = CommonReminderBean()
        reminder.isOn = binding.cbSRSwitch.isChecked()
        reminder.noDisturbInLaunch = binding.cbSRSRwitch2.isChecked
        try {
            val stH = binding.etSSH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etSSM.text.toString().trim { it <= ' ' }.toInt()
            val etH = binding.etSEH.text.toString().trim { it <= ' ' }.toInt()
            val etM = binding.etSEM.text.toString().trim { it <= ' ' }.toInt()
            val frequency = binding.etSInterval.text.toString().trim { it <= ' ' }.toInt()
            reminder.startTime = SettingTimeBean(stH, stM)
            reminder.endTime = SettingTimeBean(etH, etM)
            reminder.frequency = frequency
        } catch (e: Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setSedentaryReminder(reminder, baseSendCmdStateListener)
    }

    private fun initSedentaryReminderData() {
        /**
         * 获取久坐提醒数据
         */
        DeviceSettingLiveData.getInstance().getmSedentaryReminder().observe(this, object : Observer<CommonReminderBean?> {
            override fun onChanged(commonReminderBean: CommonReminderBean?) {
                if (commonReminderBean == null) return
                Log.i(TAG, "SedentaryReminder == $commonReminderBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s230)}：$commonReminderBean")
            }
        })
    }
    //endregion

    //region 喝水提醒
    private fun sendDrinkWaterReminder() {
        val reminder = CommonReminderBean()
        reminder.isOn = binding.cbSRSwitch.isChecked()
        reminder.noDisturbInLaunch = binding.cbSRSRwitch2.isChecked
        try {
            val stH = binding.etSSH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etSSM.text.toString().trim { it <= ' ' }.toInt()
            val etH = binding.etSEH.text.toString().trim { it <= ' ' }.toInt()
            val etM = binding.etSEM.text.toString().trim { it <= ' ' }.toInt()
            val frequency = binding.etSInterval.text.toString().trim { it <= ' ' }.toInt()
            reminder.startTime = SettingTimeBean(stH, stM)
            reminder.endTime = SettingTimeBean(etH, etM)
            reminder.frequency = frequency
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setDrinkWaterReminder(reminder, baseSendCmdStateListener)
    }

    private fun initDrinkWaterReminderData() {
        /**
         * 获取喝水提醒数据
         */
        DeviceSettingLiveData.getInstance().getmDrinkWaterReminder().observe(this, object : Observer<CommonReminderBean?> {
            override fun onChanged(commonReminderBean: CommonReminderBean?) {
                if (commonReminderBean == null) return
                Log.i(TAG, "DrinkWaterReminder == $commonReminderBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s231)}：$commonReminderBean")
            }
        })
    }
    //endregion

    //region 吃药提醒
    private fun sendMedicationReminder() {
        val reminder = CommonReminderBean()
        reminder.isOn = binding.cbSRSwitch.isChecked()
        //TODO 吃药提醒无午休免打扰
        /*reminder.noDisturbInLaunch = cbSRSwitch2.isChecked();*/
        try {
            val stH = binding.etSSH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etSSM.text.toString().trim { it <= ' ' }.toInt()
            val etH = binding.etSEH.text.toString().trim { it <= ' ' }.toInt()
            val etM = binding.etSEM.text.toString().trim { it <= ' ' }.toInt()
            val frequency = binding.etSInterval.text.toString().trim { it <= ' ' }.toInt()
            reminder.startTime = SettingTimeBean(stH, stM)
            reminder.endTime = SettingTimeBean(etH, etM)
            reminder.frequency = frequency
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setMedicationReminder(reminder, baseSendCmdStateListener)
    }

    private fun initMedicationReminderData() {
        /**
         * 获取吃药提醒数据
         */
        DeviceSettingLiveData.getInstance().getmMedicationReminder().observe(this, object : Observer<CommonReminderBean?> {
            override fun onChanged(commonReminderBean: CommonReminderBean?) {
                if (commonReminderBean == null) return
                Log.i(TAG, "MedicationReminder == $commonReminderBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s61)}：$commonReminderBean")

            }
        })


    }
    //endregion

    //region 吃饭提醒
    private fun sendHaveMealsReminder() {
        val reminder = CommonReminderBean()
        reminder.isOn = binding.cbSRSwitch.isChecked()
        //TODO 吃饭提醒无午休免打扰
        /*reminder.noDisturbInLaunch = cbSRSwitch2.isChecked();*/
        try {
            val stH = binding.etSSH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etSSM.text.toString().trim { it <= ' ' }.toInt()
            val etH = binding.etSEH.text.toString().trim { it <= ' ' }.toInt()
            val etM = binding.etSEM.text.toString().trim { it <= ' ' }.toInt()
            val frequency = binding.etSInterval.text.toString().trim { it <= ' ' }.toInt()
            reminder.startTime = SettingTimeBean(stH, stM)
            reminder.endTime = SettingTimeBean(etH, etM)
            reminder.frequency = frequency
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setHaveMealsWaterReminder(reminder, baseSendCmdStateListener)
    }

    private fun initHaveMealsReminderData() {
        /**
         * 获取吃饭提醒数据
         */
        DeviceSettingLiveData.getInstance().getmHaveMealsReminder().observe(this, object : Observer<CommonReminderBean?> {
            override fun onChanged(commonReminderBean: CommonReminderBean?) {
                if (commonReminderBean == null) return
                Log.i(TAG, "HaveMealsReminder == $commonReminderBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s262)}：$commonReminderBean")
            }
        })
    }
    //endregion

    //region 洗手提醒
    private fun sendWashHandReminder() {
        val reminder = CommonReminderBean()
        reminder.isOn = binding.cbSRSwitch.isChecked
        //TODO 吃饭提醒无午休免打扰
        /*reminder.noDisturbInLaunch = cbSRSwitch2.isChecked();*/
        try {
            val stH = binding.etSSH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etSSM.text.toString().trim { it <= ' ' }.toInt()
            val etH = binding.etSEH.text.toString().trim { it <= ' ' }.toInt()
            val etM = binding.etSEM.text.toString().trim { it <= ' ' }.toInt()
            val frequency = binding.etSInterval.text.toString().trim { it <= ' ' }.toInt()
            reminder.startTime = SettingTimeBean(stH, stM)
            reminder.endTime = SettingTimeBean(etH, etM)
            reminder.frequency = frequency
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setWashHandReminder(reminder, baseSendCmdStateListener)
    }

    private fun initWashHandReminderData() {
        DeviceSettingLiveData.getInstance().getmWashHandReminder().observe(this, object : Observer<CommonReminderBean?> {
            override fun onChanged(commonReminderBean: CommonReminderBean?) {
                if (commonReminderBean == null) return
                Log.i(TAG, "WashHandReminder == $commonReminderBean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s263)}：$commonReminderBean")
            }
        })
    }
    //endregion

    //region 睡眠提醒
    private fun sendSleepReminder() {
        val reminder = SleepReminder()
        reminder.isOn = binding.cbSleepRSwitch.isChecked
        try {
            val stH = binding.etSleepH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etSleepM.text.toString().trim { it <= ' ' }.toInt()
            reminder.reminderTime = SettingTimeBean(stH, stM)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setSleepReminder(reminder, baseSendCmdStateListener)
    }

    private fun initSleepReminderData() {
        DeviceSettingLiveData.getInstance().getmSleepReminder().observe(this, object : Observer<SleepReminder?> {
            override fun onChanged(sleepReminder: SleepReminder?) {
                if (sleepReminder == null) return
                Log.i(TAG, "SleepReminder == $sleepReminder")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s465)}：$sleepReminder")
            }
        })
    }
    //endregion

    //region 事件提醒
    private var eventMax: Int = 0 //获取设备最多支持设置数量

    private fun sendEventInfo() {
        val eventInfoBeans = ArrayList<EventInfoBean>()
        for (i in 0..4) {
            val infoBean = EventInfoBean()
            var timeBean: TimeBean
            val des = binding.etEventDescr.text.toString().trim { it <= ' ' } + "" + i
            try {
                val y = binding.etEy.text.toString().trim { it <= ' ' }.toInt()
                val M = binding.etEM.text.toString().trim { it <= ' ' }.toInt()
                val d = binding.etEd.text.toString().trim { it <= ' ' }.toInt()
                val H = binding.etEH.text.toString().trim { it <= ' ' }.toInt()
                var m = binding.etEm.text.toString().trim { it <= ' ' }.toInt()
                m = if (m < 55) {
                    m + i
                } else {
                    m - i
                }
                val s = binding.etEs.text.toString().trim { it <= ' ' }.toInt()
                //infoBean = new EventInfoBean(des,new TimeBean(y,M,d,H,m,s));
                timeBean = TimeBean(y, M, d, H, m, s)
                infoBean.time = timeBean
                infoBean.description = des
                val f = binding.etEventFinish.text.toString().trim { it <= ' ' }.toInt()
                infoBean.isFinish = (f == 1)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
                return
            }
            eventInfoBeans.add(infoBean)
        }

        //list 可设置多个 / 修改 / 删除 ,TODO 最多不能超过eventMax
        ControlBleTools.getInstance().setEventInfoList(eventInfoBeans, baseSendCmdStateListener)
    }

    private fun initEventData() {

        /**
         * 获取事件提醒数据
         */
        DeviceSettingLiveData.getInstance().getmEventInfo().observe(this, object : Observer<List<EventInfoBean?>?> {
            override fun onChanged(eventInfoBeans: List<EventInfoBean?>?) {
                if (eventInfoBeans == null) return
                Log.i(TAG, "EventInfoBeans == $eventInfoBeans")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s232)}：$eventInfoBeans")
            }
        })

        /**
         * 获取事件提醒支持设置的最大数量
         */
        DeviceSettingLiveData.getInstance().getmEventMax().observe(this, object : Observer<Int?> {
            override fun onChanged(supportMax: Int?) {
                if (supportMax == null) return
                Log.i(TAG, "EventInfo supportMax == $supportMax")
                Toast.makeText(this@BerrySettingActivity, getString(R.string.s233) + " = " + supportMax, Toast.LENGTH_LONG).show()
                eventMax = supportMax
            }
        })
    }
    //endregion

    //region 闹钟提醒
    private var clockMax: Int = 0 //获取设备最多支持设置数量

    private fun sendClockInfo() {
        val clockInfoBeans = java.util.ArrayList<ClockInfoBean>()
        val clockInfo = ClockInfoBean()
        val data = DataBean()
        try {
            val stH = binding.etCSH.text.toString().trim { it <= ' ' }.toInt()
            val stM = binding.etCSM.text.toString().trim { it <= ' ' }.toInt()
            data.time = SettingTimeBean(stH, stM)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s238))
            return
        }
        data.clockName = binding.etCName.text.toString().trim { it <= ' ' }
        data.isEnable = binding.cbCSwitch.isChecked
        data.isMonday = binding.cbC1.isChecked
        data.isTuesday = binding.cbC2.isChecked
        data.isWednesday = binding.cbC3.isChecked
        data.isThursday = binding.cbC4.isChecked
        data.isFriday = binding.cbC5.isChecked
        data.isSaturday = binding.cbC6.isChecked
        data.isSunday = binding.cbC7.isChecked
        data.calculateWeekDays()
        clockInfo.id = 0 //闹钟数量下标
        clockInfo.data = data
        clockInfoBeans.add(clockInfo)
        // 模拟增加多个
        // clockInfoBeans.add(clockInfo)

        //list可设置多个 / 修改 / 删除 ,TODO 最多不能超过clockMax
        ControlBleTools.getInstance().setClockInfoList(clockInfoBeans, baseSendCmdStateListener)
    }

    private fun initClockData() {

        /**
         * 获取闹钟提醒数据
         */
        DeviceSettingLiveData.getInstance().getmClockInfo().observe(this, object : Observer<List<ClockInfoBean?>?> {
            override fun onChanged(clockInfoBeans: List<ClockInfoBean?>?) {
                if (clockInfoBeans == null) return
                Log.i(TAG, "ClockInfoBeans == $clockInfoBeans")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s234)}：$clockInfoBeans")
            }
        })

        /**
         * 获取闹钟提醒支持设置的最大数量
         */
        DeviceSettingLiveData.getInstance().getmClockMax().observe(this, object : Observer<Int?> {
            override fun onChanged(supportMax: Int?) {
                if (supportMax == null) return
                Log.i(TAG, "ClockInfo supportMax == $supportMax")
                Toast.makeText(this@BerrySettingActivity, getString(R.string.s235) + " = " + supportMax, Toast.LENGTH_LONG).show()
                clockMax = supportMax
            }
        })
    }
    //endregion

    //region 经典蓝牙设置
    private fun setClassicBluetoothState() {
        val bean = ClassicBluetoothStateBean(binding.cbBtSwitch.isChecked, binding.cbBtRemind.isChecked)
        ControlBleTools.getInstance().setClassicBluetoothState(bean, baseSendCmdStateListener)
    }

    private fun initClassicBluetoothStateCallBack() {
        DeviceSettingLiveData.getInstance().getmClassicBluetoothStateBean().observe(this, object : Observer<ClassicBluetoothStateBean?> {
            override fun onChanged(classicBluetoothStateBean: ClassicBluetoothStateBean?) {
                if (classicBluetoothStateBean == null) return
                Log.i(TAG, "classicBluetoothStateBean == $classicBluetoothStateBean")
                ToastDialog.showToast(this@BerrySettingActivity, getString(R.string.s357) + "：" + classicBluetoothStateBean)
            }
        })
    }
    //endregion

    //region 学校模式
    private var testSchoolModeTimeValue: Int = 0
    private fun sendSchoolMode() {
        testSchoolModeTimeValue++
        if (testSchoolModeTimeValue + 10 > 24) {
            testSchoolModeTimeValue = 0
        }
        val schoolBean = SchoolBean(
            true,
            SettingTimeBean(testSchoolModeTimeValue, 0),
            SettingTimeBean(testSchoolModeTimeValue + 10, 30),
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            testSchoolModeTimeValue,
            true,
            true
        )
        ControlBleTools.getInstance().setSchoolMode(schoolBean, baseSendCmdStateListener)
    }
    //endregion

    //region 调度器
    private var testSchedulerValue = 0
    private fun sendScheduler() {
        if (DeviceSettingLiveData.getInstance().getmScheduler().value == null) {
            Toast.makeText(this@BerrySettingActivity, getString(R.string.s219), Toast.LENGTH_LONG).show()
            return
        }
        testSchedulerValue++
        if (testSchedulerValue + 10 > 24) {
            testSchedulerValue = 0
        }
        val schedulerBean = DeviceSettingLiveData.getInstance().getmScheduler().value
        if (schedulerBean.alertList != null && schedulerBean.alertList.size > 0) {
            schedulerBean.alertList[0].alertName = "alertName$testSchedulerValue"
        } else {
            schedulerBean.alertList = java.util.ArrayList()
            schedulerBean.alertList.add(
                AlertBean(
                    "alertName$testSchedulerValue",
                    "alertName",
                    SettingTimeBean(testSchedulerValue, 0),
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
                )
            )
        }
        if (schedulerBean.habitBeanList != null && schedulerBean.habitBeanList.size > 0) {
            schedulerBean.habitBeanList[0].habitName = "habitName$testSchedulerValue"
        } else {
            schedulerBean.habitBeanList = java.util.ArrayList()
            val test = java.util.ArrayList<SettingTimeBean>()
            test.add(SettingTimeBean(testSchedulerValue, 0))
            schedulerBean.habitBeanList.add(HabitBean(0, "habitName$testSchedulerValue", test, false, false, false, false, false, false, false))
        }
        if (schedulerBean.reminderBeanList != null && schedulerBean.reminderBeanList.size > 0) {
            schedulerBean.reminderBeanList[0].reminderName = "reminderName$testSchedulerValue"
        } else {
            schedulerBean.reminderBeanList = java.util.ArrayList()
            schedulerBean.reminderBeanList.add(
                ReminderBean(
                    0,
                    "reminderName$testSchedulerValue",
                    SettingTimeBean(testSchedulerValue, 0),
                    SettingTimeBean(testSchedulerValue + 10, 0),
                    testSchedulerValue,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
                )
            )
        }
        ControlBleTools.getInstance().setScheduleReminder(schedulerBean, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                    else -> MyApplication.showToast(getString(R.string.s221))
                }
            }
        })
    }

    private fun initGetSchedulerData() {
        DeviceSettingLiveData.getInstance().getmScheduler().observe(this, object : Observer<SchedulerBean?> {
            override fun onChanged(bean: SchedulerBean?) {
                if (bean == null) return
                Log.i(TAG, "getmScheduler == $bean")
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s323)}：$bean")
            }
        })
    }
    //endregion

    //region 生理周期
    private fun sendPhysiologicalCycle() {
        val physiologicalCycle = PhysiologicalCycleBean()
        try {
            val y = binding.etY.text.toString().trim { it <= ' ' }.toInt()
            val m = binding.etM.text.toString().trim { it <= ' ' }.toInt()
            val d = binding.etD.text.toString().trim { it <= ' ' }.toInt()
            val tip = binding.etTip.text.toString().trim { it <= ' ' }.toInt()
            val allDay = binding.etAllDay.text.toString().trim { it <= ' ' }.toInt()
            val day = binding.etDay.text.toString().trim { it <= ' ' }.toInt()
            physiologicalCycle.remindSwitch = binding.cbPhSwitch.isChecked
            physiologicalCycle.advanceDay = tip
            physiologicalCycle.totalCycleDay = allDay
            physiologicalCycle.physiologicalCycleDay = day
            physiologicalCycle.physiologicalStartDate = DateBean(y, m, d)
            physiologicalCycle.physiologicalCycleSwitch = binding.cbPhSwitch2.isChecked
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setPhysiologicalCycle(physiologicalCycle, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                    else -> MyApplication.showToast(getString(R.string.s221))
                }
            }
        })
    }

    private fun initPhysiologicalCycleData() {
        CallBackUtils.physiologicalCycleCallBack = PhysiologicalCycleCallBack { bean ->
            Log.i(TAG, "physiologicalCycleBean == $bean")
            ToastDialog.showToast(
                this@BerrySettingActivity, "${getString(R.string.s106)}：$bean"
            )
        }
    }
    //endregion

    //region 股票
    var stockInfos: MutableList<StockInfoBean> = mutableListOf()

    private fun initStockData() {
        //region TODO 测试股票数据
        for (i in 0..9) {
            /**
             * symbol;        //股票代码
             * market;        //股票市场编号
             * name;          //股票名称
             * latestPrice;   //最新价格
             * preClose;      //收盘价格
             */
            val stockInfoBean = StockInfoBean(
                "symbol_$i",
                "market_$i", "name$i", 110.0f, 100.0f, 11, (System.currentTimeMillis() / 1000).toInt(), 22
            )
            stockInfos.add(stockInfoBean)
        }

        //endregion
        CallBackUtils.setStockCallBack(object : StockCallBack {
            override fun onStockInfoList(list: MutableList<StockSymbolBean>?) {
                ToastDialog.showToast(this@BerrySettingActivity, "" + GsonUtils.toJson(list))
            }

            override fun onWearRequestStock() {
                binding.btnSyncStock.callOnClick()
            }

        })
    }

    //endregion

    //region 连续血氧设置
    private fun setContinuousBloodOxygenSettings() {
        val bean = ContinuousBloodOxygenSettingsBean()
        try {
            bean.mode = if (binding.cbBoModeSwitch.isChecked) 0 else 1
            bean.frequency = binding.etBoReminderSwitch.text.toString().trim { it <= ' ' }.toInt()
            val sh = binding.etBoSH.text.toString().trim { it <= ' ' }.toInt()
            val sm = binding.etBoSM.text.toString().trim { it <= ' ' }.toInt()
            val eh = binding.etBoEH.text.toString().trim { it <= ' ' }.toInt()
            val em = binding.etBoEM.text.toString().trim { it <= ' ' }.toInt()
            bean.startTime = SettingTimeBean(sh, sm)
            bean.endTime = SettingTimeBean(eh, em)
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setContinuousBloodOxygenSettings(bean, baseSendCmdStateListener)
    }

    private fun initContinuousBloodOxygenSettingsData() {
        DeviceSettingLiveData.getInstance().getmContinuousBloodOxygenSettings().observe(this, object : Observer<ContinuousBloodOxygenSettingsBean?> {
            @SuppressLint("SetTextI18n")
            override fun onChanged(settingsBean: ContinuousBloodOxygenSettingsBean?) {
                if (settingsBean != null) {
                    Log.i(TAG, "ContinuousBloodOxygenSettingsBean == $settingsBean")
                    // 0 开启  1 关闭
                    binding.cbBoModeSwitch.isChecked = settingsBean.mode == 0
                    binding.etBoReminderSwitch.setText(settingsBean.frequency.toString() + "")
                    ToastDialog.showToast(
                        this@BerrySettingActivity, "${getString(R.string.s343)}：$settingsBean"
                    )
                }
            }
        })
    }
    //endregion

    //region 找手机设置
    private fun setFindWearSetting() {
        val bean = FindWearSettingsBean()
        try {
            bean.ringMode = binding.etFwRMode.text.toString().trim { it <= ' ' }.toInt()
            bean.vibrationMode = binding.etFwVMode.text.toString().trim { it <= ' ' }.toInt()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setFindWearSettings(bean, baseSendCmdStateListener)
    }

    private fun initFindWearSettingsData() {
        DeviceSettingLiveData.getInstance().getmFindWearSettingsBean().observe(this, object : Observer<FindWearSettingsBean?> {
            @SuppressLint("SetTextI18n")
            override fun onChanged(settingsBean: FindWearSettingsBean?) {
                if (settingsBean != null) {
                    Log.i(TAG, "getmFindWearSettingsBean == $settingsBean")
                    binding.etFwRMode.setText(settingsBean.ringMode.toString() + "")
                    binding.etFwVMode.setText(settingsBean.vibrationMode.toString() + "")
                    ToastDialog.showToast(
                        this@BerrySettingActivity, "${getString(R.string.s348)}：$settingsBean"
                    )
                }
            }
        })
    }
    //endregion

    //region 通知设置
    private fun setNoticeSettings() {
        val bean = NotificationSettingsBean()
        try {
            bean.noticeNotLightUp = binding.cbNoticeNotLightUp.isChecked
            bean.isOnlyLockedNotify = binding.cbNotice2.isChecked
            bean.isOnlyWornNotify = binding.cbNotice3.isChecked
            bean.delayReminderSwitch = binding.cbNoticeDelay.isChecked
            val vMode = binding.etPhoneRemindVibrationMode.text.toString().trim { it <= ' ' }.toInt()
            val rMode = binding.etPhoneRemindRingMode.text.toString().trim { it <= ' ' }.toInt()
            bean.phoneRemindVibrationMode = vMode
            bean.phoneRemindRingMode = rMode
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
        ControlBleTools.getInstance().setNotificationSettings(bean, baseSendCmdStateListener)
    }

    private fun initNoticeSettings() {
        DeviceSettingLiveData.getInstance().getmNotificationSettings().observe(this, object : Observer<NotificationSettingsBean?> {
            override fun onChanged(settingsBean: NotificationSettingsBean?) {
                if (settingsBean != null) {
                    Log.i(TAG, "NotificationSettingsBean == $settingsBean")
                    ToastDialog.showToast(
                        this@BerrySettingActivity, "${getString(R.string.s334)}：$settingsBean"
                    )
                    binding.cbNoticeNotLightUp.isChecked = settingsBean.noticeNotLightUp
                    binding.cbNoticeDelay.isChecked = settingsBean.delayReminderSwitch
                    binding.etPhoneRemindVibrationMode.setText("" + settingsBean.phoneRemindVibrationMode)
                    binding.etPhoneRemindRingMode.setText("" + settingsBean.phoneRemindRingMode)
                }
            }
        })
    }
    //endregion

    //region 左键功能
    private fun setCustomizeSet() {
        try {
            val type = binding.etCustomizeSet.text.toString().trim { it <= ' ' }.toInt()
            ControlBleTools.getInstance().setCustomizeSet(type, baseSendCmdStateListener)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
    }

    private fun initCustomizeSet() {
        DeviceSettingLiveData.getInstance().getmCustomizeSet().observe(this, object : Observer<Int?> {
            override fun onChanged(t: Int?) {
                ToastDialog.showToast(
                    this@BerrySettingActivity, "${getString(R.string.s639)}：$t"
                )
            }
        })
    }
    //endregion

    //region 运动自识别
    private fun sendAutoSport() {
        try {
            val isRecognitionOpen = binding.cbAutoSportSwitch.isChecked
            val isPauseOpen = binding.cbAutoSportStopSwitch.isChecked
            ControlBleTools.getInstance().setMotionRecognition(isRecognitionOpen, isPauseOpen, baseSendCmdStateListener)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
            return
        }
    }

    private fun initMotionRecognitionData() {
        DeviceSettingLiveData.getInstance().getmMotionRecognition().observe(this, object : Observer<Pair<Boolean, Boolean>?> {
            override fun onChanged(t: Pair<Boolean, Boolean>?) {
                if (t == null) return
                binding.cbAutoSportSwitch.isChecked = t.first
                binding.cbAutoSportStopSwitch.isChecked = t.second
                ToastDialog.showToast(
                    this@BerrySettingActivity, "${getString(R.string.s644)}：${t.first} , ${t.second}"
                )
            }
        })
    }

    //endregion

    //region 设置早报
    private fun setMorningPost() {
        try {
            val title = binding.etMpTitle.text.toString().trim()
            val content = binding.etMpContent.text.toString().trim()
            ControlBleTools.getInstance().setMorningPost(MorningPostBean().apply {
                this.title = title
                this.content = content
            }, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
        }
    }

    private fun initMorningPostCallBack() {
        CallBackUtils.morningPostCallBack = object : MorningPostCallBack {
            override fun onRequestMorningPost() {
                //设备请求发送早报信息，再次发送
                binding.btnSetMorningPost.callOnClick()
            }
        }
    }
    //endregion

    //region 保险库

    private fun initVaultCallBack() {
        CallBackUtils.vaultCallBack = object : VaultCallBack {
            override fun onSimpleVaultInfoList(list: List<VaultSimpleBean?>?) {
                showTips(getString(R.string.s718) + ":" + GsonUtils.toJson(list))
            }

            override fun onVaultInfo(bean: VaultInfoBean?) {
                showTips(getString(R.string.s719) + ":" + GsonUtils.toJson(bean))
            }

            override fun onDevRequestVaultInfo(cardIds: List<String?>?) {
                showTips(getString(R.string.s721) + ":" + GsonUtils.toJson(cardIds))
            }
        }
    }

    private fun sendVault() {
        try {
            var cardId = binding.etVaultCardId.text.toString().trim()
            var sort = binding.etVaultSort.text.toString().trim().toInt()
            var appNum = binding.etVaultAppNum.text.toString().trim().toInt()
            var alwaysOn = binding.etVaultAlwaysOn.text.toString().trim().toInt()
            var deleteDays = binding.etVaultDeleteDays.text.toString().trim().toInt()
            var password = binding.etVaultPassword.text.toString().trim()
            var vaultStringList = ArrayList<String>()
            var strs = binding.etVaultStrings.text.toString()
            if (strs.contains(",")) {
                val split = strs.split(",")
                vaultStringList.addAll(split)
            } else {
                vaultStringList.add(strs)
            }
            ControlBleTools.getInstance().setVaultInfo(VaultInfoBean().apply {
                this.cardId = cardId
                this.sort = sort
                this.appNum = appNum
                this.alwaysOn = alwaysOn
                this.deleteDays = deleteDays
                this.password = password
                this.vaultStringList = vaultStringList
            }, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
        }
    }

    private fun getVaultInfoById() {
        try {
            val cardId = binding.etGetVaultCardId.text.toString().trim()
            ControlBleTools.getInstance().getVaultInfo(cardId, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
        }
    }

    private fun delVaultInfoByIds() {
        try {
            var cardIds = arrayListOf<String>()
            val ids = binding.etDelVaultCardId.text.toString().trim()
            if (ids.contains(",")) {
                val split = ids.split(",")
                cardIds.addAll(split)
            } else {
                cardIds.add(ids)
            }
            ControlBleTools.getInstance().delVaultInfoList(cardIds, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
        }
    }

    //endregion

    //region AI 录音
    private fun initRecordingCallBack() {
        CallBackUtils.recordingCallBack = object : RecordingCallBack {
            override fun onRecordingCmd(bean: RecordingCmdBean?) {
                //（0x00 无意义 0x01 开始 0x02 继续 0x03 结束 0x04 暂停 0x05 可传输  0x06 网络异常）
                ToastDialog.showToast(this@BerrySettingActivity, "${getString(R.string.s725)}:${GsonUtils.toJson(bean)}")
            }

            override fun onRecordingData(data: ByteArray?) {
                LogUtils.e("${getString(R.string.s726)}：${BleUtils.bytes2HexString(data)}")
            }

        }
    }

    private fun sendRecordingCmd() {
        try {
            val recordingCmd = binding.etAiCmd.text.toString().trim().toInt()
            ControlBleTools.getInstance().sendRecordingCmd(recordingCmd, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            showTips(getString(R.string.s238))
        }
    }
    //endregion

    private fun initCallBack() {
        initNoticeSettings()
        initFindWearSettingsData()
        initContinuousBloodOxygenSettingsData()
        initStockData()
        initPhysiologicalCycleData()
        initGetSchedulerData()
        initClassicBluetoothStateCallBack()
        initClockData()
        initEventData()
        initSleepReminderData()
        initWashHandReminderData()
        initHaveMealsReminderData()
        initMedicationReminderData()
        initDrinkWaterReminderData()
        initSedentaryReminderData()
        initScreenSettingData()
        initScreenDisplayData()
        initDoNotDisturbModeData()
        initWristScreenData()
        initRapidEyeMovementData()
        initOverlayScreenData()
        initPowerSavingData()
        initVibrationDurationData()
        initVibrationData()
        initPressureModeData()
        initSleepModeObserve()
        initHeartRateMonitorData()
        initCustomizeSet()
        initMotionRecognitionData()
        initMorningPostCallBack()
        initVaultCallBack()
        initRecordingCallBack()
    }

}