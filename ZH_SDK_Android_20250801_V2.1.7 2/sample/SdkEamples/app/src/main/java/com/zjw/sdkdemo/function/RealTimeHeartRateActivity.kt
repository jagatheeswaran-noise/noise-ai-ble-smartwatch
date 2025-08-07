package com.zjw.sdkdemo.function

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.RealTimeHeartRateConfigBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.RealTimeHeartRateCallback
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.databinding.ActivityRealTimeHeartRateBinding

class RealTimeHeartRateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRealTimeHeartRateBinding

    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRealTimeHeartRateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CallBackUtils.realTimeHeartRateCallback = realTimeCallback
        binding.switchHr.setOnCheckedChangeListener { _, isChecked ->
            if (ControlBleTools.getInstance().isConnect) {
                /**
                 * 设置实时心率配置
                 */
                ControlBleTools.getInstance().setRealTimeHeartRateConfig(
                    /**
                     * 参数1 实时心率开关
                     * 参数2 心率上报频率(范围为1..10,单位为秒)
                     * 参数3 心率上报时长(范围为20..240,单位为分钟)
                     */
                    RealTimeHeartRateConfigBean(isChecked, 3, 30),
                    object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
                            LogUtils.i("set real time config status=$state")
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ControlBleTools.getInstance().isConnect) {
            /**
             * 获取实时心率配置
             */
            ControlBleTools.getInstance().getRealTimeHeartRateConfig(null)
        }
    }

    private val realTimeCallback = object : RealTimeHeartRateCallback {
        /**
         * configBean.status     实时心率开关
         * configBean.frequency  心率上报频率(默认10,范围为1..10,单位为秒)
         * configBean.overtime   心率上报时长(默认20,范围为20..240,单位为分钟)
         */
        override fun onConfigResult(configBean: RealTimeHeartRateConfigBean?) {
            LogUtils.i("实时心率配置json=${gson.toJson(configBean)}")
        }

        /**
         * @param timeMillis 时间戳(秒)
         * @param hrValue 心率值
         */
        override fun onDataResult(timeMillis: Long, hrValue: Int) {
            LogUtils.i("实时心率值为$hrValue")
            binding.tvHrValue.text = hrValue.toString()
        }

    }
}