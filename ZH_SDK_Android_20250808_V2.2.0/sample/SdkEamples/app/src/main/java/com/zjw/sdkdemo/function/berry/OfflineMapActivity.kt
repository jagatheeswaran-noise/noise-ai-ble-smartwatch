package com.zjw.sdkdemo.function.berry

import android.app.Dialog
import android.os.Bundle
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.OfflineMapResBean
import com.zhapp.ble.bean.berry.BerryOfflineMapBean
import com.zhapp.ble.callback.BerryOfflineMapCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.UploadBigDataListener
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityOfflinemapBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.AssetUtils
import com.zjw.sdkdemo.utils.ToastDialog
import java.io.File

/**
 * Created by Android on 2025/4/10.
 */
class OfflineMapActivity : BaseActivity() {

    private val TAG = OfflineMapActivity::class.java.simpleName
    private val binding: ActivityOfflinemapBinding by lazy { ActivityOfflinemapBinding.inflate(layoutInflater) }
    private var offlineMapBean: BerryOfflineMapBean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s691)
        setContentView(binding.root)
        initEvent()
    }

    private fun initEvent() {
        initDeviceInfoCallBack()
        initMapCallBack()
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getDeviceInfo(null)
        }


        click(binding.btnGetMap) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getOfflineMapDataByBerry(baseSendCmdStateListener)
            }
        }

        click(binding.btnDelMap) {
            if (ControlBleTools.getInstance().isConnect) {
                if (offlineMapBean == null) {
                    ToastUtils.showShort(getString(R.string.s697))
                    return@click
                }
                if (offlineMapBean?.maps.isNullOrEmpty()) {
                    ToastUtils.showShort(getString(R.string.s698))
                    return@click
                }
                ControlBleTools.getInstance().deleteOfflineMapDataByBerry(offlineMapBean?.maps?.get(0), baseSendCmdStateListener)
            }
        }

        click(binding.btnSetMap) {
            sendOfflineMap()
        }

    }

    private fun initMapCallBack() {
        CallBackUtils.berryOfflineMapCallBack = object : BerryOfflineMapCallBack {
            override fun onOfflineMap(bean: BerryOfflineMapBean?) {
                ToastDialog.showToast(this@OfflineMapActivity, GsonUtils.toJson(bean))
                offlineMapBean = bean
            }
        }
    }

    private var deviceInfo: DeviceInfoBean? = null
    private fun initDeviceInfoCallBack() {
        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean?) {
                deviceInfo = deviceInfoBean
            }

            override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {
            }
        }
    }

    private fun sendOfflineMap() {
        if (ControlBleTools.getInstance().isConnect) {
            if (deviceInfo == null) {
                if (ControlBleTools.getInstance().isConnect) {
                    ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
                }
                return
            }
            val offlineMapResBean = OfflineMapResBean()
            offlineMapResBean.mapName = "testMap111"
            offlineMapResBean.mapFiles = mutableListOf<OfflineMapResBean.MapFile?>().apply {
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "landuse.db"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "landuse.db")
                })
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "landuse.idx"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "landuse.idx")
                })
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "landuse.mlp"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "landuse.mlp")
                })
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "roads.idx"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "roads.idx")
                })
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "roads.mlp"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "roads.mlp")
                })
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "water.idx"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "water.idx")
                })
                add(OfflineMapResBean.MapFile().apply {
                    fileName = "water.mlp"
                    fileBytes = AssetUtils.getAssetBytes(this@OfflineMapActivity, "map" + File.separator + "water.mlp")
                })
            }
            ControlBleTools.getInstance().setOfflineMapDataByBerry(offlineMapResBean, deviceInfo?.equipmentNumber, MyUploadBigDataListener())
        }
    }

    //region 大文件传输回调
    private var dialog: Dialog? = null

    inner class MyUploadBigDataListener() : UploadBigDataListener {
        override fun onSuccess() {
            if (dialog?.isShowing == true) dialog?.dismiss()
            ToastUtils.showShort(getString(R.string.s220))

        }

        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            if (curPiece == 0) {
                if (dialog?.isShowing == true) dialog?.dismiss()
                dialog = ToastDialog.showToast(this@OfflineMapActivity, getString(R.string.s648))
            }
        }

        override fun onTimeout(msg: String?) {
            if (dialog?.isShowing == true) dialog?.dismiss()
            ToastUtils.showShort(getString(R.string.s221) + " : " + msg)
        }

    }
    //endregion

}