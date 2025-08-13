package com.zjw.sdkdemo.function.berry

import android.content.Intent
import android.os.Bundle
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryOtherSetBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import kotlin.jvm.java

/**
 * Created by Android on 2024/10/24.
 */
class BerryOtherSetActivity : BaseActivity() {
    private val binding: ActivityBerryOtherSetBinding by lazy { ActivityBerryOtherSetBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s605)
        setContentView(binding.root)
        initListener()
    }


    private fun initListener() {

        click(binding.btnSyncData) {
            startActivity(Intent(this, BerrySyncDataActivity::class.java))
        }

        click(binding.btnSys) {
            startActivity(Intent(this, BerrySystemActivity::class.java))
        }

        click(binding.btnUserSet) {
            startActivity(Intent(this, BerryUserSetActivity::class.java))
        }

        click(binding.btnTime){
            startActivity(Intent(this, BerryTimeSetActivity::class.java))
        }

        click(binding.btnNotify) {
            startActivity(Intent(this, BerryNotifyActivity::class.java))
        }

        click(binding.btnMusic) {
            startActivity(Intent(this, BerryMusicActivity::class.java))
        }

        click(binding.btnMicro) {
            startActivity(Intent(this, BerryMicroActivity::class.java))
        }

        click(binding.btnWeather) {
            startActivity(Intent(this, BerryWeatherActivity::class.java))
        }

        click(binding.btnSetting) {
            startActivity(Intent(this, BerrySettingActivity::class.java))
        }

        click(binding.btnRealTimeLog) {
            startActivity(Intent(this, BerryRealtimeLogActivity::class.java))
        }

        click(binding.btnChildren){
            startActivity(Intent(this, ChildrenActivity::class.java))
        }

        click(binding.btnSetMap){
            startActivity(Intent(this, OfflineMapActivity::class.java))
        }

        click(binding.btnSetSocket){
            startActivity(Intent(this, BerrySocketActivity::class.java))
        }

        click(binding.btnAi){
            startActivity(Intent(this, BerryAiActivity::class.java))
        }

    }

}