package com.zjw.sdkdemo.function.berry

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WorldClockBean
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityBerryTimeSetBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.ToastDialog
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Created by Android on 2024/10/29.
 */
class BerryTimeSetActivity : BaseActivity() {
    private val TAG = BerryTimeSetActivity::class.java.simpleName
    private val binding: ActivityBerryTimeSetBinding by lazy { ActivityBerryTimeSetBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s117)
        setContentView(binding.root)
        initListener()
        initWorldClockData()
    }

    private fun initListener() {
        click(binding.btnSetTime) {
            ControlBleTools.getInstance().setTime(System.currentTimeMillis(), baseSendCmdStateListener)
        }

        click(binding.btnSetTime2) {
            ControlBleTools.getInstance().setTime(System.currentTimeMillis(), binding.chbTimeFormat.isChecked, baseSendCmdStateListener)
        }

        click(binding.btnSetWorldClock) {
            sendWorldClock()
        }

        click(binding.btnGetWorldClock) {
            ControlBleTools.getInstance().getWorldClockList(baseSendCmdStateListener)
        }
    }

    private fun sendWorldClock() {
        val list: MutableList<WorldClockBean> = ArrayList()


        //本地获取
        /**
         * @see .toTimezoneInt
         */
        val stringArray = resources.getStringArray(R.array.world_clock_zone_name)
        for (i in 0..4) { // max = 5  最大值由设备或者产品决定 The maximum value is determined by the device or product
            val arr = stringArray[i]
            val split = arr.split("\\*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val bean = WorldClockBean()
            bean.cityName = split[1]
            bean.offset = toTimezoneInt(split[0]) / 15 //Time zone minutes divided by 15
            list.add(bean)
        }


        //当前时钟 ---------
        val worldClockBean = WorldClockBean()
        worldClockBean.cityName = "Beijing" //城市名
        worldClockBean.offset = TimeZone.getDefault().rawOffset / 60 / 1000 / 15 //Time zone minutes divided by 15
        LogUtils.d("offset : " + worldClockBean.offset)
        list.add(worldClockBean)

        ControlBleTools.getInstance().setWorldClockList(list, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                    else -> MyApplication.showToast(R.string.s221)
                }
            }
        })
    }

    private fun initWorldClockData() {
        DeviceSettingLiveData.getInstance().worldClockList.observe(this, object : Observer<List<WorldClockBean?>?> {
            override fun onChanged(worldClockBeans: List<WorldClockBean?>?) {
                if (worldClockBeans == null) return
                Log.i(TAG, "worldClockBeans == $worldClockBeans")
                ToastDialog.showToast(
                    this@BerryTimeSetActivity, "${getString(R.string.s303)}：$worldClockBeans"
                )
            }
        })
    }

    private fun toTimezoneInt(s: String): Int {
        var value = 0
        if (s.contains("+")) {
            val replace = s.replace("+", "")
            val date = str2Date(replace, "HH:mm")
            value = date!!.hours * 60 + date!!.minutes
        } else if (s.contains("-")) {
            val replace = s.replace("-", "")
            val date = str2Date(replace, "HH:mm")
            value = -(date!!.hours * 60 + date!!.minutes)
        }
        return value
    }

    fun str2Date(time: String?, pattern: String?): Date? {
        @SuppressLint("SimpleDateFormat") val mFormatter = SimpleDateFormat(pattern)
        try {
            return mFormatter.parse(time)
        } catch (e: ParseException) {
            e.printStackTrace()
            return null
        }
    }
}