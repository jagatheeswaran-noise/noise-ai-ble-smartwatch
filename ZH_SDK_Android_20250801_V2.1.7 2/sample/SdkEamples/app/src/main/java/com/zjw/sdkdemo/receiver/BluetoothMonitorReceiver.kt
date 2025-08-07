package com.zjw.sdkdemo.receiver

import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log

/**
 * Created by Android on 2021/11/16.
 */
class BluetoothMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null) {
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    when (blueState) {
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            Log.i("BraceletActivity", "STATE_TURNING_ON")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Log.i("BraceletActivity", "STATE_ON")
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            Log.i("BraceletActivity", "STATE_TURNING_OFF")
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Log.i("BraceletActivity", "STATE_OFF")
                        }
                    }
                }

            }
        }
    }
}