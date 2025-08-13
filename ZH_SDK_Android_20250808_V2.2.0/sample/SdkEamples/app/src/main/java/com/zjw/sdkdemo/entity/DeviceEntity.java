package com.zjw.sdkdemo.entity;


/**
 * Created by android
 * on 2021/2/23
 */
public class DeviceEntity implements Comparable<DeviceEntity> {
    public String address;
    public String name;
    public int rssi;
    public byte[] scanRecord;
    @Override
    public int compareTo(DeviceEntity o) {
        int num1 = this.rssi;
        int num2 = o.rssi;
        int aa = 0;
        if (num2 > num1) {
            aa = (num2 - num1);
        }
        if (num2 < num1) {
            aa = (num2 - num1);
        }
        return aa;
    }

}
