package com.zjw.sdkdemo.function.esim.http;

import android.util.Log;

public class Logger {

    private static final String TAG = "LpaLog";
    private volatile static Logger instance;

    private static boolean DEBUG_FLAG = false;

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (HttpUtil.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public synchronized boolean isDEBUG_FLAG() {
        return DEBUG_FLAG;
    }

    public synchronized void setDEBUG_FLAG(boolean DEBUG_FLAG) {
        this.DEBUG_FLAG = DEBUG_FLAG;
    }

    public synchronized void i(String log){
        Log.i(TAG, log);
    }

    public synchronized void w(String log){
        Log.w(TAG, log);
    }

    public synchronized void d(String log){
        if (DEBUG_FLAG){
            Log.d(TAG, log);
        }
    }

    public synchronized void e(String log){
        Log.e(TAG, log);
    }

    public synchronized void v(String log){
        Log.v(TAG, log);
    }
}
