package com.zjw.sdkdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class DeviceTools {

    private Context cx;
    private static String DeviceXml = "demo_zhbracelet_device_tools";
    //连接LE蓝牙mac
    private final static String BLE_MAC  = "ble_mac";
    //BR蓝牙名
    private final static String HEADSET_NAME = "HEADSET_NAME";
    //BR蓝牙mac
    private final static String HEADSET_MAC = "HEADSET_MAC";

    public DeviceTools(Context context) {
        this.cx = context;
    }

    SharedPreferences getSharedPreferencesCommon() {
        SharedPreferences settin = cx.getSharedPreferences(DeviceXml, 0);
        return settin;
    }


    /**
     * 保存通话蓝牙的设备名
     * */
    public void setHeadsetName(String name) {
        SharedPreferences settin = getSharedPreferencesCommon();
        SharedPreferences.Editor editor = settin.edit();
        editor.putString(HEADSET_NAME, name);
        editor.commit();
    }

    /**
     * 获取通话蓝牙的设备名
     * */
    public String getHeadsetName() {
        SharedPreferences settin = getSharedPreferencesCommon();
        return settin.getString(HEADSET_NAME, "");
    }

    /**
     * 保存通话蓝牙的MAC
     * */
    public void setHeadsetMac(String mac) {
        SharedPreferences settin = getSharedPreferencesCommon();
        SharedPreferences.Editor editor = settin.edit();
        editor.putString(HEADSET_MAC, mac);
        editor.commit();
    }

    /**
     * 获取通话蓝牙mac
     * */
    public String getHeadsetMac() {
        SharedPreferences settin = getSharedPreferencesCommon();
        return settin.getString(HEADSET_MAC, "");
    }



}