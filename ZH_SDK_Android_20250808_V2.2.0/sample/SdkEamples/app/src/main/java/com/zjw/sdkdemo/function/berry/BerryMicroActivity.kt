package com.zjw.sdkdemo.function.berry

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WidgetBean
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryMicroBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.ToastDialog
import java.util.Collections


/**
 * Created by Android on 2024/10/25.
 */
class BerryMicroActivity : BaseActivity() {
    private val binding: ActivityBerryMicroBinding by lazy { ActivityBerryMicroBinding.inflate(layoutInflater) }
    private val TAG = BerryMicroActivity::class.java.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s16)
        setContentView(binding.root)
        initListener()
        initDataCallBak()
    }

    private fun initListener() {
        click(binding.btnFindWear) {
            ControlBleTools.getInstance().sendFindWear(baseSendCmdStateListener)
        }

        click(binding.btnGetWidget) {
            ControlBleTools.getInstance().getWidgetList(baseSendCmdStateListener)
        }

        click(binding.btnSetWidget) {
            sendwidget()
        }

        click(binding.btnGetApplication) {
            ControlBleTools.getInstance().getApplicationList(baseSendCmdStateListener)
        }

        click(binding.btnSetApplication) {
            sendApplication()
        }

        click(binding.btnEnterPhotogragh){
            ControlBleTools.getInstance().sendPhonePhotogragh(0,baseSendCmdStateListener)
        }

        click(binding.btnExitPhotogragh){
            ControlBleTools.getInstance().sendPhonePhotogragh(1,baseSendCmdStateListener)
        }

        click(binding.btnSetSportWidget){
            sendSportWidget()
        }

        click(binding.btnGetSportWidget){
            ControlBleTools.getInstance().getSportWidgetSortList(baseSendCmdStateListener)
        }
    }

    private fun sendwidget() {
        var widgets: ArrayList<WidgetBean?>?=null
        try {
            widgets = DeviceSettingLiveData.getInstance().widgetList.value as ArrayList<WidgetBean?>?
        }catch (e:Exception){
            e.printStackTrace()
        }
        if (widgets.isNullOrEmpty()) {
            Toast.makeText(this@BerryMicroActivity, getString(R.string.s219), Toast.LENGTH_LONG).show()
            return
        }

        //重新排序直达卡片顺序
        var ws: String = binding.etWidget.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(ws)) {
            //3,4交换
            if (!widgets[2]!!.sortable || !widgets[3]!!.sortable) {
                Toast.makeText(this@BerryMicroActivity, getString(R.string.s222), Toast.LENGTH_LONG).show()
                return
            }
            Collections.swap(widgets, 2, 3)
            //重新赋值排序字段
            for (i in widgets.indices) {
                widgets[i]!!.order = i + 1
            }
        } else {
            try {
                if (ws.contains(",")) {
                    val ids = ws.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    widgets = ArrayList()
                    for (i in ids.indices) {
                        val widgetBean = WidgetBean()
                        widgetBean.functionId = ids[i].toInt()
                        widgetBean.order = i + 1
                        widgetBean.isEnable = i != ids.size - 1
                        widgetBean.haveHide = true
                        widgetBean.sortable = true
                        widgets.add(widgetBean)
                    }
                } else {
                    Toast.makeText(this@BerryMicroActivity, getString(R.string.s238), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@BerryMicroActivity, getString(R.string.s238), Toast.LENGTH_LONG).show()
            }
        }
        com.blankj.utilcode.util.LogUtils.d("setWidgetList:" + com.blankj.utilcode.util.GsonUtils.toJson(widgets))
        ControlBleTools.getInstance().setWidgetList(widgets, baseSendCmdStateListener)
    }

    private fun sendApplication() {
        var apps: ArrayList<WidgetBean?>?=null
        try {
            apps = DeviceSettingLiveData.getInstance().applicationList.value as ArrayList<WidgetBean?>?
        }catch (e:Exception){
            e.printStackTrace()
        }
        if (apps.isNullOrEmpty()) {
            Toast.makeText(this@BerryMicroActivity, getString(R.string.s219), Toast.LENGTH_LONG).show()
            return
        }

        //重新排序应用列表
        //3,4交换
        if (!apps[2]!!.sortable || !apps[3]!!.sortable) {
            Toast.makeText(this@BerryMicroActivity, getString(R.string.s222), Toast.LENGTH_LONG).show()
            return
        }
        Collections.swap(apps, 2, 3)

        //重新赋值排序字段
        for (i in apps.indices) {
            apps[i]!!.order = i + 1
        }
        LogUtils.d("setApplicationList:" + GsonUtils.toJson(apps))
        ControlBleTools.getInstance().setApplicationList(apps, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> Log.i(TAG, getString(R.string.s220))
                    else -> Log.i(TAG, getString(R.string.s221))
                }
            }
        })
    }

    private fun sendSportWidget() {
        var widgets: ArrayList<WidgetBean?>?=null
        try {
            widgets = DeviceSettingLiveData.getInstance().sportWidgetList.value as ArrayList<WidgetBean?>?
        }catch (e:Exception){
            e.printStackTrace()
        }
        if (widgets.isNullOrEmpty()) {
            Toast.makeText(this@BerryMicroActivity, getString(R.string.s219), Toast.LENGTH_LONG).show()
            return
        }
        LogUtils.d(TAG, "Get device Data: " + GsonUtils.toJson(widgets))
        //重新排序运动
        //3,4交换
        if (!widgets[2]!!.sortable || !widgets[3]!!.sortable) {
            Toast.makeText(this@BerryMicroActivity, getString(R.string.s222), Toast.LENGTH_LONG).show()
            return
        }
        Collections.swap(widgets, 2, 3)

        //重新赋值排序字段
        for (i in widgets.indices) {
            widgets[i]!!.order = i + 1
            //widgets.get(i).isEnable = false (禁用 list启用最少1个最大10个) (disable list enable min 1 max 10)
        }
        LogUtils.d(TAG, "Set device Data:${GsonUtils.toJson(widgets)}")
        ControlBleTools.getInstance().setSportWidgetSortList(widgets, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> Log.i(TAG, getString(R.string.s220))
                    else -> Log.i(TAG, getString(R.string.s221))
                }
            }
        })
    }

    private fun initDataCallBak() {

        /**
         * 获取应用列表
         */
        DeviceSettingLiveData.getInstance().applicationList.observe(this, object : Observer<List<WidgetBean?>?> {
            override fun onChanged(widgetBeans: List<WidgetBean?>?) {
                if (widgetBeans != null) {
                    Log.i(TAG, "getApplicationList == $widgetBeans")
                    ToastDialog.showToast(this@BerryMicroActivity, "${getString(R.string.s253)} $widgetBeans")
                }
            }
        })

        /**
         * 获取首页卡片
         */
        DeviceSettingLiveData.getInstance().widgetList.observe(this, object : Observer<List<WidgetBean?>?> {
            override fun onChanged(widgetBeans: List<WidgetBean?>?) {
                if (widgetBeans != null) {
                    Log.i(TAG, "getWidgetList == $widgetBeans")
                    ToastDialog.showToast(this@BerryMicroActivity, getString(R.string.s218) + widgetBeans)
                }
            }
        })

        /**
         * 运动排序
         */
        DeviceSettingLiveData.getInstance().sportWidgetList.observe(this, object : Observer<List<WidgetBean?>?> {
            override fun onChanged(widgetBeans: List<WidgetBean?>?) {
                if (widgetBeans != null) {
                    Log.i(TAG, "getSportWidgetList == $widgetBeans")
                    ToastDialog.showToast(this@BerryMicroActivity, getString(R.string.s297) + widgetBeans)
                }
            }
        })
    }




}