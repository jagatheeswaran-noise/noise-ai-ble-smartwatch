package com.zjw.sdkdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.ScanDeviceBean;
import com.zhapp.ble.callback.ScanDeviceCallBack;
import com.zhapp.ble.callback.ZHInitStatusCallBack;
import com.zhapp.ble.manager.BleBCManager;
import com.zhapp.ble.scan.no.nordicsemi.scanner.ScanRecord;
import com.zhapp.ble.scan.no.nordicsemi.scanner.ScanResult;
import com.zhapp.ble.utils.BleUtils;
import com.zjw.sdkdemo.adapter.LeDeviceListAdapter;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.utils.DeviceTools;
import com.zjw.sdkdemo.utils.LoadingDialog;
import com.zjw.sdkdemo.utils.ToastDialog;

import java.util.Locale;
import java.util.Set;


public class ScanDeviceActivity extends AppCompatActivity implements View.OnClickListener {

    private DeviceTools mDeviceTools = MyApplication.getDeviceTools();

    private ListView lvDevice;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private int ScanDeviceTime = 10 * 1000;

    private boolean isBLEScanning = false;
    private Handler mHandler;
    private Handler ScanHandler = new Handler(Looper.getMainLooper());
    public BluetoothAdapter bluetoothAdapter = null;
    private Button scanStart, scanStop;
    private EditText etSearch;
    private String searchText = "";
    private final static String SP_SEARCH = "SP_SEARCH";
    private final static String SP_PROTOCOL_NAME = "SP_PROTOCOL_NAME";
    private Dialog loadingDialog;

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.scan_start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0x999);
                    return;
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0x9999);
                    return;
                }
            }

            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(mIntent, 0x99999);
            }

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!providerEnabled) {
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsIntent);
                return;
            }

            ControlBleTools.getInstance().setInitStatusCallBack(new ZHInitStatusCallBack() {
                @Override
                public void onInitComplete() {
                    scanLeDevice(true);
                }
            });
        } else if (id == R.id.scan_stop) {
            stopSCan();
        }
    }

    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scand_device);
        setTitle(getString(R.string.s215));
        initView();
        initHandle();
    }

    private void initView() {
        lvDevice = findViewById(R.id.lv_device);
        etSearch = findViewById(R.id.et_search);
        scanStart = findViewById(R.id.scan_start);
        scanStop = findViewById(R.id.scan_stop);

        mLeDeviceListAdapter = new LeDeviceListAdapter(ScanDeviceActivity.this);
        lvDevice.setAdapter(mLeDeviceListAdapter);
        lvDevice.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            lvDevice.post(new Runnable() {
                @Override
                public void run() {
                    itemClick(mLeDeviceListAdapter.getDevice(position));
                }
            });
        });

        searchText = SPUtils.getInstance().getString(SP_SEARCH, "");
        etSearch.setText(searchText);
        etSearch.setSelection(searchText.length());
        etSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s)) {
                            searchText = s.toString();
                            SPUtils.getInstance().put(SP_SEARCH, searchText);
                            mLeDeviceListAdapter.clearAll();
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    }
                }
        );

        scanStart.setOnClickListener(this);
        scanStop.setOnClickListener(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void startBraceletActivity(ScanDeviceBean device) {
        scanLeDevice(false);
        if (TextUtils.equals(device.protocolName, BleCommonAttributes.DEVICE_PROTOCOL_APRICOT)) {
            Intent intent = new Intent(ScanDeviceActivity.this, BraceletActivity.class);
            intent.putExtra(BraceletActivity.EXTRA_DEVICE_ADDRESS, device.address)
                    .putExtra(BraceletActivity.EXTRA_DEVICE_NAME, device.name)
                    .putExtra(BraceletActivity.EXTRA_DEVICE_PROTOCOL, device.protocolName)
                    .putExtra(BraceletActivity.EXTRA_DEVICE_ISBIND, device.isBind)
            ;
            //intent.putExtra(BraceletActivity.EXTRA_DEVICE_SCAN, device.scanRecord);
            startActivity(intent);
            SPUtils.getInstance().put(SP_PROTOCOL_NAME + device.address, BleCommonAttributes.DEVICE_PROTOCOL_APRICOT);
        } else if (TextUtils.equals(device.protocolName, BleCommonAttributes.DEVICE_PROTOCOL_BERRY) ||
                TextUtils.equals(device.protocolName, BleCommonAttributes.DEVICE_PROTOCOL_BERRY_TEST)||
                TextUtils.equals(device.protocolName, BleCommonAttributes.DEVICE_PROTOCOL_BERRY_RING)) {
            Intent intent = new Intent(ScanDeviceActivity.this, BerryDeviceActivity.class);

            intent.putExtra(BraceletActivity.EXTRA_DEVICE_ADDRESS, device.address)
                    .putExtra(BraceletActivity.EXTRA_DEVICE_NAME, device.name)
                    .putExtra(BraceletActivity.EXTRA_DEVICE_PROTOCOL, device.protocolName)
                    .putExtra(BraceletActivity.EXTRA_DEVICE_ISBIND, device.isBind);
            startActivity(intent);
            SPUtils.getInstance().put(SP_PROTOCOL_NAME + device.address, device.protocolName);
        } else {
            new AlertDialog.Builder(ActivityUtils.getTopActivity())
                    .setTitle(getString(R.string.s633))
                    .setMessage(getString(R.string.s632))
                    .setPositiveButton(getString(R.string.s633), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ScanDeviceActivity.this, BerryDeviceActivity.class);
                            intent.putExtra(BraceletActivity.EXTRA_DEVICE_ADDRESS, device.address)
                                    .putExtra(BraceletActivity.EXTRA_DEVICE_NAME, device.name)
                                    .putExtra(BraceletActivity.EXTRA_DEVICE_PROTOCOL, BleCommonAttributes.DEVICE_PROTOCOL_BERRY_TEST)
                                    .putExtra(BraceletActivity.EXTRA_DEVICE_ISBIND, device.isBind);
                            startActivity(intent);
                            SPUtils.getInstance().put(SP_PROTOCOL_NAME + device.address, BleCommonAttributes.DEVICE_PROTOCOL_BERRY_TEST);
                        }
                    }).create().show();
        }

    }

    //region N008 BT Ble Bond example
    private void startBleSetting() {
        //进入蓝牙设置
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    /*
    先配br后连ble
    private void itemClick(ScanDeviceBean device) {
        //设备支持通话蓝牙 & 通话蓝牙蓝牙mac不为空  The device supports call bluetooth & call bluetooth bluetooth mac is not empty
        if (device.isSupportHeadset && !TextUtils.isEmpty(device.headsetMac)) {
            //SP保存设备通话蓝牙的mac地址  SP saves the mac address of the device calling bluetooth
            MyApplication.getDeviceTools().setHeadsetMac(device.headsetMac);
            //SP保存设备通话蓝牙的昵称  SP saves the nickname of the device calling bluetooth
            if (TextUtils.equals(device.deviceType, "30000")) {
                String hBleName = "Pro 4_BT_" + BleUtils.getMacLastStr(device.headsetMac, 4);
                MyApplication.getDeviceTools().setHeadsetName(hBleName);
            } else { //else if 其它产品的通话蓝牙蓝牙名  Other products' call bluetooth bluetooth name
                //TODO 请换成真实的通话蓝牙蓝牙名  Please replace it with the real call bluetooth bluetooth name
                String hBleName = "XXX_Calling_" + BleUtils.getMacLastStr(device.headsetMac, 4);
                MyApplication.getDeviceTools().setHeadsetName(hBleName);
            }
            checkBTBle(device);
        }else{
            MyApplication.getDeviceTools().setHeadsetName("");
            MyApplication.getDeviceTools().setHeadsetMac("");
            startBraceletActivity(device);
        }
    }*/

    /**
     * 先连ble后配br
     *
     * @param device
     */
    private void itemClick(ScanDeviceBean device) {
        //设备支持通话蓝牙 & 通话蓝牙蓝牙mac不为空  The device supports call bluetooth & call bluetooth bluetooth mac is not empty
        if (device.isSupportHeadset && !TextUtils.isEmpty(device.headsetMac)) {
            //SP保存设备通话蓝牙的mac地址  SP saves the mac address of the device calling bluetooth
            MyApplication.getDeviceTools().setHeadsetMac(device.headsetMac);
            //SP保存设备通话蓝牙的昵称  SP saves the nickname of the device calling bluetooth
            if (TextUtils.equals(device.deviceType, "30000")) {
                String hBleName = "Pro 4_BT_" + BleUtils.getMacLastStr(device.headsetMac, 4);
                MyApplication.getDeviceTools().setHeadsetName(hBleName);
            } else { //else if 其它产品的通话蓝牙蓝牙名  Other products' call bluetooth bluetooth name
                //TODO 请换成真实的通话蓝牙蓝牙名  Please replace it with the real call bluetooth bluetooth name
                String hBleName = "XXX_Calling_" + BleUtils.getMacLastStr(device.headsetMac, 4);
                MyApplication.getDeviceTools().setHeadsetName(hBleName);
            }
        } else {
            MyApplication.getDeviceTools().setHeadsetName("");
            MyApplication.getDeviceTools().setHeadsetMac("");
        }
        startBraceletActivity(device);
    }


    //endregion

    //region BT CreateBond
    private void checkBTBle(ScanDeviceBean device) {
        //获取缓存的通话蓝牙mac和昵称  Get cached call bluetooth mac and nickname
        String name = MyApplication.getDeviceTools().getHeadsetName();
        String mac = MyApplication.getDeviceTools().getHeadsetMac();
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(mac)) {
            //设备未配对
            if (!device.isHeadsetBond) {
                //手机未配对
                loadingDialog = LoadingDialog.show(this);
                BleBCManager.getInstance().createBond(mac, new SearchHeadsetBondListener(device));
                return;
            }
            //设备已配对
            //是否当前手机配对
            if (!BleBCManager.getInstance().checkBondByMac(mac)) {
                MyApplication.showToast(getString(R.string.s364));
                return;
            }
            //设备已配对&与当前手机已配对
            //执行连接，可以不处理结果 Execute the connection ,can leave the result unprocessed
            BleBCManager.getInstance().connectHeadsetBluetoothDevice(mac, null);
            //去绑定页面
            startBraceletActivity(device);
        }
    }

    private class SearchHeadsetBondListener implements BleBCManager.BondListener {
        ScanDeviceBean device;

        public SearchHeadsetBondListener(ScanDeviceBean device) {
            this.device = device;
        }

        @Override
        public void onWaiting() {
            LogUtils.d("Waiting :" + device.headsetMac);
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
            LogUtils.d("onBonding :" + device.headsetMac);
        }

        @Override
        public void onBondFailed() {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            LogUtils.d("onBondFailed :" + device.headsetMac);
            //TODO 请换成真实的通话蓝牙蓝牙名  Please replace it with the real call bluetooth bluetooth name
            ToastDialog.showToast(ScanDeviceActivity.this, getString(R.string.s319, "XXX_Calling_" + BleUtils.getMacLastStr(device.headsetMac, 4)));
            //Instruct the user to select Pro 4_BT_MAC in the phone\'s Settings > Bluetooth > and pair it
            /** @see com.zjw.sdkdemo.ScanDeviceActivity#startBleSetting() */
            //openBleSetting();
        }

        @Override
        public void onBondSucceeded() {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            LogUtils.d("onBondSucceeded :" + device.headsetMac);
            //Execute the connection ,can leave the result unprocessed
            BleBCManager.getInstance().connectHeadsetBluetoothDevice(device.headsetMac, null);

            startBraceletActivity(device);
        }
    }

    //endregion

    /**
     * 开始扫描
     *
     * @param view
     */
    public void startScanLeDevice(View view) {
        scanLeDevice(true);
    }


    /**
     * 停止扫描
     *
     * @param view
     */
    public void stopScanLeDevice(View view) {
        scanLeDevice(false);
    }


    protected void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
        ScanHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
    }


    public final static int MESSAGE_BLE_SCANF = 101;


    void initHandle() {
        mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MESSAGE_BLE_SCANF:
                        ScanDeviceBean deviceBean = (ScanDeviceBean) msg.obj;
                        LogUtils.d("device:" + GsonUtils.toJson(deviceBean));
                        final String deviceName = deviceBean.name;
                        if (deviceName != null && deviceName.length() > 0) {
                            boolean isCanadd = true;
                            if (!TextUtils.isEmpty(searchText)
                                    && !deviceName.toLowerCase(Locale.ENGLISH).contains(searchText.toLowerCase(Locale.ENGLISH))
                                    && !deviceBean.address.toLowerCase(Locale.ENGLISH).equalsIgnoreCase(searchText.toLowerCase(Locale.ENGLISH))
                            ) {
                                isCanadd = false;
                            }
                            if (isCanadd) {
                                mLeDeviceListAdapter.addDevice(deviceBean);
                                lvDevice.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mLeDeviceListAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    private class BluetoothScanDeviceCallBack implements ScanDeviceCallBack {
        @Override
        public void onBleScan(@NonNull ScanDeviceBean device) {
            if (isBLEScanning) {
                sendDeviceDate(device);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initTestBindDevice() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            ScanDeviceBean scanDeviceBean = new ScanDeviceBean();
            scanDeviceBean.device = device;
            scanDeviceBean.name = device.getName();
            scanDeviceBean.address = device.getAddress();
            scanDeviceBean.protocolName = SPUtils.getInstance().getString(SP_PROTOCOL_NAME + scanDeviceBean.address, BleCommonAttributes.DEVICE_PROTOCOL_BERRY);
            sendDeviceDate(scanDeviceBean);
        }

    }

    void sendDeviceDate(ScanDeviceBean device) {
        Message message = new Message();
        message.what = MESSAGE_BLE_SCANF;
        message.obj = device;
        mHandler.sendMessage(message);
    }

    @SuppressLint({"NewApi", "MissingPermission"})
    private void scanLeDevice(final boolean enable) {

        if (enable) {
            if (!this.bluetoothAdapter.isEnabled()) {
                this.bluetoothAdapter.enable();
            }
            // Stops scanning after a pre-defined scan period.
            ScanHandler.removeCallbacksAndMessages(null);
            ScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSCan();
                    // return ;
                    ControlBleTools.getInstance().stopScanDevice();
                }
            }, ScanDeviceTime);

            // 正在扫描
            startSCan();
            ControlBleTools.getInstance().startScanDevice(new BluetoothScanDeviceCallBack());

            /*BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final ScanResult scanResult = new ScanResult(device, ScanRecord.parseFromBytes(scanRecord), rssi, SystemClock.elapsedRealtimeNanos());
                    ScanDeviceBean scanDeviceBean = new ScanDeviceBean(scanResult);
                }
            };
            //mBluetoothAdapter.startLeScan(mLeScanCallback);*/

        } else {
            stopSCan();
            ScanHandler.removeCallbacksAndMessages(null);
            ControlBleTools.getInstance().stopScanDevice();
        }
    }

    void stopSCan() {
        isBLEScanning = false;
    }

    void startSCan() {
        mLeDeviceListAdapter.clear();
        isBLEScanning = true;
        initTestBindDevice();
    }
}

