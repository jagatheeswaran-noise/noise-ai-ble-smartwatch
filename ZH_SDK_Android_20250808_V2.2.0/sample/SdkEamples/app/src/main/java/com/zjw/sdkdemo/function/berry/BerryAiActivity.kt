package com.zjw.sdkdemo.function.berry

import android.os.Bundle
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.BleCmd.BerryCmd.sendAiErrorCode
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ClockInfoBean
import com.zhapp.ble.bean.ScreenSettingBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.bean.TimeBean
import com.zhapp.ble.bean.berry.AiActionBean
import com.zhapp.ble.bean.berry.AiHistoryUiBean
import com.zhapp.ble.bean.berry.AiOpenFunctionBean
import com.zhapp.ble.bean.berry.AiToggleBean
import com.zhapp.ble.bean.berry.AiViewUiBean
import com.zhapp.ble.bean.berry.AiVoiceCmdBean
import com.zhapp.ble.callback.AiFunctionCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.utils.BleUtils
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryAiBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.ToastDialog
import java.util.Calendar

/**
 * Created by Android on 2025/7/2.
 */
class BerryAiActivity : BaseActivity() {

    private val binding: ActivityBerryAiBinding by lazy { ActivityBerryAiBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s727)
        setContentView(binding.root)
        initEvent()
        initCallBack()
    }

    private fun initCallBack() {
        CallBackUtils.aiFunctionCallBack = object : AiFunctionCallBack {

            override fun onDevAiVoiceCmd(bean: AiVoiceCmdBean?) {
                ToastDialog.showToast(this@BerryAiActivity, "${getString(R.string.s728)}: ${GsonUtils.toJson(bean)}")
            }

            override fun onDevAiVoiceData(data: ByteArray?) {
                if (data == null) return
                LogUtils.i("Ai Data :" + BleUtils.bytes2HexString(data))
            }

        }
    }

    private fun initEvent() {
        click(binding.btnSendAiCmd) {
            sendAiCmd()
        }

        click(binding.btnSendAiError) {
            sendAiErrorCodes()
        }

        click(binding.btnSendAiTranslatedText) {
            sendAiTText()
        }

        click(binding.btnSendAiAnswerText) {
            sendAiAText()
        }

        click(binding.btnSendViewUi) {
            sendViewUI()
        }

        click(binding.btnSendHistoryUi) {
            sendHistoryUI()
        }

        click(binding.btnSendAction) {
            sendAction()
        }

        click(binding.btnSendToggle) {
            sendToggle()
        }

        click(binding.btnSendOpenFun) {
            sendOpenFun()
        }
    }


    private fun sendAiCmd() {
        try {
            val cmd = binding.etAiCmd.text.toString().trim().toInt()
            ControlBleTools.getInstance().sendAiVoiceCmd(cmd, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendAiErrorCodes() {
        try {
            val error = binding.etAiError.text.toString().trim().toInt()
            ControlBleTools.getInstance().sendAiErrorCode(error, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendAiTText() {
        try {
            val text = binding.etAiTranslatedText.text.toString().trim()
            ControlBleTools.getInstance().sendAiTranslatedText(text, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendAiAText() {
        try {
            val text = binding.etAiAnswerText.text.toString().trim()
            ControlBleTools.getInstance().sendAiAnswerText(text, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendViewUI() {
        try {
            val title = binding.etAiVTitle.text.toString().trim()
            val value = binding.etAiVValue.text.toString().trim()
            val unit = binding.etAiVUnit.text.toString().trim()
            val footer = binding.etAiVFooter.text.toString().trim()
            val actionTime = binding.etAiVTime.text.toString().trim()
            val time = TimeUtils.string2Date(actionTime, "yyyy-MM-dd HH:mm:ss")
            val calender = Calendar.getInstance()
            calender.time = time
            val bean = AiViewUiBean().apply {
                this.title = title
                this.value = value
                this.unit = unit
                this.footer = footer
                this.actionTime = TimeBean().apply {
                    this.year = calender.get(Calendar.YEAR)
                    this.month = calender.get(Calendar.MONTH) + 1
                    this.day = calender.get(Calendar.DAY_OF_MONTH)
                    this.hour = calender.get(Calendar.HOUR_OF_DAY)
                    this.minute = calender.get(Calendar.MINUTE)
                    this.second = calender.get(Calendar.SECOND)
                }
            }
            ControlBleTools.getInstance().sendAiViewUi(bean, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendHistoryUI() {
        try {
            val title = binding.etAiHTitle.text.toString().trim()
            val preriod = binding.etAiHPeriod.text.toString().trim()
            val valueTitle = binding.etAiHValueTitle.text.toString().trim()
            val yMax = binding.etAiHYMax.text.toString().trim().toInt()
            val maxValue = binding.etAiHMaxValue.text.toString().trim().toInt()
            val minValue = binding.etAiHMinValue.text.toString().trim().toInt()
            val avgValue = binding.etAiHAvgValue.text.toString().trim().toInt()
            val unit = binding.etAiHUnit.text.toString().trim()
            val ydm = binding.etAiHYMD.text.toString().trim()
            val time = TimeUtils.string2Date(ydm, "yyyy-MM-dd")
            val chartValue = binding.etAiHChartValue.text.toString().trim().toInt()
            val category = binding.etAiHCategory.text.toString().trim()

            val calender = Calendar.getInstance()
            calender.time = time
            val bean = AiHistoryUiBean().apply {
                this.title = title
                this.period = preriod
                this.valueTitle = valueTitle
                this.setyMax(yMax)
                this.summary = AiHistoryUiBean.Summary().apply {
                    this.maxValue = maxValue
                    this.minValue = minValue
                    this.avgValue = avgValue
                    this.unit = unit
                }
                this.chartData = mutableListOf<AiHistoryUiBean.ChartData>().apply {
                    for (i in 0..6) {
                        add(AiHistoryUiBean.ChartData().apply {
                            this.year = calender.get(Calendar.YEAR)
                            this.month = calender.get(Calendar.MONTH) + 1
                            this.day = calender.get(Calendar.DAY_OF_MONTH)
                            this.week = calender.get(Calendar.DAY_OF_WEEK) - 1
                            this.chartValue = chartValue
                            this.category = category
                        })
                        //calender累加一天
                        calender.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }
            ControlBleTools.getInstance().sendAiHistoryUi(bean, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendAction() {
        try {
            val scenario = binding.etAiAScenario.text.toString().trim().toInt()
            val thresholdValue = binding.etAiAThresholdValue.text.toString().trim().toInt()
            val unit = binding.etAiAUnit.text.toString().trim()
            val ydmTime = TimeUtils.string2Date(binding.etAiAYMD.text.toString().trim(), "yyyy-MM-dd")
            val ydmCalender = Calendar.getInstance()
            ydmCalender.time = ydmTime
            val time = TimeUtils.string2Date(binding.etAiATime.text.toString().trim(), "yyyy-MM-dd HH:mm:ss")
            val timeCalender = Calendar.getInstance()
            timeCalender.time = time
            val bean = AiActionBean().apply {
                this.scenario = scenario
                this.thresholdValue = thresholdValue
                this.unit = unit
                this.year = ydmCalender.get(Calendar.YEAR)
                this.month = ydmCalender.get(Calendar.MONTH) + 1
                this.day = ydmCalender.get(Calendar.DAY_OF_MONTH)
                this.actionTime = TimeBean().apply {
                    this.year = timeCalender.get(Calendar.YEAR)
                    this.month = timeCalender.get(Calendar.MONTH) + 1
                    this.day = timeCalender.get(Calendar.DAY_OF_MONTH)
                    this.hour = timeCalender.get(Calendar.HOUR_OF_DAY)
                    this.minute = timeCalender.get(Calendar.MINUTE)
                    this.second = timeCalender.get(Calendar.SECOND)
                }
            }
            ControlBleTools.getInstance().sendAiAction(bean, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendToggle() {
        try {
            val scenario = binding.etAiTScenario.text.toString().trim().toInt()
            val isOpen = binding.cbAiTToggleStatus.isChecked
            val time = binding.etAiTTime.text.toString().trim()
            val date = TimeUtils.string2Date(time, "yyyy-MM-dd HH:mm:ss")
            val calender = Calendar.getInstance()
            calender.time = date
            val bean = AiToggleBean().apply {
                this.scenario = scenario
                this.isToggleStatus = isOpen
                this.actionTime = TimeBean().apply {
                    this.year = calender.get(Calendar.YEAR)
                    this.month = calender.get(Calendar.MONTH) + 1
                    this.day = calender.get(Calendar.DAY_OF_MONTH)
                    this.hour = calender.get(Calendar.HOUR_OF_DAY)
                    this.minute = calender.get(Calendar.MINUTE)
                    this.second = calender.get(Calendar.SECOND)
                }
            }
            ControlBleTools.getInstance().sendAiToggle(bean, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

    private fun sendOpenFun() {
        try {
            val function = binding.etAiOFScenario.text.toString().trim().toInt()
            val time = binding.etAiOFTime.text.toString().trim()
            val date = TimeUtils.string2Date(time, "yyyy-MM-dd HH:mm:ss")
            val calender = Calendar.getInstance()
            calender.time = date
            val contactsNumber = binding.etAiOFContactsNumber.text.toString().trim()
            val workoutType = binding.etAiOFWorkoutType.text.toString().trim().toInt()
            val duration = binding.etAiOFDuration.text.toString().trim().toInt()
            val musicCommand = binding.etAiOFMusicCommand.text.toString().trim().toInt()
            val bean = AiOpenFunctionBean().apply {
                this.function = function
                this.actionTime = TimeBean().apply {
                    this.year = calender.get(Calendar.YEAR)
                    this.month = calender.get(Calendar.MONTH) + 1
                    this.day = calender.get(Calendar.DAY_OF_MONTH)
                    this.hour = calender.get(Calendar.HOUR_OF_DAY)
                    this.minute = calender.get(Calendar.MINUTE)
                    this.second = calender.get(Calendar.SECOND)
                }
                this.contactsNumber = contactsNumber
                this.workoutType = workoutType
                this.duration = duration
                this.musicCommand = musicCommand
                this.screenSetting = ScreenSettingBean().apply {
                    this.level = 1
                    this.isSwitch = true
                    this.duration = 5
                    this.doubleClick = true
                }
                this.clockInfoList = mutableListOf<ClockInfoBean>().apply {
                    add(ClockInfoBean().apply {
                        this.id = 0
                        this.data = ClockInfoBean.DataBean().apply {
                            this.time = SettingTimeBean(8, 0)
                            this.isMonday = true
                            this.clockName = "clock1"
                        }
                    })
                }
            }
            ControlBleTools.getInstance().sendAiOpenFunction(bean, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(R.string.s238)
        }
    }

}