package com.dh.imagepick.crop;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import com.dh.imagepick.crop.bean.ImageItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PhotoHandler {
    public static final int REQUEST_CODE_IMAGE = 0;
    public static final int REQUEST_CODE_CAMERA = 1;
    //public static final int REQUEST_CODE_CUT = 2;

    public static final int AVATAR_SIZE = 120;

    // 头像名称
    private String SELECT_ITEMS[];
    private Activity activity;


    private File tempFile;

    private ImagePicker imagePicker;


    /**
     * 裁剪图片方法实现
     */
    void startPhotoZoom(String filePath) {
        imagePicker.clearSelectedImages();
        ImageItem imageItem = new ImageItem();
        imageItem.path = filePath;
        imagePicker.addSelectedImageItem(0, imageItem, true);
        Intent intent = new Intent(activity, ImageCropActivity.class);
        activity.startActivityForResult(intent, ImagePicker.RESULT_CODE_ITEMS);
    }


    void doCompress(String src, String dest, Runnable task) {

            new CompressTask(src, dest, task).execute();

    }


    class CompressTask extends AsyncTask {

        public CompressTask(String src, String dest, Runnable uiAction) {
            this.src = src;
            this.dest = dest;
            this.uiAction = uiAction;
        }

        Runnable uiAction;
        String src;
        String dest;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Object doInBackground(Object[] params) {
            CompressOptions compressOptions = new CompressOptions();
            compressOptions.destFile = dest;
            compressOptions.filePath = src;
            compressFromUri(compressOptions);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {

            if (uiAction != null) {
                uiAction.run();
            }
        }
    }


    public static int findBestSampleSize(int actualWidth, int actualHeight,
                                         int desiredWidth, int desiredHeight) {
        float wr = (float) actualWidth / desiredWidth;
        float hr = (float) actualHeight / desiredHeight;
        float ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }

    public static int getResizedDimension(int maxPrimary, int maxSecondary,
                                          int actualPrimary, int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling
        // ratio.
        if (maxPrimary == 0) {
            float ratio = (float) maxSecondary / (float) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        float ratio = (float) actualSecondary / (float) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    public static final String CONTENT = "content";
    public static final String FILE = "file";

    /**
     * 图片压缩参数
     *
     * @author Administrator
     */
    public static class CompressOptions {
        public static final int DEFAULT_WIDTH = 1080;
        public static final int DEFAULT_HEIGHT = 1920;

        public int maxWidth = DEFAULT_WIDTH;
        public int maxHeight = DEFAULT_HEIGHT;
        /**
         * 压缩后图片保存的文件
         */
        public String destFile;
        /**
         * 图片压缩格式,默认为jpg格式
         */
        public Bitmap.CompressFormat imgFormat = Bitmap.CompressFormat.PNG;

        /**
         * 图片压缩比例 默认为30
         */
        public int quality = 30;

        public String filePath;
    }

    public static void commonCompress(String srcFile, String destFile) {
        CompressOptions compressOptions = new CompressOptions();
        compressOptions.destFile = destFile;
        compressOptions.filePath = srcFile;
        compressFromUri(compressOptions);
    }

    public static void compressFromUri(CompressOptions compressOptions) {

        // uri指向的文件路径
        String filePath = compressOptions.filePath;
        if (null == filePath) {
            return;
        }
        //获取原始图片的宽高
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(filePath, options);

        int actualWidth = options.outWidth;
        int actualHeight = options.outHeight;

        //计算出适当的图片大小
        int desiredWidth = getResizedDimension(compressOptions.maxWidth,
                compressOptions.maxHeight, actualWidth, actualHeight);
        int desiredHeight = getResizedDimension(compressOptions.maxHeight,
                compressOptions.maxWidth, actualHeight, actualWidth);

        options.inJustDecodeBounds = false;
        options.inSampleSize = findBestSampleSize(actualWidth, actualHeight,
                desiredWidth, desiredHeight);

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

//        Log.d("hdr","原本:"+actualWidth+","+actualHeight+" 压缩后:"+destBitmap.getWidth()+","+destBitmap.getHeight()+" 目的:"+desiredWidth+","+desiredHeight);
        // compress file if need
        if (null != compressOptions.destFile) {
            compressFile(bitmap, compressOptions.imgFormat, 100, compressOptions.destFile);
        }
        bitmap.recycle();
    }

    /**
     * compress file from bitmap with compressOptions
     */
    public static void compressFile(Bitmap bitmap, Bitmap.CompressFormat format, int quality, String targetFile) {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(targetFile);
            bitmap.compress(format, quality, stream);
        } catch (FileNotFoundException e) {
            Log.e("ImageCompress", e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
