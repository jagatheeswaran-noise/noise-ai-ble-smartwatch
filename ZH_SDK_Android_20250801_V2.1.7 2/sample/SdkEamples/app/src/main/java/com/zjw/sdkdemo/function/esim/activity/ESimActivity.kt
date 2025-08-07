package com.zjw.sdkdemo.function.esim.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.PermissionUtils.SingleCallback
import com.blankj.utilcode.util.ToastUtils
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ESIMBigdataBean
import com.zhapp.ble.bean.ESIMHttpdataBean
import com.zhapp.ble.bean.EsimCommonDataBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.EsimEidSettingsCallBack
import com.zhapp.ble.callback.EsimHttpDataCallBack
import com.zhapp.ble.callback.UploadBigDataListener
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityEsimBinding
import com.zjw.sdkdemo.function.esim.http.HttpUtil
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.ToastDialog
import java.net.MalformedURLException


class ESimActivity : BaseActivity() {
    var httpData: TextView? = null
    private val binding: ActivityEsimBinding by lazy { ActivityEsimBinding.inflate(layoutInflater) }

    private var esimCommonData :EsimCommonDataBean? = null

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Esim"
        setContentView(binding.root)
        val etActivationCode = findViewById<EditText>(R.id.etActivationCode)
        val devData = findViewById<TextView>(R.id.devData)
        httpData = findViewById<TextView>(R.id.httpData)
        findViewById<View>(R.id.btnScanQr).setOnClickListener {
            val PERMISSION_GROUP_CAMERA = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.CAMERA
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.CAMERA
                    )
                }

                else -> {
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    )
                }
            }
            if (!PermissionUtils.isGranted(*PERMISSION_GROUP_CAMERA)) {
                PermissionUtils.permission(*PERMISSION_GROUP_CAMERA).callback(object : SingleCallback {
                    override fun callback(isAllGranted: Boolean, granted: MutableList<String>, deniedForever: MutableList<String>, denied: MutableList<String>) {
                        findViewById<View>(R.id.btnScanQr).callOnClick()
                    }
                }).request()
            } else {
                // “QRCODE_SCAN_TYPE”和“DATAMATRIX_SCAN_TYPE”表示只扫描QR和DataMatrix的码，setViewType设置扫码标题，0表示设置扫码标题为“扫描二维码/条码”，1表示设置扫码标题为“扫描二维码”，默认为0; setErrorCheck设置错误监听，true表示监听错误并退出扫码页面，false表示不上报错误，仅检查到识别结果后退出扫码页面，默认为false
                val options = HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE, HmsScan.DATAMATRIX_SCAN_TYPE).setViewType(1).create()
                ScanUtil.startScan(this, 0, options)
            }
        }
        findViewById<View>(R.id.btnGetId).setOnClickListener {
//            ControlBleTools.getInstance().getEsimEidSettings(null)
            ControlBleTools.getInstance()
                .startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_ESIM,
                    "{\"header\":{\"functionExecutionStatus\":{\"status\":\"Executed-Success\"}},\"transactionId\":\"EB007976B32E4D9CAAA7AE8D0BD0744D\",\"boundProfilePackage\":\"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\"}".encodeToByteArray(),
                    true,
                    object : UploadBigDataListener {
                        override fun onSuccess() {
                            runOnUiThread {
                                httpData?.text = "发送成功"
                            }
                        }

                        @SuppressLint("SetTextI18n")
                        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                            runOnUiThread {
                                httpData?.text = "${curPiece}/${dataPackTotalPieceLength}"
                            }
                        }

                        override fun onTimeout(msg: String?) {
                            runOnUiThread {
                                httpData?.text = "传输失败"
                            }
                        }

                    })
        }
        findViewById<View>(R.id.sendActivationCode).setOnClickListener {
            val bean = ESIMBigdataBean()
            bean.esimActivationCodeData = etActivationCode.text.toString()
            ControlBleTools.getInstance().setEsimBigdataSettings(bean, null)
        }

        CallBackUtils.esimHttpDataCallBack = object : EsimHttpDataCallBack {
            override fun onEsimHttpData(httpdataBean: ESIMHttpdataBean?) {
                Log.d("esimHttpDataCallBack", "ESIMHttpdataBean" + httpdataBean.toString())
                devData.text = devData.text.toString() + httpdataBean.toString() + "\n"

                val httpUtil: HttpUtil = HttpUtil.getInstance(mHttpResponseListener, this@ESimActivity)
                try {
                    httpUtil.callHttpRequest(
                        httpdataBean!!.esimHttpUrl,
                        "",
                        httpdataBean.esimHttpData, 8.toByte()
                    )
                } catch (e: MalformedURLException) {
                    throw RuntimeException(e)
                }

            }

        }

        click(binding.btnGeteeeId){
            ControlBleTools.getInstance().getEsimEidSettings(baseSendCmdStateListener)
        }

        click(binding.btnGetESIMData){
            ControlBleTools.getInstance().getEsimCommonDataSettings(baseSendCmdStateListener)
        }

        click(binding.btnDelESIM){
            if(esimCommonData == null){
                ToastUtils.showShort(R.string.s681)
                return@click
            }
            ControlBleTools.getInstance().delEsimData(esimCommonData!!.esimId,baseSendCmdStateListener)
        }

        CallBackUtils.esimEidSettingsCallBack = object : EsimEidSettingsCallBack {
            override fun onEsimEidSettings(eid: String?) {
                ToastDialog.showToast(this@ESimActivity, "eid:$eid")
            }

            override fun onEsimCommonDataSetting(bean: EsimCommonDataBean?) {
                esimCommonData = bean
                ToastDialog.showToast(this@ESimActivity, "EsimCommonDataBean:$bean")
            }
        }


    }


    private val mHttpResponseListener: HttpUtil.HttpResponseListener = object : HttpUtil.HttpResponseListener {
        override fun onSuccess(responseBodyInfo: String, code: Int) {
            Log.d("size:" + responseBodyInfo.length, "responseBodyInfo:$responseBodyInfo\n")
            runOnUiThread {
                httpData?.text = responseBodyInfo
            }
            if (responseBodyInfo.contains("boundProfilePackage")) {
                ControlBleTools.getInstance()
                    .startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_ESIM, responseBodyInfo.encodeToByteArray(), true, object : UploadBigDataListener {
                        override fun onSuccess() {
                            runOnUiThread {
                                httpData?.text = "发送成功"
                            }
                        }

                        @SuppressLint("SetTextI18n")
                        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                            runOnUiThread {
                                httpData?.text = "${curPiece}/${dataPackTotalPieceLength}"
                            }
                        }

                        override fun onTimeout(msg: String?) {
                            runOnUiThread {
                                httpData?.text = "传输失败"
                            }
                        }

                    })
            } else {
                val bean = ESIMBigdataBean()
                bean.esimHttpData = responseBodyInfo
                bean.esimHttpCode = code
                ControlBleTools.getInstance().setEsimBigdataSettings(bean, null)
                if (!responseBodyInfo.contains("Executed-Success")) {
                    //TODO Notification APP shows failure
                }
            }
        }

        override fun onFailed(errorInfo: String, code: Int) {
            runOnUiThread {
                httpData?.text = errorInfo
            }
            httpData?.text = errorInfo
            Log.e("", "errorInfo:$errorInfo\n")
            val bean = ESIMBigdataBean()
            bean.esimHttpData = errorInfo
            bean.esimHttpCode = code
            ControlBleTools.getInstance().setEsimBigdataSettings(bean, null)
        }
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        if (requestCode == 0) {
            // 导入图片扫描返回结果
            val errorCode: Int = data.getIntExtra(ScanUtil.RESULT_CODE, ScanUtil.SUCCESS)
            if (errorCode == ScanUtil.SUCCESS) {
                val obj: Any? = data.getParcelableExtra(ScanUtil.RESULT)
                Log.d("onActivityResult", "obj:$obj")
                if (obj != null) {
                    // 展示扫码结果
                    binding.etActivationCode.setText(obj.toString())
                    binding.etActivationCode.setSelection(obj.toString().length)
                }
            }
            if (errorCode == ScanUtil.ERROR_NO_READ_PERMISSION) {
                // 无文件权限，请求文件权限
            }
        }
    }
}