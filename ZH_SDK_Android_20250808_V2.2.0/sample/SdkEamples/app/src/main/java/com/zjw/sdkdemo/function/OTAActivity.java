package com.zjw.sdkdemo.function;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.callback.DeviceLargeFileStatusListener;
import com.zhapp.ble.callback.UploadBigDataListener;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.utils.DeviceTools;
import com.zjw.sdkdemo.utils.Utils;

public class OTAActivity extends BaseActivity {
    final private String TAG = OTAActivity.class.getSimpleName();
    private DeviceTools mDeviceTools = MyApplication.getDeviceTools();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        setTitle(getString(R.string.s37));
        initView();
        initData();
    }

    void initView() {

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

    private void showTips(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(OTAActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void upLoadOta(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        String version = "443";
        String md5 = "1305828";
        ControlBleTools.getInstance().getDeviceLargeFileState(true, version, md5, new DeviceLargeFileStatusListener() {
            @Override
            public void onSuccess(int statusValue, String statusName) {
                if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.READY.getState()) {
                    byte[] fileByte = Utils.getBytesByAssets(OTAActivity.this, "ota.bin");
                    ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_OTA, fileByte, true, new UploadBigDataListener() {
                        @Override
                        public void onSuccess() {
                            showTips("ota onSuccess");
                        }

                        @Override
                        public void onProgress(int curPiece, int dataPackTotalPieceLength) {
                            int percentage = (curPiece * 100 / dataPackTotalPieceLength);
                            Log.e(TAG, "onProgress " + percentage);
                            ToastUtils.showShort("onProgress " + percentage);
                        }

                        @Override
                        public void onTimeout(String msg) {
                            showTips("ota onTimeout");
                        }
                    });
                } else if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.BUSY.getState()) {
                    showTips(getString(R.string.s223));
                } else if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.DUPLICATED.getState()) {
                    showTips(getString(R.string.s224));
                } else if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.LOW_STORAGE.getState()) {
                    showTips(getString(R.string.s224));
                } else if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.LOW_BATTERY.getState()) {
                    showTips(getString(R.string.s225));
                } else if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.DOWNGRADE.getState()) {
                    showTips(getString(R.string.s224));
                }
            }

            @Override
            public void timeOut() {
                showTips("ota timeOut");
            }
        });
    }
}
