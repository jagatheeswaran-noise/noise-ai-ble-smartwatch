package com.zjw.sdkdemo.function.language

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ClickUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication

/**
 * Created by Android on 2022/1/12.
 */
open class BaseActivity : AppCompatActivity() {

    var baseSendCmdStateListener: BaseSendCmdStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBack()
    }

    fun initBack() {
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        baseSendCmdStateListener = BaseSendCmdStateListener()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                finish() // back button
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    fun click(view: View, block: () -> Unit) {
        ClickUtils.applySingleDebouncing(view) {
            if (!ControlBleTools.getInstance().isConnect) ToastUtils.showShort(getString(R.string.s294))
            block()
        }
    }

    open fun saveLog(logPath: String, content: String) {
        ThreadUtils.executeBySingle(object : ThreadUtils.Task<String>() {
            override fun doInBackground(): String {
                val buffer = StringBuffer()
                buffer.append(TimeUtils.getNowString(TimeUtils.getSafeDateFormat("MM-dd HH:mm:ss.SSS")))
                buffer.append("  ------>  ")
                buffer.append(content)
                buffer.append("\n")
                FileIOUtils.writeFileFromString(logPath, buffer.toString(), true)
                return content
            }

            override fun onSuccess(result: String?) {
                LogUtils.d("日志保存成功 --> $result")
            }

            override fun onCancel() {
            }

            override fun onFail(t: Throwable?) {
                LogUtils.e("日志保存失败 ---> ${t?.localizedMessage}")
            }
        })
    }

    inner class BaseSendCmdStateListener : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
        override fun onState(state: SendCmdState?) {
            when (state) {
                SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                else -> MyApplication.showToast(R.string.s221)
            }
        }
    }
}