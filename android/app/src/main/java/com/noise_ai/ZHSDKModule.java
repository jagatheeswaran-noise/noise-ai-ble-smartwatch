package com.noise_ai;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.BindDeviceBean;
import com.zhapp.ble.bean.DeviceInfoBean;
import com.zhapp.ble.bean.ScanDeviceBean;
import com.zhapp.ble.callback.BindDeviceStateCallBack;
import com.zhapp.ble.callback.BleStateCallBack;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.DeviceInfoCallBack;
import com.zhapp.ble.callback.ScanDeviceCallBack;
import com.zhapp.ble.callback.VerifyUserIdCallBack;
import com.zhapp.ble.callback.RequestDeviceBindStateCallBack;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;

import java.util.ArrayList;
import java.util.List;

public class ZHSDKModule extends ReactContextBaseJavaModule {
    private static final String TAG = "ZHSDKModule";
    private static final String MODULE_NAME = "ZHSDKModule";
    
    private ReactApplicationContext reactContext;
    private List<ScanDeviceBean> scannedDevices = new ArrayList<>();
    private boolean isScanning = false;
    private ScanDeviceCallBack scanCallback;

    public ZHSDKModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        setupCallbacks();
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    private void setupCallbacks() {
        // Setup scan callback
        scanCallback = new ScanDeviceCallBack() {
            @Override
            public void onBleScan(@NonNull ScanDeviceBean device) {
                Log.d(TAG, "Device found: " + device.name + " (" + device.address + ")");
                
                if (isScanning) {
                    // Add device to list if not already present
                    boolean exists = false;
                    for (ScanDeviceBean existingDevice : scannedDevices) {
                        if (existingDevice.address.equals(device.address)) {
                            exists = true;
                            break;
                        }
                    }
                    
                    if (!exists) {
                        scannedDevices.add(device);
                        sendDeviceFoundEvent(device);
                        Log.d(TAG, "Added device to list: " + device.name);
                    }
                }
            }
        };

        // Setup connection state callback
        ControlBleTools.getInstance().setBleStateCallBack(new BleStateCallBack() {
            @Override
            public void onConnectState(int state) {
                Log.d(TAG, "Connection state changed: " + state);
                sendConnectionStateEvent(state);
            }
        });
    }

    @ReactMethod
    public void checkSDKStatus(Promise promise) {
        try {
            // Check if ControlBleTools is available
            boolean sdkAvailable = ControlBleTools.getInstance() != null;
            Log.d(TAG, "SDK status check - Available: " + sdkAvailable);
            promise.resolve(sdkAvailable);
        } catch (Exception e) {
            Log.e(TAG, "Error checking SDK status", e);
            promise.reject("SDK_ERROR", "Failed to check SDK status: " + e.getMessage());
        }
    }

    @ReactMethod
    public void checkBluetoothPermissions(Promise promise) {
        Activity currentActivity = reactContext.getCurrentActivity();
        if (currentActivity == null) {
            promise.reject("NO_ACTIVITY", "No current activity");
            return;
        }

        boolean hasPermissions = true;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermissions = ActivityCompat.checkSelfPermission(currentActivity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(currentActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        
        hasPermissions = hasPermissions && ActivityCompat.checkSelfPermission(currentActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        promise.resolve(hasPermissions);
    }

    @ReactMethod
    public void requestBluetoothPermissions(Promise promise) {
        Activity currentActivity = reactContext.getCurrentActivity();
        if (currentActivity == null) {
            promise.reject("NO_ACTIVITY", "No current activity");
            return;
        }

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        ActivityCompat.requestPermissions(currentActivity, permissions, 1001);
        promise.resolve(true);
    }

    @ReactMethod
    public void isBluetoothEnabled(Promise promise) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            promise.resolve(false);
        } else {
            promise.resolve(bluetoothAdapter.isEnabled());
        }
    }

    @ReactMethod
    public void startDeviceScan(Promise promise) {
        try {
            // Check Bluetooth adapter
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                promise.reject("NO_BLUETOOTH", "Bluetooth adapter not available");
                return;
            }
            
            if (!bluetoothAdapter.isEnabled()) {
                promise.reject("BLUETOOTH_DISABLED", "Bluetooth is not enabled");
                return;
            }
            
            scannedDevices.clear();
            isScanning = true;
            
            Log.d(TAG, "Starting device scan with callback: " + scanCallback);
            ControlBleTools.getInstance().startScanDevice(scanCallback);
            
            Log.d(TAG, "Device scan started successfully");
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error starting device scan", e);
            promise.reject("SCAN_ERROR", "Failed to start device scan: " + e.getMessage());
        }
    }

    @ReactMethod
    public void stopDeviceScan(Promise promise) {
        try {
            isScanning = false;
            ControlBleTools.getInstance().stopScanDevice();
            
            Log.d(TAG, "Device scan stopped");
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping device scan", e);
            promise.reject("SCAN_ERROR", "Failed to stop device scan: " + e.getMessage());
        }
    }

    @ReactMethod
    public void getScannedDevices(Promise promise) {
        WritableArray devices = Arguments.createArray();
        
        for (ScanDeviceBean device : scannedDevices) {
            WritableMap deviceMap = Arguments.createMap();
            deviceMap.putString("name", device.name);
            deviceMap.putString("address", device.address);
            deviceMap.putString("protocol", device.protocolName);
            deviceMap.putInt("rssi", device.rssi);
            deviceMap.putBoolean("isBind", device.isBind);
            devices.pushMap(deviceMap);
        }
        
        promise.resolve(devices);
    }

    @ReactMethod
    public void connectDevice(String deviceName, String deviceAddress, String protocol, Promise promise) {
        try {
            Log.d(TAG, "Connecting to device: " + deviceName + " (" + deviceAddress + ")");
            ControlBleTools.getInstance().connect(deviceName, deviceAddress, protocol);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device", e);
            promise.reject("CONNECTION_ERROR", "Failed to connect to device: " + e.getMessage());
        }
    }

    @ReactMethod
    public void disconnectDevice(Promise promise) {
        try {
            ControlBleTools.getInstance().disconnect();
            Log.d(TAG, "Device disconnected");
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting device", e);
            promise.reject("DISCONNECTION_ERROR", "Failed to disconnect device: " + e.getMessage());
        }
    }

    @ReactMethod
    public void isDeviceConnected(Promise promise) {
        boolean isConnected = ControlBleTools.getInstance().isConnect();
        promise.resolve(isConnected);
    }

    @ReactMethod
    public void verifyUserId(String userId, Promise promise) {
        CallBackUtils.verifyUserIdCallBack = new VerifyUserIdCallBack() {
            @Override
            public void onVerifyState(int state) {
                Log.d(TAG, "User ID verification state: " + state);
                WritableMap result = Arguments.createMap();
                result.putBoolean("success", state == 0);
                result.putString("userId", userId);
                sendEvent("onUserIdVerified", result);
            }
        };

        try {
            ControlBleTools.getInstance().verifyUserId(userId, null);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying user ID", e);
            promise.reject("VERIFY_ERROR", "Failed to verify user ID: " + e.getMessage());
        }
    }

    @ReactMethod
    public void requestDeviceBindState(Promise promise) {
        CallBackUtils.requestDeviceBindStateCallBack = (state) -> {
            Log.d(TAG, "Device bind state: " + state);
            WritableMap result = Arguments.createMap();
            result.putBoolean("isBound", state);
            sendEvent("onBindStateChecked", result);
        };

        try {
            ControlBleTools.getInstance().requestDeviceBindState(null);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting bind state", e);
            promise.reject("BIND_STATE_ERROR", "Failed to request bind state: " + e.getMessage());
        }
    }

    @ReactMethod
    public void bindDevice(Promise promise) {
        CallBackUtils.bindDeviceStateCallBack = new BindDeviceStateCallBack() {
            @Override
            public void onDeviceInfo(@NonNull BindDeviceBean bindDeviceBean) {
                Log.d(TAG, "Bind device info received: " + bindDeviceBean.deviceVerify);
                WritableMap result = Arguments.createMap();
                result.putBoolean("deviceVerify", bindDeviceBean.deviceVerify);
                sendEvent("onBindDeviceInfo", result);
            }
        };

        try {
            ControlBleTools.getInstance().bindDevice(null);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error binding device", e);
            promise.reject("BIND_ERROR", "Failed to bind device: " + e.getMessage());
        }
    }

    @ReactMethod
    public void sendBindingResult(String userId, Promise promise) {
        try {
            ControlBleTools.getInstance().sendAppBindResult(userId, new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Binding result sent: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    sendEvent("onBindingResultSent", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error sending binding result", e);
            promise.reject("BIND_RESULT_ERROR", "Failed to send binding result: " + e.getMessage());
        }
    }

    // Berry Protocol specific binding methods
    @ReactMethod
    public void setUserIdByBerryProtocol(String userId, String deviceModel, String sdkVersion, Promise promise) {
        try {
            ControlBleTools.getInstance().setUserIdByBerryProtocol(userId, deviceModel, sdkVersion, new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Set user ID by Berry protocol: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    result.putString("userId", userId);
                    sendEvent("onUserIdSetByBerry", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error setting user ID by Berry protocol", e);
            promise.reject("SET_USERID_ERROR", "Failed to set user ID by Berry protocol: " + e.getMessage());
        }
    }

    @ReactMethod
    public void bindDeviceByBerryProtocol(Promise promise) {
        try {
            ControlBleTools.getInstance().bindDevice(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Bind device by Berry protocol: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    sendEvent("onDeviceBoundByBerry", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error binding device by Berry protocol", e);
            promise.reject("BIND_BERRY_ERROR", "Failed to bind device by Berry protocol: " + e.getMessage());
        }
    }

    @ReactMethod
    public void bindDeviceSuccessByBerryProtocol(Promise promise) {
        try {
            ControlBleTools.getInstance().bindDeviceSucByBerryProtocol(true, new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Bind device success by Berry protocol: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    sendEvent("onDeviceBindSuccessByBerry", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error completing bind device by Berry protocol", e);
            promise.reject("BIND_SUCCESS_BERRY_ERROR", "Failed to complete bind device by Berry protocol: " + e.getMessage());
        }
    }

    @ReactMethod
    public void performOneKeyBindByBerry(String userId, String deviceModel, String sdkVersion, Promise promise) {
        try {
            // Step 1: Set User ID
            ControlBleTools.getInstance().setUserIdByBerryProtocol(userId, deviceModel, sdkVersion, new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "One-key bind - User ID set: " + state);
                    if (state == SendCmdState.SUCCEED) {
                        // Step 2: Bind Device
                        ControlBleTools.getInstance().bindDevice(new ParsingStateManager.SendCmdStateListener() {
                            @Override
                            public void onState(SendCmdState state) {
                                Log.d(TAG, "One-key bind - Device bound: " + state);
                                if (state == SendCmdState.SUCCEED) {
                                    // Step 3: Complete binding (with delay)
                                    new android.os.Handler().postDelayed(() -> {
                                        ControlBleTools.getInstance().bindDeviceSucByBerryProtocol(true, new ParsingStateManager.SendCmdStateListener() {
                                            @Override
                                            public void onState(SendCmdState state) {
                                                Log.d(TAG, "One-key bind - Binding completed: " + state);
                                                WritableMap result = Arguments.createMap();
                                                result.putBoolean("success", state == SendCmdState.SUCCEED);
                                                result.putString("userId", userId);
                                                sendEvent("onOneKeyBindCompleted", result);
                                            }
                                        });
                                    }, 1000);
                                } else {
                                    WritableMap result = Arguments.createMap();
                                    result.putBoolean("success", false);
                                    result.putString("error", "Failed to bind device");
                                    sendEvent("onOneKeyBindCompleted", result);
                                }
                            }
                        });
                    } else {
                        WritableMap result = Arguments.createMap();
                        result.putBoolean("success", false);
                        result.putString("error", "Failed to set user ID");
                        sendEvent("onOneKeyBindCompleted", result);
                    }
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error performing one-key bind by Berry protocol", e);
            promise.reject("ONE_KEY_BIND_ERROR", "Failed to perform one-key bind by Berry protocol: " + e.getMessage());
        }
    }

    // Device status and time sync methods
    @ReactMethod
    public void getDeviceInfo(Promise promise) {
        CallBackUtils.deviceInfoCallBack = new DeviceInfoCallBack() {
            @Override
            public void onDeviceInfo(DeviceInfoBean deviceInfoBean) {
                Log.d(TAG, "Device info received: " + deviceInfoBean.toString());
                WritableMap result = Arguments.createMap();
                if (deviceInfoBean != null) {
                    result.putString("equipmentNumber", deviceInfoBean.equipmentNumber != null ? deviceInfoBean.equipmentNumber : "");
                    result.putString("firmwareVersion", deviceInfoBean.firmwareVersion != null ? deviceInfoBean.firmwareVersion : "");
                    result.putString("serialNumber", deviceInfoBean.serialNumber != null ? deviceInfoBean.serialNumber : "");
                    result.putString("macAddress", deviceInfoBean.mac != null ? deviceInfoBean.mac : "");
                }
                sendEvent("onDeviceInfoReceived", result);
            }

            @Override
            public void onBatteryInfo(int capacity, int chargeStatus) {
                Log.d(TAG, "Battery info - Capacity: " + capacity + ", Status: " + chargeStatus);
                WritableMap result = Arguments.createMap();
                result.putInt("capacity", capacity);
                result.putInt("chargeStatus", chargeStatus);
                sendEvent("onBatteryInfoReceived", result);
            }
        };

        try {
            ControlBleTools.getInstance().getDeviceInfo(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Get device info state: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    sendEvent("onDeviceInfoRequestCompleted", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error getting device info", e);
            promise.reject("GET_DEVICE_INFO_ERROR", "Failed to get device info: " + e.getMessage());
        }
    }

    @ReactMethod
    public void setTime(Promise promise) {
        try {
            long currentTimeMillis = System.currentTimeMillis();
            ControlBleTools.getInstance().setTime(currentTimeMillis, true, new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Set time state: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    result.putDouble("timestamp", currentTimeMillis);
                    sendEvent("onTimeSetCompleted", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error setting time", e);
            promise.reject("SET_TIME_ERROR", "Failed to set time: " + e.getMessage());
        }
    }

    @ReactMethod
    public void setTimeFormat(boolean is24HourFormat, Promise promise) {
        try {
            ControlBleTools.getInstance().setTimeFormat(is24HourFormat, new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(SendCmdState state) {
                    Log.d(TAG, "Set time format state: " + state);
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("success", state == SendCmdState.SUCCEED);
                    result.putBoolean("is24Hour", is24HourFormat);
                    sendEvent("onTimeFormatSetCompleted", result);
                }
            });
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error setting time format", e);
            promise.reject("SET_TIME_FORMAT_ERROR", "Failed to set time format: " + e.getMessage());
        }
    }

    private void sendDeviceFoundEvent(ScanDeviceBean device) {
        WritableMap params = Arguments.createMap();
        params.putString("name", device.name);
        params.putString("address", device.address);
        params.putString("protocol", device.protocolName);
        params.putInt("rssi", device.rssi);
        params.putBoolean("isBind", device.isBind);
        
        sendEvent("onDeviceFound", params);
    }

    private void sendConnectionStateEvent(int state) {
        WritableMap params = Arguments.createMap();
        params.putInt("state", state);
        
        String stateString;
        switch (state) {
            case BleCommonAttributes.STATE_CONNECTED:
                stateString = "connected";
                break;
            case BleCommonAttributes.STATE_CONNECTING:
                stateString = "connecting";
                break;
            case BleCommonAttributes.STATE_DISCONNECTED:
                stateString = "disconnected";
                break;
            case BleCommonAttributes.STATE_TIME_OUT:
                stateString = "timeout";
                break;
            default:
                stateString = "unknown";
                break;
        }
        
        params.putString("stateString", stateString);
        sendEvent("onConnectionStateChanged", params);
    }

    private void sendEvent(String eventName, WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }
}
