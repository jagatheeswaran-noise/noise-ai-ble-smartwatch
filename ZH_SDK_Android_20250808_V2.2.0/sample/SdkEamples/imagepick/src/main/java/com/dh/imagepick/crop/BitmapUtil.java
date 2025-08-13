package com.dh.imagepick.crop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;

public class BitmapUtil {

    private BitmapUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 获取图片的旋转角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 将图片按照指定的角度进行旋转
     *
     * @param bitmap 需要旋转的图片
     * @param degree 指定的旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bitmap, int degree) {
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return newBitmap;
    }

    /**
     * 获取我们需要的整理过旋转角度的Uri
     * @param activity  上下文环境
     * @param path      路径
     * @return          正常的Uri
     */
    public static Uri getRotatedUri(Activity activity, String path){
        int degree = BitmapUtil.getBitmapDegree(path);
        if (degree != 0){
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            Bitmap newBitmap = BitmapUtil.rotateBitmapByDegree(bitmap,degree);
            return Uri.parse(MediaStore.Images.Media.insertImage(activity.getContentResolver(),newBitmap,null,null));
        }else{
            return Uri.fromFile(new File(path));
        }
    }

    /**
     * 将图片按照指定的角度进行旋转
     *
     * @param path   需要旋转的图片的路径
     * @param degree 指定的旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(String path, int degree) {
        Bitmap bitmap = BitmapFactory.decodeFile(path)  ;
        return rotateBitmapByDegree(bitmap,degree);
    }


    /**
     * 把颜色替换成主题色，不改变图片的透明度
     */
    public static Bitmap replaceColorPix(int themeColor, Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = themeColor;
        int red = Color.red(tc);
        int green = Color.green(tc);
        int blue = Color.blue(tc);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int alpha = Color.alpha(color);
                if (alpha > 0) {
                    colors[j * width + i] = Color.argb(alpha, red, green, blue);
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }

    public static Bitmap replaceColorPix(Context context, int resId, int color) {
        Bitmap src = BitmapFactory.decodeResource(context
                .getResources(), resId);
        return replaceColorPix(color,src);
    }

    public static Bitmap replaceColor(Bitmap src, int sourceColor, int targetColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int sourceRgb = sourceColor & 0x00FFFFFF;
        int[] colors = new int[width * height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int rgb = color & 0x00FFFFFF;
                if (rgb != sourceRgb) {
                    colors[j * width + i] = color;
                    continue;
                }
                int alpha = color & 0xFF000000;
                if (alpha != 0)
                    colors[j * width + i] = alpha | targetColor;
            }
        }
        return Bitmap.createBitmap(colors, width, height, src.getConfig());
    }




    public static Bitmap replaceColorPix2(Bitmap src, int themeColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = themeColor & 0x00FFFFFF;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int alpha = 255 - Color.alpha(color);
                colors[j * width + i] = alpha << 24 | tc;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }

    /**
     * 把非白色的其他颜色替换成主题色
     *
     * @param src
     * @return
     */
    public static Bitmap replaceColorPixExceptWhite(Bitmap src, int themeColor) {

        try {
            int width = src.getWidth();
            int height = src.getHeight();
            int[] colors = new int[width * height];
            int tc = themeColor;
            int red = Color.red(tc);
            int green = Color.green(tc);
            int blue = Color.blue(tc);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int color = src.getPixel(i, j);
                    if (color == Color.WHITE) {
                        colors[j * width + i] = color;
                        continue;
                    }
                    int alpha = Color.alpha(color);
                    colors[j * width + i] = Color.argb(alpha, red, green, blue);
                }
            }

            return Bitmap.createBitmap(colors, width, height, src.getConfig());
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap replaceColorPixExceptBlack(Bitmap src, int themeColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = themeColor;
        int red = Color.red(tc);
        int green = Color.green(tc);
        int blue = Color.blue(tc);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                if (color == Color.BLACK) {
                    colors[j * width + i] = color;
                    continue;
                }
                int alpha = Color.alpha(color);
                colors[j * width + i] = Color.argb(alpha, red, green, blue);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }

}
