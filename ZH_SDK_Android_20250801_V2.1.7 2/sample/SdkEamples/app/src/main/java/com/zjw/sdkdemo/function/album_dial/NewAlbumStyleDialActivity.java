package com.zjw.sdkdemo.function.album_dial;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.zhapp.ble.BleCommonAttributes;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener;
import com.zhapp.ble.callback.DialDataCallBack;
import com.zhapp.ble.callback.UploadBigDataListener;
import com.zhapp.ble.custom.colckvff.ImageUtils;
import com.zhapp.ble.custom.colckvff.PlacementSelectModel;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.databinding.ActivityAlbumStyleDialBinding;
import com.zjw.sdkdemo.function.album_dial.adapter.BackgroundAdapter;
import com.zjw.sdkdemo.function.album_dial.adapter.PlacementAdapter;
import com.zjw.sdkdemo.function.album_dial.adapter.TextColourAdapter;
import com.zjw.sdkdemo.function.album_dial.model.BackgroundSelectModel;
import com.zjw.sdkdemo.function.album_dial.model.ColorSelectModel;
import com.zjw.sdkdemo.function.album_dial.utils.ImagePickUtils;
import com.zjw.sdkdemo.utils.AssetUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Android on 2023/10/12.
 */
public class NewAlbumStyleDialActivity extends AppCompatActivity {
    private ActivityAlbumStyleDialBinding binding;
    private List<ColorSelectModel> mColors;
    private String mSelectColor;
    private List<BackgroundSelectModel> mBackgrounds;
    private BackgroundSelectModel mBackgroundSelectModel;
    private List<PlacementSelectModel> mPlacements;
    private PlacementSelectModel mPlacementSelectModel;
    private TextColourAdapter mColourAdapter;
    private BackgroundAdapter mBackgroundAdapter;
    private PlacementAdapter mPlacementAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = ActivityAlbumStyleDialBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initData();
        initView();
    }

    private void initData() {
        Constants.init();
        mColors = new ArrayList<>();
        String[] list = {"#ffffff", "#000000", "#de4371", "#de4343",
                "#de7143", "#dba85c", "#dbcf60",
                "#b7c96b", "#a8e36d", "#85e36d", "#6de379", "#6de39c",
                "#6de3c0", "#6de3e3", "#6dc0e3", "#6d9ce3", "#6d79e3",
                "#856de3", "#a86de3", "#cb6de3", "#e36dd7", "#e36db4"};
        for (String s : list) {
            ColorSelectModel colorSelectModel = new ColorSelectModel();
            colorSelectModel.setColor(s);
            mColors.add(colorSelectModel);
        }
        mColors.get(0).setSelected(true);
        mSelectColor = mColors.get(0).getColor();
        mColourAdapter = new TextColourAdapter(mColors);

        mBackgrounds = new ArrayList<>();
        mBackgrounds.add(new BackgroundSelectModel());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        BackgroundSelectModel backgroundSelectModel = new BackgroundSelectModel();
        backgroundSelectModel.setSelected(true);
        backgroundSelectModel.setResId(R.mipmap.background);
        backgroundSelectModel.setBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.background, options));
        mBackgrounds.add(backgroundSelectModel);
        mBackgroundSelectModel = backgroundSelectModel;
        mBackgroundAdapter = new BackgroundAdapter(mBackgrounds);

        Bitmap[] placementType = {
                BitmapFactory.decodeResource(getResources(), R.mipmap.bottom_centre_1, options),
                BitmapFactory.decodeResource(getResources(), R.mipmap.top_left_1, options),
                BitmapFactory.decodeResource(getResources(), R.mipmap.top_centre_1, options)
        };

        ArrayList<byte[]> placementBins = new ArrayList<>();
        placementBins.add(AssetUtils.getAssetBytes(this, "colockff/Bottom-Centre.bin"));
        placementBins.add(AssetUtils.getAssetBytes(this, "colockff/Top-Left.bin"));
        placementBins.add(AssetUtils.getAssetBytes(this, "colockff/Top-Centre.bin"));


        mPlacements = new ArrayList<>(3);
        for (int i = 0; i < placementType.length; i++) {
            PlacementSelectModel model = new PlacementSelectModel();
            model.setBackground(mBackgroundSelectModel.getBitmap());
            model.setBackgroundResId(mBackgroundSelectModel.getResId());
            model.setPlacement(placementType[i]);
            model.setPlacementBin(placementBins.get(i));
            model.setSelected(false);
            model.setColor(mSelectColor);
            mPlacements.add(model);
        }


        mPlacements.get(0).setSelected(true);
        mPlacementSelectModel = mPlacements.get(0);
        mPlacementAdapter = new PlacementAdapter(mPlacements);
    }

    private void initView() {
        showSelect(true, true);
        LinearLayoutManager colourManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.textColourList.setLayoutManager(colourManager);
        binding.textColourList.setAdapter(mColourAdapter);
        binding.textColourList.setItemAnimator(null);
        mColourAdapter.setOnItemClickListener((adapter, view, position) -> {
            for (ColorSelectModel model : mColors) {
                model.setSelected(false);
            }
            mColors.get(position).setSelected(true);
            mSelectColor = mColors.get(position).getColor();
            mColourAdapter.notifyItemRangeChanged(0, mColors.size());
            showSelect(false, true);
        });

        LinearLayoutManager backgroundManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.backgroundList.setLayoutManager(backgroundManager);
        binding.backgroundList.setAdapter(mBackgroundAdapter);
        binding.backgroundList.setItemAnimator(null);
        mBackgroundAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (position == 0) {
                XXPermissions.with(NewAlbumStyleDialActivity.this)
                        .permission(Permission.WRITE_EXTERNAL_STORAGE)
                        .permission(Permission.READ_EXTERNAL_STORAGE)
                        .request((permissions, all) -> {
                            if (all) {
                                ImagePickUtils.INSTANCE.pickImageCrop(NewAlbumStyleDialActivity.this, Constants.WIDTH, Constants.HEIGHT, 0,
                                        path -> {
                                            for (BackgroundSelectModel model : mBackgrounds) {
                                                model.setSelected(false);
                                            }
                                            BackgroundSelectModel model = new BackgroundSelectModel();
                                            model.setBitmap(ConvertUtils.bytes2Bitmap(
                                                    FileIOUtils.readFile2BytesByStream(
                                                            path
                                                    )
                                            ));
                                            model.setSelected(true);
                                            mBackgrounds.add(1, model);
                                            mBackgroundSelectModel = model;
                                            mBackgroundAdapter.notifyItemRangeChanged(0, mBackgrounds.size());
                                            showSelect(true, false);
                                            return null;
                                        }, error -> {
                                            Log.e("NewAlbumStyleDial", error);
                                            return null;
                                        });
                            }
                        });
                return;
            }
            for (BackgroundSelectModel model : mBackgrounds) {
                model.setSelected(false);
            }
            mBackgrounds.get(position).setSelected(true);
            mBackgroundSelectModel = mBackgrounds.get(position);
            mBackgroundAdapter.notifyItemRangeChanged(0, mBackgrounds.size());
            showSelect(true, false);
        });

        LinearLayoutManager placementManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.placementList.setLayoutManager(placementManager);
        binding.placementList.setAdapter(mPlacementAdapter);
        binding.placementList.setItemAnimator(null);
        mPlacementAdapter.setOnItemClickListener((adapter, view, position) -> {
            for (PlacementSelectModel model : mPlacements) {
                model.setSelected(false);
            }
            mPlacements.get(position).setSelected(true);
            mPlacementSelectModel = mPlacements.get(position);
            mPlacementAdapter.notifyItemRangeChanged(0, mPlacements.size());
            showSelect(false, false);
        });

        binding.generate.setOnClickListener(view -> {

            int color = Color.parseColor(mSelectColor);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            //TODO 获取表盘文件   Get the watch face file
            ControlBleTools.getInstance().newCustomClockDialData(mPlacementSelectModel.getPlacementBin(),
                    red, green, blue,
                    mPlacementSelectModel.getBackground(),
                    mPlacementSelectModel.getPlacement(),
                    new DialDataCallBack() {
                @Override
                public void onDialData(byte[] data) {
                    if (data != null) {
                        //TODO 开始传输表盘   Start transferring watch faces
                        uploadWatch(data);
                    }
                }
            }, true);
        });
    }

    private void showSelect(boolean isBackgroundChanged, boolean isColorChanged) {
        if (isBackgroundChanged && mBackgroundSelectModel != null) {
            int resId = mBackgroundSelectModel.getResId();
            Bitmap bitmap = mBackgroundSelectModel.getBitmap();
            for (PlacementSelectModel model : mPlacements) {
                model.setBackgroundResId(resId);
                model.setBackground(bitmap);
            }
            mPlacementAdapter.notifyItemRangeChanged(0, mPlacements.size());
        }
        if (isColorChanged && !TextUtils.isEmpty(mSelectColor)) {
            for (PlacementSelectModel model : mPlacements) {
                model.setColor(mSelectColor);
            }
            mPlacementAdapter.notifyItemRangeChanged(0, mPlacements.size());
        }
        int color = Color.parseColor(mSelectColor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        binding.mainDial.setImageBitmap(ImageUtils.INSTANCE.composeImage(mPlacementSelectModel.getPlacement(), red, green, blue, mPlacementSelectModel.getBackground()));
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
