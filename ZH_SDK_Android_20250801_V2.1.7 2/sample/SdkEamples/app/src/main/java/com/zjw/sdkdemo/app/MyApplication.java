package com.zjw.sdkdemo.app;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.callback.ZHInitStatusCallBack;
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData;
import com.zjw.sdkdemo.receiver.BluetoothMonitorReceiver;
import com.zjw.sdkdemo.utils.DeviceTools;
import com.zjw.sdkdemo.utils.SaveLog;

public class MyApplication extends Application {

    private static DeviceTools mDeviceTools;
    public static Context context;

    public static MyApplication instance;

    public void onCreate() {
        context = this;
        instance = this;
        super.onCreate();
        initSharedPreferences();
        ControlBleTools.getInstance().setInitStatusCallBack(new ZHInitStatusCallBack() {
            @Override
            public void onInitComplete() {
                LogUtils.d("sdk init complete");
            }
        });
        ControlBleTools.getInstance().init(this);
        ControlBleTools.getInstance().enableUseSilenceMusic(true, 0);
        DeviceSettingLiveData.getInstance().initCallBack();
        //广播
        initBroadcast();
        com.zhapp.ble.utils.SaveLog.init(this);
        SaveLog.init(this);
        //创建diy路径
        FileUtils.createOrExistsDir(PathUtils.getAppDataPathExternalFirst() + "/diy");
    }

    /**
     * 初始化广播
     */
    private void initBroadcast() {
        //蓝牙状态&蓝牙设备与APP连接广播
        IntentFilter bleFilter = new IntentFilter();
        bleFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);   // 监视蓝牙关闭和打开的状态
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); // 监视蓝牙设备与APP断开的状态
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);    // 监视蓝牙设备与APP连接的状态
        registerReceiver(new BluetoothMonitorReceiver(), bleFilter);
    }

    void initSharedPreferences() {
        mDeviceTools = new DeviceTools(this);
    }

    public static DeviceTools getDeviceTools() {
        return mDeviceTools;
    }

    public static void showToast(String msg) {
        ToastUtils.showShort(msg);
    }

    public static void showToast(int msgid) {
        try {
            ToastUtils.showShort(context.getString(msgid));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
