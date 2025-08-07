package com.zjw.sdkdemo.function

import android.app.Dialog
import android.os.Bundle
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.manager.BleBCManager
import com.zhapp.ble.BleCommonAttributes
import com.zjw.sdkdemo.BraceletActivity
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBtTestBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import com.zjw.sdkdemo.utils.LoadingDialog
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2022/10/17.
 */
class BTTest : BaseActivity() {
    private val TAG = "BTTEST"
    private var loadingDialog: Dialog? = null
    private val binding by lazy { ActivityBtTestBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s334)
        setContentView(binding.root)
        inits()
        clicks()
    }

    private fun inits() {
        binding.etMac.setText(BraceletActivity.deviceAddress)

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
    }

    private fun clicks() {
        click(binding.btnBond) {
            loadingDialog = LoadingDialog.show(this)
            val mac = binding.etMac.text.toString().trim()
            BleBCManager.getInstance().createBond(mac, MyBondAListener(mac))
        }
    }

    inner class MyBondAListener(var mac: String) : BleBCManager.BondListener {

        override fun onWaiting() {
            LogUtils.i(TAG, "onWaiting $mac")

        }

        override fun onBondError(e: java.lang.Exception?) {
            LogUtils.i(TAG, "onBondError $mac $e")
            ToastDialog.showToast(this@BTTest,"配对异常：$e")
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }
        }

        override fun onBonding() {
            LogUtils.i(TAG, "onBonding $mac")
        }

        override fun onBondFailed() {
            LogUtils.i(TAG, "onBondFailed $mac")
            ToastDialog.showToast(this@BTTest,"配对失败")
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }
        }

        override fun onBondSucceeded() {
            LogUtils.i(TAG, "onBondSucceeded $mac")
            ToastDialog.showToast(this@BTTest,"配对成功")
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }
        }
    }


}