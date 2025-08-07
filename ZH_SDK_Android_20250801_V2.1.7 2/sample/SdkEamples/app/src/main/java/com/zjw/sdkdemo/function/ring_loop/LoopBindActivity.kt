package com.zjw.sdkdemo.function.ring_loop

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.OnClickListener
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.BindDeviceStateCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.RequestDeviceBindStateCallBack
import com.zhapp.ble.callback.UnbindDeviceCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.BraceletActivity
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityLoopBindBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState

class LoopBindActivity : BaseActivity(), OnClickListener {
    private var mCount = 0
    private var mInterval = 0L
    private var mDuration = 0L

    //成功次数
    private var sucNum = 0

    //失败次数
    private var fillNum = 0

    private var recordFilePath = ""

    var isStartLoop = false

    private val binding by lazy { ActivityLoopBindBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        observe()

    }

    @SuppressLint("SetTextI18n")
    fun observe() {
        BleConnectState.getInstance().observe(this, Observer<Int?> { integer ->
            when (integer) {
                BleCommonAttributes.STATE_CONNECTED -> {
                    binding.tvStatus.text = "连接状态：" + getString(R.string.ble_connected_tips)
                    saveLog(recordFilePath, binding.tvStatus.text.toString())
                    loop()
                }

                BleCommonAttributes.STATE_CONNECTING -> {
                    binding.tvStatus.text = "连接状态：" + getString(R.string.ble_connecting_tips)
                    saveLog(recordFilePath, binding.tvStatus.text.toString())
                }

                BleCommonAttributes.STATE_DISCONNECTED -> {
                    binding.tvStatus.text = "连接状态：" + getString(R.string.ble_disconnect_tips)
                    saveLog(recordFilePath, binding.tvStatus.text.toString())
                }

                BleCommonAttributes.STATE_TIME_OUT -> {
                    binding.tvStatus.text = "连接状态：" + getString(R.string.ble_connect_time_out_tips)
                    if (sucNum + fillNum < mCount) {
                        saveLog(recordFilePath, binding.tvStatus.text.toString())
                        binding.tvFailNum.text = "失败次数：${++fillNum}"
                        binding.tvCount.text = "循环总次数：${sucNum + fillNum}"
                        saveLog(recordFilePath, "总次数：$mCount 成功次数：$sucNum 失败次数：$fillNum")
                        loop()
                    } else {
                        binding.btnStart.text = "循环已完成"
                        ControlBleTools.getInstance().disconnect()
                    }
                }
            }
        })

        CallBackUtils.requestDeviceBindStateCallBack = RequestDeviceBindStateCallBack { state ->
            if (state) {
                binding.tvBindStatus.text = "绑定状态：" + "已绑定"
            } else {
                binding.tvBindStatus.text = "绑定状态：" + "未绑定"
                if (isStartLoop) {
                    Handler(Looper.myLooper()!!).postDelayed({
                        bindDevice()
                    }, 3000)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun initData() {
        recordFilePath = PathUtils.getAppDataPathExternalFirst() + "/ring/绑定行为日志" + TimeUtils.getNowString(TimeUtils.getSafeDateFormat("yyyyMMdd_HH:mm:ss")) + ".txt"
        binding.tvLog.text = getString(R.string.tv_log) + "\n" + recordFilePath

    }

    override fun onClick(v: View?) {
        try {
            mCount = binding.etCount.text.trim().toString().toInt()
            mInterval = binding.etInterval.text.trim().toString().toLong()
            mDuration = binding.etDuration.text.trim().toString().toLong()
        } catch (e: Exception) {
            ToastUtils.showShort("参数异常，重新填写")
            return
        }
        if (mCount < 1) {
            ToastUtils.showShort("参数异常，重新填写")
            return
        }
        if (mInterval < 1000) {
            ToastUtils.showShort("参数异常，重新填写")
            return
        }
        if (mDuration < 1000) {
            ToastUtils.showShort("参数异常，重新填写")
            return
        }
        if (!isStartLoop) {
            //开启循环
            isStartLoop = true
            binding.btnStart.text = "结束循环"
            binding.tvCount.text = "循环总次数：0"
            binding.tvSucNum.text = "成功次数：$sucNum"
            binding.tvFailNum.text = "失败次数：$fillNum"
            loop()
        } else {
            //结束循环
            isStartLoop = false
            binding.btnStart.text = "开始循环"
            mCount = 0
            mInterval = 0
            sucNum = 0
            fillNum = 0
        }
    }

    private fun loop() {
        if (isStartLoop) {
            if (sucNum + fillNum < mCount) {
                if (ControlBleTools.getInstance().isConnect) {
                    ControlBleTools.getInstance().requestDeviceBindState(null)
                } else {
                    ControlBleTools.getInstance().connect(BraceletActivity.deviceName, BraceletActivity.deviceAddress, BraceletActivity.deviceProtocol)
                }
            }else{
                binding.btnStart.text = "循环已完成"
            }
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BleBCManager.getInstance().dealCompanionDeviceActivityResult(requestCode, resultCode, data);
    }*/
    //endregion
    private fun bindDevice() {
        binding.tvBindStatus.text = "绑定状态：" + "绑定中"
        CallBackUtils.bindDeviceStateCallBack = BindDeviceStateCallBack { bindDeviceBean ->
            saveLog(recordFilePath, "BindDeviceState:${bindDeviceBean.deviceVerify}")
            if (bindDeviceBean.deviceVerify) {
                ControlBleTools.getInstance().sendAppBindResult("", object : SendCmdStateListener() {
                    override fun onState(state: SendCmdState) {
                        saveLog(recordFilePath, "sendAppBind onState :$state")
                        if (state != SendCmdState.SUCCEED) {
                            binding.tvFailNum.text = "失败次数：${++fillNum}"
                            binding.tvCount.text = "循环总次数：${sucNum + fillNum}"
                            saveLog(recordFilePath, "总次数：$mCount 成功次数：$sucNum 失败次数：$fillNum")
                            loop()
                        } else {
                            Handler(Looper.myLooper()!!).postDelayed({
                                binding.tvBindStatus.text = "绑定状态：" + "解绑中"
                                unBindDevice()
                            }, mDuration)
                        }
                    }
                })
            }
        }
        ControlBleTools.getInstance().bindDevice(object : SendCmdStateListener() {
            override fun onState(state: SendCmdState) {
                if (state != SendCmdState.SUCCEED) {
                    binding.tvFailNum.text = "失败次数：${++fillNum}"
                    binding.tvCount.text = "循环总次数：${sucNum + fillNum}"
                    saveLog(recordFilePath, "总次数：$mCount 成功次数：$sucNum 失败次数：$fillNum")
                    loop()
                }
            }
        })
    }

    fun unBindDevice() {
        CallBackUtils.unbindDeviceCallBack = UnbindDeviceCallBack { //清除通话蓝牙缓存信息  Clear call bluetooth cache information
            binding.tvBindStatus.text = "绑定状态：" + "未绑定"
            binding.tvSucNum.text = "成功次数：${++sucNum}"
            binding.tvCount.text = "循环总次数：${sucNum + fillNum}"
            saveLog(recordFilePath, "总次数：$mCount 成功次数：$sucNum 失败次数：$fillNum")
            Handler(Looper.myLooper()!!).postDelayed({
                loop()
            }, mInterval)
        }
        ControlBleTools.getInstance().unbindDevice(object : SendCmdStateListener() {
            override fun onState(state: SendCmdState) {
                if (state != SendCmdState.SUCCEED) {
                    binding.tvFailNum.text = "失败次数：${++fillNum}"
                    binding.tvCount.text = "循环总次数：${sucNum + fillNum}"
                    saveLog(recordFilePath, "总次数：$mCount 成功次数：$sucNum 失败次数：$fillNum")
                    loop()
                }
                saveLog(recordFilePath, "unbindDevice onState :$state")
            }
        })
    }
}