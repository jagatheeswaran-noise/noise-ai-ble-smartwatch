package com.zjw.sdkdemo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.zhapp.ble.bean.RingBatteryBean;
import com.zhapp.ble.bean.berry.DrinkWaterBean;
import com.zhapp.ble.callback.VerifyUserIdCallBack;
import com.zhapp.ble.manager.BleBCManager;
import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.ActivityDurationBean;
import com.zhapp.ble.bean.AutoActiveSportBean;
import com.zhapp.ble.bean.BindDeviceBean;
import com.zhapp.ble.bean.ContinuousBloodOxygenBean;
import com.zhapp.ble.bean.ContinuousHeartRateBean;
import com.zhapp.ble.bean.ContinuousPressureBean;
import com.zhapp.ble.bean.ContinuousTemperatureBean;
import com.zhapp.ble.bean.DailyBean;
import com.zhapp.ble.bean.DeviceBatteryValueBean;
import com.zhapp.ble.bean.DeviceInfoBean;
import com.zhapp.ble.bean.EffectiveStandingBean;
import com.zhapp.ble.bean.EmergencyContactBean;
import com.zhapp.ble.bean.ExaminationBean;
import com.zhapp.ble.bean.OffEcgDataBean;
import com.zhapp.ble.bean.OfflineBloodOxygenBean;
import com.zhapp.ble.bean.OfflineHeartRateBean;
import com.zhapp.ble.bean.OfflinePressureDataBean;
import com.zhapp.ble.bean.OfflineTemperatureDataBean;
import com.zhapp.ble.bean.OverallDayMovementData;
import com.zhapp.ble.bean.RealTimeBean;
import com.zhapp.ble.bean.RingBodyBatteryBean;
import com.zhapp.ble.bean.RingHealthScoreBean;
import com.zhapp.ble.bean.RingSleepNapBean;
import com.zhapp.ble.bean.RingSleepResultBean;
import com.zhapp.ble.bean.RingStressDetectionBean;
import com.zhapp.ble.bean.SleepBean;
import com.zhapp.ble.bean.TodayActiveTypeData;
import com.zhapp.ble.bean.TodayRespiratoryRateData;
import com.zhapp.ble.bean.TrackingLogBean;
import com.zhapp.ble.bean.WatchFaceInstallResultBean;
import com.zhapp.ble.callback.BindDeviceStateCallBack;
import com.zhapp.ble.callback.BleStateCallBack;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.DeviceInfoCallBack;
import com.zhapp.ble.callback.DisconnectReasonCallBack;
import com.zhapp.ble.callback.EmergencyContactSosCallBack;
import com.zhapp.ble.callback.FirmwareLogStateCallBack;
import com.zhapp.ble.callback.FirmwareTrackingLogCallBack;
import com.zhapp.ble.callback.FitnessDataCallBack;
import com.zhapp.ble.callback.RealTimeDataCallBack;
import com.zhapp.ble.callback.RequestDeviceBindStateCallBack;
import com.zhapp.ble.callback.UnbindDeviceCallBack;
import com.zhapp.ble.callback.WatchFaceInstallCallBack;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.ClockDialActivity;
import com.zjw.sdkdemo.function.DeviceLogTestActivity;
import com.zjw.sdkdemo.function.esim.activity.ESimActivity;
import com.zjw.sdkdemo.function.EmojiActivity;
import com.zjw.sdkdemo.function.MicroActivity;
import com.zjw.sdkdemo.function.OTAActivity;
import com.zjw.sdkdemo.function.RealTimeHeartRateActivity;
import com.zjw.sdkdemo.function.RemindActivity;
import com.zjw.sdkdemo.function.RingTestActivity;
import com.zjw.sdkdemo.function.SetActivity;
import com.zjw.sdkdemo.function.SportActivity;
import com.zjw.sdkdemo.function.diydial.SimpleDiyActivity;
import com.zjw.sdkdemo.function.sync.SyncRingRTCFitnessActivity;
import com.zjw.sdkdemo.function.album_dial.NewAlbumStyleDialActivity;
import com.zjw.sdkdemo.function.diydial.DiySelectActivity;
import com.zjw.sdkdemo.function.diydial.OldDiyDialActivity;
import com.zjw.sdkdemo.function.measure.ActiveMeasureTypeActivity;
import com.zjw.sdkdemo.function.ring_loop.LoopBindActivity;
import com.zjw.sdkdemo.function.ring_loop.LoopFitnessActivity;
import com.zjw.sdkdemo.function.sifli.SifliTestActivity;
import com.zjw.sdkdemo.livedata.BleConnectState;
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData;
import com.zjw.sdkdemo.receiver.SmsContentObserver;
import com.zjw.sdkdemo.utils.DeviceTools;
import com.zjw.sdkdemo.utils.LoadingDialog;
import com.zjw.sdkdemo.utils.ToastDialog;

import java.util.List;

public class BraceletActivity extends AppCompatActivity {
    final private String TAG = BraceletActivity.class.getSimpleName();
    private DeviceTools mDeviceTools = MyApplication.getDeviceTools();

    public static final String EXTRA_DEVICE_PROTOCOL = "ble_device_protocol";
    public static final String EXTRA_DEVICE_ADDRESS = "ble_device_address";
    public static final String EXTRA_DEVICE_NAME = "ble_device_name";
    public static final String EXTRA_DEVICE_ISBIND = "ble_device_isbind";
    //public static final String EXTRA_DEVICE_SCAN = "EXTRA_DEVICE_SCAN";
    public static String deviceProtocol;
    public static String deviceAddress;
    public static String deviceName;
    private boolean isBind = false;
    private StringBuffer dailyResult = new StringBuffer();

    public TextView tvDailyProgress;
    public TextView tvDailyResult;
    public TextView tvRealTimeData;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bracelet);
        setTitle("SDK Demo");
        initView();
        initData();
    }

    @SuppressLint("ClickableViewAccessibility")
    void initView() {
        tvDailyResult = findViewById(R.id.tvDailyResult);
        tvDailyProgress = findViewById(R.id.tvDailyProgress);
        tvRealTimeData = findViewById(R.id.tvRealTimeData);
    }

    void initData() {
        if (getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS) != null && !getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS).equals("")) {
            deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        }
        if (getIntent().getStringExtra(EXTRA_DEVICE_NAME) != null && !getIntent().getStringExtra(EXTRA_DEVICE_NAME).equals("")) {
            deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
        }
        if (getIntent().getStringExtra(EXTRA_DEVICE_PROTOCOL) != null && !getIntent().getStringExtra(EXTRA_DEVICE_PROTOCOL).equals("")) {
            deviceProtocol = getIntent().getStringExtra(EXTRA_DEVICE_PROTOCOL);
        }
        isBind = getIntent().getBooleanExtra(EXTRA_DEVICE_ISBIND, false);

        Toast.makeText(BraceletActivity.this, "device - " + deviceAddress, Toast.LENGTH_SHORT).show();

        initObserve();
        callback();
        /*mSmsContentObserver = new SmsContentObserver(this.getApplicationContext(), new Handler());
        getContentResolver().registerContentObserver(Telephony.Sms.Inbox.CONTENT_URI, false, mSmsContentObserver);*/
    }

    SmsContentObserver mSmsContentObserver;

    private void initObserve() {
        BleConnectState.getInstance().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                switch (integer) {
                    case BleCommonAttributes.STATE_CONNECTED: {
                        Log.i(TAG, "BleConnectState = STATE_CONNECTED");
                        String title = getString(R.string.app_name) + " : " + getString(R.string.ble_connected_tips);
                        setTitle(title);

                        if (isBind) {
                            checkBTBle();
                        }

                    }
                    break;
                    case BleCommonAttributes.STATE_CONNECTING: {
                        Log.i(TAG, "BleConnectState = STATE_CONNECTING");
                        String title = getString(R.string.app_name) + " : " + getString(R.string.ble_connecting_tips);
                        setTitle(title);
                    }
                    break;
                    case BleCommonAttributes.STATE_DISCONNECTED: {
                        Log.i(TAG, "BleConnectState = STATE_DISCONNECTED");
                        setTitle(getString(R.string.app_name) + " : " + getString(R.string.ble_disconnect_tips));
                    }
                    break;
                    case BleCommonAttributes.STATE_TIME_OUT: {
                        Log.i(TAG, "BleConnectState = STATE_TIME_OUT");
                        String title = getString(R.string.app_name) + " : " + getString(R.string.ble_connect_time_out_tips);
                        setTitle(title);
                        if (deviceAddress != null) {
                            Log.i("BraceletActivity", "connect() deviceProtocol = " + deviceProtocol + " deviceName = " + deviceName + " deviceAddress = " + deviceAddress);
                            ControlBleTools.getInstance().connect(deviceName, deviceAddress, deviceProtocol);
                        }
                    }
                    break;
                }
            }
        });

        DeviceSettingLiveData.getInstance().getmDeviceBatteryValueBean().observe(this, new Observer<DeviceBatteryValueBean>() {
            @Override
            public void onChanged(DeviceBatteryValueBean deviceBatteryValueBean) {
                ToastDialog.showToast(BraceletActivity.this, GsonUtils.toJson(deviceBatteryValueBean));
            }
        });
    }

    private void callback() {

        CallBackUtils.requestDeviceBindStateCallBack = new RequestDeviceBindStateCallBack() {
            @Override
            public void onBindState(boolean state) {
                if (state) {
                    showTips(getString(R.string.s203));
                } else {
                    bindDevice();
                }
            }
        };

        //日常数据回调
        CallBackUtils.fitnessDataCallBack = new FitnessDataCallBack() {
            @Override
            public void onProgress(int progress, int total) {
                Log.e(TAG, "onProgress : progress " + progress + "  total " + total);
                if (progress == 0) {
                    dailyResult = new StringBuffer();
                    tvDailyResult.setText("");
                }
                tvDailyProgress.setText("onProgress :progress  " + progress + "  total " + total);
                if (progress == total) {
                    tvDailyResult.setText(dailyResult.toString());
                    tvDailyProgress.setText("");
                }
            }

            @Override
            public void onDailyData(DailyBean data) {
                Log.e(TAG, "onDailyData : " + data.toString());
                dailyResult.append("\n\n" + "onDailyData : " + data.toString());
            }

            @Override
            public void onSleepData(SleepBean data) {
                Log.e(TAG, "onSleepData : " + data.toString());
                dailyResult.append("\n\n" + "SleepBean : " + data.toString());
            }

            @Override
            public void onContinuousHeartRateData(ContinuousHeartRateBean data) {
                Log.e(TAG, "onContinuousHeartRateData : " + data.toString());
                dailyResult.append("\n\n" + "ContinuousHeartRateBean : " + data.toString());
            }

            @Override
            public void onOfflineHeartRateData(OfflineHeartRateBean data) {
                Log.e(TAG, "onOfflineHeartRateData : " + data.toString());
                dailyResult.append("\n\n" + "OfflineHeartRateBean : " + data.toString());
            }

            @Override
            public void onContinuousBloodOxygenData(ContinuousBloodOxygenBean data) {
                Log.e(TAG, "onContinuousBloodOxygenData : " + data.toString());
                dailyResult.append("\n\n" + "ContinuousBloodOxygenBean : " + data.toString());
            }

            @Override
            public void onOfflineBloodOxygenData(OfflineBloodOxygenBean data) {
                Log.e(TAG, "onOfflineBloodOxygenData : " + data.toString());
                dailyResult.append("\n\n" + "OfflineBloodOxygenBean : " + data.toString());
            }

            @Override
            public void onContinuousPressureData(ContinuousPressureBean data) {
                Log.e(TAG, "onContinuousPressureData : " + data.toString());
                dailyResult.append("\n\n" + "ContinuousPressureBean : " + data.toString());
            }

            @Override
            public void onOfflinePressureData(OfflinePressureDataBean data) {
                Log.e(TAG, "onOfflinePressureData : " + data.toString());
                dailyResult.append("\n\n" + "OfflinePressureDataBean : " + data.toString());
            }

            @Override
            public void onContinuousTemperatureData(ContinuousTemperatureBean data) {
                Log.e(TAG, "onContinuousTemperatureData : " + data.toString());
                dailyResult.append("\n\n" + "ContinuousTemperatureBean : " + data.toString());
            }

            @Override
            public void onOfflineTemperatureData(OfflineTemperatureDataBean data) {
                Log.e(TAG, "onOfflineTemperatureData : " + data.toString());
                dailyResult.append("\n\n" + "OfflineTemperatureDataBean : " + data.toString());
            }

            @Override
            public void onEffectiveStandingData(EffectiveStandingBean data) {
                Log.e(TAG, "onEffectiveStandingData : " + data.toString());
                dailyResult.append("\n\n" + "EffectiveStandingBean : " + data.toString());
            }

            @Override
            public void onActivityDurationData(ActivityDurationBean data) {
                dailyResult.append("\n\n" + "ActivityDurationBean : " + data.toString());
            }

            @Override
            public void onOffEcgData(OffEcgDataBean data) {
                dailyResult.append("\n\n" + "OffEcgDataBean : " + data.toString());
            }

            @Override
            public void onExaminationData(ExaminationBean data) {
                Log.e(TAG, "ExaminationBean : " + GsonUtils.toJson(data));
                dailyResult.append("\n\n" + "ExaminationBean : " + GsonUtils.toJson(data));
            }

            @Override
            public void onRingTodayActiveTypeData(TodayActiveTypeData bean) {
                Log.e(TAG, "TodayActivityIndicatorsBean : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "TodayActivityIndicatorsBean : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onRingOverallDayMovementData(OverallDayMovementData bean) {
                Log.e(TAG, "onRingOverallDayMovementData : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "OverallDayMovementData : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onRingTodayRespiratoryRateData(TodayRespiratoryRateData bean) {
                Log.e(TAG, "onRingTodayRespiratoryRateData : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "TodayRespiratoryRateData : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onRingHealthScore(RingHealthScoreBean bean) {
                Log.e(TAG, "RingHealthScoreBean : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "RingHealthScoreBean : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onRingSleepResult(RingSleepResultBean bean) {
                Log.e(TAG, "RingSleepResultBean : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "RingSleepResultBean : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onRingBatteryData(RingBatteryBean bean) {
                Log.e(TAG, "onRingBatteryData : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "onRingBatteryData : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onDrinkWaterData(DrinkWaterBean bean) {
                Log.e(TAG, "onDrinkWaterData : " + GsonUtils.toJson(bean));
                dailyResult.append("\n\n" + "onDrinkWaterData : " + GsonUtils.toJson(bean));
            }

            @Override
            public void onRingSleepNAP(List<RingSleepNapBean> list) {
                Log.e(TAG, "RingSleepNapBean : " + GsonUtils.toJson(list));
                dailyResult.append("\n\n" + "RingSleepNapBean : " + GsonUtils.toJson(list));
            }

            @Override
            public void onRingAutoActiveSportData(AutoActiveSportBean data) {
                Log.e(TAG, "AutoActiveSportData : " + GsonUtils.toJson(data));
                dailyResult.append("\n\n" + "AutoActiveSportData : " + GsonUtils.toJson(data));
            }

            @Override
            public void onRingBodyBatteryData(RingBodyBatteryBean data) {
                Log.e(TAG, "onRingBodyBatteryData : " + GsonUtils.toJson(data));
                dailyResult.append("\n\n" + "RingBodyBatteryBean : " + GsonUtils.toJson(data));
            }

            @Override
            public void onRingStressDetectionData(RingStressDetectionBean data) {
                Log.e(TAG, "onRingStressDetectionData : " + GsonUtils.toJson(data));
                dailyResult.append("\n\n" + "RingStressDetectionBean : " + GsonUtils.toJson(data));
            }

        };

        if (tvDailyResult != null) {
            tvDailyResult.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardUtils.copyText(tvDailyResult.getText().toString().trim());
                    ToastUtils.showShort("copy complete");
                    return false;
                }
            });
        }

        CallBackUtils.deviceInfoCallBack = new DeviceInfoCallBack() {
            @Override
            public void onDeviceInfo(@NonNull DeviceInfoBean deviceInfoBean) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = findViewById(R.id.tvDeviceInfo);
                        String tmp = getString(R.string.s204) + deviceInfoBean.firmwareVersion + " \n" + getString(R.string.s205) + deviceInfoBean.equipmentNumber +
                                " \n" + getString(R.string.s206) + deviceInfoBean.mac + " \n" + getString(R.string.s207) + deviceInfoBean.serialNumber;
                        textView.setText(tmp);
                    }
                });
            }

            @Override
            public void onBatteryInfo(int capacity, int chargeStatus) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = findViewById(R.id.tvBattery);
                        String state = getString(R.string.s208);
                        if (chargeStatus == DeviceInfoCallBack.ChargeStatus.UNKNOWN.getState()) {
                            state = getString(R.string.s208);
                        } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.CHARGING.getState()) {
                            state = getString(R.string.s209);
                        } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.NOT_CHARGING.getState()) {
                            state = getString(R.string.s210);
                        } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.FULL.getState()) {
                            state = getString(R.string.s211);
                        }
                        String tmp = getString(R.string.s212) + capacity + " \n" + getString(R.string.s213) + chargeStatus + " \n " + state;
                        textView.setText(tmp);
                    }
                });
            }
        };

        CallBackUtils.realTimeDataCallback = new RealTimeDataCallBack() {
            @Override
            public void onResult(RealTimeBean bean) {
                tvRealTimeData.setText(bean.toString());
            }

            @Override
            public void onFail() {

            }
        };

        CallBackUtils.emergencyContactSosCallBack = new EmergencyContactSosCallBack() {
            @Override
            public void onSosMessageNeedSent(EmergencyContactBean bean) {
                ToastDialog.showToast(ActivityUtils.getTopActivity(), getString(R.string.s469) + ":" + GsonUtils.toJson(bean));
            }
        };

        CallBackUtils.watchFaceInstallCallBack = new WatchFaceInstallCallBack() {
            @Override
            public void onresult(WatchFaceInstallResultBean result) {
                ToastDialog.showToast(ActivityUtils.getTopActivity(), getString(R.string.s470) + ":" + GsonUtils.toJson(result));
            }
        };


    }


    //region BT CreateBond
    private void checkBTBle() {
        //获取缓存的通话蓝牙mac和昵称  Get cached call bluetooth mac and nickname
        String name = MyApplication.getDeviceTools().getHeadsetName();
        String mac = MyApplication.getDeviceTools().getHeadsetMac();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(mac)) {
            //断开其它设备 Disconnect other devices
            /*ArrayList<String> macs = new ArrayList<>();
            macs.add("D8:27:08:30:B3:5D");
            macs.add("D6:2B:11:20:01:1C");
            BleBCManager.getInstance().disconnectHeadsetBluetoothDevice(macs);*/

            //检测系统未配对，执行配对   The detection system is not paired, perform pairing
            if (!BleBCManager.getInstance().checkBondByMac(mac)) {
                loadingDialog = LoadingDialog.show(this);
                loadingDialog.setCancelable(true);
                BleBCManager.getInstance().createBond(mac, new SearchHeadsetBondListener(mac, name));
                /**
                 * BleBCManager.getInstance().companionDeviceCreateBond(this,mac,"",new SearchHeadsetBondListener(mac,name));
                 *
                 * 传入Activity 报异常：   The incoming Activity reports an exception:
                 * java.lang.ClassCastException: android.app.ContextImpl cannot be cast to android.app.Activity
                 * 解决:检查你的BaseActivity   Solution: check your baseActivity
                 *  override fun attachBaseContext(context: Context) {
                 *      //newConfig .... 修改一些新属性  Modify some new properties
                 *      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                 *           val displayMetrics: DisplayMetrics = context.resources.displayMetrics
                 *           context.resources.updateConfiguration(newConfig,displayMetrics)
                 *           super.attachBaseContext(context)
                 *      }else{
                 *          applyOverrideConfiguration(newConfig)
                 *          super.attachBaseContext(context)
                 *      }
                 */

            } else {
                //Execute the connection ,can leave the result unprocessed
                BleBCManager.getInstance().connectHeadsetBluetoothDevice(mac, null);
            }

        }

    }

    private Dialog loadingDialog;

    private class SearchHeadsetBondListener implements BleBCManager.BondListener {
        String mac;
        String name;

        public SearchHeadsetBondListener(String mac, String name) {
            this.mac = mac;
            this.name = name;
        }

        @Override
        public void onWaiting() {
            LogUtils.d("Waiting :" + mac);
        }

        @Override
        public void onBondError(Exception e) {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            LogUtils.d("Bond Error :" + e);
        }

        @Override
        public void onBonding() {
            LogUtils.d("onBonding :" + mac);
        }

        @Override
        public void onBondFailed() {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            LogUtils.d("onBondFailed :" + mac);
            ToastDialog.showToast(BraceletActivity.this, getString(R.string.s319, name));
            //Instruct the user to select Pro 4_BT_MAC in the phone\'s Settings > Bluetooth > and pair it
            /** @see com.zjw.sdkdemo.ScanDeviceActivity#startBleSetting() */
            //openBleSetting();
        }

        @Override
        public void onBondSucceeded() {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            LogUtils.d("onBondSucceeded :" + mac);
            //Execute the connection ,can leave the result unprocessed
            BleBCManager.getInstance().connectHeadsetBluetoothDevice(mac, null);
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BleBCManager.getInstance().dealCompanionDeviceActivityResult(requestCode, resultCode, data);
    }*/

    //endregion

    private void bindDevice() {
        CallBackUtils.bindDeviceStateCallBack = new BindDeviceStateCallBack() {
            @Override
            public void onDeviceInfo(@NonNull BindDeviceBean bindDeviceBean) {
                if (bindDeviceBean.deviceVerify) {
                    setTitle(getString(R.string.s252));
                    isBind = true;
                    checkBTBle();
                    String userId = ((EditText) findViewById(R.id.etUserId)).getText().toString().trim();
                    ControlBleTools.getInstance().sendAppBindResult(userId, null);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (ControlBleTools.getInstance().isConnect()) {
                                String title = getString(R.string.app_name) + " : " + getString(R.string.ble_connected_tips);
                                setTitle(title);
                            }
                        }
                    }, 5000);
                } else {
                    setTitle(getString(R.string.s214));
                }
            }
        };
        ControlBleTools.getInstance().bindDevice(null);
    }

    private void showTips(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BraceletActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        callback();
        super.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
        //getContentResolver().unregisterContentObserver(mSmsContentObserver);
    }


    /**
     * 运动相关
     *
     * @param view
     */
    public void toSportInfo(View view) {
        startActivity(new Intent(BraceletActivity.this, SportActivity.class));
    }

    /**
     * 提醒相关
     *
     * @param view
     */
    public void toRemindInfo(View view) {
        startActivity(new Intent(BraceletActivity.this, RemindActivity.class));
    }

    /**
     * 小功能综合
     */
    public void toMicro(View view) {
        startActivity(new Intent(BraceletActivity.this, MicroActivity.class));
    }

    /**
     * 设置相关
     *
     * @param view
     */
    public void toSetInfo(View view) {
        startActivity(new Intent(BraceletActivity.this, SetActivity.class));
    }

    /**
     * 获取设备固件日志
     *
     * @param view
     */
    public void getDeviceFirmwareLog(View view) {
        if (true) {
            startActivity(new Intent(BraceletActivity.this, DeviceLogTestActivity.class));
            return;
        }
        CallBackUtils.firmwareLogStateCallBack = new FirmwareLogStateCallBack() {
            @Override
            public void onFirmwareLogState(int state) {
                LogUtils.d("getDeviceFirmwareLog state:" + state);
                if (state == FirmwareLogState.START.getState()) {
                    // start
                    loadingDialog = LoadingDialog.show(BraceletActivity.this);
                } else if (state == FirmwareLogState.UPLOADING.getState()) {
                    // uploading....
                } else if (state == FirmwareLogState.END.getState()) {
                    // end
                    if (!isFinishing() && !isDestroyed() && loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                }
            }

            @Override
            public void onFirmwareLogFilePath(String filePath) {

            }
        };
        ControlBleTools.getInstance().getFirmwareLog(new ParsingStateManager.SendCmdStateListener() {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(R.string.s220);
                        break;
                    default:
                        MyApplication.showToast(R.string.s221);
                        break;
                }
            }
        });
    }

    public void getFirmwareTrackingLog(View view) {
        CallBackUtils.firmwareTrackingLogCallBack = new FirmwareTrackingLogCallBack() {
            @Override
            public void onTrackingLog(TrackingLogBean trackingLogBean) {
                LogUtils.d("TrackingFileName:" + trackingLogBean.fileName + "\n data:" + ConvertUtils.bytes2String(trackingLogBean.trackingLog, "UTF-8"));
                com.zhapp.ble.utils.DeviceLog.writeFile(trackingLogBean.fileName, trackingLogBean.trackingLog);
            }
        };

        ControlBleTools.getInstance().getFirmwareTrackingLog(new ParsingStateManager.SendCmdStateListener() {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(R.string.s220);
                        break;
                    default:
                        MyApplication.showToast(R.string.s221);
                        break;
                }
            }
        });
    }


    /**
     * 表盘相关
     *
     * @param view
     */
    public void toClockDialInfo(View view) {
        startActivity(new Intent(BraceletActivity.this, ClockDialActivity.class));
    }

    /**
     * Diy表盘
     *
     * @param view
     */
    public void toDiyDial(View view) {
        if (!ControlBleTools.getInstance().isConnect()) {
            ToastDialog.showToast(this, getString(R.string.s294));
            return;
        }
        startActivity(new Intent(BraceletActivity.this, DiySelectActivity.class));
    }

    public void toOldDiyDial(View view) {
        if (!ControlBleTools.getInstance().isConnect()) {
            ToastDialog.showToast(this, getString(R.string.s294));
            return;
        }
        startActivity(new Intent(BraceletActivity.this, OldDiyDialActivity.class));
    }

    public void toDiyDial2(View view) {
        if (!ControlBleTools.getInstance().isConnect()) {
            ToastDialog.showToast(this, getString(R.string.s294));
            return;
        }
        startActivity(new Intent(BraceletActivity.this, SimpleDiyActivity.class));
    }


    /**
     * 自定义相册表盘
     *
     * @param view
     */
    public void toCustomAlbumDial(View view) {
        if (!ControlBleTools.getInstance().isConnect()) {
            ToastDialog.showToast(this, getString(R.string.s294));
            return;
        }
        startActivity(new Intent(BraceletActivity.this, NewAlbumStyleDialActivity.class));
    }

    /**
     * OTA相关
     *
     * @param view
     */
    public void toOTAInfo(View view) {
        startActivity(new Intent(BraceletActivity.this, OTAActivity.class));
    }


    public void toRing(View view) {
        startActivity(new Intent(BraceletActivity.this, RingTestActivity.class));
    }

    public void btScanTest(View view) {
        /*M65A 需要增加
        if(gattService.getUuid().equals(UUID.fromString("0000aa01-0000-1000-8000-00805f9b34fb"))){
             channelList.add(gattService.getCharacteristic(UUID.fromString("0000aa03-0000-1000-8000-00805f9b34fb")));
        }*/
        BleBCManager.getInstance().createBond(deviceAddress, new BleBCManager.BondListener() {
            @Override
            public void onWaiting() {
            }

            @Override
            public void onBondError(Exception e) {
                ToastUtils.showShort("配对异常：" + e);
            }

            @Override
            public void onBonding() {
                ToastUtils.showShort("配对中");
            }

            @Override
            public void onBondFailed() {
                ToastUtils.showShort("配对失败/取消");
            }

            @Override
            public void onBondSucceeded() {
                ToastUtils.showShort("配对成功");
            }
        });
    }

    public void btTest(View view) {
        BleBCManager.getInstance().scanlessCreateBond(deviceAddress, new BleBCManager.BondListener() {
            @Override
            public void onWaiting() {
            }

            @Override
            public void onBondError(Exception e) {
                ToastUtils.showShort("配对异常：" + e);
            }

            @Override
            public void onBonding() {
                ToastUtils.showShort("配对中");
            }

            @Override
            public void onBondFailed() {
                ToastUtils.showShort("配对失败/取消");
            }

            @Override
            public void onBondSucceeded() {
                ToastUtils.showShort("配对成功");
            }
        });
    }


    @SuppressLint("MissingPermission")
    public void toClassicPair(View view) {
        String mac = "E3:4D:D2:4D:71:65";
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
        LogUtils.i(TAG, "device paired device = Name:" + device.getName() + ", Address:" + device.getAddress() +
                ", BondState:" + device.getBondState() + ", Type:" + device.getType());
        device.createBond();
    }

    public void connectDevice(View view) {
        Log.i("BraceletActivity", "connectDevice()");
        ControlBleTools.getInstance().setBleStateCallBack(new BleStateCallBack() {
            @Override
            public void onConnectState(int state) {
                Log.i("BraceletActivity", "onConnectState state  = " + state);
                BleConnectState.getInstance().postValue(state);
            }
        });
        Log.i("BraceletActivity", "connect()01 deviceProtocol = " + deviceProtocol + " deviceName = " + deviceName + " deviceAddress = " + deviceAddress);
        ControlBleTools.getInstance().connect(deviceName, deviceAddress, deviceProtocol);
    }

    public void disconnectDevice(View view) {
        Log.i("BraceletActivity", "disconnectDevice()");
        ControlBleTools.getInstance().disconnect();
        //AppUtils.relaunchApp(true);
    }


    public void bindDevice(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().requestDeviceBindState(null);
        }
    }

    public void verifyUserId(View view) {
        CallBackUtils.verifyUserIdCallBack = new VerifyUserIdCallBack() {
            @Override
            public void onVerifyState(int state) {
                String userId = ((EditText) findViewById(R.id.etUserId2)).getText().toString().trim();
                showTips(getString(R.string.s734) + " : " + userId + " == " + (state == 0 ? getString(R.string.s735) : getString(R.string.s736)));
            }
        };
        if (ControlBleTools.getInstance().isConnect()) {
            String userId = ((EditText) findViewById(R.id.etUserId2)).getText().toString().trim();
            ControlBleTools.getInstance().verifyUserId(userId, null);
        }
    }

    public void unBindDevice(View view) {
        CallBackUtils.unbindDeviceCallBack = new UnbindDeviceCallBack() {
            @Override
            public void unbindDeviceSuccess() {
                //清除通话蓝牙缓存信息  Clear call bluetooth cache information
                isBind = false;
                String name = MyApplication.getDeviceTools().getHeadsetName();
                String mac = MyApplication.getDeviceTools().getHeadsetMac();
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(mac)) {
                    BleBCManager.getInstance().removeBond(mac);
                    MyApplication.getDeviceTools().setHeadsetName("");
                    MyApplication.getDeviceTools().setHeadsetMac("");
                }

                ControlBleTools.getInstance().disconnect();
                startActivity(new Intent(BraceletActivity.this, ScanDeviceActivity.class));
                BraceletActivity.this.finish();
            }
        };
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().unbindDevice(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.e(TAG, "unbindDevice onState :" + state);
                }
            });
        }
    }

    public void unbindDeviceWaitConfirmation(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().unbindDeviceWaitConfirmation(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.e(TAG, "unbindDeviceWaitConfirmation onState :" + state);
                    if (state == SendCmdState.SUCCEED) {
                        //断开连接 disconnect
                        ControlBleTools.getInstance().disconnect();
                        //清除bt蓝牙配对 removeBond
                        String name = MyApplication.getDeviceTools().getHeadsetName();
                        String mac = MyApplication.getDeviceTools().getHeadsetMac();
                        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(mac)) {
                            BleBCManager.getInstance().removeBond(mac);
                            MyApplication.getDeviceTools().setHeadsetName("");
                            MyApplication.getDeviceTools().setHeadsetMac("");
                        }
                        //返回到扫描界面   Return to the scanning interface
                        AppUtils.relaunchApp(true);
                    }
                }
            });
        }
    }

    public void getDailyHistoryData(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().getDailyHistoryData(null);
        }
    }

    private boolean receptionStatus = false;

    public void ringExecutesDeleteDailyData(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            int status = 1;
            if (receptionStatus) {
                status = 0;
                receptionStatus = false;
            } else {
                status = 1;
                receptionStatus = true;
            }
            ControlBleTools.getInstance().ringExecutesDeleteDailyData(status, null);
        }
    }

    public void ringGetDailyHistoryData(View view) {
        startActivity(new Intent(this, SyncRingRTCFitnessActivity.class));
    }

    public void getDeviceInfo(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().getDeviceInfo(null);
        }
    }

    public void getDeviceBatteryInfo(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().getDeviceBattery(null);
        }
    }

    public void realTimeDataSwitch(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            if (view.getTag() == null) {
                ControlBleTools.getInstance().realTimeDataSwitch(true, null);
                view.setTag("1");
            } else {
                if (view.getTag().equals("0")) {
                    ControlBleTools.getInstance().realTimeDataSwitch(true, null);
                    view.setTag("1");
                } else {
                    ControlBleTools.getInstance().realTimeDataSwitch(false, null);
                    view.setTag("0");
                }
            }
        }
    }


    public void getDisconnectReason(View view) {
        CallBackUtils.disconnectReasonCallBack = new DisconnectReasonCallBack() {
            @Override
            public void onReason(DeviceInfoBean deviceInfoBean) {
                ToastDialog.showToast(BraceletActivity.this, "getDisconnectReason = deviceInfoBean = " + deviceInfoBean.toString());
            }
        };

        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().getDisconnectReason(null);
        }
    }

    public void sifli(View view) {
        startActivity(new Intent(this, SifliTestActivity.class));
    }

    public void toRealTimeHeartRate(View view) {
        startActivity(new Intent(this, RealTimeHeartRateActivity.class));
    }

    public void toLoopBind(View view) {
        startActivity(new Intent(this, LoopBindActivity.class));
    }

    public void toLoopFitness(View view) {
        startActivity(new Intent(this, LoopFitnessActivity.class));
    }

    public void toEmoji(View view) {
        startActivity(new Intent(this, EmojiActivity.class));
    }

    /*public void getTestDailyHistoryData(View view) {
        startActivity(new Intent(this, TestSyncActivity.class));
    }*/

    public void toActiveMeasure(View view) {
        startActivity(new Intent(this, ActiveMeasureTypeActivity.class).putExtra("mType", 1));
    }

    public void toActiveEsim(View view) {
        startActivity(new Intent(this, ESimActivity.class));
    }

    public void toSpp(View view) {

    }


}
