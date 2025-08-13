package com.zjw.sdkdemo.function

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.google.gson.Gson
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.HeartRateLeakageRawBean
import com.zhapp.ble.bean.RingAutoActiveSportConfigBean
import com.zhapp.ble.bean.RingChargingCaseInfoBean
import com.zhapp.ble.bean.RingSleepConfigBean
import com.zhapp.ble.callback.AutoSportDataCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.HeartRateLeakageRawCallBack
import com.zhapp.ble.callback.RingAllDaySleepConfigCallBack
import com.zhapp.ble.callback.RingAutoActiveSportConfigCallBack
import com.zhapp.ble.callback.RingChargingCaseInfoCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityRingTestBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.function.measure.ActiveMeasureTypeActivity
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2023/5/6.
 */
class RingTestActivity : BaseActivity() {
    private val binding by lazy { ActivityRingTestBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s462)
        setContentView(binding.root)
        initDataCallBack()
    }

    private fun initDataCallBack() {
        CallBackUtils.autoSportDataCallBack = AutoSportDataCallBack { sportData ->
            ToastDialog.showToast(this@RingTestActivity, "TodayActivityIndicatorsBean  = " + GsonUtils.toJson(sportData))
        }

        CallBackUtils.ringAllDaySleepConfigCallBack = RingAllDaySleepConfigCallBack { config ->
            ToastDialog.showToast(this@RingTestActivity, "RingSleepConfigBean  = " + GsonUtils.toJson(config))
            binding.cbAllDaySleepSwitch.isChecked = config.isAllDaySleepSwitch
        }

        CallBackUtils.ringAutoActiveSportConfigCallBack = RingAutoActiveSportConfigCallBack { configBean ->
            ToastDialog.showToast(this@RingTestActivity, "RingAutoActiveSportConfigBean  = " + GsonUtils.toJson(configBean))
            binding.cbAutoActiveSportSwitch.isChecked = configBean.isAutoActiveSportSwitch
        }

        CallBackUtils.heartRateLeakageRawCallBack = HeartRateLeakageRawCallBack { bean ->
            LogUtils.e("Raw:" + bean.toCsvContent())
            rawData.add(bean)
        }

    }

    fun toActiveMeasure(view: View?) {
        startActivity(Intent(this@RingTestActivity, ActiveMeasureTypeActivity::class.java))
    }


    /**
     * 获取自动运动数据列表
     *
     * @param view
     */
    fun getAutoSportData(view: View?) {

        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getAutoSportData(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    fun getRingAllDaySleepConfig(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getRingAllDaySleepConfig(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    fun setRingAllDaySleepConfig(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance()
                .setRingAllDaySleepConfig(RingSleepConfigBean(binding.cbAllDaySleepSwitch.isChecked), object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        when (state) {
                            SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                            else -> MyApplication.showToast(getString(R.string.s221))
                        }
                    }
                })
        }
    }

    fun getAutoActiveSportConfig(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getRingAutoActiveSportConfig(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    fun setAutoActiveSportConfig(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            var timeValue = 0
            try {
                val time = binding.etTime.text.toString().trim()
                timeValue = time.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showLong(R.string.s238)
                return
            }
            ControlBleTools.getInstance()
                .setRingAutoActiveSportConfig(RingAutoActiveSportConfigBean(binding.cbAutoActiveSportSwitch.isChecked).apply {
                    this.autoActiveSportActivetime = timeValue
                }, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        when (state) {
                            SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                            else -> MyApplication.showToast(getString(R.string.s221))
                        }
                    }
                })
        }
    }


    //region 原始数据

    private val rawData = mutableListOf<HeartRateLeakageRawBean>()

    fun startRawUpload(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().setHeartRateRawSwitch(true, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    fun endRawUpload(view: View?) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().setHeartRateRawSwitch(false, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    fun shareRaw(view: View?) {
        if (rawData.isEmpty()) {
            MyApplication.showToast(getString(R.string.s221))
            return
        }
        var validNum = 0
        for (item in rawData) {
            if (!item.toCsvContent().isNullOrEmpty()) {
                validNum++
            }
        }
        //无有效
        if (validNum == 0) {
            MyApplication.showToast(getString(R.string.s221))
        }

        val csvDir = PathUtils.getAppDataPathExternalFirst() + "/csv"
        FileUtils.createOrExistsDir(csvDir)
        val csvFilePath = csvDir + "/raw_" + System.currentTimeMillis() + ".csv"
        //head
        FileIOUtils.writeFileFromString(csvFilePath, HeartRateLeakageRawBean.getHeadStr(), true)
        //content
        for (item in rawData) {
            if (!item.toCsvContent().isNullOrEmpty()) {
                FileIOUtils.writeFileFromString(csvFilePath, item.toCsvContent(), true)
            }
        }
        binding.rawFileName.setText(getString(R.string.s513) + " $csvFilePath")
        //share
        val zipFile = FileUtils.getFileByPath(csvFilePath)
        var intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/csv"
        val uri = UriUtils.file2Uri(zipFile)
        //AppUtils.grantUriPermission(this@DebugFeedbackActivity,intent,uri)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(Intent.EXTRA_TEXT, "abc")
        intent = Intent.createChooser(intent, "日志分享")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }
    //endregion

    //region 充电仓
    public fun getRingChargingCaseInfo(view: View) {
        CallBackUtils.ringChargingCaseInfoCallBack = object : RingChargingCaseInfoCallBack {
            override fun onRingChargingCaseInfo(ringChargingCaseInfoBean: RingChargingCaseInfoBean) {
                ToastDialog.showToast(this@RingTestActivity, getString(R.string.s744) + " : " + GsonUtils.toJson(ringChargingCaseInfoBean))
            }
        }
        ControlBleTools.getInstance().getRingChargingCaseInfo(baseSendCmdStateListener)
    }

    //endregion

}