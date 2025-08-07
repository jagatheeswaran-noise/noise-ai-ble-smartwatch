package com.zjw.sdkdemo.function;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.WatchFaceListBean;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener;
import com.zhapp.ble.callback.UploadBigDataListener;
import com.zhapp.ble.callback.WatchFaceListCallBack;
import com.zhapp.ble.parsing.SendCmdState;
import com.zhapp.ble.utils.BleUtils;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.utils.DeviceTools;
import com.zjw.sdkdemo.utils.Utils;


import java.util.List;

public class ClockDialActivity extends BaseActivity {
    final private String TAG = ClockDialActivity.class.getSimpleName();
    private DeviceTools mDeviceTools = MyApplication.getDeviceTools();
    private boolean isLongCLick = false;

    public TextView tvDeviceWatchList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_dial);
        setTitle(getString(R.string.s216));
        initView();
        initData();
    }

    @SuppressLint("ClickableViewAccessibility")
    void initView() {
        tvDeviceWatchList = findViewById(R.id.tvDeviceWatchList);
//        tvDeviceWatchList.setMovementMethod(ScrollingMovementMethod.getInstance());
//        tvDeviceWatchList.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (isLongCLick){
//                    isLongCLick = false;
//                    view.getParent().requestDisallowInterceptTouchEvent(false);
//                    return false;
//                }
//                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
//                    view.getParent().requestDisallowInterceptTouchEvent(true);
//                }
//                if(motionEvent.getAction()==MotionEvent.ACTION_MOVE){
//                    view.getParent().requestDisallowInterceptTouchEvent(true);
//                }
//                if(motionEvent.getAction()==MotionEvent.ACTION_UP){
//                    view.getParent().requestDisallowInterceptTouchEvent(false);
//                }
//                return false;
//            }
//        });
//        tvDeviceWatchList.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                Log.e("dddd", "onLongClick");
//                isLongCLick = true;
//                return false;
//            }
//        });
    }

    void initData() {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    public void uploadWatch(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        byte[] fileByte = Utils.getBytesByAssets(ClockDialActivity.this, "00000表盘0.bin");
        ControlBleTools.getInstance().getDeviceWatchFace("180"/* 真实的表盘id | real dial id*/,
                fileByte.length, true, new DeviceWatchFaceFileStatusListener() {
                    @Override
                    public void onSuccess(int statusValue, String statusName) {
                        if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.READY.getState()) {
                            ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_WATCH, fileByte, true, new UploadBigDataListener() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onProgress(int curPiece, int dataPackTotalPieceLength) {
                                    int percentage = (curPiece * 100 / dataPackTotalPieceLength);
                                    Log.e(TAG, "onProgress " + percentage);
                                }

                                @Override
                                public void onTimeout(String msg) {
                                }
                            });
                        } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.BUSY.getState()) {
                            MyApplication.showToast(getString(R.string.s223));
                        } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.DUPLICATED.getState()) {
                            MyApplication.showToast(getString(R.string.s224));
                        } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_STORAGE.getState()) {
                            MyApplication.showToast(getString(R.string.s224));
                        } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_BATTERY.getState()) {
                            MyApplication.showToast(getString(R.string.s225));
                        } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.DOWNGRADE.getState()) {
                            MyApplication.showToast(getString(R.string.s224));
                        }
                    }

                    @Override
                    public void timeOut() {
                        MyApplication.showToast("timeOut");
                    }
                });
    }


    public void getWatchList(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        ControlBleTools.getInstance().getWatchFaceList(null);
        CallBackUtils.watchFaceListCallBack = new WatchFaceListCallBack() {
            @Override
            public void timeOut(SendCmdState errorState) {
                //TIMEOUT
            }

            @Override
            public void onResponse(List<WatchFaceListBean> list) {
                ((TextView) findViewById(R.id.tvDeviceWatchList)).setText(list.toString());
            }
        };
    }

    public void setDeviceWatchFromId(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        ControlBleTools.getInstance().setDeviceWatchFromId(((EditText) findViewById(R.id.etDeviceWatchID)).getText().toString().trim(), null);
    }

    public void deleteDeviceWatchFromId(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        ControlBleTools.getInstance().deleteDeviceWatchFromId(((EditText) findViewById(R.id.etDeviceWatchID)).getText().toString().trim(), null);
    }

    public void photoDial(View view) {
        startActivity(new Intent(this, PhotoDialActivity.class));
    }
}
