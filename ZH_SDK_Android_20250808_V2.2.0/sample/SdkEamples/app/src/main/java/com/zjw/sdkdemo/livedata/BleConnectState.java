package com.zjw.sdkdemo.livedata;

import androidx.lifecycle.MutableLiveData;

public class BleConnectState extends MutableLiveData<Integer> {
    private BleConnectState() {
    }

    private static class Holder {
        public static final BleConnectState INSTANCE = new BleConnectState();
    }

    public static BleConnectState getInstance() {
        return BleConnectState.Holder.INSTANCE;
    }
}