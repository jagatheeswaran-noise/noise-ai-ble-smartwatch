package com.zjw.sdkdemo.function.berry

import android.os.Bundle
import com.zhapp.ble.ControlBleTools
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerrySysBinding
import com.zjw.sdkdemo.function.language.BaseActivity

/**
 * Created by Android on 2024/10/24.
 */
class BerrySystemActivity : BaseActivity() {
    private val binding : ActivityBerrySysBinding by lazy { ActivityBerrySysBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s38)
        setContentView(binding.root)
        initListener()
    }


    private fun initListener() {
        click(binding.btnReboot) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().rebootDevice(baseSendCmdStateListener)
            }
        }

        click(binding.btnShutdown) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().shutdownDevice(baseSendCmdStateListener)
            }
        }

//        click(binding.btnTestMode){
//
//        }
//
//        click(binding.btnUserMode){
//
//        }
    }
}