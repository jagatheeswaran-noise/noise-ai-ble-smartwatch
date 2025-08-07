package com.dh.imagepick.crop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.dh.imagepick.R;
import com.dh.imagepick.crop.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;

public class ImageCropActivity extends ImageBaseActivity implements CropImageView.OnBitmapSaveCompleteListener {

    private CropImageView mCropImageView;
    private Bitmap mBitmap;
    private boolean mIsSaveRectangle;
    private int mOutputX;
    private int mOutputY;
    private ArrayList<ImageItem> mImageItems;
    private ImagePicker imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        imagePicker = ImagePicker.getInstance();

        //初始化View
        ImageView backImageView =  findViewById(R.id.backImageView);
        backImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        ImageView btn_ok = findViewById(R.id.confirmBtn);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropImageView.saveBitmapToFile(imagePicker.getCropCacheFolder(ImageCropActivity.this), mOutputX, mOutputY, mIsSaveRectangle);
            }
        });
        mCropImageView = (CropImageView) findViewById(R.id.cv_crop_image);
        mCropImageView.setOnBitmapSaveCompleteListener(this);

        //获取需要的参数
        mOutputX = imagePicker.getOutPutX();
        mOutputY = imagePicker.getOutPutY();
        mIsSaveRectangle = imagePicker.isSaveRectangle();
        mImageItems = imagePicker.getSelectedImages();
        String imagePath = mImageItems.get(0).path;

        mCropImageView.setFocusStyle(imagePicker.getStyle());
        mCropImageView.setFocusWidth(imagePicker.getFocusWidth());
        mCropImageView.setFocusHeight(imagePicker.getFocusHeight());

        //缩放图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        options.inSampleSize = calculateInSampleSize(options, displayMetrics.widthPixels, displayMetrics.heightPixels);
        options.inJustDecodeBounds = false;
        mBitmap = BitmapFactory.decodeFile(imagePath, options);
//        mCropImageView.setImageBitmap(mBitmap);
        //设置默认旋转角度
        mCropImageView.setImageBitmap(mCropImageView.rotate(mBitmap, BitmapUtil.getBitmapDegree(imagePath)));
        mCropImageView.setMinWidth(mBitmap.getWidth()/8);
        mCropImageView.setMinHeight(mBitmap.getHeight()/8);
//        mCropImageView.setImageURI(Uri.fromFile(new File(imagePath)));
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = width / reqWidth;
            } else {
                inSampleSize = height / reqHeight;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onBitmapSaveSuccess(File file) {
//        Toast.makeText(ImageCropActivity.this, "裁剪成功:" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        //裁剪后替换掉返回数据的内容，但是不要改变全局中的选中数据
        mImageItems.remove(0);
        ImageItem imageItem = new ImageItem();
        imageItem.path = file.getAbsolutePath();
        mImageItems.add(imageItem);

        Intent intent = new Intent();
        intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, mImageItems);
        setResult(RESULT_OK, intent);   //单选不需要裁剪，返回数据
        finish();
    }

    @Override
    public void onBitmapSaveError(File file) {
        Log.e("cdfd","裁剪失败");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCropImageView.setOnBitmapSaveCompleteListener(null);
        if (null != mBitmap && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
