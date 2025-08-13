package com.zjw.sdkdemo.function.berry

import android.content.Intent
import android.os.Bundle
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.berry.weather.BerryForecastWeatherBean
import com.zhapp.ble.bean.berry.weather.BerryLatestWeatherBean
import com.zhapp.ble.bean.berry.weather.BerryWeatherIdBean
import com.zhapp.ble.bean.berry.weather.BerryWeatherKeyValueBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.WeatherCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryWeatherBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import kotlin.random.Random

/**
 * Created by Android on 2024/10/25.
 */
class BerryWeatherActivity : BaseActivity() {
    private val binding by lazy { ActivityBerryWeatherBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s420)
        setContentView(binding.root)
        initListener()
        initCallBack()
    }

    private fun initCallBack() {
        CallBackUtils.weatherCallBack = object : WeatherCallBack {
            override fun onRequestWeather() {
                ToastUtils.showShort("onRequestWeather")
                binding.btnLatestWeather.callOnClick()
                binding.btnForecastWeatherByDay.callOnClick()
            }
        }
    }

    private fun initListener() {
        click(binding.btnLatestWeather) {
            sendLatestWeather()
        }

        click(binding.btnForecastWeatherByDay) {
            sendForecastWeatherByDay()
        }

        click(binding.btnForecastWeatherByHour) {
            sendForecastWeatherByHour()
        }

        click(binding.btnSendPressure) {
            sendPressure()
        }

        click(binding.btnWeatherTest) {
            startActivity(Intent(this, BerryWeatherTestActivity::class.java))
        }
    }

    private fun sendLatestWeather() {
        val latestWeatherBean = BerryLatestWeatherBean()
        latestWeatherBean.id = BerryWeatherIdBean(System.currentTimeMillis() / 1000, "ShenZhen", "ShenZhen", "ShenZhen", false)
        latestWeatherBean.weather = 800
        latestWeatherBean.temperature = BerryWeatherKeyValueBean("temperature", 25)
        latestWeatherBean.humidity = BerryWeatherKeyValueBean("humidity", 24)
        latestWeatherBean.windSpeed = BerryWeatherKeyValueBean("windSpeed", 12)
        latestWeatherBean.windDeg = BerryWeatherKeyValueBean("windDeg", 20)
        latestWeatherBean.uvindex = BerryWeatherKeyValueBean("uvindex", 22)
        latestWeatherBean.aqi = BerryWeatherKeyValueBean("aqi", 21)
        var alertsList = mutableListOf<BerryLatestWeatherBean.WeatherAlertsListBean>()
        for (i in 0..10) {
            alertsList.add(BerryLatestWeatherBean.WeatherAlertsListBean("id$i", "type$i", "level$i", "title$i", "detail$i"))
        }
        latestWeatherBean.alertsList = alertsList
        latestWeatherBean.pressure = 20f

        ControlBleTools.getInstance().sendBerryLatestWeather(latestWeatherBean, baseSendCmdStateListener)
    }

    private fun sendForecastWeatherByDay() {
        val bean = BerryForecastWeatherBean()
        bean.id = BerryWeatherIdBean(System.currentTimeMillis() / 1000, "ShenZhen", "ShenZhen", "ShenZhen", false)
        val datas = mutableListOf<BerryForecastWeatherBean.WeatherData>()
        for (i in 0..3) {
            datas.add(
                BerryForecastWeatherBean.WeatherData(
                    BerryWeatherKeyValueBean("api", 30 + i),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(28 + i, 29 + i),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(26 + i, 27 + i),
                    "℃ ",
                    BerryForecastWeatherBean.BerryWeatherSunRiseSetBean((System.currentTimeMillis() - 480000 + i) / 1000, System.currentTimeMillis() / 1000),
                    BerryWeatherKeyValueBean("wind_speed", 12),
                    BerryWeatherKeyValueBean("wind_deg", 24 + i)
                )
            )
        }
        bean.data = datas
        ControlBleTools.getInstance().sendBerryDailyForecastWeather(bean, baseSendCmdStateListener)
    }

    private fun sendForecastWeatherByHour() {
        val bean = BerryForecastWeatherBean()
        bean.id = BerryWeatherIdBean(System.currentTimeMillis() / 1000, "ShenZhen", "ShenZhen", "ShenZhen", false)
        val datas = mutableListOf<BerryForecastWeatherBean.WeatherData>()
        for (i in 0..24) {
            datas.add(
                BerryForecastWeatherBean.WeatherData(
                    BerryWeatherKeyValueBean("api", 30 + (i % 10)),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(28 + (i % 10), 29 + (i % 10)),
                    BerryForecastWeatherBean.BerryWeatherRangeValueBean(26 + (i % 10), 27 + (i % 10)),
                    "℃",
                    BerryForecastWeatherBean.BerryWeatherSunRiseSetBean((System.currentTimeMillis() - 480000 + (i % 10)) / 1000, System.currentTimeMillis() / 1000),
                    BerryWeatherKeyValueBean("wind_speed", 24 + (i % 10)),
                    BerryWeatherKeyValueBean("wind_deg", 23 + (i % 10))
                )
            )
        }
        bean.data = datas
        ControlBleTools.getInstance().sendBerryHourlyForecastWeather(bean, baseSendCmdStateListener)
    }


    private fun sendPressure() {
        ControlBleTools.getInstance().sendBerryPressureByWeather(Random.nextFloat() * (2000 - 1) + 2000, baseSendCmdStateListener)
    }


}