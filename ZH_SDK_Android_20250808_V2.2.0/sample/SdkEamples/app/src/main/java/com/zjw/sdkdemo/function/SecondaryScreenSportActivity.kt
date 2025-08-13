package com.zjw.sdkdemo.function

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.SecondaryScreenWearDataBean
import com.zhapp.ble.bean.SportRequestBean
import com.zhapp.ble.bean.SportResponseBean
import com.zhapp.ble.bean.SportStatusBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.SecondaryScreenSportCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivitySecondaryScreenSportBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2023/6/16.
 * 副屏运动
 */
class SecondaryScreenSportActivity : BaseActivity() {
    //运动开始时间
    private var sportStartTime = 0L

    //运动类型
    private var sportType = 0

    //运动状态   1：运动开始(运动中)， 2：运动暂停 ， 3：运动重新开始(运动中)，4：运动结束
    private var sportStatus = 0

    //运动时长 秒
    private var sportDuration = 0

    private val binding by lazy { ActivitySecondaryScreenSportBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s471)
        setContentView(binding.root)
        initView()
        initCallBack()
        events()
    }

    private fun initView() {

    }

    private fun getSportStatusStr(): String {
        return when (sportStatus) {
            0 -> getString(R.string.s478)
            1, 3 -> getString(R.string.s479)
            2 -> getString(R.string.s480)
            4 -> getString(R.string.s481)
            else -> getString(R.string.s478)
        }
    }

    private fun refSportStatusButton() {
        binding.btnStart.isEnabled = sportStatus == 4
        binding.btnPause.isEnabled = sportStatus == 1 || sportStatus == 3
        binding.btnResume.isEnabled = sportStatus == 2
        binding.btnStop.isEnabled = sportStatus == 2
    }

    private fun initCallBack() {
        CallBackUtils.secondaryScreenSportCallBack = object : SecondaryScreenSportCallBack {
            override fun onSecondaryScreenSportResponseBean(bean: SportResponseBean?) {
                LogUtils.d("onSecondaryScreenSportResponseBean -->" + GsonUtils.toJson(bean))
                //bean.code
                //状态回应 0 OK; 1 设备正忙; 2 恢复/暂停类型不匹配; 3 没有位置权限; 4 运动不支持; 5 精确gps关闭或后台无gps许可; 6 充电中; 7 低电量 ; 8 定位失败； 9 运动前空间将满--当不能储存运动三小时的数据或运动记录条数大于15条；
                //10 未知； 11 运动前空间已满--运动记录条数大于20后flash存储空间不能继续存储 ； 12 设备处于运动中

                if (bean != null) {
                    if (sportStatus == 0) { //未开始运动，response是开始运动的回复
                        if (bean.code == SecondaryScreenSportCallBack.ResponseCode.OK.code) {
                            sportStatus = SecondaryScreenSportCallBack.SportState.START.state
                            binding.tvSportStatus.text = getSportStatusStr()
                            binding.llSportStatus.visibility = View.VISIBLE
                            refSportStatusButton()
                        } else {
                            ToastDialog.showToast(this@SecondaryScreenSportActivity, getString(R.string.s476) + " code == " + bean.code)

                            sportStatus = 4
                            binding.tvSportStatus.text = getSportStatusStr()
                            refSportStatusButton()
                        }
                    }
                    //设备回复app运动状态改变
                    if (sportStatus != 0) {
                        LogUtils.d("SportResponseBean:$bean")
                    }
                }
            }

            override fun onSecondaryScreenSportRequestBean(bean: SportRequestBean?) {
                //设备上报副屏运动状态改变
                LogUtils.d("onSecondaryScreenSportRequestBean -->" + GsonUtils.toJson(bean))
                if (bean != null) {
                    replyRequest()
                    if (bean.timestamp != sportStartTime || bean.sportType != sportType) {
                        MyApplication.showToast(R.string.s221)
                        return
                    }
                    if (bean.state == SecondaryScreenSportCallBack.SportState.STOP.state && bean.stopErrorCode != SecondaryScreenSportCallBack.StopErrorCode.NORMAL_END.code) {
                        showLessTimeStopHintDialog()
                        return
                    }
                    sportStatus = bean.state
                    binding.tvSportStatus.text = getSportStatusStr()
                    refSportStatusButton()
                }

            }

            override fun onSecondaryScreenWearData(bean: SecondaryScreenWearDataBean?) {
                //副屏运动过程中数据
                LogUtils.d("onSecondaryScreenWearData -->" + GsonUtils.toJson(bean))
                if (bean != null) {
                    binding.tvWearData.text = bean.toString()

                    sportDuration = bean.sportTimestamp
                }

            }

            override fun onSportStatus(bean: SportStatusBean?) {
                //App查询设备副屏运动状态，设备回复
                LogUtils.d("onSportStatus -->" + GsonUtils.toJson(bean))
                if (bean != null) {
                    if (bean.isAppLaunched && bean.sportType == sportType && bean.timestamp == sportStartTime) {
                        sportStatus = if (bean.isPaused) 2 else 3
                        binding.tvSportStatus.text = getSportStatusStr()
                        refSportStatusButton()
                    }
                }
            }

        }

        BleConnectState.getInstance().observe(this, object : Observer<Int> {
            override fun onChanged(t: Int) {
                t?.let {
                    if (t == BleCommonAttributes.STATE_DISCONNECTED) {
                        sportStatus = 4
                        binding.tvWearData.text = ""
                        binding.tvSportStatus.text = getSportStatusStr()
                        refSportStatusButton()
                    }
                }
            }

        })
    }

    /**
     * 回复设备request
     */
    private fun replyRequest() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastDialog.showToast(this, getString(R.string.s294))
            return
        }
        val response = SportResponseBean()
        /**
         * 状态回应 0 OK; 1 设备正忙; 2 恢复/暂停类型不匹配; 3 没有位置权限; 4 运动不支持; 5 精确gps关闭或后台无gps许可; 6 充电中; 7 低电量 ; 8 定位失败； 9 运动前空间将满--当不能储存运动三小时的数据或运动记录条数大于15条；
         * 10 未知； 11 运动前空间已满--运动记录条数大于20后flash存储空间不能继续存储 ； 12 设备处于运动中
         */
        response.code = SecondaryScreenSportCallBack.ResponseCode.OK.code
        //GPS状态 低 0; 中 1; 高 2; 未知 10;
        response.gpsAccuracy = 0
        response.selectVersion = 1
        ControlBleTools.getInstance().replyDevSecondaryScreenSportRequest(response, object : SendCmdStateListener(this.lifecycle) {
            override fun onState(state: SendCmdState?) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                    else -> MyApplication.showToast(R.string.s221)
                }
            }
        })

    }

    /**
     * 结束原因运动时长小于1分钟弹窗提示
     */
    private var lessTimeStopHintDialog: Dialog? = null
    private fun showLessTimeStopHintDialog() {
        lessTimeStopHintDialog = Dialog(this)
        lessTimeStopHintDialog?.setCancelable(false)
        lessTimeStopHintDialog?.setContentView(layoutInflater.inflate(R.layout.dialog_sss_lesstime, null))
        val params = lessTimeStopHintDialog?.window?.attributes
        params?.width = (ScreenUtils.getScreenWidth() * 0.9).toInt()
        params?.height = (ScreenUtils.getScreenHeight() * 0.9).toInt()
        lessTimeStopHintDialog?.window?.attributes = params
        lessTimeStopHintDialog?.findViewById<Button>(R.id.btnCancel)?.setOnClickListener {
            lessTimeStopHintDialog?.dismiss()
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@setOnClickListener
            }
            ControlBleTools.getInstance()
                .secondaryScreenSportRequest(SportRequestBean.getResumePhoneSportRequest(sportType, sportStartTime), object : SendCmdStateListener(this.lifecycle) {
                    override fun onState(state: SendCmdState?) {
                        when (state) {
                            SendCmdState.SUCCEED -> {
                                MyApplication.showToast(R.string.s220)

                                sportStatus = 3
                                binding.tvSportStatus.text = getSportStatusStr()
                                refSportStatusButton()
                            }

                            else -> MyApplication.showToast(R.string.s221)
                        }
                    }
                })
        }
        lessTimeStopHintDialog?.findViewById<Button>(R.id.btnConfirm)?.setOnClickListener {
            lessTimeStopHintDialog?.dismiss()
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@setOnClickListener
            }
            //正常结束运动
            ControlBleTools.getInstance()
                .secondaryScreenSportRequest(SportRequestBean.getStopPhoneSportRequest(sportType, sportStartTime), object : SendCmdStateListener(this.lifecycle) {
                    override fun onState(state: SendCmdState?) {
                        when (state) {
                            SendCmdState.SUCCEED -> {
                                MyApplication.showToast(R.string.s220)

                                sportStatus = 4
                                binding.tvSportStatus.text = getSportStatusStr()
                                refSportStatusButton()
                            }

                            else -> MyApplication.showToast(R.string.s221)
                        }
                    }
                })
        }
        if (!isDestroyed) {
            lessTimeStopHintDialog?.show()
        }
    }

    private fun events() {
        //APP断连，重连后查询设备副屏运动状态
        click(binding.btnDevConnect) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@click
            }
            ControlBleTools.getInstance().getSportStatus(object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }

        //开始运动
        click(binding.btnStart) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@click
            }

            sportType = try {
                binding.etType.text.toString().trim().toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                ToastDialog.showToast(this, getString(R.string.s238))
                3
            }



            sportStartTime = System.currentTimeMillis() / 1000L

            ControlBleTools.getInstance()
                .secondaryScreenSportRequest(SportRequestBean.getStartPhoneSportRequest(sportType, sportStartTime), object : SendCmdStateListener(this.lifecycle) {
                    override fun onState(state: SendCmdState?) {
                        when (state) {
                            SendCmdState.SUCCEED -> {
                                MyApplication.showToast(R.string.s220)

                                sportStatus = 1
                                binding.tvSportStatus.text = getSportStatusStr()
                                refSportStatusButton()
                            }

                            else -> MyApplication.showToast(R.string.s221)
                        }
                    }
                })
        }

        //暂停运动
        click(binding.btnPause) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@click
            }
            if (sportStatus == 1 || sportStatus == 3) {
                ControlBleTools.getInstance()
                    .secondaryScreenSportRequest(SportRequestBean.getPausePhoneSportRequest(sportType, sportStartTime), object : SendCmdStateListener(this.lifecycle) {
                        override fun onState(state: SendCmdState?) {
                            when (state) {
                                SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                                else -> MyApplication.showToast(R.string.s221)
                            }
                        }
                    })

                sportStatus = 2
                binding.tvSportStatus.text = getSportStatusStr()
                refSportStatusButton()
            } else {
                MyApplication.showToast(R.string.s238)
            }
        }

        //继续运动
        click(binding.btnResume) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@click
            }
            if (sportStatus == 2) {
                ControlBleTools.getInstance()
                    .secondaryScreenSportRequest(SportRequestBean.getResumePhoneSportRequest(sportType, sportStartTime), object : SendCmdStateListener(this.lifecycle) {
                        override fun onState(state: SendCmdState?) {
                            when (state) {
                                SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                                else -> MyApplication.showToast(R.string.s221)
                            }
                        }
                    })

                sportStatus = 3
                binding.tvSportStatus.text = getSportStatusStr()
                refSportStatusButton()
            } else {
                MyApplication.showToast(R.string.s238)
            }
        }

        //结束运动
        click(binding.btnStop) {
            if (!ControlBleTools.getInstance().isConnect) {
                ToastDialog.showToast(this, getString(R.string.s294))
                return@click
            }
            if (sportStatus == 2) {
                if (sportDuration < 60) {
                    //结束原因运动时长小于1分钟
                    ControlBleTools.getInstance()
                        .secondaryScreenSportRequest(
                            SportRequestBean.getErrorStopPhoneSportRequest(
                                sportType,
                                sportStartTime,
                                SecondaryScreenSportCallBack.StopErrorCode.SPORT_DURATION_NO_METTING.code
                            ), object : SendCmdStateListener(this.lifecycle) {
                                override fun onState(state: SendCmdState?) {
                                    when (state) {
                                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                                        else -> MyApplication.showToast(R.string.s221)
                                    }
                                }
                            })
                    showLessTimeStopHintDialog()

                    sportStatus = 4
                    binding.tvSportStatus.text = getSportStatusStr()
                    refSportStatusButton()
                } else {
                    //正常结束运动
                    ControlBleTools.getInstance()
                        .secondaryScreenSportRequest(SportRequestBean.getStopPhoneSportRequest(sportType, sportStartTime), object : SendCmdStateListener(this.lifecycle) {
                            override fun onState(state: SendCmdState?) {
                                when (state) {
                                    SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                                    else -> MyApplication.showToast(R.string.s221)
                                }
                            }
                        })

                    sportStatus = 4
                    binding.tvSportStatus.text = getSportStatusStr()
                    refSportStatusButton()
                }
            } else {
                MyApplication.showToast(R.string.s238)
            }
        }
    }
}