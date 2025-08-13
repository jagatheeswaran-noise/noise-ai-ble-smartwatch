package com.zjw.sdkdemo.function.measure

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ActiveMeasureParamsBean
import com.zhapp.ble.bean.ActiveMeasureResultBean
import com.zhapp.ble.bean.ActiveMeasureStatusBean
import com.zhapp.ble.bean.ActiveMeasuringBean
import com.zhapp.ble.callback.ActiveMeasureCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.WearActiveMeasureCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityActiveMeasureBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.LoadingDialog
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2023/3/10.
 */
class ActiveMeasureActivity : BaseActivity() {

    private var mType = 0  //0 戒指测量  1 手表测量

    private var measureType = 0

    private var loadingDialog: Dialog? = null

    private lateinit var timeoutHeadler: Handler

    private val binding by lazy { ActivityActiveMeasureBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        inits()
        callback()

        timeoutHeadler = Handler(Looper.getMainLooper())
    }

    private fun inits() {
        mType = intent.getIntExtra("mType", 0)
        measureType = intent.getIntExtra("type", 1)
        when (measureType) {
            ActiveMeasureCallBack.MeasureType.HEART_RATE.type -> {
                title = getString(R.string.s405) + getString(R.string.s445)
            }

            ActiveMeasureCallBack.MeasureType.BLOOD_OXYGEN.type -> {
                title = getString(R.string.s404) + getString(R.string.s445)
            }

            ActiveMeasureCallBack.MeasureType.STRESS_HRV.type -> {
                title = getString(R.string.s406) + getString(R.string.s445)
            }

            ActiveMeasureCallBack.MeasureType.BODY_TEMPERATURE.type -> {
                title = getString(R.string.s446) + getString(R.string.s445)
            }
        }

        binding.btnStop.isEnabled = false
    }

    /**
     * 开始测量
     */
    fun startMeasure(view: View) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastDialog.showToast(this, getString(R.string.s294))
            return
        }
        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
        binding.tvStatus.text = ""
        binding.tvMeasuring.text = ""
        binding.tvResult.text = ""


        loadingDialog = LoadingDialog.show(this)
        val params = ActiveMeasureParamsBean()
        params.measureType = measureType
        params.isSwitchMeasure = true
        if (mType == 0) {
            ControlBleTools.getInstance().activeMeasurementStart(params, object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> {
                            binding.btnStart.isEnabled = true
                            binding.btnStop.isEnabled = false
                            MyApplication.showToast(R.string.s221)
                            if (loadingDialog != null && loadingDialog!!.isShowing) {
                                loadingDialog!!.dismiss()
                            }
                        }
                    }
                }
            })
        } else {
            ControlBleTools.getInstance().wearActiveMeasurementStart(params, object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> {
                            binding.btnStart.isEnabled = true
                            binding.btnStop.isEnabled = false
                            MyApplication.showToast(R.string.s221)
                            if (loadingDialog != null && loadingDialog!!.isShowing) {
                                loadingDialog!!.dismiss()
                            }
                        }
                    }
                }
            })
        }
    }


    /**
     * 结束测量
     */
    fun stopMeasure(view: View? = null) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastDialog.showToast(this, getString(R.string.s294))
            return
        }
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
        val params = ActiveMeasureParamsBean()
        params.measureType = measureType
        params.isSwitchMeasure = false
        if (mType == 0) {
            ControlBleTools.getInstance().activeMeasurementStop(params, object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        } else {
            ControlBleTools.getInstance().wearActiveMeasurementStop(params, object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }
    }

    private fun callback() {
        if (mType == 0) {
            CallBackUtils.activeMeasureCallBack = object : ActiveMeasureCallBack {
                override fun onMeasureStatus(statusBean: ActiveMeasureStatusBean?) {
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                    if (statusBean != null) {
                        binding.tvStatus.text = statusBean.toString()

                        if (!statusBean.isSuccess) {
                            binding.btnStart.isEnabled = true
                            binding.btnStop.isEnabled = false
                        }

                        //TODO measureTime 超时
                        if (statusBean.measureTime != 0) {
                            timeoutHeadler.postDelayed({
                                binding.btnStart.isEnabled = true
                                binding.btnStop.isEnabled = false
                            }, statusBean.measureTime * 1000L)
                        }

                    }

                }

                @SuppressLint("SetTextI18n")
                override fun onMeasuring(measuringBean: ActiveMeasuringBean?) {
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                    if (measuringBean != null) {
                        binding.tvMeasuring.text = binding.tvMeasuring.text.toString() + measuringBean.toString() + "\n"
                    }
                }

                override fun onMeasureResult(resultBean: ActiveMeasureResultBean?) {
                    binding.btnStart.isEnabled = true
                    binding.btnStop.isEnabled = false
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                    if (resultBean != null) {
                        binding.tvResult.text = resultBean.toString()
                        when (resultBean.errorReason) {
                            //1.中途出现未佩戴 2.数据异常 3.....
                            ActiveMeasureCallBack.MeasureRrrorReason.SUCCEEDED.reason -> {
                                MyApplication.showToast(R.string.s220)
                            }

                            ActiveMeasureCallBack.MeasureRrrorReason.NOT_WRIST.reason -> {
                                MyApplication.showToast(getString(R.string.s221) + ":" + getString(R.string.s452))
                            }

                            ActiveMeasureCallBack.MeasureRrrorReason.DATA_ERROR.reason -> {
                                MyApplication.showToast(getString(R.string.s221) + ":" + getString(R.string.s453))
                            }

                        }
                    }

                }

            }
        } else {
            CallBackUtils.wearActiveMeasureCallBack = object : WearActiveMeasureCallBack {
                override fun onMeasureStatus(statusBean: ActiveMeasureStatusBean?) {
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                    if (statusBean != null) {
                        binding.tvStatus.text = statusBean.toString()

                        if (!statusBean.isSuccess) {
                            binding.btnStart.isEnabled = true
                            binding.btnStop.isEnabled = false
                        }

                        //TODO measureTime 超时
                        if (statusBean.measureTime != 0) {
                            timeoutHeadler.postDelayed({
                                binding.btnStart.isEnabled = true
                                binding.btnStop.isEnabled = false
                            }, statusBean.measureTime * 1000L)
                        }

                    }

                }

                @SuppressLint("SetTextI18n")
                override fun onMeasuring(measuringBean: ActiveMeasuringBean?) {
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                    if (measuringBean != null) {
                        binding.tvMeasuring.text = binding.tvMeasuring.text.toString() + measuringBean.toString() + "\n"
                    }
                }

                override fun onMeasureResult(resultBean: ActiveMeasureResultBean?) {
                    binding.btnStart.isEnabled = true
                    binding.btnStop.isEnabled = false
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                    if (resultBean != null) {
                        binding.tvResult.text = resultBean.toString()
                        when (resultBean.errorReason) {
                            //1.中途出现未佩戴 2.数据异常 3.....
                            ActiveMeasureCallBack.MeasureRrrorReason.SUCCEEDED.reason -> {
                                MyApplication.showToast(R.string.s220)
                            }

                            ActiveMeasureCallBack.MeasureRrrorReason.NOT_WRIST.reason -> {
                                MyApplication.showToast(getString(R.string.s221) + ":" + getString(R.string.s452))
                            }

                            ActiveMeasureCallBack.MeasureRrrorReason.DATA_ERROR.reason -> {
                                MyApplication.showToast(getString(R.string.s221) + ":" + getString(R.string.s453))
                            }

                        }
                    }

                }

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopMeasure()
    }

}