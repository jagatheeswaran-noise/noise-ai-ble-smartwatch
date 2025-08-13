package com.zjw.sdkdemo.function;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.callback.DeviceLargeFileStatusListener;
import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener;
import com.zhapp.ble.callback.DialDataCallBack;
import com.zhapp.ble.callback.UploadBigDataListener;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.utils.AssetUtils;
import com.zjw.sdkdemo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class PhotoDialActivity extends BaseActivity {

    public ImageView ivEffect;
    public TextView ivColor1;
    public TextView ivColor2;
    public TextView ivColor3;

    String numberTag = "368x448_1002";
    private int colorR = 255;
    private int colorG = 255;
    private int colorB = 255;
    private String sourceBinFileName;
    private String textFileNmae;
    private String bgFileName;
    private Bitmap bgBitmap = null;
    private android.graphics.Bitmap textBitmap = null;

    private byte[] sourceData = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_dial);
        setTitle(getString(R.string.s21));
        initView();
        intData();
    }

    private void initView() {
        ivColor2 = findViewById(R.id.ivColor2);
        ivColor1 = findViewById(R.id.ivColor1);
        ivEffect = findViewById(R.id.ivEffect);
        ivColor3 = findViewById(R.id.ivColor3);
    }

    private void intData() {
        initSet();
        updateUi();
    }

    private void initSet() {
        sourceBinFileName = numberTag + "_" + "source.bin";
        sourceData = AssetUtils.getAssetBytes(this, AssetUtils.ASSETS_CUSTOM_DIAL_DIR + sourceBinFileName);
        textFileNmae = "img_zdy_text.png";
        textBitmap = AssetUtils.getAssetBitmap(this, AssetUtils.ASSETS_CUSTOM_DIAL_DIR + textFileNmae);
        bgFileName = "img_zdy_bg.png";
    }

    private void initSet1() {
        sourceBinFileName = "hor.bin";
        sourceData = AssetUtils.getAssetBytes(this, AssetUtils.ASSETS_CUSTOM_DIAL_DIR + sourceBinFileName);
        textFileNmae = "img_zdy_text1.png";
        textBitmap = AssetUtils.getAssetBitmap(this, AssetUtils.ASSETS_CUSTOM_DIAL_DIR + textFileNmae);
        bgFileName = "img_zdy_bg1.png";
    }

    void updateUi() {
        bgBitmap = AssetUtils.getAssetBitmap(this, AssetUtils.ASSETS_CUSTOM_DIAL_DIR + bgFileName);
        //需要将背景图修改为当前设备屏幕高宽像素大小 eg: 350 * 400   The background image needs to be modified to correspond to the height, width and pixel size of the device screen eg: 350 * 400
        //获取修改后的效果图 Get the modified rendering
        ControlBleTools.getInstance().myCustomClockUtils(sourceData, colorR, colorG, colorB, bgBitmap, textBitmap,
                result -> ivEffect.setImageBitmap(result));
    }

    int type = 0;

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.switchData1) {
            initSet();
            updateUi();
        } else if (id == R.id.switchData2) {
            initSet1();
            updateUi();
        } else if (id == R.id.ivColor1) {
            colorR = 51;
            colorG = 153;
            colorB = 255;
            updateUi();
        } else if (id == R.id.ivColor2) {
            colorR = 255;
            colorG = 153;
            colorB = 51;
            updateUi();
        } else if (id == R.id.ivColor3) {
            colorR = 51;
            colorG = 255;
            colorB = 153;
            updateUi();
        } else if (id == R.id.btnSpp) {
            type = 1;
            //如果是方形表盘 If it is a square dial
            ControlBleTools.getInstance().newCustomClockDialData(sourceData, colorR, colorG,
                    colorB, bgBitmap, textBitmap, new DialDataCallBack() {
                        @Override
                        public void onDialData(byte[] data) {
                            uploadWatch(data);
                        }
                    }, true);
        } else if (id == R.id.btnBle) {
            type = 0;
            //如果是方形表盘 If it is a square dial
            ControlBleTools.getInstance().newCustomClockDialData(sourceData, colorR, colorG,
                    colorB, bgBitmap, textBitmap, new DialDataCallBack() {
                        @Override
                        public void onDialData(byte[] data) {
                            uploadWatch(data);
                        }
                    }, true);
        }
    }

    public static Bitmap getCoverBitmap(Context context, Bitmap inputBitmap) {
        String coverFileName = "clock_dial_2_bg.png";
        if (inputBitmap == null) {
            return null;
        }
        int width = inputBitmap.getWidth();
        int height = inputBitmap.getHeight();

        Bitmap auxiliaryBitmap = getAssetBitmap(context, "cover_img" + File.separator + coverFileName);
        auxiliaryBitmap = zoomImg(auxiliaryBitmap, width, height);
        Bitmap newBitmap = combineBitmap(inputBitmap, auxiliaryBitmap, 0, 0);
        return newBitmap;
    }

    /**
     * 获取Asset文件-图片
     */
    public static Bitmap getAssetBitmap(Context context, String fileName) {
        Bitmap bitmap = null;
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open(fileName);//filename是assets目录下的图片名
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
    }

    /**
     * 合并两张bitmap为一张
     *
     * @param background
     * @param foreground
     * @return Bitmap
     */
    public static Bitmap combineBitmap(Bitmap background, Bitmap foreground, int x, int y) {
        if (background == null) {
            return null;
        }
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(foreground, x, y, null);
        canvas.save();
        canvas.restore();
        return newmap;
    }

    private void uploadWatch(byte[] data) {
        ControlBleTools.getInstance().getDeviceWatchFace("180", data.length, true, new DeviceWatchFaceFileStatusListener() {
            @Override
            public void onSuccess(int statusValue, String statusName) {
                if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.READY.getState()) {
                    sendWatchData(data);
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

    private void sendWatchData(byte[] data) {
        showDialog();
        if (type == 0) {
            ControlBleTools.getInstance().startUploadBigData(
                    BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
                    data, true, new UploadBigDataListener() {
                        @Override
                        public void onSuccess() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onProgress(int curPiece, int dataPackTotalPieceLength) {
                            progressBar.setProgress(curPiece * 100 / dataPackTotalPieceLength);
                        }

                        @Override
                        public void onTimeout(String msg) {
                            Log.e("PhotoDialActivity", "startUploadBigData timeOut");
                        }
                    });
        } else {
            ControlBleTools.getInstance().startUploadBigDataUseSpp(
                    BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
                    data, true, new UploadBigDataListener() {
                        @Override
                        public void onSuccess() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onProgress(int curPiece, int dataPackTotalPieceLength) {
                            progressBar.setProgress(curPiece * 100 / dataPackTotalPieceLength);
                        }

                        @Override
                        public void onTimeout(String msg) {
                            Log.e("PhotoDialActivity", "startUploadBigData timeOut");
                        }
                    });
        }

    }

    private Dialog progressDialog;
    TextView msg, tvDeviceUpdateProgress;
    private ProgressBar progressBar;

    private void showDialog() {
        if (progressDialog == null) {
            progressDialog = new Dialog(this, R.style.progress_dialog);
            progressDialog.setContentView(R.layout.update_layout);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        msg = (TextView) progressDialog.findViewById(R.id.tvLoading);
        progressBar = progressDialog.findViewById(R.id.progressBar);
        tvDeviceUpdateProgress = progressDialog.findViewById(R.id.tvDeviceUpdateProgress);
        progressBar.setVisibility(View.VISIBLE);

        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);

        progressDialog.setOnDismissListener(dialog -> {
        });
    }
}