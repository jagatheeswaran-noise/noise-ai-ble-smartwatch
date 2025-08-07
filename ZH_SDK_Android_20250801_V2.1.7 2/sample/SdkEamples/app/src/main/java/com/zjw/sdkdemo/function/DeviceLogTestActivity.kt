package com.zjw.sdkdemo.function

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import com.blankj.utilcode.util.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.MptPowerLogBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceMptPowerLogCallBack
import com.zhapp.ble.callback.FirmwareLogStateCallBack
import com.zhapp.ble.callback.FirmwareLogStateCallBack.FirmwareLogState
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityDevicesLogBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.LoadingDialog
import com.zjw.sdkdemo.utils.ToastDialog
import kotlin.random.Random


/**
 * Created by Android on 2023/2/21.
 */
class DeviceLogTestActivity : BaseActivity() {
    private var loadingDialog: Dialog? = null

    private val binding by lazy { ActivityDevicesLogBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s365)
        setContentView(binding.root)
        inits()
        callBacks()
        event()
    }

    private fun inits() {
        //设置中心圆大小
        binding.pieChart.holeRadius = 0f
        //设置中心圆颜色大小
        binding.pieChart.transparentCircleRadius = 0f
        //右下角英文是否显示
        binding.pieChart.description.isEnabled = false
        //去除右下角的label
        val description = Description()
        description.setText("")
        binding.pieChart.description = description
        //图例大小
        binding.pieChart.legend.formSize = 20f
        //图例文本大小
        binding.pieChart.legend.textSize = 20f
        //百分比值
        binding.pieChart.setUsePercentValues(true)
        //数据描述文本颜色
        binding.pieChart.setEntryLabelColor(Color.BLACK)
        //数据描述文本大小
        binding.pieChart.setEntryLabelTextSize(16f)
        //设置piecahrt图表点击Item高亮是否可用
        binding.pieChart.setHighlightPerTapEnabled(true)
        //设置图列换行
        val l: Legend = binding.pieChart.getLegend()
        l.setWordWrapEnabled(true);
    }

    private fun event() {
        ClickUtils.applySingleDebouncing(binding.btnGet) {
            //获取日志文件
            ControlBleTools.getInstance().getFirmwareLog(object : SendCmdStateListener() {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }

        ClickUtils.applySingleDebouncing(binding.btnGetPower2) {
            //获取耗电数据
            ControlBleTools.getInstance().getMptPowerLogList(object : SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                        else -> MyApplication.showToast(R.string.s221)
                    }
                }
            })
        }

    }

    private fun callBacks() {
        CallBackUtils.firmwareLogStateCallBack = object : FirmwareLogStateCallBack {
            override fun onFirmwareLogState(state: Int) {
                LogUtils.d("getDeviceFirmwareLog state:$state")
                if (state == FirmwareLogState.START.state) {
                    // start
                    loadingDialog = LoadingDialog.show(this@DeviceLogTestActivity)
                } else if (state == FirmwareLogState.UPLOADING.state) {
                    // uploading....
                } else if (state == FirmwareLogState.END.state) {
                    // end
                    if (loadingDialog?.isShowing == true) {
                        loadingDialog?.dismiss()
                    }
                }
            }

            override fun onFirmwareLogFilePath(filePath: String) {
                LogUtils.d("onFirmwareLogFilePath filePath:$filePath")
                /*ThreadUtils.runOnUiThreadDelayed({
                    val data = FileIOUtils.readFile2String(FileUtils.getFileByPath(filePath))
                    if (data != null) setData(data)
                }, 1000)*/

                /*                private fun setData(data: String) {
                                    try {
                                        //[1676973905][Calculate_Battery_percent][88]2023:2:21:18:5:5 #1:20,3%;29:0,10%;28:0,14%;30:0,10%;8:6,7%;3:3,3%;10:3,0%;16:2,0%;11:1,0%;
                                        LogUtils.d("data:$data")
                                        val datas = data.split("\n")
                                        LogUtils.d("data:${datas.get(datas.size - 2)}")
                                        val json = datas.get(datas.size - 2).split("#")[1]
                                        val list = json.split(";")

                                        for (i in 0..list.size - 2) {
                                            val name = list.get(i).split(":")[0]
                                            val value = list.get(i).split(",")[1].replace("%", "")
                                            energys.add(EnergyData(name.toInt(), value.toFloat()))
                                        }

                                        if (energys.isNotEmpty()) {
                                            setPieChartData()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }*/
            }
        }

        CallBackUtils.deviceMptPowerLogCallBack = object : DeviceMptPowerLogCallBack {

            override fun onMptPowerLogList(list: MutableList<MptPowerLogBean>?) {
                //[{"powerLogs":[{"id":0,"percent":0,"quantity":0},{"id":1,"percent":4,"quantity":1},
                // {"id":2,"percent":0,"quantity":2},{"id":3,"percent":0,"quantity":3},{"id":4,"percent":8,"quantity":4},
                // {"id":5,"percent":4,"quantity":5},{"id":6,"percent":13,"quantity":6},{"id":7,"percent":13,"quantity":7},
                // {"id":8,"percent":13,"quantity":8},{"id":9,"percent":8,"quantity":9},{"id":10,"percent":8,"quantity":10},
                // {"id":11,"percent":39,"quantity":11},{"id":12,"percent":0,"quantity":12},{"id":0,"percent":0,"quantity":0}]
                // ,"powerPercent":66,
                // "startTimestamp":1677003460,
                // "endTimestamp":1677003460},
                //
                // .......]
                LogUtils.d("onMptPowerLogList:" + GsonUtils.toJson(list))
                ToastDialog.showToast(this@DeviceLogTestActivity,GsonUtils.toJson(list))
                if (!list.isNullOrEmpty()) {
                    setData(list.get(0))
                }
            }
        }
    }

    data class EnergyData(val nameId: Int, val value: Float)

    //能量数据
    val energys: MutableList<EnergyData> = ArrayList()

    @SuppressLint("SetTextI18n")
    private fun setData(bean: MptPowerLogBean) {
        //其它信息信息
        binding.tvTime.setText(getString(R.string.s444) + TimeUtils.millis2String(bean.startTimestamp * 1000L))

        binding.tvTime2.setText(getString(R.string.s504) + TimeUtils.millis2String(bean.endTimestamp * 1000L))

        binding.tvAllPower.setText(getString(R.string.s440) + "${bean.powerPercent} %")

        //图标数据
        energys.clear()
        if (!bean.powerLogs.isNullOrEmpty()) {
            var allData = StringBuilder()
            for (powerLog in bean.powerLogs) {
                //去除 id 为 0 的数据
                if (powerLog.id != 0) {
                    /* allData.append(getString(R.string.s441)).append(":").append(getNameById(powerLog.id)).append(", ")
                         .append(getString(R.string.s442)).append(":").append(powerLog.quantity).append(", ")
                         .append(getString(R.string.s442)).append(":").append("${powerLog.percent}%").append("\n")*/

                    energys.add(EnergyData(powerLog.id, powerLog.percent.toFloat()))
                }
            }

            if (energys.isNotEmpty()) {
                setPieChartData()
            }
            //tvAllData.setText(allData)
        }

    }

    private fun setPieChartData() {
        val pieEntryList: MutableList<PieEntry> = ArrayList()
        val colors = ArrayList<Int>()

        energys.sortByDescending { it.value }
        LogUtils.json(energys)
        //获取5个最高比列，剩余为其它 （100 - 5个最高比列和）
        if (energys.size >= 5) {
            for (i in 0..4) {
                pieEntryList.add(PieEntry(energys.get(i).value, getNameById(energys.get(i).nameId)))
                colors.add(Color.argb(255, Random.nextInt(0, 256), Random.nextInt(0, 256), Random.nextInt(0, 256)))
            }
        }
        var allData = 0f
        for (entry in pieEntryList) {
            allData += entry.value
        }
        if (100f - allData >= 0) {
            pieEntryList.add(PieEntry(100f - allData, getString(R.string.s430)))
            colors.add(Color.parseColor("#666666"))
        }
        val pieDataSet = PieDataSet(pieEntryList, "")
        pieDataSet.setColors(colors)
        val pieData = PieData(pieDataSet)
        pieData.setValueTextSize(12f)
        //设置数据百分比显示
        pieData.setDrawValues(true)
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()

    }

    fun getNameById(nameId: Int): String {
        var name = getString(R.string.s430)
        val type = DeviceMptPowerLogCallBack.PowerType.intToEnum(nameId)
        when (type) {
            DeviceMptPowerLogCallBack.PowerType.CALLER_ALERT -> {
                name = getString(R.string.s50)
            }

            DeviceMptPowerLogCallBack.PowerType.SMS_ALERTS -> {
                name = getString(R.string.s437)
            }

            DeviceMptPowerLogCallBack.PowerType.WRIST_WAKE -> {
                name = getString(R.string.s125)
            }

            DeviceMptPowerLogCallBack.PowerType.BLUETOOTH_CALL -> {
                name = getString(R.string.s438)
            }

            DeviceMptPowerLogCallBack.PowerType.ALARM_CLOCK -> {
                name = getString(R.string.s93)
            }

            DeviceMptPowerLogCallBack.PowerType.BREATHING_EXERCISES -> {
                name = getString(R.string.s408)
            }

            DeviceMptPowerLogCallBack.PowerType.STRESS_LEVELS -> {
                name = getString(R.string.s406)
            }

            DeviceMptPowerLogCallBack.PowerType.BLOOD_OXYGEN -> {
                name = getString(R.string.s404)
            }

            DeviceMptPowerLogCallBack.PowerType.HEART_RATE -> {
                name = getString(R.string.s405)
            }

            DeviceMptPowerLogCallBack.PowerType.COUNTDOWN -> {
                name = getString(R.string.s411)
            }

            DeviceMptPowerLogCallBack.PowerType.FLASHLIGHT -> {
                name = getString(R.string.s421)
            }

            DeviceMptPowerLogCallBack.PowerType.CONTINUOUS_HEART_RATE -> {
                name = getString(R.string.s439)
            }
        }
        return name
    }

}