package com.zjw.sdkdemo.function.measure

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.zhapp.ble.callback.ActiveMeasureCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.function.language.BaseActivity

/**
 * Created by Android on 2023/3/10.
 */
class ActiveMeasureTypeActivity : BaseActivity() {

    private var mType = 0;  //0 戒指测量  1 手表测量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s445)
        setContentView(R.layout.activity_measure_type)
        mType = intent.getIntExtra("mType",0)
    }

    fun toHeartRate(view: View) {
        startActivity(Intent(this, ActiveMeasureActivity::class.java).apply {
            putExtra("type", ActiveMeasureCallBack.MeasureType.HEART_RATE.type)
            putExtra("mType",mType)
        })
    }

    fun toBloodOxygen(view: View) {
        startActivity(Intent(this, ActiveMeasureActivity::class.java).apply {
            putExtra("type", ActiveMeasureCallBack.MeasureType.BLOOD_OXYGEN.type)
            putExtra("mType",mType)
        })

    }

    fun toStress(view: View) {
        startActivity(Intent(this, ActiveMeasureActivity::class.java).apply {
            putExtra("type", ActiveMeasureCallBack.MeasureType.STRESS_HRV.type)
            putExtra("mType",mType)
        })
    }

    fun toTemperature(view: View) {
        startActivity(Intent(this, ActiveMeasureActivity::class.java).apply {
            putExtra("type", ActiveMeasureCallBack.MeasureType.BODY_TEMPERATURE.type)
            putExtra("mType",mType)
        })
    }

    fun toGomoreStress(view: View) {
        startActivity(Intent(this, ActiveMeasureActivity::class.java).apply {
            putExtra("type", ActiveMeasureCallBack.MeasureType.STRESS.type)
            putExtra("mType",mType)
        })
    }
}