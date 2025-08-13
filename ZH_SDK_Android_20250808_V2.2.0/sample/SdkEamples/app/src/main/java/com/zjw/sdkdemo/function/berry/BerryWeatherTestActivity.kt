package com.zjw.sdkdemo.function.berry

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.berry.weather.BerryForecastWeatherBean
import com.zhapp.ble.bean.berry.weather.BerryLatestWeatherBean
import com.zhapp.ble.bean.berry.weather.BerryWeatherIdBean
import com.zhapp.ble.bean.berry.weather.BerryWeatherKeyValueBean
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryWeatherTestBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.ToastDialog

/**
 * Created by Android on 2024/12/14.
 */
class BerryWeatherTestActivity : BaseActivity() {

    private val binding by lazy { ActivityBerryWeatherTestBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s640)
        setContentView(binding.root)
        inits()
    }

    private var weatherType = 200

    private fun inits() {

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                var texts = resources.getStringArray(R.array.weatherTypeValue)
                weatherType = texts[position].trim().toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    fun Send(v: View) {
        try {
            val newTemp = binding.etCurTemp.text.toString().trim().toInt()
            sendWeather(newTemp, weatherType)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerryWeatherTestActivity, getString(R.string.s238))
            return
        }
    }

    fun SendT006(v: View) {
        try {
            if (!ControlBleTools.getInstance().isConnect) return
            val newTemp = binding.etT006Temp.text.toString().trim().toInt()
            val newType = binding.etT006id.text.toString().trim().toInt()
            sendWeather(newTemp, newType)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@BerryWeatherTestActivity, getString(R.string.s238))
            return
        }
    }


    fun sendWeather(temp: Int, weatherType: Int) {
        if (!ControlBleTools.getInstance().isConnect) return
        val latestWeatherBean = BerryLatestWeatherBean()
        latestWeatherBean.id = BerryWeatherIdBean(System.currentTimeMillis() / 1000, "ShenZhen", "ShenZhen", "ShenZhen", false)
        latestWeatherBean.weather = weatherType
        latestWeatherBean.temperature = BerryWeatherKeyValueBean("temperature", temp + 5)
        latestWeatherBean.humidity = BerryWeatherKeyValueBean("humidity", temp + 4)
        latestWeatherBean.windSpeed = BerryWeatherKeyValueBean("windSpeed", temp + 3)
        latestWeatherBean.windDeg = BerryWeatherKeyValueBean("windDeg", temp + 0)
        latestWeatherBean.uvindex = BerryWeatherKeyValueBean("uvindex", temp + 2)
        latestWeatherBean.aqi = BerryWeatherKeyValueBean("aqi", temp + 1)
        var alertsList = mutableListOf<BerryLatestWeatherBean.WeatherAlertsListBean>()
        for (i in 0..10) {
            alertsList.add(BerryLatestWeatherBean.WeatherAlertsListBean("id$i", "type$i", "level$i", "title$i", "detail$i"))
        }
        latestWeatherBean.alertsList = alertsList
        latestWeatherBean.pressure = temp * 1.0F
        ControlBleTools.getInstance().sendBerryLatestWeather(latestWeatherBean, baseSendCmdStateListener)


        val berryForecastWeatherBean = BerryForecastWeatherBean()
        berryForecastWeatherBean.id = BerryWeatherIdBean(System.currentTimeMillis() / 1000, "ShenZhen", "ShenZhen", "ShenZhen", false)
        val forecastDatas = mutableListOf<BerryForecastWeatherBean.WeatherData>()
        for (i in 0..3) {
            forecastDatas.add(
                BerryForecastWeatherBean.WeatherData(
                    BerryWeatherKeyValueBean("api", temp + i),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(temp + 2 + i, temp + 3 + i),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(temp + i, temp + 1 + i),
                    "℃ ",
                    BerryForecastWeatherBean.BerryWeatherSunRiseSetBean((System.currentTimeMillis() - 480000 + i) / 1000, System.currentTimeMillis() / 1000),
                    BerryWeatherKeyValueBean("wind_speed", temp + 4 + (i % 10)),
                    BerryWeatherKeyValueBean("wind_deg", temp + 5 + (i % 10))
                )
            )
        }
        berryForecastWeatherBean.data = forecastDatas
        ControlBleTools.getInstance().sendBerryDailyForecastWeather(berryForecastWeatherBean, baseSendCmdStateListener)


        val bean = BerryForecastWeatherBean()
        bean.id = BerryWeatherIdBean(System.currentTimeMillis() / 1000, "ShenZhen", "ShenZhen", "ShenZhen", false)
        val datas = mutableListOf<BerryForecastWeatherBean.WeatherData>()
        for (i in 0..24) {
            datas.add(
                BerryForecastWeatherBean.WeatherData(
                    BerryWeatherKeyValueBean("api", 30 + (i % 10)),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(temp + 8 + (i % 10), temp + 9 + (i % 10)),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(temp + 6 + (i % 10), temp + 7 + (i % 10)),
                    "℃",
                    BerryForecastWeatherBean.BerryWeatherSunRiseSetBean((System.currentTimeMillis() - 480000 + (i % 10)) / 1000, System.currentTimeMillis() / 1000),
                    BerryWeatherKeyValueBean("wind_speed", temp + 5 + (i % 10)),
                    BerryWeatherKeyValueBean("wind_deg", temp + 6 + (i % 10))
                )
            )
        }
        bean.data = datas
        ControlBleTools.getInstance().sendBerryHourlyForecastWeather(bean, baseSendCmdStateListener)
    }

}