package com.zjw.sdkdemo.function

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DevSportInfoBean
import com.zhapp.ble.bean.RingSportDataBean
import com.zhapp.ble.bean.RingSportStatusBean
import com.zhapp.ble.bean.SendRingSportStatusBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.RingSportCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.parsing.SportParsing
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityRingSportBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.DevSportManager
import com.zjw.sdkdemo.utils.DevSportManager.TestSportDataListener
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2023/12/20.
 */
class RingSportActivity : BaseActivity() {

    //运动开始时间
    private var sportStartTime = 0L

    //运动类型
    //206 室内跑步-戒指  Indoor running-ring
    //207 户外跑步-戒指  Outdoor running-ring
    //208 户外健走-戒指  Outdoor walking-ring
    //209 室内骑行-戒指  Indoor Cycling-Ring
    //210 室外骑行-戒指  Outdoor Cycling-Ring
    //211 羽毛球-戒指  Badminton-Ring
    //212 网球-戒指	Tennis-Ring
    //213 足球-戒指	Soccer-Ring
    //214 板球-戒指	Cricket-Ring
    //215 瑜伽-戒指	Yoga-Ring
    private var sportType = 206

    //运动状态   0 无运动  1 开始  2 暂停  3 继续  4 结束
    private var sportStatus = 0

    private val binding by lazy { ActivityRingSportBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s537)
        setContentView(binding.root)
        initClicks()
        initBacks()
        getRingSportStatus()
    }

    private fun initClicks() {
        click(binding.btnDevConnect) {
            getRingSportStatus()
        }

        click(binding.btnStart) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.ble_disconnect_tips))
                return@click
            }
            sportType = try {
                binding.etType.text.toString().trim().toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                ToastDialog.showToast(this, getString(R.string.s238))
                0
            }
            if (sportTypeIsIllegal(sportType)) {
                ToastDialog.showToast(this, getString(R.string.s238))
                return@click
            }
            sportStartTime = System.currentTimeMillis() / 1000
            val bean = SendRingSportStatusBean(sportType, RingSportCallBack.RingSportStatus.SPORT_STATUS_START.status, sportStartTime)
            if (!SportParsing.isData10(bean.getSportType()) && !SportParsing.isData11(bean.getSportType())) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            ControlBleTools.getInstance().sendRingSportStatus(bean, object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> {
                            LogUtils.d("state == $state")
                            MyApplication.showToast(R.string.s221)
                        }
                    }
                }
            })

        }

        click(binding.btnPause) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.ble_disconnect_tips))
                return@click
            }
            if (sportTypeIsIllegal(sportType)) {
                ToastDialog.showToast(this, getString(R.string.s238))
                return@click
            }
            if (sportStartTime == 0L) {
                ToastDialog.showToast(this, getString(R.string.s238))
                return@click
            }
            val bean = SendRingSportStatusBean(sportType, RingSportCallBack.RingSportStatus.SPORT_STATUS_PAUSE.status, sportStartTime)
            if (!SportParsing.isData10(bean.getSportType()) && !SportParsing.isData11(bean.getSportType())) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            ControlBleTools.getInstance().sendRingSportStatus(bean, object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }

        click(binding.btnResume) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.ble_disconnect_tips))
                return@click
            }
            if (sportTypeIsIllegal(sportType)) {
                ToastDialog.showToast(this, getString(R.string.s238))
                return@click
            }
            if (sportStartTime == 0L) {
                ToastDialog.showToast(this, getString(R.string.s238))
                return@click
            }
            val bean = SendRingSportStatusBean(sportType, RingSportCallBack.RingSportStatus.SPORT_STATUS_RESUME.status, sportStartTime)
            if (!SportParsing.isData10(bean.getSportType()) && !SportParsing.isData11(bean.getSportType())) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            ControlBleTools.getInstance().sendRingSportStatus(bean, object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }

        click(binding.btnStop) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.ble_disconnect_tips))
                return@click
            }
            if (sportTypeIsIllegal(sportType)) {
                ToastDialog.showToast(this, getString(R.string.s238))
                return@click
            }
            //结束时，时间传当前时间戳  At the end, the time is passed to the current timestamp.
            val bean = SendRingSportStatusBean(sportType, RingSportCallBack.RingSportStatus.SPORT_STATUS_END.status, System.currentTimeMillis() / 1000)
            if (!SportParsing.isData10(bean.getSportType()) && !SportParsing.isData11(bean.getSportType())) {
                ToastUtils.showShort(getString(R.string.s238))
                return@click
            }
            ControlBleTools.getInstance().sendRingSportStatus(bean, object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }

        if (binding.tvWearData != null) {
            binding.tvWearData.setOnLongClickListener(View.OnLongClickListener {
                ClipboardUtils.copyText(binding.tvWearData.getText().toString().trim { it <= ' ' })
                ToastUtils.showShort("copy complete")
                false
            })
        }

    }

    private fun getRingSportStatus() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastDialog.showToast(this, getString(R.string.ble_disconnect_tips))
            return
        }
        ControlBleTools.getInstance().getRingSportStatus(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
            override fun onState(state: SendCmdState?) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                    else -> MyApplication.showToast(R.string.s221)
                }
            }
        })

    }

    private fun initBacks() {
        CallBackUtils.ringSportCallBack = object : RingSportCallBack {
            override fun onRingSportStatus(bean: RingSportStatusBean?) {
                if (bean != null) {

                    sportType = bean.sportType
                    sportStartTime = bean.startTime
                    sportStatus = bean.sportStatus

                    refSportStatusButton()
                    binding.tvSportStatus.text = getSportStatusStr()

                    //’bean.startResult‘ -> 是指APP开启运动的结果  It refers to the result of starting sport on the APP
                    //SPORT_START_RESULT_NONE  -> 正常开启运动 Start sport normally
                    if (bean.startResult != RingSportCallBack.RingSportStartResult.SPORT_START_RESULT_NONE.result) {
                        when (bean.startResult) {
                            //SPORT_START_RESULT_LOW_POWER  -> 设备低电量，无法开启运动 The device has low battery and cannot start sport.
                            RingSportCallBack.RingSportStartResult.SPORT_START_RESULT_LOW_POWER.result -> {
                                ToastDialog.showToast(this@RingSportActivity, getString(R.string.s538) + ":" + getString(R.string.s540))
                            }
                            //SPORT_START_RESULT_LOW_POWER  -> 设备未佩戴，无法开启运动 The device is not worn and sport cannot be started.
                            RingSportCallBack.RingSportStartResult.SPORT_START_RESULT_UN_WEAR.result -> {
                                ToastDialog.showToast(this@RingSportActivity, getString(R.string.s538) + ":" + getString(R.string.s541))
                            }
                            //SPORT_START_RESULT_CHARGING  -> 设备充电中，无法开启运动 The device is charging and sport cannot be started.
                            RingSportCallBack.RingSportStartResult.SPORT_START_RESULT_CHARGING.result -> {
                                ToastDialog.showToast(this@RingSportActivity, getString(R.string.s538) + ":" + getString(R.string.s542))
                            }
                            //SPORT_START_RESULT_SPORTING -> 设备已经在运动中，无法开启新的运动 The device is already in motion and a new motion cannot be started
                            RingSportCallBack.RingSportStartResult.SPORT_START_RESULT_SPORTING.result -> {
                                ToastDialog.showToast(this@RingSportActivity, getString(R.string.s538) + ":" + getString(R.string.s551))
                            }
                        }
                    }

                    //bean.endReason -> 是指设备运动结束的原因 Refers to the reason why the equipment sport ends
                    if (sportStatus == RingSportCallBack.RingSportStatus.SPORT_STATUS_END.status) {
                        //SPORT_END_REASON_NONE  -> 正常结束运动 End sport normally
                        if (bean.endReason != RingSportCallBack.RingSportEndReason.SPORT_END_REASON_NONE.reason) {
                            when (bean.endReason) {
                                //SPORT_END_REASON_LOW_POWER  -> 设备5%低电量，导致异常结束运动 The device has 5% low battery, causing the sport to end abnormally.
                                RingSportCallBack.RingSportEndReason.SPORT_END_REASON_LOW_POWER.reason -> {
                                    ToastDialog.showToast(this@RingSportActivity, getString(R.string.s539) + ":" + getString(R.string.s540))
                                }
                                //SPORT_END_REASON_TIMEOUT  -> 设备运动8小时，导致异常结束运动 The device was in motion for 8 hours, resulting in an abnormal end of motion.
                                RingSportCallBack.RingSportEndReason.SPORT_END_REASON_TIMEOUT.reason -> {
                                    ToastDialog.showToast(this@RingSportActivity, getString(R.string.s539) + ":" + getString(R.string.s543))
                                }
                                //SPORT_END_REASON_NO_MEMORY  -> 设备内测低，导致异常结束运动 The internal measurement of the equipment is low, causing the sport to end abnormally.
                                RingSportCallBack.RingSportEndReason.SPORT_END_REASON_NO_MEMORY.reason -> {
                                    ToastDialog.showToast(this@RingSportActivity, getString(R.string.s539) + ":" + getString(R.string.s544))
                                }
                                //SPORT_END_REASON_CHARGE  ->  设备充电中，导致异常结束运动 The device is charging, causing the movement to end abnormally.
                                RingSportCallBack.RingSportEndReason.SPORT_END_REASON_CHARGE.reason -> {
                                    ToastDialog.showToast(this@RingSportActivity, getString(R.string.s539) + ":" + getString(R.string.s542))
                                }
                            }
                        }

                        if (bean.isSportNoSync) {
                            // 获取运动数据  Get sports data
                            DevSportManager.getInstance().getFitnessSportIdsData()
                        }
                    }
                }
            }

            //运动中数据  Data in Sporting
            override fun onRingSportData(bean: RingSportDataBean?) {
                binding.tvWearSportData.text = GsonUtils.toJson(bean)
            }
        }

        DevSportManager.getInstance().setListener(
            object : TestSportDataListener {
                @SuppressLint("SetTextI18n")
                override fun onSportData(data: DevSportInfoBean) {
                    val old: String = binding.tvWearData.getText().toString().trim { it <= ' ' }
                    if (TextUtils.isEmpty(old)) {
                        binding.tvWearData.setText(getString(R.string.s166) + "---------》\n" + GsonUtils.toJson(data))
                    } else {
                        binding.tvWearData.setText("$old\n---------》\n${GsonUtils.toJson(data)}")
                    }
                }
            })
    }

    private fun refSportStatusButton() {
        //0 无运动  1 开始  2 暂停  3 继续  4 结束
        binding.btnStart.isEnabled = true
        binding.btnPause.isEnabled = sportStatus == 1 || sportStatus == 3
        binding.btnResume.isEnabled = sportStatus == 2
        binding.btnStop.isEnabled = sportStatus == 2
    }

    private fun getSportStatusStr(): String {
        //0 无运动  1 开始  2 暂停  3 继续  4 结束
        return when (sportStatus) {
            0 -> getString(R.string.s478)
            1, 3 -> getString(R.string.s479)
            2 -> getString(R.string.s480)
            4 -> getString(R.string.s481)
            else -> getString(R.string.s478)
        }
    }

    private fun sportTypeIsIllegal(sportType: Int): Boolean {
        return !SportParsing.isData10(sportType) && !SportParsing.isData11(sportType)
    }


}