package com.zjw.sdkdemo.function.berry

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.ZipUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DeviceFileUploadStatusBean
import com.zhapp.ble.bean.LogFileStatusBean
import com.zhapp.ble.bean.berry.BerryRealTimeLogBean
import com.zhapp.ble.callback.BerryFirmwareLogCallBack
import com.zhapp.ble.callback.BerryRealTimeLogCollectCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryRelatimeBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import com.zjw.sdkdemo.utils.WakeLockManager
import java.io.File


/**
 * Created by Android on 2024/11/15.
 */
class BerryRealtimeLogActivity : BaseActivity() {

    val binding: ActivityBerryRelatimeBinding by lazy { ActivityBerryRelatimeBinding.inflate(layoutInflater) }

    private var isEnd = true

//    private var wakeLock: PowerManager.WakeLock? = null

    var type = 1

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s627)
        setContentView(binding.root)

        supportActionBar?.apply {
            setHomeButtonEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }

//        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
//        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag")
//        wakeLock?.acquire()

        //默认1
        binding.tvSType.text = binding.btnType1.text.toString()

        binding.tvStatus.text = getString(R.string.s647)

        initListener()
        initCallBack()

        WakeLockManager.instance.keepUnLock(this.lifecycle)
    }

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun initListener() {
        click(binding.btnType1) {
            type = 1
            binding.tvSType.text = binding.btnType1.text.toString()
        }
        click(binding.btnType2) {
            type = 2
            binding.tvSType.text = binding.btnType2.text.toString()
        }
        click(binding.btnType3) {
            type = 3
            binding.tvSType.text = binding.btnType3.text.toString()
        }
        click(binding.btnType4) {
            type = 4
            binding.tvSType.text = binding.btnType4.text.toString()
        }
        click(binding.btnType5) {
            type = 5
            binding.tvSType.text = binding.btnType5.text.toString()
        }
        click(binding.btnType6) {
            type = 6
            binding.tvSType.text = binding.btnType6.text.toString()
        }
        click(binding.btnType7) {
            type = 7
            binding.tvSType.text = binding.btnType7.text.toString()
        }
        click(binding.btnType8) {
            type = 8
            binding.tvSType.text = binding.btnType8.text.toString()
        }
        click(binding.btnType9) {
            type = 9
            binding.tvSType.text = binding.btnType9.text.toString()
        }
        click(binding.btnType10) {
            type = 10
            binding.tvSType.text = binding.btnType10.text.toString()
        }
        click(binding.btnType11) {
            type = 11
            binding.tvSType.text = binding.btnType11.text.toString()
        }
        click(binding.btnType12) {
            type = 12
            binding.tvSType.text = binding.btnType12.text.toString()
        }
        click(binding.btnType13) {
            type = 13
            binding.tvSType.text = binding.btnType13.text.toString()
        }
        click(binding.btnType14) {
            type = 14
            binding.tvSType.text = binding.btnType14.text.toString()
        }
        click(binding.btnType15) {
            type = 15
            binding.tvSType.text = binding.btnType15.text.toString()
        }
        click(binding.btnType16) {
            type = 16
            binding.tvSType.text = binding.btnType16.text.toString()
        }
        click(binding.btnType17) {
            type = 17
            binding.tvSType.text = binding.btnType17.text.toString()
        }
        click(binding.btnType18) {
            type = 18
            binding.tvSType.text = binding.btnType18.text.toString()
        }
        click(binding.btnType19) {
            type = 19
            binding.tvSType.text = binding.btnType19.text.toString()
        }
        click(binding.btnType20) {
            type = 20
            binding.tvSType.text = binding.btnType20.text.toString()
        }
        click(binding.btnType21) {
            type = 21
            binding.tvSType.text = binding.btnType21.text.toString()
        }
        click(binding.btnType22) {
            type = 22
            binding.tvSType.text = binding.btnType22.text.toString()
        }
        click(binding.btnType23) {
            type = 23
            binding.tvSType.text = binding.btnType23.text.toString()
        }

        click(binding.btnStartLog) {
            binding.btnStartLog.isEnabled = false
            binding.tvStatus.text = getString(R.string.s648)

            ControlBleTools.getInstance().berryRealTimeLog(true, type, baseSendCmdStateListener)
        }

        click(binding.btnEndLog) {
            isEnd = true
            binding.btnStartLog.isEnabled = true

            binding.tvStatus.text = getString(R.string.s647)
            ControlBleTools.getInstance().berryRealTimeLog(false, type, baseSendCmdStateListener)
        }

        binding.btnExit.setOnLongClickListener {
            if (!isEnd) {
                return@setOnLongClickListener false
            }
            finish()
            return@setOnLongClickListener true
        }

        click(binding.btnShare){
            val zipFilePath = getExternalFilesDir("logZip")?.absolutePath + File.separator +
                    "devicelog_log_" + System.currentTimeMillis() + ".zip"

            val logDirs = arrayListOf<String>()
            getExternalFilesDir("deviceLog")?.absolutePath?.let {
                logDirs.add(it)
            }
            shareAllZip(logDirs, zipFilePath)
        }

    }

    private var logFileStatus: LogFileStatusBean? = null

    private fun initCallBack() {
        BleConnectState.getInstance().observe(this) { integer ->
            when (integer) {
                BleCommonAttributes.STATE_CONNECTED -> {
                    isEnd = true
                    binding.btnStartLog.isEnabled = true
                }

                BleCommonAttributes.STATE_CONNECTING -> {
                }

                BleCommonAttributes.STATE_DISCONNECTED -> {
                    isEnd = true
                    binding.btnStartLog.isEnabled = true
                }

                BleCommonAttributes.STATE_TIME_OUT -> {
                }
            }
        }

        CallBackUtils.berryRealTimeLogCollectCallBack = object : BerryRealTimeLogCollectCallBack {
            override fun onRealTimeLogCollect(berryRealTimeLogBean: BerryRealTimeLogBean?) {
                if (berryRealTimeLogBean != null) {
                    if (!berryRealTimeLogBean.fileName.isNullOrEmpty()) {
                        if (berryRealTimeLogBean.status != 1) {
                            ToastUtils.showShort("开启日志失败，status = ${berryRealTimeLogBean.status}")
                        }
                    }
                    if (berryRealTimeLogBean.dataLen != 0) {
                        ControlBleTools.getInstance().requestLogFileStatusByBerry(13, "", "", "", "", baseSendCmdStateListener)
                    }
                }
            }
        }

        CallBackUtils.berryFirmwareLogCallBack = object : BerryFirmwareLogCallBack {
            override fun onDeviceRequestAppGetLog() {
                ControlBleTools.getInstance().requestLogFileStatusByBerry(13, "", "", "", "", baseSendCmdStateListener)
            }

            override fun onLogFileStatus(bean: LogFileStatusBean?) {
                logFileStatus = bean
                if (bean != null && bean.type == 13 && bean.fileSize != 0) {
                    isEnd = false
                    ControlBleTools.getInstance().requestUploadLogFileByBerry(true, bean.type, bean.fileSize, baseSendCmdStateListener)
                }
            }

            override fun onLogProgress(curSize: Int, allSize: Int) {
                isEnd = false
                binding.tvProgress.setText("progress : $curSize / $allSize")
            }

            override fun onLogFileUploadStatus(bean: DeviceFileUploadStatusBean?) {
                if (logFileStatus != null && bean != null && bean.isSuccessful) {
                    ControlBleTools.getInstance().requestUploadLogFileByBerry(false, bean.type, bean.fileSize, baseSendCmdStateListener)
                }
            }

            override fun onLogFilePath(type: Int, path: String?) {

            }

        }

    }

    private fun shareAllZip(logDirs: List<String>, zipFilePath: String) {
        ThreadUtils.executeByIo(object : ThreadUtils.Task<String>() {
            override fun onCancel() {}
            override fun onFail(t: Throwable?) {
                LogUtils.e("shareZip ->$t")
            }

            override fun doInBackground(): String {
                var zipPath = ""
                val logPaths = mutableListOf<String>()
                for (logDir in logDirs) {
                    for (file in listAllFilesInDir(logDir)) {
                        logPaths.add(file.absolutePath)
                    }
                }
                if (logPaths.size > 0) {
                    ZipUtils.zipFiles(logPaths, zipFilePath)
                    zipPath = zipFilePath
                }
                return zipPath
            }

            override fun onSuccess(result: String?) {
                if (result.isNullOrEmpty()) {
                    ToastUtils.showLong("暂无日志可分享")
                    return
                }
                val zipFile = FileUtils.getFileByPath(result)
                var intent = Intent(Intent.ACTION_SEND)
                intent.type = "application/zip"
                intent.putExtra(Intent.EXTRA_STREAM, UriUtils.file2Uri(zipFile))
                intent.putExtra(Intent.EXTRA_TEXT, "abc")
                intent = Intent.createChooser(intent, "日志分享")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

        })
    }

    fun listAllFilesInDir(dir: String): List<File> {
        val list = mutableListOf<File>()
        for (file in FileUtils.listFilesInDir(dir)) {
            if (file.isFile) {
                list.add(file)
            } else if (file.isDirectory) {
                list.addAll(listAllFilesInDir(file.absolutePath))
            }
        }
        return list
    }

    override fun onDestroy() {
        super.onDestroy()
//        wakeLock?.release() // 使用完后记得释放
    }

}